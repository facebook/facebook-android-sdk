/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.internal;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.facebook.*;
import com.facebook.widget.FacebookDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 */
public class LikeActionController {

    public static final String ACTION_LIKE_ACTION_CONTROLLER_UPDATED = "com.facebook.sdk.LikeActionController.UPDATED";
    public static final String ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR = "com.facebook.sdk.LikeActionController.DID_ERROR";
    public static final String ACTION_LIKE_ACTION_CONTROLLER_DID_RESET = "com.facebook.sdk.LikeActionController.DID_RESET";

    public static final String ACTION_OBJECT_ID_KEY = "com.facebook.sdk.LikeActionController.OBJECT_ID";

    public static final String ERROR_INVALID_OBJECT_ID = "Invalid Object Id";

    private static final String TAG = LikeActionController.class.getSimpleName();

    private static final int LIKE_ACTION_CONTROLLER_VERSION = 2;
    private static final int MAX_CACHE_SIZE = 128;
    // MAX_OBJECT_SUFFIX basically accommodates for 1000 session-state changes before the async disk-cache-clear
    // finishes. The value is reasonably arbitrary.
    private static final int MAX_OBJECT_SUFFIX = 1000;

    private static final String LIKE_ACTION_CONTROLLER_STORE = "com.facebook.LikeActionController.CONTROLLER_STORE_KEY";
    private static final String LIKE_ACTION_CONTROLLER_STORE_PENDING_OBJECT_ID_KEY = "PENDING_CONTROLLER_KEY";
    private static final String LIKE_ACTION_CONTROLLER_STORE_OBJECT_SUFFIX_KEY = "OBJECT_SUFFIX";

    private static final String JSON_INT_VERSION_KEY = "com.facebook.internal.LikeActionController.version";
    private static final String JSON_STRING_OBJECT_ID_KEY = "object_id";
    private static final String JSON_STRING_LIKE_COUNT_WITH_LIKE_KEY = "like_count_string_with_like";
    private static final String JSON_STRING_LIKE_COUNT_WITHOUT_LIKE_KEY = "like_count_string_without_like";
    private static final String JSON_STRING_SOCIAL_SENTENCE_WITH_LIKE_KEY = "social_sentence_with_like";
    private static final String JSON_STRING_SOCIAL_SENTENCE_WITHOUT_LIKE_KEY = "social_sentence_without_like";
    private static final String JSON_BOOL_IS_OBJECT_LIKED_KEY = "is_object_liked";
    private static final String JSON_STRING_UNLIKE_TOKEN_KEY = "unlike_token";
    private static final String JSON_STRING_PENDING_CALL_ID_KEY = "pending_call_id";
    private static final String JSON_BUNDLE_PENDING_CALL_ANALYTICS_BUNDLE = "pending_call_analytics_bundle";

    private static final String LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY = "object_is_liked";
    private static final String LIKE_DIALOG_RESPONSE_LIKE_COUNT_STRING_KEY = "like_count_string";
    private static final String LIKE_DIALOG_RESPONSE_SOCIAL_SENTENCE_KEY = "social_sentence";
    private static final String LIKE_DIALOG_RESPONSE_UNLIKE_TOKEN_KEY = "unlike_token";

    private static final int ERROR_CODE_OBJECT_ALREADY_LIKED = 3501;

    private static FileLruCache controllerDiskCache;
    private static final ConcurrentHashMap<String, LikeActionController> cache =
            new ConcurrentHashMap<String, LikeActionController>();
    private static WorkQueue mruCacheWorkQueue = new WorkQueue(1); // This MUST be 1 for proper synchronization
    private static WorkQueue diskIOWorkQueue = new WorkQueue(1); // This MUST be 1 for proper synchronization
    private static Handler handler;
    private static String objectIdForPendingController;
    private static boolean isPendingBroadcastReset;
    private static boolean isInitialized;
    private static volatile int objectSuffix;

    private Session session;
    private Context context;
    private String objectId;
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

    private UUID pendingCallId;

    private Bundle pendingCallAnalyticsBundle;

    private AppEventsLogger appEventsLogger;

    /**
     * Called from UiLifecycleHelper to process any pending likes that had resulted in the Like dialog
     * being displayed
     *
     * @param context Hosting context
     * @param requestCode From the originating call to onActivityResult
     * @param resultCode From the originating call to onActivityResult
     * @param data From the originating call to onActivityResult
     * @return Indication of whether the Intent was handled
     */
    public static boolean handleOnActivityResult(Context context,
                                                 final int requestCode,
                                                 final int resultCode,
                                                 final Intent data) {
        final UUID callId = NativeProtocol.getCallIdFromIntent(data);
        if (callId == null) {
            return false;
        }

        // See if we were waiting on a Like dialog completion.
        if (Utility.isNullOrEmpty(objectIdForPendingController)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(
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
                context,
                objectIdForPendingController,
                new CreationCallback() {
                    @Override
                    public void onComplete(LikeActionController likeActionController) {
                        likeActionController.onActivityResult(requestCode, resultCode, data, callId);
                    }
                });

        return true;
    }

    /**
     * Called by the LikeView when an object-id is set on it.
     * @param context context
     * @param objectId Object Id
     * @param callback Callback to be invoked when the LikeActionController has been created.
     */
    public static void getControllerForObjectId(
            Context context,
            String objectId,
            CreationCallback callback) {
        if (!isInitialized) {
            performFirstInitialize(context);
        }

        LikeActionController controllerForObject = getControllerFromInMemoryCache(objectId);
        if (controllerForObject != null) {
            // Direct object-cache hit
            invokeCallbackWithController(callback, controllerForObject);
        } else {
            diskIOWorkQueue.addActiveWorkItem(new CreateLikeActionControllerWorkItem(context, objectId, callback));
        }
    }

    /**
     * NOTE: This MUST be called ONLY via the CreateLikeActionControllerWorkItem class to ensure that it happens on the
     * right thread, at the right time.
     */
    private static void createControllerForObjectId(
            Context context,
            String objectId,
            CreationCallback callback) {
        // Check again to see if the controller was created before attempting to deserialize/create one.
        // Need to check this in the case where multiple LikeViews are looking for a controller for the same object
        // and all got queued up to create one. We only want the first one to go through with the creation, and the
        // rest should get the same instance from the object-cache.
        LikeActionController controllerForObject = getControllerFromInMemoryCache(objectId);
        if (controllerForObject != null) {
            // Direct object-cache hit
            invokeCallbackWithController(callback, controllerForObject);
            return;
        }

        // Try deserialize from disk
        controllerForObject = deserializeFromDiskSynchronously(context, objectId);

        if (controllerForObject == null) {
            controllerForObject = new LikeActionController(context, Session.getActiveSession(), objectId);
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

        invokeCallbackWithController(callback, controllerToRefresh);
    }

    private synchronized static void performFirstInitialize(Context context) {
        if (isInitialized) {
            return;
        }

        handler = new Handler(Looper.getMainLooper());

        SharedPreferences sharedPreferences = context.getSharedPreferences(
                LIKE_ACTION_CONTROLLER_STORE,
                Context.MODE_PRIVATE);

        objectSuffix = sharedPreferences.getInt(LIKE_ACTION_CONTROLLER_STORE_OBJECT_SUFFIX_KEY, 1);
        controllerDiskCache = new FileLruCache(context, TAG, new FileLruCache.Limits());

        registerSessionBroadcastReceivers(context);

        isInitialized = true;
    }

    private static void invokeCallbackWithController(final CreationCallback callback, final LikeActionController controller) {
        if (callback == null) {
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                callback.onComplete(controller);
            }
        });
    }

    //
    // In-memory mru-caching code
    //

    private static void registerSessionBroadcastReceivers(Context context) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Session.ACTION_ACTIVE_SESSION_UNSET);
        filter.addAction(Session.ACTION_ACTIVE_SESSION_CLOSED);
        filter.addAction(Session.ACTION_ACTIVE_SESSION_OPENED);

        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (isPendingBroadcastReset) {
                    return;
                }

                String action = intent.getAction();
                final boolean shouldClearDisk =
                        Utility.areObjectsEqual(Session.ACTION_ACTIVE_SESSION_UNSET, action) ||
                                Utility.areObjectsEqual(Session.ACTION_ACTIVE_SESSION_CLOSED, action);


                isPendingBroadcastReset = true;
                // Delaying sending the broadcast to reset, because we might get many successive calls from Session
                // (to UNSET, SET & OPEN) and a delay would prevent excessive chatter.
                final Context broadcastContext = receiverContext;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Bump up the objectSuffix so that we don't have a filename collision between a cache-clear and
                        // and a cache-read/write.
                        //
                        // NOTE: We know that onReceive() was called on the main thread. This means that even this code
                        // is running on the main thread, and therefore, there aren't synchronization issues with
                        // incrementing the objectSuffix and clearing the caches here.
                        if (shouldClearDisk) {
                            objectSuffix = (objectSuffix + 1) % MAX_OBJECT_SUFFIX;
                            broadcastContext.getSharedPreferences(LIKE_ACTION_CONTROLLER_STORE, Context.MODE_PRIVATE)
                                    .edit()
                                    .putInt(LIKE_ACTION_CONTROLLER_STORE_OBJECT_SUFFIX_KEY, objectSuffix)
                                    .apply();

                            // Only clearing the actual caches. The MRU index will self-clean with usage.
                            // Clearing the caches is necessary to prevent leaking like-state across sessions.
                            cache.clear();
                            controllerDiskCache.clearCache();
                        }

                        broadcastAction(broadcastContext, null, ACTION_LIKE_ACTION_CONTROLLER_DID_RESET);
                        isPendingBroadcastReset = false;
                    }
                }, 100);
            }
        }, filter);
    }

    private static void putControllerInMemoryCache(String objectId, LikeActionController controllerForObject) {
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
            diskIOWorkQueue.addActiveWorkItem(new SerializeToDiskWorkItem(cacheKey, controllerJson));
        }
    }

    /**
     * NOTE: This MUST be called ONLY via the SerializeToDiskWorkItem class to ensure that it happens on the
     * right thread, at the right time.
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
     * NOTE: This MUST be called ONLY via the CreateLikeActionControllerWorkItem class to ensure that it happens on the
     * right thread, at the right time.
     */
    private static LikeActionController deserializeFromDiskSynchronously(
            Context context,
            String objectId) {
        LikeActionController controller = null;

        InputStream inputStream = null;
        try {
            String cacheKey = getCacheKeyForObjectId(objectId);
            inputStream = controllerDiskCache.get(cacheKey);
            if (inputStream != null) {
                String controllerJsonString = Utility.readStreamToString(inputStream);
                if (!Utility.isNullOrEmpty(controllerJsonString)) {
                    controller = deserializeFromJson(context, controllerJsonString);
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

    private static LikeActionController deserializeFromJson(Context context, String controllerJsonString) {
        LikeActionController controller;

        try {
            JSONObject controllerJson = new JSONObject(controllerJsonString);
            int version = controllerJson.optInt(JSON_INT_VERSION_KEY, -1);
            if (version != LIKE_ACTION_CONTROLLER_VERSION) {
                // Don't attempt to deserialize a controller that might be serialized differently than expected.
                return null;
            }

            controller = new LikeActionController(
                    context,
                    Session.getActiveSession(),
                    controllerJson.getString(JSON_STRING_OBJECT_ID_KEY));

            // Make sure to default to null and not empty string, to keep the logic elsewhere functioning properly.
            controller.likeCountStringWithLike = controllerJson.optString(JSON_STRING_LIKE_COUNT_WITH_LIKE_KEY, null) ;
            controller.likeCountStringWithoutLike = controllerJson.optString(JSON_STRING_LIKE_COUNT_WITHOUT_LIKE_KEY, null) ;
            controller.socialSentenceWithLike = controllerJson.optString(JSON_STRING_SOCIAL_SENTENCE_WITH_LIKE_KEY, null);
            controller.socialSentenceWithoutLike = controllerJson.optString(JSON_STRING_SOCIAL_SENTENCE_WITHOUT_LIKE_KEY, null);
            controller.isObjectLiked = controllerJson.optBoolean(JSON_BOOL_IS_OBJECT_LIKED_KEY);
            controller.unlikeToken = controllerJson.optString(JSON_STRING_UNLIKE_TOKEN_KEY, null);
            String pendingCallIdString = controllerJson.optString(JSON_STRING_PENDING_CALL_ID_KEY, null);
            if (!Utility.isNullOrEmpty(pendingCallIdString)) {
                controller.pendingCallId = UUID.fromString(pendingCallIdString);
            }

            JSONObject analyticsJSON = controllerJson.optJSONObject(JSON_BUNDLE_PENDING_CALL_ANALYTICS_BUNDLE);
            if (analyticsJSON != null) {
                controller.pendingCallAnalyticsBundle = BundleJSONConverter.convertToBundle(analyticsJSON);
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
            controllerJson.put(JSON_STRING_LIKE_COUNT_WITH_LIKE_KEY, controller.likeCountStringWithLike);
            controllerJson.put(JSON_STRING_LIKE_COUNT_WITHOUT_LIKE_KEY, controller.likeCountStringWithoutLike);
            controllerJson.put(JSON_STRING_SOCIAL_SENTENCE_WITH_LIKE_KEY, controller.socialSentenceWithLike);
            controllerJson.put(JSON_STRING_SOCIAL_SENTENCE_WITHOUT_LIKE_KEY, controller.socialSentenceWithoutLike);
            controllerJson.put(JSON_BOOL_IS_OBJECT_LIKED_KEY, controller.isObjectLiked);
            controllerJson.put(JSON_STRING_UNLIKE_TOKEN_KEY, controller.unlikeToken);
            if (controller.pendingCallId != null) {
                controllerJson.put(JSON_STRING_PENDING_CALL_ID_KEY, controller.pendingCallId.toString());
            }
            if (controller.pendingCallAnalyticsBundle != null) {
                JSONObject analyticsJSON = BundleJSONConverter.convertToJSON(controller.pendingCallAnalyticsBundle);
                if (analyticsJSON != null) {
                    controllerJson.put(JSON_BUNDLE_PENDING_CALL_ANALYTICS_BUNDLE, analyticsJSON);
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
        Session activeSession = Session.getActiveSession();
        if (activeSession != null && activeSession.isOpened()) {
            accessTokenPortion = activeSession.getAccessToken();
        }
        if (accessTokenPortion != null) {
            // Cache-key collisions are not something to worry about here, since we only store state for
            // one session. Even in the case where the previous session's serialized files have not been deleted yet,
            // the objectSuffix will be different due to the session-change, thus making the key different.
            accessTokenPortion = Utility.md5hash(accessTokenPortion);
        }
        return String.format(
                "%s|%s|com.fb.sdk.like|%d",
                objectId,
                Utility.coerceValueIfNullOrEmpty(accessTokenPortion, ""),
                objectSuffix);
    }

    //
    // Broadcast handling code
    //

    private static void broadcastAction(Context context, LikeActionController controller, String action) {
        broadcastAction(context, controller, action, null);
    }

    private static void broadcastAction(Context context, LikeActionController controller, String action, Bundle data) {
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
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(broadcastIntent);
    }

    /**
     * Constructor
     */
    private LikeActionController(Context context, Session session, String objectId) {
        this.context = context;
        this.session = session;
        this.objectId = objectId;

        appEventsLogger = AppEventsLogger.newLogger(context, session);
    }

    /**
     * Gets the the associated object id
     * @return object id
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Gets the String representation of the like-count for the associated object
     * @return String representation of the like-count for the associated object
     */
    public String getLikeCountString() {
        return isObjectLiked ? likeCountStringWithLike : likeCountStringWithoutLike;
    }

    /**
     * Gets the String representation of the like-count for the associated object
     * @return String representation of the like-count for the associated object
     */
    public String getSocialSentence() {
        return isObjectLiked ? socialSentenceWithLike : socialSentenceWithoutLike;
    }

    /**
     * Indicates whether the associated object is liked
     * @return Indication of whether the associated object is liked
     */
    public boolean isObjectLiked() {
        return isObjectLiked;
    }

    /**
     * Entry-point to the code that performs the like/unlike action.
     */
    public void toggleLike(Activity activity, Bundle analyticsParameters) {
        appEventsLogger.logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_DID_TAP, null, analyticsParameters);

        boolean shouldLikeObject = !this.isObjectLiked;
        if (canUseOGPublish()) {
            // Update UI state optimistically
            updateState(shouldLikeObject,
                    this.likeCountStringWithLike,
                    this.likeCountStringWithoutLike,
                    this.socialSentenceWithLike,
                    this.socialSentenceWithoutLike,
                    this.unlikeToken);
            if (isPendingLikeOrUnlike) {
                // If the user toggled the button quickly, and there is still a publish underway, don't fire off
                // another request. Also log this behavior.

                appEventsLogger.logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_DID_UNDO_QUICKLY, null, analyticsParameters);
                return;
            }
        }

        performLikeOrUnlike(activity, shouldLikeObject, analyticsParameters);
    }

    private void performLikeOrUnlike(Activity activity, boolean shouldLikeObject, Bundle analyticsParameters) {
        if (canUseOGPublish()) {
            if (shouldLikeObject) {
                publishLikeAsync(activity, analyticsParameters);
            } else {
                if (!Utility.isNullOrEmpty(this.unlikeToken)) {
                    publishUnlikeAsync(activity, analyticsParameters);
                } else {
                    // If we don't have an unlikeToken, we must fall back to the dialog.
                    fallbackToDialog(activity, analyticsParameters, true);
                }
            }
        } else {
            presentLikeDialog(activity, analyticsParameters);
        }
    }

    /**
     * Only to be called after an OG-publish was attempted and something went wrong. The Button state is reverted
     * and the dialog is presented.
     */
    private void fallbackToDialog(
            Activity activity,
            Bundle analyticsParameters,
            boolean oldLikeState) {
        updateState(
                oldLikeState,
                this.likeCountStringWithLike,
                this.likeCountStringWithoutLike,
                this.socialSentenceWithLike,
                this.socialSentenceWithoutLike,
                this.unlikeToken);

        presentLikeDialog(activity, analyticsParameters);
    }

    private void updateState(boolean isObjectLiked,
                             String likeCountStringWithLike,
                             String likeCountStringWithoutLike,
                             String socialSentenceWithLike,
                             String socialSentenceWithoutLike,
                             String unlikeToken) {
        // Normalize all empty strings to null, so that we don't have any problems with comparison.
        likeCountStringWithLike = Utility.coerceValueIfNullOrEmpty(likeCountStringWithLike, null);
        likeCountStringWithoutLike = Utility.coerceValueIfNullOrEmpty(likeCountStringWithoutLike, null);
        socialSentenceWithLike = Utility.coerceValueIfNullOrEmpty(socialSentenceWithLike, null);
        socialSentenceWithoutLike = Utility.coerceValueIfNullOrEmpty(socialSentenceWithoutLike, null);
        unlikeToken = Utility.coerceValueIfNullOrEmpty(unlikeToken, null);

        boolean stateChanged = isObjectLiked != this.isObjectLiked ||
                !Utility.areObjectsEqual(likeCountStringWithLike, this.likeCountStringWithLike) ||
                !Utility.areObjectsEqual(likeCountStringWithoutLike, this.likeCountStringWithoutLike) ||
                !Utility.areObjectsEqual(socialSentenceWithLike, this.socialSentenceWithLike) ||
                !Utility.areObjectsEqual(socialSentenceWithoutLike, this.socialSentenceWithoutLike) ||
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

        broadcastAction(context, this, ACTION_LIKE_ACTION_CONTROLLER_UPDATED);
    }

    private void presentLikeDialog(Activity activity, Bundle analyticsParameters) {
        LikeDialogBuilder likeDialogBuilder = new LikeDialogBuilder(activity, objectId);

        if (likeDialogBuilder.canPresent()) {
            trackPendingCall(likeDialogBuilder.build().present(), analyticsParameters);
            appEventsLogger.logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_DID_PRESENT_DIALOG, null, analyticsParameters);
        } else {
            String webFallbackUrl = likeDialogBuilder.getWebFallbackUrl();
            if (!Utility.isNullOrEmpty(webFallbackUrl)) {
                boolean webFallbackShown = FacebookWebFallbackDialog.presentWebFallback(
                        activity,
                        webFallbackUrl,
                        likeDialogBuilder.getApplicationId(),
                        likeDialogBuilder.getAppCall(),
                        getFacebookDialogCallback(analyticsParameters));
                if (webFallbackShown) {
                    appEventsLogger.logSdkEvent(
                            AnalyticsEvents.EVENT_LIKE_VIEW_DID_PRESENT_FALLBACK, null, analyticsParameters);
                }
            }
        }
    }

    private boolean onActivityResult(int requestCode, int resultCode, Intent data, UUID callId) {
        if (pendingCallId == null || !pendingCallId.equals(callId)) {
            return false;
        }

        // See if we were waiting for a dialog completion
        FacebookDialog.PendingCall pendingCall = PendingCallStore.getInstance().getPendingCallById(pendingCallId);
        if (pendingCall == null) {
            return false;
        }

        // Look for results
        FacebookDialog.handleActivityResult(
                context,
                pendingCall,
                requestCode,
                data,
                getFacebookDialogCallback(pendingCallAnalyticsBundle));

        // The handlers from above will run synchronously. So by the time we get here, it should be safe to
        // stop tracking this call and also serialize the controller to disk
        stopTrackingPendingCall();

        return true;
    }

    private FacebookDialog.Callback getFacebookDialogCallback(final Bundle analyticsParameters) {
        return new FacebookDialog.Callback() {
            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                if (data == null || !data.containsKey(LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY)) {
                    // This is an empty result that we can't handle.
                    return;
                }

                boolean isObjectLiked = data.getBoolean(LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY);

                // Default to known/cached state, if properties are missing.
                String likeCountStringWithLike = LikeActionController.this.likeCountStringWithLike;
                String likeCountStringWithoutLike = LikeActionController.this.likeCountStringWithoutLike;
                if (data.containsKey(LIKE_DIALOG_RESPONSE_LIKE_COUNT_STRING_KEY)) {
                    likeCountStringWithLike = data.getString(LIKE_DIALOG_RESPONSE_LIKE_COUNT_STRING_KEY);
                    likeCountStringWithoutLike = likeCountStringWithLike;
                }

                String socialSentenceWithLike = LikeActionController.this.socialSentenceWithLike;
                String socialSentenceWithoutWithoutLike = LikeActionController.this.socialSentenceWithoutLike;
                if (data.containsKey(LIKE_DIALOG_RESPONSE_SOCIAL_SENTENCE_KEY)) {
                    socialSentenceWithLike = data.getString(LIKE_DIALOG_RESPONSE_SOCIAL_SENTENCE_KEY);
                    socialSentenceWithoutWithoutLike = socialSentenceWithLike;
                }

                String unlikeToken = data.containsKey(LIKE_DIALOG_RESPONSE_OBJECT_IS_LIKED_KEY)
                        ? data.getString(LIKE_DIALOG_RESPONSE_UNLIKE_TOKEN_KEY)
                        : LikeActionController.this.unlikeToken;

                Bundle logParams = (analyticsParameters == null) ? new Bundle() : analyticsParameters;
                logParams.putString(AnalyticsEvents.PARAMETER_CALL_ID, pendingCall.getCallId().toString());
                appEventsLogger.logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_DIALOG_DID_SUCCEED, null, logParams);

                updateState(
                        isObjectLiked,
                        likeCountStringWithLike,
                        likeCountStringWithoutLike,
                        socialSentenceWithLike,
                        socialSentenceWithoutWithoutLike,
                        unlikeToken);
            }

            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Logger.log(LoggingBehavior.REQUESTS, TAG, "Like Dialog failed with error : %s", error);

                Bundle logParams = analyticsParameters == null ? new Bundle() : analyticsParameters;
                logParams.putString(AnalyticsEvents.PARAMETER_CALL_ID, pendingCall.getCallId().toString());

                // Log the error and AppEvent
                logAppEventForError("present_dialog", logParams);

                broadcastAction(context, LikeActionController.this, ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR, data);
            }
        };
    }

    private void trackPendingCall(FacebookDialog.PendingCall pendingCall, Bundle analyticsParameters) {
        PendingCallStore.getInstance().trackPendingCall(pendingCall);

        // Save off the call id for processing the response
        pendingCallId = pendingCall.getCallId();
        storeObjectIdForPendingController(objectId);

        // Store off the analytics parameters as well, for completion-logging
        pendingCallAnalyticsBundle = analyticsParameters;

        // Serialize to disk, in case we get terminated while waiting for the dialog to complete
        serializeToDiskAsync(this);
    }

    private void stopTrackingPendingCall() {
        PendingCallStore.getInstance().stopTrackingPendingCall(pendingCallId);

        pendingCallId = null;
        pendingCallAnalyticsBundle = null;

        storeObjectIdForPendingController(null);
    }

    private void storeObjectIdForPendingController(String objectId) {
        objectIdForPendingController = objectId;
        context.getSharedPreferences(LIKE_ACTION_CONTROLLER_STORE, Context.MODE_PRIVATE)
                .edit()
                .putString(LIKE_ACTION_CONTROLLER_STORE_PENDING_OBJECT_ID_KEY, objectIdForPendingController)
                .apply();
    }

    private boolean canUseOGPublish() {
        // Verify that the object isn't a Page, that we have permissions and that, if we're unliking, then
        // we have an unlike token.
        return !objectIsPage &&
                verifiedObjectId != null &&
                session != null &&
                session.getPermissions() != null &&
                session.getPermissions().contains("publish_actions");
    }

    private void publishLikeAsync(final Activity activity, final Bundle analyticsParameters) {
        isPendingLikeOrUnlike = true;

        fetchVerifiedObjectId(new RequestCompletionCallback() {
            @Override
            public void onComplete() {
                if (Utility.isNullOrEmpty(verifiedObjectId)) {
                    // Could not get a verified id
                    Bundle errorBundle = new Bundle();
                    errorBundle.putString(NativeProtocol.STATUS_ERROR_DESCRIPTION, ERROR_INVALID_OBJECT_ID);

                    broadcastAction(context, LikeActionController.this, ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR, errorBundle);
                    return;
                }

                // Perform the Like.
                RequestBatch requestBatch = new RequestBatch();
                final PublishLikeRequestWrapper likeRequest = new PublishLikeRequestWrapper(verifiedObjectId);
                likeRequest.addToBatch(requestBatch);
                requestBatch.addCallback(new RequestBatch.Callback() {
                    @Override
                    public void onBatchCompleted(RequestBatch batch) {
                        isPendingLikeOrUnlike = false;

                        if (likeRequest.error != null) {
                            // We already updated the UI to show button in the Liked state. Since this failed, let's
                            // revert back to the Unliked state and show the dialog. We need to do this because the
                            // dialog-flow expects the button to only be updated once the dialog returns

                            fallbackToDialog(activity, analyticsParameters, false);
                        } else {
                            unlikeToken = Utility.coerceValueIfNullOrEmpty(likeRequest.unlikeToken, null);
                            isObjectLikedOnServer = true;

                            appEventsLogger.logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_DID_LIKE, null, analyticsParameters);

                            toggleAgainIfNeeded(activity, analyticsParameters);
                        }
                    }
                });

                requestBatch.executeAsync();
            }
        });
    }

    private void publishUnlikeAsync(final Activity activity, final Bundle analyticsParameters) {
        isPendingLikeOrUnlike = true;

        // Perform the Unlike.
        RequestBatch requestBatch = new RequestBatch();
        final PublishUnlikeRequestWrapper unlikeRequest = new PublishUnlikeRequestWrapper(unlikeToken);
        unlikeRequest.addToBatch(requestBatch);
        requestBatch.addCallback(new RequestBatch.Callback() {
            @Override
            public void onBatchCompleted(RequestBatch batch) {
                isPendingLikeOrUnlike = false;

                if (unlikeRequest.error != null) {
                    // We already updated the UI to show button in the Unliked state. Since this failed, let's
                    // revert back to the Liked state and show the dialog. We need to do this because the
                    // dialog-flow expects the button to only be updated once the dialog returns

                    fallbackToDialog(activity, analyticsParameters, true);
                } else {
                    unlikeToken = null;
                    isObjectLikedOnServer = false;

                    appEventsLogger.logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_DID_UNLIKE, null, analyticsParameters);

                    toggleAgainIfNeeded(activity, analyticsParameters);
                }
            }
        });

        requestBatch.executeAsync();
    }

    private void refreshStatusAsync() {
        if (session == null || session.isClosed() || SessionState.CREATED.equals(session.getState())) {
            // Only when we know that there is no active session, or if there is, it is not open OR being opened,
            // should we attempt getting like state from the service. Otherwise, use the access token of the session
            // to make sure we get the correct like state.
            refreshStatusViaService();
            return;
        } else if (!session.isOpened()) {
            // The session might be OPENING. In this case, we don't have an access token yet and
            // cannot make server requests. We hit this code path during login when the
            // Session.ACTION_ACTIVE_SESSION_UNSET broadcast fires, which LikeActionController
            // responds to by resetting.
            return;
        }

        fetchVerifiedObjectId(new RequestCompletionCallback() {
            @Override
            public void onComplete() {
                final GetOGObjectLikesRequestWrapper objectLikesRequest =
                        new GetOGObjectLikesRequestWrapper(verifiedObjectId);
                final GetEngagementRequestWrapper engagementRequest =
                        new GetEngagementRequestWrapper(verifiedObjectId);

                RequestBatch requestBatch = new RequestBatch();
                objectLikesRequest.addToBatch(requestBatch);
                engagementRequest.addToBatch(requestBatch);

                requestBatch.addCallback(new RequestBatch.Callback() {
                    @Override
                    public void onBatchCompleted(RequestBatch batch) {
                        if (objectLikesRequest.error != null ||
                                engagementRequest.error != null) {
                            // Refreshing is best-effort. If the refresh fails, don't lose old state.
                            Logger.log(
                                    LoggingBehavior.REQUESTS,
                                    TAG,
                                    "Unable to refresh like state for id: '%s'", objectId);
                            return;
                        }

                        updateState(
                                objectLikesRequest.objectIsLiked,
                                engagementRequest.likeCountStringWithLike,
                                engagementRequest.likeCountStringWithoutLike,
                                engagementRequest.socialSentenceStringWithLike,
                                engagementRequest.socialSentenceStringWithoutLike,
                                objectLikesRequest.unlikeToken);
                    }
                });

                requestBatch.executeAsync();
            }
        });
    }

    private void refreshStatusViaService() {
        LikeStatusClient likeStatusClient = new LikeStatusClient(
                context,
                Settings.getApplicationId(),
                objectId);
        if (!likeStatusClient.start()) {
            return;
        }

        LikeStatusClient.CompletedListener callback = new LikeStatusClient.CompletedListener() {
            @Override
            public void completed(Bundle result) {
                // Don't lose old state if the service response is incomplete.
                if (result == null || !result.containsKey(NativeProtocol.EXTRA_OBJECT_IS_LIKED)) {
                    return;
                }

                boolean objectIsLiked = result.getBoolean(NativeProtocol.EXTRA_OBJECT_IS_LIKED);

                String likeCountWithLike =
                        result.containsKey(NativeProtocol.EXTRA_LIKE_COUNT_STRING_WITH_LIKE)
                                ? result.getString(NativeProtocol.EXTRA_LIKE_COUNT_STRING_WITH_LIKE)
                                : LikeActionController.this.likeCountStringWithLike;

                String likeCountWithoutLike =
                        result.containsKey(NativeProtocol.EXTRA_LIKE_COUNT_STRING_WITHOUT_LIKE)
                                ? result.getString(NativeProtocol.EXTRA_LIKE_COUNT_STRING_WITHOUT_LIKE)
                                : LikeActionController.this.likeCountStringWithoutLike;

                String socialSentenceWithLike =
                        result.containsKey(NativeProtocol.EXTRA_SOCIAL_SENTENCE_WITH_LIKE)
                                ? result.getString(NativeProtocol.EXTRA_SOCIAL_SENTENCE_WITH_LIKE)
                                : LikeActionController.this.socialSentenceWithLike;

                String socialSentenceWithoutLike =
                        result.containsKey(NativeProtocol.EXTRA_SOCIAL_SENTENCE_WITHOUT_LIKE)
                                ? result.getString(NativeProtocol.EXTRA_SOCIAL_SENTENCE_WITHOUT_LIKE)
                                : LikeActionController.this.socialSentenceWithoutLike;

                String unlikeToken =
                        result.containsKey(NativeProtocol.EXTRA_UNLIKE_TOKEN)
                                ? result.getString(NativeProtocol.EXTRA_UNLIKE_TOKEN)
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

    private void toggleAgainIfNeeded(Activity activity, Bundle analyticsParameters) {
        if (isObjectLiked != isObjectLikedOnServer) {
            performLikeOrUnlike(activity, isObjectLiked, analyticsParameters);
        }
    }

    private void fetchVerifiedObjectId(final RequestCompletionCallback completionHandler) {
        if (!Utility.isNullOrEmpty(verifiedObjectId)) {
            if (completionHandler != null) {
                completionHandler.onComplete();
            }

            return;
        }

        final GetOGObjectIdRequestWrapper objectIdRequest = new GetOGObjectIdRequestWrapper(objectId);
        final GetPageIdRequestWrapper pageIdRequest = new GetPageIdRequestWrapper(objectId);

        RequestBatch requestBatch = new RequestBatch();
        objectIdRequest.addToBatch(requestBatch);
        pageIdRequest.addToBatch(requestBatch);

        requestBatch.addCallback(new RequestBatch.Callback() {
            @Override
            public void onBatchCompleted(RequestBatch batch) {
                verifiedObjectId = objectIdRequest.verifiedObjectId;
                if (Utility.isNullOrEmpty(verifiedObjectId)) {
                    verifiedObjectId = pageIdRequest.verifiedObjectId;
                    objectIsPage = pageIdRequest.objectIsPage;
                }

                if (Utility.isNullOrEmpty(verifiedObjectId)) {
                    Logger.log(LoggingBehavior.DEVELOPER_ERRORS,
                            TAG,
                            "Unable to verify the FB id for '%s'. Verify that it is a valid FB object or page", objectId);
                    logAppEventForError("get_verified_id",
                            pageIdRequest.error != null ? pageIdRequest.error : objectIdRequest.error);
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
        logParams.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_CURRENT_ACTION, action);

        appEventsLogger.logSdkEvent(AnalyticsEvents.EVENT_LIKE_VIEW_ERROR, null, logParams);
    }

    private void logAppEventForError(String action, FacebookRequestError error) {
        Bundle logParams = new Bundle();
        if (error != null) {
            JSONObject requestResult = error.getRequestResult();
            if (requestResult != null) {
                logParams.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_ERROR_JSON, requestResult.toString());
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
        public void onComplete(LikeActionController likeActionController);
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

        GetOGObjectIdRequestWrapper(String objectId) {
            super(objectId);

            Bundle objectIdRequestParams = new Bundle();
            objectIdRequestParams.putString("fields", "og_object.fields(id)");
            objectIdRequestParams.putString("ids", objectId);

            setRequest(new Request(session, "", objectIdRequestParams, HttpMethod.GET));
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
                        "Error getting the FB id for object '%s' : %s", objectId, error);
            }
        }

        @Override
        protected void processSuccess(Response response) {
            JSONObject results = Utility.tryGetJSONObjectFromResponse(response.getGraphObject(), objectId);
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

        GetPageIdRequestWrapper(String objectId) {
            super(objectId);

            Bundle pageIdRequestParams = new Bundle();
            pageIdRequestParams.putString("fields", "id");
            pageIdRequestParams.putString("ids", objectId);

            setRequest(new Request(session, "", pageIdRequestParams, HttpMethod.GET));
        }

        @Override
        protected void processSuccess(Response response) {
            JSONObject results = Utility.tryGetJSONObjectFromResponse(response.getGraphObject(), objectId);
            if (results != null) {
                verifiedObjectId = results.optString("id");
                objectIsPage = !Utility.isNullOrEmpty(verifiedObjectId);
            }
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error getting the FB id for object '%s' : %s", objectId, error);
        }
    }

    private class PublishLikeRequestWrapper extends AbstractRequestWrapper {
        String unlikeToken;

        PublishLikeRequestWrapper(String objectId) {
            super(objectId);

            Bundle likeRequestParams = new Bundle();
            likeRequestParams.putString("object", objectId);

            setRequest(new Request(session, "me/og.likes", likeRequestParams, HttpMethod.POST));
        }

        @Override
        protected void processSuccess(Response response) {
            unlikeToken = Utility.safeGetStringFromResponse(response.getGraphObject(), "id");
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
                        "Error liking object '%s' : %s", objectId, error);
                logAppEventForError("publish_like", error);
            }
        }
    }

    private class PublishUnlikeRequestWrapper extends AbstractRequestWrapper {
        private String unlikeToken;

        PublishUnlikeRequestWrapper(String unlikeToken) {
            super(null);

            this.unlikeToken = unlikeToken;

            setRequest(new Request(session, unlikeToken, null, HttpMethod.DELETE));
        }

        @Override
        protected void processSuccess(Response response) {
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error unliking object with unlike token '%s' : %s", unlikeToken, error);
            logAppEventForError("publish_unlike", error);
        }
    }

    private class GetOGObjectLikesRequestWrapper extends AbstractRequestWrapper {
        // Initialize the like status to what we currently have. This way, empty/error responses don't end
        // up clearing out the state.
        boolean objectIsLiked = LikeActionController.this.isObjectLiked;
        String unlikeToken;

        GetOGObjectLikesRequestWrapper(String objectId) {
            super(objectId);

            Bundle requestParams = new Bundle();
            requestParams.putString("fields", "id,application");
            requestParams.putString("object", objectId);

            setRequest(new Request(session, "me/og.likes", requestParams, HttpMethod.GET));
        }

        @Override
        protected void processSuccess(Response response) {
            JSONArray dataSet = Utility.tryGetJSONArrayFromResponse(response.getGraphObject(), "data");
            if (dataSet != null) {
                for (int i = 0; i < dataSet.length(); i++) {
                    JSONObject data = dataSet.optJSONObject(i);
                    if (data != null) {
                        objectIsLiked = true;
                        JSONObject appData = data.optJSONObject("application");
                        if (appData != null) {
                            if (Utility.areObjectsEqual(session.getApplicationId(), appData.optString("id"))) {
                                unlikeToken = data.optString("id");
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error fetching like status for object '%s' : %s", objectId, error);
            logAppEventForError("get_og_object_like", error);
        }
    }

    private class GetEngagementRequestWrapper extends AbstractRequestWrapper {
        // Initialize the like status to what we currently have. This way, empty/error responses don't end
        // up clearing out the state.
        String likeCountStringWithLike = LikeActionController.this.likeCountStringWithLike;
        String likeCountStringWithoutLike = LikeActionController.this.likeCountStringWithoutLike;
        String socialSentenceStringWithLike = LikeActionController.this.socialSentenceWithLike;
        String socialSentenceStringWithoutLike = LikeActionController.this.socialSentenceWithoutLike;

        GetEngagementRequestWrapper(String objectId) {
            super(objectId);

            Bundle requestParams = new Bundle();
            requestParams.putString(
                    "fields",
                    "engagement.fields(" +
                            "count_string_with_like," +
                            "count_string_without_like," +
                            "social_sentence_with_like," +
                            "social_sentence_without_like)");

            setRequest(new Request(session, objectId, requestParams, HttpMethod.GET));
        }

        @Override
        protected void processSuccess(Response response) {
            JSONObject engagementResults = Utility.tryGetJSONObjectFromResponse(response.getGraphObject(), "engagement");
            if (engagementResults != null) {
                // Missing properties in the response should default to cached like status
                likeCountStringWithLike =
                        engagementResults.optString("count_string_with_like", likeCountStringWithLike);

                likeCountStringWithoutLike =
                        engagementResults.optString("count_string_without_like", likeCountStringWithoutLike);

                socialSentenceStringWithLike =
                        engagementResults.optString("social_sentence_with_like", socialSentenceStringWithLike);

                socialSentenceStringWithoutLike =
                        engagementResults.optString("social_sentence_without_like", socialSentenceStringWithoutLike);
            }
        }

        @Override
        protected void processError(FacebookRequestError error) {
            Logger.log(LoggingBehavior.REQUESTS,
                    TAG,
                    "Error fetching engagement for object '%s' : %s", objectId, error);
            logAppEventForError("get_engagement", error);
        }
    }

    private abstract class AbstractRequestWrapper {
        private Request request;
        protected String objectId;

        FacebookRequestError error;

        protected AbstractRequestWrapper(String objectId) {
            this.objectId = objectId;
        }

        void addToBatch(RequestBatch batch) {
            batch.add(request);
        }

        protected void setRequest(Request request) {
            this.request = request;
            // Make sure that our requests are hitting the latest version of the API known to this sdk.
            request.setVersion(ServerProtocol.GRAPH_API_VERSION);
            request.setCallback(new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
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
                    "Error running request for object '%s' : %s", objectId, error);
        }

        protected abstract void processSuccess(Response response);
    }

    private enum LikeDialogFeature implements FacebookDialog.DialogFeature {

        LIKE_DIALOG(NativeProtocol.PROTOCOL_VERSION_20140701);

        private int minVersion;

        private LikeDialogFeature(int minVersion) {
            this.minVersion = minVersion;
        }

        public String getAction() {
            return NativeProtocol.ACTION_LIKE_DIALOG;
        }

        public int getMinVersion() {
            return minVersion;
        }
    }

    private static class LikeDialogBuilder extends FacebookDialog.Builder<LikeDialogBuilder> {
        private String objectId;

        public LikeDialogBuilder(Activity activity, String objectId) {
            super(activity);

            this.objectId = objectId;
        }

        @Override
        protected EnumSet<? extends FacebookDialog.DialogFeature> getDialogFeatures() {
            return EnumSet.of(LikeDialogFeature.LIKE_DIALOG);
        }

        @Override
        protected Bundle getMethodArguments() {
            Bundle methodArgs = new Bundle();

            methodArgs.putString(NativeProtocol.METHOD_ARGS_OBJECT_ID, objectId);

            return methodArgs;
        }

        public FacebookDialog.PendingCall getAppCall() {
            return appCall;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public String getWebFallbackUrl() {
            return getWebFallbackUrlInternal();
        }
    }

    // Performs cache re-ordering/trimming to keep most-recently-used items up front
    // ** NOTE ** It is expected that only _ONE_ MRUCacheWorkItem is ever running. This is enforced by
    // setting the concurrency of the WorkQueue to 1. Changing the concurrency will most likely lead to errors.
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
        private Context context;
        private String objectId;
        private CreationCallback callback;

        CreateLikeActionControllerWorkItem(Context context, String objectId, CreationCallback callback) {
            this.context = context;
            this.objectId = objectId;
            this.callback = callback;
        }

        @Override
        public void run() {
            createControllerForObjectId(context, objectId, callback);
        }
    }
}
