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

package com.facebook;

import com.facebook.internal.FacebookRequestErrorClassification;
import com.facebook.internal.Utility;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class represents an error that occurred during a Facebook request.
 * <p/>
 * In general, one would call {@link #getCategory()} to determine the type
 * of error that occurred, and act accordingly. For more information on error
 * handling, see <a href="https://developers.facebook.com/docs/reference/api/errors/">
 * https://developers.facebook.com/docs/reference/api/errors/</a>
 */
public final class FacebookRequestError {

    /** Represents an invalid or unknown error code from the server. */
    public static final int INVALID_ERROR_CODE = -1;

    /**
     * Indicates that there was no valid HTTP status code returned, indicating
     * that either the error occurred locally, before the request was sent, or
     * that something went wrong with the HTTP connection. Check the exception
     * from {@link #getException()};
     */
    public static final int INVALID_HTTP_STATUS_CODE = -1;

    private static final String CODE_KEY = "code";
    private static final String BODY_KEY = "body";
    private static final String ERROR_KEY = "error";
    private static final String ERROR_TYPE_FIELD_KEY = "type";
    private static final String ERROR_CODE_FIELD_KEY = "code";
    private static final String ERROR_MESSAGE_FIELD_KEY = "message";
    private static final String ERROR_CODE_KEY = "error_code";
    private static final String ERROR_SUB_CODE_KEY = "error_subcode";
    private static final String ERROR_MSG_KEY = "error_msg";
    private static final String ERROR_REASON_KEY = "error_reason";
    private static final String ERROR_USER_TITLE_KEY = "error_user_title";
    private static final String ERROR_USER_MSG_KEY = "error_user_msg";
    private static final String ERROR_IS_TRANSIENT_KEY = "is_transient";

    private static class Range {
        private final int start, end;

        private Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        boolean contains(int value) {
            return start <= value && value <= end;
        }
    }

    static final Range HTTP_RANGE_SUCCESS = new Range(200, 299);

    private final Category category;
    private final int requestStatusCode;
    private final int errorCode;
    private final int subErrorCode;
    private final String errorType;
    private final String errorMessage;
    private final String errorUserTitle;
    private final String errorUserMessage;
    private final String errorRecoveryMessage;
    private final JSONObject requestResult;
    private final JSONObject requestResultBody;
    private final Object batchRequestResult;
    private final HttpURLConnection connection;
    private final FacebookException exception;

    private FacebookRequestError(
            int requestStatusCode,
            int errorCode,
            int subErrorCode,
            String errorType,
            String errorMessage,
            String errorUserTitle,
            String errorUserMessage,
            boolean errorIsTransient,
            JSONObject requestResultBody,
            JSONObject requestResult,
            Object batchRequestResult,
            HttpURLConnection connection,
            FacebookException exception) {
        this.requestStatusCode = requestStatusCode;
        this.errorCode = errorCode;
        this.subErrorCode = subErrorCode;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.requestResultBody = requestResultBody;
        this.requestResult = requestResult;
        this.batchRequestResult = batchRequestResult;
        this.connection = connection;
        this.errorUserTitle = errorUserTitle;
        this.errorUserMessage = errorUserMessage;

        boolean isLocalException = false;
        if (exception != null) {
            this.exception = exception;
            isLocalException =  true;
        } else {
            this.exception = new FacebookServiceException(this, errorMessage);
        }

        FacebookRequestErrorClassification errorClassification = getErrorClassification();
        this.category = isLocalException
                ? Category.OTHER
                : errorClassification.classify(errorCode, subErrorCode, errorIsTransient);
        this.errorRecoveryMessage = errorClassification.getRecoveryMessage(this.category);
    }

    FacebookRequestError(HttpURLConnection connection, Exception exception) {
        this(
                INVALID_HTTP_STATUS_CODE,
                INVALID_ERROR_CODE,
                INVALID_ERROR_CODE,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                connection,
                (exception instanceof FacebookException) ?
                        (FacebookException) exception : new FacebookException(exception));
    }

    public FacebookRequestError(int errorCode, String errorType, String errorMessage) {
        this(
                INVALID_HTTP_STATUS_CODE,
                errorCode,
                INVALID_ERROR_CODE,
                errorType,
                errorMessage,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Returns the category in which the error belongs. Applications can use the category
     * to determine how best to handle the errors (e.g. exponential backoff for retries if
     * being throttled).
     *
     * @return the category in which the error belong
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Returns the HTTP status code for this particular request.
     *
     * @return the HTTP status code for the request
     */
    public int getRequestStatusCode() {
        return requestStatusCode;
    }

    /**
     * Returns the error code returned from Facebook.
     *
     * @return the error code returned from Facebook
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the sub-error code returned from Facebook.
     *
     * @return the sub-error code returned from Facebook
     */
    public int getSubErrorCode() {
        return subErrorCode;
    }

    /**
     * Returns the type of error as a raw string. This is generally less useful
     * than using the {@link #getCategory()} method, but can provide further details
     * on the error.
     *
     * @return the type of error as a raw string
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Returns the error message returned from Facebook.
     *
     * @return the error message returned from Facebook
     */
    public String getErrorMessage() {
        if (errorMessage != null) {
            return errorMessage;
        } else {
            return exception.getLocalizedMessage();
        }
    }

    /**
     * Returns the message that can be displayed to the user before attempting error recovery.
     * @return the message that can be displayed to the user before attempting error recovery
     */
    public String getErrorRecoveryMessage() {
        return this.errorRecoveryMessage;
    }

    /**
     * Returns a message suitable for display to the user, describing a user action necessary to
     * enable Facebook functionality. Not all Facebook errors yield a message suitable for user
     * display; however in all cases where shouldNotifyUser() returns true, this method returns a
     * non-null message suitable for display.
     *
     * @return the error message returned from Facebook
     */
    public String getErrorUserMessage() {
        return errorUserMessage;
    }

    /**
     * Returns a short summary of the error suitable for display to the user. Not all Facebook
     * errors yield a title/message suitable for user display; however in all cases where
     * getErrorUserTitle() returns valid String - user should be notified.
     *
     * @return the error message returned from Facebook
     */
    public String getErrorUserTitle() {
        return errorUserTitle;
    }

    /**
     * Returns the body portion of the response corresponding to the request from Facebook.
     *
     * @return the body of the response for the request
     */
    public JSONObject getRequestResultBody() {
        return requestResultBody;
    }

    /**
     * Returns the full JSON response for the corresponding request. In a non-batch request,
     * this would be the raw response in the form of a JSON object. In a batch request, this
     * result will contain the body of the response as well as the HTTP headers that pertain
     * to the specific request (in the form of a "headers" JSONArray).
     *
     * @return the full JSON response for the request
     */
    public JSONObject getRequestResult() {
        return requestResult;
    }

    /**
     * Returns the full JSON response for the batch request. If the request was not a batch
     * request, then the result from this method is the same as {@link #getRequestResult()}.
     * In case of a batch request, the result will be a JSONArray where the elements
     * correspond to the requests in the batch. Callers should check the return type against
     * either JSONObject or JSONArray and cast accordingly.
     *
     * @return the full JSON response for the batch
     */
    public Object getBatchRequestResult() {
        return batchRequestResult;
    }

    /**
     * Returns the HTTP connection that was used to make the request.
     *
     * @return the HTTP connection used to make the request
     */
    public HttpURLConnection getConnection() {
        return connection;
    }

    /**
     * Returns the exception associated with this request, if any.
     *
     * @return the exception associated with this request
     */
    public FacebookException getException() {
        return exception;
    }

    @Override
    public String toString() {
        return new StringBuilder("{HttpStatus: ")
                .append(requestStatusCode)
                .append(", errorCode: ")
                .append(errorCode)
                .append(", errorType: ")
                .append(errorType)
                .append(", errorMessage: ")
                .append(getErrorMessage())
                .append("}")
                .toString();
    }

    static FacebookRequestError checkResponseAndCreateError(
            JSONObject singleResult,
            Object batchResult,
            HttpURLConnection connection) {
        try {
            if (singleResult.has(CODE_KEY)) {
                int responseCode = singleResult.getInt(CODE_KEY);
                Object body = Utility.getStringPropertyAsJSON(singleResult, BODY_KEY,
                        GraphResponse.NON_JSON_RESPONSE_PROPERTY);

                if (body != null && body instanceof JSONObject) {
                    JSONObject jsonBody = (JSONObject) body;
                    // Does this response represent an error from the service? We might get either
                    // an "error" with several sub-properties, or else one or more top-level fields
                    // containing error info.
                    String errorType = null;
                    String errorMessage = null;
                    String errorUserMessage = null;
                    String errorUserTitle = null;
                    boolean errorIsTransient = false;
                    int errorCode = INVALID_ERROR_CODE;
                    int errorSubCode = INVALID_ERROR_CODE;

                    boolean hasError = false;
                    if (jsonBody.has(ERROR_KEY)) {
                        // We assume the error object is correctly formatted.
                        JSONObject error = (JSONObject)
                                Utility.getStringPropertyAsJSON(jsonBody, ERROR_KEY, null);

                        errorType = error.optString(ERROR_TYPE_FIELD_KEY, null);
                        errorMessage = error.optString(ERROR_MESSAGE_FIELD_KEY, null);
                        errorCode = error.optInt(ERROR_CODE_FIELD_KEY, INVALID_ERROR_CODE);
                        errorSubCode = error.optInt(ERROR_SUB_CODE_KEY, INVALID_ERROR_CODE);
                        errorUserMessage =  error.optString(ERROR_USER_MSG_KEY, null);
                        errorUserTitle =  error.optString(ERROR_USER_TITLE_KEY, null);
                        errorIsTransient = error.optBoolean(ERROR_IS_TRANSIENT_KEY, false);
                        hasError = true;
                    } else if (jsonBody.has(ERROR_CODE_KEY) || jsonBody.has(ERROR_MSG_KEY)
                            || jsonBody.has(ERROR_REASON_KEY)) {
                        errorType = jsonBody.optString(ERROR_REASON_KEY, null);
                        errorMessage = jsonBody.optString(ERROR_MSG_KEY, null);
                        errorCode = jsonBody.optInt(ERROR_CODE_KEY, INVALID_ERROR_CODE);
                        errorSubCode = jsonBody.optInt(ERROR_SUB_CODE_KEY, INVALID_ERROR_CODE);
                        hasError = true;
                    }

                    if (hasError) {
                        return new FacebookRequestError(
                                responseCode,
                                errorCode,
                                errorSubCode,
                                errorType,
                                errorMessage,
                                errorUserTitle,
                                errorUserMessage,
                                errorIsTransient,
                                jsonBody,
                                singleResult,
                                batchResult,
                                connection,
                                null);
                    }
                }

                // If we didn't get error details, but we did get a failure response code, report
                // it.
                if (!HTTP_RANGE_SUCCESS.contains(responseCode)) {
                    return new FacebookRequestError(
                            responseCode,
                            INVALID_ERROR_CODE,
                            INVALID_ERROR_CODE,
                            null,
                            null,
                            null,
                            null,
                            false,
                            singleResult.has(BODY_KEY) ?
                                    (JSONObject) Utility.getStringPropertyAsJSON(
                                            singleResult,
                                            BODY_KEY,
                                            GraphResponse.NON_JSON_RESPONSE_PROPERTY
                                    ) : null,
                            singleResult,
                            batchResult,
                            connection,
                            null);
                }
            }
        } catch (JSONException e) {
        }
        return null;
    }

    static synchronized FacebookRequestErrorClassification getErrorClassification() {
        FacebookRequestErrorClassification errorClassification;
        Utility.FetchedAppSettings appSettings =
                Utility.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId());
        if (appSettings == null) {
            return FacebookRequestErrorClassification.getDefaultErrorClassification();
        }
        return appSettings.getErrorClassification();
    }

    /**
     * An enum that represents the Facebook SDK classification for the error that occurred.
     */
    public enum Category {
        /**
         * Indicates that the error is authentication related. The {@link
         * com.facebook.login.LoginManager#resolveError(android.app.Activity, GraphResponse)} method
         * or {@link com.facebook.login.LoginManager#resolveError(android.support.v4.app.Fragment,
         * GraphResponse)} method can be called to recover from this error.
         */
        LOGIN_RECOVERABLE,

        /**
         * Indicates that the error is not transient or recoverable by the Facebook SDK.
         */
        OTHER,

        /**
         * Indicates that the error is transient, the request can be attempted again.
         */
        TRANSIENT,
    };
}
