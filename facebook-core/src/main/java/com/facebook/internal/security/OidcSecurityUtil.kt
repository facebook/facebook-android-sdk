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
package com.facebook.internal.security

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import org.json.JSONObject

object OidcSecurityUtil {
  val OPENID_KEYS_URL_STRING = "https://www.facebook.com/.well-known/oauth/openid/keys/"
  const val SIGNATURE_ALGORITHM_SHA256 = "SHA256withRSA"

  @JvmStatic
  fun getRawKeyFromEndPoint(kid: String): String? {
    val openIdKeyUrl = URL(OPENID_KEYS_URL_STRING)
    val connection = openIdKeyUrl.openConnection() as HttpURLConnection
    return try {
      val data = connection.inputStream.bufferedReader().readText()
      JSONObject(data).optString(kid)
    } catch (_ex: Exception) {
      null
    } finally {
      connection.disconnect()
    }
  }

  /**
   * get the PublicKey object from public key string
   *
   * @param key the public key in string format, could begin with "-----BEGIN PUBLIC KEY-----"
   * @return PublicKey object
   */
  @JvmStatic
  fun getPublicKeyFromString(key: String): PublicKey {
    var pubKeyString = key.replace("\n", "")
    pubKeyString = pubKeyString.replace("-----BEGIN PUBLIC KEY-----", "")
    pubKeyString = pubKeyString.replace("-----END PUBLIC KEY-----", "")

    val byteKey: ByteArray = Base64.decode(pubKeyString, Base64.DEFAULT)
    val x509publicKey = X509EncodedKeySpec(byteKey)
    val kf = KeyFactory.getInstance("RSA")
    return kf.generatePublic(x509publicKey)
  }

  /**
   * Verifies that the signature from the server matches the computed signature on the data. Returns
   * true if the data is correctly signed.
   *
   * @param publicKey public key associated with the developer account
   * @param data encoded data string need to be verify against
   * @param signature encoded signature from Authentication Token
   * @return true successfully verified
   */
  @JvmStatic
  fun verify(publicKey: PublicKey, data: String, signature: String): Boolean {
    return try {
      val sig = Signature.getInstance(SIGNATURE_ALGORITHM_SHA256)
      sig.initVerify(publicKey)
      sig.update(data.toByteArray())
      val decodedSignature: ByteArray = Base64.decode(signature, Base64.URL_SAFE)
      sig.verify(decodedSignature)
    } catch (_ex: Exception) {
      // return not valid if any exception occurs
      return false
    }
  }
}
