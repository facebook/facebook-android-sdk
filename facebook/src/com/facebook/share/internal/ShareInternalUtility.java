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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookGraphResponseException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequest.Callback;
import com.facebook.GraphResponse;
import com.facebook.internal.GraphUtil;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.NativeAppCallAttachmentStore;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.Utility;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.LikeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public final class ShareInternalUtility {
    private static final String OBJECT_PARAM = "object";
    private static final String MY_PHOTOS = "me/photos";
    private static final String MY_VIDEOS = "me/videos";
    private static final String MY_FEED = "me/feed";
    private static final String MY_STAGING_RESOURCES = "me/staging_resources";
    private static final String MY_OBJECTS_FORMAT = "me/objects/%s";
    private static final String MY_ACTION_FORMAT = "me/%s";

    // Parameter names/values
    private static final String PICTURE_PARAM = "picture";
    private static final String STAGING_PARAM = "file";

    public static void invokeCallbackWithException(
            FacebookCallback<Sharer.Result> callback,
            final Exception exception) {
        if (exception instanceof FacebookException) {
            invokeOnErrorCallback(callback, (FacebookException) exception);
            return;
        }
        invokeCallbackWithError(
                callback,
                "Error preparing share content: " + exception.getLocalizedMessage());
    }

    public static void invokeCallbackWithError(
            FacebookCallback<Sharer.Result> callback,
            String error) {
        invokeOnErrorCallback(callback, error);
    }

    public static void invokeCallbackWithResults(
            FacebookCallback<Sharer.Result> callback,
            final String postId,
            final GraphResponse graphResponse) {
        FacebookRequestError requestError = graphResponse.getError();
        if (requestError != null) {
            String errorMessage = requestError.getErrorMessage();
            if (Utility.isNullOrEmpty(errorMessage)) {
                errorMessage = "Unexpected error sharing.";
            }
            invokeOnErrorCallback(callback, graphResponse, errorMessage);
        } else {
            invokeOnSuccessCallback(callback, postId);
        }
    }

    /**
     * Determines whether the native dialog completed normally (without error or exception).
     *
     * @param result the bundle passed back to onActivityResult
     * @return true if the native dialog completed normally
     */
    public static boolean getNativeDialogDidComplete(Bundle result) {
        if (result.containsKey(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETE_KEY)) {
            return result.getBoolean(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETE_KEY);
        }
        return result.getBoolean(NativeProtocol.EXTRA_DIALOG_COMPLETE_KEY, false);
    }

    /**
     * Returns the gesture with which the user completed the native dialog. This is only returned
     * if the user has previously authorized the calling app with basic permissions.
     *
     * @param result the bundle passed back to onActivityResult
     * @return "post" or "cancel" as the completion gesture
     */
    public static String getNativeDialogCompletionGesture(Bundle result) {
        if (result.containsKey(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY)) {
            return result.getString(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY);
        }
        return result.getString(NativeProtocol.EXTRA_DIALOG_COMPLETION_GESTURE_KEY);
    }

    /**
     * Returns the id of the published post. This is only returned if the user has previously
     * given the app publish permissions.
     *
     * @param result the bundle passed back to onActivityResult
     * @return the id of the published post
     */
    public static String getShareDialogPostId(Bundle result) {
        if (result.containsKey(ShareConstants.RESULT_POST_ID)) {
            return result.getString(ShareConstants.RESULT_POST_ID);
        }
        if (result.containsKey(ShareConstants.EXTRA_RESULT_POST_ID)) {
            return result.getString(ShareConstants.EXTRA_RESULT_POST_ID);
        }
        return result.getString(ShareConstants.WEB_DIALOG_RESULT_PARAM_POST_ID);
    }

    public static boolean handleActivityResult(
            int requestCode,
            int resultCode,
            Intent data,
            ResultProcessor resultProcessor) {
        AppCall appCall = getAppCallFromActivityResult(requestCode, resultCode, data);
        if (appCall == null) {
            return false;
        }

        NativeAppCallAttachmentStore.cleanupAttachmentsForCall(appCall.getCallId());
        if (resultProcessor == null) {
            return true;
        }

        FacebookException exception = NativeProtocol.getExceptionFromErrorData(
                NativeProtocol.getErrorDataFromResultIntent(data));
        if (exception != null) {
            if (exception instanceof FacebookOperationCanceledException) {
                resultProcessor.onCancel(appCall);
            } else {
                resultProcessor.onError(appCall, exception);
            }
        } else {
            // If here, we did not find an error in the result.
            Bundle results = NativeProtocol.getSuccessResultsFromIntent(data);
            resultProcessor.onSuccess(appCall, results);
        }

        return true;
    }

    // Custom handling for Share so that we can log results
    public static ResultProcessor getShareResultProcessor(
            final FacebookCallback<Sharer.Result> callback) {
        return new ResultProcessor(callback) {
            @Override
            public void onSuccess(AppCall appCall, Bundle results) {
                if (results != null) {
                    final String gesture = getNativeDialogCompletionGesture(results);
                    if (gesture == null || "post".equalsIgnoreCase(gesture)) {
                        String postId = getShareDialogPostId(results);
                        invokeOnSuccessCallback(callback, postId);
                    } else if ("cancel".equalsIgnoreCase(gesture)) {
                        invokeOnCancelCallback(callback);
                    } else {
                        invokeOnErrorCallback(
                                callback,
                                new FacebookException(NativeProtocol.ERROR_UNKNOWN_ERROR));
                    }
                }
            }

            @Override
            public void onCancel(AppCall appCall) {
                invokeOnCancelCallback(callback);
            }

            @Override
            public void onError(AppCall appCall, FacebookException error) {
                invokeOnErrorCallback(callback, error);
            }
        };
    }

    private static AppCall getAppCallFromActivityResult(int requestCode,
                                                        int resultCode,
                                                        Intent data) {
        UUID callId = NativeProtocol.getCallIdFromIntent(data);
        if (callId == null) {
            return null;
        }

        return AppCall.finishPendingCall(callId, requestCode);
    }

    public static void registerStaticShareCallback(
            final int requestCode) {
        CallbackManagerImpl.registerStaticCallback(
                requestCode,
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        return handleActivityResult(
                                requestCode,
                                resultCode,
                                data,
                                getShareResultProcessor(null));
                    }
                }
        );
    }

    public static void registerSharerCallback(
            final int requestCode,
            final CallbackManager callbackManager,
            final FacebookCallback<Sharer.Result> callback) {
        if (!(callbackManager instanceof CallbackManagerImpl)) {
            throw new FacebookException("Unexpected CallbackManager, " +
                    "please use the provided Factory.");
        }

        ((CallbackManagerImpl) callbackManager).registerCallback(
                requestCode,
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        return handleActivityResult(
                                requestCode,
                                resultCode,
                                data,
                                getShareResultProcessor(callback));
                    }
                });
    }

    public static List<String> getPhotoUrls(
            final SharePhotoContent photoContent,
            final UUID appCallId) {
        List<SharePhoto> photos;
        if (photoContent == null || (photos = photoContent.getPhotos()) == null) {
            return null;
        }

        List<NativeAppCallAttachmentStore.Attachment> attachments = Utility.map(
                photos,
                new Utility.Mapper<SharePhoto, NativeAppCallAttachmentStore.Attachment>() {
                    @Override
                    public NativeAppCallAttachmentStore.Attachment apply(SharePhoto item) {
                        return getAttachment(appCallId, item);
                    }
                });

        List<String> attachmentUrls = Utility.map(
                attachments,
                new Utility.Mapper<NativeAppCallAttachmentStore.Attachment, String>() {
                    @Override
                    public String apply(NativeAppCallAttachmentStore.Attachment item) {
                        return item.getAttachmentUrl();
                    }
                });

        NativeAppCallAttachmentStore.addAttachments(attachments);

        return attachmentUrls;
    }

    public static JSONObject toJSONObjectForCall(
            final UUID callId,
            final ShareOpenGraphAction action)
            throws JSONException {

        final ArrayList<NativeAppCallAttachmentStore.Attachment> attachments = new ArrayList<>();
        JSONObject actionJSON = OpenGraphJSONUtility.toJSONObject(
                action,
                new OpenGraphJSONUtility.PhotoJSONProcessor() {
                    @Override
                    public JSONObject toJSONObject(SharePhoto photo) {
                        NativeAppCallAttachmentStore.Attachment attachment = getAttachment(
                                callId,
                                photo);

                        if (attachment == null) {
                            return null;
                        }

                        attachments.add(attachment);

                        JSONObject photoJSONObject = new JSONObject();
                        try {
                            photoJSONObject.put(
                                    NativeProtocol.IMAGE_URL_KEY, attachment.getAttachmentUrl());
                            if (photo.getUserGenerated()) {
                                photoJSONObject.put(NativeProtocol.IMAGE_USER_GENERATED_KEY, true);
                            }
                        } catch (JSONException e) {
                            throw new FacebookException("Unable to attach images", e);
                        }
                        return photoJSONObject;
                    }
                });

        NativeAppCallAttachmentStore.addAttachments(attachments);

        return actionJSON;
    }

    public static JSONObject toJSONObjectForWeb(
            final ShareOpenGraphContent shareOpenGraphContent)
            throws JSONException {
        ShareOpenGraphAction action = shareOpenGraphContent.getAction();

        return OpenGraphJSONUtility.toJSONObject(
                action,
                new OpenGraphJSONUtility.PhotoJSONProcessor() {
                    @Override
                    public JSONObject toJSONObject(SharePhoto photo) {
                        Uri photoUri = photo.getImageUrl();
                        JSONObject photoJSONObject = new JSONObject();
                        try {
                            photoJSONObject.put(
                                    NativeProtocol.IMAGE_URL_KEY, photoUri.toString());
                        } catch (JSONException e) {
                            throw new FacebookException("Unable to attach images", e);
                        }
                        return photoJSONObject;
                    }
                });
    }

    public static JSONArray removeNamespacesFromOGJsonArray(
            JSONArray jsonArray,
            boolean requireNamespace) throws JSONException {
        JSONArray newArray = new JSONArray();
        for (int i = 0; i < jsonArray.length(); ++i) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONArray) {
                value = removeNamespacesFromOGJsonArray((JSONArray) value, requireNamespace);
            } else if (value instanceof JSONObject) {
                value = removeNamespacesFromOGJsonObject((JSONObject) value, requireNamespace);
            }
            newArray.put(value);
        }

        return newArray;
    }

    public static JSONObject removeNamespacesFromOGJsonObject(
            JSONObject jsonObject,
            boolean requireNamespace) {
        if (jsonObject == null) {
            return null;
        }

        try {
            JSONObject newJsonObject = new JSONObject();
            JSONObject data = new JSONObject();
            JSONArray names = jsonObject.names();
            for (int i = 0; i < names.length(); ++i) {
                String key = names.getString(i);
                Object value = null;
                value = jsonObject.get(key);
                if (value instanceof JSONObject) {
                    value = removeNamespacesFromOGJsonObject((JSONObject) value, true);
                } else if (value instanceof JSONArray) {
                    value = removeNamespacesFromOGJsonArray((JSONArray) value, true);
                }

                Pair<String, String> fieldNameAndNamespace = getFieldNameAndNamespaceFromFullName(
                        key);
                String namespace = fieldNameAndNamespace.first;
                String fieldName = fieldNameAndNamespace.second;

                if (requireNamespace) {
                    if (namespace != null && namespace.equals("fbsdk")) {
                        newJsonObject.put(key, value);
                    } else if (namespace == null || namespace.equals("og")) {
                        newJsonObject.put(fieldName, value);
                    } else {
                        data.put(fieldName, value);
                    }
                } else {
                    newJsonObject.put(fieldName, value);
                }
            }

            if (data.length() > 0) {
                newJsonObject.put("data", data);
            }

            return newJsonObject;
        } catch (JSONException e) {
            throw new FacebookException("Failed to create json object from share content");
        }
    }

    public static Pair<String, String> getFieldNameAndNamespaceFromFullName(String fullName) {
        String namespace = null;
        String fieldName;
        int index = fullName.indexOf(':');
        if (index != -1 && fullName.length() > index + 1) {
            namespace = fullName.substring(0, index);
            fieldName = fullName.substring(index + 1);
        } else {
            fieldName = fullName;
        }
        return new Pair<>(namespace, fieldName);
    }

    ;

    private ShareInternalUtility() {
    }

    private static NativeAppCallAttachmentStore.Attachment getAttachment(
            UUID callId,
            SharePhoto photo) {
        Bitmap bitmap = photo.getBitmap();
        Uri photoUri = photo.getImageUrl();
        NativeAppCallAttachmentStore.Attachment attachment = null;
        if (bitmap != null) {
            attachment = NativeAppCallAttachmentStore.createAttachment(
                    callId,
                    bitmap);
        } else if (photoUri != null) {
            attachment = NativeAppCallAttachmentStore.createAttachment(
                    callId,
                    photoUri);
        }

        return attachment;
    }

    private static void invokeOnCancelCallback(FacebookCallback<Sharer.Result> callback) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_CANCELLED, null);
        if (callback != null) {
            callback.onCancel();
        }
    }

    private static void invokeOnSuccessCallback(
            FacebookCallback<Sharer.Result> callback,
            String postId) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_SUCCEEDED, null);
        if (callback != null) {
            callback.onSuccess(new Sharer.Result(postId));
        }
    }

    private static void invokeOnErrorCallback(
            FacebookCallback<Sharer.Result> callback,
            GraphResponse response,
            String message) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, message);
        if (callback != null) {
            callback.onError(new FacebookGraphResponseException(response, message));
        }
    }


    private static void invokeOnErrorCallback(
            FacebookCallback<Sharer.Result> callback,
            String message) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, message);
        if (callback != null) {
            callback.onError(new FacebookException(message));
        }
    }

    private static void invokeOnErrorCallback(
            FacebookCallback<Sharer.Result> callback,
            FacebookException ex) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, ex.getMessage());
        if (callback != null) {
            callback.onError(ex);
        }
    }

    private static void logShareResult(String shareOutcome, String errorMessage) {
        Context context = FacebookSdk.getApplicationContext();
        AppEventsLogger logger = AppEventsLogger.newLogger(context);
        Bundle parameters = new Bundle();
        parameters.putString(
                AnalyticsEvents.PARAMETER_SHARE_OUTCOME,
                shareOutcome
        );

        if (errorMessage != null) {
            parameters.putString(AnalyticsEvents.PARAMETER_SHARE_ERROR_MESSAGE, errorMessage);
        }
        logger.logSdkEvent(AnalyticsEvents.EVENT_SHARE_RESULT, null, parameters);
    }

    /**
     * Creates a new Request configured to create a user owned Open Graph object.
     *
     * @param accessToken     the accessToken to use, or null
     * @param openGraphObject the Open Graph object to create; must not be null, and must have a
     *                        non-empty type and title
     * @param callback        a callback that will be called when the request is completed to handle
     *                        success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newPostOpenGraphObjectRequest(
            AccessToken accessToken,
            JSONObject openGraphObject,
            Callback callback) {
        if (openGraphObject == null) {
            throw new FacebookException("openGraphObject cannot be null");
        }
        if (Utility.isNullOrEmpty(openGraphObject.optString("type"))) {
            throw new FacebookException("openGraphObject must have non-null 'type' property");
        }
        if (Utility.isNullOrEmpty(openGraphObject.optString("title"))) {
            throw new FacebookException("openGraphObject must have non-null 'title' property");
        }

        String path = String.format(MY_OBJECTS_FORMAT, openGraphObject.optString("type"));
        Bundle bundle = new Bundle();
        bundle.putString(OBJECT_PARAM, openGraphObject.toString());
        return new GraphRequest(accessToken, path, bundle, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to create a user owned Open Graph object.
     *
     * @param accessToken      the access token to use, or null
     * @param type             the fully-specified Open Graph object type (e.g.,
     *                         my_app_namespace:my_object_name); must not be null
     * @param title            the title of the Open Graph object; must not be null
     * @param imageUrl         the link to an image to be associated with the Open Graph object; may
     *                         be null
     * @param url              the url to be associated with the Open Graph object; may be null
     * @param description      the description to be associated with the object; may be null
     * @param objectProperties any additional type-specific properties for the Open Graph object;
     *                         may be null
     * @param callback         a callback that will be called when the request is completed to
     *                         handle success or error conditions; may be null
     * @return a Request that is ready to execute
     */
    public static GraphRequest newPostOpenGraphObjectRequest(
            AccessToken accessToken,
            String type,
            String title,
            String imageUrl,
            String url,
            String description,
            JSONObject objectProperties,
            Callback callback) {
        JSONObject openGraphObject = GraphUtil.createOpenGraphObjectForPost(
                type, title, imageUrl, url, description, objectProperties, null);
        return newPostOpenGraphObjectRequest(accessToken, openGraphObject, callback);
    }

    /**
     * Creates a new Request configured to publish an Open Graph action.
     *
     * @param accessToken     the access token to use, or null
     * @param openGraphAction the Open Graph action to create; must not be null, and must have a
     *                        non-empty 'type'
     * @param callback        a callback that will be called when the request is completed to handle
     *                        success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newPostOpenGraphActionRequest(
            AccessToken accessToken,
            JSONObject openGraphAction,
            Callback callback) {
        if (openGraphAction == null) {
            throw new FacebookException("openGraphAction cannot be null");
        }
        String type = openGraphAction.optString("type");
        if (Utility.isNullOrEmpty(type)) {
            throw new FacebookException("openGraphAction must have non-null 'type' property");
        }

        String path = String.format(MY_ACTION_FORMAT, type);
        return GraphRequest.newPostRequest(accessToken, path, openGraphAction, callback);
    }

    /**
     * Creates a new Request configured to update a user owned Open Graph object.
     *
     * @param accessToken     the access token to use, or null
     * @param openGraphObject the Open Graph object to update, which must have a valid 'id'
     *                        property
     * @param callback        a callback that will be called when the request is completed to handle
     *                        success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newUpdateOpenGraphObjectRequest(
            AccessToken accessToken,
            JSONObject openGraphObject,
            Callback callback) {
        if (openGraphObject == null) {
            throw new FacebookException("openGraphObject cannot be null");
        }

        String path = openGraphObject.optString("id");
        if (path == null) {
            throw new FacebookException("openGraphObject must have an id");
        }

        Bundle bundle = new Bundle();
        bundle.putString(OBJECT_PARAM, openGraphObject.toString());
        return new GraphRequest(accessToken, path, bundle, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to update a user owned Open Graph object.
     *
     * @param accessToken      the access token to use, or null
     * @param id               the id of the Open Graph object
     * @param title            the title of the Open Graph object
     * @param imageUrl         the link to an image to be associated with the Open Graph object
     * @param url              the url to be associated with the Open Graph object
     * @param description      the description to be associated with the object
     * @param objectProperties any additional type-specific properties for the Open Graph object
     * @param callback         a callback that will be called when the request is completed to
     *                         handle success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newUpdateOpenGraphObjectRequest(
            AccessToken accessToken,
            String id,
            String title,
            String imageUrl,
            String url,
            String description,
            JSONObject objectProperties,
            Callback callback) {
        JSONObject openGraphObject = GraphUtil.createOpenGraphObjectForPost(
                null, title, imageUrl, url, description, objectProperties, id);
        return newUpdateOpenGraphObjectRequest(accessToken, openGraphObject, callback);
    }

    /**
     * Creates a new Request configured to upload a photo to the user's default photo album.
     *
     * @param accessToken the access token to use, or null
     * @param image       the image to upload
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newUploadPhotoRequest(
            AccessToken accessToken,
            Bitmap image,
            Callback callback) {
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(PICTURE_PARAM, image);

        return new GraphRequest(accessToken, MY_PHOTOS, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to upload a photo to the user's default photo album. The
     * photo will be read from the specified file.
     *
     * @param accessToken the access token to use, or null
     * @param file        the file containing the photo to upload
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     * @throws java.io.FileNotFoundException
     */
    public static GraphRequest newUploadPhotoRequest(
            AccessToken accessToken,
            File file,
            Callback callback
    ) throws FileNotFoundException {
        ParcelFileDescriptor descriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(PICTURE_PARAM, descriptor);

        return new GraphRequest(accessToken, MY_PHOTOS, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to upload a photo to the user's default photo album. The
     * photo will be read from the specified Uri.

     * @param accessToken the access token to use, or null
     * @param photoUri    the file:// or content:// Uri to the photo on device.
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     * @throws FileNotFoundException
     */
    public static GraphRequest newUploadPhotoRequest(
            AccessToken accessToken,
            Uri photoUri,
            Callback callback)
            throws FileNotFoundException {
        if (Utility.isFileUri(photoUri)) {
            return newUploadPhotoRequest(accessToken, new File(photoUri.getPath()), callback);
        } else if (!Utility.isContentUri(photoUri)) {
            throw new FacebookException("The photo Uri must be either a file:// or content:// Uri");
        }

        Bundle parameters = new Bundle(1);
        parameters.putParcelable(PICTURE_PARAM, photoUri);

        return new GraphRequest(accessToken, MY_PHOTOS, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to upload a video to the user's default video album. The
     * video will be read from the specified file.
     *
     * @param accessToken the access token to use, or null
     * @param file        the file to upload
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     * @throws FileNotFoundException
     */
    public static GraphRequest newUploadVideoRequest(
            AccessToken accessToken,
            File file,
            Callback callback
    ) throws FileNotFoundException {
        ParcelFileDescriptor descriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(file.getName(), descriptor);

        return new GraphRequest(accessToken, MY_VIDEOS, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to upload a photo to the user's default photo album. The
     * photo will be read from the specified Uri.

     * @param accessToken the access token to use, or null
     * @param videoUri    the file:// or content:// Uri to the video on device.
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     * @throws FileNotFoundException
     */
    public static GraphRequest newUploadVideoRequest(
            AccessToken accessToken,
            Uri videoUri,
            Callback callback)
            throws FileNotFoundException {
        if (Utility.isFileUri(videoUri)) {
            return newUploadVideoRequest(accessToken, new File(videoUri.getPath()), callback);
        } else if (!Utility.isContentUri(videoUri)) {
            throw new FacebookException("The video Uri must be either a file:// or content:// Uri");
        }

        // We need to pass the file name to the graph api endpoint.
        Cursor cursor = FacebookSdk
                .getApplicationContext()
                .getContentResolver()
                .query(videoUri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

        cursor.moveToFirst();
        String fileName = cursor.getString(nameIndex);
        cursor.close();

        Bundle parameters = new Bundle(1);
        parameters.putParcelable(fileName, videoUri);

        return new GraphRequest(accessToken, MY_VIDEOS, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to post a status update to a user's feed.
     *
     * @param accessToken the access token to use, or null
     * @param message     the text of the status update
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newStatusUpdateRequest(
            AccessToken accessToken,
            String message,
            Callback callback) {
        return newStatusUpdateRequest(accessToken, message, (String)null, null, callback);
    }

    /**
     * Creates a new Request configured to post a status update to a user's feed.
     *
     * @param accessToken the access token to use, or null
     * @param message     the text of the status update
     * @param placeId     an optional place id to associate with the post
     * @param tagIds      an optional list of user ids to tag in the post
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    private static GraphRequest newStatusUpdateRequest(
            AccessToken accessToken,
            String message,
            String placeId,
            List<String> tagIds,
            Callback callback) {

        Bundle parameters = new Bundle();
        parameters.putString("message", message);

        if (placeId != null) {
            parameters.putString("place", placeId);
        }

        if (tagIds != null && tagIds.size() > 0) {
            String tags = TextUtils.join(",", tagIds);
            parameters.putString("tags", tags);
        }

        return new GraphRequest(accessToken, MY_FEED, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to post a status update to a user's feed.
     *
     * @param accessToken the access token to use, or null
     * @param message     the text of the status update
     * @param place       an optional place to associate with the post
     * @param tags        an optional list of users to tag in the post
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newStatusUpdateRequest(
            AccessToken accessToken,
            String message,
            JSONObject place,
            List<JSONObject> tags,
            Callback callback) {

        List<String> tagIds = null;
        if (tags != null) {
            tagIds = new ArrayList<String>(tags.size());
            for (JSONObject tag: tags) {
                tagIds.add(tag.optString("id"));
            }
        }
        String placeId = place == null ? null : place.optString("id");
        return newStatusUpdateRequest(accessToken, message, placeId, tagIds, callback);
    }



    /**
     * Creates a new Request configured to upload an image to create a staging resource. Staging
     * resources allow you to post binary data such as images, in preparation for a post of an Open
     * Graph object or action which references the image. The URI returned when uploading a staging
     * resource may be passed as the image property for an Open Graph object or action.
     *
     * @param accessToken the access token to use, or null
     * @param image       the image to upload
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newUploadStagingResourceWithImageRequest(
            AccessToken accessToken,
            Bitmap image,
            Callback callback) {
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(STAGING_PARAM, image);

        return new GraphRequest(
                accessToken,
                MY_STAGING_RESOURCES,
                parameters,
                HttpMethod.POST,
                callback);
    }

    /**
     * Creates a new Request configured to upload an image to create a staging resource. Staging
     * resources allow you to post binary data such as images, in preparation for a post of an Open
     * Graph object or action which references the image. The URI returned when uploading a staging
     * resource may be passed as the image property for an Open Graph object or action.
     *
     * @param accessToken the access token to use, or null
     * @param file        the file containing the image to upload
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     * @throws FileNotFoundException
     */
    public static GraphRequest newUploadStagingResourceWithImageRequest(
            AccessToken accessToken,
            File file,
            Callback callback
    ) throws FileNotFoundException {
        ParcelFileDescriptor descriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        GraphRequest.ParcelableResourceWithMimeType<ParcelFileDescriptor> resourceWithMimeType =
                new GraphRequest.ParcelableResourceWithMimeType<>(descriptor, "image/png");
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(STAGING_PARAM, resourceWithMimeType);

        return new GraphRequest(
                accessToken,
                MY_STAGING_RESOURCES,
                parameters,
                HttpMethod.POST,
                callback);
    }

    /**
     * Creates a new Request configured to upload an image to create a staging resource. Staging
     * resources allow you to post binary data such as images, in preparation for a post of an Open
     * Graph object or action which references the image. The URI returned when uploading a staging
     * resource may be passed as the image property for an Open Graph object or action.
     *
     * @param accessToken the access token to use, or null
     * @param imageUri    the file:// or content:// Uri pointing to the image to upload
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     * @throws FileNotFoundException
     */
    public static GraphRequest newUploadStagingResourceWithImageRequest(
            AccessToken accessToken,
            Uri imageUri,
            Callback callback
    ) throws FileNotFoundException {
        if (Utility.isFileUri(imageUri)) {
            return newUploadStagingResourceWithImageRequest(
                    accessToken,
                    new File(imageUri.getPath()),
                    callback);
        } else if (!Utility.isContentUri(imageUri)) {
            throw new FacebookException("The image Uri must be either a file:// or content:// Uri");
        }

        GraphRequest.ParcelableResourceWithMimeType<Uri> resourceWithMimeType =
                new GraphRequest.ParcelableResourceWithMimeType<>(imageUri, "image/png");
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(STAGING_PARAM, resourceWithMimeType);

        return new GraphRequest(
                accessToken,
                MY_STAGING_RESOURCES,
                parameters,
                HttpMethod.POST,
                callback);
    }

    @Nullable
    public static LikeView.ObjectType getMostSpecificObjectType(
            LikeView.ObjectType objectType1,
            LikeView.ObjectType objectType2) {
        if (objectType1 == objectType2) {
            return objectType1;
        }

        if (objectType1 == LikeView.ObjectType.UNKNOWN) {
            return objectType2;
        } else if (objectType2 == LikeView.ObjectType.UNKNOWN) {
            return objectType1;
        } else {
            // We can't have a PAGE and an OPEN_GRAPH type be compatible.
            return null;
        }
    }
}
