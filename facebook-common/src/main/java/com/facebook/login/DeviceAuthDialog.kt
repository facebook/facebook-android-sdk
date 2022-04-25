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

package com.facebook.login

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.FacebookActivity
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphRequestAsyncTask
import com.facebook.HttpMethod
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.common.R
import com.facebook.devicerequests.internal.DeviceRequestsHelper
import com.facebook.internal.AnalyticsEvents
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
import com.facebook.internal.Utility
import com.facebook.internal.Validate
import com.facebook.login.LoginClient.Request
import java.util.Date
import java.util.Locale
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.Volatile
import org.json.JSONException
import org.json.JSONObject

open class DeviceAuthDialog : DialogFragment() {
  private lateinit var progressBar: View
  private lateinit var confirmationCode: TextView
  private lateinit var instructions: TextView
  private var deviceAuthMethodHandler: DeviceAuthMethodHandler? = null
  private val completed = AtomicBoolean()

  @Volatile private var currentGraphRequestPoll: GraphRequestAsyncTask? = null
  @Volatile private var scheduledPoll: ScheduledFuture<*>? = null
  @Volatile private var currentRequestState: RequestState? = null

  // Used to tell if we are destroying the fragment because it was dismissed or dismissing the
  // fragment because it is being destroyed.
  private var isBeingDestroyed = false
  private var isRetry = false
  private var request: Request? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    val view = super.onCreateView(inflater, container, savedInstanceState)
    val facebookActivity = requireActivity() as FacebookActivity
    val fragment = facebookActivity.currentFragment as LoginFragment?
    deviceAuthMethodHandler = fragment?.loginClient?.getCurrentHandler() as DeviceAuthMethodHandler?
    savedInstanceState?.getParcelable<RequestState>(REQUEST_STATE_KEY)?.let { requestState ->
      setCurrentRequestState(requestState)
    }
    return view
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog: Dialog =
        object : Dialog(requireActivity(), R.style.com_facebook_auth_dialog) {
          override fun onBackPressed() {
            onBackButtonPressed()
            super.onBackPressed()
          }
        }
    dialog.setContentView(initializeContentView(DeviceRequestsHelper.isAvailable() && !isRetry))
    return dialog
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    if (!isBeingDestroyed) {
      onCancel()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (currentRequestState != null) {
      outState.putParcelable(REQUEST_STATE_KEY, currentRequestState)
    }
  }

  override fun onDestroyView() {
    // Set this to true so we know if we are being destroyed and then dismissing the dialog
    // Or if we are dismissing the dialog and then destroying the fragment. In latter we want
    // to do a cancel callback.
    isBeingDestroyed = true
    completed.set(true)
    super.onDestroyView()
    currentGraphRequestPoll?.cancel(true)
    scheduledPoll?.cancel(true)
  }

  /**
   * Start the login/auth process with a login request
   * @param request the login request
   */
  open fun startLogin(request: Request) {
    this.request = request
    val parameters = Bundle()
    parameters.putString("scope", TextUtils.join(",", request.permissions))
    Utility.putNonEmptyString(parameters, "redirect_uri", request.deviceRedirectUriString)
    Utility.putNonEmptyString(
        parameters, DeviceRequestsHelper.DEVICE_TARGET_USER_ID, request.deviceAuthTargetUserId)
    val accessToken = Validate.hasAppID() + "|" + Validate.hasClientToken()
    parameters.putString(GraphRequest.ACCESS_TOKEN_PARAM, accessToken)
    parameters.putString(
        DeviceRequestsHelper.DEVICE_INFO_PARAM,
        DeviceRequestsHelper.getDeviceInfo(additionalDeviceInfo()?.toMutableMap()))
    val graphRequest =
        GraphRequest.newPostRequestWithBundle(
            null,
            DEVICE_LOGIN_ENDPOINT,
            parameters,
            GraphRequest.Callback { response ->
              if (isBeingDestroyed) {
                return@Callback
              }
              if (response.error != null) {
                onError(response.error?.exception ?: FacebookException())
                return@Callback
              }
              val jsonObject = response.getJSONObject() ?: JSONObject()
              val requestState = RequestState()
              try {
                requestState.setUserCode(jsonObject.getString("user_code"))
                requestState.requestCode = jsonObject.getString("code")
                requestState.interval = jsonObject.getLong("interval")
              } catch (ex: JSONException) {
                onError(FacebookException(ex))
                return@Callback
              }
              setCurrentRequestState(requestState)
            })
    graphRequest.executeAsync()
  }

  /** Additional device information for this device auth. It's only for internal use. */
  open fun additionalDeviceInfo(): Map<String, String>? = null

  private fun setCurrentRequestState(currentRequestState: RequestState) {
    this.currentRequestState = currentRequestState
    confirmationCode.text = currentRequestState.getUserCode()
    val bitmap = DeviceRequestsHelper.generateQRCode(currentRequestState.authorizationUri)
    val qrCode = BitmapDrawable(resources, bitmap)
    instructions.setCompoundDrawablesWithIntrinsicBounds(null, qrCode, null, null)
    confirmationCode.visibility = View.VISIBLE
    progressBar.visibility = View.GONE
    if (!isRetry) {
      if (DeviceRequestsHelper.startAdvertisementService(currentRequestState.getUserCode())) {
        val logger = InternalAppEventsLogger(context)
        logger.logEventImplicitly(AnalyticsEvents.EVENT_SMART_LOGIN_SERVICE)
      }
    }

    // If we polled within the last interval schedule a poll else start a poll.
    if (currentRequestState.withinLastRefreshWindow()) {
      schedulePoll()
    } else {
      poll()
    }
  }

  protected open fun initializeContentView(isSmartLogin: Boolean): View {
    val view: View
    val inflater = requireActivity().layoutInflater
    view = inflater.inflate(getLayoutResId(isSmartLogin), null)
    progressBar = view.findViewById(R.id.progress_bar)
    confirmationCode = view.findViewById<View>(R.id.confirmation_code) as TextView
    val cancelButton = view.findViewById<View>(R.id.cancel_button) as Button
    cancelButton.setOnClickListener { onCancel() }
    instructions = view.findViewById<View>(R.id.com_facebook_device_auth_instructions) as TextView
    instructions.text = Html.fromHtml(getString(R.string.com_facebook_device_auth_instructions))
    return view
  }

  @LayoutRes
  protected open fun getLayoutResId(isSmartLogin: Boolean): Int {
    return if (isSmartLogin) R.layout.com_facebook_smart_device_dialog_fragment
    else R.layout.com_facebook_device_auth_dialog_fragment
  }

  private fun poll() {
    currentRequestState?.setLastPoll(Date().time)
    currentGraphRequestPoll = pollRequest.executeAsync()
  }

  private fun schedulePoll() {
    val interval = currentRequestState?.interval
    if (interval != null) {
      scheduledPoll =
          DeviceAuthMethodHandler.getBackgroundExecutor()
              .schedule({ poll() }, interval, TimeUnit.SECONDS)
    }
  }

  private val pollRequest: GraphRequest
    get() {
      val parameters = Bundle()
      parameters.putString("code", currentRequestState?.requestCode)
      return GraphRequest.newPostRequestWithBundle(
          null,
          DEVICE_LOGIN_STATUS_ENDPOINT,
          parameters,
          GraphRequest.Callback { response -> // Check if the request was already cancelled
            if (completed.get()) {
              return@Callback
            }
            val error = response.error
            if (error != null) {
              // We need to decide if this is a fatal error by checking the error
              // message text
              when (error.subErrorCode) {
                LOGIN_ERROR_SUBCODE_AUTHORIZATION_PENDING,
                LOGIN_ERROR_SUBCODE_EXCESSIVE_POLLING -> {

                  // Keep polling. If we got the slow down message just ignore
                  schedulePoll()
                }
                LOGIN_ERROR_SUBCODE_CODE_EXPIRED -> {
                  currentRequestState?.let {
                    DeviceRequestsHelper.cleanUpAdvertisementService(it.getUserCode())
                  }
                  val request = this.request
                  if (request != null) {
                    startLogin(request)
                  } else {
                    onCancel()
                  }
                }
                LOGIN_ERROR_SUBCODE_AUTHORIZATION_DECLINED -> {
                  onCancel()
                }
                else -> {
                  onError(response.error?.exception ?: FacebookException())
                }
              }
              return@Callback
            }
            try {
              val resultObject = response.getJSONObject() ?: JSONObject()
              onSuccess(
                  resultObject.getString("access_token"),
                  resultObject.getLong("expires_in"),
                  resultObject.optLong("data_access_expiration_time"))
            } catch (ex: JSONException) {
              onError(FacebookException(ex))
            }
          })
    }

  private fun presentConfirmation(
      userId: String,
      permissions: PermissionsLists,
      accessToken: String,
      name: String,
      expirationTime: Date?,
      dataAccessExpirationTime: Date?
  ) {
    val message = resources.getString(R.string.com_facebook_smart_login_confirmation_title)
    val continueFormat =
        resources.getString(R.string.com_facebook_smart_login_confirmation_continue_as)
    val cancel = resources.getString(R.string.com_facebook_smart_login_confirmation_cancel)
    val continueText = String.format(continueFormat, name)
    val builder = AlertDialog.Builder(context)
    builder
        .setMessage(message)
        .setCancelable(true)
        .setNegativeButton(continueText) { _, _ ->
          completeLogin(userId, permissions, accessToken, expirationTime, dataAccessExpirationTime)
        }
        .setPositiveButton(cancel) { _, _ ->
          val view = initializeContentView(false)
          dialog?.setContentView(view)
          request?.let { startLogin(it) }
        }
    builder.create().show()
  }

  private fun onSuccess(accessToken: String, expiresIn: Long, dataAccessExpirationTime: Long?) {
    val parameters = Bundle()
    parameters.putString(GraphRequest.FIELDS_PARAM, "id,permissions,name")
    val expirationTime = if (expiresIn != 0L) Date(Date().time + expiresIn * 1000L) else null
    val dataAccessExpirationTimeDate =
        if (dataAccessExpirationTime != 0L && dataAccessExpirationTime != null)
            Date(dataAccessExpirationTime * 1000L)
        else null
    val temporaryToken =
        AccessToken(
            accessToken,
            FacebookSdk.getApplicationId(),
            "0",
            null,
            null,
            null,
            null,
            expirationTime,
            null,
            dataAccessExpirationTimeDate)
    val request =
        GraphRequest.newGraphPathRequest(
            temporaryToken,
            "me",
            GraphRequest.Callback { response ->
              if (completed.get()) {
                return@Callback
              }
              val error = response.error
              if (error != null) {
                onError(error.exception ?: FacebookException())
                return@Callback
              }
              val userId: String
              val permissions: PermissionsLists
              val name: String
              try {
                val jsonObject = response.getJSONObject() ?: JSONObject()
                userId = jsonObject.getString("id")
                permissions = handlePermissionResponse(jsonObject)
                name = jsonObject.getString("name")
              } catch (ex: JSONException) {
                onError(FacebookException(ex))
                return@Callback
              }
              currentRequestState?.let {
                DeviceRequestsHelper.cleanUpAdvertisementService(it.getUserCode())
              }
              val requireConfirm =
                  FetchedAppSettingsManager.getAppSettingsWithoutQuery(
                          FacebookSdk.getApplicationId())
                      ?.smartLoginOptions?.contains(SmartLoginOption.RequireConfirm)
              if (requireConfirm == true && !isRetry) {
                isRetry = true
                presentConfirmation(
                    userId,
                    permissions,
                    accessToken,
                    name,
                    expirationTime,
                    dataAccessExpirationTimeDate)
                return@Callback
              }
              completeLogin(
                  userId, permissions, accessToken, expirationTime, dataAccessExpirationTimeDate)
            })
    request.httpMethod = HttpMethod.GET
    request.parameters = parameters
    request.executeAsync()
  }

  private fun completeLogin(
      userId: String,
      permissions: PermissionsLists,
      accessToken: String,
      expirationTime: Date?,
      dataAccessExpirationTime: Date?
  ) {
    deviceAuthMethodHandler?.onSuccess(
        accessToken,
        FacebookSdk.getApplicationId(),
        userId,
        permissions.grantedPermissions,
        permissions.declinedPermissions,
        permissions.expiredPermissions,
        AccessTokenSource.DEVICE_AUTH,
        expirationTime,
        null,
        dataAccessExpirationTime)
    dialog?.dismiss()
  }

  protected open fun onError(ex: FacebookException) {
    if (!completed.compareAndSet(false, true)) {
      return
    }
    currentRequestState?.let { DeviceRequestsHelper.cleanUpAdvertisementService(it.getUserCode()) }
    deviceAuthMethodHandler?.onError(ex)
    dialog?.dismiss()
  }

  protected open fun onCancel() {
    if (!completed.compareAndSet(false, true)) {
      // Should not have happened but we called cancel twice
      return
    }
    currentRequestState?.let { DeviceRequestsHelper.cleanUpAdvertisementService(it.getUserCode()) }
    // We are detached and cannot send a cancel message back
    deviceAuthMethodHandler?.onCancel()
    dialog?.dismiss()
  }

  protected open fun onBackButtonPressed() {
    // no-op
  }

  private class RequestState : Parcelable {
    var authorizationUri: String? = null
      private set
    private var userCode: String? = null
    var requestCode: String? = null
    var interval: Long = 0
    private var lastPoll: Long = 0
    internal constructor()
    fun getUserCode(): String? = userCode

    fun setUserCode(userCode: String?) {
      this.userCode = userCode
      authorizationUri =
          String.format(
              Locale.ENGLISH, "https://facebook.com/device?user_code=%1\$s&qr=1", userCode)
    }

    fun setLastPoll(lastPoll: Long) {
      this.lastPoll = lastPoll
    }
    protected constructor(parcel: Parcel) {
      authorizationUri = parcel.readString()
      userCode = parcel.readString()
      requestCode = parcel.readString()
      interval = parcel.readLong()
      lastPoll = parcel.readLong()
    }

    /** @return True if the current time is less than last poll time + polling interval. */
    fun withinLastRefreshWindow(): Boolean {
      if (lastPoll == 0L) {
        return false
      }
      val diff = Date().time - lastPoll - interval * 1000L
      return diff < 0
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeString(authorizationUri)
      dest.writeString(userCode)
      dest.writeString(requestCode)
      dest.writeLong(interval)
      dest.writeLong(lastPoll)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<RequestState> =
          object : Parcelable.Creator<RequestState> {
            override fun createFromParcel(parcel: Parcel): RequestState {
              return RequestState(parcel)
            }

            override fun newArray(size: Int): Array<RequestState?> {
              return arrayOfNulls(size)
            }
          }
    }
  }

  companion object {
    @VisibleForTesting internal val DEVICE_LOGIN_ENDPOINT = "device/login"

    @VisibleForTesting internal val DEVICE_LOGIN_STATUS_ENDPOINT = "device/login_status"
    private const val REQUEST_STATE_KEY = "request_state"
    private const val LOGIN_ERROR_SUBCODE_EXCESSIVE_POLLING = 1349172
    private const val LOGIN_ERROR_SUBCODE_AUTHORIZATION_DECLINED = 1349173

    @VisibleForTesting internal val LOGIN_ERROR_SUBCODE_AUTHORIZATION_PENDING = 1349174
    private const val LOGIN_ERROR_SUBCODE_CODE_EXPIRED = 1349152

    @Throws(JSONException::class)
    private fun handlePermissionResponse(result: JSONObject): PermissionsLists {
      val permissions = result.getJSONObject("permissions")
      val data = permissions.getJSONArray("data")
      val grantedPermissions = arrayListOf<String>()
      val declinedPermissions = arrayListOf<String>()
      val expiredPermissions = arrayListOf<String>()
      for (i in 0 until data.length()) {
        val obj = data.optJSONObject(i)
        val permission = obj.optString("permission")
        if (permission.isEmpty() || permission == "installed") {
          continue
        }
        when (obj.optString("status")) {
          "granted" -> {
            grantedPermissions.add(permission)
          }
          "declined" -> {
            declinedPermissions.add(permission)
          }
          "expired" -> {
            expiredPermissions.add(permission)
          }
          else -> continue
        }
      }
      return PermissionsLists(grantedPermissions, declinedPermissions, expiredPermissions)
    }
  }

  /**
   * Internal helper class that is used to hold three different permission lists (granted, declined
   * and expired)
   */
  private class PermissionsLists(
      var grantedPermissions: List<String>,
      var declinedPermissions: List<String>,
      var expiredPermissions: List<String>
  )
}
