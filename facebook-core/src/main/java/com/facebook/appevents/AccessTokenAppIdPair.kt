/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
class AccessTokenAppIdPair(accessTokenString: String?, val applicationId: String) : Serializable {
  val accessTokenString: String? = if (isNullOrEmpty(accessTokenString)) null else accessTokenString

  constructor(accessToken: AccessToken) : this(accessToken.token, FacebookSdk.getApplicationId())

  override fun hashCode(): Int {
    return (accessTokenString?.hashCode() ?: 0) xor (applicationId.hashCode() ?: 0)
  }

  override fun equals(o: Any?): Boolean {
    if (o !is AccessTokenAppIdPair) {
      return false
    }
    return areObjectsEqual(o.accessTokenString, accessTokenString) &&
        areObjectsEqual(o.applicationId, applicationId)
  }

  internal class SerializationProxyV1
  constructor(private val accessTokenString: String?, private val appId: String) : Serializable {
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
