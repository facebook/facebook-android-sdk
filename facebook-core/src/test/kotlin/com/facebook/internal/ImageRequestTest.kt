package com.facebook.internal

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.util.common.assertThrows
import com.nhaarman.mockitokotlin2.whenever
import java.net.URLEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class ImageRequestTest : FacebookPowerMockTestCase() {

  private val heightParam = "height"
  private val widthParam = "width"
  private val accessTokenParam = "access_token"
  private val migrationParam = "migration_overrides"

  private val apiVersion = "xyz"
  private val appId = "9999"
  private val clientToken = "abcd"
  private val accessToken = "accessToken"
  private val userId = "0000"
  private val height = 12
  private val width = 34
  private val migrateValueEncoded = URLEncoder.encode("{october_2012:true}", "utf-8")

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getGraphApiVersion()).thenReturn(apiVersion)
  }

  @Test
  fun `all valid ok`() {
    val uri = ImageRequest.getProfilePictureUri(userId, width, height, accessToken)
    val expectedUri =
        "https://graph.null/$apiVersion/$userId/picture?height=$height&width=$width&migration_overrides=$migrateValueEncoded&access_token=$accessToken"

    assertNotNull(uri)
    assertNotNull(uri.path)
    assertNotNull(uri.getQueryParameter(widthParam))
    assertNotNull(uri.getQueryParameter(heightParam))
    assertNotNull(uri.getQueryParameter(accessTokenParam))
    assertNotNull(uri.getQueryParameter(migrationParam))
    assertEquals(expectedUri, uri.toString())
  }

  @Test
  fun `all valid ok no access token`() {
    whenever(FacebookSdk.getClientToken()).thenReturn(clientToken)
    whenever(FacebookSdk.getApplicationId()).thenReturn(appId)
    val uri = ImageRequest.getProfilePictureUri(userId, width, height)
    val expectedToken = URLEncoder.encode("$appId|$clientToken", "utf-8")
    val expectedUri =
        "https://graph.null/$apiVersion/$userId/picture?height=$height&width=$width&migration_overrides=$migrateValueEncoded&access_token=$expectedToken"

    assertNotNull(uri)
    assertNotNull(uri.path)
    assertNotNull(uri.getQueryParameter(widthParam))
    assertNotNull(uri.getQueryParameter(heightParam))
    assertNotNull(uri.getQueryParameter(accessTokenParam))
    assertNotNull(uri.getQueryParameter(migrationParam))
    assertEquals(expectedUri, uri.toString())
  }

  @Test
  fun `valid no client token`() {
    whenever(FacebookSdk.getClientToken()).thenReturn("")
    whenever(FacebookSdk.getApplicationId()).thenReturn(appId)
    val uri = ImageRequest.getProfilePictureUri(userId, width, height, "")
    val expectedUri =
        "https://graph.null/$apiVersion/$userId/picture?height=$height&width=$width&migration_overrides=$migrateValueEncoded"

    assertNotNull(uri)
    assertNotNull(uri.path)
    assertNotNull(uri.getQueryParameter(widthParam))
    assertNotNull(uri.getQueryParameter(heightParam))
    assertNull(uri.getQueryParameter(accessTokenParam))
    assertNotNull(uri.getQueryParameter(migrationParam))
    assertEquals(expectedUri, uri.toString())
  }

  @Test
  fun `valid no application id`() {
    whenever(FacebookSdk.getClientToken()).thenReturn(clientToken)
    whenever(FacebookSdk.getApplicationId()).thenReturn("")
    val uri = ImageRequest.getProfilePictureUri(userId, width, height, "")
    val expectedUri =
        "https://graph.null/$apiVersion/$userId/picture?height=$height&width=$width&migration_overrides=$migrateValueEncoded"

    assertNotNull(uri)
    assertNotNull(uri.path)
    assertNotNull(uri.getQueryParameter(widthParam))
    assertNotNull(uri.getQueryParameter(heightParam))
    assertNull(uri.getQueryParameter(accessTokenParam))
    assertNotNull(uri.getQueryParameter(migrationParam))
    assertEquals(expectedUri, uri.toString())
  }

  @Test
  fun `valid no height`() {
    val uri = ImageRequest.getProfilePictureUri(userId, width, 0, accessToken)
    val expectedUri =
        "https://graph.null/$apiVersion/$userId/picture?width=$width&migration_overrides=$migrateValueEncoded&access_token=$accessToken"

    assertNotNull(uri)
    assertNotNull(uri.path)
    assertNotNull(uri.getQueryParameter(widthParam))
    assertNull(uri.getQueryParameter(heightParam))
    assertNotNull(uri.getQueryParameter(accessTokenParam))
    assertNotNull(uri.getQueryParameter(migrationParam))
    assertEquals(expectedUri, uri.toString())
  }

  @Test
  fun `valid no width`() {
    val uri = ImageRequest.getProfilePictureUri(userId, 0, height, accessToken)
    val expectedUri =
        "https://graph.null/$apiVersion/$userId/picture?height=$height&migration_overrides=$migrateValueEncoded&access_token=$accessToken"

    assertNotNull(uri)
    assertNotNull(uri.path)
    assertNull(uri.getQueryParameter(widthParam))
    assertNotNull(uri.getQueryParameter(heightParam))
    assertNotNull(uri.getQueryParameter(accessTokenParam))
    assertNotNull(uri.getQueryParameter(migrationParam))
    assertEquals(expectedUri, uri.toString())
  }

  @Test
  fun `exception when no height and no width`() {
    assertThrows<IllegalArgumentException> {
      ImageRequest.getProfilePictureUri(userId, 0, 0, accessToken)
    }
  }

  @Test
  fun `all valid ok builder`() {
    val profilePictureUri = ImageRequest.getProfilePictureUri(userId, width, height, accessToken)
    val imageRequestBuilder =
        ImageRequest.Builder(ApplicationProvider.getApplicationContext(), profilePictureUri)
    val callBack = PowerMockito.mock(ImageRequest.Callback::class.java)
    val callerTag = "1234abc"
    val imageRequest =
        imageRequestBuilder
            .setAllowCachedRedirects(true)
            .setCallback(callBack)
            .setCallerTag(callerTag)
            .build()

    assertEquals(ApplicationProvider.getApplicationContext(), imageRequest.context)
    assertEquals(profilePictureUri, imageRequest.imageUri)
    assertEquals(true, imageRequest.isCachedRedirectAllowed)
    assertEquals(callBack, imageRequest.callback)
    assertEquals(callerTag, imageRequest.callerTag)
  }
}
