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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Looper
import android.os.Parcel
import android.os.RemoteException
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import com.facebook.internal.Utility.getMethodQuietly
import com.facebook.internal.Utility.invokeMethodQuietly
import com.facebook.internal.Utility.logd
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class AttributionIdentifiers {
  private var androidAdvertiserIdValue: String? = null
  private var fetchTime: Long = 0
  var attributionId: String? = null
    private set
  var androidInstallerPackage: String? = null
    private set
  var isTrackingLimited = false
    private set

  val androidAdvertiserId: String?
    get() {
      return if (FacebookSdk.isInitialized() && FacebookSdk.getAdvertiserIDCollectionEnabled()) {
        androidAdvertiserIdValue
      } else {
        null
      }
    }

  companion object {
    private val TAG = AttributionIdentifiers::class.java.canonicalName
    private const val ATTRIBUTION_ID_CONTENT_PROVIDER =
        "com.facebook.katana.provider.AttributionIdProvider"
    private const val ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI =
        "com.facebook.wakizashi.provider.AttributionIdProvider"
    private const val ATTRIBUTION_ID_COLUMN_NAME = "aid"
    private const val ANDROID_ID_COLUMN_NAME = "androidid"
    private const val LIMIT_TRACKING_COLUMN_NAME = "limit_tracking"

    // com.google.android.gms.common.ConnectionResult.SUCCESS
    private const val CONNECTION_RESULT_SUCCESS = 0
    private const val IDENTIFIER_REFRESH_INTERVAL_MILLIS = (3600 * 1000).toLong()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmField
    internal var cachedIdentifiers: AttributionIdentifiers? = null

    private fun getAndroidId(context: Context): AttributionIdentifiers {
      var identifiers = getAndroidIdViaReflection(context)
      if (identifiers == null) {
        identifiers = getAndroidIdViaService(context)
        if (identifiers == null) {
          identifiers = AttributionIdentifiers()
        }
      }
      return identifiers
    }

    private fun getAndroidIdViaReflection(context: Context): AttributionIdentifiers? {
      try {
        if (!isGooglePlayServicesAvailable(context)) {
          return null
        }
        val getAdvertisingIdInfo =
            getMethodQuietly(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                "getAdvertisingIdInfo",
                Context::class.java)
                ?: return null
        val advertisingInfo =
            invokeMethodQuietly(null, getAdvertisingIdInfo, context) ?: return null
        val getId = getMethodQuietly(advertisingInfo.javaClass, "getId")
        val isLimitAdTrackingEnabled =
            getMethodQuietly(advertisingInfo.javaClass, "isLimitAdTrackingEnabled")
        if (getId == null || isLimitAdTrackingEnabled == null) {
          return null
        }
        val identifiers = AttributionIdentifiers()
        identifiers.androidAdvertiserIdValue =
            invokeMethodQuietly(advertisingInfo, getId) as String?
        identifiers.isTrackingLimited =
            (invokeMethodQuietly(advertisingInfo, isLimitAdTrackingEnabled) as Boolean?) ?: false
        return identifiers
      } catch (e: Exception) {
        logd("android_id", e)
      }
      return null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmStatic
    fun isTrackingLimited(context: Context): Boolean {
      val attributionIdentifiers = getAttributionIdentifiers(context)
      return attributionIdentifiers != null && attributionIdentifiers.isTrackingLimited
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
      val method =
          getMethodQuietly(
              "com.google.android.gms.common.GooglePlayServicesUtil",
              "isGooglePlayServicesAvailable",
              Context::class.java)
              ?: return false
      val connectionResult = invokeMethodQuietly(null, method, context)
      return !(connectionResult !is Int || connectionResult != CONNECTION_RESULT_SUCCESS)
    }

    private fun getAndroidIdViaService(context: Context): AttributionIdentifiers? {
      val connection = GoogleAdServiceConnection()
      val intent = Intent("com.google.android.gms.ads.identifier.service.START")
      intent.setPackage("com.google.android.gms")
      val isBindServiceSucceed =
          try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
          } catch (_: SecurityException) {
            return null
          }
      if (isBindServiceSucceed) {
        try {
          val adInfo = GoogleAdInfo(connection.binder)
          val identifiers = AttributionIdentifiers()
          identifiers.androidAdvertiserIdValue = adInfo.advertiserId
          identifiers.isTrackingLimited = adInfo.isTrackingLimited
          return identifiers
        } catch (exception: Exception) {
          logd("android_id", exception)
        } finally {
          context.unbindService(connection)
        }
      }
      return null
    }

    @JvmStatic
    fun getAttributionIdentifiers(context: Context): AttributionIdentifiers? {
      val identifiers = getAndroidId(context)
      var c: Cursor? = null
      try {
        // We can't call getAdvertisingIdInfo on the main thread or the app will potentially
        // freeze, if this is the case throw:
        if (Looper.myLooper() == Looper.getMainLooper()) {
          throw FacebookException("getAttributionIdentifiers cannot be called on the main thread.")
        }
        val cachedIdentifiers = this.cachedIdentifiers
        if (cachedIdentifiers != null &&
            System.currentTimeMillis() - cachedIdentifiers.fetchTime <
                IDENTIFIER_REFRESH_INTERVAL_MILLIS) {
          return cachedIdentifiers
        }
        val projection =
            arrayOf(ATTRIBUTION_ID_COLUMN_NAME, ANDROID_ID_COLUMN_NAME, LIMIT_TRACKING_COLUMN_NAME)
        var providerUri: Uri? = null
        val contentProviderInfo =
            context.packageManager.resolveContentProvider(ATTRIBUTION_ID_CONTENT_PROVIDER, 0)
        val wakizashiProviderInfo =
            context.packageManager.resolveContentProvider(
                ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI, 0)
        if (contentProviderInfo != null &&
            validateSignature(context, contentProviderInfo.packageName)) {
          providerUri = Uri.parse("content://" + ATTRIBUTION_ID_CONTENT_PROVIDER)
        } else if (wakizashiProviderInfo != null &&
            validateSignature(context, wakizashiProviderInfo.packageName)) {
          providerUri = Uri.parse("content://" + ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI)
        }
        val installerPackageName = getInstallerPackageName(context)
        if (installerPackageName != null) {
          identifiers.androidInstallerPackage = installerPackageName
        }
        if (providerUri == null) {
          return cacheAndReturnIdentifiers(identifiers)
        }
        c = context.contentResolver.query(providerUri, projection, null, null, null)
        if (c == null || !c.moveToFirst()) {
          return cacheAndReturnIdentifiers(identifiers)
        }
        val attributionColumnIndex = c.getColumnIndex(ATTRIBUTION_ID_COLUMN_NAME)
        val androidIdColumnIndex = c.getColumnIndex(ANDROID_ID_COLUMN_NAME)
        val limitTrackingColumnIndex = c.getColumnIndex(LIMIT_TRACKING_COLUMN_NAME)
        identifiers.attributionId = c.getString(attributionColumnIndex)

        // if we failed to call Google's APIs directly (due to improper integration by the
        // client), it may be possible for the local facebook application to relay it to us.
        if (androidIdColumnIndex > 0 &&
            limitTrackingColumnIndex > 0 &&
            identifiers.androidAdvertiserId == null) {
          identifiers.androidAdvertiserIdValue = c.getString(androidIdColumnIndex)
          identifiers.isTrackingLimited =
              java.lang.Boolean.parseBoolean(c.getString(limitTrackingColumnIndex))
        }
      } catch (e: Exception) {
        logd(TAG, "Caught unexpected exception in getAttributionId(): $e")
        return null
      } finally {
        c?.close()
      }
      return cacheAndReturnIdentifiers(identifiers)
    }

    private fun cacheAndReturnIdentifiers(
        identifiers: AttributionIdentifiers
    ): AttributionIdentifiers {
      identifiers.fetchTime = System.currentTimeMillis()
      cachedIdentifiers = identifiers
      return identifiers
    }

    private fun getInstallerPackageName(context: Context): String? {
      val packageManager = context.packageManager
      return packageManager?.getInstallerPackageName(context.packageName)
    }
  }

  private class GoogleAdServiceConnection : ServiceConnection {
    private val consumed = AtomicBoolean(false)
    private val queue: BlockingQueue<IBinder> = LinkedBlockingDeque()
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      try {
        if (service != null) {
          queue.put(service)
        }
      } catch (e: InterruptedException) {}
    }

    override fun onServiceDisconnected(name: ComponentName?) = Unit

    @get:Throws(InterruptedException::class)
    val binder: IBinder
      get() {
        check(!consumed.compareAndSet(true, true)) { "Binder already consumed" }
        return queue.take()
      }
  }

  private class GoogleAdInfo constructor(private val binder: IBinder) : IInterface {
    override fun asBinder(): IBinder = binder

    @get:Throws(RemoteException::class)
    val advertiserId: String?
      get() {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
          data.writeInterfaceToken(
              "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
          binder.transact(FIRST_TRANSACTION_CODE, data, reply, 0)
          reply.readException()
          reply.readString()
        } finally {
          reply.recycle()
          data.recycle()
        }
      }

    @get:Throws(RemoteException::class)
    val isTrackingLimited: Boolean
      get() {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
          data.writeInterfaceToken(
              "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
          data.writeInt(1)
          binder.transact(SECOND_TRANSACTION_CODE, data, reply, 0)
          reply.readException()
          0 != reply.readInt()
        } finally {
          reply.recycle()
          data.recycle()
        }
      }

    companion object {
      private const val FIRST_TRANSACTION_CODE = Binder.FIRST_CALL_TRANSACTION
      private const val SECOND_TRANSACTION_CODE = FIRST_TRANSACTION_CODE + 1
    }
  }
}
