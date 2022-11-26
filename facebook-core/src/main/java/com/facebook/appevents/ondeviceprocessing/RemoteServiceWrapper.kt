/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.ondeviceprocessing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.internal.AppEventUtility.assertIsNotMainThread
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import com.facebook.internal.Utility.logd
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.ppml.receiver.IReceiverService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object RemoteServiceWrapper {
  private val TAG = RemoteServiceWrapper::class.java.simpleName
  const val RECEIVER_SERVICE_ACTION = "ReceiverService"
  const val RECEIVER_SERVICE_PACKAGE = "com.facebook.katana"
  const val RECEIVER_SERVICE_PACKAGE_WAKIZASHI = "com.facebook.wakizashi"
  private var isServiceAvailable: Boolean? = null

  @JvmStatic
  fun isServiceAvailable(): Boolean {
    if (isServiceAvailable == null) {
      val context = FacebookSdk.getApplicationContext()
      isServiceAvailable = getVerifiedServiceIntent(context) != null
    }
    return isServiceAvailable ?: false
  }

  /**
   * Synchronously sends install event to the remote service. **Do not call from the UI thread.**
   *
   * @returns [ServiceResult.OPERATION_SUCCESS] if events were sent successfully. Other
   * [ServiceResult] indicate failure and are not recoverable.
   */
  @JvmStatic
  fun sendInstallEvent(applicationId: String): ServiceResult {
    return sendEvents(EventType.MOBILE_APP_INSTALL, applicationId, listOf())
  }

  /**
   * Synchronously sends custom app events to the remote service. **Do not call from the UI
   * thread.**
   *
   * @returns [ServiceResult.OPERATION_SUCCESS] if events were sent successfully. Other
   * [ServiceResult] indicate failure and are not recoverable.
   */
  @JvmStatic
  fun sendCustomEvents(applicationId: String, appEvents: List<AppEvent>): ServiceResult {
    return sendEvents(EventType.CUSTOM_APP_EVENTS, applicationId, appEvents)
  }

  private fun sendEvents(
      eventType: EventType,
      applicationId: String,
      appEvents: List<AppEvent>
  ): ServiceResult {
    var serviceResult = ServiceResult.SERVICE_NOT_AVAILABLE
    assertIsNotMainThread()
    val context = FacebookSdk.getApplicationContext()
    val verifiedIntent = getVerifiedServiceIntent(context)
    if (verifiedIntent != null) {
      val connection = RemoteServiceConnection()
      if (context.bindService(verifiedIntent, connection, Context.BIND_AUTO_CREATE)) {
        try {
          val binder = connection.getBinder()
          serviceResult =
              if (binder != null) {
                val service = IReceiverService.Stub.asInterface(binder)
                val eventBundle =
                    RemoteServiceParametersHelper.buildEventsBundle(
                        eventType, applicationId, appEvents)
                if (eventBundle != null) {
                  service.sendEvents(eventBundle)
                  logd(TAG, "Successfully sent events to the remote service: $eventBundle")
                }
                ServiceResult.OPERATION_SUCCESS
              } else {
                ServiceResult.SERVICE_NOT_AVAILABLE
              }
        } catch (exception: InterruptedException) {
          serviceResult = ServiceResult.SERVICE_ERROR
          logd(TAG, exception)
        } catch (exception: RemoteException) {
          serviceResult = ServiceResult.SERVICE_ERROR
          logd(TAG, exception)
        } finally {
          context.unbindService(connection)
          logd(TAG, "Unbound from the remote service")
        }
      } else {
        serviceResult = ServiceResult.SERVICE_ERROR
      }
    }
    return serviceResult
  }

  private fun getVerifiedServiceIntent(context: Context): Intent? {
    val packageManager = context.packageManager
    if (packageManager != null) {
      val serviceIntent = Intent(RECEIVER_SERVICE_ACTION)
      serviceIntent.setPackage(RECEIVER_SERVICE_PACKAGE)
      val serviceInfo = packageManager.resolveService(serviceIntent, 0)
      if (serviceInfo != null && validateSignature(context, RECEIVER_SERVICE_PACKAGE)) {
        return serviceIntent
      } else {
        val wakizashiServiceIntent = Intent(RECEIVER_SERVICE_ACTION)
        wakizashiServiceIntent.setPackage(RECEIVER_SERVICE_PACKAGE_WAKIZASHI)
        val wakizashiServiceInfo = packageManager.resolveService(wakizashiServiceIntent, 0)
        if (wakizashiServiceInfo != null &&
            validateSignature(context, RECEIVER_SERVICE_PACKAGE_WAKIZASHI)) {
          return wakizashiServiceIntent
        }
      }
    }
    return null
  }

  enum class ServiceResult {
    OPERATION_SUCCESS,
    SERVICE_NOT_AVAILABLE,
    SERVICE_ERROR
  }

  enum class EventType(private val eventType: String) {
    MOBILE_APP_INSTALL("MOBILE_APP_INSTALL"),
    CUSTOM_APP_EVENTS("CUSTOM_APP_EVENTS");

    override fun toString(): String = eventType
  }

  private class RemoteServiceConnection : ServiceConnection {
    private val latch = CountDownLatch(1)
    private var binder: IBinder? = null
    override fun onServiceConnected(name: ComponentName, serviceBinder: IBinder) {
      binder = serviceBinder
      latch.countDown()
    }

    override fun onNullBinding(name: ComponentName) {
      latch.countDown()
    }

    override fun onServiceDisconnected(name: ComponentName) = Unit

    @Throws(InterruptedException::class)
    fun getBinder(): IBinder? {
      latch.await(5, TimeUnit.SECONDS)
      return binder
    }
  }
}
