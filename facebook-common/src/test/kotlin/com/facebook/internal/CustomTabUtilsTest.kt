package com.facebook.internal

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, Validate::class)
class CustomTabUtilsTest : FacebookPowerMockTestCase() {
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(Validate::class.java)
  }

  @Test
  fun `test get valid redirect URI if developer defined redirect is valid`() {
    whenever(Validate.hasCustomTabRedirectActivity(any(), any())).thenReturn(true)
    assertThat(CustomTabUtils.getValidRedirectURI(TEST_DOMAIN)).isEqualTo(TEST_DOMAIN)
  }

  @Test
  fun `test get valid redirect URI if no redirect is not valid`() {
    whenever(Validate.hasCustomTabRedirectActivity(any(), any())).thenReturn(false)
    assertThat(CustomTabUtils.getValidRedirectURI(TEST_DOMAIN)).isEqualTo("")
  }

  companion object {
    const val TEST_DOMAIN = "protocol://test-domain/1234"
  }
}
