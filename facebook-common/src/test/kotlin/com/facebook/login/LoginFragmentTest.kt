/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.facebook.FacebookTestCase
import com.facebook.common.R
import java.util.UUID
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
            "testAuthType",
            "123456789",
            UUID.randomUUID().toString(),
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
