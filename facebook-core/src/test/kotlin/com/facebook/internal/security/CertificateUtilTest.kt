package com.facebook.internal.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock

class CertificateUtilTest : FacebookPowerMockTestCase() {
  private lateinit var mockContext: Context
  private lateinit var mockPackageManager: PackageManager
  private lateinit var mockPackageInfo: PackageInfo
  private lateinit var mockSignature1: Signature
  private lateinit var mockSignature2: Signature

  companion object {
    private const val PACKAGE_NAME = "com.facebook.orca"
  }

  @Before
  fun init() {
    mockSignature1 = mock(Signature::class.java)
    whenever(mockSignature1.toByteArray()).thenReturn(byteArrayOf(1))
    mockSignature2 = mock(Signature::class.java)
    whenever(mockSignature2.toByteArray()).thenReturn(byteArrayOf(2))

    mockContext = mock(Context::class.java)
    mockPackageManager = mock(PackageManager::class.java)
    mockPackageInfo = PackageInfo()
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockContext.packageName).thenReturn(PACKAGE_NAME)
    whenever(mockPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(mockPackageInfo)
  }

  @Test
  fun `test no signature is provided`() {
    mockPackageInfo.signatures = emptyArray()
    val certificateHash = CertificateUtil.getCertificateHash(mockContext)
    assertEquals("", certificateHash)
  }

  @Test
  fun `test exception in access package info`() {
    whenever(mockPackageManager.getPackageInfo(anyString(), anyInt()))
        .thenThrow(PackageManager.NameNotFoundException(PACKAGE_NAME))
    val certificateHash = CertificateUtil.getCertificateHash(mockContext)
    assertEquals("", certificateHash)
  }

  @Test
  fun `test with sample signature`() {
    mockPackageInfo.signatures = arrayOf(mockSignature1)
    val certificateHash = CertificateUtil.getCertificateHash(mockContext)
    assertNotEquals("", certificateHash)
  }

  @Test
  fun `test with two sample signatures`() {
    mockPackageInfo.signatures = arrayOf(mockSignature1)
    val certificateHash1 = CertificateUtil.getCertificateHash(mockContext)
    mockPackageInfo.signatures = arrayOf(mockSignature1, mockSignature2)
    val certificateHash = CertificateUtil.getCertificateHash(mockContext)
    assertThat(certificateHash).contains(certificateHash1)
    assertThat(certificateHash).contains(CertificateUtil.DELIMITER)
    assertThat(certificateHash.endsWith(CertificateUtil.DELIMITER)).isFalse
  }
}
