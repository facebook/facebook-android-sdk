/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.Bundle
import com.facebook.FacebookTestUtility.assertEqualContentsWithoutOrder
import com.facebook.FacebookTestUtility.assertSameCollectionContents
import com.facebook.FacebookTestUtility.nowPlusSeconds
import com.facebook.internal.Utility
import java.lang.reflect.Array
import java.util.ArrayList
import java.util.Date
import java.util.Random
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RuntimeEnvironment

@PrepareForTest(Utility::class, FacebookSdk::class)
class LegacyTokenCacheTest : FacebookPowerMockTestCase() {
  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext()).thenReturn(RuntimeEnvironment.application)
    MemberModifier.stub<Any>(
            PowerMockito.method(
                Utility::class.java, "awaitGetGraphMeRequestWithCache", String::class.java))
        .toReturn(JSONObject().put("id", "1000"))
  }

  @Test
  fun testAllTypes() {
    val originalBundle = Bundle()
    putBoolean(BOOLEAN_KEY, originalBundle)
    putBooleanArray(BOOLEAN_ARRAY_KEY, originalBundle)
    putByte(BYTE_KEY, originalBundle)
    putByteArray(BYTE_ARRAY_KEY, originalBundle)
    putShort(SHORT_KEY, originalBundle)
    putShortArray(SHORT_ARRAY_KEY, originalBundle)
    putInt(INT_KEY, originalBundle)
    putIntArray(INT_ARRAY_KEY, originalBundle)
    putLong(LONG_KEY, originalBundle)
    putLongArray(LONG_ARRAY_KEY, originalBundle)
    putFloat(FLOAT_KEY, originalBundle)
    putFloatArray(FLOAT_ARRAY_KEY, originalBundle)
    putDouble(DOUBLE_KEY, originalBundle)
    putDoubleArray(DOUBLE_ARRAY_KEY, originalBundle)
    putChar(CHAR_KEY, originalBundle)
    putCharArray(CHAR_ARRAY_KEY, originalBundle)
    putString(STRING_KEY, originalBundle)
    putStringList(STRING_LIST_KEY, originalBundle)
    originalBundle.putSerializable(SERIALIZABLE_KEY, AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    ensureApplicationContext()
    val cache = LegacyTokenHelper(RuntimeEnvironment.application)
    cache.save(originalBundle)
    val cache2 = LegacyTokenHelper(RuntimeEnvironment.application)
    val cachedBundle = checkNotNull(cache2.load())
    Assert.assertEquals(
        originalBundle.getBoolean(BOOLEAN_KEY), cachedBundle.getBoolean(BOOLEAN_KEY))
    assertArrayEquals(
        originalBundle.getBooleanArray(BOOLEAN_ARRAY_KEY),
        cachedBundle.getBooleanArray(BOOLEAN_ARRAY_KEY))
    Assert.assertEquals(
        originalBundle.getByte(BYTE_KEY).toLong(), cachedBundle.getByte(BYTE_KEY).toLong())
    assertArrayEquals(
        originalBundle.getByteArray(BYTE_ARRAY_KEY), cachedBundle.getByteArray(BYTE_ARRAY_KEY))
    Assert.assertEquals(
        originalBundle.getShort(SHORT_KEY).toLong(), cachedBundle.getShort(SHORT_KEY).toLong())
    assertArrayEquals(
        originalBundle.getShortArray(SHORT_ARRAY_KEY), cachedBundle.getShortArray(SHORT_ARRAY_KEY))
    Assert.assertEquals(
        originalBundle.getInt(INT_KEY).toLong(), cachedBundle.getInt(INT_KEY).toLong())
    assertArrayEquals(
        originalBundle.getIntArray(INT_ARRAY_KEY), cachedBundle.getIntArray(INT_ARRAY_KEY))
    Assert.assertEquals(originalBundle.getLong(LONG_KEY), cachedBundle.getLong(LONG_KEY))
    assertArrayEquals(
        originalBundle.getLongArray(LONG_ARRAY_KEY), cachedBundle.getLongArray(LONG_ARRAY_KEY))
    Assert.assertEquals(
        originalBundle.getFloat(FLOAT_KEY).toDouble(),
        cachedBundle.getFloat(FLOAT_KEY).toDouble(),
        FacebookTestUtility.DOUBLE_EQUALS_DELTA)
    assertArrayEquals(
        originalBundle.getFloatArray(FLOAT_ARRAY_KEY), cachedBundle.getFloatArray(FLOAT_ARRAY_KEY))
    Assert.assertEquals(
        originalBundle.getDouble(DOUBLE_KEY),
        cachedBundle.getDouble(DOUBLE_KEY),
        FacebookTestUtility.DOUBLE_EQUALS_DELTA)
    assertArrayEquals(
        originalBundle.getDoubleArray(DOUBLE_ARRAY_KEY),
        cachedBundle.getDoubleArray(DOUBLE_ARRAY_KEY))
    Assert.assertEquals(
        originalBundle.getChar(CHAR_KEY).toLong(), cachedBundle.getChar(CHAR_KEY).toLong())
    assertArrayEquals(
        originalBundle.getCharArray(CHAR_ARRAY_KEY), cachedBundle.getCharArray(CHAR_ARRAY_KEY))
    Assert.assertEquals(originalBundle.getString(STRING_KEY), cachedBundle.getString(STRING_KEY))
    assertListEquals(
        originalBundle.getStringArrayList(STRING_LIST_KEY),
        cachedBundle.getStringArrayList(STRING_LIST_KEY))
    Assert.assertEquals(
        originalBundle.getSerializable(SERIALIZABLE_KEY),
        cachedBundle.getSerializable(SERIALIZABLE_KEY))
  }

  @Test
  fun testMultipleCaches() {
    val bundle1 = Bundle()
    val bundle2 = Bundle()
    bundle1.putInt(INT_KEY, 10)
    bundle1.putString(STRING_KEY, "ABC")
    bundle2.putInt(INT_KEY, 100)
    bundle2.putString(STRING_KEY, "xyz")
    ensureApplicationContext()
    var cache1 = LegacyTokenHelper(RuntimeEnvironment.application)
    var cache2 = LegacyTokenHelper(RuntimeEnvironment.application, "CustomCache")
    cache1.save(bundle1)
    cache2.save(bundle2)

    // Get new references to make sure we are getting persisted data.
    // Reverse the cache references for fun.
    cache1 = LegacyTokenHelper(RuntimeEnvironment.application, "CustomCache")
    cache2 = LegacyTokenHelper(RuntimeEnvironment.application)
    val newBundle1 = checkNotNull(cache1.load())
    val newBundle2 = checkNotNull(cache2.load())
    Assert.assertEquals(bundle2.getInt(INT_KEY).toLong(), newBundle1.getInt(INT_KEY).toLong())
    Assert.assertEquals(bundle2.getString(STRING_KEY), newBundle1.getString(STRING_KEY))
    Assert.assertEquals(bundle1.getInt(INT_KEY).toLong(), newBundle2.getInt(INT_KEY).toLong())
    Assert.assertEquals(bundle1.getString(STRING_KEY), newBundle2.getString(STRING_KEY))
  }

  @Test
  fun testCacheRoundtrip() {
    val permissions: Set<String> = hashSetOf("stream_publish", "go_outside_and_play")
    val token = "AnImaginaryTokenValue"
    val later = nowPlusSeconds(60)
    val earlier = nowPlusSeconds(-60)
    val applicationId = "1234"
    val cache = LegacyTokenHelper(RuntimeEnvironment.application)
    cache.clear()
    var bundle = Bundle()
    LegacyTokenHelper.putToken(bundle, token)
    LegacyTokenHelper.putExpirationDate(bundle, later)
    LegacyTokenHelper.putSource(bundle, AccessTokenSource.FACEBOOK_APPLICATION_NATIVE)
    LegacyTokenHelper.putLastRefreshDate(bundle, earlier)
    LegacyTokenHelper.putPermissions(bundle, permissions)
    LegacyTokenHelper.putDeclinedPermissions(bundle, arrayListOf("whatever"))
    LegacyTokenHelper.putExpiredPermissions(bundle, arrayListOf("anyway"))
    LegacyTokenHelper.putApplicationId(bundle, applicationId)
    cache.save(bundle)
    bundle = checkNotNull(cache.load())
    val accessToken = checkNotNull(AccessToken.createFromLegacyCache(bundle))
    assertSameCollectionContents(permissions, accessToken.permissions)
    Assert.assertEquals(token, accessToken.token)
    Assert.assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, accessToken.source)
    assertThat(accessToken.isExpired).isFalse
    val cachedBundle = AccessTokenTestHelper.toLegacyCacheBundle(accessToken)
    assertEqualContentsWithoutOrder(bundle, cachedBundle)
  }

  private fun ensureApplicationContext() {
    // Since the test case is not running on the UI thread, the applicationContext might
    // not be ready (i.e. it might be null). Wait for a bit to resolve this.
    var waitedFor: Long = 0
    try {
      // Don't hold up execution for too long.
      while (RuntimeEnvironment.application.applicationContext == null && waitedFor <= 2_000) {
        Thread.sleep(50)
        waitedFor += 50
      }
    } catch (e: InterruptedException) {}
  }

  companion object {
    private const val BOOLEAN_KEY = "booleanKey"
    private const val BOOLEAN_ARRAY_KEY = "booleanArrayKey"
    private const val BYTE_KEY = "byteKey"
    private const val BYTE_ARRAY_KEY = "byteArrayKey"
    private const val SHORT_KEY = "shortKey"
    private const val SHORT_ARRAY_KEY = "shortArrayKey"
    private const val INT_KEY = "intKey"
    private const val INT_ARRAY_KEY = "intArrayKey"
    private const val LONG_KEY = "longKey"
    private const val LONG_ARRAY_KEY = "longArrayKey"
    private const val FLOAT_ARRAY_KEY = "floatKey"
    private const val FLOAT_KEY = "floatArrayKey"
    private const val DOUBLE_KEY = "doubleKey"
    private const val DOUBLE_ARRAY_KEY = "doubleArrayKey"
    private const val CHAR_KEY = "charKey"
    private const val CHAR_ARRAY_KEY = "charArrayKey"
    private const val STRING_KEY = "stringKey"
    private const val STRING_LIST_KEY = "stringListKey"
    private const val SERIALIZABLE_KEY = "serializableKey"
    private val random = Random(Date().time)
    private fun assertArrayEquals(a1: Any?, a2: Any?) {
      checkNotNull(a1)
      checkNotNull(a2)
      Assert.assertEquals(a1.javaClass, a2.javaClass)
      assertThat(a1.javaClass.isArray).isTrue
      val length = Array.getLength(a1)
      Assert.assertEquals(length.toLong(), Array.getLength(a2).toLong())
      for (i in 0 until length) {
        val a1Value = Array.get(a1, i)
        val a2Value = Array.get(a2, i)
        Assert.assertEquals(a1Value, a2Value)
      }
    }

    private fun assertListEquals(l1: List<*>?, l2: List<*>?) {
      checkNotNull(l1)
      checkNotNull(l2)
      val i1 = l1.iterator()
      val i2 = l2.iterator()
      while (i1.hasNext() && i2.hasNext()) {
        Assert.assertEquals(i1.next(), i2.next())
      }
      assertThat(i1.hasNext()).isFalse // "Lists not of the same length"
      assertThat(i2.hasNext()).isFalse // "Lists not of the same length"
    }

    private fun putInt(key: String, bundle: Bundle) {
      bundle.putInt(key, random.nextInt())
    }

    private fun putIntArray(key: String, bundle: Bundle) {
      val length = random.nextInt(50)
      val array = IntArray(length)
      for (i in 0 until length) {
        array[i] = random.nextInt()
      }
      bundle.putIntArray(key, array)
    }

    private fun putShort(key: String, bundle: Bundle) {
      bundle.putShort(key, random.nextInt().toShort())
    }

    private fun putShortArray(key: String, bundle: Bundle) {
      val length = random.nextInt(50)
      val array = ShortArray(length)
      for (i in 0 until length) {
        array[i] = random.nextInt().toShort()
      }
      bundle.putShortArray(key, array)
    }

    private fun putByte(key: String, bundle: Bundle) {
      bundle.putByte(key, random.nextInt().toByte())
    }

    private fun putByteArray(key: String, bundle: Bundle) {
      val length = random.nextInt(50)
      val array = ByteArray(length)
      random.nextBytes(array)
      bundle.putByteArray(key, array)
    }

    private fun putBoolean(key: String, bundle: Bundle) {
      bundle.putBoolean(key, random.nextBoolean())
    }

    private fun putBooleanArray(key: String, bundle: Bundle) {
      val length = random.nextInt(50)
      val array = BooleanArray(length)
      for (i in 0 until length) {
        array[i] = random.nextBoolean()
      }
      bundle.putBooleanArray(key, array)
    }

    private fun putLong(key: String, bundle: Bundle) {
      bundle.putLong(key, random.nextLong())
    }

    private fun putLongArray(key: String, bundle: Bundle) {
      val length = random.nextInt(50)
      val array = LongArray(length)
      for (i in 0 until length) {
        array[i] = random.nextLong()
      }
      bundle.putLongArray(key, array)
    }

    private fun putFloat(key: String, bundle: Bundle) {
      bundle.putFloat(key, random.nextFloat())
    }

    private fun putFloatArray(key: String, bundle: Bundle) {
      val length = random.nextInt(50)
      val array = FloatArray(length)
      for (i in 0 until length) {
        array[i] = random.nextFloat()
      }
      bundle.putFloatArray(key, array)
    }

    private fun putDouble(key: String, bundle: Bundle) {
      bundle.putDouble(key, random.nextDouble())
    }

    private fun putDoubleArray(key: String, bundle: Bundle) {
      val length = random.nextInt(50)
      val array = DoubleArray(length)
      for (i in 0 until length) {
        array[i] = random.nextDouble()
      }
      bundle.putDoubleArray(key, array)
    }

    private fun putChar(key: String, bundle: Bundle) {
      bundle.putChar(key, char)
    }

    private fun putCharArray(key: String, bundle: Bundle) {
      bundle.putCharArray(key, charArray)
    }

    private fun putString(key: String, bundle: Bundle) {
      bundle.putString(key, String(charArray))
    }

    private fun putStringList(key: String, bundle: Bundle) {
      var length = random.nextInt(50)
      val stringList = ArrayList<String?>(length)
      while (0 < length--) {
        if (length == 0) {
          stringList.add(null)
        } else {
          stringList.add(String(charArray))
        }
      }
      bundle.putStringArrayList(key, stringList)
    }

    private val charArray: CharArray
      private get() {
        val length = random.nextInt(50)
        val array = CharArray(length)
        for (i in 0 until length) {
          array[i] = char
        }
        return array
      }
    private val char: Char
      private get() = random.nextInt(255).toChar()
  }
}
