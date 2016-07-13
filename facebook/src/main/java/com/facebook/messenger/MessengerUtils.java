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
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.facebook.FacebookSdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bolts.AppLinks;

/**
 * Utilities for Messenger Content Platform.
 * <p>
 *   Applications should specify the app id in their manifest or call
 *   {@link com.facebook.FacebookSdk#setApplicationId(String)} } in
 *   their application startup path. For specifying in the manifest, add a meta tag in your
 *   &lt;application&gt; tag.
 *
 *   <pre>
 *    &lt;meta-data android:name="com.facebook.sdk.ApplicationId" android:value="YOUR_APP_ID"/&gt;
 *   </pre>
 *
 * </p>
 *
 * <p>
 *   When sharing to Messenger, apps should call the {@link #shareToMessenger} method. For example,
 *
 *   <pre>
 *     ShareToMessengerParams params = ShareToMessengerParams.newBuilder(uri, "image/*")
 *         .setMetaData(metaData)
 *         .build();
 *     MessengerUtils.shareToMessenger(this, REQUEST_CODE_SHARE_TO_MESSENGER, params);
 *   </pre>
 *
 *   To handle receiving a composer shortcut or reply intent from Messenger, apps should
 *   put the following intent filter in their manifest for the activity that receives the intent:
 *
 *   <pre>
 *           &lt;intent-filter&gt;
 *             &lt;action android:name="android.intent.action.PICK" /&gt;
 *             &lt;category android:name="android.intent.category.DEFAULT"/&gt;
 *             &lt;category android:name="com.facebook.orca.category.PLATFORM_THREAD_20150311"/&gt;
 *           &lt;/intent-filter&gt;
 *   </pre>
 *
 *   When handling the intent, then call {@link #getMessengerThreadParamsForIntent} to receive
 *   the parameters for messenger. When the user has clicked the Send button to send the content
 *   to Messenger, then call {@link #finishShareToMessenger} to return the data back to Messenger.
 * </p>
 */
public class MessengerUtils {

  private static final String TAG = "MessengerUtils";

  public static final String PACKAGE_NAME = "com.facebook.orca";

  public static final String EXTRA_PROTOCOL_VERSION = "com.facebook.orca.extra.PROTOCOL_VERSION";
  public static final String EXTRA_APP_ID = "com.facebook.orca.extra.APPLICATION_ID";
  public static final String EXTRA_REPLY_TOKEN_KEY = "com.facebook.orca.extra.REPLY_TOKEN";
  public static final String EXTRA_THREAD_TOKEN_KEY = "com.facebook.orca.extra.THREAD_TOKEN";
  public static final String EXTRA_METADATA = "com.facebook.orca.extra.METADATA";
  public static final String EXTRA_EXTERNAL_URI = "com.facebook.orca.extra.EXTERNAL_URI";
  public static final String EXTRA_PARTICIPANTS = "com.facebook.orca.extra.PARTICIPANTS";
  public static final String EXTRA_IS_REPLY = "com.facebook.orca.extra.IS_REPLY";
  public static final String EXTRA_IS_COMPOSE = "com.facebook.orca.extra.IS_COMPOSE";
  public static final int PROTOCOL_VERSION_20150314 = 20150314;

  public static final String ORCA_THREAD_CATEGORY_20150314 =
      "com.facebook.orca.category.PLATFORM_THREAD_20150314";

  /**
   * Starts an intent to share a piece of media on Messenger using the messenger content platform.
   *
   * @param activity the activity sharing the content
   * @param requestCode a unique request code for {@link Activity#startActivityForResult}
   * @param shareToMessengerParams parameters for what to share
   */
  public static void shareToMessenger(
      Activity activity,
      int requestCode,
      ShareToMessengerParams shareToMessengerParams) {
    if (!MessengerUtils.hasMessengerInstalled(activity)) {
      MessengerUtils.openMessengerInPlayStore(activity);
      return;
    }

    Set<Integer> allAvailableVersions = getAllAvailableProtocolVersions(activity);
    if (allAvailableVersions.contains(PROTOCOL_VERSION_20150314)) {
      shareToMessenger20150314(activity, requestCode, shareToMessengerParams);
    } else {
      // TODO -- should we show a upgrade dialog?
      MessengerUtils.openMessengerInPlayStore(activity);
    }
  }

  private static void shareToMessenger20150314(
      Activity activity,
      int requestCode,
      ShareToMessengerParams shareToMessengerParams) {
    try {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      shareIntent.setPackage(PACKAGE_NAME);
      shareIntent.putExtra(Intent.EXTRA_STREAM, shareToMessengerParams.uri);
      shareIntent.setType(shareToMessengerParams.mimeType);
      String appId = FacebookSdk.getApplicationId();
      if (appId != null) {
        shareIntent.putExtra(EXTRA_PROTOCOL_VERSION, PROTOCOL_VERSION_20150314);
        shareIntent.putExtra(EXTRA_APP_ID, appId);
        shareIntent.putExtra(EXTRA_METADATA, shareToMessengerParams.metaData);
        shareIntent.putExtra(EXTRA_EXTERNAL_URI, shareToMessengerParams.externalUri);
      }

      activity.startActivityForResult(shareIntent, requestCode);
    } catch (ActivityNotFoundException e) {
      Intent openMessenger = activity.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
      activity.startActivity(openMessenger);
    }
  }

  /**
   * When handling an {@code Intent} from Messenger, call this to parse the parameters of the
   * intent.
   *
   * @param intent the intent of the activity
   * @return a {@link MessengerThreadParams} or null if this intent wasn't recognized as a request
   *     from Messenger to share.
   */
  public static MessengerThreadParams getMessengerThreadParamsForIntent(Intent intent) {
    Set<String> categories = intent.getCategories();
    if (categories == null) {
      return null;
    }
    if (categories.contains(ORCA_THREAD_CATEGORY_20150314)) {
      Bundle appLinkExtras = AppLinks.getAppLinkExtras(intent);
      String threadToken = appLinkExtras.getString(EXTRA_THREAD_TOKEN_KEY);
      String metadata = appLinkExtras.getString(EXTRA_METADATA);
      String participants = appLinkExtras.getString(EXTRA_PARTICIPANTS);
      boolean isReply = appLinkExtras.getBoolean(EXTRA_IS_REPLY);
      boolean isCompose = appLinkExtras.getBoolean(EXTRA_IS_COMPOSE);
      MessengerThreadParams.Origin origin = MessengerThreadParams.Origin.UNKNOWN;
      if (isReply) {
        origin = MessengerThreadParams.Origin.REPLY_FLOW;
      } else if (isCompose) {
        origin = MessengerThreadParams.Origin.COMPOSE_FLOW;
      }

      return new MessengerThreadParams(
          origin,
          threadToken,
          metadata,
          parseParticipants(participants));
    } else {
      return null;
    }
  }

  /**
   * Finishes the activity and returns the media item the user picked to Messenger.
   *
   * @param activity the activity that received the original intent from Messenger
   * @param shareToMessengerParams parameters for what to share
   */
  public static void finishShareToMessenger(
      Activity activity,
      ShareToMessengerParams shareToMessengerParams) {
    Intent originalIntent = activity.getIntent();
    Set<String> categories = originalIntent.getCategories();
    if (categories == null) {
      // This shouldn't happen.
      activity.setResult(Activity.RESULT_CANCELED, null);
      activity.finish();
      return;
    }

    if (categories.contains(ORCA_THREAD_CATEGORY_20150314)) {
      Bundle appLinkExtras = AppLinks.getAppLinkExtras(originalIntent);

      Intent resultIntent = new Intent();
      if (categories.contains(ORCA_THREAD_CATEGORY_20150314)) {
        resultIntent.putExtra(EXTRA_PROTOCOL_VERSION, MessengerUtils.PROTOCOL_VERSION_20150314);
        String threadToken = appLinkExtras.getString(MessengerUtils.EXTRA_THREAD_TOKEN_KEY);
        resultIntent.putExtra(EXTRA_THREAD_TOKEN_KEY, threadToken);
      } else {
        throw new RuntimeException(); // Can't happen.
      }
      resultIntent.setDataAndType(shareToMessengerParams.uri, shareToMessengerParams.mimeType);
      resultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      resultIntent.putExtra(EXTRA_APP_ID, FacebookSdk.getApplicationId());
      resultIntent.putExtra(EXTRA_METADATA, shareToMessengerParams.metaData);
      resultIntent.putExtra(EXTRA_EXTERNAL_URI, shareToMessengerParams.externalUri);
      activity.setResult(Activity.RESULT_OK, resultIntent);
      activity.finish();
    } else {
      // This shouldn't happen.
      activity.setResult(Activity.RESULT_CANCELED, null);
      activity.finish();
    }
  }

  /**
   * Checks whether any version of messenger is installed.
   *
   * @param context an android context
   * @return whether any version of messenger is installed
   */
  public static boolean hasMessengerInstalled(Context context) {
    try {
      context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  /**
   * Opens the play store to install Messenger.
   *
   * @param context an android context.
   */
  public static void openMessengerInPlayStore(Context context) {
    try {
      startViewUri(context, "market://details?id=" + PACKAGE_NAME);
    } catch (ActivityNotFoundException anfe) {
      startViewUri(context, "http://play.google.com/store/apps/details?id=" + PACKAGE_NAME);
    }
  }

  private static Set<Integer> getAllAvailableProtocolVersions(Context context) {
    ContentResolver contentResolver = context.getContentResolver();
    Set<Integer> allAvailableVersions = new HashSet<Integer>();
    Uri uri = Uri.parse("content://com.facebook.orca.provider.MessengerPlatformProvider/versions");
    String [] projection = new String[]{ "version" };
    Cursor c = contentResolver.query(uri, projection, null, null, null);
    if (c != null) {
      try {
        int versionColumnIndex = c.getColumnIndex("version");
        while (c.moveToNext()) {
          int version = c.getInt(versionColumnIndex);
          allAvailableVersions.add(version);
        }
      } finally {
        c.close();
      }
    }
    return allAvailableVersions;
  }

  private static void startViewUri(Context context, String uri) {
    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
  }

  private static List<String> parseParticipants(String s) {
    if (s == null || s.length() == 0) {
      return Collections.emptyList();
    }
    String[] parts = s.split(",");
    List<String> ret = new ArrayList<String>();
    for (String part : parts) {
      ret.add(part.trim());
    }
    return ret;
  }
}
