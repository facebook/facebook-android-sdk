/*
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

package com.facebook.appevents.cloudbridge

import com.facebook.GraphRequest
import com.facebook.LoggingBehavior
import com.facebook.internal.Logger
import com.facebook.internal.Utility.convertJSONObjectToHashMap
import com.facebook.internal.Utility.runOnNonUiThread
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONObject

object AppEventsConversionsAPITransformerWebRequests {

  data class CloudBridgeCredentials(
      val datasetID: String,
      val cloudBridgeURL: String,
      val accessKey: String
  )

  // Note we do not use canonical name due to Kotlin logger tag length limit.
  // Disabling limit has risks
  // https://stackoverflow.com/questions/28168622/the-logging-tag-can-be-at-most-23-characters
  private const val TAG = "CAPITransformerWebRequests"
  internal const val MAX_CACHED_TRANSFORMED_EVENTS = 1000
  private const val MAX_PROCESSED_TRANSFORMED_EVENTS = 10
  private const val TIMEOUT_INTERVAL = 60000
  private val ACCEPTABLE_HTTP_RESPONSE =
      hashSetOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_ACCEPTED)
  private val RETRY_EVENTS_HTTP_RESPONSE =
      hashSetOf(HttpURLConnection.HTTP_UNAVAILABLE, HttpURLConnection.HTTP_GATEWAY_TIMEOUT, 429)
  // 429 too many requests

  internal lateinit var credentials: CloudBridgeCredentials
  internal lateinit var transformedEvents: MutableList<Map<String, Any>>

  internal const val MAX_RETRY_COUNT = 5
  internal var currentRetryCount: Int = 0

  @JvmStatic
  /**
   * Set the CAPI G Endpoint. Can be used to override
   * @property datasetID CAPI G datasetID
   * @property url CAPI G server url
   * @property accessKey CAPI G secret access key
   */
  fun configure(datasetID: String, url: String, accessKey: String) {
    Logger.log(
        LoggingBehavior.APP_EVENTS,
        TAG,
        " \n\nCloudbridge Configured: \n================\ndatasetID: %s\nurl: %s\naccessKey: %s\n\n",
        datasetID,
        url,
        accessKey)

    credentials = CloudBridgeCredentials(datasetID, url, accessKey)
    transformedEvents = mutableListOf()
  }

  @JvmStatic
  /**
   * Take a graph request, extract the App Events within it and transform them. Then send it to the
   * cb endpoint.
   *
   * @property request GraphRequest to convert and send to CAPI G
   */
  fun transformGraphRequestAndSendToCAPIGEndPoint(request: GraphRequest) {

    runOnNonUiThread {
      val graphPathComponents = request.graphPath?.split("/")
      if (graphPathComponents == null || graphPathComponents.size != 2) {
        Logger.log(
            LoggingBehavior.DEVELOPER_ERRORS,
            TAG,
            "\n GraphPathComponents Error when logging: \n%s",
            request)
        return@runOnNonUiThread
      }
      val cbEndpoint: String
      try {

        val cloudBridgeURL = this.credentials.cloudBridgeURL
        val datasetID = this.credentials.datasetID

        cbEndpoint = "$cloudBridgeURL/capi/$datasetID/events"
      } catch (e: UninitializedPropertyAccessException) {
        Logger.log(
            LoggingBehavior.DEVELOPER_ERRORS,
            TAG,
            "\n Credentials not initialized Error when logging: \n%s",
            e)

        return@runOnNonUiThread
      }
      val transformed = transformAppEventRequestForCAPIG(request) ?: return@runOnNonUiThread
      appendEvents(transformed)
      val eventToProcessCount = min(transformedEvents.count(), MAX_PROCESSED_TRANSFORMED_EVENTS)

      // create copy of the events we are going to send off
      val processedEvents = transformedEvents.slice(IntRange(0, eventToProcessCount - 1))
      transformedEvents.subList(0, eventToProcessCount).clear()

      val jsonArr = JSONArray(processedEvents)
      val jsonMap = mutableMapOf<String, Any?>()
      jsonMap["data"] = jsonArr
      jsonMap["accessKey"] = this.credentials.accessKey
      val jsonBodyStr = JSONObject(jsonMap)

      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "\nTransformed_CAPI_JSON:\n" +
              "URL: %s\n" +
              "FROM=========\n" +
              "%s\n" +
              ">>>>>>TO>>>>>>\n" +
              "%s\n" +
              "=============\n",
          cbEndpoint,
          request,
          jsonBodyStr.toString(2))

      makeHttpRequest(
          cbEndpoint,
          "POST",
          jsonBodyStr.toString(),
          mapOf("Content-Type" to "application/json"),
          TIMEOUT_INTERVAL,
      ) { _: String?, responseCode: Int? ->
        runOnNonUiThread {
          if (!ACCEPTABLE_HTTP_RESPONSE.contains(responseCode)) {
            handleError(responseCode, processedEvents, MAX_RETRY_COUNT)
          }
        }
      }
    }
  }

  private fun transformAppEventRequestForCAPIG(request: GraphRequest): List<Map<String, Any>>? {
    // tag is custom data
    // graphRequest is the common data
    val graphRequestObjJSON = request.graphObject

    if (graphRequestObjJSON != null) {
      val requestData = convertJSONObjectToHashMap(graphRequestObjJSON).toMutableMap()

      // add the custom event data in
      requestData["custom_events"] = request.tag as Any

      val sb = StringBuilder()
      for (key in requestData.keys) {
        sb.append(key)
        sb.append(" : ")
        sb.append(requestData[key])
        sb.append(System.getProperty("line.separator"))
      }
      Logger.log(LoggingBehavior.APP_EVENTS, TAG, "\nGraph Request data: \n\n%s \n\n", sb)

      return AppEventsConversionsAPITransformer.conversionsAPICompatibleEvent(requestData)
    }

    return null
  }

  internal fun handleError(
      responseCode: Int?,
      processedEvents: List<Map<String, Any>>,
      maxRetryCount: Int = 5
  ) {

    // We received an error, but it's not server error.
    // we'll re-append the events we processed to the event queue
    if (RETRY_EVENTS_HTTP_RESPONSE.contains(responseCode)) {

      if (this.currentRetryCount >= maxRetryCount) {
        transformedEvents.clear()
        this.currentRetryCount = 0
      } else {
        transformedEvents.addAll(0, processedEvents)
        this.currentRetryCount++
      }
    }
  }

  internal fun appendEvents(events: List<Map<String, Any>>?) {
    if (events != null) {
      transformedEvents.addAll(events)
    }
    val overCacheLimitCount = max(0, transformedEvents.count() - MAX_CACHED_TRANSFORMED_EVENTS)
    if (overCacheLimitCount > 0) {
      transformedEvents =
          transformedEvents.drop(overCacheLimitCount) as MutableList<Map<String, Any>>
    }
  }

  internal fun makeHttpRequest(
      urlStr: String,
      requestMethod: String,
      jsonBodyStr: String?,
      requestProperties: Map<String, String>?,
      timeOutInterval: Int = 60000,
      requestCallback: ((requestResult: String?, responseCode: Int?) -> Unit)?
  ) {
    try {

      val url = URL(urlStr)
      val httpURLConnection = url.openConnection() as HttpURLConnection
      httpURLConnection.requestMethod = requestMethod
      requestProperties?.keys?.forEach { key ->
        httpURLConnection.setRequestProperty(key, requestProperties[key])
      }

      httpURLConnection.doOutput =
          httpURLConnection.requestMethod.equals("POST") ||
              httpURLConnection.requestMethod.equals("PUT")
      httpURLConnection.connectTimeout = timeOutInterval

      val output: OutputStream = BufferedOutputStream(httpURLConnection.outputStream)
      val writer = BufferedWriter(OutputStreamWriter(output, "UTF-8"))
      writer.write(jsonBodyStr)
      writer.flush()
      writer.close()
      output.close()

      val connResponseSB = StringBuilder()
      if (ACCEPTABLE_HTTP_RESPONSE.contains(httpURLConnection.responseCode)) {
        BufferedReader(InputStreamReader(httpURLConnection.inputStream, "UTF-8")).use {
            bufferedReader ->
          var line: String?
          while (bufferedReader.readLine().also { line = it } != null) {
            connResponseSB.append(line)
          }
        }
      }

      val connResponse = connResponseSB.toString()
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "\nResponse Received: \n%s\n%s",
          connResponse,
          httpURLConnection.responseCode)

      if (requestCallback !== null) {
        requestCallback(connResponse, httpURLConnection.responseCode)
      }
    } catch (e: java.net.UnknownHostException) {
      // referencing HTTPURLConnection and having no internet connection
      // https://stackoverflow.com/questions/20361567/httpurlconnection-and-no-internet

      Logger.log(LoggingBehavior.APP_EVENTS, TAG, "Connection failed, retrying: \n%s", e.toString())

      if (requestCallback !== null) {
        requestCallback(null, HttpURLConnection.HTTP_UNAVAILABLE)
      }
    } catch (e: java.io.IOException) {
      Logger.log(LoggingBehavior.DEVELOPER_ERRORS, TAG, "Send to server failed: \n%s", e.toString())
    }
  }
}
