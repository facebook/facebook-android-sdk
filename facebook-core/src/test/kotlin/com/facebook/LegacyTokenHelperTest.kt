package com.facebook

import android.os.Bundle
import com.facebook.internal.Utility
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, Utility::class)
class LegacyTokenHelperTest : FacebookPowerMockTestCase() {

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.mockStatic(Utility::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.isFullyInitialized()).thenReturn(true)
    val json = JSONObject("{\"id\":\"swag\"}")
    PowerMockito.`when`(Utility.awaitGetGraphMeRequestWithCache(anyString())).thenReturn(json)
  }

  @Test
  fun `test legacy token helper`() {
    val permissions: Set<String> = Utility.hashSet("stream_publish", "go_outside_and_play")
    val declinedPermissions: Set<String> = Utility.hashSet("no you may not", "no soup for you")
    val expiredPermissions: Set<String> = Utility.hashSet("expired", "oh no")
    val token = "AnImaginaryTokenValue"
    val later = FacebookTestUtility.nowPlusSeconds(60)
    val earlier = FacebookTestUtility.nowPlusSeconds(-60)
    val applicationId = "1234"

    val bundle = Bundle()
    LegacyTokenHelper.putToken(bundle, token)
    LegacyTokenHelper.putExpirationDate(bundle, later)
    LegacyTokenHelper.putSource(bundle, AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    LegacyTokenHelper.putLastRefreshDate(bundle, earlier)
    LegacyTokenHelper.putPermissions(bundle, permissions)
    LegacyTokenHelper.putDeclinedPermissions(bundle, declinedPermissions)
    LegacyTokenHelper.putExpiredPermissions(bundle, expiredPermissions)
    LegacyTokenHelper.putApplicationId(bundle, applicationId)

    val accessToken = AccessToken.createFromLegacyCache(bundle)
    Assert.assertNotNull(accessToken)
    checkNotNull(accessToken)
    FacebookTestUtility.assertSameCollectionContents(permissions, accessToken.permissions)
    Assert.assertEquals(token, accessToken.token)
    Assert.assertEquals(AccessTokenSource.FACEBOOK_APPLICATION_WEB, accessToken.source)
    Assert.assertTrue(!accessToken.isExpired)

    val cache = AccessTokenTestHelper.toLegacyCacheBundle(accessToken)
    FacebookTestUtility.assertEqualContentsWithoutOrder(bundle, cache)
  }
}
