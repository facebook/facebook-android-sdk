/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.messenger

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.bolts.AppLinks.getAppLinkExtras
import com.facebook.internal.FacebookSignatureValidator.validateSignature
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.RuntimeException
import java.util.HashSet

/**
 * **Utilities for Messenger Content Platform.**
 *
 * Applications should specify the app id in their manifest or call
 * [com.facebook.FacebookSdk.setApplicationId] in their application startup path. For specifying in
 * the manifest, add a meta tag in your &lt;application&gt; tag.
 *
 * ```xml
 * <meta-data android:name="com.facebook.sdk.ApplicationId" android:value="YOUR_APP_ID"/>
 * ```
 *
 * When sharing to Messenger, apps should call the [shareToMessenger] method. For example,
 *
 * ```kotlin
 * val params = ShareToMessengerParams.newBuilder(uri, "image/ *")
 *   .setMetaData(metaData)
 *   .build();
 * MessengerUtils.shareToMessenger(this, REQUEST_CODE_SHARE_TO_MESSENGER, params);
 * ```
 *
 * To handle receiving a composer shortcut or reply intent from Messenger, apps should put the
 * following intent filter in their manifest for the activity that receives the intent:
 *
 * ```xml
 * <intent-filter>
 *   <action android:name="android.intent.action.PICK" />
 *   <category android:name="android.intent.category.DEFAULT"/>
 *   <category android:name="com.facebook.orca.category.PLATFORM_THREAD_20150311"/>
 * </intent-filter>
 * ```
 *
 * When handling the intent, then call [getMessengerThreadParamsForIntent] to receive the parameters
 * for messenger. When the user has clicked the Send button to send the content to Messenger, then
 * call [finishShareToMessenger] to return the data back to Messenger.
 */
object MessengerUtils {
  private const val TAG = "MessengerUtils"
  const val PACKAGE_NAME = "com.facebook.orca"
  const val EXTRA_PROTOCOL_VERSION = "com.facebook.orca.extra.PROTOCOL_VERSION"
  const val EXTRA_APP_ID = "com.facebook.orca.extra.APPLICATION_ID"
  const val EXTRA_REPLY_TOKEN_KEY = "com.facebook.orca.extra.REPLY_TOKEN"
  const val EXTRA_THREAD_TOKEN_KEY = "com.facebook.orca.extra.THREAD_TOKEN"
  const val EXTRA_METADATA = "com.facebook.orca.extra.METADATA"
  const val EXTRA_EXTERNAL_URI = "com.facebook.orca.extra.EXTERNAL_URI"
  const val EXTRA_PARTICIPANTS = "com.facebook.orca.extra.PARTICIPANTS"
  const val EXTRA_IS_REPLY = "com.facebook.orca.extra.IS_REPLY"
  const val EXTRA_IS_COMPOSE = "com.facebook.orca.extra.IS_COMPOSE"
  const val PROTOCOL_VERSION_20150314 = 20150314
  const val ORCA_THREAD_CATEGORY_20150314 = "com.facebook.orca.category.PLATFORM_THREAD_20150314"

  /**
   * Starts an intent to share a piece of media on Messenger using the messenger content platform.
   *
   * @param activity the activity sharing the content
   * @param requestCode a unique request code for [Activity.startActivityForResult]
   * @param shareToMessengerParams parameters for what to share
   */
  @AutoHandleExceptions
  fun shareToMessenger(
      activity: Activity,
      requestCode: Int,
      shareToMessengerParams: ShareToMessengerParams
  ) {
    if (!hasMessengerInstalled(activity)) {
      openMessengerInPlayStore(activity)
      return
    }
    val allAvailableVersions = getAllAvailableProtocolVersions(activity)
    if (allAvailableVersions.contains(PROTOCOL_VERSION_20150314)) {
      shareToMessenger20150314(activity, requestCode, shareToMessengerParams)
    } else {
      // TODO -- should we show a upgrade dialog?
      openMessengerInPlayStore(activity)
    }
  }

  private fun shareToMessenger20150314(
      activity: Activity,
      requestCode: Int,
      shareToMessengerParams: ShareToMessengerParams
  ) {
    try {
      val shareIntent = Intent(Intent.ACTION_SEND)
      shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      shareIntent.setPackage(PACKAGE_NAME)
      shareIntent.putExtra(Intent.EXTRA_STREAM, shareToMessengerParams.uri)
      shareIntent.type = shareToMessengerParams.mimeType
      shareIntent.putExtra(EXTRA_PROTOCOL_VERSION, PROTOCOL_VERSION_20150314)
      shareIntent.putExtra(EXTRA_APP_ID, getApplicationId())
      shareIntent.putExtra(EXTRA_METADATA, shareToMessengerParams.metaData)
      shareIntent.putExtra(EXTRA_EXTERNAL_URI, shareToMessengerParams.externalUri)
      activity.startActivityForResult(shareIntent, requestCode)
    } catch (e: ActivityNotFoundException) {
      val openMessenger = activity.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
      activity.startActivity(openMessenger)
    }
  }

  /**
   * When handling an `Intent` from Messenger, call this to parse the parameters of the intent.
   *
   * @param intent the intent of the activity
   * @return a [MessengerThreadParams] or null if this intent wasn't recognized as a request from
   * Messenger to share.
   */
  @AutoHandleExceptions
  fun getMessengerThreadParamsForIntent(intent: Intent): MessengerThreadParams? {
    val categories = intent.categories ?: return null
    if (categories.contains(ORCA_THREAD_CATEGORY_20150314)) {
      val appLinkExtras = getAppLinkExtras(intent)
      val threadToken = appLinkExtras?.getString(EXTRA_THREAD_TOKEN_KEY)
      val metadata = appLinkExtras?.getString(EXTRA_METADATA)
      val participants = appLinkExtras?.getString(EXTRA_PARTICIPANTS)
      val isReply = appLinkExtras?.getBoolean(EXTRA_IS_REPLY)
      val isCompose = appLinkExtras?.getBoolean(EXTRA_IS_COMPOSE)
      val origin =
          if (isReply == true) {
            MessengerThreadParams.Origin.REPLY_FLOW
          } else if (isCompose == true) {
            MessengerThreadParams.Origin.COMPOSE_FLOW
          } else {
            MessengerThreadParams.Origin.UNKNOWN
          }
      if (threadToken != null && metadata != null) {
        return MessengerThreadParams(origin, threadToken, metadata, parseParticipants(participants))
      }
    }
    return null
  }

  /**
   * Finishes the activity and returns the media item the user picked to Messenger.
   *
   * @param activity the activity that received the original intent from Messenger
   * @param shareToMessengerParams parameters for what to share
   */
  fun finishShareToMessenger(activity: Activity, shareToMessengerParams: ShareToMessengerParams) {
    val originalIntent = activity.intent
    val categories = originalIntent.categories
    if (categories == null) {
      // This shouldn't happen.
      activity.setResult(Activity.RESULT_CANCELED, null)
      activity.finish()
      return
    }
    if (categories.contains(ORCA_THREAD_CATEGORY_20150314)) {
      val appLinkExtras = getAppLinkExtras(originalIntent)
      val resultIntent = Intent()
      if (appLinkExtras != null && categories.contains(ORCA_THREAD_CATEGORY_20150314)) {
        resultIntent.putExtra(EXTRA_PROTOCOL_VERSION, PROTOCOL_VERSION_20150314)
        val threadToken = appLinkExtras.getString(EXTRA_THREAD_TOKEN_KEY)
        resultIntent.putExtra(EXTRA_THREAD_TOKEN_KEY, threadToken)
      } else {
        throw RuntimeException() // Can't happen.
      }
      resultIntent.setDataAndType(shareToMessengerParams.uri, shareToMessengerParams.mimeType)
      resultIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      resultIntent.putExtra(EXTRA_APP_ID, getApplicationId())
      resultIntent.putExtra(EXTRA_METADATA, shareToMessengerParams.metaData)
      resultIntent.putExtra(EXTRA_EXTERNAL_URI, shareToMessengerParams.externalUri)
      activity.setResult(Activity.RESULT_OK, resultIntent)
      activity.finish()
    } else {
      // This shouldn't happen.
      activity.setResult(Activity.RESULT_CANCELED, null)
      activity.finish()
    }
  }

  /**
   * Checks whether any version of messenger is installed.
   *
   * @param context an android context
   * @return whether any version of messenger is installed
   */
  fun hasMessengerInstalled(context: Context): Boolean {
    return validateSignature(context, PACKAGE_NAME)
  }

  /**
   * Opens the play store to install Messenger.
   *
   * @param context an android context.
   */
  @AutoHandleExceptions
  fun openMessengerInPlayStore(context: Context) {
    try {
      startViewUri(context, "market://details?id=$PACKAGE_NAME")
    } catch (anfe: ActivityNotFoundException) {
      startViewUri(context, "http://play.google.com/store/apps/details?id=$PACKAGE_NAME")
    }
  }

  private fun getAllAvailableProtocolVersions(context: Context): Set<Int> {
    val contentResolver = context.contentResolver
    val allAvailableVersions: MutableSet<Int> = HashSet()
    val uri = Uri.parse("content://com.facebook.orca.provider.MessengerPlatformProvider/versions")
    val projection = arrayOf("version")
    val cursor = contentResolver.query(uri, projection, null, null, null)
    cursor?.use { c ->
      val versionColumnIndex = c.getColumnIndex("version")
      while (c.moveToNext()) {
        allAvailableVersions.add(c.getInt(versionColumnIndex))
      }
    }
    return allAvailableVersions
  }

  private fun startViewUri(context: Context, uri: String) =
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))

  private fun parseParticipants(s: String?): List<String> {
    if (s.isNullOrEmpty()) {
      return listOf()
    }
    val parts = s.split(",").toTypedArray()
    return parts.map { it.trim { c -> c <= ' ' } }
  }
}
