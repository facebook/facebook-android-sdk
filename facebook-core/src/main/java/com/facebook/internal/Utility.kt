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
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcel
import android.os.StatFs
import android.provider.OpenableColumns
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.autofill.AutofillManager
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import com.facebook.AccessToken
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.HttpMethod
import com.facebook.appevents.UserDataStore
import com.facebook.internal.ProfileInformationCache.getProfileInformation
import com.facebook.internal.ProfileInformationCache.putProfileInformation
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URLConnection
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object Utility {
  const val LOG_TAG = "FacebookSDK"
  private const val HASH_ALGORITHM_MD5 = "MD5"
  private const val HASH_ALGORITHM_SHA1 = "SHA-1"
  private const val HASH_ALGORITHM_SHA256 = "SHA-256"
  private const val URL_SCHEME = "https"
  private const val EXTRA_APP_EVENTS_INFO_FORMAT_VERSION = "a2"
  private const val UTF8 = "UTF-8"

  // This is the default used by the buffer streams, but they trace a warning if you do not
  // specify.
  const val DEFAULT_STREAM_BUFFER_SIZE = 8192

  // Refresh extended device info every 30 minutes
  private const val REFRESH_TIME_FOR_EXTENDED_DEVICE_INFO_MILLIS = 30 * 60 * 1000
  private const val NO_CARRIER = "NoCarrier"
  private var numCPUCores = 0
  private var timestampOfLastCheck: Long = -1
  private var totalExternalStorageGB: Long = -1
  private var availableExternalStorageGB: Long = -1
  private var deviceTimezoneAbbreviation = ""
  private var deviceTimeZoneName = ""
  private var carrierName = NO_CARRIER

  // https://stackoverflow.com/questions/39784415/how-to-detect-programmatically-if-android-app-is-running-in-chrome-book-or-in
  private const val ARC_DEVICE_PATTERN = ".+_cheets|cheets_.+"

  private const val FACEBOOK_PROFILE_FIELDS = "id,name,first_name,middle_name,last_name"
  private const val INSTAGRAM_PROFILE_FIELDS = "id,name,profile_picture"

  @JvmStatic
  fun isNullOrEmpty(c: Collection<*>?): Boolean {
    return c == null || c.isEmpty()
  }

  @JvmStatic
  fun isNullOrEmpty(s: String?): Boolean {
    return s == null || s.isEmpty()
  }

  /**
   * Use this when you want to normalize empty and null strings This way, Utility.areObjectsEqual
   * can used for comparison, where a null string is to be treated the same as an empty string.
   *
   * @param s The string to coerce
   * @param valueIfNullOrEmpty The value if s is null or empty.
   * @return The original string s if it's not null or empty, otherwise the valueIfNullOrEmpty
   */
  @JvmStatic
  fun coerceValueIfNullOrEmpty(s: String?, valueIfNullOrEmpty: String?): String? {
    return if (isNullOrEmpty(s)) {
      valueIfNullOrEmpty
    } else s
  }

  @JvmStatic
  fun md5hash(key: String): String? {
    return hashWithAlgorithm(HASH_ALGORITHM_MD5, key)
  }

  @JvmStatic
  fun sha1hash(key: String): String? {
    return hashWithAlgorithm(HASH_ALGORITHM_SHA1, key)
  }

  @JvmStatic
  fun sha1hash(bytes: ByteArray): String? {
    return hashWithAlgorithm(HASH_ALGORITHM_SHA1, bytes)
  }

  @JvmStatic
  fun sha256hash(key: String?): String? {
    return if (key == null) {
      null
    } else hashWithAlgorithm(HASH_ALGORITHM_SHA256, key)
  }

  @JvmStatic
  fun sha256hash(bytes: ByteArray?): String? {
    return if (bytes == null) {
      null
    } else hashWithAlgorithm(HASH_ALGORITHM_SHA256, bytes)
  }

  private fun hashWithAlgorithm(algorithm: String, key: String): String? {
    return hashWithAlgorithm(algorithm, key.toByteArray())
  }

  private fun hashWithAlgorithm(algorithm: String, bytes: ByteArray): String? {
    val hash =
        try {
          MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
          return null
        }
    return hashBytes(hash, bytes)
  }

  private fun hashBytes(hash: MessageDigest, bytes: ByteArray): String {
    hash.update(bytes)
    val digest = hash.digest()
    val builder = StringBuilder()
    for (b in digest) {
      builder.append(Integer.toHexString(b.toInt() shr 4 and 0xf))
      builder.append(Integer.toHexString(b.toInt() shr 0 and 0xf))
    }
    return builder.toString()
  }

  @JvmStatic
  fun buildUri(authority: String?, path: String?, parameters: Bundle?): Uri {
    val builder = Uri.Builder()
    builder.scheme(URL_SCHEME)
    builder.authority(authority)
    builder.path(path)
    if (parameters != null) {
      for (key in parameters.keySet()) {
        val parameter = parameters[key]
        if (parameter is String) {
          builder.appendQueryParameter(key, parameter as String?)
        }
      }
    }
    return builder.build()
  }

  @JvmStatic
  fun parseUrlQueryString(queryString: String?): Bundle {
    val params = Bundle()
    if (!isNullOrEmpty(queryString)) {
      val array = checkNotNull(queryString).split("&").toTypedArray()
      for (parameter in array) {
        val keyValuePair = parameter.split("=").toTypedArray()
        try {
          if (keyValuePair.size == 2) {
            params.putString(
                URLDecoder.decode(keyValuePair[0], UTF8), URLDecoder.decode(keyValuePair[1], UTF8))
          } else if (keyValuePair.size == 1) {
            params.putString(URLDecoder.decode(keyValuePair[0], UTF8), "")
          }
        } catch (e: UnsupportedEncodingException) {
          // shouldn't happen
          logd(LOG_TAG, e)
        }
      }
    }
    return params
  }

  @JvmStatic
  fun putNonEmptyString(b: Bundle, key: String?, value: String?) {
    if (!isNullOrEmpty(value)) {
      b.putString(key, value)
    }
  }

  @JvmStatic
  fun putCommaSeparatedStringList(b: Bundle, key: String?, list: List<String?>?) {
    if (list != null) {
      val commaSeparated = TextUtils.join(",", list)
      b.putString(key, commaSeparated)
    }
  }

  @JvmStatic
  fun putUri(b: Bundle, key: String?, uri: Uri?) {
    if (uri != null) {
      putNonEmptyString(b, key, uri.toString())
    }
  }

  @JvmStatic
  fun putJSONValueInBundle(bundle: Bundle, key: String?, value: Any?): Boolean {
    if (value == null) {
      bundle.remove(key)
    } else if (value is Boolean) {
      bundle.putBoolean(key, value)
    } else if (value is BooleanArray) {
      bundle.putBooleanArray(key, value as BooleanArray?)
    } else if (value is Double) {
      bundle.putDouble(key, value)
    } else if (value is DoubleArray) {
      bundle.putDoubleArray(key, value as DoubleArray?)
    } else if (value is Int) {
      bundle.putInt(key, value)
    } else if (value is IntArray) {
      bundle.putIntArray(key, value as IntArray?)
    } else if (value is Long) {
      bundle.putLong(key, value)
    } else if (value is LongArray) {
      bundle.putLongArray(key, value as LongArray?)
    } else if (value is String) {
      bundle.putString(key, value as String?)
    } else if (value is JSONArray) {
      bundle.putString(key, value.toString())
    } else if (value is JSONObject) {
      bundle.putString(key, value.toString())
    } else {
      return false
    }
    return true
  }

  @JvmStatic
  fun closeQuietly(closeable: Closeable?) {
    try {
      closeable?.close()
    } catch (ioe: IOException) {
      // ignore
    }
  }

  @JvmStatic
  fun disconnectQuietly(connection: URLConnection?) {
    if (connection != null && connection is HttpURLConnection) {
      connection.disconnect()
    }
  }

  @JvmStatic
  fun getMetadataApplicationId(context: Context?): String {
    Validate.notNull(context, "context")
    return FacebookSdk.getApplicationId()
  }

  @JvmStatic
  fun convertJSONObjectToHashMap(jsonObject: JSONObject): Map<String, Any> {
    val map = HashMap<String, Any>()
    val keys = jsonObject.names() ?: return map
    for (i in 0 until keys.length()) {
      var key: String
      try {
        key = keys.getString(i)
        var value = jsonObject[key]
        if (value is JSONObject) {
          value = convertJSONObjectToHashMap(value)
        }
        map[key] = value
      } catch (e: JSONException) {}
    }
    return map
  }

  @JvmStatic
  fun convertJSONObjectToStringMap(jsonObject: JSONObject): Map<String, String> {
    val map = HashMap<String, String>()
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      val value = jsonObject.optString(key)
      if (value != null) {
        map[key] = value
      }
    }
    return map
  }

  @JvmStatic
  fun convertJSONArrayToList(jsonArray: JSONArray): List<String> {
    return try {
      val result: MutableList<String> = ArrayList()
      for (i in 0 until jsonArray.length()) {
        result.add(jsonArray.getString(i))
      }
      result
    } catch (je: JSONException) {
      ArrayList()
    }
  }

  // Returns either a JSONObject or JSONArray representation of the 'key' property of
  // 'jsonObject'.
  @Throws(JSONException::class)
  @JvmStatic
  fun getStringPropertyAsJSON(
      jsonObject: JSONObject,
      key: String?,
      nonJSONPropertyKey: String?
  ): Any? {
    var jsonObject = jsonObject
    var value = jsonObject.opt(key)
    if (value != null && value is String) {
      val tokener = JSONTokener(value)
      value = tokener.nextValue()
    }
    return if (value != null && !(value is JSONObject || value is JSONArray)) {
      if (nonJSONPropertyKey != null) {
        // Facebook sometimes gives us back a non-JSON value such as
        // literal "true" or "false" as a result.
        // If we got something like that, we present it to the caller as a JSONObject
        // with a single property. We only do this if the caller wants that behavior.
        jsonObject = JSONObject()
        jsonObject.putOpt(nonJSONPropertyKey, value)
        jsonObject
      } else {
        throw FacebookException("Got an unexpected non-JSON object.")
      }
    } else value
  }

  @Throws(IOException::class)
  @JvmStatic
  fun readStreamToString(inputStream: InputStream?): String {
    var bufferedInputStream: BufferedInputStream? = null
    var reader: InputStreamReader? = null
    return try {
      bufferedInputStream = BufferedInputStream(inputStream)
      reader = InputStreamReader(bufferedInputStream)
      val stringBuilder = StringBuilder()
      val bufferSize = 1024 * 2
      val buffer = CharArray(bufferSize)
      var n = 0
      while (reader.read(buffer).also { n = it } != -1) {
        stringBuilder.append(buffer, 0, n)
      }
      stringBuilder.toString()
    } finally {
      closeQuietly(bufferedInputStream)
      closeQuietly(reader)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun copyAndCloseInputStream(inputStream: InputStream?, outputStream: OutputStream): Int {
    var bufferedInputStream: BufferedInputStream? = null
    var totalBytes = 0
    try {
      bufferedInputStream = BufferedInputStream(inputStream)
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
        totalBytes += bytesRead
      }
    } finally {
      bufferedInputStream?.close()
      inputStream?.close()
    }
    return totalBytes
  }

  @JvmStatic
  fun stringsEqualOrEmpty(a: String?, b: String?): Boolean {
    val aEmpty = a.isNullOrEmpty()
    val bEmpty = b.isNullOrEmpty()
    if (aEmpty && bEmpty) {
      // Both null or empty, they match.
      return true
    }
    return if (!aEmpty && !bEmpty) {
      // Both non-empty, check equality.
      a == b
    } else false
    // One empty, one non-empty, can't match.
  }

  private fun clearCookiesForDomain(context: Context, domain: String) {
    // This is to work around a bug where CookieManager may fail to instantiate if
    // CookieSyncManager has never been created.
    val syncManager = CookieSyncManager.createInstance(context)
    syncManager.sync()
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(domain) ?: return
    val splitCookies = cookies.split(";").toTypedArray()
    for (cookie in splitCookies) {
      val cookieParts = cookie.split("=").toTypedArray()
      if (cookieParts.size > 0) {
        val newCookie =
            cookieParts[0].trim { it <= ' ' } + "=;expires=Sat, 1 Jan 2000 00:00:01 UTC;"
        cookieManager.setCookie(domain, newCookie)
      }
    }
    cookieManager.removeExpiredCookie()
  }

  @JvmStatic
  fun clearFacebookCookies(context: Context) {
    // setCookie acts differently when trying to expire cookies between builds of Android that
    // are using Chromium HTTP stack and those that are not. Using both of these domains to
    // ensure it works on both.
    try {
      clearCookiesForDomain(context, "facebook.com")
      clearCookiesForDomain(context, ".facebook.com")
      clearCookiesForDomain(context, "https://facebook.com")
      clearCookiesForDomain(context, "https://.facebook.com")
    } catch (e: Exception) {
      // An exception is thrown during cookie clearance. It's likely that webview is not available.
    }
  }

  @JvmStatic
  fun logd(tag: String?, e: Exception?) {
    if (FacebookSdk.isDebugEnabled() && tag != null && e != null) {
      Log.d(tag, e.javaClass.simpleName + ": " + e.message)
    }
  }

  @JvmStatic
  fun logd(tag: String?, msg: String?) {
    if (FacebookSdk.isDebugEnabled() && tag != null && msg != null) {
      Log.d(tag, msg)
    }
  }

  @JvmStatic
  fun logd(tag: String?, msg: String?, t: Throwable?) {
    if (FacebookSdk.isDebugEnabled() && !isNullOrEmpty(tag)) {
      Log.d(tag, msg, t)
    }
  }

  @JvmStatic
  fun <T> areObjectsEqual(a: T?, b: T?): Boolean {
    return if (a == null) {
      b == null
    } else a == b
  }

  @JvmStatic
  fun safeGetStringFromResponse(response: JSONObject?, propertyName: String?): String {
    return if (response != null) response.optString(propertyName, "") else ""
  }

  @JvmStatic
  fun tryGetJSONObjectFromResponse(response: JSONObject?, propertyKey: String?): JSONObject? {
    return response?.optJSONObject(propertyKey)
  }

  @JvmStatic
  fun tryGetJSONArrayFromResponse(response: JSONObject?, propertyKey: String?): JSONArray? {
    return response?.optJSONArray(propertyKey)
  }

  @Throws(JSONException::class)
  @JvmStatic
  fun jsonArrayToStringList(jsonArray: JSONArray): List<String> {
    val result = ArrayList<String>()
    for (i in 0 until jsonArray.length()) {
      result.add(jsonArray.getString(i))
    }
    return result
  }

  @Throws(JSONException::class)
  @JvmStatic
  fun jsonArrayToSet(jsonArray: JSONArray): Set<String> {
    val result: MutableSet<String> = HashSet()
    for (i in 0 until jsonArray.length()) {
      result.add(jsonArray.getString(i))
    }
    return result
  }

  @JvmStatic
  fun mapToJsonStr(map: Map<String?, String?>): String {
    return if (map.isEmpty()) {
      ""
    } else {
      try {
        val jsonObject = JSONObject()
        for ((key, value) in map) {
          jsonObject.put(key, value)
        }
        jsonObject.toString()
      } catch (_e: JSONException) {
        ""
      }
    }
  }

  @JvmStatic
  fun jsonStrToMap(str: String): Map<String, String> {
    return if (str.isEmpty()) {
      HashMap()
    } else {
      try {
        val map: MutableMap<String, String> = HashMap()
        val jsonObject = JSONObject(str)
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          map[key] = jsonObject.getString(key)
        }
        map
      } catch (_e: JSONException) {
        HashMap()
      }
    }
  }

  @Throws(JSONException::class)
  @JvmStatic
  fun setAppEventAttributionParameters(
      params: JSONObject,
      attributionIdentifiers: AttributionIdentifiers?,
      anonymousAppDeviceGUID: String?,
      limitEventUsage: Boolean,
      context: Context
  ) {
    if (!FeatureManager.isEnabled(FeatureManager.Feature.ServiceUpdateCompliance)) {
      params.put("anon_id", anonymousAppDeviceGUID)
    }
    params.put("application_tracking_enabled", !limitEventUsage)
    params.put("advertiser_id_collection_enabled", FacebookSdk.getAdvertiserIDCollectionEnabled())
    if (attributionIdentifiers != null) {
      if (FeatureManager.isEnabled(FeatureManager.Feature.ServiceUpdateCompliance)) {
        appendAnonIdUnderCompliance(params, attributionIdentifiers, anonymousAppDeviceGUID, context)
      }
      if (attributionIdentifiers.attributionId != null) {
        if (FeatureManager.isEnabled(FeatureManager.Feature.ServiceUpdateCompliance)) {
          appendAttributionIdUnderCompliance(params, attributionIdentifiers, context)
        } else {
          params.put("attribution", attributionIdentifiers.attributionId)
        }
      }
      if (attributionIdentifiers.androidAdvertiserId != null) {
        params.put("advertiser_id", attributionIdentifiers.androidAdvertiserId)
        params.put("advertiser_tracking_enabled", !attributionIdentifiers.isTrackingLimited)
      }
      if (!attributionIdentifiers.isTrackingLimited) {
        val userData = UserDataStore.getAllHashedUserData()
        if (!userData.isEmpty()) {
          params.put("ud", userData)
        }
      }
      if (attributionIdentifiers.androidInstallerPackage != null) {
        params.put("installer_package", attributionIdentifiers.androidInstallerPackage)
      }
    }
  } /* no op */

  /**
   * Get the app version of the app, as specified by the manifest.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   *
   * @return The version name of this app
   */
  @JvmStatic
  fun getAppVersion(): String? {
    val context = FacebookSdk.getApplicationContext() ?: return null
    val pkgName = context.packageName
    try {
      val pi = context.packageManager.getPackageInfo(pkgName, 0) ?: return null
      return pi.versionName
    } catch (e: PackageManager.NameNotFoundException) {
      /* no op */
    }
    return null
  }

  @Throws(JSONException::class)
  @JvmStatic
  fun setAppEventExtendedDeviceInfoParameters(params: JSONObject, appContext: Context) {
    val extraInfoArray = JSONArray()
    extraInfoArray.put(EXTRA_APP_EVENTS_INFO_FORMAT_VERSION)
    refreshPeriodicExtendedDeviceInfo(appContext)

    // Application Manifest info:
    val pkgName = appContext.packageName
    var versionCode = -1
    var versionName: String? = ""
    try {
      val pi = appContext.packageManager.getPackageInfo(pkgName, 0) ?: return
      versionCode = pi.versionCode
      versionName = pi.versionName
    } catch (e: PackageManager.NameNotFoundException) {
      // Swallow
    }

    // Application Manifest info:
    extraInfoArray.put(pkgName)
    extraInfoArray.put(versionCode)
    extraInfoArray.put(versionName)

    // OS/Device info
    extraInfoArray.put(Build.VERSION.RELEASE)
    extraInfoArray.put(Build.MODEL)

    // Locale
    val locale =
        try {
          appContext.resources.configuration.locale
        } catch (e: Exception) {
          Locale.getDefault()
        }
    extraInfoArray.put(locale.language + "_" + locale.country)

    // Time zone
    extraInfoArray.put(deviceTimezoneAbbreviation)

    // Carrier
    extraInfoArray.put(carrierName)

    // Screen dimensions
    var width = 0
    var height = 0
    var density = 0.0
    try {
      val display =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val displayManager =
                appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
          } else {
            (appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
          }
      if (display != null) {
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        width = displayMetrics.widthPixels
        height = displayMetrics.heightPixels
        density = displayMetrics.density.toDouble()
      }
    } catch (e: Exception) {
      // Swallow
    }
    extraInfoArray.put(width)
    extraInfoArray.put(height)
    val df = DecimalFormat("#.##")
    extraInfoArray.put(df.format(density))

    // CPU Cores
    extraInfoArray.put(refreshBestGuessNumberOfCPUCores())

    // External Storage
    extraInfoArray.put(totalExternalStorageGB)
    extraInfoArray.put(availableExternalStorageGB)
    extraInfoArray.put(deviceTimeZoneName)
    params.put("extinfo", extraInfoArray.toString())
  }

  @JvmStatic
  fun getMethodQuietly(
      clazz: Class<*>,
      methodName: String,
      vararg parameterTypes: Class<*>?
  ): Method? {
    return try {
      clazz.getMethod(methodName, *parameterTypes)
    } catch (ex: NoSuchMethodException) {
      null
    }
  }

  @JvmStatic
  fun getMethodQuietly(
      className: String,
      methodName: String,
      vararg parameterTypes: Class<*>?
  ): Method? {
    return try {
      val clazz = Class.forName(className)
      getMethodQuietly(clazz, methodName, *parameterTypes)
    } catch (ex: ClassNotFoundException) {
      null
    }
  }

  @JvmStatic
  fun invokeMethodQuietly(receiver: Any?, method: Method, vararg args: Any?): Any? {
    return try {
      method.invoke(receiver, *args)
    } catch (ex: IllegalAccessException) {
      null
    } catch (ex: InvocationTargetException) {
      null
    }
  }

  /**
   * Returns the name of the current activity if the context is an activity, otherwise return
   * "unknown"
   */
  @JvmStatic
  fun getActivityName(context: Context?): String {
    return if (context == null) {
      "null"
    } else if (context === context.applicationContext) {
      "unknown"
    } else {
      context.javaClass.simpleName
    }
  }

  @JvmStatic
  fun getUriString(uri: Uri?): String? {
    return uri?.toString()
  }

  @JvmStatic
  fun isWebUri(uri: Uri?): Boolean {
    return uri != null &&
        ("http".equals(uri.scheme, ignoreCase = true) ||
            "https".equals(uri.scheme, ignoreCase = true) ||
            "fbstaging".equals(uri.scheme, ignoreCase = true))
  }

  @JvmStatic
  fun isContentUri(uri: Uri?): Boolean {
    return uri != null && "content".equals(uri.scheme, ignoreCase = true)
  }

  @JvmStatic
  fun isFileUri(uri: Uri?): Boolean {
    return uri != null && "file".equals(uri.scheme, ignoreCase = true)
  }

  @JvmStatic
  fun getContentSize(contentUri: Uri): Long {
    var cursor: Cursor? = null
    return try {
      cursor =
          FacebookSdk.getApplicationContext()
              .contentResolver
              .query(contentUri, null, null, null, null)
      if (cursor === null) {
        return 0L
      }
      val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
      cursor.moveToFirst()
      cursor.getLong(sizeIndex)
    } finally {
      cursor?.close()
    }
  }

  @JvmStatic
  fun getBundleLongAsDate(bundle: Bundle?, key: String?, dateBase: Date): Date? {
    if (bundle == null) {
      return null
    }
    val secondsFromBase: Long
    val secondsObject = bundle[key]
    if (secondsObject is Long) {
      secondsFromBase = secondsObject
    } else if (secondsObject is String) {
      try {
        secondsFromBase = secondsObject.toLong()
      } catch (e: NumberFormatException) {
        return null
      }
    } else {
      return null
    }
    return if (secondsFromBase == 0L) {
      Date(Long.MAX_VALUE)
    } else {
      Date(dateBase.time + secondsFromBase * 1000L)
    }
  }

  @JvmStatic
  fun writeStringMapToParcel(parcel: Parcel, map: Map<String?, String?>?) {
    if (map == null) {
      // 0 is for empty map, -1 to indicate null
      parcel.writeInt(-1)
    } else {
      parcel.writeInt(map.size)
      for ((key, value) in map) {
        parcel.writeString(key)
        parcel.writeString(value)
      }
    }
  }

  @JvmStatic
  fun readStringMapFromParcel(parcel: Parcel): Map<String?, String?>? {
    val size = parcel.readInt()
    if (size < 0) {
      return null
    }
    val map: MutableMap<String?, String?> = HashMap()
    for (i in 0 until size) {
      map[parcel.readString()] = parcel.readString()
    }
    return map
  }

  @JvmStatic
  fun writeNonnullStringMapToParcel(parcel: Parcel, map: Map<String, String>?) {
    if (map == null) {
      // 0 is for empty map, -1 to indicate null
      parcel.writeInt(-1)
    } else {
      parcel.writeInt(map.size)
      for ((key, value) in map) {
        parcel.writeString(key)
        parcel.writeString(value)
      }
    }
  }

  @JvmStatic
  fun readNonnullStringMapFromParcel(parcel: Parcel): Map<String, String>? {
    val size = parcel.readInt()
    if (size < 0) {
      return null
    }
    val map: MutableMap<String, String> = HashMap()
    for (i in 0 until size) {
      val key = parcel.readString()
      val value = parcel.readString()
      if (key != null && value != null) {
        map[key] = value
      }
    }
    return map
  }

  @JvmStatic
  fun isCurrentAccessToken(token: AccessToken?): Boolean {
    return token != null && token == AccessToken.getCurrentAccessToken()
  }

  @JvmStatic
  fun getGraphDomainFromTokenDomain(tokenGraphDomain: String?): String {
    val facebookDomain = FacebookSdk.getFacebookDomain()
    if (tokenGraphDomain == null) {
      return facebookDomain
    }
    return when (tokenGraphDomain) {
      FacebookSdk.GAMING -> facebookDomain.replace(FacebookSdk.FACEBOOK_COM, FacebookSdk.FB_GG)
      FacebookSdk.INSTAGRAM ->
          facebookDomain.replace(FacebookSdk.FACEBOOK_COM, FacebookSdk.INSTAGRAM_COM)
      else -> facebookDomain
    }
  }

  @JvmStatic
  fun getGraphMeRequestWithCacheAsync(
      accessToken: String,
      callback: GraphMeRequestWithCacheCallback
  ) {
    val cachedValue = getProfileInformation(accessToken)
    if (cachedValue != null) {
      callback.onSuccess(cachedValue)
      return
    }
    val graphCallback =
        GraphRequest.Callback { response ->
          if (response.error != null) {
            callback.onFailure(response.error.exception)
          } else {
            putProfileInformation(accessToken, checkNotNull(response.jsonObject))
            callback.onSuccess(response.jsonObject)
          }
        }
    val graphRequest = getGraphMeRequestWithCache(accessToken)
    graphRequest.callback = graphCallback
    graphRequest.executeAsync()
  }

  @JvmStatic
  fun awaitGetGraphMeRequestWithCache(accessToken: String): JSONObject? {
    val cachedValue = getProfileInformation(accessToken)
    if (cachedValue != null) {
      return cachedValue
    }
    val graphRequest = getGraphMeRequestWithCache(accessToken)
    val response = graphRequest.executeAndWait()
    return if (response.error != null) {
      null
    } else response.jsonObject
  }

  private fun getGraphMeRequestWithCache(accessToken: String): GraphRequest {
    val parameters = Bundle()
    parameters.putString(
        GraphRequest.FIELDS_PARAM,
        getProfileFieldsForGraphDomain(getCurrentTokenDomainWithDefault()))
    parameters.putString("access_token", accessToken)
    val request = GraphRequest.newMeRequest(null, null)
    request.parameters = parameters
    request.httpMethod = HttpMethod.GET
    return request
  }

  private fun getProfileFieldsForGraphDomain(graphDomain: String?): String {
    return if (graphDomain == FacebookSdk.INSTAGRAM) {
      INSTAGRAM_PROFILE_FIELDS
    } else FACEBOOK_PROFILE_FIELDS
  }

  private fun getCurrentTokenDomainWithDefault(): String? {
    val accessToken = AccessToken.getCurrentAccessToken()
    return if (accessToken != null && accessToken.graphDomain != null) {
      accessToken.graphDomain
    } else AccessToken.DEFAULT_GRAPH_DOMAIN
  }

  /**
   * Return our best guess at the available number of cores. Will always return at least 1.
   *
   * @return The minimum number of CPU cores
   */
  private fun refreshBestGuessNumberOfCPUCores(): Int {
    // If we have calculated this before, return that value
    if (numCPUCores > 0) {
      return numCPUCores
    }

    // Enumerate all available CPU files and try to count the number of CPU cores.
    try {
      val cpuDir = File("/sys/devices/system/cpu/")
      val cpuFiles = cpuDir.listFiles { dir, fileName -> Pattern.matches("cpu[0-9]+", fileName) }
      if (cpuFiles != null) {
        numCPUCores = cpuFiles.size
      }
    } catch (e: Exception) {}

    // If enumerating and counting the CPU cores fails, use the runtime. Fallback to 1 if
    // that returns bogus values.
    if (numCPUCores <= 0) {
      numCPUCores = Math.max(Runtime.getRuntime().availableProcessors(), 1)
    }
    return numCPUCores
  }

  private fun refreshPeriodicExtendedDeviceInfo(appContext: Context) {
    if (timestampOfLastCheck == -1L ||
        System.currentTimeMillis() - timestampOfLastCheck >=
            REFRESH_TIME_FOR_EXTENDED_DEVICE_INFO_MILLIS) {
      timestampOfLastCheck = System.currentTimeMillis()
      refreshTimezone()
      refreshCarrierName(appContext)
      refreshTotalExternalStorage()
      refreshAvailableExternalStorage()
    }
  }

  private fun refreshTimezone() {
    try {
      val tz = TimeZone.getDefault()
      deviceTimezoneAbbreviation = tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT)
      deviceTimeZoneName = tz.id
    } catch (e: AssertionError) {
      // Workaround for a bug in Android that can cause crashes on Android 8.0 and 8.1
    } catch (e: Exception) {}
  }

  /**
   * Get and cache the carrier name since this won't change during the lifetime of the app.
   *
   * @return The carrier name
   */
  private fun refreshCarrierName(appContext: Context) {
    if (carrierName == NO_CARRIER) {
      try {
        val telephonyManager =
            appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        carrierName = telephonyManager.networkOperatorName
      } catch (e: Exception) {}
    }
  }

  /** @return whether there is external storage: */
  private fun externalStorageExists(): Boolean {
    return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
  }

  // getAvailableBlocks/getBlockSize deprecated but required pre-API v18
  private fun refreshAvailableExternalStorage() {
    try {
      if (externalStorageExists()) {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        availableExternalStorageGB = stat.availableBlocks.toLong() * stat.blockSize.toLong()
      }
      availableExternalStorageGB = convertBytesToGB(availableExternalStorageGB.toDouble())
    } catch (e: Exception) {
      // Swallow
    }
  }

  // getAvailableBlocks/getBlockSize deprecated but required pre-API v18
  private fun refreshTotalExternalStorage() {
    try {
      if (externalStorageExists()) {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        totalExternalStorageGB = stat.blockCount.toLong() * stat.blockSize.toLong()
      }
      totalExternalStorageGB = convertBytesToGB(totalExternalStorageGB.toDouble())
    } catch (e: Exception) {
      // Swallow
    }
  }

  private fun convertBytesToGB(bytes: Double): Long {
    return Math.round(bytes / (1024.0 * 1024.0 * 1024.0))
  }

  private fun appendAnonIdUnderCompliance(
      params: JSONObject,
      attributionIdentifiers: AttributionIdentifiers,
      anonymousAppDeviceGUID: String?,
      context: Context
  ) {
    // TODO: change to Build.VERSION_CODES.S after we start building with API 31
    if (Build.VERSION.SDK_INT >= 31 && isGooglePlayServicesAvailable(context)) {
      if (!attributionIdentifiers.isTrackingLimited) {
        params.put("anon_id", anonymousAppDeviceGUID)
      }
    } else {
      params.put("anon_id", anonymousAppDeviceGUID)
    }
  }

  private fun appendAttributionIdUnderCompliance(
      params: JSONObject,
      attributionIdentifiers: AttributionIdentifiers,
      context: Context
  ) {
    // TODO: change to Build.VERSION_CODES.S after we start building with API 31
    if (Build.VERSION.SDK_INT >= 31 && isGooglePlayServicesAvailable(context)) {
      if (!attributionIdentifiers.isTrackingLimited) {
        params.put("attribution", attributionIdentifiers.attributionId)
      }
    } else {
      params.put("attribution", attributionIdentifiers.attributionId)
    }
  }

  private fun isGooglePlayServicesAvailable(context: Context): Boolean {
    val method =
        getMethodQuietly(
            "com.google.android.gms.common.GooglePlayServicesUtil",
            "isGooglePlayServicesAvailable",
            Context::class.java)
            ?: return false
    val connectionResult = invokeMethodQuietly(null, method, context)
    return !(connectionResult !is Int || connectionResult != 0)
  }

  @JvmStatic
  fun generateRandomString(length: Int): String {
    val r = Random()
    return BigInteger(length * 5, r).toString(32)
  }

  /*
   * There is a bug on Android O that excludes the dialog's view hierarchy from the
   * ViewStructure used by Autofill because the window token is lost when the dialog
   * is resized, hence the token needs to be saved dialog is attached to a window and restored
   * when the dialog attributes change after it is resized.
   */
  @JvmStatic
  fun mustFixWindowParamsForAutofill(context: Context): Boolean {
    // TODO: once this bug is fixed on Android P, checks for version here as well
    return isAutofillAvailable(context)
  }

  @JvmStatic
  fun isAutofillAvailable(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      // Autofill Framework is only available on Android O and higher
      return false
    }
    val afm = context.getSystemService(AutofillManager::class.java)
    // Returns whether autofill is supported by device or and enabled for current user.
    return afm != null && afm.isAutofillSupported && afm.isEnabled
  }

  /**
   * Determines whether the application is running on Chrome OS or not
   *
   * @param context the [Context]
   * @return true if the application is running on Chrome OS; false otherwise.
   */
  @JvmStatic
  fun isChromeOS(context: Context): Boolean {
    // TODO: (T29986208) android.os.Build.VERSION_CODES.O_MR1 and PackageManager.FEATURE_PC
    val isChromeOS: Boolean
    isChromeOS =
        if (Build.VERSION.SDK_INT >= 27) {
          context.packageManager.hasSystemFeature("android.hardware.type.pc")
        } else {
          Build.DEVICE != null && Build.DEVICE.matches(Regex(ARC_DEVICE_PATTERN))
        }
    return isChromeOS
  }

  val resourceLocale: Locale?
    @JvmStatic
    get() =
        try {
          FacebookSdk.getApplicationContext().resources.configuration.locale
        } catch (e: Exception) {
          null
        }
  val currentLocale: Locale
    @JvmStatic
    get() {
      val locale = resourceLocale
      return locale ?: Locale.getDefault()
    }

  @JvmStatic
  fun runOnNonUiThread(runnable: Runnable?) {
    try {
      FacebookSdk.getExecutor().execute(runnable)
    } catch (e: Exception) {
      /*no op*/
    }
  }

  @JvmStatic
  fun getAppName(context: Context): String {
    return try {
      val applicationName = FacebookSdk.getApplicationName()
      if (applicationName != null) {
        return applicationName
      }
      val applicationInfo = context.applicationInfo
      val stringId = applicationInfo.labelRes
      if (stringId == 0) applicationInfo.nonLocalizedLabel.toString()
      else context.getString(stringId)
    } catch (e: Exception) {
      ""
    }
  }

  /* no op */
  @JvmStatic
  val isAutoAppLinkSetup: Boolean
    get() {
      try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(String.format("fb%s://applinks", FacebookSdk.getApplicationId()))
        val ctx = FacebookSdk.getApplicationContext()
        val packageManager = ctx.packageManager
        val packageName = ctx.packageName
        val activities =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (info in activities) {
          if (packageName == info.activityInfo.packageName) {
            return true
          }
        }
      } catch (e: Exception) {
        /* no op */
      }
      return false
    }

  val dataProcessingOptions: JSONObject?
    @JvmStatic
    @AutoHandleExceptions
    get() {
      val context = FacebookSdk.getApplicationContext()
      val data =
          context
              .getSharedPreferences(
                  FacebookSdk.DATA_PROCESSING_OPTIONS_PREFERENCES, Context.MODE_PRIVATE)
              .getString(FacebookSdk.DATA_PROCESSION_OPTIONS, null)
      if (data != null) {
        try {
          return JSONObject(data)
        } catch (e: JSONException) {}
      }
      return null
    }

  val isDataProcessingRestricted: Boolean
    @JvmStatic
    @AutoHandleExceptions
    get() {
      val dataProcessingOptions = dataProcessingOptions ?: return false
      try {
        val options = dataProcessingOptions.getJSONArray("data_processing_options")
        for (i in 0 until options.length()) {
          val option = options.getString(i).toLowerCase()
          if (option == "ldu") {
            return true
          }
        }
      } catch (e: Exception) {}
      return false
    }

  interface GraphMeRequestWithCacheCallback {
    fun onSuccess(userInfo: JSONObject?)
    fun onFailure(error: FacebookException?)
  }
}
