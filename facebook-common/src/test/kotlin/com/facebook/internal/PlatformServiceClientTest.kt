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

package com.facebook.internal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(PlatformServiceClient::class, NativeProtocol::class)
class PlatformServiceClientTest : FacebookPowerMockTestCase() {
  private lateinit var mockContext: Context
  private lateinit var mockIntent: Intent
  private lateinit var mockMessenger: Messenger
  private lateinit var testClient: PlatformServiceClient

  companion object {
    private const val APPLICATION_ID = "123456789"
    private const val NONCE = "nonce"
    private const val REQUEST_MESSAGE = 0x7E37
    private const val REPLY_MESSAGE = 0x7E38
    private const val PROTOCOL_VERSION = NativeProtocol.PROTOCOL_VERSION_20121101
  }

  override fun setup() {
    super.setup()
    mockContext = mock()
    whenever(mockContext.applicationContext).thenReturn(mockContext)
    mockIntent = mock()
    mockMessenger = mock()
    PowerMockito.mockStatic(NativeProtocol::class.java)
    PowerMockito.`when`(
            NativeProtocol.getLatestAvailableProtocolVersionForService(PROTOCOL_VERSION))
        .thenReturn(PROTOCOL_VERSION)
    PowerMockito.`when`(NativeProtocol.createPlatformServiceIntent(mockContext))
        .thenReturn(mockIntent)
    PowerMockito.whenNew(Messenger::class.java).withAnyArguments().thenReturn(mockMessenger)

    testClient =
        object :
            PlatformServiceClient(
                mockContext,
                REQUEST_MESSAGE,
                REPLY_MESSAGE,
                PROTOCOL_VERSION,
                APPLICATION_ID,
                NONCE) {
          override fun populateRequestBundle(data: Bundle?) {
            data?.putString("TEST", "TEST_DATA")
          }
        }
  }

  @Test
  fun `test start will bind the client to the application context`() {
    assertThat(testClient.start()).isTrue
    verify(mockContext).bindService(mockIntent, testClient, Context.BIND_AUTO_CREATE)
    assertThat(testClient.start()).isFalse
  }

  @Test
  fun `test onServiceConnected will send message to service`() {
    testClient.start()
    testClient.onServiceConnected(mock(), mock())
    val messageCaptor = argumentCaptor<Message>()
    verify(mockMessenger).send(messageCaptor.capture())
    val capturedMessage = messageCaptor.firstValue
    assertThat(capturedMessage.data["TEST"]).isEqualTo("TEST_DATA")
  }

  @Test
  fun `test onServiceDisconnected will unbind the client`() {
    testClient.start()
    testClient.onServiceDisconnected(mock())
    verify(mockContext).unbindService(testClient)
  }
}
