/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
  private const val FBL_HASH = "df6b721c8b4d3b6eb44c861d4415007e5a35fc95"
  private const val FBR_HASH = "8a3c4b262d721acd49a4bf97d5213199c86fa2b9"
  private const val FBR2_HASH = "cc2751449a350f668590264ed76692694a80308a"
  private const val MSR_HASH = "9b8f518b086098de3d77736f9458a3d2f6f95a37"
  private const val FBF_HASH = "2438bce1ddb7bd026d5ff89f598b3b5e5bb824b3"
  private const val IGR_HASH = "c56fb7d591ba6704df047fd98f535372fea00211"

  private val validAppSignatureHashes =
      hashSetOf(FBR_HASH, FBR2_HASH, FBI_HASH, FBL_HASH, MSR_HASH, FBF_HASH, IGR_HASH)

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
