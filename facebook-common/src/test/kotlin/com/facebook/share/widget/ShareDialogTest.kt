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

package com.facebook.share.widget

import android.app.Activity
import android.net.Uri
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import com.facebook.AccessToken
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.AppCall
import com.facebook.internal.DialogPresenter
import com.facebook.internal.FragmentWrapper
import com.facebook.share.internal.ShareDialogFeature
import com.facebook.share.internal.ShareInternalUtility
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.ShareMediaContent
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareVideoContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(ShareInternalUtility::class, FacebookSdk::class, DialogPresenter::class)
class ShareDialogTest : FacebookPowerMockTestCase() {
  private lateinit var testShareLinkContent: ShareLinkContent
  private lateinit var staticShareCallbackRequestCodeCaptor: KArgumentCaptor<Int>
  private lateinit var mockInternalAppEventsLogger: InternalAppEventsLogger
  private lateinit var mockAccessTokenCompanion: AccessToken.Companion

  override fun setup() {
    super.setup()
    testShareLinkContent =
        ShareLinkContent.Builder().setContentUrl(Uri.parse("https://facebook.com/")).build()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")

    staticShareCallbackRequestCodeCaptor = argumentCaptor()
    PowerMockito.mockStatic(ShareInternalUtility::class.java)
    whenever(
            ShareInternalUtility.registerStaticShareCallback(
                staticShareCallbackRequestCodeCaptor.capture()))
        .thenAnswer {}

    PowerMockito.mockStatic(DialogPresenter::class.java)

    mockInternalAppEventsLogger = mock()
    val mockInternalAppEventsLoggerCompanion = mock<InternalAppEventsLogger.Companion>()
    whenever(mockInternalAppEventsLoggerCompanion.createInstance(anyOrNull(), anyOrNull()))
        .thenReturn(mockInternalAppEventsLogger)
    Whitebox.setInternalState(
        InternalAppEventsLogger::class.java, "Companion", mockInternalAppEventsLoggerCompanion)

    mockAccessTokenCompanion = mock()
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockAccessTokenCompanion)
  }

  @Test
  fun `test show share dialog with an Android legacy activity will call DialogPresenter with the activity`() {
    val mockActivity = mock<Activity>()
    val appCallCaptor = argumentCaptor<AppCall>()
    var isPresentCalled = false
    whenever(DialogPresenter.present(appCallCaptor.capture(), eq(mockActivity))).thenAnswer {
      isPresentCalled = true
      Unit
    }

    ShareDialog.show(mockActivity, testShareLinkContent)

    val capturedAppCall = appCallCaptor.firstValue
    // verify DialogPresenter.present is called
    assertThat(isPresentCalled).isTrue
    // verify the request code is registered as a static callback
    assertThat(capturedAppCall.requestCode)
        .isEqualTo(staticShareCallbackRequestCodeCaptor.firstValue)
  }

  @Test
  fun `test show share dialog with an AndroidX activity will call DialogPresenter with the activity result registry`() {
    val mockActivity = mock<ComponentActivity>()
    val mockActivityResultRegistry = mock<ActivityResultRegistry>()
    whenever(mockActivity.activityResultRegistry).thenReturn(mockActivityResultRegistry)
    val appCallCaptor = argumentCaptor<AppCall>()
    var isPresentCalled = false
    whenever(
            DialogPresenter.present(
                appCallCaptor.capture(), eq(mockActivityResultRegistry), anyOrNull()))
        .thenAnswer {
          isPresentCalled = true
          Unit
        }

    ShareDialog.show(mockActivity, testShareLinkContent)

    val capturedAppCall = appCallCaptor.firstValue
    // verify DialogPresenter.present is called
    assertThat(isPresentCalled).isTrue
    // verify the request code is registered as a static callback
    assertThat(capturedAppCall.requestCode)
        .isEqualTo(staticShareCallbackRequestCodeCaptor.firstValue)
  }

  @Test
  fun `test show share dialog with an AndroidX fragment will call DialogPresenter with the activity result registry`() {
    val mockActivity = mock<androidx.fragment.app.FragmentActivity>()
    val mockActivityResultRegistry = mock<ActivityResultRegistry>()
    val mockFragment = mock<androidx.fragment.app.Fragment>()
    whenever(mockFragment.activity).thenReturn(mockActivity)
    whenever(mockActivity.activityResultRegistry).thenReturn(mockActivityResultRegistry)
    val appCallCaptor = argumentCaptor<AppCall>()
    var isPresentCalled = false
    whenever(
            DialogPresenter.present(
                appCallCaptor.capture(), eq(mockActivityResultRegistry), anyOrNull()))
        .thenAnswer {
          isPresentCalled = true
          Unit
        }

    ShareDialog.show(mockFragment, testShareLinkContent)

    val capturedAppCall = appCallCaptor.firstValue
    // verify DialogPresenter.present is called
    assertThat(isPresentCalled).isTrue
    // verify the request code is registered as a static callback
    assertThat(capturedAppCall.requestCode)
        .isEqualTo(staticShareCallbackRequestCodeCaptor.firstValue)
  }

  @Test
  fun `test show share dialog with a legacy fragment will call DialogPresenter with the activity result registry`() {
    val mockActivity = mock<Activity>()
    val mockFragment = mock<android.app.Fragment>()
    whenever(mockFragment.activity).thenReturn(mockActivity)
    val appCallCaptor = argumentCaptor<AppCall>()
    val fragmentWrapperCaptor = argumentCaptor<FragmentWrapper>()
    var isPresentCalled = false
    whenever(DialogPresenter.present(appCallCaptor.capture(), fragmentWrapperCaptor.capture()))
        .thenAnswer {
          isPresentCalled = true
          Unit
        }

    ShareDialog.show(mockFragment, testShareLinkContent)

    val capturedAppCall = appCallCaptor.firstValue
    // verify DialogPresenter.present is called
    assertThat(isPresentCalled).isTrue
    assertThat(fragmentWrapperCaptor.firstValue.nativeFragment).isEqualTo(mockFragment)
    // verify the request code is registered as a static callback
    assertThat(capturedAppCall.requestCode)
        .isEqualTo(staticShareCallbackRequestCodeCaptor.firstValue)
  }

  @Test
  fun `test canShow return true for link content type if the access token and the native app are not available`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(any())).thenReturn(false)
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(false)

    assertThat(ShareDialog.canShow(ShareLinkContent::class.java)).isTrue
  }

  @Test
  fun `test canShow return true for link content type if the access token is available and the native app are not available`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(any())).thenReturn(false)
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(true)

    assertThat(ShareDialog.canShow(ShareLinkContent::class.java)).isTrue
  }
  @Test
  fun `test canShow return false for photo content type if the access token and the native app are not available`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(any())).thenReturn(false)
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(false)

    assertThat(ShareDialog.canShow(SharePhotoContent::class.java)).isFalse
  }

  @Test
  fun `test canShow return true for photo content type if the access token is available and the native app are not available`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(any())).thenReturn(false)
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(true)

    assertThat(ShareDialog.canShow(SharePhotoContent::class.java)).isTrue
  }

  @Test
  fun `test canShow return false for video content type if native app are not available`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(any())).thenReturn(false)

    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(true)
    assertThat(ShareDialog.canShow(ShareVideoContent::class.java)).isFalse
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(false)
    assertThat(ShareDialog.canShow(ShareVideoContent::class.java)).isFalse
  }

  @Test
  fun `test canShow return true for video content type if native app supports video feature`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(eq(ShareDialogFeature.VIDEO)))
        .thenReturn(true)

    assertThat(ShareDialog.canShow(ShareVideoContent::class.java)).isTrue
  }

  @Test
  fun `test canShow return false for media content type if native app does not multimedia feature`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(any())).thenReturn(false)

    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(true)
    assertThat(ShareDialog.canShow(ShareMediaContent::class.java)).isFalse
    whenever(mockAccessTokenCompanion.isCurrentAccessTokenActive()).thenReturn(false)
    assertThat(ShareDialog.canShow(ShareMediaContent::class.java)).isFalse
  }

  @Test
  fun `test canShow return true for media content type if native app supports multimedia feature`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(ShareDialogFeature.MULTIMEDIA))
        .thenReturn(true)

    assertThat(ShareDialog.canShow(ShareMediaContent::class.java)).isTrue
  }

  @Test
  fun `test canShow return false for unknown content type`() {
    whenever(DialogPresenter.canPresentNativeDialogWithFeature(any())).thenReturn(true)

    assertThat(ShareDialog.canShow(UnknownContent::class.java)).isFalse
  }

  @Test
  fun `test canShow a link content with web mode is always true`() {
    val mockActivity = mock<Activity>()
    val shareDialog = ShareDialog(mockActivity)
    val shareLinkContent =
        ShareLinkContent.Builder().setContentUrl(Uri.parse("http://facebook.com")).build()

    whenever(AccessToken.isCurrentAccessTokenActive()).thenReturn(true)
    assertThat(shareDialog.canShow(shareLinkContent, ShareDialog.Mode.WEB)).isTrue
    whenever(AccessToken.isCurrentAccessTokenActive()).thenReturn(false)
    assertThat(shareDialog.canShow(shareLinkContent, ShareDialog.Mode.WEB)).isTrue
  }

  @Test
  fun `test canShow a photo content with web mode is true if access token is active`() {
    val mockActivity = mock<Activity>()
    val mockFragment = mock<android.app.Fragment>()
    whenever(mockFragment.activity).thenReturn(mockActivity)
    val shareDialog = ShareDialog(mockFragment)
    val sharePhotoContent =
        SharePhotoContent.Builder().setContentUrl(Uri.parse("http://facebook.com")).build()

    whenever(AccessToken.isCurrentAccessTokenActive()).thenReturn(true)
    assertThat(shareDialog.canShow(sharePhotoContent, ShareDialog.Mode.WEB)).isTrue
    whenever(AccessToken.isCurrentAccessTokenActive()).thenReturn(false)
    assertThat(shareDialog.canShow(sharePhotoContent, ShareDialog.Mode.WEB)).isFalse
  }

  private class UnknownContent : ShareContent<UnknownContent, UnknownContent.Builder> {
    constructor(builder: Builder) : super(builder)
    constructor(parcel: Parcel) : super(parcel)

    inner class Builder : ShareContent.Builder<UnknownContent, Builder>() {
      override fun build(): UnknownContent {
        return UnknownContent(this)
      }
    }
  }
}
