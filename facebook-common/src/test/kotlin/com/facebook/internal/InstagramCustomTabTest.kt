/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.login.CustomTabLoginMethodHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InstagramCustomTabTest : FacebookPowerMockTestCase() {
  @Test
  fun `test get URI for oauth action`() {
    val parameters = Bundle()
    parameters.putString(ServerProtocol.DIALOG_PARAM_SCOPE, "user_name,user_birthday")
    val uri =
        InstagramCustomTab.getURIForAction(CustomTabLoginMethodHandler.OAUTH_DIALOG, parameters)
    assertThat(uri.toString())
        .isEqualTo("https://m.instagram.com/oauth/authorize?scope=user_name%2Cuser_birthday")
  }

  @Test
  fun `test get URI for other action`() {
    val parameters = Bundle()
    parameters.putString("status", "Hi Instagram")
    val uri = InstagramCustomTab.getURIForAction("share", parameters)
    val version = FacebookSdk.getGraphApiVersion()
    assertThat(uri.toString())
        .isEqualTo("https://m.instagram.com/$version/dialog/share?status=Hi%20Instagram")
  }
}
