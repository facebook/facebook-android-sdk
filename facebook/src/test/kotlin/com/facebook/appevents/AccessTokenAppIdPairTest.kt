package com.facebook.appevents

import com.facebook.AccessToken
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class AccessTokenAppIdPairTest : FacebookPowerMockTestCase() {

  companion object {
    const val TOKEN_STRING = "fb123token"
    const val TOKEN_STRING_NEW = "fb123tokennew"
    const val APP_ID = "1234567"
  }

  lateinit var pair1: AccessTokenAppIdPair
  lateinit var pair2: AccessTokenAppIdPair
  lateinit var pair3: AccessTokenAppIdPair

  @Before
  fun init() {
    pair1 = AccessTokenAppIdPair(TOKEN_STRING, APP_ID)
    pair2 = AccessTokenAppIdPair(TOKEN_STRING, APP_ID)
    pair3 = AccessTokenAppIdPair(TOKEN_STRING_NEW, APP_ID)

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
  }

  @Test
  fun testEqual() {
    assertThat(pair1 == pair2).isTrue
    assertThat(pair1 == pair3).isFalse
  }

  @Test
  fun testAsMapKey() {
    val map = hashMapOf<AccessTokenAppIdPair, String>()
    map[pair1] = "test1"
    map[pair2] = "test2"
    map[pair3] = "test3"
    assertThat(map.size).isEqualTo(2)
    assertThat(map[pair1]).isEqualTo("test2")
    assertThat(map[pair2]).isEqualTo("test2")
    assertThat(map[pair3]).isEqualTo("test3")
  }

  @Test
  fun testConstructFromAccessToken() {
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(APP_ID)
    val tokenMock: AccessToken = mock()
    PowerMockito.`when`(tokenMock.token).thenReturn(TOKEN_STRING)

    val pair = AccessTokenAppIdPair(tokenMock)
    assertThat(pair.accessTokenString).isEqualTo(TOKEN_STRING)
    assertThat(pair.applicationId).isEqualTo(APP_ID)
    assertThat(pair == pair1).isTrue
  }
}
