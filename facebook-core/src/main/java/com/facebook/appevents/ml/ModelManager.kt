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

package com.facebook.appevents.ml

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.GraphRequest.Companion.newGraphPathRequest
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.integrity.IntegrityManager
import com.facebook.appevents.internal.FileDownloadTask
import com.facebook.appevents.ml.Utils.getMlDir
import com.facebook.appevents.suggestedevents.SuggestedEventsManager
import com.facebook.appevents.suggestedevents.ViewOnClickListener
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.resourceLocale
import com.facebook.internal.Utility.runOnNonUiThread
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.File
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY)
object ModelManager {
  enum class Task {
    MTML_INTEGRITY_DETECT,
    MTML_APP_EVENT_PREDICTION;

    fun toKey(): String {
      return when (this) {
        MTML_INTEGRITY_DETECT -> "integrity_detect"
        MTML_APP_EVENT_PREDICTION -> "app_event_pred"
      }
    }

    fun toUseCase(): String {
      return when (this) {
        MTML_INTEGRITY_DETECT -> "MTML_INTEGRITY_DETECT"
        MTML_APP_EVENT_PREDICTION -> "MTML_APP_EVENT_PRED"
      }
    }
  }

  private val taskHandlers: MutableMap<String?, TaskHandler> = ConcurrentHashMap()
  private const val SDK_MODEL_ASSET = "%s/model_asset"
  private const val MODEL_ASSERT_STORE = "com.facebook.internal.MODEL_STORE"
  private const val CACHE_KEY_MODELS = "models"
  private const val MTML_USE_CASE = "MTML"
  private const val USE_CASE_KEY = "use_case"
  private const val VERSION_ID_KEY = "version_id"
  private const val ASSET_URI_KEY = "asset_uri"
  private const val RULES_URI_KEY = "rules_uri"
  private const val THRESHOLD_KEY = "thresholds"
  private const val MODEL_REQUEST_INTERVAL_MILLISECONDS = 1000 * 60 * 60 * 24 * 3
  private const val CACHE_KEY_REQUEST_TIMESTAMP = "model_request_timestamp"

  @SuppressWarnings("deprecation")
  private val MTML_SUGGESTED_EVENTS_PREDICTION =
      listOf(
          ViewOnClickListener.OTHER_EVENT,
          AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
          AppEventsConstants.EVENT_NAME_ADDED_TO_CART,
          AppEventsConstants.EVENT_NAME_PURCHASED,
          AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT)

  private val MTML_INTEGRITY_DETECT_PREDICTION =
      listOf(
          IntegrityManager.INTEGRITY_TYPE_NONE,
          IntegrityManager.INTEGRITY_TYPE_ADDRESS,
          IntegrityManager.INTEGRITY_TYPE_HEALTH)

  @JvmStatic
  fun enable() {
    runOnNonUiThread(
        Runnable {
          try {
            val sharedPreferences =
                FacebookSdk.getApplicationContext()
                    .getSharedPreferences(MODEL_ASSERT_STORE, Context.MODE_PRIVATE)
            val cached = sharedPreferences.getString(CACHE_KEY_MODELS, null)
            var models: JSONObject =
                if (cached == null || cached.isEmpty()) JSONObject() else JSONObject(cached)
            val cachedTimestamp = sharedPreferences.getLong(CACHE_KEY_REQUEST_TIMESTAMP, 0)
            if (!isEnabled(FeatureManager.Feature.ModelRequest) ||
                models.length() == 0 ||
                !isValidTimestamp(cachedTimestamp)) {
              models = fetchModels() ?: return@Runnable
              sharedPreferences
                  .edit()
                  .putString(CACHE_KEY_MODELS, models.toString())
                  .putLong(CACHE_KEY_REQUEST_TIMESTAMP, System.currentTimeMillis())
                  .apply()
            }
            addModels(models)
            enableMTML()
          } catch (e: Exception) {
            /* no op*/
          }
        })
  }

  private fun isValidTimestamp(timestamp: Long): Boolean {
    return if (timestamp == 0L) {
      false
    } else System.currentTimeMillis() - timestamp < MODEL_REQUEST_INTERVAL_MILLISECONDS
  }

  private fun addModels(models: JSONObject) {
    val keys = models.keys()
    try {
      while (keys.hasNext()) {
        val key = keys.next()
        val handler = TaskHandler.build(models.getJSONObject(key)) ?: continue
        taskHandlers[handler.useCase] = handler
      }
    } catch (je: JSONException) {
      /* no op*/
    }
  }

  private fun parseRawJsonObject(jsonObject: JSONObject): JSONObject {
    val resultJsonObject = JSONObject()
    return try {
      val jsonArray = jsonObject.getJSONArray("data")
      for (i in 0 until jsonArray.length()) {
        val curJsonObject = jsonArray.getJSONObject(i)
        val tempJsonObject = JSONObject()
        tempJsonObject.put(VERSION_ID_KEY, curJsonObject.getString(VERSION_ID_KEY))
        tempJsonObject.put(USE_CASE_KEY, curJsonObject.getString(USE_CASE_KEY))
        tempJsonObject.put(THRESHOLD_KEY, curJsonObject.getJSONArray(THRESHOLD_KEY))
        tempJsonObject.put(ASSET_URI_KEY, curJsonObject.getString(ASSET_URI_KEY))
        // rule_uri is optional
        if (curJsonObject.has(RULES_URI_KEY)) {
          tempJsonObject.put(RULES_URI_KEY, curJsonObject.getString(RULES_URI_KEY))
        }
        resultJsonObject.put(curJsonObject.getString(USE_CASE_KEY), tempJsonObject)
      }
      resultJsonObject
    } catch (je: JSONException) {
      JSONObject()
    }
  }

  private fun fetchModels(): JSONObject? {
    val appSettingFields =
        arrayOf(USE_CASE_KEY, VERSION_ID_KEY, ASSET_URI_KEY, RULES_URI_KEY, THRESHOLD_KEY)
    val appSettingsParams = Bundle()
    appSettingsParams.putString("fields", TextUtils.join(",", appSettingFields))
    val rawResponse =
        if (isNullOrEmpty(FacebookSdk.getClientToken())) {
          val graphRequest =
              newGraphPathRequest(
                  null, String.format(SDK_MODEL_ASSET, FacebookSdk.getApplicationId()), null)
          graphRequest.setSkipClientToken(true)
          graphRequest.parameters = appSettingsParams
          graphRequest.executeAndWait().getJSONObject() ?: return null
        } else {
          val graphRequest = newGraphPathRequest(null, "app/model_asset", null)
          graphRequest.parameters = appSettingsParams
          graphRequest.executeAndWait().getJSONObject() ?: return null
        }
    return parseRawJsonObject(rawResponse)
  }

  private fun enableMTML() {
    val slaveTasks: MutableList<TaskHandler> = ArrayList()
    var mtmlAssetUri: String? = null
    var mtmlVersionId = 0
    for ((useCase, handler) in taskHandlers) {
      if (useCase == Task.MTML_APP_EVENT_PREDICTION.toUseCase()) {
        mtmlAssetUri = handler.assetUri
        mtmlVersionId = Math.max(mtmlVersionId, handler.versionId)
        if (isEnabled(FeatureManager.Feature.SuggestedEvents) && isLocaleEnglish) {
          slaveTasks.add(handler.setOnPostExecute { SuggestedEventsManager.enable() })
        }
      }
      if (useCase == Task.MTML_INTEGRITY_DETECT.toUseCase()) {
        mtmlAssetUri = handler.assetUri
        mtmlVersionId = Math.max(mtmlVersionId, handler.versionId)
        if (isEnabled(FeatureManager.Feature.IntelligentIntegrity)) {
          slaveTasks.add(handler.setOnPostExecute { IntegrityManager.enable() })
        }
      }
    }
    if (mtmlAssetUri != null && mtmlVersionId > 0 && !slaveTasks.isEmpty()) {
      val mtmlHandler = TaskHandler(MTML_USE_CASE, mtmlAssetUri, null, mtmlVersionId, null)
      TaskHandler.execute(mtmlHandler, slaveTasks)
    }
  }

  private val isLocaleEnglish: Boolean
    private get() {
      val locale = resourceLocale
      return locale == null || locale.language.contains("en")
    }

  private fun parseJsonArray(jsonArray: JSONArray?): FloatArray? {
    if (jsonArray == null) {
      return null
    }
    val thresholds = FloatArray(jsonArray.length())
    for (i in 0 until jsonArray.length()) {
      try {
        thresholds[i] = jsonArray.getString(i).toFloat()
      } catch (e: JSONException) {
        /*no op*/
      }
    }
    return thresholds
  }

  @JvmStatic
  fun getRuleFile(task: Task): File? {
    val handler = taskHandlers[task.toUseCase()] ?: return null
    return handler.ruleFile
  }

  @JvmStatic
  fun predict(task: Task, denses: Array<FloatArray>, texts: Array<String>): Array<String>? {
    val handler = taskHandlers[task.toUseCase()]
    val model = handler?.model ?: return null
    val thresholds = handler.thresholds

    val exampleSize = texts.size
    val denseSize: Int = denses[0].size
    val dense = MTensor(intArrayOf(exampleSize, denseSize))
    for (n in 0 until exampleSize) {
      System.arraycopy(denses[n], 0, dense.data, n * denseSize, denseSize)
    }
    val res = model.predictOnMTML(dense, texts, task.toKey())
    return if (res == null || thresholds == null || res.data.isEmpty() || thresholds.isEmpty()) {
      null
    } else
        when (task) {
          Task.MTML_APP_EVENT_PREDICTION -> processSuggestedEventResult(res, thresholds)
          Task.MTML_INTEGRITY_DETECT -> processIntegrityDetectionResult(res, thresholds)
        }
  }

  private fun processSuggestedEventResult(res: MTensor, thresholds: FloatArray): Array<String>? {
    val exampleSize = res.getShape(0)
    val resSize = res.getShape(1)
    val resData = res.data
    if (resSize != thresholds.size) {
      return null
    }
    return (0 until exampleSize)
        .map {
          var resultItem = ViewOnClickListener.OTHER_EVENT
          thresholds.forEachIndexed { idx, threshold ->
            if (resData[it * resSize + idx] >= threshold) {
              resultItem = MTML_SUGGESTED_EVENTS_PREDICTION[idx]
            }
          }
          return@map resultItem
        }
        .toTypedArray()
  }

  private fun processIntegrityDetectionResult(
      res: MTensor,
      thresholds: FloatArray
  ): Array<String>? {
    val exampleSize = res.getShape(0)
    val resSize = res.getShape(1)
    val resData = res.data
    if (resSize != thresholds.size) {
      return null
    }
    return (0 until exampleSize)
        .map {
          var resultItem = IntegrityManager.INTEGRITY_TYPE_NONE
          thresholds.forEachIndexed { idx, threshold ->
            if (resData[it * resSize + idx] >= threshold) {
              resultItem = MTML_INTEGRITY_DETECT_PREDICTION[idx]
            }
          }
          return@map resultItem
        }
        .toTypedArray()
  }

  private class TaskHandler
  constructor(
      var useCase: String,
      var assetUri: String,
      var ruleUri: String?,
      var versionId: Int,
      var thresholds: FloatArray?
  ) {
    var ruleFile: File? = null
    var model: Model? = null
    private var onPostExecute: Runnable? = null
    fun setOnPostExecute(onPostExecute: Runnable?): TaskHandler {
      this.onPostExecute = onPostExecute
      return this
    }

    companion object {
      fun build(json: JSONObject?): TaskHandler? {
        return if (json == null) {
          null
        } else {
          try {
            val useCase = json.getString(USE_CASE_KEY)
            val assetUri = json.getString(ASSET_URI_KEY)
            val ruleUri = json.optString(RULES_URI_KEY, null)
            val versionId = json.getInt(VERSION_ID_KEY)
            val thresholds = parseJsonArray(json.getJSONArray(THRESHOLD_KEY))
            TaskHandler(useCase, assetUri, ruleUri, versionId, thresholds)
          } catch (e: Exception) {
            null
          }
        }
      }

      fun execute(handler: TaskHandler) {
        execute(handler, listOf(handler))
      }

      fun execute(master: TaskHandler, slaves: List<TaskHandler>) {
        deleteOldFiles(master.useCase, master.versionId)
        val modelFileName = master.useCase + "_" + master.versionId
        download(master.assetUri, modelFileName) { file ->
          val model = Model.build(file)
          if (model != null) {
            for (slave in slaves) {
              val ruleFileName = slave.useCase + "_" + slave.versionId + "_rule"
              download(slave.ruleUri, ruleFileName) { file ->
                slave.model = model
                slave.ruleFile = file
                slave.onPostExecute?.run()
              }
            }
          }
        }
      }

      private fun deleteOldFiles(useCase: String, versionId: Int) {
        val dir = getMlDir() ?: return
        val existingFiles = dir.listFiles()
        if (existingFiles == null || existingFiles.isEmpty()) {
          return
        }
        val prefixWithVersion = useCase + "_" + versionId
        for (f in existingFiles) {
          val name = f.name
          if (name.startsWith(useCase) && !name.startsWith(prefixWithVersion)) {
            f.delete()
          }
        }
      }

      private fun download(uri: String?, name: String, onComplete: FileDownloadTask.Callback) {
        val file = File(getMlDir(), name)
        if (uri == null || file.exists()) {
          onComplete.onComplete(file)
          return
        }
        FileDownloadTask(uri, file, onComplete).execute()
      }
    }
  }
}
