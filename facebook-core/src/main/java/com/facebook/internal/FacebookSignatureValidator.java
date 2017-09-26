/**
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

package com.facebook.internal;

import java.util.HashSet;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class FacebookSignatureValidator {

  private static final String FBI_HASH = "a4b7452e2ed8f5f191058ca7bbfd26b0d3214bfc";
  private static final String FBL_HASH = "5e8f16062ea3cd2c4a0d547876baa6f38cabf625";
  private static final String FBL2_HASH = "df6b721c8b4d3b6eb44c861d4415007e5a35fc95";
  private static final String FBR_HASH = "8a3c4b262d721acd49a4bf97d5213199c86fa2b9";
  private static final String FBR2_HASH = "cc2751449a350f668590264ed76692694a80308a";
  private static final String MSR_HASH = "9b8f518b086098de3d77736f9458a3d2f6f95a37";

  private static final HashSet<String> validAppSignatureHashes = buildAppSignatureHashes();

  private static HashSet<String> buildAppSignatureHashes() {
    HashSet<String> set = new HashSet<String>();
    set.add(FBR_HASH);
    set.add(FBR2_HASH);
    set.add(FBI_HASH);
    set.add(FBL_HASH);
    set.add(FBL2_HASH);
    set.add(MSR_HASH);
    return set;
  }

  public static boolean validateSignature(Context context, String packageName) {
    String brand = Build.BRAND;
    int applicationFlags = context.getApplicationInfo().flags;
    if (brand.startsWith("generic") &&
        (applicationFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      // We are debugging on an emulator, don't validate package signature.
      return true;
    }

    PackageInfo packageInfo;
    try {
      packageInfo = context.getPackageManager().getPackageInfo(
          packageName,
          PackageManager.GET_SIGNATURES);
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }

    // just in case
    if (packageInfo.signatures == null || packageInfo.signatures.length <= 0) {
      return false;
    }

    for (Signature signature : packageInfo.signatures) {
      String hashedSignature = Utility.sha1hash(signature.toByteArray());
      if (!validAppSignatureHashes.contains(hashedSignature)) {
        return false;
      }
    }

    return true;
  }
}
