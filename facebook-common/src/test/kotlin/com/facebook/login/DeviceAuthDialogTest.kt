/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.FacebookActivity
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.ServerProtocol
import com.facebook.internal.SmartLoginOption
import java.util.EnumSet
import java.util.UUID
import java.util.concurrent.ScheduledThreadPoolExecutor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

@PrepareForTest(FacebookSdk::class, FetchedAppSettingsManager::class)
class DeviceAuthDialogTest : FacebookPowerMockTestCase() {
  private lateinit var deviceAuthDialog: DeviceAuthDialog
  private lateinit var loginRequest: LoginClient.Request
  private lateinit var authId: UUID
  private lateinit var mockGraphRequestCompanion: GraphRequest.Companion
  private lateinit var activityController: ActivityController<FacebookActivity>
  private lateinit var mockDeviceLoginRequest: GraphRequest
  private lateinit var mockPollRequest: GraphRequest
  private lateinit var mockMeRequest: GraphRequest
  private lateinit var mockDeviceAuthMethodHandler: DeviceAuthMethodHandler
  private lateinit var mockDeviceAuthMethodBackgroundExecutor: ScheduledThreadPoolExecutor

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APP_ID)
    whenever(FacebookSdk.getClientToken()).thenReturn(CLIENT_TOKEN)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    val mockAppSettings = mock<FetchedAppSettings>()
    whenever(mockAppSettings.smartLoginOptions)
        .thenReturn(EnumSet.noneOf(SmartLoginOption::class.java))
    whenever(mockAppSettings.errorClassification)
        .thenReturn(FacebookRequestErrorClassification.defaultErrorClassification)
    whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(anyOrNull()))
        .thenReturn(mockAppSettings)
    authId = UUID.randomUUID()

    loginRequest =
        LoginClient.Request(
            loginBehavior = LoginBehavior.DEVICE_AUTH,
            permissions = PERMISSIONS,
            defaultAudience = DefaultAudience.FRIENDS,
            authType = ServerProtocol.DIALOG_REREQUEST_AUTH_TYPE,
            applicationId = APP_ID,
            authId = authId.toString())

    deviceAuthDialog = DeviceAuthDialog()
    mockGraphRequestCompanion = mock()
    Whitebox.setInternalState(GraphRequest::class.java, "Companion", mockGraphRequestCompanion)
    mockDeviceLoginRequest = mock()
    whenever(
            mockGraphRequestCompanion.newPostRequestWithBundle(
                anyOrNull(), eq(DeviceAuthDialog.DEVICE_LOGIN_ENDPOINT), anyOrNull(), anyOrNull()))
        .thenReturn(mockDeviceLoginRequest)
    mockPollRequest = mock()
    whenever(
            mockGraphRequestCompanion.newPostRequestWithBundle(
                anyOrNull(),
                eq(DeviceAuthDialog.DEVICE_LOGIN_STATUS_ENDPOINT),
                anyOrNull(),
                anyOrNull()))
        .thenReturn(mockPollRequest)
    mockMeRequest = mock()
    whenever(mockGraphRequestCompanion.newGraphPathRequest(anyOrNull(), eq("me"), anyOrNull()))
        .thenReturn(mockMeRequest)

    mockDeviceAuthMethodBackgroundExecutor = mock()
    val mockDeviceAuthMethodHandlerCompanion = mock<DeviceAuthMethodHandler.Companion>()
    whenever(mockDeviceAuthMethodHandlerCompanion.getBackgroundExecutor())
        .thenReturn(mockDeviceAuthMethodBackgroundExecutor)
    Whitebox.setInternalState(
        DeviceAuthMethodHandler::class.java, "Companion", mockDeviceAuthMethodHandlerCompanion)

    activityController = Robolectric.buildActivity(FacebookActivity::class.java)
  }

  @Test
  fun `test starting login will make an async request to device endpoint with proper parameters`() {
    val parameterCaptor = argumentCaptor<Bundle>()
    val callbackCaptor = argumentCaptor<GraphRequest.Callback>()

    deviceAuthDialog.startLogin(loginRequest)

    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_ENDPOINT),
            parameterCaptor.capture(),
            callbackCaptor.capture())
    verify(mockDeviceLoginRequest).executeAsync()
    val capturedParameters = parameterCaptor.firstValue
    assertThat(capturedParameters.getString("scope")?.split(','))
        .containsExactlyInAnyOrder(*PERMISSIONS.toTypedArray())
  }

  @Test
  fun `test showing device login dialog will poll the status`() {
    val callbackCaptor = argumentCaptor<GraphRequest.Callback>()
    val responseJSONData = JSONObject()
    responseJSONData.put("user_code", "USERCODE")
    responseJSONData.put("code", "code")
    responseJSONData.put("interval", 1)
    val mockResponse = mockResponseWithJSONObject(responseJSONData)
    activityController.create()
    activityController.start()
    prepareLoginClientWithMockDeviceAuthMethodHandler()

    deviceAuthDialog.showNow(
        activityController.get().supportFragmentManager, "device_login_with_facebook")
    deviceAuthDialog.startLogin(loginRequest)

    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_ENDPOINT),
            anyOrNull(),
            callbackCaptor.capture())
    callbackCaptor.firstValue.onCompleted(mockResponse)
    verify(mockPollRequest).executeAsync()
  }

  @Test
  fun `test showing device login dialog when error is received`() {
    val callbackCaptor = argumentCaptor<GraphRequest.Callback>()
    val mockResponse = mock<GraphResponse>()
    val testException = FacebookException("test exception")
    val requestException = FacebookRequestError(mock(), testException)
    whenever(mockResponse.error).thenReturn(requestException)
    activityController.create()
    activityController.start()
    prepareLoginClientWithMockDeviceAuthMethodHandler()

    deviceAuthDialog.showNow(
        activityController.get().supportFragmentManager, "device_login_with_facebook")
    deviceAuthDialog.startLogin(loginRequest)

    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_ENDPOINT),
            anyOrNull(),
            callbackCaptor.capture())
    callbackCaptor.firstValue.onCompleted(mockResponse)
    verify(mockDeviceAuthMethodHandler).onError(testException)
  }

  @Test
  fun `test showing device login dialog when json exception is thrown`() {
    val callbackCaptor = argumentCaptor<GraphRequest.Callback>()
    val responseJSONData =
        JSONObject(
            mapOf("user_code" to "USERCODE", "code" to "code", "interval" to "invalid value"))
    val mockResponse = mockResponseWithJSONObject(responseJSONData)
    activityController.create()
    activityController.start()
    prepareLoginClientWithMockDeviceAuthMethodHandler()

    deviceAuthDialog.showNow(
        activityController.get().supportFragmentManager, "device_login_with_facebook")
    deviceAuthDialog.startLogin(loginRequest)

    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_ENDPOINT),
            anyOrNull(),
            callbackCaptor.capture())
    callbackCaptor.firstValue.onCompleted(mockResponse)
    verify(mockDeviceAuthMethodHandler).onError(any<FacebookException>())
  }

  @Test
  fun `test device login success will make a me request for user info`() {
    val callbackCaptor = argumentCaptor<GraphRequest.Callback>()
    val responseJSONData =
        JSONObject(mapOf("user_code" to "USERCODE", "code" to "code", "interval" to 1))
    val mockResponse = mockResponseWithJSONObject(responseJSONData)
    activityController.create()
    activityController.start()
    prepareLoginClientWithMockDeviceAuthMethodHandler()

    deviceAuthDialog.showNow(
        activityController.get().supportFragmentManager, "device_login_with_facebook")
    deviceAuthDialog.startLogin(loginRequest)

    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_ENDPOINT),
            anyOrNull(),
            callbackCaptor.capture())
    callbackCaptor.firstValue.onCompleted(mockResponse)
    val pollRequestCallbackCaptor = argumentCaptor<GraphRequest.Callback>()
    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_STATUS_ENDPOINT),
            anyOrNull(),
            pollRequestCallbackCaptor.capture())
    verify(mockPollRequest).executeAsync()
    val pollCallback = pollRequestCallbackCaptor.firstValue

    // send response for poll request to finish the device auth
    val loginResultJSONData = JSONObject(mapOf("access_token" to ACCESS_TOKEN, "expires_in" to 0))
    val mockPollResponse = mockResponseWithJSONObject(loginResultJSONData)
    pollCallback.onCompleted(mockPollResponse)

    // verify requests after device auth
    val accessTokenCaptor = argumentCaptor<AccessToken>()
    val meRequestCallbackCaptor = argumentCaptor<GraphRequest.Callback>()
    verify(mockGraphRequestCompanion)
        .newGraphPathRequest(
            accessTokenCaptor.capture(), eq("me"), meRequestCallbackCaptor.capture())
    verify(mockMeRequest).executeAsync()
    assertThat(accessTokenCaptor.firstValue.token).isEqualTo(ACCESS_TOKEN)
    assertThat(accessTokenCaptor.firstValue.applicationId).isEqualTo(APP_ID)
    val mockUserInfoResponse = mockResponseWithJSONObject(JSONObject(RETURNED_LOGIN_JSON))
    meRequestCallbackCaptor.firstValue.onCompleted(mockUserInfoResponse)
    verify(mockDeviceAuthMethodHandler)
        .onSuccess(
            eq(ACCESS_TOKEN),
            eq(APP_ID),
            eq("12345"),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(AccessTokenSource.DEVICE_AUTH),
            anyOrNull(),
            anyOrNull(),
            anyOrNull())
  }

  @Test
  fun `test device login keeps polling when authorization pending`() {
    val callbackCaptor = argumentCaptor<GraphRequest.Callback>()
    val responseJSONData =
        JSONObject(mapOf("user_code" to "USERCODE", "code" to "code", "interval" to 1))
    val mockResponse = mockResponseWithJSONObject(responseJSONData)
    activityController.create()
    activityController.start()
    prepareLoginClientWithMockDeviceAuthMethodHandler()

    deviceAuthDialog.showNow(
        activityController.get().supportFragmentManager, "device_login_with_facebook")
    deviceAuthDialog.startLogin(loginRequest)

    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_ENDPOINT),
            anyOrNull(),
            callbackCaptor.capture())
    callbackCaptor.firstValue.onCompleted(mockResponse)
    val pollRequestCallbackCaptor = argumentCaptor<GraphRequest.Callback>()
    verify(mockGraphRequestCompanion)
        .newPostRequestWithBundle(
            anyOrNull(),
            eq(DeviceAuthDialog.DEVICE_LOGIN_STATUS_ENDPOINT),
            anyOrNull(),
            pollRequestCallbackCaptor.capture())
    verify(mockPollRequest).executeAsync()
    val pollCallback = pollRequestCallbackCaptor.firstValue

    // send response for poll request to finish the device auth
    val mockPollResponse = mock<GraphResponse>()
    val responseError = mock<FacebookRequestError>()
    whenever(responseError.subErrorCode)
        .thenReturn(DeviceAuthDialog.LOGIN_ERROR_SUBCODE_AUTHORIZATION_PENDING)
    whenever(mockPollResponse.error).thenReturn(responseError)
    pollCallback.onCompleted(mockPollResponse)
    // verify the polling command is scheduled
    verify(mockDeviceAuthMethodBackgroundExecutor).schedule(any(), any(), any())
  }

  private fun prepareLoginClientWithMockDeviceAuthMethodHandler() {
    mockDeviceAuthMethodHandler = mock()
    val loginFragment = activityController.get().currentFragment as LoginFragment
    loginFragment.loginClient.handlersToTry = arrayOf(mockDeviceAuthMethodHandler)
    Whitebox.setInternalState(loginFragment.loginClient, "currentHandler", 0)
  }

  private fun mockResponseWithJSONObject(jsonObject: JSONObject): GraphResponse {
    val mockRequest = mock<GraphResponse>()
    whenever(mockRequest.jsonObject).thenReturn(jsonObject)
    whenever(mockRequest.getJSONObject()).thenReturn(jsonObject)
    return mockRequest
  }

  companion object {
    private const val APP_ID = "123456789"
    private const val CLIENT_TOKEN = "1234567890abcdef"
    private val PERMISSIONS = setOf("user_friends", "public_profile")
    private const val ACCESS_TOKEN = "access_token"
    private const val RETURNED_LOGIN_JSON =
        """{"id":"12345","permissions":{"data":[{"permission":"user_friends","status":"granted"},{"permission":"public_profile","status":"granted"}]},"name":"tester"}"""
  }
}
