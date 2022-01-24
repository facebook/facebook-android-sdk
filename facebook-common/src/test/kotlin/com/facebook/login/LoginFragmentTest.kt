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

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.facebook.FacebookTestCase
import com.facebook.common.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController

class LoginFragmentTest : FacebookTestCase() {
  // test activity: it's the host for the login fragment during robolectric test
  open class TestActivity : FragmentActivity() {
    lateinit var fragment: LoginFragment
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      fragment = LoginFragment()
      supportFragmentManager.beginTransaction().add(fragment, "FRAGMENT_TAG").commit()
    }
  }

  private lateinit var activityController: ActivityController<TestActivity>

  override fun setUp() {
    super.setUp()
    activityController = Robolectric.buildActivity(TestActivity::class.java)
  }

  @Test
  fun `test creating the fragment will create a login client`() {
    activityController.create()
    activityController.start()

    val fragment = activityController.get().fragment
    assertThat(fragment.loginClient).isNotNull
    assertThat(fragment.loginClient.onCompletedListener).isNotNull
  }

  @Test
  fun `test finishing the activity when the login client complete `() {
    val request =
        LoginClient.Request(
            LoginBehavior.NATIVE_WITH_FALLBACK,
            null,
            DefaultAudience.EVERYONE,
            null,
            null,
            null,
            LoginTargetApp.FACEBOOK)
    val result = LoginClient.Result(request, LoginClient.Result.Code.CANCEL, null, null, null)

    activityController.create()
    activityController.start()

    val fragment = activityController.get().fragment
    val loginClient = fragment.loginClient
    loginClient.complete(result)
    val shadowActivity = Shadows.shadowOf(activityController.get())
    assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    assertThat(activityController.get().isFinishing).isTrue()
  }

  @Test
  fun `test fragment display progress bar when login client processing `() {
    activityController.create()
    activityController.start()
    val fragment = activityController.get().fragment
    val loginClient = fragment.loginClient
    loginClient.notifyBackgroundProcessingStart()
    val progressBar =
        fragment.view?.findViewById<View>(R.id.com_facebook_login_fragment_progress_bar)
    assertThat(progressBar?.visibility).isEqualTo(View.VISIBLE)
  }
}
