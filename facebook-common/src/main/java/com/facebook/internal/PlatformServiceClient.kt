/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import com.facebook.internal.NativeProtocol.createPlatformServiceIntent
import com.facebook.internal.NativeProtocol.getLatestAvailableProtocolVersionForService
import java.lang.IllegalArgumentException

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
abstract class PlatformServiceClient(
    context: Context,
    requestMessage: Int,
    replyMessage: Int,
    protocolVersion: Int,
    applicationId: String,
    redirectURI: String,
    nonce: String?
) : ServiceConnection {
  protected val context: Context
  private val handler: Handler
  private var listener: CompletedListener? = null
  private var running = false
  private var sender: Messenger? = null
  private val requestMessage: Int
  private val replyMessage: Int
  private val applicationId: String
  private val redirectURI: String?
  private val protocolVersion: Int
  val nonce: String?

  fun setCompletedListener(listener: CompletedListener?) {
    this.listener = listener
  }

  fun start(): Boolean {
    synchronized(this) {
      if (running) {
        return false
      }

      // Make sure that the service can handle the requested protocol version
      val availableVersion = getLatestAvailableProtocolVersionForService(protocolVersion)
      if (availableVersion == NativeProtocol.NO_PROTOCOL_AVAILABLE) {
        return false
      }
      val intent = createPlatformServiceIntent(context)
      return if (intent == null) {
        false
      } else {
        running = true
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        true
      }
    }
  }

  fun cancel() {
    running = false
  }

  override fun onServiceConnected(name: ComponentName, service: IBinder) {
    sender = Messenger(service)
    sendMessage()
  }

  override fun onServiceDisconnected(name: ComponentName) {
    sender = null
    try {
      context.unbindService(this)
    } catch (ex: IllegalArgumentException) {
      // Do nothing, the connection was already unbound
    }
    callback(null)
  }

  private fun sendMessage() {
    val data = Bundle()
    data.putString(NativeProtocol.EXTRA_APPLICATION_ID, applicationId)
    if (nonce != null) {
      data.putString(NativeProtocol.EXTRA_NONCE, nonce)
    }
    if (redirectURI != null) {
      data.putString(NativeProtocol.EXTRA_REDIRECT_URI, redirectURI)
    }
    populateRequestBundle(data)
    val request = Message.obtain(null, requestMessage)
    request.arg1 = protocolVersion
    request.data = data
    request.replyTo = Messenger(handler)
    try {
      sender?.send(request)
    } catch (e: RemoteException) {
      callback(null)
    }
  }

  protected abstract fun populateRequestBundle(data: Bundle)
  protected fun handleMessage(message: Message) {
    if (message.what == replyMessage) {
      val extras = message.data
      val errorType = extras.getString(NativeProtocol.STATUS_ERROR_TYPE)
      if (errorType != null) {
        callback(null)
      } else {
        callback(extras)
      }
      try {
        context.unbindService(this)
      } catch (ex: IllegalArgumentException) {
        // Do nothing, the connection was already unbound
      }
    }
  }

  private fun callback(result: Bundle?) {
    if (!running) {
      return
    }
    running = false
    val callback = listener
    callback?.completed(result)
  }

  fun interface CompletedListener {
    fun completed(result: Bundle?)
  }

  init {
    val applicationContext = context.applicationContext
    this.context = applicationContext ?: context
    this.requestMessage = requestMessage
    this.replyMessage = replyMessage
    this.applicationId = applicationId
    this.redirectURI = redirectURI
    this.protocolVersion = protocolVersion
    this.nonce = nonce
    handler =
        object : Handler() {
          override fun handleMessage(message: Message) {
            this@PlatformServiceClient.handleMessage(message)
          }
        }
  }
}
