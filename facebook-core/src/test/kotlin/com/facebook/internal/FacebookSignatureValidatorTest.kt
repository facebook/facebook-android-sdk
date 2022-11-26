/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
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
    whenever(mockActivity.packageManager).thenReturn(mockPackageManager)
    whenever(mockActivity.applicationInfo).thenReturn(mockApplicationInfo)
  }

  @Test
  fun testInvalidWhenAppNotInstalled() {
    setupPackageManagerForApp(isInstalled = false, hasValidSignature = false)
    assertThat(validateSignature(mockActivity, PACKAGE_NAME)).isFalse
  }

  @Test
  fun testInvalidWhenInstalledWithIncorrectSignature() {
    setupPackageManagerForApp(isInstalled = true, hasValidSignature = false)
    assertThat(validateSignature(mockActivity, PACKAGE_NAME)).isFalse
  }

  @Test
  fun testValidWhenInstalledWithCorrectSignature() {
    setupPackageManagerForApp(isInstalled = true, hasValidSignature = true)
    assertThat(validateSignature(mockActivity, PACKAGE_NAME)).isTrue
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
      whenever(mockPackageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNATURES))
          .thenReturn(packageInfo)
      val signature = mock(Signature::class.java)
      whenever(signature.toByteArray()).thenReturn(byteArrayOf())
      packageInfo.signatures = arrayOf(signature)

      if (hasValidSignature) {
        whenever(Utility.sha1hash(signature.toByteArray())).thenReturn(APP_HASH)
      }
    } else {
      whenever(mockPackageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_SIGNATURES))
          .thenThrow(PackageManager.NameNotFoundException())
    }
  }
}
