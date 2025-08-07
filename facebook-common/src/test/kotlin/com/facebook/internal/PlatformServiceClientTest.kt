/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
    private const val HTTPS_REDIRECT_URI = "https://example.com/my/redirect"
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
                HTTPS_REDIRECT_URI,
                NONCE) {
          override fun populateRequestBundle(data: Bundle) {
            data.putString("TEST", "TEST_DATA")
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
