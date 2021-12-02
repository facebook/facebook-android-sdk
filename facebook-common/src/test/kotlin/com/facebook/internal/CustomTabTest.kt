package com.facebook.internal

import android.content.Intent
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.login.CustomTabLoginMethodHandler.OAUTH_DIALOG
import com.facebook.login.CustomTabPrefetchHelper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(CustomTabsIntent::class)
class CustomTabTest : FacebookPowerMockTestCase() {
  private lateinit var mockCustomTabsIntent: CustomTabsIntent
  private lateinit var parameters: Bundle

  @Before
  fun init() {
    parameters = Bundle()
    parameters.putString(ServerProtocol.DIALOG_PARAM_SCOPE, "user_name,user_birthday")

    mockCustomTabsIntent = mock()
    Whitebox.setInternalState(mockCustomTabsIntent, "intent", Intent(Intent.ACTION_VIEW))

    PowerMockito.whenNew(CustomTabsIntent::class.java)
        .withAnyArguments()
        .thenReturn(mockCustomTabsIntent)

    val mockCustomTabPrefetchHelperCompanion = mock<CustomTabPrefetchHelper.Companion>()
    Whitebox.setInternalState(
        CustomTabPrefetchHelper::class.java, "Companion", mockCustomTabPrefetchHelperCompanion)
  }

  @Test
  fun `test get URI for action`() {
    val uri = CustomTab.getURIForAction(OAUTH_DIALOG, parameters)
    val version = FacebookSdk.getGraphApiVersion()
    assertThat(uri.toString())
        .isEqualTo("https://m.facebook.com/$version/dialog/oauth?scope=user_name%2Cuser_birthday")
  }

  @Test
  fun `test open custom tab`() {
    val customTab = CustomTab(OAUTH_DIALOG, parameters)
    customTab.openCustomTab(mock(), "com.facebook.internal")
    verify(mockCustomTabsIntent).launchUrl(any(), any())
  }
}
