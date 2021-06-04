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
package com.facebook.appevents

import androidx.annotation.RestrictTo
import com.facebook.AccessToken
import com.facebook.FacebookSdk
import com.facebook.internal.Utility.areObjectsEqual
import com.facebook.internal.Utility.isNullOrEmpty
import java.io.ObjectStreamException
import java.io.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AccessTokenAppIdPair(accessTokenString: String?, val applicationId: String?) : Serializable {
  val accessTokenString: String? = if (isNullOrEmpty(accessTokenString)) null else accessTokenString

  constructor(accessToken: AccessToken) : this(accessToken.token, FacebookSdk.getApplicationId())

  override fun hashCode(): Int {
    return (accessTokenString?.hashCode() ?: 0) xor (applicationId?.hashCode() ?: 0)
  }

  override fun equals(o: Any?): Boolean {
    if (o !is AccessTokenAppIdPair) {
      return false
    }
    return areObjectsEqual(o.accessTokenString, accessTokenString) &&
        areObjectsEqual(o.applicationId, applicationId)
  }

  internal class SerializationProxyV1
  constructor(private val accessTokenString: String?, private val appId: String?) : Serializable {
    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
      return AccessTokenAppIdPair(accessTokenString, appId)
    }

    companion object {
      private const val serialVersionUID = -2488473066578201069L
    }
  }
  @Throws(ObjectStreamException::class)
  private fun writeReplace(): Any {
    return SerializationProxyV1(accessTokenString, applicationId)
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}
