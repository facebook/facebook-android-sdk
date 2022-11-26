/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless.internal

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(UnityReflection::class)
class UnityReflectionTest : FacebookPowerMockTestCase() {

  override fun setup() {
    super.setup()
    MemberModifier.stub<Class<*>>(
            PowerMockito.method(UnityReflection::class.java, "getUnityPlayerClass"))
        .toReturn(MockUnityPlayer::class.java)
  }

  @Test
  fun `test captureViewHierarchy`() {
    UnityReflection.captureViewHierarchy()
    assertThat(MockUnityPlayer.capturedObject).isEqualTo("UnityFacebookSDKPlugin")
    assertThat(MockUnityPlayer.capturedMethod).isEqualTo("CaptureViewHierarchy")
    assertThat(MockUnityPlayer.capturedMessage).isEqualTo("")
  }

  @Test
  fun `test sendEventMapping`() {
    val eventMapping = "test event mapping"
    UnityReflection.sendEventMapping(eventMapping)
    assertThat(MockUnityPlayer.capturedObject).isEqualTo("UnityFacebookSDKPlugin")
    assertThat(MockUnityPlayer.capturedMethod).isEqualTo("OnReceiveMapping")
    assertThat(MockUnityPlayer.capturedMessage).isEqualTo(eventMapping)
  }

  object MockUnityPlayer {
    var capturedObject: String? = null
    var capturedMethod: String? = null
    var capturedMessage: String? = null
    @JvmStatic
    fun UnitySendMessage(obj: String?, method: String?, message: String?) {
      capturedObject = obj
      capturedMethod = method
      capturedMessage = message
    }
  }
}
