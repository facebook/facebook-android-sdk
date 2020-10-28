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

package com.facebook.appevents.ml;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.integrity.IntegrityManager;
import com.facebook.appevents.internal.FileDownloadTask;
import com.facebook.appevents.suggestedevents.SuggestedEventsManager;
import com.facebook.appevents.suggestedevents.ViewOnClickListener;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ModelManager {

  public enum Task {
    MTML_INTEGRITY_DETECT,
    MTML_APP_EVENT_PREDICTION;

    public String toKey() {
      switch (this) {
        case MTML_INTEGRITY_DETECT:
          return "integrity_detect";
        case MTML_APP_EVENT_PREDICTION:
          return "app_event_pred";
      }
      return "Unknown";
    }

    @Nullable
    public String toUseCase() {
      switch (this) {
        case MTML_INTEGRITY_DETECT:
          return "MTML_INTEGRITY_DETECT";
        case MTML_APP_EVENT_PREDICTION:
          return "MTML_APP_EVENT_PRED";
      }
      return null;
    }
  }

  private static final Map<String, TaskHandler> mTaskHandlers = new ConcurrentHashMap<>();

  private static final String SDK_MODEL_ASSET = "%s/model_asset";
  private static final String MODEL_ASSERT_STORE = "com.facebook.internal.MODEL_STORE";
  private static final String CACHE_KEY_MODELS = "models";

  private static final String MTML_USE_CASE = "MTML";
  private static final String USE_CASE_KEY = "use_case";
  private static final String VERSION_ID_KEY = "version_id";
  private static final String ASSET_URI_KEY = "asset_uri";
  private static final String RULES_URI_KEY = "rules_uri";
  private static final String THRESHOLD_KEY = "thresholds";

  private static final Integer MODEL_REQUEST_INTERVAL_MILLISECONDS = 1000 * 60 * 60 * 24 * 3;
  private static final String CACHE_KEY_REQUEST_TIMESTAMP = "model_request_timestamp";

  @SuppressWarnings("deprecation")
  private static final List<String> MTML_SUGGESTED_EVENTS_PREDICTION =
      Arrays.asList(
          ViewOnClickListener.OTHER_EVENT,
          AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
          AppEventsConstants.EVENT_NAME_ADDED_TO_CART,
          AppEventsConstants.EVENT_NAME_PURCHASED,
          AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT);

  private static final List<String> MTML_INTEGRITY_DETECT_PREDICTION =
      Arrays.asList(
          IntegrityManager.INTEGRITY_TYPE_NONE,
          IntegrityManager.INTEGRITY_TYPE_ADDRESS,
          IntegrityManager.INTEGRITY_TYPE_HEALTH);

  public static void enable() {
    Utility.runOnNonUiThread(
        new Runnable() {
          @Override
          public void run() {
            try {
              final SharedPreferences sharedPreferences =
                  FacebookSdk.getApplicationContext()
                      .getSharedPreferences(MODEL_ASSERT_STORE, Context.MODE_PRIVATE);
              String cached = sharedPreferences.getString(CACHE_KEY_MODELS, null);
              JSONObject models =
                  (cached == null || cached.isEmpty()) ? new JSONObject() : new JSONObject(cached);
              long cachedTimestamp = sharedPreferences.getLong(CACHE_KEY_REQUEST_TIMESTAMP, 0);

              if (!FeatureManager.isEnabled(FeatureManager.Feature.ModelRequest)
                  || models.length() == 0
                  || !isValidTimestamp(cachedTimestamp)) {
                models = fetchModels();
                if (models == null) {
                  return;
                }
                sharedPreferences
                    .edit()
                    .putString(CACHE_KEY_MODELS, models.toString())
                    .putLong(CACHE_KEY_REQUEST_TIMESTAMP, System.currentTimeMillis())
                    .apply();
              }

              addModels(models);
              enableMTML();
            } catch (Exception e) {
              /* no op*/
            }
          }
        });
  }

  private static boolean isValidTimestamp(long timestamp) {
    if (timestamp == 0) {
      return false;
    }
    return System.currentTimeMillis() - timestamp < MODEL_REQUEST_INTERVAL_MILLISECONDS;
  }

  private static void addModels(JSONObject models) {
    Iterator<String> keys = models.keys();
    try {
      while (keys.hasNext()) {
        String key = keys.next();
        TaskHandler handler = TaskHandler.build(models.getJSONObject(key));
        if (handler == null) {
          continue;
        }
        mTaskHandlers.put(handler.useCase, handler);
      }
    } catch (JSONException je) {
      /* no op*/
    }
  }

  private static JSONObject parseRawJsonObject(JSONObject jsonObject) {
    JSONObject resultJsonObject = new JSONObject();
    try {
      JSONArray jsonArray = jsonObject.getJSONArray("data");
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject curJsonObject = jsonArray.getJSONObject(i);
        JSONObject tempJsonObject = new JSONObject();
        tempJsonObject.put(VERSION_ID_KEY, curJsonObject.getString(VERSION_ID_KEY));
        tempJsonObject.put(USE_CASE_KEY, curJsonObject.getString(USE_CASE_KEY));
        tempJsonObject.put(THRESHOLD_KEY, curJsonObject.getJSONArray(THRESHOLD_KEY));
        tempJsonObject.put(ASSET_URI_KEY, curJsonObject.getString(ASSET_URI_KEY));
        // rule_uri is optional
        if (curJsonObject.has(RULES_URI_KEY)) {
          tempJsonObject.put(RULES_URI_KEY, curJsonObject.getString(RULES_URI_KEY));
        }
        resultJsonObject.put(curJsonObject.getString(USE_CASE_KEY), tempJsonObject);
      }
      return resultJsonObject;
    } catch (JSONException je) {
      return new JSONObject();
    }
  }

  @Nullable
  private static JSONObject fetchModels() {
    String[] appSettingFields =
        new String[] {
          USE_CASE_KEY, VERSION_ID_KEY, ASSET_URI_KEY, RULES_URI_KEY, THRESHOLD_KEY,
        };
    Bundle appSettingsParams = new Bundle();

    appSettingsParams.putString("fields", TextUtils.join(",", appSettingFields));
    GraphRequest graphRequest =
        GraphRequest.newGraphPathRequest(
            null, String.format(SDK_MODEL_ASSET, FacebookSdk.getApplicationId()), null);
    graphRequest.setSkipClientToken(true);
    graphRequest.setParameters(appSettingsParams);
    JSONObject rawResponse = graphRequest.executeAndWait().getJSONObject();
    if (rawResponse == null) {
      return null;
    }
    return parseRawJsonObject(rawResponse);
  }

  private static void enableMTML() {
    List<TaskHandler> slaveTasks = new ArrayList<>();
    String mtmlAssetUri = null;
    int mtmlVersionId = 0;
    for (Map.Entry<String, TaskHandler> entry : mTaskHandlers.entrySet()) {
      String useCase = entry.getKey();
      if (useCase.equals(Task.MTML_APP_EVENT_PREDICTION.toUseCase())) {
        TaskHandler handler = entry.getValue();
        mtmlAssetUri = handler.assetUri;
        mtmlVersionId = Math.max(mtmlVersionId, handler.versionId);
        if (FeatureManager.isEnabled(FeatureManager.Feature.SuggestedEvents) && isLocaleEnglish()) {
          slaveTasks.add(
              handler.setOnPostExecute(
                  new Runnable() {
                    @Override
                    public void run() {
                      SuggestedEventsManager.enable();
                    }
                  }));
        }
      }
      if (useCase.equals(Task.MTML_INTEGRITY_DETECT.toUseCase())) {
        TaskHandler handler = entry.getValue();
        mtmlAssetUri = handler.assetUri;
        mtmlVersionId = Math.max(mtmlVersionId, handler.versionId);
        if (FeatureManager.isEnabled(FeatureManager.Feature.IntelligentIntegrity)) {
          slaveTasks.add(
              handler.setOnPostExecute(
                  new Runnable() {
                    @Override
                    public void run() {
                      IntegrityManager.enable();
                    }
                  }));
        }
      }
    }

    if (mtmlAssetUri != null && mtmlVersionId > 0 && !slaveTasks.isEmpty()) {
      TaskHandler mtmlHandler =
          new TaskHandler(MTML_USE_CASE, mtmlAssetUri, null, mtmlVersionId, null);
      TaskHandler.execute(mtmlHandler, slaveTasks);
    }
  }

  private static boolean isLocaleEnglish() {
    Locale locale = Utility.getResourceLocale();
    return locale == null || locale.getLanguage().contains("en");
  }

  @Nullable
  private static float[] parseJsonArray(@Nullable JSONArray jsonArray) {
    if (jsonArray == null) {
      return null;
    }
    float[] thresholds = new float[jsonArray.length()];
    for (int i = 0; i < jsonArray.length(); i++) {
      try {
        thresholds[i] = Float.parseFloat(jsonArray.getString(i));
      } catch (JSONException e) {
        /*no op*/
      }
    }
    return thresholds;
  }

  @Nullable
  public static File getRuleFile(Task task) {
    TaskHandler handler = mTaskHandlers.get(task.toUseCase());
    if (handler == null) {
      return null;
    }

    return handler.ruleFile;
  }

  @Nullable
  public static String[] predict(Task task, float[][] denses, String[] texts) {
    TaskHandler handler = mTaskHandlers.get(task.toUseCase());
    if (handler == null || handler.model == null) {
      return null;
    }

    int n_examples = texts.length;
    int dense_size = denses[0].length;
    MTensor dense = new MTensor(new int[] {n_examples, dense_size});
    for (int n = 0; n < n_examples; n++) {
      System.arraycopy(denses[n], 0, dense.getData(), n * dense_size, dense_size);
    }

    MTensor res = handler.model.predictOnMTML(dense, texts, task.toKey());
    float[] thresholds = handler.thresholds;
    if (res == null || thresholds == null || res.getData().length == 0 || thresholds.length == 0) {
      return null;
    }

    switch (task) {
      case MTML_APP_EVENT_PREDICTION:
        return processSuggestedEventResult(res, thresholds);
      case MTML_INTEGRITY_DETECT:
        return processIntegrityDetectionResult(res, thresholds);
    }
    return null;
  }

  @Nullable
  private static String[] processSuggestedEventResult(MTensor res, float[] thresholds) {
    int n_examples = res.getShape(0);
    int res_size = res.getShape(1);
    float[] res_data = res.getData();
    String[] result = new String[n_examples];

    if (res_size != thresholds.length) {
      return null;
    }

    for (int n = 0; n < n_examples; n++) {
      result[n] = ViewOnClickListener.OTHER_EVENT;
      for (int i = 0; i < thresholds.length; i++) {
        if (res_data[n * res_size + i] >= thresholds[i]) {
          result[n] = MTML_SUGGESTED_EVENTS_PREDICTION.get(i);
        }
      }
    }
    return result;
  }

  @Nullable
  private static String[] processIntegrityDetectionResult(MTensor res, float[] thresholds) {
    int n_examples = res.getShape(0);
    int res_size = res.getShape(1);
    float[] res_data = res.getData();
    String[] result = new String[n_examples];

    if (res_size != thresholds.length) {
      return null;
    }

    for (int n = 0; n < n_examples; n++) {
      result[n] = IntegrityManager.INTEGRITY_TYPE_NONE;
      for (int i = 0; i < thresholds.length; i++) {
        if (res_data[n * res_size + i] >= thresholds[i]) {
          result[n] = MTML_INTEGRITY_DETECT_PREDICTION.get(i);
        }
      }
    }
    return result;
  }

  private static class TaskHandler {
    String useCase;
    String assetUri;
    @Nullable String ruleUri;
    int versionId;
    @Nullable float[] thresholds;
    File ruleFile;
    @Nullable Model model;
    private Runnable onPostExecute;

    TaskHandler(
        String useCase,
        String assetUri,
        @Nullable String ruleUri,
        int versionId,
        @Nullable float[] thresholds) {
      this.useCase = useCase;
      this.assetUri = assetUri;
      this.ruleUri = ruleUri;
      this.versionId = versionId;
      this.thresholds = thresholds;
    }

    TaskHandler setOnPostExecute(Runnable onPostExecute) {
      this.onPostExecute = onPostExecute;
      return this;
    }

    @Nullable
    static TaskHandler build(@Nullable JSONObject json) {
      if (json == null) {
        return null;
      }
      try {
        String useCase = json.getString(USE_CASE_KEY);
        String assetUri = json.getString(ASSET_URI_KEY);
        String ruleUri = json.optString(RULES_URI_KEY, null);
        int versionId = json.getInt(VERSION_ID_KEY);
        float[] thresholds = parseJsonArray(json.getJSONArray(THRESHOLD_KEY));
        return new TaskHandler(useCase, assetUri, ruleUri, versionId, thresholds);
      } catch (Exception e) {
        return null;
      }
    }

    static void execute(TaskHandler handler) {
      execute(handler, Collections.singletonList(handler));
    }

    static void execute(TaskHandler master, final List<TaskHandler> slaves) {
      deleteOldFiles(master.useCase, master.versionId);

      String modelFileName = master.useCase + "_" + master.versionId;
      download(
          master.assetUri,
          modelFileName,
          new FileDownloadTask.Callback() {
            @Override
            public void onComplete(File file) {
              final Model model = Model.build(file);
              if (model != null) {
                for (final TaskHandler slave : slaves) {
                  String ruleFileName = slave.useCase + "_" + slave.versionId + "_rule";
                  download(
                      slave.ruleUri,
                      ruleFileName,
                      new FileDownloadTask.Callback() {
                        @Override
                        public void onComplete(File file) {
                          slave.model = model;
                          slave.ruleFile = file;
                          if (slave.onPostExecute != null) {
                            slave.onPostExecute.run();
                          }
                        }
                      });
                }
              }
            }
          });
    }

    private static void deleteOldFiles(String useCase, int versionId) {
      File dir = Utils.getMlDir();
      if (dir == null) {
        return;
      }
      File[] existingFiles = dir.listFiles();
      if (existingFiles == null || existingFiles.length == 0) {
        return;
      }
      String prefixWithVersion = useCase + "_" + versionId;
      for (File f : existingFiles) {
        String name = f.getName();
        if (name.startsWith(useCase) && !name.startsWith(prefixWithVersion)) {
          f.delete();
        }
      }
    }

    private static void download(String uri, String name, FileDownloadTask.Callback onComplete) {
      File file = new File(Utils.getMlDir(), name);
      if (uri == null || file.exists()) {
        onComplete.onComplete(file);
        return;
      }
      new FileDownloadTask(uri, file, onComplete).execute();
    }
  }
}
