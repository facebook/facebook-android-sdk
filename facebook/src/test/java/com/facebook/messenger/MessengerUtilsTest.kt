/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.messenger

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookSignatureValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Tests for [com.facebook.messenger.MessengerUtils] */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [18], manifest = Config.NONE)
@PrepareForTest(FacebookSignatureValidator::class)
class MessengerUtilsTest : FacebookPowerMockTestCase() {

  private lateinit var mMockActivity: Activity
  private lateinit var mMockContentResolver: ContentResolver

  @Before
  override fun setup() {
    mMockActivity = Mockito.mock(Activity::class.java)
    mMockContentResolver = Mockito.mock(ContentResolver::class.java)
    Mockito.`when`(mMockActivity.contentResolver).thenReturn(mMockContentResolver)
    FacebookSdk.setApplicationId("200")
    FacebookSdk.setClientToken("abcdefg")
    FacebookSdk.setAutoLogAppEventsEnabled(false)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    PowerMockito.mockStatic(FacebookSignatureValidator::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun testMessengerIsInstalled() {
    setupPackageManagerForMessenger(true)
    assertTrue(MessengerUtils.hasMessengerInstalled(mMockActivity))
  }

  @Test
  @Throws(Exception::class)
  fun testMessengerNotInstalled() {
    setupPackageManagerForMessenger(false)
    assertFalse(MessengerUtils.hasMessengerInstalled(mMockActivity))
  }

  @Test
  @Throws(Exception::class)
  fun testShareToMessengerWith20150314Protocol() {
    setupPackageManagerForMessenger(true)
    setupContentResolverForProtocolVersions(20150314)
    val uri = Uri.parse("file:///foo.jpeg")
    val externalUri = Uri.parse("http://example.com/foo.jpeg")
    val params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build()
    MessengerUtils.shareToMessenger(mMockActivity, 1, params)

    // Expect it to have launched messenger with the right intent.
    val intentArgumentCaptor = ArgumentCaptor.forClass(Intent::class.java)
    verify(mMockActivity)
        .startActivityForResult(intentArgumentCaptor.capture(), ArgumentMatchers.eq(1))
    val intent = intentArgumentCaptor.value
    assertEquals(Intent.ACTION_SEND, intent.action)
    assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION.toLong(), intent.flags.toLong())
    assertEquals("com.facebook.orca", intent.getPackage())
    assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
    assertEquals("image/jpeg", intent.type)
    assertEquals("200", intent.getStringExtra("com.facebook.orca.extra.APPLICATION_ID"))
    assertEquals(
        20150314, intent.getIntExtra("com.facebook.orca.extra.PROTOCOL_VERSION", -1).toLong())
    assertEquals("{}", intent.getStringExtra("com.facebook.orca.extra.METADATA"))
    assertEquals(externalUri, intent.getParcelableExtra("com.facebook.orca.extra.EXTERNAL_URI"))
  }

  @Test
  @Throws(Exception::class)
  fun testShareToMessengerWithNoProtocol() {
    setupPackageManagerForMessenger(true)
    setupContentResolverForProtocolVersions()
    val uri = Uri.parse("file:///foo.jpeg")
    val externalUri = Uri.parse("http://example.com/foo.jpeg")
    val params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build()
    MessengerUtils.shareToMessenger(mMockActivity, 1, params)

    // Expect it to have gone to the play store.
    val intentArgumentCaptor = ArgumentCaptor.forClass(Intent::class.java)
    verify(mMockActivity).startActivity(intentArgumentCaptor.capture())
    val intent = intentArgumentCaptor.value
    assertEquals(Intent.ACTION_VIEW, intent.action)
    assertEquals(Uri.parse("market://details?id=com.facebook.orca"), intent.data)
  }

  @Test
  fun testGetMessengerThreadParamsForIntentWith20150314Protocol() {
    // Simulate an intent that Messenger would send.
    val intent = Intent()
    intent.addCategory("com.facebook.orca.category.PLATFORM_THREAD_20150314")
    val extrasBundle = setupIntentWithAppLinkExtrasBundle(intent)
    extrasBundle.putString("com.facebook.orca.extra.THREAD_TOKEN", "thread_token")
    extrasBundle.putString("com.facebook.orca.extra.METADATA", "{}")
    extrasBundle.putString("com.facebook.orca.extra.PARTICIPANTS", "100,400,500")
    extrasBundle.putBoolean("com.facebook.orca.extra.IS_REPLY", true)

    // Check the parsing logic.
    val params = MessengerUtils.getMessengerThreadParamsForIntent(intent)
    assertEquals(MessengerThreadParams.Origin.REPLY_FLOW, params?.origin)
    assertEquals("thread_token", params?.threadToken)
    assertEquals("{}", params?.metadata)
    assertEquals(listOf("100", "400", "500"), params?.participants)
  }

  @Test
  @Throws(Exception::class)
  fun testGetMessengerThreadParamsForIntentWithUnrecognizedIntent() {
    // Simulate an intent that Messenger would send.
    val intent = Intent()
    assertNull(MessengerUtils.getMessengerThreadParamsForIntent(intent))
  }

  @Test
  @Throws(Exception::class)
  fun testFinishShareToMessengerWith20150314Protocol() {
    // Simulate an intent that Messenger would send.
    val originalIntent = Intent()
    originalIntent.addCategory("com.facebook.orca.category.PLATFORM_THREAD_20150314")
    val extrasBundle = setupIntentWithAppLinkExtrasBundle(originalIntent)
    extrasBundle.putString("com.facebook.orca.extra.THREAD_TOKEN", "thread_token")
    extrasBundle.putString("com.facebook.orca.extra.METADATA", "{}")
    extrasBundle.putString("com.facebook.orca.extra.PARTICIPANTS", "100,400,500")
    Mockito.`when`(mMockActivity.intent).thenReturn(originalIntent)

    // Setup the data the app will send back to messenger.
    val uri = Uri.parse("file:///foo.jpeg")
    val externalUri = Uri.parse("http://example.com/foo.jpeg")
    val params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build()

    // Call finishShareToMessenger and verify the results.
    MessengerUtils.finishShareToMessenger(mMockActivity, params)
    val intentArgumentCaptor = ArgumentCaptor.forClass(Intent::class.java)
    verify(mMockActivity)
        .setResult(ArgumentMatchers.eq(Activity.RESULT_OK), intentArgumentCaptor.capture())
    verify(mMockActivity).finish()
    val intent = intentArgumentCaptor.value
    assertNotNull(intent)
    assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION.toLong(), intent.flags.toLong())
    assertEquals(
        20150314, intent.getIntExtra("com.facebook.orca.extra.PROTOCOL_VERSION", -1).toLong())
    assertEquals("thread_token", intent.getStringExtra("com.facebook.orca.extra.THREAD_TOKEN"))
    assertEquals(uri, intent.data)
    assertEquals("image/jpeg", intent.type)
    assertEquals("200", intent.getStringExtra("com.facebook.orca.extra.APPLICATION_ID"))
    assertEquals("{}", intent.getStringExtra("com.facebook.orca.extra.METADATA"))
    assertEquals(externalUri, intent.getParcelableExtra("com.facebook.orca.extra.EXTERNAL_URI"))
  }

  @Test
  @Throws(Exception::class)
  fun testFinishShareToMessengerWithUnexpectedIntent() {
    // Simulate an intent that Messenger would send.
    val originalIntent = Intent()
    Mockito.`when`(mMockActivity.intent).thenReturn(originalIntent)

    // Setup the data the app will send back to messenger.
    val uri = Uri.parse("file:///foo.jpeg")
    val externalUri = Uri.parse("http://example.com/foo.jpeg")
    val params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build()

    // Call finishShareToMessenger and verify the results.
    MessengerUtils.finishShareToMessenger(mMockActivity, params)
    verify(mMockActivity).setResult(Activity.RESULT_CANCELED, null)
    verify(mMockActivity).finish()
  }

  /**
   * Sets up the PackageManager to return what we expect depending on whether messenger is
   * installed.
   *
   * @param isInstalled true to simulate that messenger is installed
   */
  private fun setupPackageManagerForMessenger(isInstalled: Boolean) {
    Mockito.`when`(FacebookSignatureValidator.validateSignature(mMockActivity, "com.facebook.orca"))
        .thenReturn(isInstalled)
  }

  /**
   * Sets up the Messenger content resolver to reply that it supports the specified versions.
   *
   * @param versions the versions that it should support
   */
  private fun setupContentResolverForProtocolVersions(vararg versions: Int) {
    val matrixCursor = MatrixCursor(arrayOf("version"))
    versions.forEach { matrixCursor.addRow(arrayOf<Any>(it)) }

    val versionsUri =
        Uri.parse("content://com.facebook.orca.provider.MessengerPlatformProvider/versions")
    Mockito.`when`(mMockContentResolver.query(versionsUri, arrayOf("version"), null, null, null))
        .thenReturn(matrixCursor)
  }

  /**
   * Adds the structure to the Intent to look like an app link and returns the Extras section which
   * is where the messenger parameters go.
   *
   * @param intent the intent to add to
   * @return the extras Bundle
   */
  private fun setupIntentWithAppLinkExtrasBundle(intent: Intent): Bundle {
    val appLinksDataBundle = Bundle()
    intent.putExtra("al_applink_data", appLinksDataBundle)
    val extrasBundle = Bundle()
    appLinksDataBundle.putBundle("extras", extrasBundle)
    return extrasBundle
  }
}
