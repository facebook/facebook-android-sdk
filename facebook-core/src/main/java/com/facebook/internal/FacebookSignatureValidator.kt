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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object FacebookSignatureValidator {

  private const val FBI_HASH = "a4b7452e2ed8f5f191058ca7bbfd26b0d3214bfc"
  private const val FBL_HASH = "5e8f16062ea3cd2c4a0d547876baa6f38cabf625"
  private const val FBL2_HASH = "df6b721c8b4d3b6eb44c861d4415007e5a35fc95"
  private const val FBR_HASH = "8a3c4b262d721acd49a4bf97d5213199c86fa2b9"
  private const val FBR2_HASH = "cc2751449a350f668590264ed76692694a80308a"
  private const val MSR_HASH = "9b8f518b086098de3d77736f9458a3d2f6f95a37"
  private const val FBF_HASH = "2438bce1ddb7bd026d5ff89f598b3b5e5bb824b3"

  private val validAppSignatureHashes =
      hashSetOf(FBR_HASH, FBR2_HASH, FBI_HASH, FBL_HASH, FBL2_HASH, MSR_HASH, FBF_HASH)

  @JvmStatic
  fun validateSignature(context: Context, packageName: String): Boolean {
    val brand = Build.BRAND
    val applicationFlags = context.applicationInfo.flags
    if (brand.startsWith("generic") && applicationFlags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
      // We are debugging on an emulator, don't validate package signature.
      return true
    }

    try {
      val packageInfo =
          context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)

      // just in case
      if (packageInfo.signatures == null || packageInfo.signatures.isEmpty()) {
        return false
      }

      return packageInfo.signatures.all {
        validAppSignatureHashes.contains(Utility.sha1hash(it.toByteArray()))
      }
    } catch (e: PackageManager.NameNotFoundException) {
      return false
    }
  }
}
