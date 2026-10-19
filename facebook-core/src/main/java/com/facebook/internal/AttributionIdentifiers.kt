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
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import com.facebook.internal.Utility.getMethodQuietly
import com.facebook.internal.Utility.invokeMethodQuietly
import com.facebook.internal.Utility.logd
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class AttributionIdentifiers {
  private var androidAdvertiserIdValue: String? = null
  private var fetchTime: Long = 0
  private var advertisingIdReadTime: Long = 0
  private var isAdvertisingIdCarriedOver = false
  var attributionId: String? = null
    private set
  var androidInstallerPackage: String? = null
    private set
  var isTrackingLimited = false
    private set

  /**
   * Whether these identifiers hold a successful advertising ID read, from this fetch or carried
   * over from a recent one. Independent of the advertiser ID collection setting, which hides the
   * ID itself but not the limit-tracking flag read with it.
   */
  internal val isAdvertisingIdRead: Boolean
    get() = androidAdvertiserIdValue != null

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
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal const val ATTRIBUTION_ID_CONTENT_PROVIDER =
        "com.facebook.katana.provider.AttributionIdProvider"
    private const val ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI =
        "com.facebook.wakizashi.provider.AttributionIdProvider"
    private const val ATTRIBUTION_ID_COLUMN_NAME = "aid"
    private const val ANDROID_ID_COLUMN_NAME = "androidid"
    private const val LIMIT_TRACKING_COLUMN_NAME = "limit_tracking"

    // com.google.android.gms.common.ConnectionResult.SUCCESS
    private const val CONNECTION_RESULT_SUCCESS = 0
    private const val IDENTIFIER_REFRESH_INTERVAL_MILLIS = (3600 * 1000).toLong()
    private const val FAILED_READ_RETRY_INTERVAL_MILLIS = (5 * 60 * 1000).toLong()
    // A good read outlives its refresh by at most one more interval while reads keep failing.
    private const val CARRY_OVER_MAX_AGE_MILLIS = 2 * IDENTIFIER_REFRESH_INTERVAL_MILLIS

    // Matches the bind timeout Google's AdvertisingIdClient uses.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmField
    internal var serviceBindTimeoutMillis = 10_000L

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Volatile
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
        // AdvertisingIdClient checks Google Play services availability itself. The pre-check
        // looks up a class from play-services-base, which apps can bundle the client without.
        if (!isAdvertisingIdReliabilityEnabled() && !isGooglePlayServicesAvailable(context)) {
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
      } catch (e: LinkageError) {
        // The client class can resolve while a class it depends on cannot.
        logd("android_id", "Advertising ID client failed to load", e)
      }
      return null
    }

    private fun isAdvertisingIdReliabilityEnabled(): Boolean =
        FeatureManager.isEnabled(FeatureManager.Feature.AndroidAdvertisingIDReliability)

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
      if (!isGooglePlayServicesAvailable(context)) {
        return null
      }
      
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
      // Reading the advertising ID blocks on Google Play services, so refuse before reading:
      // on the main thread the service connection can never be delivered.
      if (Looper.myLooper() == Looper.getMainLooper()) {
        logd(TAG, "getAttributionIdentifiers cannot be called on the main thread.")
        return null
      }
      val readCacheFirst = isAdvertisingIdReliabilityEnabled()
      if (readCacheFirst) {
        val cachedIdentifiers = this.cachedIdentifiers
        if (cachedIdentifiers != null && isFresh(cachedIdentifiers)) {
          return cachedIdentifiers
        }
      }
      val identifiers = getAndroidId(context)
      var c: Cursor? = null
      try {
        val cachedIdentifiers = this.cachedIdentifiers
        if (!readCacheFirst &&
            cachedIdentifiers != null &&
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
          providerUri = Uri.parse("content://$ATTRIBUTION_ID_CONTENT_PROVIDER")
        } else if (wakizashiProviderInfo != null &&
            validateSignature(context, wakizashiProviderInfo.packageName)) {
          providerUri = Uri.parse("content://$ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI")
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

    // A failed read is retried sooner than a successful one is refreshed. A fetch time in the
    // future means the clock moved backwards, so it is treated as stale.
    private fun isFresh(identifiers: AttributionIdentifiers): Boolean {
      val age = System.currentTimeMillis() - identifiers.fetchTime
      val maxAge =
          if (identifiers.androidAdvertiserIdValue == null ||
              identifiers.isAdvertisingIdCarriedOver) {
            FAILED_READ_RETRY_INTERVAL_MILLIS
          } else {
            IDENTIFIER_REFRESH_INTERVAL_MILLIS
          }
      return age in 0 until maxAge
    }

    private fun cacheAndReturnIdentifiers(
        identifiers: AttributionIdentifiers
    ): AttributionIdentifiers {
      val now = System.currentTimeMillis()
      identifiers.fetchTime = now
      if (identifiers.isAdvertisingIdRead) {
        identifiers.advertisingIdReadTime = now
      } else if (isAdvertisingIdReliabilityEnabled()) {
        carryOverRecentRead(identifiers, now)
      }
      cachedIdentifiers = identifiers
      return identifiers
    }

    // A failed refresh would otherwise replace a good read with blank identifiers, dropping the ID
    // and its limit-tracking flag for every caller until a later read succeeds. Resets and opt-outs
    // arrive as successful reads, so they are never masked by this.
    private fun carryOverRecentRead(identifiers: AttributionIdentifiers, now: Long) {
      val previous = cachedIdentifiers ?: return
      if (!previous.isAdvertisingIdRead ||
          now - previous.advertisingIdReadTime !in 0 until CARRY_OVER_MAX_AGE_MILLIS) {
        return
      }
      identifiers.androidAdvertiserIdValue = previous.androidAdvertiserIdValue
      identifiers.isTrackingLimited = previous.isTrackingLimited
      identifiers.advertisingIdReadTime = previous.advertisingIdReadTime
      identifiers.isAdvertisingIdCarriedOver = true
    }

    @Suppress("DEPRECATION")
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
      } catch (e: InterruptedException) {
        logd(TAG, "Service connection interrupted", e)
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) = Unit

    @get:Throws(InterruptedException::class, TimeoutException::class)
    val binder: IBinder
      get() {
        check(consumed.compareAndSet(false, true)) { "Binder already consumed" }
        return queue.poll(serviceBindTimeoutMillis, TimeUnit.MILLISECONDS)
            ?: throw TimeoutException("Timed out waiting for the advertising ID service")
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
