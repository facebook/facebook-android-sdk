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
