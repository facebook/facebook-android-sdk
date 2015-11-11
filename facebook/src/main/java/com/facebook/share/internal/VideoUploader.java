/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.share.internal;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookGraphResponseException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.internal.WorkQueue;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class VideoUploader {

    private static final String TAG = "VideoUploader";

    private static final String PARAM_UPLOAD_PHASE = "upload_phase";
    private static final String PARAM_VALUE_UPLOAD_START_PHASE = "start";
    private static final String PARAM_VALUE_UPLOAD_TRANSFER_PHASE = "transfer";
    private static final String PARAM_VALUE_UPLOAD_FINISH_PHASE = "finish";

    private static final String PARAM_TITLE = "title";
    private static final String PARAM_DESCRIPTION = "description";
    private static final String PARAM_REF = "ref";
    private static final String PARAM_FILE_SIZE = "file_size";
    private static final String PARAM_SESSION_ID = "upload_session_id";
    private static final String PARAM_VIDEO_ID = "video_id";
    private static final String PARAM_START_OFFSET = "start_offset";
    private static final String PARAM_END_OFFSET = "end_offset";
    private static final String PARAM_VIDEO_FILE_CHUNK = "video_file_chunk";

    private static final String ERROR_UPLOAD = "Video upload failed";
    private static final String ERROR_BAD_SERVER_RESPONSE = "Unexpected error in server response";

    private static final int UPLOAD_QUEUE_MAX_CONCURRENT = WorkQueue.DEFAULT_MAX_CONCURRENT;
    private static final int MAX_RETRIES_PER_PHASE = 2;
    private static final int RETRY_DELAY_UNIT_MS = 5000;
    private static final int RETRY_DELAY_BACK_OFF_FACTOR = 3;

    private static boolean initialized;

    private static Handler handler;
    private static WorkQueue uploadQueue = new WorkQueue(UPLOAD_QUEUE_MAX_CONCURRENT);

    private static Set<UploadContext> pendingUploads = new HashSet<>();

    private static AccessTokenTracker accessTokenTracker;

    public static synchronized void uploadAsync(
            ShareVideoContent videoContent,
            FacebookCallback<Sharer.Result> callback)
            throws FileNotFoundException {
        uploadAsync(videoContent, "me", callback);
    }

    public static synchronized void uploadAsync(
            ShareVideoContent videoContent,
            String graphNode,
            FacebookCallback<Sharer.Result> callback)
            throws FileNotFoundException {
        if (!initialized) {
            registerAccessTokenTracker();
            initialized = true;
        }

        Validate.notNull(videoContent, "videoContent");
        Validate.notNull(graphNode, "graphNode");
        ShareVideo video = videoContent.getVideo();
        Validate.notNull(video, "videoContent.video");
        Uri videoUri = video.getLocalUrl();
        Validate.notNull(videoUri, "videoContent.video.localUrl");

        UploadContext uploadContext = new UploadContext(videoContent, graphNode, callback);
        uploadContext.initialize();

        pendingUploads.add(uploadContext);

        enqueueUploadStart(
                uploadContext,
                0);
    }

    private static synchronized void cancelAllRequests() {
        for (UploadContext uploadContext : pendingUploads) {
            uploadContext.isCanceled = true;
        }
    }

    private static synchronized void removePendingUpload(
            UploadContext uploadContext) {
        pendingUploads.remove(uploadContext);
    }

    private static synchronized Handler getHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    private static void issueResponse(
            final UploadContext uploadContext,
            final FacebookException error,
            final String videoId) {
        // Remove the UploadContext synchronously
        // Once the UploadContext is removed, this is the only reference to it.
        removePendingUpload(uploadContext);

        Utility.closeQuietly(uploadContext.videoStream);

        if (uploadContext.callback != null) {
            if (error != null) {
                ShareInternalUtility.invokeOnErrorCallback(uploadContext.callback, error);
            } else if (uploadContext.isCanceled) {
                ShareInternalUtility.invokeOnCancelCallback(uploadContext.callback);
            } else {
                ShareInternalUtility.invokeOnSuccessCallback(uploadContext.callback, videoId);
            }
        }
    }

    private static void enqueueUploadStart(UploadContext uploadContext, int completedRetries) {
        enqueueRequest(
                uploadContext,
                new StartUploadWorkItem(
                        uploadContext,
                        completedRetries));
    }

    private static void enqueueUploadChunk(
            UploadContext uploadContext,
            String chunkStart,
            String chunkEnd,
            int completedRetries) {
        enqueueRequest(
                uploadContext,
                new TransferChunkWorkItem(
                        uploadContext,
                        chunkStart,
                        chunkEnd,
                        completedRetries));
    }

    private static void enqueueUploadFinish(UploadContext uploadContext, int completedRetries) {
        enqueueRequest(
                uploadContext,
                new FinishUploadWorkItem(
                        uploadContext,
                        completedRetries));
    }

    private static synchronized void enqueueRequest(
            UploadContext uploadContext,
            Runnable workItem) {
        uploadContext.workItem = uploadQueue.addActiveWorkItem(workItem);
    }

    private static byte[] getChunk(
            UploadContext uploadContext,
            String chunkStart,
            String chunkEnd)
            throws IOException {
        if (!Utility.areObjectsEqual(chunkStart, uploadContext.chunkStart)) {
            // Something went wrong in the book-keeping here.
            logError(
                    null,
                    "Error reading video chunk. Expected chunk '%s'. Requested chunk '%s'.",
                    uploadContext.chunkStart,
                    chunkStart);
            return null;
        }

        long chunkStartLong = Long.parseLong(chunkStart);
        long chunkEndLong = Long.parseLong(chunkEnd);
        int chunkSize = (int) (chunkEndLong - chunkStartLong);

        ByteArrayOutputStream byteBufferStream = new ByteArrayOutputStream();
        int bufferSize = Math.min(8192, chunkSize);
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = uploadContext.videoStream.read(buffer)) != -1) {
            byteBufferStream.write(buffer, 0, len);

            chunkSize -= len;
            if (chunkSize == 0) {
                // Done!
                break;
            } else if (chunkSize < 0) {
                // This should not happen. Signal an error.
                logError(
                        null,
                        "Error reading video chunk. Expected buffer length - '%d'. Actual - '%d'.",
                        chunkSize + len,
                        len);
                return null;
            }
        }

        uploadContext.chunkStart = chunkEnd;

        return byteBufferStream.toByteArray();
    }

    private static void registerAccessTokenTracker() {
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                if (oldAccessToken == null) {
                    // If we never had an access token, then there would be no pending uploads.
                    return;
                }

                if (currentAccessToken == null ||
                        !Utility.areObjectsEqual(
                                currentAccessToken.getUserId(),
                                oldAccessToken.getUserId())) {
                    // Cancel any pending uploads since the user changed.
                    cancelAllRequests();
                }
            }
        };
    }

    private static void logError(
            Exception e,
            String format,
            Object... args) {
        Log.e(TAG, String.format(Locale.ROOT, format, args), e);
    }

    private static class UploadContext {
        public final Uri videoUri;
        public final String title;
        public final String description;
        public final String ref;
        public final String graphNode;

        public final AccessToken accessToken;

        public final FacebookCallback<Sharer.Result> callback;

        public String sessionId;
        public String videoId;
        public InputStream videoStream;
        public long videoSize;
        public String chunkStart = "0";
        public boolean isCanceled;
        public WorkQueue.WorkItem workItem;
        public Bundle params;

        private UploadContext(
                ShareVideoContent videoContent,
                String graphNode,
                FacebookCallback<Sharer.Result> callback) {
            // Store off the access token right away so that under no circumstances will we
            // end up with different tokens between phases. We will rely on the access token tracker
            // to cancel pending uploads.
            this.accessToken = AccessToken.getCurrentAccessToken();
            this.videoUri = videoContent.getVideo().getLocalUrl();
            this.title = videoContent.getContentTitle();
            this.description = videoContent.getContentDescription();
            this.ref = videoContent.getRef();
            this.graphNode = graphNode;
            this.callback = callback;
            this.params = videoContent.getVideo().getParameters();
        }

        private void initialize()
                throws FileNotFoundException {
            ParcelFileDescriptor fileDescriptor = null;
            try {
                if (Utility.isFileUri(videoUri)) {
                    fileDescriptor = ParcelFileDescriptor.open(
                            new File(videoUri.getPath()),
                            ParcelFileDescriptor.MODE_READ_ONLY);
                    videoSize = fileDescriptor.getStatSize();
                    videoStream = new ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor);
                } else if (Utility.isContentUri(videoUri)) {
                    videoSize = Utility.getContentSize(videoUri);
                    videoStream = FacebookSdk
                            .getApplicationContext()
                            .getContentResolver()
                            .openInputStream(videoUri);
                } else {
                    throw new FacebookException("Uri must be a content:// or file:// uri");
                }
            } catch (FileNotFoundException e) {
                Utility.closeQuietly(videoStream);

                throw e;
            }
        }
    }

    private static class StartUploadWorkItem extends UploadWorkItemBase {
        static final Set<Integer> transientErrorCodes = new HashSet<Integer>() {{
            add(6000);
        }};

        public StartUploadWorkItem(UploadContext uploadContext, int completedRetries) {
            super(uploadContext, completedRetries);
        }

        @Override
        public Bundle getParameters() {
            Bundle parameters = new Bundle();
            parameters.putString(PARAM_UPLOAD_PHASE, PARAM_VALUE_UPLOAD_START_PHASE);
            parameters.putLong(PARAM_FILE_SIZE, uploadContext.videoSize);

            return parameters;
        }

        @Override
        protected void handleSuccess(JSONObject jsonObject)
                throws JSONException {
            uploadContext.sessionId = jsonObject.getString(PARAM_SESSION_ID);
            uploadContext.videoId = jsonObject.getString(PARAM_VIDEO_ID);
            String startOffset = jsonObject.getString(PARAM_START_OFFSET);
            String endOffset = jsonObject.getString(PARAM_END_OFFSET);

            enqueueUploadChunk(
                    uploadContext,
                    startOffset,
                    endOffset,
                    0);
        }

        @Override
        protected void handleError(FacebookException error) {
            logError(error, "Error starting video upload");
            endUploadWithFailure(error);
        }

        @Override
        protected Set<Integer> getTransientErrorCodes() {
            return transientErrorCodes;
        }

        @Override
        protected void enqueueRetry(int retriesCompleted) {
            enqueueUploadStart(uploadContext, retriesCompleted);
        }
    }

    private static class TransferChunkWorkItem extends UploadWorkItemBase {
        static final Set<Integer> transientErrorCodes = new HashSet<Integer>() {{
            add(1363019);
            add(1363021);
            add(1363030);
            add(1363033);
            add(1363041);
        }};

        private String chunkStart;
        private String chunkEnd;

        public TransferChunkWorkItem(
                UploadContext uploadContext,
                String chunkStart,
                String chunkEnd,
                int completedRetries) {
            super(uploadContext, completedRetries);
            this.chunkStart = chunkStart;
            this.chunkEnd = chunkEnd;
        }

        @Override
        public Bundle getParameters()
                throws IOException {
            Bundle parameters = new Bundle();
            parameters.putString(PARAM_UPLOAD_PHASE, PARAM_VALUE_UPLOAD_TRANSFER_PHASE);
            parameters.putString(PARAM_SESSION_ID, uploadContext.sessionId);
            parameters.putString(PARAM_START_OFFSET, chunkStart);

            byte[] chunk = getChunk(uploadContext, chunkStart, chunkEnd);
            if (chunk != null) {
                parameters.putByteArray(PARAM_VIDEO_FILE_CHUNK, chunk);
            } else {
                throw new FacebookException("Error reading video");
            }

            return parameters;
        }

        @Override
        protected void handleSuccess(JSONObject jsonObject)
                throws JSONException {
            String startOffset = jsonObject.getString(PARAM_START_OFFSET);
            String endOffset = jsonObject.getString(PARAM_END_OFFSET);

            if (Utility.areObjectsEqual(startOffset, endOffset)) {
                enqueueUploadFinish(
                        uploadContext,
                        0);
            } else {
                enqueueUploadChunk(
                        uploadContext,
                        startOffset,
                        endOffset,
                        0);
            }
        }

        @Override
        protected void handleError(FacebookException error) {
            logError(error, "Error uploading video '%s'", uploadContext.videoId);
            endUploadWithFailure(error);
        }

        @Override
        protected Set<Integer> getTransientErrorCodes() {
            return transientErrorCodes;
        }

        @Override
        protected void enqueueRetry(int retriesCompleted) {
            enqueueUploadChunk(uploadContext, chunkStart, chunkEnd, retriesCompleted);
        }
    }

    private static class FinishUploadWorkItem extends UploadWorkItemBase {
        static final Set<Integer> transientErrorCodes = new HashSet<Integer>() {{
            add(1363011);
        }};

        public FinishUploadWorkItem(UploadContext uploadContext, int completedRetries) {
            super(uploadContext, completedRetries);
        }

        @Override
        public Bundle getParameters() {
            Bundle parameters = new Bundle();
            if (uploadContext.params != null) {
                parameters.putAll(uploadContext.params);
            }
            parameters.putString(PARAM_UPLOAD_PHASE, PARAM_VALUE_UPLOAD_FINISH_PHASE);
            parameters.putString(PARAM_SESSION_ID, uploadContext.sessionId);
            Utility.putNonEmptyString(parameters, PARAM_TITLE, uploadContext.title);
            Utility.putNonEmptyString(parameters, PARAM_DESCRIPTION, uploadContext.description);
            Utility.putNonEmptyString(parameters, PARAM_REF, uploadContext.ref);

            return parameters;
        }

        @Override
        protected void handleSuccess(JSONObject jsonObject)
                throws JSONException {
            if (jsonObject.getBoolean("success")) {
                issueResponseOnMainThread(null, uploadContext.videoId);
            } else {
                handleError(new FacebookException(ERROR_BAD_SERVER_RESPONSE));
            }
        }

        @Override
        protected void handleError(FacebookException error) {
            logError(error, "Video '%s' failed to finish uploading", uploadContext.videoId);
            endUploadWithFailure(error);
        }

        @Override
        protected Set<Integer> getTransientErrorCodes() {
            return transientErrorCodes;
        }

        @Override
        protected void enqueueRetry(int retriesCompleted) {
            enqueueUploadFinish(uploadContext, retriesCompleted);
        }
    }

    private static abstract class UploadWorkItemBase implements Runnable {
        protected UploadContext uploadContext;
        protected int completedRetries;

        protected UploadWorkItemBase(
                UploadContext uploadContext,
                int completedRetries) {
            this.uploadContext = uploadContext;
            this.completedRetries = completedRetries;
        }

        @Override
        public void run() {
            if (!uploadContext.isCanceled) {
                try {
                    executeGraphRequestSynchronously(getParameters());
                } catch (FacebookException fe) {
                    endUploadWithFailure(fe);
                } catch (Exception e) {
                    endUploadWithFailure(new FacebookException(ERROR_UPLOAD, e));
                }
            } else {
                // No specific failure here.
                endUploadWithFailure(null);
            }
        }

        protected void executeGraphRequestSynchronously(Bundle parameters) {
            GraphRequest request = new GraphRequest(
                    uploadContext.accessToken,
                    String.format(Locale.ROOT, "%s/videos", uploadContext.graphNode),
                    parameters,
                    HttpMethod.POST,
                    null);
            GraphResponse response = request.executeAndWait();

            if (response != null) {
                FacebookRequestError error = response.getError();
                JSONObject responseJSON = response.getJSONObject();
                if (error != null) {
                    if (!attemptRetry(error.getSubErrorCode())) {
                        handleError(new FacebookGraphResponseException(response, ERROR_UPLOAD));
                    }
                } else if (responseJSON != null) {
                    try {
                        handleSuccess(responseJSON);
                    } catch (JSONException e) {
                        endUploadWithFailure(new FacebookException(ERROR_BAD_SERVER_RESPONSE, e));
                    }
                } else {
                    handleError(new FacebookException(ERROR_BAD_SERVER_RESPONSE));
                }
            } else {
                handleError(new FacebookException(ERROR_BAD_SERVER_RESPONSE));
            }
        }

        private boolean attemptRetry(int errorCode) {
            if (completedRetries < MAX_RETRIES_PER_PHASE &&
                    getTransientErrorCodes().contains(errorCode)) {
                int delay = RETRY_DELAY_UNIT_MS * (int) Math.pow(
                        RETRY_DELAY_BACK_OFF_FACTOR, completedRetries);

                // Enqueuing the retry from the main thread which should be a lightweight
                // action with no I/O.
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        enqueueRetry(completedRetries + 1);
                    }
                }, delay);

                return true;
            } else {
                return false;
            }
        }

        protected void endUploadWithFailure(FacebookException error) {
            issueResponseOnMainThread(error, null);
        }

        protected void issueResponseOnMainThread(
                final FacebookException error,
                final String videoId) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    issueResponse(uploadContext, error, videoId);
                }
            });
        }

        protected abstract Bundle getParameters()
                throws Exception;

        protected abstract void handleSuccess(JSONObject jsonObject)
                throws JSONException;

        protected abstract void handleError(FacebookException error);

        protected abstract Set<Integer> getTransientErrorCodes();

        protected abstract void enqueueRetry(int retriesCompleted);
    }
}
