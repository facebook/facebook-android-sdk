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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.util.Date
import org.assertj.core.api.Assertions.assertThat

object FacebookTestUtility {
  const val DOUBLE_EQUALS_DELTA = 0.00001

  fun nowPlusSeconds(offset: Long): Date {
    return Date(Date().time + offset * 1000L)
  }

  fun <T> assertSameCollectionContents(expected: Collection<T?>?, actual: Collection<T?>?) {
    if (expected == null || expected.isEmpty()) {
      assertThat(actual).isNullOrEmpty()
    } else {
      checkNotNull(actual)
      assertThat(actual).isNotEmpty
      for (item in expected) {
        assertThat(actual).contains(item)
      }
      for (item in actual) {
        assertThat(expected).contains(item)
      }
    }
  }

  private fun assertEqualContents(a: Bundle, b: Bundle, collectionOrderMatters: Boolean) {
    for (key in a.keySet()) {
      assertThat(b.containsKey(key)).isTrue.withFailMessage("bundle does not include key $key")
      val aValue = a[key]
      val bValue = b[key]
      if (!collectionOrderMatters && aValue is Collection<*> && bValue is Collection<*>) {
        this.assertSameCollectionContents(aValue as Collection<*>?, bValue as Collection<*>?)
      } else {
        assertThat(a[key]).isEqualTo(b[key])
      }
    }
    for (key in b.keySet()) {
      assertThat(a.containsKey(key)).isTrue.withFailMessage("bundle does not include key $key")
    }
  }

  fun assertEqualContentsWithoutOrder(a: Bundle, b: Bundle) {
    assertEqualContents(a, b, false)
  }

  fun assertEqualContents(a: Bundle, b: Bundle) {
    assertEqualContents(a, b, true)
  }

  inline fun <reified E : Parcelable?> parcelAndUnparcel(obj: E): E? {
    val writeParcel = Parcel.obtain()
    val readParcel = Parcel.obtain()
    return try {
      writeParcel.writeParcelable(obj, 0)
      val bytes = writeParcel.marshall()
      readParcel.unmarshall(bytes, 0, bytes.size)
      readParcel.setDataPosition(0)
      val classLoader = E::class.java.classLoader
      readParcel.readParcelable(classLoader)
    } finally {
      writeParcel.recycle()
      readParcel.recycle()
    }
  }
}
