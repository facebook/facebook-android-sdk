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

package com.facebook.devicerequests.internal

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
import com.facebook.internal.Utility
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import java.lang.IllegalArgumentException
import java.util.EnumMap
import org.json.JSONObject

/**
 * com.facebook.devicerequests.internal is solely for the use of other packages within the Facebook
 * SDK for Android. Use of any of the classes in this package is unsupported, and they may be
 * modified or removed without warning at any time.
 */
@AutoHandleExceptions
object DeviceRequestsHelper {
  private val TAG = DeviceRequestsHelper::class.java.canonicalName
  const val DEVICE_INFO_PARAM = "device_info"
  const val DEVICE_TARGET_USER_ID = "target_user_id"
  const val DEVICE_INFO_DEVICE = "device"
  const val DEVICE_INFO_MODEL = "model"
  const val SDK_HEADER = "fbsdk"
  const val SDK_FLAVOR = "android"
  const val SERVICE_TYPE = "_fb._tcp."
  private val deviceRequestsListeners = HashMap<String?, NsdManager.RegistrationListener>()

  @JvmStatic
  fun getDeviceInfo(deviceInfo: MutableMap<String, String>?): String {
    // Device info
    // We don't need all the information in Utility.setAppEventExtendedDeviceInfoParameters
    // We only want the model so we can show it to the user, so they know which device
    // the login request comes from
    val deviceInfo = deviceInfo ?: HashMap()
    deviceInfo[DEVICE_INFO_DEVICE] = Build.DEVICE
    deviceInfo[DEVICE_INFO_MODEL] = Build.MODEL
    return JSONObject(deviceInfo as Map<*, *>).toString()
  }

  @JvmStatic
  fun getDeviceInfo(): String {
    return getDeviceInfo(null)
  }

  @JvmStatic
  fun startAdvertisementService(userCode: String?): Boolean {
    return if (isAvailable()) {
      startAdvertisementServiceImpl(userCode)
    } else false
  }

  /*
  returns true if smart login is enabled and the android api level is supported.
   */
  @JvmStatic
  fun isAvailable(): Boolean {
    val settings =
        FetchedAppSettingsManager.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId())
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
        settings != null &&
        settings.smartLoginOptions.contains(SmartLoginOption.Enabled))
  }

  @JvmStatic
  fun generateQRCode(url: String?): Bitmap? {
    var qrCode: Bitmap? = null
    val hints: MutableMap<EncodeHintType, Any?> = EnumMap(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 2
    try {
      val matrix = MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 200, 200, hints)
      val h = matrix.height
      val w = matrix.width
      val pixels = IntArray(h * w)
      for (i in 0 until h) {
        val offset = i * w
        for (j in 0 until w) {
          pixels[offset + j] = if (matrix[j, i]) Color.BLACK else Color.WHITE
        }
      }
      qrCode = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      qrCode.setPixels(pixels, 0, w, 0, 0, w, h)
    } catch (ignored: WriterException) {
      // ignored because exception would be thrown from ZXing library.
    }
    return qrCode
  }

  @JvmStatic
  fun cleanUpAdvertisementService(userCode: String?) {
    cleanUpAdvertisementServiceImpl(userCode)
  }

  @TargetApi(16)
  private fun startAdvertisementServiceImpl(userCode: String?): Boolean {
    if (deviceRequestsListeners.containsKey(userCode)) {
      return true
    }
    // Dots in the version will mess up the Bonjour DNS record parsing
    val sdkVersion = FacebookSdk.getSdkVersion().replace('.', '|')
    // Other SDKs that adopt this feature should use different flavor name
    // The whole name should not exceed 60 characters
    val sdkVersionWithFlavor = "$SDK_FLAVOR-$sdkVersion"
    val nsdServiceName = "${SDK_HEADER}_${sdkVersionWithFlavor}_${userCode}"
    val nsdLoginAdvertisementService = NsdServiceInfo()
    nsdLoginAdvertisementService.serviceType = SERVICE_TYPE
    nsdLoginAdvertisementService.serviceName = nsdServiceName
    nsdLoginAdvertisementService.port = 80
    val nsdManager =
        FacebookSdk.getApplicationContext().getSystemService(Context.NSD_SERVICE) as NsdManager
    val nsdRegistrationListener: NsdManager.RegistrationListener =
        object : NsdManager.RegistrationListener {
          override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Android may have changed the service name in order to resolve a conflict
            if (nsdServiceName != NsdServiceInfo.serviceName) {
              cleanUpAdvertisementService(userCode)
            }
          }

          override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
          override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            cleanUpAdvertisementService(userCode)
          }

          override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
    deviceRequestsListeners[userCode] = nsdRegistrationListener
    nsdManager.registerService(
        nsdLoginAdvertisementService, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener)
    return true
  }

  @TargetApi(16)
  private fun cleanUpAdvertisementServiceImpl(userCode: String?) {
    val nsdRegistrationListener = deviceRequestsListeners[userCode]
    if (nsdRegistrationListener != null) {
      val nsdManager =
          FacebookSdk.getApplicationContext().getSystemService(Context.NSD_SERVICE) as NsdManager
      try {
        nsdManager.unregisterService(nsdRegistrationListener)
      } catch (e: IllegalArgumentException) {
        Utility.logd(TAG, e)
      }
      deviceRequestsListeners.remove(userCode)
    }
  }
}
