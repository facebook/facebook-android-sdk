/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.messenger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.FacebookSignatureValidator;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests for {@link com.facebook.messenger.MessengerUtils} */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18, manifest = Config.NONE)
@PrepareForTest({FacebookSignatureValidator.class})
public class MessengerUtilsTest extends FacebookPowerMockTestCase {

  private Activity mMockActivity;
  private ContentResolver mMockContentResolver;

  @Before
  public void setup() {
    mMockActivity = mock(Activity.class);
    mMockContentResolver = mock(ContentResolver.class);
    when(mMockActivity.getContentResolver()).thenReturn(mMockContentResolver);
    FacebookSdk.setApplicationId("200");
    FacebookSdk.setClientToken("abcdefg");
    FacebookSdk.setAutoLogAppEventsEnabled(false);
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
    PowerMockito.mockStatic(FacebookSignatureValidator.class);
  }

  @Test
  public void testMessengerIsInstalled() throws Exception {
    setupPackageManagerForMessenger(true);
    assertTrue(MessengerUtils.hasMessengerInstalled(mMockActivity));
  }

  @Test
  public void testMessengerNotInstalled() throws Exception {
    setupPackageManagerForMessenger(false);
    assertFalse(MessengerUtils.hasMessengerInstalled(mMockActivity));
  }

  @Test
  public void testShareToMessengerWith20150314Protocol() throws Exception {
    setupPackageManagerForMessenger(true);
    setupContentResolverForProtocolVersions(20150314);

    Uri uri = Uri.parse("file:///foo.jpeg");
    Uri externalUri = Uri.parse("http://example.com/foo.jpeg");
    ShareToMessengerParams params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build();
    MessengerUtils.shareToMessenger(mMockActivity, 1, params);

    // Expect it to have launched messenger with the right intent.
    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockActivity).startActivityForResult(intentArgumentCaptor.capture(), eq(1));
    Intent intent = intentArgumentCaptor.getValue();
    assertEquals(Intent.ACTION_SEND, intent.getAction());
    assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.getFlags());
    assertEquals("com.facebook.orca", intent.getPackage());
    assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM));
    assertEquals("image/jpeg", intent.getType());
    assertEquals("200", intent.getStringExtra("com.facebook.orca.extra.APPLICATION_ID"));
    assertEquals(20150314, intent.getIntExtra("com.facebook.orca.extra.PROTOCOL_VERSION", -1));
    assertEquals("{}", intent.getStringExtra("com.facebook.orca.extra.METADATA"));
    assertEquals(externalUri, intent.getParcelableExtra("com.facebook.orca.extra.EXTERNAL_URI"));
  }

  @Test
  public void testShareToMessengerWithNoProtocol() throws Exception {
    setupPackageManagerForMessenger(true);
    setupContentResolverForProtocolVersions(/* empty */ );

    Uri uri = Uri.parse("file:///foo.jpeg");
    Uri externalUri = Uri.parse("http://example.com/foo.jpeg");
    ShareToMessengerParams params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build();
    MessengerUtils.shareToMessenger(mMockActivity, 1, params);

    // Expect it to have gone to the play store.
    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockActivity).startActivity(intentArgumentCaptor.capture());
    Intent intent = intentArgumentCaptor.getValue();
    assertEquals(Intent.ACTION_VIEW, intent.getAction());
    assertEquals(Uri.parse("market://details?id=com.facebook.orca"), intent.getData());
  }

  @Test
  public void testGetMessengerThreadParamsForIntentWith20150314Protocol() throws Exception {
    // Simulate an intent that Messenger would send.
    Intent intent = new Intent();
    intent.addCategory("com.facebook.orca.category.PLATFORM_THREAD_20150314");
    Bundle extrasBundle = setupIntentWithAppLinkExtrasBundle(intent);
    extrasBundle.putString("com.facebook.orca.extra.THREAD_TOKEN", "thread_token");
    extrasBundle.putString("com.facebook.orca.extra.METADATA", "{}");
    extrasBundle.putString("com.facebook.orca.extra.PARTICIPANTS", "100,400,500");
    extrasBundle.putBoolean("com.facebook.orca.extra.IS_REPLY", true);

    // Check the parsing logic.
    MessengerThreadParams params = MessengerUtils.getMessengerThreadParamsForIntent(intent);
    assertEquals(MessengerThreadParams.Origin.REPLY_FLOW, params.origin);
    assertEquals("thread_token", params.threadToken);
    assertEquals("{}", params.metadata);
    assertEquals(Arrays.asList("100", "400", "500"), params.participants);
  }

  @Test
  public void testGetMessengerThreadParamsForIntentWithUnrecognizedIntent() throws Exception {
    // Simulate an intent that Messenger would send.
    Intent intent = new Intent();
    assertNull(MessengerUtils.getMessengerThreadParamsForIntent(intent));
  }

  @Test
  public void testFinishShareToMessengerWith20150314Protocol() throws Exception {
    // Simulate an intent that Messenger would send.
    Intent originalIntent = new Intent();
    originalIntent.addCategory("com.facebook.orca.category.PLATFORM_THREAD_20150314");
    Bundle extrasBundle = setupIntentWithAppLinkExtrasBundle(originalIntent);
    extrasBundle.putString("com.facebook.orca.extra.THREAD_TOKEN", "thread_token");
    extrasBundle.putString("com.facebook.orca.extra.METADATA", "{}");
    extrasBundle.putString("com.facebook.orca.extra.PARTICIPANTS", "100,400,500");
    when(mMockActivity.getIntent()).thenReturn(originalIntent);

    // Setup the data the app will send back to messenger.
    Uri uri = Uri.parse("file:///foo.jpeg");
    Uri externalUri = Uri.parse("http://example.com/foo.jpeg");
    ShareToMessengerParams params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build();

    // Call finishShareToMessenger and verify the results.
    MessengerUtils.finishShareToMessenger(mMockActivity, params);
    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockActivity).setResult(eq(Activity.RESULT_OK), intentArgumentCaptor.capture());
    verify(mMockActivity).finish();

    Intent intent = intentArgumentCaptor.getValue();
    assertNotNull(intent);
    assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.getFlags());
    assertEquals(20150314, intent.getIntExtra("com.facebook.orca.extra.PROTOCOL_VERSION", -1));
    assertEquals("thread_token", intent.getStringExtra("com.facebook.orca.extra.THREAD_TOKEN"));
    assertEquals(uri, intent.getData());
    assertEquals("image/jpeg", intent.getType());
    assertEquals("200", intent.getStringExtra("com.facebook.orca.extra.APPLICATION_ID"));
    assertEquals("{}", intent.getStringExtra("com.facebook.orca.extra.METADATA"));
    assertEquals(externalUri, intent.getParcelableExtra("com.facebook.orca.extra.EXTERNAL_URI"));
  }

  @Test
  public void testFinishShareToMessengerWithUnexpectedIntent() throws Exception {
    // Simulate an intent that Messenger would send.
    Intent originalIntent = new Intent();
    when(mMockActivity.getIntent()).thenReturn(originalIntent);

    // Setup the data the app will send back to messenger.
    Uri uri = Uri.parse("file:///foo.jpeg");
    Uri externalUri = Uri.parse("http://example.com/foo.jpeg");
    ShareToMessengerParams params =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{}")
            .setExternalUri(externalUri)
            .build();

    // Call finishShareToMessenger and verify the results.
    MessengerUtils.finishShareToMessenger(mMockActivity, params);
    verify(mMockActivity).setResult(Activity.RESULT_CANCELED, null);
    verify(mMockActivity).finish();
  }

  /**
   * Sets up the PackageManager to return what we expect depending on whether messenger is
   * installed.
   *
   * @param isInstalled true to simulate that messenger is installed
   */
  private void setupPackageManagerForMessenger(boolean isInstalled) {
    when(FacebookSignatureValidator.validateSignature(mMockActivity, "com.facebook.orca"))
        .thenReturn(isInstalled);
  }

  /**
   * Sets up the Messenger content resolver to reply that it supports the specified versions.
   *
   * @param versions the versions that it should support
   */
  private void setupContentResolverForProtocolVersions(int... versions) {
    MatrixCursor matrixCursor = new MatrixCursor(new String[] {"version"});
    for (int version : versions) {
      matrixCursor.addRow(new Object[] {version});
    }

    when(mMockContentResolver.query(
            Uri.parse("content://com.facebook.orca.provider.MessengerPlatformProvider/versions"),
            new String[] {"version"},
            null,
            null,
            null))
        .thenReturn(matrixCursor);
  }

  /**
   * Adds the structure to the Intent to look like an app link and returns the Extras section which
   * is where the messenger parameters go.
   *
   * @param intent the intent to add to
   * @return the extras Bundle
   */
  private Bundle setupIntentWithAppLinkExtrasBundle(Intent intent) {
    Bundle appLinksDataBundle = new Bundle();
    intent.putExtra("al_applink_data", appLinksDataBundle);
    Bundle extrasBundle = new Bundle();
    appLinksDataBundle.putBundle("extras", extrasBundle);
    return extrasBundle;
  }
}
