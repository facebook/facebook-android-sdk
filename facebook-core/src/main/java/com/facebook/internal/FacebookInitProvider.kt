/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.facebook.FacebookSdk
import java.lang.Exception

class FacebookInitProvider : ContentProvider() {
  companion object {
    private val TAG = FacebookInitProvider::class.java.simpleName
  }
  @SuppressWarnings("deprecation")
  override fun onCreate(): Boolean {
    try {
      val context = this.context
      requireNotNull(context)
      FacebookSdk.sdkInitialize(context)
    } catch (ex: Exception) {
      Log.i(TAG, "Failed to auto initialize the Facebook SDK", ex)
    }
    return false
  }

  override fun query(
      uri: Uri,
      projection: Array<String>?,
      selection: String?,
      selectionArgs: Array<String>?,
      sortOrder: String?
  ): Cursor? {
    return null
  }

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

  override fun update(
      uri: Uri,
      values: ContentValues?,
      selection: String?,
      selectionArgs: Array<String>?
  ): Int {
    return 0
  }
}
