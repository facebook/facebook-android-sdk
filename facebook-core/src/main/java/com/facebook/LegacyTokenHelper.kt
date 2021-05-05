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
package com.facebook

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.facebook.internal.Logger.Companion.log
import java.util.Date
import kotlin.collections.ArrayList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class LegacyTokenHelper @JvmOverloads constructor(context: Context, cacheKey: String? = null) {
  private val cacheKey: String
  private val cache: SharedPreferences

  init {
    var context = context
    this.cacheKey = if (cacheKey == null || cacheKey.isEmpty()) DEFAULT_CACHE_KEY else cacheKey

    // If the application context is available, use that. However, if it isn't
    // available (possibly because of a context that was created manually), use
    // the passed in context directly.
    val applicationContext = context.applicationContext
    context = applicationContext ?: context
    cache = context.getSharedPreferences(this.cacheKey, Context.MODE_PRIVATE)
  }

  fun load(): Bundle? {
    val settings = Bundle()
    val allCachedEntries = cache.all
    for (key in allCachedEntries.keys) {
      try {
        deserializeKey(key, settings)
      } catch (e: JSONException) {
        // Error in the cache. So consider it corrupted and return null
        log(
            LoggingBehavior.CACHE,
            Log.WARN,
            TAG,
            "Error reading cached value for key: '$key' -- $e")
        return null
      }
    }
    return settings
  }

  fun save(bundle: Bundle) {
    val editor = cache.edit()
    for (key in bundle.keySet()) {
      try {
        serializeKey(key, bundle, editor)
      } catch (e: JSONException) {
        // Error in the bundle. Don't store a partial cache.
        log(LoggingBehavior.CACHE, Log.WARN, TAG, "Error processing value for key: '$key' -- $e")

        // Bypass the commit and just return. This cancels the entire edit transaction
        return
      }
    }
    editor.apply()
  }

  /** Clears out all token information stored in this cache. */
  fun clear() {
    cache.edit().clear().apply()
  }

  @Throws(JSONException::class)
  private fun serializeKey(key: String, bundle: Bundle, editor: SharedPreferences.Editor) {
    val value =
        bundle[key] ?: // Cannot serialize null values.
        return
    var supportedType: String? = null
    var jsonArray: JSONArray? = null
    val json = JSONObject()
    when (value) {
      is Byte -> {
        supportedType = TYPE_BYTE
        json.put(JSON_VALUE, value.toInt())
      }
      is Short -> {
        supportedType = TYPE_SHORT
        json.put(JSON_VALUE, value.toInt())
      }
      is Int -> {
        supportedType = TYPE_INTEGER
        json.put(JSON_VALUE, value.toInt())
      }
      is Long -> {
        supportedType = TYPE_LONG
        json.put(JSON_VALUE, value.toLong())
      }
      is Float -> {
        supportedType = TYPE_FLOAT
        json.put(JSON_VALUE, value.toDouble())
      }
      is Double -> {
        supportedType = TYPE_DOUBLE
        json.put(JSON_VALUE, value.toDouble())
      }
      is Boolean -> {
        supportedType = TYPE_BOOLEAN
        json.put(JSON_VALUE, value as Boolean)
      }
      is Char -> {
        supportedType = TYPE_CHAR
        json.put(JSON_VALUE, value.toString())
      }
      is String -> {
        supportedType = TYPE_STRING
        json.put(JSON_VALUE, value)
      }
      is Enum<*> -> {
        supportedType = TYPE_ENUM
        json.put(JSON_VALUE, value.toString())
        json.put(JSON_VALUE_ENUM_TYPE, value.javaClass.name)
      }
      else -> {
        // Optimistically create a JSONArray. If not an array type, we can null
        // it out later
        jsonArray = JSONArray()
        when (value) {
          is ByteArray -> {
            supportedType = TYPE_BYTE_ARRAY
            for (v in value) {
              jsonArray.put(v.toInt())
            }
          }
          is ShortArray -> {
            supportedType = TYPE_SHORT_ARRAY
            for (v in value) {
              jsonArray.put(v.toInt())
            }
          }
          is IntArray -> {
            supportedType = TYPE_INTEGER_ARRAY
            for (v in value) {
              jsonArray.put(v)
            }
          }
          is LongArray -> {
            supportedType = TYPE_LONG_ARRAY
            for (v in value) {
              jsonArray.put(v)
            }
          }
          is FloatArray -> {
            supportedType = TYPE_FLOAT_ARRAY
            for (v in value) {
              jsonArray.put(v.toDouble())
            }
          }
          is DoubleArray -> {
            supportedType = TYPE_DOUBLE_ARRAY
            for (v in value) {
              jsonArray.put(v)
            }
          }
          is BooleanArray -> {
            supportedType = TYPE_BOOLEAN_ARRAY
            for (v in value) {
              jsonArray.put(v)
            }
          }
          is CharArray -> {
            supportedType = TYPE_CHAR_ARRAY
            for (v in value) {
              jsonArray.put(v.toString())
            }
          }
          is List<*> -> {
            supportedType = TYPE_STRING_LIST
            val stringList = value as List<String>
            for (v in stringList) {
              jsonArray.put(v ?: JSONObject.NULL)
            }
          }
          else -> {
            // Unsupported type. Clear out the array as a precaution even though
            // it is redundant with the null supportedType.
            jsonArray = null
          }
        }
      }
    }
    if (supportedType != null) {
      json.put(JSON_VALUE_TYPE, supportedType)
      if (jsonArray != null) {
        // If we have an array, it has already been converted to JSON. So use
        // that instead.
        json.putOpt(JSON_VALUE, jsonArray)
      }
      val jsonString = json.toString()
      editor.putString(key, jsonString)
    }
  }

  @Throws(JSONException::class)
  private fun deserializeKey(key: String, bundle: Bundle) {
    val jsonString = checkNotNull(cache.getString(key, "{}"))
    val json = JSONObject(jsonString)
    val valueType = json.getString(JSON_VALUE_TYPE)
    if (valueType == TYPE_BOOLEAN) {
      bundle.putBoolean(key, json.getBoolean(JSON_VALUE))
    } else if (valueType == TYPE_BOOLEAN_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = BooleanArray(jsonArray.length())
      for (i in array.indices) {
        array[i] = jsonArray.getBoolean(i)
      }
      bundle.putBooleanArray(key, array)
    } else if (valueType == TYPE_BYTE) {
      bundle.putByte(key, json.getInt(JSON_VALUE).toByte())
    } else if (valueType == TYPE_BYTE_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = ByteArray(jsonArray.length())
      for (i in array.indices) {
        array[i] = jsonArray.getInt(i).toByte()
      }
      bundle.putByteArray(key, array)
    } else if (valueType == TYPE_SHORT) {
      bundle.putShort(key, json.getInt(JSON_VALUE).toShort())
    } else if (valueType == TYPE_SHORT_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = ShortArray(jsonArray.length())
      for (i in array.indices) {
        array[i] = jsonArray.getInt(i).toShort()
      }
      bundle.putShortArray(key, array)
    } else if (valueType == TYPE_INTEGER) {
      bundle.putInt(key, json.getInt(JSON_VALUE))
    } else if (valueType == TYPE_INTEGER_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = IntArray(jsonArray.length())
      for (i in array.indices) {
        array[i] = jsonArray.getInt(i)
      }
      bundle.putIntArray(key, array)
    } else if (valueType == TYPE_LONG) {
      bundle.putLong(key, json.getLong(JSON_VALUE))
    } else if (valueType == TYPE_LONG_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = LongArray(jsonArray.length())
      for (i in array.indices) {
        array[i] = jsonArray.getLong(i)
      }
      bundle.putLongArray(key, array)
    } else if (valueType == TYPE_FLOAT) {
      bundle.putFloat(key, json.getDouble(JSON_VALUE).toFloat())
    } else if (valueType == TYPE_FLOAT_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = FloatArray(jsonArray.length())
      for (i in array.indices) {
        array[i] = jsonArray.getDouble(i).toFloat()
      }
      bundle.putFloatArray(key, array)
    } else if (valueType == TYPE_DOUBLE) {
      bundle.putDouble(key, json.getDouble(JSON_VALUE))
    } else if (valueType == TYPE_DOUBLE_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = DoubleArray(jsonArray.length())
      for (i in array.indices) {
        array[i] = jsonArray.getDouble(i)
      }
      bundle.putDoubleArray(key, array)
    } else if (valueType == TYPE_CHAR) {
      val charString = json.getString(JSON_VALUE)
      if (charString != null && charString.length == 1) {
        bundle.putChar(key, charString[0])
      }
    } else if (valueType == TYPE_CHAR_ARRAY) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val array = CharArray(jsonArray.length())
      for (i in array.indices) {
        val charString = jsonArray.getString(i)
        if (charString != null && charString.length == 1) {
          array[i] = charString[0]
        }
      }
      bundle.putCharArray(key, array)
    } else if (valueType == TYPE_STRING) {
      bundle.putString(key, json.getString(JSON_VALUE))
    } else if (valueType == TYPE_STRING_LIST) {
      val jsonArray = json.getJSONArray(JSON_VALUE)
      val numStrings = jsonArray.length()
      val stringList = ArrayList<String?>(numStrings)
      for (i in 0 until numStrings) {
        val jsonStringValue = jsonArray[i]
        stringList.add(
            i, if (jsonStringValue === JSONObject.NULL) null else jsonStringValue as String)
      }
      bundle.putStringArrayList(key, stringList)
    } else if (valueType == TYPE_ENUM) {
      try {
        val enumType = json.getString(JSON_VALUE_ENUM_TYPE)
        val enumClass = Class.forName(enumType) as Class<out Enum<*>>
        val enumValue = java.lang.Enum.valueOf(enumClass, json.getString(JSON_VALUE))
        bundle.putSerializable(key, enumValue)
      } catch (e: ClassNotFoundException) {} catch (e: IllegalArgumentException) {}
    }
  }

  companion object {
    /**
     * The key used by AccessTokenCache to store the token value in the Bundle during load and save.
     */
    const val TOKEN_KEY = "com.facebook.TokenCachingStrategy.Token"

    /**
     * The key used by AccessTokenCache to store the expiration date value in the Bundle during load
     * and save.
     */
    const val EXPIRATION_DATE_KEY = "com.facebook.TokenCachingStrategy.ExpirationDate"

    /**
     * The key used by AccessTokenCache to store the last refresh date value in the Bundle during
     * load and save.
     */
    const val LAST_REFRESH_DATE_KEY = "com.facebook.TokenCachingStrategy.LastRefreshDate"

    /**
     * The key used by AccessTokenCache to store an enum indicating the source of the token in the
     * Bundle during load and save.
     */
    const val TOKEN_SOURCE_KEY = "com.facebook.TokenCachingStrategy.AccessTokenSource"

    /**
     * The key used by AccessTokenCache to store the list of permissions granted by the token in the
     * Bundle during load and save.
     */
    const val PERMISSIONS_KEY = "com.facebook.TokenCachingStrategy.Permissions"

    /**
     * The key used by AccessTokenCache to store the list of permissions declined by the user in the
     * token in the Bundle during load and save.
     */
    const val DECLINED_PERMISSIONS_KEY = "com.facebook.TokenCachingStrategy.DeclinedPermissions"
    const val EXPIRED_PERMISSIONS_KEY = "com.facebook.TokenCachingStrategy.ExpiredPermissions"
    const val APPLICATION_ID_KEY = "com.facebook.TokenCachingStrategy.ApplicationId"
    private const val INVALID_BUNDLE_MILLISECONDS = Long.MIN_VALUE
    private const val IS_SSO_KEY = "com.facebook.TokenCachingStrategy.IsSSO"
    const val DEFAULT_CACHE_KEY = "com.facebook.SharedPreferencesTokenCachingStrategy.DEFAULT_KEY"
    private val TAG = LegacyTokenHelper::class.java.simpleName
    private const val JSON_VALUE_TYPE = "valueType"
    private const val JSON_VALUE = "value"
    private const val JSON_VALUE_ENUM_TYPE = "enumType"
    private const val TYPE_BOOLEAN = "bool"
    private const val TYPE_BOOLEAN_ARRAY = "bool[]"
    private const val TYPE_BYTE = "byte"
    private const val TYPE_BYTE_ARRAY = "byte[]"
    private const val TYPE_SHORT = "short"
    private const val TYPE_SHORT_ARRAY = "short[]"
    private const val TYPE_INTEGER = "int"
    private const val TYPE_INTEGER_ARRAY = "int[]"
    private const val TYPE_LONG = "long"
    private const val TYPE_LONG_ARRAY = "long[]"
    private const val TYPE_FLOAT = "float"
    private const val TYPE_FLOAT_ARRAY = "float[]"
    private const val TYPE_DOUBLE = "double"
    private const val TYPE_DOUBLE_ARRAY = "double[]"
    private const val TYPE_CHAR = "char"
    private const val TYPE_CHAR_ARRAY = "char[]"
    private const val TYPE_STRING = "string"
    private const val TYPE_STRING_LIST = "stringList"
    private const val TYPE_ENUM = "enum"

    @JvmStatic
    fun hasTokenInformation(bundle: Bundle?): Boolean {
      if (bundle == null) {
        return false
      }
      val token = bundle.getString(TOKEN_KEY)
      if (token == null || token.isEmpty()) {
        return false
      }
      val expiresMilliseconds = bundle.getLong(EXPIRATION_DATE_KEY, 0L)
      return expiresMilliseconds != 0L
    }

    @JvmStatic
    fun getToken(bundle: Bundle): String? {
      return bundle.getString(TOKEN_KEY)
    }

    @JvmStatic
    fun putToken(bundle: Bundle, value: String) {
      bundle.putString(TOKEN_KEY, value)
    }

    @JvmStatic
    fun getExpirationDate(bundle: Bundle): Date? {
      return getDate(bundle, EXPIRATION_DATE_KEY)
    }

    @JvmStatic
    fun putExpirationDate(bundle: Bundle, value: Date) {
      putDate(bundle, EXPIRATION_DATE_KEY, value)
    }

    @JvmStatic
    fun getExpirationMilliseconds(bundle: Bundle): Long {
      return bundle.getLong(EXPIRATION_DATE_KEY)
    }

    @JvmStatic
    fun putExpirationMilliseconds(bundle: Bundle, value: Long) {
      bundle.putLong(EXPIRATION_DATE_KEY, value)
    }

    @JvmStatic
    fun getPermissions(bundle: Bundle): Set<String>? {
      val arrayList = bundle.getStringArrayList(PERMISSIONS_KEY) ?: return null
      return HashSet(arrayList)
    }

    @JvmStatic
    fun putPermissions(bundle: Bundle, value: Collection<String>) {
      bundle.putStringArrayList(PERMISSIONS_KEY, ArrayList(value))
    }

    @JvmStatic
    fun putDeclinedPermissions(bundle: Bundle, value: Collection<String>) {
      bundle.putStringArrayList(DECLINED_PERMISSIONS_KEY, ArrayList(value))
    }

    @JvmStatic
    fun putExpiredPermissions(bundle: Bundle, value: Collection<String>) {
      bundle.putStringArrayList(EXPIRED_PERMISSIONS_KEY, ArrayList(value))
    }

    @JvmStatic
    fun getSource(bundle: Bundle): AccessTokenSource? {
      return if (bundle.containsKey(TOKEN_SOURCE_KEY)) {
        bundle.getSerializable(TOKEN_SOURCE_KEY) as AccessTokenSource?
      } else {
        val isSSO = bundle.getBoolean(IS_SSO_KEY)
        if (isSSO) AccessTokenSource.FACEBOOK_APPLICATION_WEB else AccessTokenSource.WEB_VIEW
      }
    }

    @JvmStatic
    fun putSource(bundle: Bundle, value: AccessTokenSource) {
      bundle.putSerializable(TOKEN_SOURCE_KEY, value)
    }

    @JvmStatic
    fun getLastRefreshDate(bundle: Bundle): Date? {
      return getDate(bundle, LAST_REFRESH_DATE_KEY)
    }

    @JvmStatic
    fun putLastRefreshDate(bundle: Bundle, value: Date) {
      putDate(bundle, LAST_REFRESH_DATE_KEY, value)
    }

    @JvmStatic
    fun getLastRefreshMilliseconds(bundle: Bundle): Long {
      return bundle.getLong(LAST_REFRESH_DATE_KEY)
    }

    @JvmStatic
    fun putLastRefreshMilliseconds(bundle: Bundle, value: Long) {
      bundle.putLong(LAST_REFRESH_DATE_KEY, value)
    }

    @JvmStatic
    fun getApplicationId(bundle: Bundle): String? {
      return bundle.getString(APPLICATION_ID_KEY)
    }

    @JvmStatic
    fun putApplicationId(bundle: Bundle, value: String?) {
      bundle.putString(APPLICATION_ID_KEY, value)
    }

    private fun getDate(bundle: Bundle?, key: String?): Date? {
      if (bundle == null) {
        return null
      }
      val n = bundle.getLong(key, INVALID_BUNDLE_MILLISECONDS)
      return if (n == INVALID_BUNDLE_MILLISECONDS) {
        null
      } else Date(n)
    }

    private fun putDate(bundle: Bundle, key: String?, date: Date) {
      bundle.putLong(key, date.time)
    }
  }
}
