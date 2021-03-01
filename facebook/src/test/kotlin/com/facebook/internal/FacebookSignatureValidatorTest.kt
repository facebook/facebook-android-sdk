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

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.core.classloader.annotations.PrepareForTest

/** Tests for {@link com.facebook.internal.FacebookSignatureValidator}. */
@PrepareForTest(Utility::class)
class FacebookSignatureValidatorTest : FacebookPowerMockTestCase() {

  companion object {
    private const val PACKAGE_NAME = "com.facebook.orca"
    private const val APP_HASH = "8a3c4b262d721acd49a4bf97d5213199c86fa2b9"
  }

  @Mock private lateinit var mockActivity: Activity
  @Mock private lateinit var mockPackageManager: PackageManager
  @Mock private lateinit var mockApplicationInfo: ApplicationInfo

  @Before
  fun init() {
    mockStatic(Utility::class.java)
    `when`(mockActivity.packageManager).thenReturn(mockPackageManager)
    `when`(mockActivity.applicationInfo).thenReturn(mockApplicationInfo)
  }

  @Test
  fun testInvalidWhenAppNotInstalled() {
    setupPackageManagerForApp(isInstalled = false, hasValidSignature = false)
    assertFalse(validateSignature(mockActivity, PACKAGE_NAME))
  }

  @Test
  fun testInvalidWhenInstalledWithIncorrectSignature() {
    setupPackageManagerForApp(isInstalled = true, hasValidSignature = false)
    assertFalse(validateSignature(mockActivity, PACKAGE_NAME))
  }

  @Test
  fun testValidWhenInstalledWithCorrectSignature() {
    setupPackageManagerForApp(isInstalled = true, hasValidSignature = true)
    assertTrue(validateSignature(mockActivity, PACKAGE_NAME))
  }

  /**
   * Sets up the PackageManager to return what we expect depending on whether app is installed.
   *
   * @param isInstalled true to simulate that app is installed
   * @param hasValidSignature true to give the app a valid signature
   */
  private fun setupPackageManagerForApp(isInstalled: Boolean, hasValidSignature: Boolean) {
    if (isInstalled) {
      val packageInfo = PackageInfo()
      `when`(mockPackageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNATURES))
          .thenReturn(packageInfo)
      val signature = mock(Signature::class.java)
      packageInfo.signatures = arrayOf(signature)

      if (hasValidSignature) {
        `when`(Utility.sha1hash(signature.toByteArray())).thenReturn(APP_HASH)
      }
    } else {
      `when`(mockPackageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNATURES))
          .thenThrow(PackageManager.NameNotFoundException())
    }
  }
}
