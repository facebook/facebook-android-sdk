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

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.*;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.AppCall;
import com.facebook.internal.BundleJSONConverter;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FileLruCache;
import com.facebook.internal.Logger;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.internal.WorkQueue;
import com.facebook.share.widget.LikeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class LikeActionController {

    public static final String ACTION_LIKE_ACTION_CONTROLLER_UPDATED =
            "com.facebook.sdk.LikeActionController.UPDATED";
    public static final String ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR =
            "com.facebook.sdk.LikeActionController.DID_ERROR";
    public static final String ACTION_LIKE_ACTION_CONTROLLER_DID_RESET =
            "com.facebook.sdk.LikeActionController.DID_RESET";

    public static final String ACTION_OBJECT_ID_KEY =
            "com.facebook.sdk.LikeActionController.OBJECT_ID";

    public static final String ERROR_INVALID_OBJECT_ID = "Invalid Object Id";
    public static final String ERROR_PUBLISH_ERROR = "Unable to publish the like/unlike action";

    private static final String TAG = LikeActionController.class.getSimpleName();

    private static final int LIKE_ACTION_CONTROLLER_VERSION = 3;
    private static final int MAX_CACHE_SIZE = 128;
    // MAX_OBJECT_SUFFIX basically accommodates for 1000 access token changes before the async
    // disk-cache-clear finishes. The value is reasonably arbitrary.
    private static final int MAX_OBJECT_SUFFIX = 1000;

    private static final String LIKE_ACTION_CONTROLLER_STORE =
            "com.facebook.LikeActionController.CONTROLLER_STORE_KEY";
    private static final String LIKE_ACTION_CONTROLLER_STORE_PENDING_OBJECT_ID_KEY =
            "PENDING_CONTROLLER_KEY";
    private static final String LIKE_ACTION_CONTROLLER_STORE_OBJECT_SUFFIX_KEY = "OBJECT_SUFFIX";

    private static final String JSON_INT_VERSION_KEY =
            "com.facebook.share.internal.LikeActionController.version";
    private static final String JSON_STRING_OBJECT_ID_KEY = "object_id";
    private static final String JSON_INT_OBJECT_TYPE_KEY = "object_type";
    private static final String JSON_STRING_LIKE_COUNT_WITH_LIKE_KEY =
            "like_count_string_with_like";
    private static final String JSON_STRING_LIKE_COUNT_WITHOUT_LIKE_KEY =
            "like_count_string_without_like";
    private static final String JSON_STRING_SOCIAL_SENTENCE_WITH_LIKE_KEY =
            "social_sentence_with_like";
    private static final String JSON_STRING_SOCIAL_SENTENCE_WITHOUT_LIKE_KEY =
            "social_sentence_without_like";
    private static final String JSON_BOOL_IS_OBJECT_LIKED_KEY = "is_object_liked";
    private static final String JSON_STRING_UNLIKE_TOKEN_KEY = "unlike_token";
    private static final String JSON_BUNDLE_FACEBOOK_DIALOG_ANALYTICS_BUNDLE =
            "facebook_dialog_analytics_bundle";

    private static final String LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY = "object_is_liked";
    private static final String LIKE_DIALOG_RESPONSE_LIKE_COUNT_STRING_KEY = "like_count_string";
    private static final String LIKE_DIALOG_RESPONSE_SOCIAL_SENTENCE_KEY = "social_sentence";
    private static final String LIKE_DIALOG_RESPONSE_UNLIKE_TOKEN_KEY = "unlike_token";

    private static final int ERROR_CODE_OBJECT_ALREADY_LIKED = 3501;

    private static FileLruCache controllerDiskCache;
    private static final ConcurrentHashMap<String, LikeActionController> cache =
            new ConcurrentHashMap<>();

    // This MUST be 1 for proper synchronization
    private static WorkQueue mruCacheWorkQueue = new WorkQueue(1);
    // This MUST be 1 for proper synchronization
    private static WorkQueue diskIOWorkQueue = new WorkQueue(1);

    private static Handler handler;
    private static String objectIdForPendingController;
    private static boolean isInitialized;
    private static volatile int objectSuffix;
    private static AccessTokenTracker accessTokenTracker;

    private String objectId;
    private LikeView.ObjectType objectType;
    private boolean isObjectLiked;
    private String likeCountStringWithLike;
    private String likeCountStringWithoutLike;
    private String socialSentenceWithLike;
    private String socialSentenceWithoutLike;
    private String unlikeToken;

    private String verifiedObjectId;
    private boolean objectIsPage;
    private boolean isObjectLikedOnServer;

    private boolean isPendingLikeOrUnlike;

    private Bundle facebookDialogAnalyticsBundle;

    private AppEventsLogger appEventsLogger;

    /**
     * Called from CallbackManager to process any pending likes that had resulted in the Like
     * dialog being displayed
     *
     * @param requestCode From the originating call to onActivityResult
     * @param resultCode  From the originating call to onActivityResult
     * @param data        From the originating call to onActivityResult
     * @return Indication of whether the Intent was handled
     */
    public static boolean handleOnActivityResult(final int requestCode,
                                                 final int resultCode,
                                                 final Intent data) {
        // See if we were waiting on a Like dialog completion.
        if (Utility.isNullOrEmpty(objectIdForPendingController)) {
            Context appContext = FacebookSdk.getApplicationContext();
            SharedPreferences sharedPreferences = appContext.getSharedPreferences(
                    LIKE_ACTION_CONTROLLER_STORE,
                    Context.MODE_PRIVATE);

            objectIdForPendingController = sharedPreferences.getString(
                    LIKE_ACTION_CONTROLLER_STORE_PENDING_OBJECT_ID_KEY,
                    null);
        }

        if (Utility.isNullOrEmpty(objectIdForPendingController)) {
            // Doesn't look like we were waiting on a Like dialog completion
            return false;
        }

        getControllerForObjectId(
                objectIdForPendingController,
                LikeView.ObjectType.UNKNOWN,
                new CreationCallback() {
                    @Override
                    public void onComplete(
                            LikeActionController likeActionController,
                            FacebookException error) {
                        if (error == null) {
                            likeActionController.onActivityResult(
                                    requestCode,
                                    resultCode,
                                    data);
                        } else {
                            Utility.logd(TAG, error);
                        }
                    }
                });

        return true;
    }

    /**
     * Called by the LikeView when an object-id is set on it.
     *
     * @param objectId Object Id
     * @param callback Callback to be invoked when the LikeActionController has been created.
     */
    public static void getControllerForObjectId(
            String objectId,
            LikeView.ObjectType objectType,
            CreationCallback callback) {
        if (!isInitialized) {
            performFirstInitialize();
        }

        LikeActionController controllerForObject = getControllerFromInMemoryCache(objectId);
        if (controllerForObject != null) {
            // Direct object-cache hit
            verifyControllerAndInvokeCallback(controllerForObject, objectType, callback);
        } else {
            diskIOWorkQueue.addActiveWorkItem(
                    new CreateLikeActionControllerWorkItem(objectId, objectType, callback));
        }
    }

    private static void verifyControllerAndInvokeCallback(
            LikeActionController likeActionController,
            LikeView.ObjectType objectType,
            CreationCallback callback) {
        LikeView.ObjectType bestObjectType = ShareInternalUtility.getMostSpecificObjectType(
                objectType,
                likeActionController.objectType);
        FacebookException error = null;
        if (bestObjectType == null) {
            // Looks like the existing controller has an object_type for this object_id that is
            // not compatible with the requested object type.
            error = new FacebookException(
                    "Object with id:\"%s\" is already marked as type:\"%s\". " +
                            "Cannot change the type to:\"%s\"",
                    likeActionController.objectId,
                    likeActionController.objectType.toString(),
                    objectType.toString());
            likeActionController = null;
        } else {
            likeActionController.objectType = bestObjectType;
        }

        invokeCallbackWithController(callback, likeActionController, error);
    }

    /**
     * NOTE: This MUST be called ONLY via the CreateLikeActionControllerWorkItem class to ensure
     * that it happens on the right thread, at the right time.
     */
    private static void createControllerForObjectIdAndType(
            String objectId,
            LikeView.ObjectType objectType,
            CreationCallback callback) {
        // Check again to see if the controller was created before attempting to deserialize/create
        // one. Need to check this in the case where multiple LikeViews are looking for a controller
        // for the same object and all got queued up to create one. We only want the first one to go
        // through with the creation, and the rest should get the same instance from the
        // object-cache.
        LikeActionController controllerForObject = getControllerFromInMemoryCache(objectId);
        if (controllerForObject != null) {
            // Direct object-cache hit
            verifyControllerAndInvokeCallback(controllerForObject, objectType, callback);
            return;
        }

        // Try deserialize from disk
        controllerForObject = deserializeFromDiskSynchronously(objectId);

        if (controllerForObject == null) {
            controllerForObject = new LikeActionController(objectId, objectType);
            serializeToDiskAsync(controllerForObject);
        }

        // Update object-cache.
        putControllerInMemoryCache(objectId, controllerForObject);

        // Refresh the controller on the Main thread.
        final LikeActionController controllerToRefresh = controllerForObject;
        handler.post(new Runnable() {
            @Override
            public void run() {
                controllerToRefresh.refreshStatusAsync();
            }
        });

        invokeCallbackWithController(callback, controllerToRefresh, null);
    }

    private synchronized static void performFirstInitialize() {
        if (isInitialized) {
            return;
        }

        handler = new Handler(Looper.getMainLooper());

        Context appContext = FacebookSdk.getApplicationContext();
        SharedPreferences sharedPreferences = appContext.getSharedPreferences(
                LIKE_ACTION_CONTROLLER_STORE,
                Context.MODE_PRIVATE);

        objectSuffix = sharedPreferences.getInt(LIKE_ACTION_CONTROLLER_STORE_OBJECT_SUFFIX_KEY, 1);
        controllerDiskCache = new FileLruCache(TAG, new FileLruCache.Limits());

        registerAccessTokenTracker();

        CallbackManagerImpl.registerStaticCallback(
                CallbackManagerImpl.RequestCodeOffset.Like.toRequestCode(),
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        return handleOnActivityResult(
                                CallbackManagerImpl.RequestCodeOffset.Like.toRequestCode(),
                                resultCode,
                                data);
                    }
                });

        isInitialized = true;
    }

    private static void invokeCallbackWithController(
            final CreationCallback callback,
            final LikeActionController controller,
            final FacebookException error) {
        if (callback == null) {
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onComplete(controller, error);
            }
        });
    }

    //
    // In-memory mru-caching code
    //

    private static void registerAccessTokenTracker() {
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                Context appContext = FacebookSdk.getApplicationContext();
                if (currentAccessToken == null) {
                    // Bump up the objectSuffix so that we don't have a filename collision between a
                    // cache-clear and and a cache-read/write.
                    //
                    // NOTE: We know that onReceive() was called on the main thread. This means that
                    // even this code is running on the main thread, and therefore, there aren't
                    // synchronization issues with incrementing the objectSuffix and clearing the
                    // caches here.
                    objectSuffix = (objectSuffix + 1) % MAX_OBJECT_SUFFIX;
                    appContext.getSharedPreferences(
                            LIKE_ACTION_CONTROLLER_STORE,
                            Context.MODE_PRIVATE)
                            .edit()
                            .putInt(LIKE_ACTION_CONTROLLER_STORE_OBJECT_SUFFIX_KEY, objectSuffix)
                            .apply();

                    // Only clearing the actual caches. The MRU index will self-clean with usage.
                    // Clearing the caches is necessary to prevent leaking like-state across
                    // users.
                    cache.clear();
                    controllerDiskCache.clearCache();
                }
                broadcastAction(null, ACTION_LIKE_ACTION_CONTROLLER_DID_RESET);
            }
        };
    }

    private static void putControllerInMemoryCache(
            String objectId,
            LikeActionController controllerForObject) {
        String cacheKey = getCacheKeyForObjectId(objectId);
        // Move this object to the front. Also trim cache if necessary
        mruCacheWorkQueue.addActiveWorkItem(new MRUCacheWorkItem(cacheKey, true));

        cache.put(cacheKey, controllerForObject);
    }

    private static LikeActionController getControllerFromInMemoryCache(String objectId) {
        String cacheKey = getCacheKeyForObjectId(objectId);

        LikeActionController controller = cache.get(cacheKey);
        if (controller != null) {
            // Move this object to the front
            mruCacheWorkQueue.addActiveWorkItem(new MRUCacheWorkItem(cacheKey, false));
        }

        return controller;
    }

    //
    // Disk caching code
    //

    private static void serializeToDiskAsync(LikeActionController controller) {
        String controllerJson = serializeToJson(controller);
        String cacheKey = getCacheKeyForObjectId(controller.objectId);

        if (!Utility.isNullOrEmpty(controllerJson) && !Utility.isNullOrEmpty(cacheKey)) {
            diskIOWorkQueue.addActiveWorkItem(
                    new SerializeToDiskWorkItem(cacheKey, controllerJson));
        }
    }

    /**
     * NOTE: This MUST be called ONLY via the SerializeToDiskWorkItem class to ensure that it
     * happens on the right thread, at the right time.
     */
    private static void serializeToDiskSynchronously(String cacheKey, String controllerJson) {
        OutputStream outputStream = null;
        try {
            outputStream = controllerDiskCache.openPutStream(cacheKey);
            outputStream.write(controllerJson.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Unable to serialize controller to disk", e);
        } finally {
            if (outputStream != null) {
                Utility.closeQuietly(outputStream);
            }
        }
    }

    /**
     * NOTE: This MUST be called ONLY via the CreateLikeActionControllerWorkItem class to ensure
     * that it happens on the right thread, at the right time.
     */
    private static LikeActionController deserializeFromDiskSynchronously(String objectId) {
        LikeActionController controller = null;

        InputStream inputStream = null;
        try {
            String cacheKey = getCacheKeyForObjectId(objectId);
            inputStream = controllerDiskCache.get(cacheKey);
            if (inputStream != null) {
                String controllerJsonString = Utility.readStreamToString(inputStream);
                if (!Utility.isNullOrEmpty(controllerJsonString)) {
                    controller = deserializeFromJson(controllerJsonString);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to deserialize controller from disk", e);
            controller = null;
        } finally {
            if (inputStream != null) {
                Utility.closeQuietly(inputStream);
            }
        }

        return controller;
    }

    private static LikeActionController deserializeFromJson(String controllerJsonString) {
        LikeActionController controller;

        try {
            JSONObject controllerJson = new JSONObject(controllerJsonString);
            int version = controllerJson.optInt(JSON_INT_VERSION_KEY, -1);
            if (version != LIKE_ACTION_CONTROLLER_VERSION) {
                // Don't attempt to deserialize a controller that might be serialized differently
                // than expected.
                return null;
            }

            String objectId = controllerJson.getString(JSON_STRING_OBJECT_ID_KEY);
            int objectTypeInt = controllerJson.optInt(
                    JSON_INT_OBJECT_TYPE_KEY,
                    LikeView.ObjectType.UNKNOWN.getValue());

            controller = new LikeActionController(
                    objectId,
                    LikeView.ObjectType.fromInt(objectTypeInt));

            // Make sure to default to null and not empty string, to keep the logic elsewhere
            // functioning properly.
            controller.likeCountStringWithLike =
                    controllerJson.optString(JSON_STRING_LIKE_COUNT_WITH_LIKE_KEY, null);
            controller.likeCountStringWithoutLike =
                    controllerJson.optString(JSON_STRING_LIKE_COUNT_WITHOUT_LIKE_KEY, null);
            controller.socialSentenceWithLike =
                    controllerJson.optString(JSON_STRING_SOCIAL_SENTENCE_WITH_LIKE_KEY, null);
            controller.socialSentenceWithoutLike =
                    controllerJson.optString(JSON_STRING_SOCIAL_SENTENCE_WITHOUT_LIKE_KEY, null);
            controller.isObjectLiked = controllerJson.optBoolean(JSON_BOOL_IS_OBJECT_LIKED_KEY);
            controller.unlikeToken = controllerJson.optString(JSON_STRING_UNLIKE_TOKEN_KEY, null);

            JSONObject analyticsJSON = controllerJson.optJSONObject(
                    JSON_BUNDLE_FACEBOOK_DIALOG_ANALYTICS_BUNDLE);
            if (analyticsJSON != null) {
                controller.facebookDialogAnalyticsBundle =
                        BundleJSONConverter.convertToBundle(analyticsJSON);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unable to deserialize controller from JSON", e);
            controller = null;
        }

        return controller;
    }

    private static String serializeToJson(LikeActionController controller) {
        JSONObject controllerJson = new JSONObject();
        try {
            controllerJson.put(JSON_INT_VERSION_KEY, LIKE_ACTION_CONTROLLER_VERSION);
            controllerJson.put(JSON_STRING_OBJECT_ID_KEY, controller.objectId);
            controllerJson.put(JSON_INT_OBJECT_TYPE_KEY, controller.objectType.getValue());
            controllerJson.put(
                    JSON_STRING_LIKE_COUNT_WITH_LIKE_KEY,
                    controller.likeCountStringWithLike);
            controllerJson.put(
                    JSON_STRING_LIKE_COUNT_WITHOUT_LIKE_KEY,
                    controller.likeCountStringWithoutLike);
            controllerJson.put(
                    JSON_STRING_SOCIAL_SENTENCE_WITH_LIKE_KEY,
                    controller.socialSentenceWithLike);
            controllerJson.put(
                    JSON_STRING_SOCIAL_SENTENCE_WITHOUT_LIKE_KEY,
                    controller.socialSentenceWithoutLike);
            controllerJson.put(JSON_BOOL_IS_OBJECT_LIKED_KEY, controller.isObjectLiked);
            controllerJson.put(JSON_STRING_UNLIKE_TOKEN_KEY, controller.unlikeToken);
            if (controller.facebookDialogAnalyticsBundle != null) {
                JSONObject analyticsJSON =
                        BundleJSONConverter.convertToJSON(
                                controller.facebookDialogAnalyticsBundle);
                if (analyticsJSON != null) {
                    controllerJson.put(
                            JSON_BUNDLE_FACEBOOK_DIALOG_ANALYTICS_BUNDLE,
                            analyticsJSON);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unable to serialize controller to JSON", e);
            return null;
        }

        return controllerJson.toString();
    }

    private static String getCacheKeyForObjectId(String objectId) {
        String accessTokenPortion = null;
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            accessTokenPortion = accessToken.getToken();
        }
        if (accessTokenPortion != null) {
            // Cache-key collisions are not something to worry about here, since we only store state
            // for one access token. Even in the case where the previous access tokens serialized
            // files have not been deleted yet, the objectSuffix will be different due to the access
            // token change, thus making the key different.
            accessTokenPortion = Utility.md5hash(accessTokenPortion);
        }
        return String.format(
                Locale.ROOT,
                "%s|%s|com.fb.sdk.like|%d",
                objectId,
                Utility.coerceValueIfNullOrEmpty(accessTokenPortion, ""),
                objectSuffix);
    }

    //
    // Broadcast handling code
    //

    private static void broadcastAction(
            LikeActionController controller,
            String action) {
        broadcastAction(controller, action, null);
    }

    private static void broadcastAction(
            LikeActionController controller,
            String action,
            Bundle data) {
        Intent broadcastIntent = new Intent(action);
        if (controller != null) {
            if (data == null) {
                data = new Bundle();
            }

            data.putString(ACTION_OBJECT_ID_KEY, controller.getObjectId());
        }

        if (data != null) {
            broadcastIntent.putExtras(data);
        }
        LocalBroadcastManager.getInstance(FacebookSdk.getApplicationContext())
                .sendBroadcast(broadcastIntent);
    }

    /**
     * Constructor
     */
    private LikeActionController(String objectId, LikeView.ObjectType objectType) {
        this.objectId = objectId;
        this.objectType = objectType;
    }

    /**
     * Gets the the associated object id
     *
     * @return object id
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Gets the String representation of the like-count for the associated object
     *
     * @return String representation of the like-count for the associated object
     */
    public String getLikeCountString() {
        return isObjectLiked ? likeCountStringWithLike : likeCountStringWithoutLike;
    }

    /**
     * Gets the String representation of the like-count for the associated object
     *
     * @return String representation of the like-count for the associated object
     */
    public String getSocialSentence() {
        return isObjectLiked ? socialSentenceWithLike : socialSentenceWithoutLike;
    }

    /**
     * Indicates whether the associated object is liked
     *
     * @return Indication of whether the associated object is liked
     */
    public boolean isObjectLiked() {
        return isObjectLiked;
    }

    /**
     * Indicates whether the LikeView should enable itself.
     *
     * @return Indication of whether the LikeView should enable itself.
     */
    public boolean shouldEnableView() {
        if (LikeDialog.canShowNativeDialog() || LikeDialog.canShowWebFallback()) {
            return true;
        }
        if (objectIsPage || (objectType == LikeView.ObjectType.PAGE)) {
            // If we can't use the dialogs, then we can't like Pages.
            // Before any requests are made to the server, we have to rely on the object type set
            // by the app. If we have permissions to make requests, we will know the real type after
            // the first request.
            return false;
        }

        // See if we have publish permissions.
        // NOTE: This will NOT be accurate if the app has the type set as UNKNOWN, and the
        // underlying object is a page.
        AccessToken token = AccessToken.getCurrentAccessToken();
        return token != null
                && token.getPermissions() != null
                && token.getPermissions().contains("publish_actions");
    }

    /**
     * Entry-point to the code that performs the like/unlike action.
     */
    public void toggleLike(Activity activity, Fragment fragment, Bundle analyticsParameters) {
        boolean shouldLikeObject = !this.isObjectLiked;

        if (canUseOGPublish()) {
            // Update UI Like state optimistically
            updateLikeState(shouldLikeObject);
            if (isPendingLikeOrUnlike) {
                // If the user toggled the button quickly, and there is still a publish underway,
                // don't fire off another request. Also log this behavior.

                getAppEventsLogger().logSdkEvent(
                        AnalyticsEvents.EVENT_LIKE_VIEW_DID_UNDO_QUICKLY,
                        null,
                        analyticsParameters);
            } else if (!publishLikeOrUnlikeAsync(shouldLikeObject, analyticsParameters)) {
                // We were not able to send a graph request to unlike or like the object
                // Undo the optimistic state-update and show the dialog instead
                updateLikeState(!shouldLikeObject);
                presentLikeDialog(activity, fragment, analyticsParameters);
            }
        } else {
            presentLikeDialog(activity, fragment, analyticsParameters);
        }
    }

    private AppEventsLogger getAppEventsLogger() {
        if (appEventsLogger == null) {
            appEventsLogger = AppEventsLogger.newLogger(FacebookSdk.getApplicationContext());
        }
        return appEventsLogger;
    }

    private boolean publishLikeOrUnlikeAsync(
            boolean shouldLikeObject,
            Bundle analyticsParameters) {
        boolean requested = false;
        if (canUseOGPublish()) {
            if (shouldLikeObject) {
                requested = true;
                publishLikeAsync(analyticsParameters);
            } else if (!Utility.isNullOrEmpty(this.unlikeToken)) {
                requested = true;
                publishUnlikeAsync(analyticsParameters);
            }
        }

        return requested;
    }

    /**
     * Only to be called after an OG-publish was attempted and something went wrong. The Button
     * state is reverted and an error is returned to the LikeViews
     */
    private void publishDidError(boolean oldLikeState) {
        updateLikeState(oldLikeState);

        Bundle errorBundle = new Bundle();
        errorBundle.putString(
                NativeProtocol.STATUS_ERROR_DESCRIPTION,
                ERROR_PUBLISH_ERROR);

        broadcastAction(
                LikeActionController.this,
                ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR,
                errorBundle);
    }

    private void updateLikeState(boolean isObjectLiked) {
        updateState(isObjectLiked,
                this.likeCountStringWithLike,
                this.likeCountStringWithoutLike,
                this.socialSentenceWithLike,
                this.socialSentenceWithoutLike,
                this.unlikeToken);
    }

    private void updateState(boolean isObjectLiked,
                             String likeCountStringWithLike,
                             String likeCountStringWithoutLike,
                             String socialSentenceWithLike,
                             String socialSentenceWithoutLike,
                             String unlikeToken) {
        // Normalize all empty strings to null, so that we don't have any problems with comparison.
        likeCountStringWithLike = Utility.coerceValueIfNullOrEmpty(likeCountStringWithLike, null);
        likeCountStringWithoutLike =
                Utility.coerceValueIfNullOrEmpty(likeCountStringWithoutLike, null);
        socialSentenceWithLike = Utility.coerceValueIfNullOrEmpty(socialSentenceWithLike, null);
        socialSentenceWithoutLike =
                Utility.coerceValueIfNullOrEmpty(socialSentenceWithoutLike, null);
        unlikeToken = Utility.coerceValueIfNullOrEmpty(unlikeToken, null);

        boolean stateChanged = isObjectLiked != this.isObjectLiked ||
                !Utility.areObjectsEqual(
                        likeCountStringWithLike,
                        this.likeCountStringWithLike) ||
                !Utility.areObjectsEqual(
                        likeCountStringWithoutLike,
                        this.likeCountStringWithoutLike) ||
                !Utility.areObjectsEqual(socialSentenceWithLike, this.socialSentenceWithLike) ||
                !Utility.areObjectsEqual(
                        socialSentenceWithoutLike,
                        this.socialSentenceWithoutLike) ||
                !Utility.areObjectsEqual(unlikeToken, this.unlikeToken);

        if (!stateChanged) {
            return;
        }

        this.isObjectLiked = isObjectLiked;
        this.likeCountStringWithLike = likeCountStringWithLike;
        this.likeCountStringWithoutLike = likeCountStringWithoutLike;
        this.socialSentenceWithLike = socialSentenceWithLike;
        this.socialSentenceWithoutLike = socialSentenceWithoutLike;
        this.unlikeToken = unlikeToken;

        serializeToDiskAsync(this);

        broadcastAction(this, ACTION_LIKE_ACTION_CONTROLLER_UPDATED);
    }

    private void presentLikeDialog(
            final Activity activity,
            final Fragment fragment,
            final Bundle analyticsParameters) {
        String analyticsEvent = null;

        if (LikeDialog.canShowNativeDialog()) {
            analyticsEvent = AnalyticsEvents.EVENT_LIKE_VIEW_DID_PRESENT_DIALOG;
        } else if (LikeDialog.canShowWebFallback()) {
            analyticsEvent = AnalyticsEvents.EVENT_LIKE_VIEW_DID_PRESENT_FALLBACK;
        } else {
            // We will get here if the user tapped the button when dialogs cannot be shown.
            logAppEventForError("present_dialog", analyticsParameters);
            Utility.logd(TAG, "Cannot show the Like Dialog on this device.");

            // If we got to this point, we should ask the views to check if they should now
            // be disabled.
            broadcastAction(null, ACTION_LIKE_ACTION_CONTROLLER_UPDATED);
        }

        // Using the value of analyticsEvent to see if we can show any version of the dialog.
        // Written this way just to prevent extra lines of code.
        if (analyticsEvent != null) {
            String objectTypeString = (this.objectType != null)
                    ? this.objectType.toString()
                    : LikeView.ObjectType.UNKNOWN.toString();
            LikeContent likeContent = new LikeContent.Builder()
                    .setObjectId(this.objectId)
                    .setObjectType(objectTypeString)
                    .build();

            if (fragment != null) {
                new LikeDialog(fragment).show(likeContent);
            } else {
                new LikeDialog(activity).show(likeContent);
            }

            saveState(analyticsParameters);

            getAppEventsLogger().logSdkEvent(
                    AnalyticsEvents.EVENT_LIKE_VIEW_DID_PRESENT_DIALOG,
                    null,
                    analyticsParameters);
        }
    }

    private void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        // Look for results
        ShareInternalUtility.handleActivityResult(
                requestCode,
                resultCode,
                data,
                getResultProcessor(facebookDialogAnalyticsBundle));

        // The handlers from above will run synchronously. So by the time we get here, it should be
        // safe to stop tracking this call and also serialize the controller to disk
        clearState();
    }

    private ResultProcessor getResultProcessor(final Bundle analyticsParameters) {
        return new ResultProcessor(null) {
            @Override
            public void onSuccess(AppCall appCall, Bundle data) {
                if (data == null || !data.containsKey(LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY)) {
                    // This is an empty result that we can't handle.
                    return;
                }

                boolean isObjectLiked = data.getBoolean(LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY);

                // Default to known/cached state, if properties are missing.
                String likeCountStringWithLike =
                        LikeActionController.this.likeCountStringWithLike;
                String likeCountStringWithoutLike =
                        LikeActionController.this.likeCountStringWithoutLike;
                if (data.containsKey(LIKE_DIALOG_RESPONSE_LIKE_COUNT_STRING_KEY)) {
                    likeCountStringWithLike =
                            data.getString(LIKE_DIALOG_RESPONSE_LIKE_COUNT_STRING_KEY);
                    likeCountStringWithoutLike = likeCountStringWithLike;
                }

                String socialSentenceWithLike = LikeActionController.this.socialSentenceWithLike;
                String socialSentenceWithoutWithoutLike =
                        LikeActionController.this.socialSentenceWithoutLike;
                if (data.containsKey(LIKE_DIALOG_RESPONSE_SOCIAL_SENTENCE_KEY)) {
                    socialSentenceWithLike = data.getString(
                            LIKE_DIALOG_RESPONSE_SOCIAL_SENTENCE_KEY);
                    socialSentenceWithoutWithoutLike = socialSentenceWithLike;
                }

                String unlikeToken = data.containsKey(LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY)
                        ? data.getString(LIKE_DIALOG_RESPONSE_UNLIKE_TOKEN_KEY)
                        : LikeActionController.this.unlikeToken;

                Bundle logParams =
                        (analyticsParameters == null) ? new Bundle() : analyticsParameters;
                logParams.putString(
                        AnalyticsEvents.PARAMETER_CALL_ID,
                        appCall.getCallId().toString());
                getAppEventsLogger().logSdkEvent(
                        AnalyticsEvents.EVENT_LIKE_VIEW_DIALOG_DID_SUCCEED,
                        null,
                        logParams);

                updateState(
                        isObjectLiked,
                        likeCountStringWithLike,
                        likeCountStringWithoutLike,
                        socialSentenceWithLike,
                        socialSentenceWithoutWithoutLike,
                        unlikeToken);
            }

            @Override
            public void onError(AppCall appCall, FacebookException error) {
                Logger.log(
                        LoggingBehavior.REQUESTS,
                        TAG,
                        "Like Dialog failed with error : %s",
                        error);

                Bundle logParams = analyticsParameters == null ? new Bundle() : analyticsParameters;
                logParams.putString(
                        AnalyticsEvents.PARAMETER_CALL_ID,
                        appCall.getCallId().toString());

                // Log the error and AppEvent
                logAppEventForError("present_dialog", logParams);

                broadcastAction(
                        LikeActionController.this,
                        ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR,
                        NativeProtocol.createBundleForException(error));
            }

            @Override
            public void onCancel(AppCall appCall) {
                onError(appCall, new FacebookOperationCanceledException());
            }
        };
    }

    private void saveState(Bundle analyticsParameters) {
        // Save off the call id for processing the response
        storeObjectIdForPendingController(objectId);

        // Store off the analytics parameters as well, for completion-logging
        facebookDialogAnalyticsBundle = analyticsParameters;

        // Serialize to disk, in case we get terminated while waiting for the dialog to complete
        serializeToDiskAsync(this);
    }

    private void clearState() {
        facebookDialogAnalyticsBundle = null;

        storeObjectIdForPendingController(null);
    }

    private static void storeObjectIdForPendingController(String objectId) {
        objectIdForPendingController = objectId;
        Context appContext = FacebookSdk.getApplicationContext();

        appContext.getSharedPreferences(LIKE_ACTION_CONTROLLER_STORE, Context.MODE_PRIVATE)
                .edit()
                .putString(
                        LIKE_ACTION_CONTROLLER_STORE_PENDING_OBJECT_ID_KEY,
                        objectIdForPendingController)
                .apply();
    }

    private boolean canUseOGPublish() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        // Verify that the object isn't a Page, that we have permissions and that, if we're
        // unliking, then we have an unlike token.
        return !objectIsPage &&
                verifiedObjectId != null &&
                accessToken != null &&
                accessToken.getPermissions() != null &&
                accessToken.getPermissions().contains("publish_actions");
    }

    private void publishLikeAsync(final Bundle analyticsParameters) {
        isPendingLikeOrUnlike = true;

        fetchVerifiedObjectId(new RequestCompletionCallback() {
            @Override
            public void onComplete() {
                if (Utility.isNullOrEmpty(verifiedObjectId)) {
                    // Could not get a verified id
                    Bundle errorBundle = new Bundle();
                    errorBundle.putString(
                            NativeProtocol.STATUS_ERROR_DESCRIPTION,
                            ERROR_INVALID_OBJECT_ID);

                    broadcastAction(
                            LikeActionController.this,
                            ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR,
                            errorBundle);
                    return;
                }

                // Perform the Like.
                GraphRequestBatch requestBatch = new GraphRequestBatch();
                final PublishLikeRequestWrapper likeRequest =
                        new PublishLikeRequestWrapper(verifiedObjectId, objectType);
                likeRequest.addToBatch(requestBatch);
                requestBatch.addCallback(new GraphRequestBatch.Callback() {
                    @Override
                    public void onBatchCompleted(GraphRequestBatch batch) {
                        isPendingLikeOrUnlike = false;

                        if (likeRequest.getError() != null) {
                            // We already updated the UI to show button in the Liked state. Since
                            // this failed, let's revert back to the Unliked state and broadcast
                            // an error
                            publishDidError(false);
                        } else {
                            unlikeToken =
                                    Utility.coerceValueIfNullOrEmpty(likeRequest.unlikeToken, null);
                            isObjectLikedOnServer = true;

                            getAppEventsLogger().logSdkEvent(
                                    AnalyticsEvents.EVENT_LIKE_VIEW_DID_LIKE,
                                    null,
                                    analyticsParameters);

                            // See if the user toggled the button back while this request was
                            // completing
                            publishAgainIfNeeded(analyticsParameters);
                        }
                    }
                });

                requestBatch.executeAsync();
            }
        });
    }

    private void publishUnlikeAsync(final Bundle analyticsParameters) {
        isPendingLikeOrUnlike = true;

        // Perform the Unlike.
        GraphRequestBatch requestBatch = new GraphRequestBatch();
        final PublishUnlikeRequestWrapper unlikeRequest =
                new PublishUnlikeRequestWrapper(unlikeToken);
        unlikeRequest.addToBatch(requestBatch);
        requestBatch.addCallback(new GraphRequestBatch.Callback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
                isPendingLikeOrUnlike = false;

                if (unlikeRequest.getError() != null) {
                    // We already updated the UI to show button in the Unliked state. Since this
                    // failed, let's revert back to the Liked state and broadcast an error.
                    publishDidError(true);
                } else {
                    unlikeToken = null;
                    isObjectLikedOnServer = false;

                    getAppEventsLogger().logSdkEvent(
                            AnalyticsEvents.EVENT_LIKE_VIEW_DID_UNLIKE,
                            null,
                            analyticsParameters);

                    // See if the user toggled the button back while this request was
                    // completing
                    publishAgainIfNeeded(analyticsParameters);
                }
            }
        });

        requestBatch.executeAsync();
    }

    private void refreshStatusAsync() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) {
            // Only when we know that there is no active access token should we attempt getting like
            // state from the service. Otherwise, use the access token to make sure we get the
            // correct like state.
            refreshStatusViaService();
            return;
        }

        fetchVerifiedObjectId(new RequestCompletionCallback() {
            @Override
            public void onComplete() {
                final LikeRequestWrapper likeRequestWrapper;
                switch (objectType) {
                    case PAGE:
                        likeRequestWrapper = new GetPageLikesRequestWrapper(verifiedObjectId);
                        break;
                    default:
                        likeRequestWrapper =
                                new GetOGObjectLikesRequestWrapper(verifiedObjectId, objectType);
                        break;
                }
                final GetEngagementRequestWrapper engagementRequest =
                        new GetEngagementRequestWrapper(verifiedObjectId, objectType);

                GraphRequestBatch requestBatch = new GraphRequestBatch();
                likeRequestWrapper.addToBatch(requestBatch);
                engagementRequest.addToBatch(requestBatch);

                requestBatch.addCallback(new GraphRequestBatch.Callback() {
                    @Override
                    public void onBatchCompleted(GraphRequestBatch batch) {
                        if (likeRequestWrapper.getError() != null ||
                                engagementRequest.getError() != null) {
                            // Refreshing is best-effort. If the refresh fails, don't lose old
                            // state.
                            Logger.log(
                                    LoggingBehavior.REQUESTS,
                                    TAG,
                                    "Unable to refresh like state for id: '%s'", objectId);
                            return;
                        }

                        updateState(
                                likeRequestWrapper.isObjectLiked(),
                                engagementRequest.likeCountStringWithLike,
                                engagementRequest.likeCountStringWithoutLike,
                                engagementRequest.socialSentenceStringWithLike,
                                engagementRequest.socialSentenceStringWithoutLike,
                                likeRequestWrapper.getUnlikeToken());
                    }
                });

                requestBatch.executeAsync();
            }
        });
    }

    private void refreshStatusViaService() {
        LikeStatusClient likeStatusClient = new LikeStatusClient(
                FacebookSdk.getApplicationContext(),
                FacebookSdk.getApplicationId(),
                objectId);
        if (!likeStatusClient.start()) {
            return;
        }

        LikeStatusClient.CompletedListener callback = new LikeStatusClient.CompletedListener() {
            @Override
            public void completed(Bundle result) {
                // Don't lose old state if the service response is incomplete.
                if (result == null || !result.containsKey(ShareConstants.EXTRA_OBJECT_IS_LIKED)) {
                    return;
                }

                boolean objectIsLiked = result.getBoolean(ShareConstants.EXTRA_OBJECT_IS_LIKED);

                String likeCountWithLike =
                        result.containsKey(ShareConstants.EXTRA_LIKE_COUNT_STRING_WITH_LIKE)
                                ? result.getString(ShareConstants.EXTRA_LIKE_COUNT_STRING_WITH_LIKE)
                                : LikeActionController.this.likeCountStringWithLike;

                String likeCountWithoutLike =
                        result.containsKey(ShareConstants.EXTRA_LIKE_COUNT_STRING_WITHOUT_LIKE)
                                ? result.getString(
                                ShareConstants.EXTRA_LIKE_COUNT_STRING_WITHOUT_LIKE)
                                : LikeActionController.this.likeCountStringWithoutLike;

                String socialSentenceWithLike =
                        result.containsKey(ShareConstants.EXTRA_SOCIAL_SENTENCE_WITH_LIKE)
                                ? result.getString(ShareConstants.EXTRA_SOCIAL_SENTENCE_WITH_LIKE)
                                : LikeActionController.this.socialSentenceWithLike;

                String socialSentenceWithoutLike =
                        result.containsKey(ShareConstants.EXTRA_SOCIAL_SENTENCE_WITHOUT_LIKE)
                                ? result.getString(
                                ShareConstants.EXTRA_SOCIAL_SENTENCE_WITHOUT_LIKE)
                                : LikeActionController.this.socialSentenceWithoutLike;

                String unlikeToken =
                        result.containsKey(ShareConstants.EXTRA_UNLIKE_TOKEN)
                                ? result.getString(ShareConstants.EXTRA_UNLIKE_TOKEN)
                                : LikeActionController.this.unlikeToken;

                updateState(
                        objectIsLiked,
                        likeCountWithLike,
                        likeCountWithoutLike,
                        socialSentenceWithLike,
                        socialSentenceWithoutLike,
                        unlikeToken);
            }
        };

        likeStatusClient.setCompletedListener(callback);
    }

    private void publishAgainIfNeeded(final Bundle analyticsParameters) {
        if (isObjectLiked != isObjectLikedOnServer &&
                !publishLikeOrUnlikeAsync(isObjectLiked, analyticsParameters)) {
            // Unable to re-publish the new desired state. Signal that there is an error and
            // revert the like state back.
            publishDidError(!isObjectLiked);
        }
    }

    private void fetchVerifiedObjectId(final RequestCompletionCallback completionHandler) {
        if (!Utility.isNullOrEmpty(verifiedObjectId)) {
            if (completionHandler != null) {
                completionHandler.onComplete();
            }

            return;
        }

        final GetOGObjectIdRequestWrapper objectIdRequest =
                new GetOGObjectIdRequestWrapper(objectId, objectType);
        final GetPageIdRequestWrapper pageIdRequest =
                new GetPageIdRequestWrapper(objectId, objectType);

        GraphRequestBatch requestBatch = new GraphRequestBatch();
        objectIdRequest.addToBatch(requestBatch);
        pageIdRequest.addToBatch(requestBatch);

        requestBatch.addCallback(new GraphRequestBatch.Callback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
                verifiedObjectId = objectIdRequest.verifiedObjectId;
                if (Utility.isNullOrEmpty(verifiedObjectId)) {
                    verifiedObjectId = pageIdRequest.verifiedObjectId;
                    objectIsPage = pageIdRequest.objectIsPage;
                }

                if (Utility.isNullOrEmpty(verifiedObjectId)) {
                    Logger.log(LoggingBehavior.DEVELOPER_ERRORS,
                            TAG,
                            "Unable to verify the FB id for '%s'. Verify that it is a valid FB" +
                                    " object or page",
                            objectId);
                    logAppEventForError("get_verified_id",
                            pageIdRequest.getError() != null
                                    ? pageIdRequest.getError()
                                    : objectIdRequest.getError());
                }

                if (completionHandler != null) {
                    completionHandler.onComplete();
                }
            }
        });

        requestBatch.executeAsync();
    }

    private void logAppEventForError(String action, Bundle parameters) {
        Bundle logParams = new Bundle(parameters);
        logParams.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_OBJECT_ID, objectId);
        logParams.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_OBJECT_TYPE, objectType.toString());
        logParams.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_CURRENT_ACTION, action);

        getAppEventsLogger().logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_ERROR, null, logParams);
    }

    private void logAppEventForError(String action, FacebookRequestError error) {
        Bundle logParams = new Bundle();
        if (error != null) {
            JSONObject requestResult = error.getRequestResult();
            if (requestResult != null) {
                logParams.putString(
                        AnalyticsEvents.PARAMETER_LIKE_VIEW_ERROR_JSON,
                        requestResult.toString());
            }
        }
        logAppEventForError(action, logParams);
    }

    //
    // Interfaces
    //

    /**
     * Used by the call to getControllerForObjectId()
     */
    public interface CreationCallback {
        public void onComplete(
                LikeActionController likeActionController,
                FacebookException error);
    }

    /**
     * Used by all the request wrappers
     */
    private interface RequestCompletionCallback {
        void onComplete();
    }

    //
    // Inner classes
    //

    private class GetOGObjectIdRequestWrapper extends AbstractRequestWrapper {
        String verifiedObjectId;

        GetOGObjectIdRequestWrapper(String objectId, LikeView.ObjectType objectType) {
            super(objectId, objectType);

            Bundle objectIdRequestParams = new Bundle();
            objectIdRequestParams.putString("fields", "og_object.fields(id)");
            objectIdRequestParams.putString("ids", objectId);

            setRequest(new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "",
                    objectIdRequestParams,
                    HttpMethod.GET));
        }

        @Override
        protected void processError(FacebookRequestError error) {
            // If this object Id is for a Page, an error will be received for this request
            // We will then rely on the other request to come through.
            if (error.getErrorMessage().contains("og_object")) {
                this.error = null;
            } else {
                Logger.log(LoggingBehavior.REQUESTS,
                        TAG,
                        "Error getting the FB id for object '%s' with type '%s' : %s",
                        objectId,
                        objectType,
                        error);
            }
        }

        @Override
        protected void processSuccess(GraphResponse response) {
            JSONObject results = Utility.tryGetJSONObjectFromResponse(
                    response.getJSONObject(),
                    objectId);
            if (results != null) {
                // See if we can get the OG object Id out
                JSONObject ogObject = results.optJSONObject("og_object");
                if (ogObject != null) {
                    verifiedObjectId = ogObject.optString("id");
                }
            }
        }
    }

    private class GetPageIdRequestWrapper extends AbstractRequestWrapper {
        String verifiedObjectId;
        boolean objectIsPage;

        GetPageIdRequestWrapper(String objectId, LikeView.ObjectType objectType) {
            super(objectId, objectType);

            Bundle pageIdRequestParams = new Bundle();
            pageIdRequestParams.putString("fields", "id");
            pageIdRequestParams.putString("ids", objectId);

            setRequest(new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "",
                    pageIdRequestParams,
                    HttpMethod.GET));
        }

        @Override
        protected void processSuccess(GraphResponse response) {
            JSONObject results = Utility.tryGetJSONObjectFromResponse(
                    response.getJSONObject(),
                    objectId);
            if (results != null) {
                verifiedObjectId = results.optString("id");
                objectIsPage = !Utility.isNullOrEmpty(verifiedObjectId);
            }
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error getting the FB id for object '%s' with type '%s' : %s",
                    objectId,
                    objectType,
                    error);
        }
    }

    private class PublishLikeRequestWrapper extends AbstractRequestWrapper {
        String unlikeToken;

        PublishLikeRequestWrapper(String objectId, LikeView.ObjectType objectType) {
            super(objectId, objectType);

            Bundle likeRequestParams = new Bundle();
            likeRequestParams.putString("object", objectId);

            setRequest(new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me/og.likes",
                    likeRequestParams,
                    HttpMethod.POST));
        }

        @Override
        protected void processSuccess(GraphResponse response) {
            unlikeToken = Utility.safeGetStringFromResponse(response.getJSONObject(), "id");
        }

        @Override
        protected void processError(FacebookRequestError error) {
            int errorCode = error.getErrorCode();
            if (errorCode == ERROR_CODE_OBJECT_ALREADY_LIKED) {
                // This isn't an error for us. Client was just out of sync with server
                // This will prevent us from showing the dialog for this.

                // However, there is no unliketoken. So a subsequent unlike WILL show the dialog
                this.error = null;
            } else {
                Logger.log(LoggingBehavior.REQUESTS,
                        TAG,
                        "Error liking object '%s' with type '%s' : %s",
                        objectId,
                        objectType,
                        error);
                logAppEventForError("publish_like", error);
            }
        }
    }

    private class PublishUnlikeRequestWrapper extends AbstractRequestWrapper {
        private String unlikeToken;

        PublishUnlikeRequestWrapper(String unlikeToken) {
            super(null, null);

            this.unlikeToken = unlikeToken;

            setRequest(new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    unlikeToken,
                    null,
                    HttpMethod.DELETE));
        }

        @Override
        protected void processSuccess(GraphResponse response) {
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error unliking object with unlike token '%s' : %s", unlikeToken, error);
            logAppEventForError("publish_unlike", error);
        }
    }

    private interface LikeRequestWrapper extends RequestWrapper {
        boolean isObjectLiked();
        String getUnlikeToken();
    }

    private class GetPageLikesRequestWrapper
            extends AbstractRequestWrapper
            implements LikeRequestWrapper {
        private boolean objectIsLiked = LikeActionController.this.isObjectLiked;
        private String pageId;

        GetPageLikesRequestWrapper(String pageId) {
            super(pageId, LikeView.ObjectType.PAGE);
            this.pageId = pageId;

            Bundle requestParams = new Bundle();
            requestParams.putString("fields", "id");

            setRequest(new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me/likes/" + pageId,
                    requestParams,
                    HttpMethod.GET));
        }

        @Override
        protected void processSuccess(GraphResponse response) {
            JSONArray dataSet = Utility.tryGetJSONArrayFromResponse(
                    response.getJSONObject(),
                    "data");
            if (dataSet != null && dataSet.length() > 0) {
                objectIsLiked = true;
            }
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error fetching like status for page id '%s': %s",
                    this.pageId,
                    error);
            logAppEventForError("get_page_like", error);
        }


        @Override
        public boolean isObjectLiked() {
            return this.objectIsLiked;
        }

        @Override
        public String getUnlikeToken() {
            return null;
        }
    }

    private class GetOGObjectLikesRequestWrapper
            extends AbstractRequestWrapper
            implements LikeRequestWrapper {
        // Initialize the like status to what we currently have. This way, empty/error responses
        // don't end up clearing out the state.
        private boolean objectIsLiked = LikeActionController.this.isObjectLiked;
        private String unlikeToken;
        private final String objectId;
        private final LikeView.ObjectType objectType;

        GetOGObjectLikesRequestWrapper(String objectId, LikeView.ObjectType objectType) {
            super(objectId, objectType);
            this.objectId = objectId;
            this.objectType = objectType;

            Bundle requestParams = new Bundle();
            requestParams.putString("fields", "id,application");
            requestParams.putString("object", this.objectId);

            setRequest(new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me/og.likes",
                    requestParams,
                    HttpMethod.GET));
        }

        @Override
        protected void processSuccess(GraphResponse response) {
            JSONArray dataSet = Utility.tryGetJSONArrayFromResponse(
                    response.getJSONObject(),
                    "data");
            if (dataSet != null) {
                for (int i = 0; i < dataSet.length(); i++) {
                    JSONObject data = dataSet.optJSONObject(i);
                    if (data != null) {
                        objectIsLiked = true;
                        JSONObject appData = data.optJSONObject("application");
                        AccessToken accessToken = AccessToken.getCurrentAccessToken();
                        if (appData != null &&
                                accessToken != null &&
                                Utility.areObjectsEqual(
                                        accessToken.getApplicationId(),
                                        appData.optString("id"))) {
                            unlikeToken = data.optString("id");
                        }
                    }
                }
            }
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error fetching like status for object '%s' with type '%s' : %s",
                    this.objectId,
                    this.objectType,
                    error);
            logAppEventForError("get_og_object_like", error);
        }

        @Override
        public boolean isObjectLiked() {
            return this.objectIsLiked;
        }

        @Override
        public String getUnlikeToken() {
            return this.unlikeToken;
        }
    }



    private class GetEngagementRequestWrapper extends AbstractRequestWrapper {
        // Initialize the like status to what we currently have. This way, empty/error responses
        // don't end up clearing out the state.
        String likeCountStringWithLike = LikeActionController.this.likeCountStringWithLike;
        String likeCountStringWithoutLike = LikeActionController.this.likeCountStringWithoutLike;
        String socialSentenceStringWithLike = LikeActionController.this.socialSentenceWithLike;
        String socialSentenceStringWithoutLike =
                LikeActionController.this.socialSentenceWithoutLike;

        GetEngagementRequestWrapper(String objectId, LikeView.ObjectType objectType) {
            super(objectId, objectType);

            Bundle requestParams = new Bundle();
            requestParams.putString(
                    "fields",
                    "engagement.fields(" +
                            "count_string_with_like," +
                            "count_string_without_like," +
                            "social_sentence_with_like," +
                            "social_sentence_without_like)");

            setRequest(new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    objectId,
                    requestParams,
                    HttpMethod.GET));
        }

        @Override
        protected void processSuccess(GraphResponse response) {
            JSONObject engagementResults = Utility.tryGetJSONObjectFromResponse(
                    response.getJSONObject(),
                    "engagement");
            if (engagementResults != null) {
                // Missing properties in the response should default to cached like status
                likeCountStringWithLike =
                        engagementResults.optString(
                                "count_string_with_like",
                                likeCountStringWithLike);

                likeCountStringWithoutLike =
                        engagementResults.optString(
                                "count_string_without_like",
                                likeCountStringWithoutLike);

                socialSentenceStringWithLike =
                        engagementResults.optString(
                                "social_sentence_with_like",
                                socialSentenceStringWithLike);

                socialSentenceStringWithoutLike =
                        engagementResults.optString(
                                "social_sentence_without_like",
                                socialSentenceStringWithoutLike);
            }
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error fetching engagement for object '%s' with type '%s' : %s",
                    objectId,
                    objectType,
                    error);
            logAppEventForError("get_engagement", error);
        }
    }

    private interface RequestWrapper {
        FacebookRequestError getError();
        void addToBatch(GraphRequestBatch batch);
    }

    private abstract class AbstractRequestWrapper implements RequestWrapper{
        private GraphRequest request;
        protected String objectId;
        protected LikeView.ObjectType objectType;
        protected FacebookRequestError error;

        protected AbstractRequestWrapper(String objectId, LikeView.ObjectType objectType) {
            this.objectId = objectId;
            this.objectType = objectType;
        }

        public void addToBatch(GraphRequestBatch batch) {
            batch.add(request);
        }

        public FacebookRequestError getError() {
            return this.error;
        }

        protected void setRequest(GraphRequest request) {
            this.request = request;
            // Make sure that our requests are hitting the latest version of the API known to this
            // sdk.
            request.setVersion(ServerProtocol.GRAPH_API_VERSION);
            request.setCallback(new GraphRequest.Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    error = response.getError();
                    if (error != null) {
                        processError(error);
                    } else {
                        processSuccess(response);
                    }
                }
            });
        }

        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error running request for object '%s' with type '%s' : %s",
                    this.objectId,
                    this.objectType,
                    error);
        }

        protected abstract void processSuccess(GraphResponse response);
    }

    // Performs cache re-ordering/trimming to keep most-recently-used items up front
    // ** NOTE ** It is expected that only _ONE_ MRUCacheWorkItem is ever running. This is enforced
    // by setting the concurrency of the WorkQueue to 1. Changing the concurrency will most likely
    // lead to errors.
    private static class MRUCacheWorkItem implements Runnable {
        private static ArrayList<String> mruCachedItems = new ArrayList<String>();
        private String cacheItem;
        private boolean shouldTrim;

        MRUCacheWorkItem(String cacheItem, boolean shouldTrim) {
            this.cacheItem = cacheItem;
            this.shouldTrim = shouldTrim;
        }

        @Override
        public void run() {
            if (cacheItem != null) {
                mruCachedItems.remove(cacheItem);
                mruCachedItems.add(0, cacheItem);
            }
            if (shouldTrim && mruCachedItems.size() >= MAX_CACHE_SIZE) {
                int targetSize = MAX_CACHE_SIZE / 2; // Optimize for fewer trim-passes.
                while (targetSize < mruCachedItems.size()) {
                    String cacheKey = mruCachedItems.remove(mruCachedItems.size() - 1);

                    // Here is where we actually remove from the cache of LikeActionControllers.
                    cache.remove(cacheKey);
                }
            }
        }
    }

    private static class SerializeToDiskWorkItem implements Runnable {
        private String cacheKey;
        private String controllerJson;

        SerializeToDiskWorkItem(String cacheKey, String controllerJson) {
            this.cacheKey = cacheKey;
            this.controllerJson = controllerJson;
        }

        @Override
        public void run() {
            serializeToDiskSynchronously(cacheKey, controllerJson);
        }
    }

    private static class CreateLikeActionControllerWorkItem implements Runnable {
        private String objectId;
        private LikeView.ObjectType objectType;
        private CreationCallback callback;

        CreateLikeActionControllerWorkItem(
                String objectId,
                LikeView.ObjectType objectType,
                CreationCallback callback) {
            this.objectId = objectId;
            this.objectType = objectType;
            this.callback = callback;
        }

        @Override
        public void run() {
            createControllerForObjectIdAndType(objectId, objectType, callback);
        }
    }
}
