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

package com.facebook;

import android.content.Context;
import android.os.Bundle;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.model.GraphObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Set;

/**
 * The InsightsLogger class allows the developer to log various types of events back to Facebook.  In order to log
 * events, the app must create an instance of this class via a {@link #newLogger newLogger} method, and then call
 * the various "log" methods off of that.  Note that a Client Token for the app is required in calls to newLogger so
 * apps that have not authenticated their users can still get meaningful user-demographics from the logged events
 * on Facebook.
 */
public class InsightsLogger {

    // Constants

    // Event names, these match what the server expects.
    private static final String EVENT_NAME_LOG_CONVERSION_PIXEL = "fb_log_offsite_pixel";
    private static final String EVENT_NAME_LOG_MOBILE_PURCHASE  = "fb_mobile_purchase";

    // Event parameter names, these match what the server expects.
    private static final String EVENT_PARAMETER_CURRENCY        = "fb_currency";
    private static final String EVENT_PARAMETER_PIXEL_ID        = "fb_offsite_pixel_id";
    private static final String EVENT_PARAMETER_PIXEL_VALUE     = "fb_offsite_pixel_value";

    // Static member variables
    private static Session appAuthSession = null;

    // Instance member variables
    private final Context context;
    private final String  clientToken;
    private final String  applicationId;
    private final Session specifiedSession;


    /**
     * Constructor is private, newLogger() methods should be used to build an instance.
     */
    private InsightsLogger(Context context, String clientToken, String applicationId, Session session) {

        Validate.notNull(context, "context");

        // Always ensure the client token is present, even if not needed for this particular logging (because at
        // some point it will be required).  Be harsh by throwing an exception because this is all too easy to miss
        // and things will work with authenticated sessions, but start failing with users that don't have
        // authenticated sessions.
        Validate.notNullOrEmpty(clientToken, "clientToken");

        if (applicationId == null) {
            applicationId = Utility.getMetadataApplicationId(context);
        }

        this.context = context;
        this.clientToken = clientToken;
        this.applicationId = applicationId;
        this.specifiedSession = session;
    }

    /**
     * Build an InsightsLogger instance to log events through.  The Facebook app that these events are targeted at
     * comes from this application's metadata.
     *
     * @param context      Used to access the applicationId and the attributionId for non-authenticated users.
     * @param clientToken  The Facebook app's "client token", which, for a given appid can be found in the Security
     *                     section of the Advanced tab of the Facebook App settings found
     *                     at <https://developers.facebook.com/apps/[your-app-id]>.
     *
     * @return          InsightsLogger instance to invoke log* methods on.
     */
    public static InsightsLogger newLogger(Context context, String clientToken) {
        return new InsightsLogger(context, clientToken, null, null);
    }

    /**
     * Build an InsightsLogger instance to log events through.  Allow explicit specification of an Facebook app
     * to target.
     *
     * @param context        Used to access the attributionId for non-authenticated users.
     * @param clientToken    The Facebook app's "client token", which, for a given appid can be found in the Security
     *                       section of the Advanced tab of the Facebook App settings found
     *                       at <https://developers.facebook.com/apps/[your-app-id]>
     * @param applicationId  Explicitly specified Facebook applicationId to log events against.  If null, the
     *                       applicationId embedded in the application metadata accessible from 'context' will
     *                       be used.
     *
     * @return          InsightsLogger instance to invoke log* methods on.
     */
    public static InsightsLogger newLogger(Context context, String clientToken, String applicationId) {
        return new InsightsLogger(context, clientToken, applicationId, null);
    }

    /**
     * Build an InsightsLogger instance to log events through.
     *
     * @param context        Used to access the attributionId for non-authenticated users.
     * @param clientToken    The Facebook app's "client token", which, for a given appid can be found in the Security
     *                       section of the Advanced tab of the Facebook App settings found
     *                       at <https://developers.facebook.com/apps/[your-app-id]>
     * @param applicationId  Explicitly specified Facebook applicationId to log events against.  If null, the
     *                       applicationId embedded in the application metadata accessible from 'context' will
     *                       be used.
     * @param session        Explicitly specified Session to log events against.  If null, the activeSession
     *                       will be used if it's open, otherwise the logging will happen via the "clientToken"
     *                       and specified appId.
     *
     * @return          InsightsLogger instance to invoke log* methods on.
     */
    public static InsightsLogger newLogger(Context context, String clientToken, String applicationId, Session session) {
        return new InsightsLogger(context, clientToken, applicationId, session);
    }

    /**
     * Logs a purchase event with Facebook, in the specified amount and with the specified currency.
     *
     * @param purchaseAmount  Amount of purchase, in the currency specified by the 'currency' parameter. This value
     *                        will be rounded to the thousandths place (e.g., 12.34567 becomes 12.346).
     * @param currency        Currency used to specify the amount.
     */
    public void logPurchase(BigDecimal purchaseAmount, Currency currency) {
        logPurchase(purchaseAmount, currency, null);
    }

    /**
     * Logs a purchase event with Facebook, in the specified amount and with the specified currency.  Additional
     * detail about the purchase can be passed in through the parameters bundle.
     *
     * @param purchaseAmount  Amount of purchase, in the currency specified by the 'currency' parameter. This value
     *                        will be rounded to the thousandths place (e.g., 12.34567 becomes 12.346).
     * @param currency        Currency used to specify the amount.
     * @param parameters      Arbitrary additional information for describing this event.  Should have no more than
     *                        10 entries, and keys should be mostly consistent from one purchase event to the next.
     */
    public void logPurchase(BigDecimal purchaseAmount, Currency currency, Bundle parameters) {

        if (purchaseAmount == null) {
            notifyDeveloperError("purchaseAmount cannot be null");
            return;
        } else if (currency == null) {
            notifyDeveloperError("currency cannot be null");
            return;
        }

        if (parameters == null) {
            parameters = new Bundle();
        }
        parameters.putString(EVENT_PARAMETER_CURRENCY, currency.getCurrencyCode());

        logEventNow(EVENT_NAME_LOG_MOBILE_PURCHASE, purchaseAmount.doubleValue(), parameters);
    }

    /**
     * Log, or "Fire" a Conversion Pixel.  Conversion Pixels are used for Ads Conversion Tracking.  See
     * https://www.facebook.com/help/435189689870514 to learn more.
     *
     * @param pixelId      Numeric ID for the conversion pixel to be logged.  See
     *                     https://www.facebook.com/help/435189689870514 to learn how to create a conversion pixel.
     * @param valueOfPixel Value of what the logging of this pixel is worth to the calling app.  The currency that this
     *                     is expressed in doesn't matter, so long as it is consistent across all logging for this
     *                     pixel. This value will be rounded to the thousandths place (e.g., 12.34567 becomes 12.346).
     */
    public void logConversionPixel(String pixelId, double valueOfPixel) {

        if (pixelId == null) {
            notifyDeveloperError("pixelID cannot be null");
            return;
        }

        Bundle parameters = new Bundle();
        parameters.putString(EVENT_PARAMETER_PIXEL_ID, pixelId);
        parameters.putDouble(EVENT_PARAMETER_PIXEL_VALUE, valueOfPixel);

        logEventNow(EVENT_NAME_LOG_CONVERSION_PIXEL, valueOfPixel, parameters);
    }

    /**
     * This is the workhorse function of the InsightsLogger class and does the packaging and POST.  As InsightsLogger
     * is expanded to support more custom app events, this logic will become more complicated and allow for batching
     * and flushing of multiple events, of persisting to disk so as to survive network outages, implicitly logging
     * (with the dev's permission) SDK actions, etc.
     */
    private void logEventNow(
            final String eventName,
            final double valueToSum,
            final Bundle parameters) {

        // Run everything synchronously on a worker thread.
        Settings.getExecutor().execute(new Runnable() {

            @Override
            public void run() {

                final String eventJSON = buildJSONForEvent(eventName, valueToSum, parameters);
                if (eventJSON == null) {
                    // Failure in building JSON, already reported, so just return.
                    return;
                }

                GraphObject publishParams = GraphObject.Factory.create();
                publishParams.setProperty("event", "CUSTOM_APP_EVENTS");
                publishParams.setProperty("custom_events", eventJSON);

                if (Utility.queryAppAttributionSupportAndWait(applicationId)) {
                    String attributionId = Settings.getAttributionId(context.getContentResolver());
                    if (attributionId != null) {
                        publishParams.setProperty("attribution", attributionId);
                    }
                }

                String publishUrl = String.format("%s/activities", applicationId);

                try {

                    Request postRequest = Request.newPostRequest(sessionToLogTo(), publishUrl, publishParams, null);
                    Response response = postRequest.executeAndWait();

                    // A -1 error code happens if there is no connectivity.  No need to notify the
                    // developer in that case.
                    final int NO_CONNECTIVITY_ERROR_CODE = -1;
                    if (response.getError() != null &&
                        response.getError().getErrorCode() != NO_CONNECTIVITY_ERROR_CODE) {
                        notifyDeveloperError(
                                String.format(
                                        "Error publishing Insights event '%s'\n  Response: %s\n  Error: %s",
                                        eventJSON,
                                        response.toString(),
                                        response.getError().toString()));
                    }

                } catch (Exception e) {

                    Utility.logd("Insights-exception: ", e);

                }
            }
        });

    }

    private static String buildJSONForEvent(String eventName, double valueToSum, Bundle parameters) {
        String result;
        try {

            // Build custom event payload
            JSONObject eventObject = new JSONObject();
            eventObject.put("_eventName", eventName);
            if (valueToSum != 1.0) {
                eventObject.put("_valueToSum", valueToSum);
            }

            if (parameters != null) {

                Set<String> keys = parameters.keySet();
                for (String key : keys) {
                    Object value = parameters.get(key);

                    if (!(value instanceof String) &&
                        !(value instanceof Number)) {

                        notifyDeveloperError(
                                String.format("Parameter '%s' must be a string or a numeric type.", key));
                    }

                    eventObject.put(key, value);
                }
            }

            JSONArray eventArray = new JSONArray();
            eventArray.put(eventObject);

            result = eventArray.toString();

        } catch (JSONException exception) {

            notifyDeveloperError(exception.toString());
            result = null;

        }

        return result;
    }

    /**
     * Using the specifiedSession member variable (which may be nil), find the real session to log to
     * (with an access token).  Precedence: 1) specified session, 2) activeSession, 3) app authenticated
     * session via Client Token.
     */
    private Session sessionToLogTo() {

        synchronized (this) {

            Session session = specifiedSession;

            // Require an open session.

            if (session == null || !session.isOpened()) {
                session = Session.getActiveSession();
            }

            if (session == null || !session.isOpened() || session.getAccessToken() == null) {

                if (appAuthSession == null) {

                    // Build and stash a client-token based session.

                    // Form the clientToken based access token from appID and client token.
                    String tokenString = String.format("%s|%s", applicationId, clientToken);
                    AccessToken token = AccessToken.createFromString(tokenString, null, AccessTokenSource.CLIENT_TOKEN);

                    appAuthSession = new Session(null, applicationId, new NonCachingTokenCachingStrategy(), false);
                    appAuthSession.open(token, null);
                }

                session = appAuthSession;
            }

            return session;
        }
    }

    /**
     * Invoke this method, rather than throwing an Exception, for situations where user/server input might reasonably
     * cause this to occur, and thus don't want an exception thrown at production time, but do want logging
     * notification.
     */
    private static void notifyDeveloperError(String message) {
        Logger.log(LoggingBehavior.DEVELOPER_ERRORS, "Insights", message);
    }
}
