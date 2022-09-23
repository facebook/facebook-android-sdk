/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class Logger(behavior: LoggingBehavior, tag: String) {
  private val behavior: LoggingBehavior
  private val tag: String
  private var contents: StringBuilder

  var priority = Log.DEBUG
    set(value: Int) {
      Validate.oneOf(
          value, "value", Log.ASSERT, Log.DEBUG, Log.ERROR, Log.INFO, Log.VERBOSE, Log.WARN)
      priority = value
    }

  fun getContents(): String {
    return replaceStrings(contents.toString())
  }

  // Writes the accumulated contents, then clears contents to start again.
  fun log() {
    logString(contents.toString())
    contents = StringBuilder()
  }

  // Immediately logs a string, ignoring any accumulated contents, which are left unchanged.
  fun logString(string: String) {
    log(behavior, priority, tag, string)
  }

  fun append(stringBuilder: StringBuilder) {
    if (shouldLog()) {
      contents.append(stringBuilder)
    }
  }

  fun append(string: String) {
    if (shouldLog()) {
      contents.append(string)
    }
  }

  fun append(format: String, vararg args: Any) {
    if (shouldLog()) {
      contents.append(String.format(format, *args))
    }
  }

  fun appendKeyValue(key: String, value: Any) {
    append("  %s:\t%s\n", key, value)
  }

  private fun shouldLog(): Boolean {
    return FacebookSdk.isLoggingBehaviorEnabled(behavior)
  }

  companion object {
    const val LOG_TAG_BASE = "FacebookSDK."
    private val stringsToReplace = HashMap<String, String>()

    // Note that the mapping of replaced strings is never emptied, so it should be used only for
    // things that are not expected to be too numerous, such as access tokens.
    @Synchronized
    @JvmStatic
    fun registerStringToReplace(original: String, replace: String) {
      stringsToReplace[original] = replace
    }

    @Synchronized
    @JvmStatic
    fun registerAccessToken(accessToken: String) {
      if (!FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.INCLUDE_ACCESS_TOKENS)) {
        registerStringToReplace(accessToken, "ACCESS_TOKEN_REMOVED")
      }
    }

    @JvmStatic
    fun log(behavior: LoggingBehavior, tag: String, string: String) {
      log(behavior, Log.DEBUG, tag, string)
    }

    @JvmStatic
    fun log(behavior: LoggingBehavior, tag: String, format: String, vararg args: Any) {
      if (FacebookSdk.isLoggingBehaviorEnabled(behavior)) {
        val string = String.format(format, *args)
        log(behavior, Log.DEBUG, tag, string)
      }
    }

    @JvmStatic
    fun log(
        behavior: LoggingBehavior,
        priority: Int,
        tag: String,
        format: String,
        vararg args: Any
    ) {
      if (FacebookSdk.isLoggingBehaviorEnabled(behavior)) {
        val string = String.format(format, *args)
        log(behavior, priority, tag, string)
      }
    }

    @JvmStatic
    fun log(behavior: LoggingBehavior, priority: Int, tag: String, string: String) {
      var tag = tag
      var string = string
      if (FacebookSdk.isLoggingBehaviorEnabled(behavior)) {
        string = replaceStrings(string)
        if (tag.startsWith(LOG_TAG_BASE) == false) {
          tag = LOG_TAG_BASE + tag
        }
        Log.println(priority, tag, string)

        // Developer errors warrant special treatment by printing out a stack trace, to make
        // both more noticeable, and let the source of the problem be more easily pinpointed.
        if (behavior == LoggingBehavior.DEVELOPER_ERRORS) {
          Exception().printStackTrace()
        }
      }
    }

    @Synchronized
    private fun replaceStrings(string: String): String {
      var string = string
      for ((key, value) in stringsToReplace) {
        string = string.replace(key, value)
      }
      return string
    }
  }

  init {
    this.behavior = behavior
    this.tag = LOG_TAG_BASE + Validate.notNullOrEmpty(tag, "tag")
    contents = StringBuilder()
  }
}
