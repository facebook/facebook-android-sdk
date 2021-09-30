/*
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
package com.facebook

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FacebookRequestErrorClassification.Companion.defaultErrorClassification
import com.facebook.internal.FetchedAppSettingsManager.getAppSettingsWithoutQuery
import com.facebook.internal.Utility.getStringPropertyAsJSON
import com.facebook.internal.qualityvalidation.Excuse
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations
import java.net.HttpURLConnection
import org.json.JSONException
import org.json.JSONObject

/**
 * This class represents an error that occurred during a Facebook request.
 *
 * In general, one would call [.getCategory] to determine the type of error that occurred, and act
 * accordingly. For more information on error handling, see
 * [
 * https://developers.facebook.com/docs/reference/api/errors/](https://developers.facebook.com/docs/reference/api/errors/)
 */
@ExcusesForDesignViolations(Excuse(type = "KOTLIN_JVM_FIELD", reason = "Legacy migration"))
class FacebookRequestError
private constructor(
    /**
     * Returns the HTTP status code for this particular request.
     *
     * @return the HTTP status code for the request
     */
    val requestStatusCode: Int,
    /**
     * Returns the error code returned from Facebook.
     *
     * @return the error code returned from Facebook
     */
    val errorCode: Int,
    /**
     * Returns the sub-error code returned from Facebook.
     *
     * @return the sub-error code returned from Facebook
     */
    val subErrorCode: Int,
    /**
     * Returns the type of error as a raw string. This is generally less useful than using the [
     * ][.getCategory] method, but can provide further details on the error.
     *
     * @return the type of error as a raw string
     */
    val errorType: String?,
    errorMessageField: String?,
    /**
     * Returns a short summary of the error suitable for display to the user. Not all Facebook
     * errors yield a title/message suitable for user display; however in all cases where
     * getErrorUserTitle() returns valid String - user should be notified.
     *
     * @return the error message returned from Facebook
     */
    val errorUserTitle: String?,
    /**
     * Returns a message suitable for display to the user, describing a user action necessary to
     * enable Facebook functionality. Not all Facebook errors yield a message suitable for user
     * display; however in all cases where shouldNotifyUser() returns true, this method returns a
     * non-null message suitable for display.
     *
     * @return the error message returned from Facebook
     */
    val errorUserMessage: String?,
    /**
     * Returns the body portion of the response corresponding to the request from Facebook.
     *
     * @return the body of the response for the request
     */
    val requestResultBody: JSONObject?,
    /**
     * Returns the full JSON response for the corresponding request. In a non-batch request, this
     * would be the raw response in the form of a JSON object. In a batch request, this result will
     * contain the body of the response as well as the HTTP headers that pertain to the specific
     * request (in the form of a "headers" JSONArray).
     *
     * @return the full JSON response for the request
     */
    val requestResult: JSONObject?,
    /**
     * Returns the full JSON response for the batch request. If the request was not a batch request,
     * then the result from this method is the same as [.getRequestResult]. In case of a batch
     * request, the result will be a JSONArray where the elements correspond to the requests in the
     * batch. Callers should check the return type against either JSONObject or JSONArray and cast
     * accordingly.
     *
     * @return the full JSON response for the batch
     */
    val batchRequestResult: Any?,
    /**
     * Returns the HTTP connection that was used to make the request.
     *
     * @return the HTTP connection used to make the request
     */
    val connection: HttpURLConnection?,
    exceptionField: FacebookException?,
    errorIsTransient: Boolean
) : Parcelable {

  class Range internal constructor(private val start: Int, private val end: Int) {
    operator fun contains(value: Int): Boolean {
      return value in start..end
    }
  }

  /**
   * Returns the error message returned from Facebook.
   *
   * @return the error message returned from Facebook
   */
  val errorMessage: String? = errorMessageField
    get() = field ?: exception?.localizedMessage

  /**
   * Returns the exception associated with this request, if any.
   *
   * @return the exception associated with this request
   */
  var exception: FacebookException? = null
    private set

  /**
   * Returns the category in which the error belongs. Applications can use the category to determine
   * how best to handle the errors (e.g. exponential backoff for retries if being throttled).
   *
   * @return the category in which the error belong
   */
  val category: Category

  /**
   * Returns the message that can be displayed to the user before attempting error recovery.
   *
   * @return the message that can be displayed to the user before attempting error recovery
   */
  val errorRecoveryMessage: String?

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  constructor(
      connection: HttpURLConnection?,
      exception: Exception?
  ) : this(
      INVALID_HTTP_STATUS_CODE,
      INVALID_ERROR_CODE,
      INVALID_ERROR_CODE,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      connection,
      if (exception is FacebookException) exception else FacebookException(exception),
      false)

  constructor(
      errorCode: Int,
      errorType: String?,
      errorMessage: String?
  ) : this(
      INVALID_HTTP_STATUS_CODE,
      errorCode,
      INVALID_ERROR_CODE,
      errorType,
      errorMessage,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      false)

  override fun toString(): String {
    return StringBuilder("{HttpStatus: ")
        .append(requestStatusCode)
        .append(", errorCode: ")
        .append(errorCode)
        .append(", subErrorCode: ")
        .append(subErrorCode)
        .append(", errorType: ")
        .append(errorType)
        .append(", errorMessage: ")
        .append(errorMessage)
        .append("}")
        .toString()
  }

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeInt(requestStatusCode)
    out.writeInt(errorCode)
    out.writeInt(subErrorCode)
    out.writeString(errorType)
    out.writeString(errorMessage)
    out.writeString(errorUserTitle)
    out.writeString(errorUserMessage)
  }

  private constructor(
      parcel: Parcel
  ) : this(
      parcel.readInt(), // requestStatusCode
      parcel.readInt(), // errorCode
      parcel.readInt(), // subErrorCode
      parcel.readString(), // errorType
      parcel.readString(), // errorMessage
      parcel.readString(), // errorUserTitle
      parcel.readString(), // errorUserMessage
      null, // requestResultBody
      null, // requestResult
      null, // batchRequestResult
      null, // connection
      null, // exception
      false // errorIsTransient
      )

  override fun describeContents(): Int {
    return 0
  }

  /** An enum that represents the Facebook SDK classification for the error that occurred. */
  enum class Category {
    /**
     * Indicates that the error is authentication related. The [ ]
     * [com.facebook.login.LoginManager.resolveError] method or
     * [com.facebook.login.LoginManager.resolveError] method can be called to recover from this
     * error.
     */
    LOGIN_RECOVERABLE,

    /** Indicates that the error is not transient or recoverable by the Facebook SDK. */
    OTHER,

    /** Indicates that the error is transient, the request can be attempted again. */
    TRANSIENT
  }

  companion object {
    /** Represents an invalid or unknown error code from the server. */
    const val INVALID_ERROR_CODE = -1

    /**
     * Indicates that there was no valid HTTP status code returned, indicating that either the error
     * occurred locally, before the request was sent, or that something went wrong with the HTTP
     * connection. Check the exception from [.getException];
     */
    const val INVALID_HTTP_STATUS_CODE = -1
    private const val CODE_KEY = "code"
    private const val BODY_KEY = "body"
    private const val ERROR_KEY = "error"
    private const val ERROR_TYPE_FIELD_KEY = "type"
    private const val ERROR_CODE_FIELD_KEY = "code"
    private const val ERROR_MESSAGE_FIELD_KEY = "message"
    private const val ERROR_CODE_KEY = "error_code"
    private const val ERROR_SUB_CODE_KEY = "error_subcode"
    private const val ERROR_MSG_KEY = "error_msg"
    private const val ERROR_REASON_KEY = "error_reason"
    private const val ERROR_USER_TITLE_KEY = "error_user_title"
    private const val ERROR_USER_MSG_KEY = "error_user_msg"
    private const val ERROR_IS_TRANSIENT_KEY = "is_transient"
    internal val HTTP_RANGE_SUCCESS = Range(200, 299)

    /**
     * Check Response and create error if necessary
     *
     * @param singleResult jsonObject result
     * @param batchResult bach call result
     * @param connection
     */
    @JvmStatic
    fun checkResponseAndCreateError(
        singleResult: JSONObject,
        batchResult: Any?,
        connection: HttpURLConnection?
    ): FacebookRequestError? {
      try {
        if (singleResult.has(CODE_KEY)) {
          val responseCode = singleResult.getInt(CODE_KEY)
          val body =
              getStringPropertyAsJSON(
                  singleResult, BODY_KEY, GraphResponse.NON_JSON_RESPONSE_PROPERTY)
          if (body != null && body is JSONObject) {
            val jsonBody = body
            // Does this response represent an error from the service? We might get either
            // an "error" with several sub-properties, or else one or more top-level fields
            // containing error info.
            var errorType: String? = null
            var errorMessage: String? = null
            var errorUserMessage: String? = null
            var errorUserTitle: String? = null
            var errorIsTransient = false
            var errorCode = INVALID_ERROR_CODE
            var errorSubCode = INVALID_ERROR_CODE
            var hasError = false
            if (jsonBody.has(ERROR_KEY)) {
              // We assume the error object is correctly formatted.
              val error = getStringPropertyAsJSON(jsonBody, ERROR_KEY, null) as JSONObject?
              errorType = error?.optString(ERROR_TYPE_FIELD_KEY, null)
              errorMessage = error?.optString(ERROR_MESSAGE_FIELD_KEY, null)
              errorCode =
                  error?.optInt(ERROR_CODE_FIELD_KEY, INVALID_ERROR_CODE) ?: INVALID_ERROR_CODE
              errorSubCode =
                  error?.optInt(ERROR_SUB_CODE_KEY, INVALID_ERROR_CODE) ?: INVALID_ERROR_CODE
              errorUserMessage = error?.optString(ERROR_USER_MSG_KEY, null)
              errorUserTitle = error?.optString(ERROR_USER_TITLE_KEY, null)
              errorIsTransient = error?.optBoolean(ERROR_IS_TRANSIENT_KEY, false) ?: false
              hasError = true
            } else if (jsonBody.has(ERROR_CODE_KEY) ||
                jsonBody.has(ERROR_MSG_KEY) ||
                jsonBody.has(ERROR_REASON_KEY)) {
              errorType = jsonBody.optString(ERROR_REASON_KEY, null)
              errorMessage = jsonBody.optString(ERROR_MSG_KEY, null)
              errorCode = jsonBody.optInt(ERROR_CODE_KEY, INVALID_ERROR_CODE)
              errorSubCode = jsonBody.optInt(ERROR_SUB_CODE_KEY, INVALID_ERROR_CODE)
              hasError = true
            }
            if (hasError) {
              return FacebookRequestError(
                  responseCode,
                  errorCode,
                  errorSubCode,
                  errorType,
                  errorMessage,
                  errorUserTitle,
                  errorUserMessage,
                  jsonBody,
                  singleResult,
                  batchResult,
                  connection,
                  null,
                  errorIsTransient)
            }
          }

          // If we didn't get error details, but we did get a failure response code, report
          // it.
          if (!HTTP_RANGE_SUCCESS.contains(responseCode)) {
            return FacebookRequestError(
                responseCode,
                INVALID_ERROR_CODE,
                INVALID_ERROR_CODE,
                null,
                null,
                null,
                null,
                if (singleResult.has(BODY_KEY)) {
                  getStringPropertyAsJSON(
                      singleResult, BODY_KEY, GraphResponse.NON_JSON_RESPONSE_PROPERTY) as
                      JSONObject?
                } else {
                  null
                },
                singleResult,
                batchResult,
                connection,
                null,
                false)
          }
        }
      } catch (e: JSONException) {}
      return null
    }

    @get:Synchronized
    val errorClassification: FacebookRequestErrorClassification
      @JvmStatic
      get() {
        val appSettings =
            getAppSettingsWithoutQuery(FacebookSdk.getApplicationId())
                ?: return defaultErrorClassification
        return appSettings.errorClassification
      }

    @JvmField
    val CREATOR: Parcelable.Creator<FacebookRequestError> =
        object : Parcelable.Creator<FacebookRequestError> {
          override fun createFromParcel(parcel: Parcel): FacebookRequestError {
            return FacebookRequestError(parcel)
          }

          override fun newArray(size: Int): Array<FacebookRequestError?> {
            return arrayOfNulls(size)
          }
        }
  }

  init {
    var isLocalException = false
    if (exceptionField != null) {
      exception = exceptionField
      isLocalException = true
    } else {
      exception = FacebookServiceException(this, errorMessage)
    }

    category =
        if (isLocalException) Category.OTHER
        else errorClassification.classify(errorCode, subErrorCode, errorIsTransient)
    errorRecoveryMessage = errorClassification.getRecoveryMessage(this.category)
  }
}
