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

package com.facebook.share;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookGraphResponseException;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.internal.CollectionMapper;
import com.facebook.internal.Mutable;
import com.facebook.internal.Utility;
import com.facebook.share.internal.ShareContentValidation;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.internal.VideoUploader;
import com.facebook.share.model.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Provides an interface for sharing through the graph API. Using this class requires an access
 * token in AccessToken.currentAccessToken that has been granted the "publish_actions" permission.
 */
public final class ShareApi {
    private static final String TAG = "ShareApi";
    private static final String DEFAULT_GRAPH_NODE = "me";
    private static final String PHOTOS_EDGE = "photos";
    private static final String GRAPH_PATH_FORMAT = "%s/%s";
    private static final String DEFAULT_CHARSET = "UTF-8";

    private String message;
    private String graphNode;
    private final ShareContent shareContent;

    /**
     * Convenience method to share a piece of content.
     *
     * @param shareContent the content to share.
     * @param callback     the callback to call once the share is complete.
     */
    public static void share(
            final ShareContent shareContent,
            final FacebookCallback<Sharer.Result> callback) {
        new ShareApi(shareContent)
                .share(callback);
    }

    /**
     * Constructs a new instance.
     *
     * @param shareContent the content to share.
     */
    public ShareApi(final ShareContent shareContent) {
        this.shareContent = shareContent;
        this.graphNode = DEFAULT_GRAPH_NODE;
    }

    /**
     * Returns the message the person has provided through the custom dialog that will accompany the
     * share content.
     * @return the message.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Sets the message the person has provided through the custom dialog that will accompany the
     * share content.
     * @param message the message.
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Returns the graph node to share to.
     * @return the graph node.
     */
    public String getGraphNode() {
        return this.graphNode;
    }

    /**
     * Sets the graph node to share to (this can be a user id, event id, page id, group id, album
     * id, etc).
     * @param graphNode the graph node to share to.
     */
    public void setGraphNode(final String graphNode) {
        this.graphNode = graphNode;
    }

    /**
     * Returns the content to be shared.
     *
     * @return the content to be shared.
     */
    public ShareContent getShareContent() {
        return this.shareContent;
    }

    /**
     * Returns true if the content can be shared. Warns if the access token is missing the
     * publish_actions permission. Doesn't fail when this permission is missing, because the app
     * could have been granted that permission in another installation.
     *
     * @return true if the content can be shared.
     */
    public boolean canShare() {
        if (this.getShareContent() == null) {
            return false;
        }
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) {
            return false;
        }
        final Set<String> permissions = accessToken.getPermissions();
        if (permissions == null || !permissions.contains("publish_actions")) {
            Log.w(TAG, "The publish_actions permissions are missing, the share will fail unless" +
                    " this app was authorized to publish in another installation.");
        }

        return true;
    }

    /**
     * Share the content.
     *
     * @param callback the callback to call once the share is complete.
     */
    public void share(FacebookCallback<Sharer.Result> callback) {
        if (!this.canShare()) {
            ShareInternalUtility.invokeCallbackWithError(
                    callback, "Insufficient permissions for sharing content via Api.");
            return;
        }
        final ShareContent shareContent = this.getShareContent();

        // Validate the share content
        try {
            ShareContentValidation.validateForApiShare(shareContent);
        } catch (FacebookException ex) {
            ShareInternalUtility.invokeCallbackWithException(callback, ex);
            return;
        }

        if (shareContent instanceof ShareLinkContent) {
            this.shareLinkContent((ShareLinkContent) shareContent, callback);
        } else if (shareContent instanceof SharePhotoContent) {
            this.sharePhotoContent((SharePhotoContent) shareContent, callback);
        } else if (shareContent instanceof ShareVideoContent) {
            this.shareVideoContent((ShareVideoContent) shareContent, callback);
        } else if (shareContent instanceof ShareOpenGraphContent) {
            this.shareOpenGraphContent((ShareOpenGraphContent) shareContent, callback);
        }
    }

    // Get the graph path, pathAfterGraphNode must be properly URL encoded
    private String getGraphPath(final String pathAfterGraphNode) {
        try {
            return String.format(
                    Locale.ROOT, GRAPH_PATH_FORMAT,
                    URLEncoder.encode(getGraphNode(), DEFAULT_CHARSET),
                    pathAfterGraphNode);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private void addCommonParameters(final Bundle bundle, ShareContent shareContent) {
        final List<String> peopleIds = shareContent.getPeopleIds();
        if (!Utility.isNullOrEmpty(peopleIds)) {
            bundle.putString("tags", TextUtils.join(", ", peopleIds));
        }

        if (!Utility.isNullOrEmpty(shareContent.getPlaceId())) {
            bundle.putString("place", shareContent.getPlaceId());
        }

        if (!Utility.isNullOrEmpty(shareContent.getRef())) {
            bundle.putString("ref", shareContent.getRef());
        }
    }

    private void shareOpenGraphContent(final ShareOpenGraphContent openGraphContent,
                                       final FacebookCallback<Sharer.Result> callback) {
        // In order to create a new Open Graph action using a custom object that does not already
        // exist (objectID or URL), you must first send a request to post the object and then
        // another to post the action.  If a local image is supplied with the object or action, that
        // must be staged first and then referenced by the staging URL that is returned by that
        // request.
        final GraphRequest.Callback requestCallback = new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                final JSONObject data = response.getJSONObject();
                final String postId = (data == null ? null : data.optString("id"));
                ShareInternalUtility.invokeCallbackWithResults(callback, postId, response);
            }
        };
        final ShareOpenGraphAction action = openGraphContent.getAction();
        final Bundle parameters = action.getBundle();
        this.addCommonParameters(parameters, openGraphContent);
        if (!Utility.isNullOrEmpty(this.getMessage())) {
            parameters.putString("message", this.getMessage());
        }

        final CollectionMapper.OnMapperCompleteListener stageCallback = new CollectionMapper
                .OnMapperCompleteListener() {
            @Override
            public void onComplete() {
                try {
                    handleImagesOnAction(parameters);

                    new GraphRequest(
                            AccessToken.getCurrentAccessToken(),
                            getGraphPath(
                                    URLEncoder.encode(action.getActionType(), DEFAULT_CHARSET)),
                            parameters,
                            HttpMethod.POST,
                            requestCallback).executeAsync();
                } catch (final UnsupportedEncodingException ex) {
                    ShareInternalUtility.invokeCallbackWithException(callback, ex);
                }
            }

            @Override
            public void onError(FacebookException exception) {
                ShareInternalUtility.invokeCallbackWithException(callback, exception);
            }
        };
        this.stageOpenGraphAction(parameters, stageCallback);
    }

    private static void handleImagesOnAction(Bundle parameters) {
        // In general, graph objects are passed by reference (ID/URL). But if this is an OG Action,
        // we need to pass the entire values of the contents of the 'image' property, as they
        // contain important metadata beyond just a URL.
        String imageStr = parameters.getString("image");
        if (imageStr != null) {
            try {
                // Check to see if this is an json array. Will throw if not
                JSONArray images = new JSONArray(imageStr);
                for (int i = 0; i < images.length(); ++i) {
                    JSONObject jsonImage = images.optJSONObject(i);
                    if(jsonImage != null) {
                        putImageInBundleWithArrayFormat(parameters, i, jsonImage);
                    } else {
                        // If we don't have jsonImage we probably just have a url
                        String url = images.getString(i);
                        parameters.putString(String.format(Locale.ROOT, "image[%d][url]", i), url);
                    }
                }
                parameters.remove("image");
                return;
            } catch (JSONException ex) {
                // We couldn't parse the string as an array
            }

            // If the image is not in an array it might just be an single photo
            try {
                JSONObject image = new JSONObject(imageStr);
                putImageInBundleWithArrayFormat(parameters, 0, image);
                parameters.remove("image");
            } catch (JSONException exception) {
                // The image was not in array format or a json object and can be safely passed
                // without modification
            }
        }
    }

    private static void putImageInBundleWithArrayFormat(
            Bundle parameters,
            int index,
            JSONObject image) throws JSONException{
        Iterator<String> keys = image.keys();
        while (keys.hasNext()) {
            String property = keys.next();
            String key = String.format(Locale.ROOT, "image[%d][%s]", index, property);
            parameters.putString(key, image.get(property).toString());
        }
    }

    private void sharePhotoContent(final SharePhotoContent photoContent,
                                   final FacebookCallback<Sharer.Result> callback) {
        final Mutable<Integer> requestCount = new Mutable<Integer>(0);
        final AccessToken accessToken = AccessToken.getCurrentAccessToken();
        final ArrayList<GraphRequest> requests = new ArrayList<GraphRequest>();
        final ArrayList<JSONObject> results = new ArrayList<JSONObject>();
        final ArrayList<GraphResponse> errorResponses = new ArrayList<GraphResponse>();
        final GraphRequest.Callback requestCallback = new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                final JSONObject result = response.getJSONObject();
                if (result != null) {
                    results.add(result);
                }
                if (response.getError() != null) {
                    errorResponses.add(response);
                }
                requestCount.value -= 1;
                if (requestCount.value == 0) {
                    if (!errorResponses.isEmpty()) {
                        ShareInternalUtility.invokeCallbackWithResults(
                                callback,
                                null,
                                errorResponses.get(0));
                    } else if (!results.isEmpty()) {
                        final String postId = results.get(0).optString("id");
                        ShareInternalUtility.invokeCallbackWithResults(
                                callback,
                                postId,
                                response);
                    }
                }
            }
        };
        try {
            for (SharePhoto photo : photoContent.getPhotos()) {
                final Bitmap bitmap = photo.getBitmap();
                final Uri photoUri = photo.getImageUrl();
                String caption = photo.getCaption();
                if (caption == null) {
                    caption = this.getMessage();
                }
                if (bitmap != null) {
                    requests.add(ShareInternalUtility.newUploadPhotoRequest(
                            getGraphPath(PHOTOS_EDGE),
                            accessToken,
                            bitmap,
                            caption,
                            photo.getParameters(),
                            requestCallback));
                } else if (photoUri != null) {
                    requests.add(ShareInternalUtility.newUploadPhotoRequest(
                            getGraphPath(PHOTOS_EDGE),
                            accessToken,
                            photoUri,
                            caption,
                            photo.getParameters(),
                            requestCallback));
                }
            }
            requestCount.value += requests.size();
            for (GraphRequest request : requests) {
                request.executeAsync();
            }
        } catch (final FileNotFoundException ex) {
            ShareInternalUtility.invokeCallbackWithException(callback, ex);
        }
    }

    private void shareLinkContent(final ShareLinkContent linkContent,
                                  final FacebookCallback<Sharer.Result> callback) {
        final GraphRequest.Callback requestCallback = new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                final JSONObject data = response.getJSONObject();
                final String postId = (data == null ? null : data.optString("id"));
                ShareInternalUtility.invokeCallbackWithResults(callback, postId, response);
            }
        };
        final Bundle parameters = new Bundle();
        this.addCommonParameters(parameters, linkContent);
        parameters.putString("message", this.getMessage());
        parameters.putString("link", Utility.getUriString(linkContent.getContentUrl()));
        parameters.putString("picture", Utility.getUriString(linkContent.getImageUrl()));
        parameters.putString("name", linkContent.getContentTitle());
        parameters.putString("description", linkContent.getContentDescription());
        parameters.putString("ref", linkContent.getRef());
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                getGraphPath("feed"),
                parameters,
                HttpMethod.POST,
                requestCallback).executeAsync();
    }

    private void shareVideoContent(final ShareVideoContent videoContent,
                                   final FacebookCallback<Sharer.Result> callback) {
        try {
            VideoUploader.uploadAsync(videoContent, getGraphNode(), callback);
        } catch (final FileNotFoundException ex) {
            ShareInternalUtility.invokeCallbackWithException(callback, ex);
        }
    }

    private void stageArrayList(final ArrayList arrayList,
                                       final CollectionMapper.OnMapValueCompleteListener
                                               onArrayListStagedListener) {
        final JSONArray stagedObject = new JSONArray();
        final CollectionMapper.Collection<Integer> collection = new CollectionMapper
                .Collection<Integer>() {
            @Override
            public Iterator<Integer> keyIterator() {
                final int size = arrayList.size();
                final Mutable<Integer> current = new Mutable<Integer>(0);
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return current.value < size;
                    }

                    @Override
                    public Integer next() {
                        return current.value++;
                    }

                    @Override
                    public void remove() {
                    }
                };
            }

            @Override
            public Object get(Integer key) {
                return arrayList.get(key);
            }

            @Override
            public void set(Integer key,
                            Object value,
                            CollectionMapper.OnErrorListener onErrorListener) {
                try {
                    stagedObject.put(key, value);
                } catch (final JSONException ex) {
                    String message = ex.getLocalizedMessage();
                    if (message == null) {
                        message = "Error staging object.";
                    }
                    onErrorListener.onError(new FacebookException(message));
                }
            }
        };
        final CollectionMapper.OnMapperCompleteListener onStagedArrayMapperCompleteListener =
                new CollectionMapper.OnMapperCompleteListener() {
                    @Override
                    public void onComplete() {
                        onArrayListStagedListener.onComplete(stagedObject);
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        onArrayListStagedListener.onError(exception);
                    }
                };
        stageCollectionValues(collection, onStagedArrayMapperCompleteListener);
    }

    private <T> void stageCollectionValues(final CollectionMapper.Collection<T> collection,
                                                  final CollectionMapper.OnMapperCompleteListener
                                                          onCollectionValuesStagedListener) {
        final CollectionMapper.ValueMapper valueMapper = new CollectionMapper.ValueMapper() {
            @Override
            public void mapValue(Object value,
                                 CollectionMapper.OnMapValueCompleteListener
                                         onMapValueCompleteListener) {
                if (value instanceof ArrayList) {
                    stageArrayList((ArrayList) value, onMapValueCompleteListener);
                } else if (value instanceof ShareOpenGraphObject) {
                    stageOpenGraphObject(
                            (ShareOpenGraphObject) value,
                            onMapValueCompleteListener);
                } else if (value instanceof SharePhoto) {
                    stagePhoto((SharePhoto) value, onMapValueCompleteListener);
                } else {
                    onMapValueCompleteListener.onComplete(value);
                }
            }
        };
        CollectionMapper.iterate(collection, valueMapper, onCollectionValuesStagedListener);
    }

    private void stageOpenGraphAction(final Bundle parameters,
                                             final CollectionMapper.OnMapperCompleteListener
                                                     onOpenGraphActionStagedListener) {
        final CollectionMapper.Collection<String> collection = new CollectionMapper
                .Collection<String>() {
            @Override
            public Iterator<String> keyIterator() {
                return parameters.keySet().iterator();
            }

            @Override
            public Object get(String key) {
                return parameters.get(key);
            }

            @Override
            public void set(String key,
                            Object value,
                            CollectionMapper.OnErrorListener onErrorListener) {
                if (!Utility.putJSONValueInBundle(parameters, key, value)) {
                    onErrorListener.onError(
                            new FacebookException("Unexpected value: " + value.toString()));
                }
            }
        };
        stageCollectionValues(collection, onOpenGraphActionStagedListener);
    }

    private void stageOpenGraphObject(final ShareOpenGraphObject object,
                                             final CollectionMapper.OnMapValueCompleteListener
                                                     onOpenGraphObjectStagedListener) {
        String type = object.getString("type");
        if (type == null) {
            type = object.getString("og:type");
        }

        if (type == null) {
            onOpenGraphObjectStagedListener.onError(
                    new FacebookException("Open Graph objects must contain a type value."));
            return;
        }
        final JSONObject stagedObject = new JSONObject();
        final CollectionMapper.Collection<String> collection = new CollectionMapper
                .Collection<String>() {
            @Override
            public Iterator<String> keyIterator() {
                return object.keySet().iterator();
            }

            @Override
            public Object get(String key) {
                return object.get(key);
            }

            @Override
            public void set(String key,
                            Object value,
                            CollectionMapper.OnErrorListener onErrorListener) {
                try {
                    stagedObject.put(key, value);
                } catch (final JSONException ex) {
                    String message = ex.getLocalizedMessage();
                    if (message == null) {
                        message = "Error staging object.";
                    }
                    onErrorListener.onError(new FacebookException(message));
                }
            }
        };
        final GraphRequest.Callback requestCallback = new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                final FacebookRequestError error = response.getError();
                if (error != null) {
                    String message = error.getErrorMessage();
                    if (message == null) {
                        message = "Error staging Open Graph object.";
                    }
                    onOpenGraphObjectStagedListener.onError(
                            new FacebookGraphResponseException(response, message));
                    return;
                }
                final JSONObject data = response.getJSONObject();
                if (data == null) {
                    onOpenGraphObjectStagedListener.onError(
                            new FacebookGraphResponseException(response,
                                    "Error staging Open Graph object."));
                    return;
                }
                final String stagedObjectId = data.optString("id");
                if (stagedObjectId == null) {
                    onOpenGraphObjectStagedListener.onError(
                            new FacebookGraphResponseException(response,
                                    "Error staging Open Graph object."));
                    return;
                }
                onOpenGraphObjectStagedListener.onComplete(stagedObjectId);
            }
        };
        final String ogType = type;
        final CollectionMapper.OnMapperCompleteListener onMapperCompleteListener =
                new CollectionMapper.OnMapperCompleteListener() {
                    @Override
                    public void onComplete() {
                        final String objectString = stagedObject.toString();
                        final Bundle parameters = new Bundle();
                        parameters.putString("object", objectString);
                        try {
                            new GraphRequest(
                                    AccessToken.getCurrentAccessToken(),
                                    getGraphPath(
                                            "objects/" +
                                                    URLEncoder.encode(ogType, DEFAULT_CHARSET)),
                                    parameters,
                                    HttpMethod.POST,
                                    requestCallback).executeAsync();
                        } catch (final UnsupportedEncodingException ex) {
                            String message = ex.getLocalizedMessage();
                            if (message == null) {
                                message = "Error staging Open Graph object.";
                            }
                            onOpenGraphObjectStagedListener.onError(new FacebookException(message));
                        }
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        onOpenGraphObjectStagedListener.onError(exception);
                    }
                };
        stageCollectionValues(collection, onMapperCompleteListener);
    }

    private void stagePhoto(final SharePhoto photo,
                                   final CollectionMapper.OnMapValueCompleteListener
                                           onPhotoStagedListener) {
        final Bitmap bitmap = photo.getBitmap();
        final Uri imageUrl = photo.getImageUrl();
        if ((bitmap != null) || (imageUrl != null)) {
            final GraphRequest.Callback requestCallback = new GraphRequest.Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    final FacebookRequestError error = response.getError();
                    if (error != null) {
                        String message = error.getErrorMessage();
                        if (message == null) {
                            message = "Error staging photo.";
                        }
                        onPhotoStagedListener.onError(
                                new FacebookGraphResponseException(response, message));
                        return;
                    }
                    final JSONObject data = response.getJSONObject();
                    if (data == null) {
                        onPhotoStagedListener.onError(
                                new FacebookException("Error staging photo."));
                        return;
                    }
                    final String stagedImageUri = data.optString("uri");
                    if (stagedImageUri == null) {
                        onPhotoStagedListener.onError(
                                new FacebookException("Error staging photo."));
                        return;
                    }

                    final JSONObject stagedObject = new JSONObject();
                    try {
                        stagedObject.put("url", stagedImageUri);
                        stagedObject.put("user_generated", photo.getUserGenerated());
                    } catch (final JSONException ex) {
                        String message = ex.getLocalizedMessage();
                        if (message == null) {
                            message = "Error staging photo.";
                        }
                        onPhotoStagedListener.onError(new FacebookException(message));
                        return;
                    }
                    onPhotoStagedListener.onComplete(stagedObject);
                }
            };
            if (bitmap != null) {
                ShareInternalUtility.newUploadStagingResourceWithImageRequest(
                        AccessToken.getCurrentAccessToken(),
                        bitmap,
                        requestCallback).executeAsync();
            } else {
                try {
                    ShareInternalUtility.newUploadStagingResourceWithImageRequest(
                            AccessToken.getCurrentAccessToken(),
                            imageUrl,
                            requestCallback).executeAsync();
                } catch (final FileNotFoundException ex) {
                    String message = ex.getLocalizedMessage();
                    if (message == null) {
                        message = "Error staging photo.";
                    }
                    onPhotoStagedListener.onError(new FacebookException(message));
                }
            }
        } else {
            onPhotoStagedListener.onError(
                    new FacebookException("Photos must have an imageURL or bitmap."));
        }
    }
}
