/**
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

package com.facebook.messenger;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

import com.facebook.FacebookSdk;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link com.facebook.messenger.MessengerUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18, manifest = Config.NONE)
public class MessengerUtilsTest {

  private Activity mMockActivity;
  private PackageManager mMockPackageManager;
  private ContentResolver mMockContentResolver;

  @Before
  public void setup() {
    mMockActivity = mock(Activity.class);
    mMockPackageManager = mock(PackageManager.class);
    mMockContentResolver = mock(ContentResolver.class);
    when(mMockActivity.getPackageManager()).thenReturn(mMockPackageManager);
    when(mMockActivity.getContentResolver()).thenReturn(mMockContentResolver);
    FacebookSdk.sdkInitialize(Robolectric.application);
    FacebookSdk.setApplicationId("200");
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
    ShareToMessengerParams params = ShareToMessengerParams
        .newBuilder(uri, "image/jpeg")
        .setMetaData("{}")
        .setExternalUri(externalUri)
        .build();
    MessengerUtils.shareToMessenger(mMockActivity, 1, params);

    // Expect it to have launched messenger with the right intent.
    ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockActivity).startActivityForResult(
        intentArgumentCaptor.capture(),
        eq(1));
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
    setupContentResolverForProtocolVersions(/* empty */);

    Uri uri = Uri.parse("file:///foo.jpeg");
    Uri externalUri = Uri.parse("http://example.com/foo.jpeg");
    ShareToMessengerParams params = ShareToMessengerParams
        .newBuilder(uri, "image/jpeg")
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
    ShareToMessengerParams params = ShareToMessengerParams
        .newBuilder(uri, "image/jpeg")
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
    ShareToMessengerParams params = ShareToMessengerParams
        .newBuilder(uri, "image/jpeg")
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
  private void setupPackageManagerForMessenger(boolean isInstalled) throws Exception {
    if (isInstalled) {
      when(mMockPackageManager.getPackageInfo("com.facebook.orca", 0))
          .thenReturn(new PackageInfo());
    } else {
      when(mMockPackageManager.getPackageInfo("com.facebook.orca", 0))
          .thenThrow(new PackageManager.NameNotFoundException());
    }
  }

  /**
   * Sets up the Messenger content resolver to reply that it supports the specified versions.
   *
   * @param versions the versions that it should support
   */
  private void setupContentResolverForProtocolVersions(int... versions) {
    MatrixCursor matrixCursor = new MatrixCursor(new String[]{"version"});
    for (int version : versions) {
      matrixCursor.addRow(new Object[]{version});
    }

    when(mMockContentResolver.query(
        Uri.parse("content://com.facebook.orca.provider.MessengerPlatformProvider/versions"),
        new String[]{"version"},
        null,
        null,
        null))
        .thenReturn(matrixCursor);
  }

  /**
   * Adds the structure to the Intent to look like an app link and returns the Extras section
   * which is where the messenger parameters go.
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
