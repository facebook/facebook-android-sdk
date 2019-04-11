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

package com.facebook.appevents;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.internal.ActivityLifecycleTracker;
import com.facebook.appevents.internal.AutomaticAnalyticsLogger;
import com.facebook.internal.Utility;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * <p>
 * The AppEventsLogger class allows the developer to log various types of events back to Facebook.  In order to log
 * events, the app must create an instance of this class via a {@link #newLogger newLogger} method, and then call
 * the various "log" methods off of that.
 * </p>
 * <p>
 * This client-side event logging is then available through Facebook App Insights
 * and for use with Facebook Ads conversion tracking and optimization.
 * </p>
 * <p>
 * The AppEventsLogger class has a few related roles:
 * </p>
 * <ul>
 * <li>
 * Logging predefined and application-defined events to Facebook App Insights with a
 * numeric value to sum across a large number of events, and an optional set of key/value
 * parameters that define "segments" for this event (e.g., 'purchaserStatus' : 'frequent', or
 * 'gamerLevel' : 'intermediate').  These events may also be used for ads conversion tracking,
 * optimization, and other ads related targeting in the future.
 * </li>
 * <li>
 * Methods that control the way in which events are flushed out to the Facebook servers.
 * </li>
 * </ul>
 * <p>
 * Here are some important characteristics of the logging mechanism provided by AppEventsLogger:
 * <ul>
 * <li>
 * Events are not sent immediately when logged.  They're cached and flushed out to the
 * Facebook servers in a number of situations:
 * <ul>
 * <li>when an event count threshold is passed (currently 100 logged events).</li>
 * <li>when a time threshold is passed (currently 15 seconds).</li>
 * <li>when an app has gone to background and is then brought back to the foreground.</li>
 * </ul>
 * <li>
 * Events will be accumulated when the app is in a disconnected state, and sent when the connection
 * is restored and one of the above 'flush' conditions are met.
 * </li>
 * <li>
 * The AppEventsLogger class is intended to be used from the thread it was created on.  Multiple
 * AppEventsLoggers may be created on other threads if desired.
 * </li>
 * <li>
 * The developer can call the setFlushBehavior method to force the flushing of events to only
 * occur on an explicit call to the `flush` method.
 * </li>
 * <li>
 * The developer can turn on console debug output for event logging and flushing to the server by
 * calling FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
 * </li>
 * </ul>
 * </p>
 * <p>
 * Some things to note when logging events:
 * <ul>
 * <li>
 * There is a limit on the number of unique event names an app can use, on the order of 1000.
 * </li>
 * <li>
 * There is a limit to the number of unique parameter names in the provided parameters that can
 * be used per event, on the order of 25.  This is not just for an individual call, but for all
 * invocations for that eventName.
 * </li>
 * <li>
 * Event names and parameter names must be between 2 and 40
 * characters, and must consist of alphanumeric characters, _, -, or spaces.
 * </li>
 * <li>
 * The length of each parameter value can be no more than on the order of 100 characters.
 * </li>
 * </ul>
 * </p>
 */
public class AppEventsLogger {
    // Enums

    /**
     * Controls when an AppEventsLogger sends log events to the server
     */
    public enum FlushBehavior {
        /**
         * Flush automatically: periodically (every 15 seconds or after every 100 events), and
         * always at app reactivation. This is the default value.
         */
        AUTO,

        /**
         * Only flush when AppEventsLogger.flush() is explicitly invoked.
         */
        EXPLICIT_ONLY,
    }

    /**
     * Product availability for Product Catalog product item update
     */
    public enum ProductAvailability {
        /**
         * Item ships immediately
         */
        IN_STOCK,
        /**
         * No plan to restock
         */
        OUT_OF_STOCK,
        /**
         * Available in future
         */
        PREORDER,
        /**
         * Ships in 1-2 weeks
         */
        AVALIABLE_FOR_ORDER,
        /**
         * Discontinued
         */
        DISCONTINUED,
    }

    /**
     * Product condition for Product Catalog product item update
     */
    public enum ProductCondition {
        NEW,
        REFURBISHED,
        USED,
    }

    // Constants
    private static final String TAG = AppEventsLogger.class.getCanonicalName();

    // TODO: T41629092 remove these constants after migration
    public static final String APP_EVENT_PREFERENCES = "com.facebook.sdk.appEventPreferences";

    private AppEventsLoggerImpl loggerImpl;

    /**
     * Notifies the events system that the app has launched and activate and deactivate events
     * should start being logged automatically. By default this function is called automatically
     * from sdkInitialize() flow. In case 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest
     * setting is set to false, it should typically be called from the OnCreate method
     * of you application.
     *
     * @param application The running application
     */
    public static void activateApp(Application application) {
        AppEventsLoggerImpl.activateApp(application, null);
    }

    /**
     * Notifies the events system that the app has launched and activate and deactivate events
     * should start being logged automatically. By default this function is called automatically
     * from sdkInitialize() flow. In case 'com.facebook.sdk.AutoLogAppEventsEnabled' manifest
     * setting is set to false, it should typically be called from the OnCreate method
     * of you application.
     *
     * Call this if you wish to use a different Application ID then the one specified in the
     * Facebook SDK.
     *
     * @param application The running application
     * @param applicationId The application id used to log activate/deactivate events.
     */
    public static void activateApp(Application application, String applicationId) {
        AppEventsLoggerImpl.activateApp(application, applicationId);
    }

    /**
     * Notifies the events system that the app has launched & logs an activatedApp event.  Should be
     * called whenever your app becomes active, typically in the onResume() method of each
     * long-running Activity of your app.
     * <p/>
     * Use this method if your application ID is stored in application metadata, otherwise see
     * {@link AppEventsLogger#activateApp(android.content.Context, String)}.
     *
     * @param context Used to access the applicationId and the attributionId for non-authenticated
     *                users.
     * @deprecated Use {@link AppEventsLogger#activateApp(Application)}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static void activateApp(Context context) {
        AppEventsLoggerImpl.activateApp(context);
    }

    /**
     * Notifies the events system that the app has launched & logs an activatedApp event.  Should be
     * called whenever your app becomes active, typically in the onResume() method of each
     * long-running Activity of your app.
     *
     * @param context       Used to access the attributionId for non-authenticated users.
     * @param applicationId The specific applicationId to report the activation for.
     * @deprecated Use {@link AppEventsLogger#activateApp(Application)}
     */
    @Deprecated
    public static void activateApp(Context context, String applicationId) {
        AppEventsLoggerImpl.activateApp(context, applicationId);
    }

    /**
     * Notifies the events system that the app has been deactivated (put in the background) and
     * tracks the application session information. Should be called whenever your app becomes
     * inactive, typically in the onPause() method of each long-running Activity of your app.
     *
     * Use this method if your application ID is stored in application metadata, otherwise see
     * {@link AppEventsLogger#deactivateApp(android.content.Context, String)}.
     *
     * @param context Used to access the applicationId and the attributionId for non-authenticated
     *                users.
     * @deprecated When using {@link AppEventsLogger#activateApp(Application)} deactivate app will
     * be logged automatically.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static void deactivateApp(Context context) {
        AppEventsLoggerImpl.deactivateApp(context);
    }

    /**
     * Notifies the events system that the app has been deactivated (put in the background) and
     * tracks the application session information. Should be called whenever your app becomes
     * inactive, typically in the onPause() method of each long-running Activity of your app.
     *
     * @param context       Used to access the attributionId for non-authenticated users.
     * @param applicationId The specific applicationId to track session information for.
     * @deprecated When using {@link AppEventsLogger#activateApp(Application)} deactivate app will
     * be logged automatically.
     */
    @Deprecated
    public static void deactivateApp(Context context, String applicationId) {
        AppEventsLoggerImpl.deactivateApp(context, applicationId);
    }

    /**
     * Notifies the events system which internal SDK Libraries,
     * and some specific external Libraries that the app is utilizing.
     * This is called internally and does NOT need to be called externally.
     *
     * @param context The Context
     * @param applicationId The String applicationId
     */
    public static void initializeLib(Context context, String applicationId) {
        AppEventsLoggerImpl.initializeLib(context, applicationId);
    }

    /**
     * Build an AppEventsLogger instance to log events through.  The Facebook app that these events
     * are targeted at comes from this application's metadata. The application ID used to log events
     * will be determined from the app ID specified in the package metadata.
     *
     * @param context Used to access the applicationId and the attributionId for non-authenticated
     *                users.
     * @return AppEventsLogger instance to invoke log* methods on.
     */
    public static AppEventsLogger newLogger(Context context) {
        return new AppEventsLogger(context, null, null);
    }

    /**
     * Build an AppEventsLogger instance to log events through.
     *
     * @param context Used to access the attributionId for non-authenticated users.
     * @param accessToken Access token to use for logging events. If null, the active access token
     *                    will be used, if any; if not the logging will happen against the default
     *                    app ID specified in the package metadata.
     */
    public static AppEventsLogger newLogger(Context context, AccessToken accessToken) {
        return new AppEventsLogger(context, null, accessToken);
    }

    /**
     * Build an AppEventsLogger instance to log events through.
     *
     * @param context       Used to access the attributionId for non-authenticated users.
     * @param applicationId Explicitly specified Facebook applicationId to log events against.  If
     *                      null, the default app ID specified in the package metadata will be
     *                      used.
     * @param accessToken   Access token to use for logging events. If null, the active access token
     *                      will be used, if any; if not the logging will happen against the default
     *                      app ID specified in the package metadata.
     * @return AppEventsLogger instance to invoke log* methods on.
     */
    public static AppEventsLogger newLogger(
            Context context,
            String applicationId,
            AccessToken accessToken) {
        return new AppEventsLogger(context, applicationId, accessToken);
    }

    /**
     * Build an AppEventsLogger instance to log events that are attributed to the application but
     * not to any particular Session.
     *
     * @param context       Used to access the attributionId for non-authenticated users.
     * @param applicationId Explicitly specified Facebook applicationId to log events against.  If
     *                      null, the default app ID specified in the package metadata will be
     *                      used.
     * @return AppEventsLogger instance to invoke log* methods on.
     */
    public static AppEventsLogger newLogger(Context context, String applicationId) {
        return new AppEventsLogger(context, applicationId, null);
    }

    /**
     * Constructor is private, newLogger() methods should be used to build an instance.
     */
    private AppEventsLogger(Context context, String applicationId, AccessToken accessToken) {
        loggerImpl = new AppEventsLoggerImpl(
                context,
                applicationId,
                accessToken);
    }

    /**
     * The action used to indicate that a flush of app events has occurred. This should
     * be used as an action in an IntentFilter and BroadcastReceiver registered with
     * the {@link android.support.v4.content.LocalBroadcastManager}.
     */
    public static final String ACTION_APP_EVENTS_FLUSHED = "com.facebook.sdk.APP_EVENTS_FLUSHED";

    public static final String APP_EVENTS_EXTRA_NUM_EVENTS_FLUSHED =
            "com.facebook.sdk.APP_EVENTS_NUM_EVENTS_FLUSHED";
    public static final String APP_EVENTS_EXTRA_FLUSH_RESULT =
            "com.facebook.sdk.APP_EVENTS_FLUSH_RESULT";

    /**
     * Access the behavior that AppEventsLogger uses to determine when to flush logged events to the
     * server. This setting applies to all instances of AppEventsLogger.
     *
     * @return Specified flush behavior.
     */
    public static FlushBehavior getFlushBehavior() {
        return AppEventsLoggerImpl.getFlushBehavior();
    }

    /**
     * Set the behavior that this AppEventsLogger uses to determine when to flush logged events to
     * the server. This setting applies to all instances of AppEventsLogger.
     *
     * @param flushBehavior the desired behavior.
     */
    public static void setFlushBehavior(FlushBehavior flushBehavior) {
        AppEventsLoggerImpl.setFlushBehavior(flushBehavior);
    }

    /**
     * Log an app event with the specified name.
     *
     * @param eventName eventName used to denote the event.  Choose amongst the EVENT_NAME_*
     *                  constants in {@link AppEventsConstants} when possible.  Or create your own
     *                  if none of the EVENT_NAME_* constants are applicable. Event names should be
     *                  40 characters or less, alphanumeric, and can include spaces, underscores or
     *                  hyphens, but must not have a space or hyphen as the first character.  Any
     *                  given app should have no more than 1000 distinct event names.
     */
    public void logEvent(String eventName) {
        loggerImpl.logEvent(eventName);
    }

    /**
     * Log an app event with the specified name and the supplied value.
     *
     * @param eventName  eventName used to denote the event.  Choose amongst the EVENT_NAME_*
     *                   constants in {@link AppEventsConstants} when possible.  Or create your own
     *                   if none of the EVENT_NAME_* constants are applicable. Event names should be
     *                   40 characters or less, alphanumeric, and can include spaces, underscores or
     *                   hyphens, but must not have a space or hyphen as the first character.  Any
     *                   given app should have no more than 1000 distinct event names. * @param
     *                   eventName
     * @param valueToSum a value to associate with the event which will be summed up in Insights for
     *                   across all instances of the event, so that average values can be
     *                   determined, etc.
     */
    public void logEvent(String eventName, double valueToSum) {
        loggerImpl.logEvent(eventName, valueToSum);
    }

    /**
     * Log an app event with the specified name and set of parameters.
     *
     * @param eventName  eventName used to denote the event.  Choose amongst the EVENT_NAME_*
     *                   constants in {@link AppEventsConstants} when possible.  Or create your own
     *                   if none of the EVENT_NAME_* constants are applicable. Event names should be
     *                   40 characters or less, alphanumeric, and can include spaces, underscores or
     *                   hyphens, but must not have a space or hyphen as the first character.  Any
     *                   given app should have no more than 1000 distinct event names.
     * @param parameters A Bundle of parameters to log with the event.  Insights will allow looking
     *                   at the logs of these events via different parameter values.  You can log on
     *                   the order of 25 parameters with each distinct eventName.  It's advisable to
     *                   limit the number of unique values provided for each parameter in the
     *                   thousands.  As an example, don't attempt to provide a unique
     *                   parameter value for each unique user in your app.  You won't get meaningful
     *                   aggregate reporting on so many parameter values.  The values in the bundles
     *                   should be Strings or numeric values.
     */
    public void logEvent(String eventName, Bundle parameters) {
        loggerImpl.logEvent(eventName, parameters);
    }

    /**
     * Log an app event with the specified name, supplied value, and set of parameters.
     *
     * @param eventName  eventName used to denote the event.  Choose amongst the EVENT_NAME_*
     *                   constants in {@link AppEventsConstants} when possible.  Or create your own
     *                   if none of the EVENT_NAME_* constants are applicable. Event names should be
     *                   40 characters or less, alphanumeric, and can include spaces, underscores or
     *                   hyphens, but must not have a space or hyphen as the first character.  Any
     *                   given app should have no more than 1000 distinct event names.
     * @param valueToSum a value to associate with the event which will be summed up in Insights for
     *                   across all instances of the event, so that average values can be
     *                   determined, etc.
     * @param parameters A Bundle of parameters to log with the event.  Insights will allow looking
     *                   at the logs of these events via different parameter values.  You can log on
     *                   the order of 25 parameters with each distinct eventName.  It's advisable to
     *                   limit the number of unique values provided for each parameter in the
     *                   thousands.  As an example, don't attempt to provide a unique
     *                   parameter value for each unique user in your app.  You won't get meaningful
     *                   aggregate reporting on so many parameter values.  The values in the bundles
     *                   should be Strings or numeric values.
     */
    public void logEvent(String eventName, double valueToSum, Bundle parameters) {
        loggerImpl.logEvent(eventName, valueToSum, parameters);
    }

    /**
     * Logs a purchase event with Facebook, in the specified amount and with the specified
     * currency.
     *
     * @param purchaseAmount Amount of purchase, in the currency specified by the 'currency'
     *                       parameter. This value will be rounded to the thousandths place (e.g.,
     *                       12.34567 becomes 12.346).
     * @param currency       Currency used to specify the amount.
     */
    public void logPurchase(BigDecimal purchaseAmount, Currency currency) {
        loggerImpl.logPurchase(purchaseAmount, currency);
    }

    /**
     * Logs a purchase event with Facebook explicitly, in the specified amount and with the
     * specified currency. Additional detail about the purchase can be passed in through the
     * parameters bundle.
     * @param purchaseAmount Amount of purchase, in the currency specified by the 'currency'
     *                       parameter. This value will be rounded to the thousandths place (e.g.,
     *                       12.34567 becomes 12.346).
     * @param currency       Currency used to specify the amount.
     * @param parameters     Arbitrary additional information for describing this event. This should
     *                       have no more than 24 entries, and keys should be mostly consistent from
     *                       one purchase event to the next.
     */
    public void logPurchase(
            BigDecimal purchaseAmount, Currency currency, Bundle parameters) {
        loggerImpl.logPurchase(purchaseAmount, currency, parameters);
    }

    /**
     *@deprecated Use {@link
     *  AppEventsLogger#logPurchase(
     *  java.math.BigDecimal, java.util.Currency, android.os.Bundle)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public void logPurchaseImplicitly(
            BigDecimal purchaseAmount, Currency currency, Bundle parameters) {
        String errMsg = "Function logPurchaseImplicitly() is deprecated " +
                "and your purchase events cannot be logged with this function. ";

        if (AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()) {
            errMsg += "Auto-logging of in-app purchase has been enabled in the SDK, " +
                    "so you don't have to manually log purchases";
        } else {
            errMsg += "Please use logPurchase() function instead.";
        }

        Log.e(TAG, errMsg);
    }

    /**
     * Logs an app event that tracks that the application was open via Push Notification.
     * @param payload Notification payload received.
     */
    public void logPushNotificationOpen(Bundle payload) {
        loggerImpl.logPushNotificationOpen(payload, null);
    }

    /**
     * Logs an app event that tracks that the application was open via Push Notification.
     * @param payload Notification payload received.
     */
    public void logPushNotificationOpen(Bundle payload, String action) {
        loggerImpl.logPushNotificationOpen(payload, action);
    }

    /**
     * Uploads product catalog product item as an app event.
     * @param itemID            Unique ID for the item. Can be a variant for a product.
     *                          Max size is 100.
     * @param availability      If item is in stock. Accepted values are:
     *                              in stock - Item ships immediately
     *                              out of stock - No plan to restock
     *                              preorder - Available in future
     *                              available for order - Ships in 1-2 weeks
     *                              discontinued - Discontinued
     * @param condition         Product condition: new, refurbished or used.
     * @param description       Short text describing product. Max size is 5000.
     * @param imageLink         Link to item image used in ad.
     * @param link              Link to merchant's site where someone can buy the item.
     * @param title             Title of item.
     * @param priceAmount       Amount of purchase, in the currency specified by the 'currency'
     *                          parameter. This value will be rounded to the thousandths place
     *                          (e.g., 12.34567 becomes 12.346).
     * @param currency          Currency used to specify the amount.
     * @param gtin              Global Trade Item Number including UPC, EAN, JAN and ISBN
     * @param mpn               Unique manufacture ID for product
     * @param brand             Name of the brand
     *                          Note: Either gtin, mpn or brand is required.
     * @param parameters        Optional fields for deep link specification.
     */
    public void logProductItem(
            String itemID,
            ProductAvailability availability,
            ProductCondition condition,
            String description,
            String imageLink,
            String link,
            String title,
            BigDecimal priceAmount,
            Currency currency,
            String gtin,
            String mpn,
            String brand,
            Bundle parameters
    ) {
        loggerImpl.logProductItem(
                itemID,
                availability,
                condition,
                description,
                imageLink,
                link,
                title,
                priceAmount,
                currency,
                gtin,
                mpn,
                brand,
                parameters
        );
    }

    /**
     * Explicitly flush any stored events to the server.  Implicit flushes may happen depending on
     * the value of getFlushBehavior.  This method allows for explicit, app invoked flushing.
     */
    public void flush() {
        loggerImpl.flush();
    }

    /**
     * Call this when the consuming Activity/Fragment receives an onStop() callback in order to
     * persist any outstanding events to disk so they may be flushed at a later time. The next
     * flush (explicit or not) will check for any outstanding events and if present, include them
     * in that flush. Note that this call may trigger an I/O operation on the calling thread.
     * Explicit use of this method is necessary.
     */
    public static void onContextStop() {
        AppEventsLoggerImpl.onContextStop();
    }

    /**
     * Determines if the logger is valid for the given access token.
     * @param accessToken The access token to check.
     * @return True if the access token is valid for this logger.
     */
    public boolean isValidForAccessToken(AccessToken accessToken) {
        return loggerImpl.isValidForAccessToken(accessToken);
    }

    /**
     * Sets and sends registration id to register the current app for push notifications.
     * @param registrationId RegistrationId received from FCM.
     */
    public static void setPushNotificationsRegistrationId(String registrationId) {
        AppEventsLoggerImpl.setPushNotificationsRegistrationId(registrationId);
    }

    /**
     *  Intended to be used as part of a hybrid webapp.
     *  If you call this method, the FB SDK will add a new JavaScript interface into your webview.
     *  If the FB Pixel is used within the webview, and references the app ID of this app,  then it
     *  will detect the presence of this injected JavaScript object and pass Pixel events back to
     *  the FB SDK for logging using the AppEvents framework.
     *
     * @param webView The webview to augment with the additional JavaScript behaviour
     * @param context Used to access the applicationId and the attributionId for non-authenticated
     *                users.
     */
    public static void augmentWebView(WebView webView, Context context) {
        AppEventsLoggerImpl.augmentWebView(webView, context);
    }

    /**
     * Sets a user id to associate with all app events. This can be used to associate your own
     * user id with the app events logged from this instance of an application.
     *
     * The user ID will be persisted between application instances.
     *
     * @param userID A User ID
     */
    public static void setUserID(final String userID) {
        AnalyticsUserIDStore.setUserID(userID);
    }

    /**
     * Returns the set user id else null.
     */
    public static String getUserID() {
        return AnalyticsUserIDStore.getUserID();
    }

    /**
     * Clears the currently set user id.
     */
    public static void clearUserID() {
        AnalyticsUserIDStore.setUserID(null);
    }

    /**
     * Sets user data to associate with all app events. All user data are hashed and used to
     * match Facebook user from this instance of an application.
     *
     * The user data will be persisted between application instances.
     *
     * @param userData user data to identify the user. User data should be formated as a bundle of
     *                 data type name and value. Supported data types and names are:
     *                 Email: em
     *                 First Name: fn
     *                 Last Name: ln
     *                 Phone: ph
     *                 Date of Birth: db
     *                 Gender: ge
     *                 City: ct
     *                 State: st
     *                 Zip: zp
     *                 Country: country
     */
    @Deprecated
    public static void setUserData(final Bundle userData) {
        UserDataStore.setUserDataAndHash(userData);
    }

    /**
     * Sets user data to associate with all app events. All user data are hashed and used to
     * match Facebook user from this instance of an application.
     *
     * The user data will be persisted between application instances.
     *
     * @param email user's email
     * @param firstName user's first name
     * @param lastName user's last name
     * @param phone  user's phone
     * @param dateOfBirth user's date of birth
     * @param gender user's gender
     * @param city user's city
     * @param state user's state
     * @param zip user's zip
     * @param country user's country
     */
    public static void setUserData(
            @Nullable final String email,
            @Nullable final String firstName,
            @Nullable final String lastName,
            @Nullable final String phone,
            @Nullable final String dateOfBirth,
            @Nullable final String gender,
            @Nullable final String city,
            @Nullable final String state,
            @Nullable final String zip,
            @Nullable final String country) {
        UserDataStore.setUserDataAndHash(
                email,
                firstName,
                lastName,
                phone,
                dateOfBirth,
                gender,
                city,
                state,
                zip,
                country);
    }

    /**
     * Returns the set user data else null.
     */
    public static String getUserData() {
        return UserDataStore.getHashedUserData();
    }

    /**
     * Clears the current user data
     */
    public static void clearUserData() {
        UserDataStore.clear();
    }

    public static void updateUserProperties(
            Bundle parameters,
            GraphRequest.Callback callback) {
        updateUserProperties(
                parameters,
                FacebookSdk.getApplicationId(),
                callback);
    }

    public static void updateUserProperties(
            final Bundle parameters,
            final String applicationID,
            final GraphRequest.Callback callback) {
        AppEventsLoggerImpl.updateUserProperties(
                parameters,
                applicationID,
                callback);
    }

    /**
     * This method is only for internal and use by the Facebook SDK account kit
     * for legacy reason. Other usage is not allowed.
     */
    @Deprecated
    public void logSdkEvent(String eventName, Double valueToSum, Bundle parameters) {
        loggerImpl.logSdkEvent(
                eventName,
                valueToSum,
                parameters);
    }

    /**
     * Returns the app ID this logger was configured to log to.
     *
     * @return the Facebook app ID
     */
    public String getApplicationId() {
        return loggerImpl.getApplicationId();
    }

    /**
     * Each app/device pair gets an GUID that is sent back with App Events and persisted with this
     * app/device pair.
     * @param context The application context.
     * @return The GUID for this app/device pair.
     */
    public static String getAnonymousAppDeviceGUID(Context context) {
        return AppEventsLoggerImpl.getAnonymousAppDeviceGUID(context);
    }
}
