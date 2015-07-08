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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
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
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.LikeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public final class ShareInternalUtility {
    public static final String MY_PHOTOS = "me/photos";
    private static final String MY_STAGING_RESOURCES = "me/staging_resources";

    // Parameter names/values
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

    public static String getVideoUrl(final ShareVideoContent videoContent, final UUID appCallId) {
        if (videoContent == null || videoContent.getVideo() == null) {
            return null;
        }

        NativeAppCallAttachmentStore.Attachment attachment =
                NativeAppCallAttachmentStore.createAttachment(
                        appCallId,
                        videoContent.getVideo().getLocalUrl());

        ArrayList<NativeAppCallAttachmentStore.Attachment> attachments = new ArrayList<>(1);
        attachments.add(attachment);
        NativeAppCallAttachmentStore.addAttachments(attachments);

        return attachment.getAttachmentUrl();
    }

    public static JSONObject toJSONObjectForCall(
            final UUID callId,
            final ShareOpenGraphContent content)
            throws JSONException {
        final ShareOpenGraphAction action = content.getAction();
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
        // People and place tags must be moved from the share content to the open graph action
        if (content.getPlaceId() != null) {
            String placeTag = actionJSON.optString("place");

            // Only if the place tag is already empty or null replace with the id from the
            // share content
            if (Utility.isNullOrEmpty(placeTag)) {
                actionJSON.put("place", content.getPlaceId());
            }
        }

        if (content.getPeopleIds() != null) {
            JSONArray peopleTags = actionJSON.optJSONArray("tags");
            Set<String> peopleIdSet = peopleTags == null
                    ? new HashSet<String>()
                    : Utility.jsonArrayToSet(peopleTags);

            for (String peopleId : content.getPeopleIds()) {
                peopleIdSet.add(peopleId);
            }
            actionJSON.put("tags", new ArrayList<>(peopleIdSet));
        }

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
                } else if (namespace != null && namespace.equals("fb")) {
                    newJsonObject.put(key, value);
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

    static void invokeOnCancelCallback(FacebookCallback<Sharer.Result> callback) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_CANCELLED, null);
        if (callback != null) {
            callback.onCancel();
        }
    }

    static void invokeOnSuccessCallback(
            FacebookCallback<Sharer.Result> callback,
            String postId) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_SUCCEEDED, null);
        if (callback != null) {
            callback.onSuccess(new Sharer.Result(postId));
        }
    }

    static void invokeOnErrorCallback(
            FacebookCallback<Sharer.Result> callback,
            GraphResponse response,
            String message) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, message);
        if (callback != null) {
            callback.onError(new FacebookGraphResponseException(response, message));
        }
    }


    static void invokeOnErrorCallback(
            FacebookCallback<Sharer.Result> callback,
            String message) {
        logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, message);
        if (callback != null) {
            callback.onError(new FacebookException(message));
        }
    }

    static void invokeOnErrorCallback(
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
