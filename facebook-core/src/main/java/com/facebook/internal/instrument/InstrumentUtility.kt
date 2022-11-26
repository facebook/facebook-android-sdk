/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument

import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.internal.Utility
import com.facebook.internal.Utility.readStreamToString
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InstrumentUtility {
  const val ANALYSIS_REPORT_PREFIX = "analysis_log_"
  const val ANR_REPORT_PREFIX = "anr_log_"
  const val CRASH_REPORT_PREFIX = "crash_log_"
  const val CRASH_SHIELD_PREFIX = "shield_log_"
  const val THREAD_CHECK_PREFIX = "thread_check_log_"
  const val ERROR_REPORT_PREFIX = "error_log_"
  private const val FBSDK_PREFIX = "com.facebook"
  private const val CODELESS_PREFIX = "com.facebook.appevents.codeless"
  private const val SUGGESTED_EVENTS_PREFIX = "com.facebook.appevents.suggestedevents"
  private const val INSTRUMENT_DIR = "instrument"

  /**
   * Get the cause of the raised exception.
   *
   * @param e The Throwable containing the exception that was raised
   * @return The String containing the cause of the raised exception
   */
  @JvmStatic
  fun getCause(e: Throwable?): String? {
    if (e == null) {
      return null
    }
    return if (e.cause == null) {
      e.toString()
    } else e.cause.toString()
  }

  /**
   * Get the iterated call stack traces of the raised exception.
   *
   * @param e The Throwable containing the exception that was raised
   * @return The String containing the stack traces of the raised exception
   */
  @JvmStatic
  fun getStackTrace(e: Throwable?): String? {
    if (e == null) {
      return null
    }

    // Iterate on causes recursively
    val array = JSONArray()
    var previous: Throwable? = null // Prevent infinite loops
    var t = e
    while (t != null && t !== previous) {
      for (element in t.stackTrace) {
        array.put(element.toString())
      }
      previous = t
      t = t.cause
    }
    return array.toString()
  }

  /**
   * Get the stack trace of the input Thread.
   *
   * @param thread The Thread to obtain the stack trace
   * @return The String containing the stack traces of the raised exception
   */
  @JvmStatic
  fun getStackTrace(thread: Thread): String? {
    val stackTrace = thread.stackTrace
    val array = JSONArray()
    for (element in stackTrace) {
      array.put(element.toString())
    }
    return array.toString()
  }

  /**
   * Check whether a Throwable is related to Facebook SDK by looking at iterated stack traces and
   * return true if one of the traces has prefix "com.facebook".
   *
   * @param e The Throwable containing the exception that was raised
   * @return Whether the raised exception is related to Facebook SDK
   */
  @JvmStatic
  fun isSDKRelatedException(e: Throwable?): Boolean {
    if (e == null) {
      return false
    }

    // Iterate on causes recursively
    var previous: Throwable? = null // Prevent infinite loops
    var t = e
    while (t != null && t !== previous) {
      for (element in t.stackTrace) {
        if (element.className.startsWith(FBSDK_PREFIX)) {
          return true
        }
      }
      previous = t
      t = t.cause
    }
    return false
  }

  /**
   * Check whether an Thread is related to Facebook SDK by looking at iterated stack traces
   *
   * @param thread The Thread to obtain the stack trace
   * @return Whether the thread is related to Facebook SDK
   */
  @JvmStatic
  fun isSDKRelatedThread(thread: Thread?): Boolean {

    // Iterate on thread's stack traces
    thread?.stackTrace?.forEach { element ->
      if (element.className.startsWith(FBSDK_PREFIX)) {

        // Ignore the ANR caused by calling app itself's click listener or touch listener
        if (element.className.startsWith(CODELESS_PREFIX) ||
            element.className.startsWith(SUGGESTED_EVENTS_PREFIX)) {
          if (element.methodName.startsWith("onClick") ||
              element.methodName.startsWith("onItemClick") ||
              element.methodName.startsWith("onTouch")) {
            return@forEach
          }
        }

        return true
      }
    }

    return false
  }

  /**
   * Get the list of anr report files from instrument report directory defined in
   * [InstrumentUtility.getInstrumentReportDir] method.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   *
   * @return The list of anr files
   */
  @JvmStatic
  fun listAnrReportFiles(): Array<File> {
    val reportDir = getInstrumentReportDir() ?: return arrayOf()
    val reports =
        reportDir.listFiles { _, name ->
          name.matches(String.format("^%s[0-9]+.json$", ANR_REPORT_PREFIX).toRegex())
        }
    return reports ?: arrayOf()
  }

  /**
   * Get the list of exception analysis report files from instrument report directory defined in
   * [InstrumentUtility.getInstrumentReportDir] method.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   *
   * @return The list of exception analysis report files
   */
  @JvmStatic
  fun listExceptionAnalysisReportFiles(): Array<File> {
    val reportDir = getInstrumentReportDir() ?: return arrayOf()
    val reports =
        reportDir.listFiles { _, name ->
          name.matches(String.format("^%s[0-9]+.json$", ANALYSIS_REPORT_PREFIX).toRegex())
        }
    return reports ?: arrayOf()
  }

  /**
   * Get the list of exception report files from instrument report directory defined in [ ]
   * [InstrumentUtility.getInstrumentReportDir] method.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   *
   * @return The list of crash report files
   */
  @JvmStatic
  fun listExceptionReportFiles(): Array<File> {
    val reportDir = getInstrumentReportDir() ?: return arrayOf()
    val reports =
        reportDir.listFiles { _, name ->
          name.matches(
              String.format(
                      "^(%s|%s|%s)[0-9]+.json$",
                      CRASH_REPORT_PREFIX,
                      CRASH_SHIELD_PREFIX,
                      THREAD_CHECK_PREFIX)
                  .toRegex())
        }
    return reports ?: arrayOf()
  }

  /**
   * Read the content from the file which is denoted by filename and the directory is the instrument
   * report directory defined in [InstrumentUtility.getInstrumentReportDir] method.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   */
  @JvmStatic
  fun readFile(filename: String?, deleteOnException: Boolean): JSONObject? {
    val reportDir = getInstrumentReportDir()
    if (reportDir == null || filename == null) {
      return null
    }
    val file = File(reportDir, filename)
    val inputStream: FileInputStream
    try {
      inputStream = FileInputStream(file)
      val content = readStreamToString(inputStream)
      return JSONObject(content)
    } catch (e: Exception) {
      if (deleteOnException) {
        deleteFile(filename)
      }
    }
    return null
  }

  /**
   * Write the content to the file which is denoted by filename and the file will be put in
   * instrument report directory defined in [InstrumentUtility.getInstrumentReportDir] method.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   */
  @JvmStatic
  fun writeFile(filename: String?, content: String?) {
    val reportDir = getInstrumentReportDir()
    if (reportDir == null || filename == null || content == null) {
      return
    }
    val file = File(reportDir, filename)
    val outputStream: FileOutputStream
    try {
      outputStream = FileOutputStream(file)
      outputStream.write(content.toByteArray())
      outputStream.close()
    } catch (e: Exception) {
      /* no op */
    }
  }

  /**
   * Deletes the cache file under instrument report directory. If the instrument report directory
   * exists and the file exists under the directory, the file will be deleted.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   *
   * @return Whether the file is successfully deleted
   */
  @JvmStatic
  fun deleteFile(filename: String?): Boolean {
    val reportDir = getInstrumentReportDir()
    if (reportDir == null || filename == null) {
      return false
    }
    val file = File(reportDir, filename)
    return file.delete()
  }

  /** Create Graph Request for Instrument reports and send the reports to Facebook. */
  @JvmStatic
  fun sendReports(key: String?, reports: JSONArray, callback: GraphRequest.Callback?) {
    if (reports.length() == 0) {
      return
    }
    val params = JSONObject()
    try {
      params.put(key, reports.toString())
      val dataProcessingOptions = Utility.dataProcessingOptions
      if (dataProcessingOptions != null) {
        val it = dataProcessingOptions.keys()
        while (it.hasNext()) {
          val k = it.next()
          params.put(k, dataProcessingOptions[k])
        }
      }
    } catch (e: JSONException) {
      return
    }
    val request =
        GraphRequest.newPostRequest(
            null,
            String.format("%s" + "/instruments", FacebookSdk.getApplicationId()),
            params,
            callback)
    request.executeAsync()
  }

  /**
   * Get the instrument directory for report if the directory exists. If the directory doesn't
   * exist, will attempt to create the directory. Note that, the instrument directory is under cache
   * directory of the Application.
   *
   * Note that the function should be called after FacebookSdk is initialized. Otherwise, exception
   * FacebookSdkNotInitializedException will be thrown.
   *
   * @return The instrument cache directory if and only if the directory exists or it's successfully
   * created, otherwise return null.
   */
  @JvmStatic
  fun getInstrumentReportDir(): File? {
    val cacheDir = FacebookSdk.getApplicationContext().cacheDir
    val dir = File(cacheDir, INSTRUMENT_DIR)
    return if (dir.exists() || dir.mkdirs()) {
      dir
    } else null
  }
}
