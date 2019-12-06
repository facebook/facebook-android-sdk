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
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.suggestedevents.ViewOnClickListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Model {
    private static final String DIR_NAME = "facebook_ml/";
    public static final String SHOULD_FILTER = "SHOULD_FILTER";
    @SuppressWarnings("deprecation")
    private static final List<String> SUGGESTED_EVENTS_PREDICTION =
            Arrays.asList(
                    AppEventsConstants.EVENT_NAME_ADDED_TO_CART,
                    AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
                    ViewOnClickListener.OTHER_EVENT,
                    AppEventsConstants.EVENT_NAME_PURCHASED);

    private String useCase;
    private File modelFile;
    private File ruleFile;
    private File dir;
    private int versionID;
    private float[] thresholds;
    @Nullable private String modelUri;
    @Nullable private String ruleUri;

    @Nullable private static Weight embedding;
    @Nullable private static Weight convs_1_weight;
    @Nullable private static Weight convs_2_weight;
    @Nullable private static Weight convs_3_weight;
    @Nullable private static Weight convs_1_bias;
    @Nullable private static Weight convs_2_bias;
    @Nullable private static Weight convs_3_bias;
    @Nullable private static Weight fc1_weight;
    @Nullable private static Weight fc2_weight;
    @Nullable private static Weight fc3_weight;
    @Nullable private static Weight fc1_bias;
    @Nullable private static Weight fc2_bias;
    @Nullable private static Weight fc3_bias;

    private static final int SEQ_LEN = 128;
    private static final int EMBEDDING_SIZE = 64;

    private static final Map<String, String> WEIGHTS_KEY_MAPPING = new HashMap<String, String>() {{
        put("embedding.weight", "embed.weight");
        put("dense1.weight", "fc2.weight");
        put("dense2.weight", "fc2.weight");
        put("dense3.weight", "fc3.weight");
        put("dense1.bias", "fc1.bias");
        put("dense2.bias", "fc2.bias");
        put("dense3.bias", "fc3.bias");
    }};

    Model(String useCase, int versionID, String modelUri,
          @Nullable String ruleUri, float[] thresholds) {
        this.useCase = useCase;
        this.versionID = versionID;
        this.thresholds = thresholds;
        this.modelUri = modelUri;
        this.ruleUri = ruleUri;

        dir = new File(FacebookSdk.getApplicationContext().getFilesDir(), DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.modelFile = new File(dir, useCase + "_" + versionID);
        this.ruleFile = new File(dir, useCase + "_" + versionID + "_rule");
    }


    private void deleteOldFiles() {
        File[] existingFiles = dir.listFiles();
        if (existingFiles == null || existingFiles.length == 0) {
            return;
        }
        String prefixWithVersion = useCase + "_" + versionID;
        for (File f : existingFiles) {
            String name = f.getName();
            if (name.startsWith(useCase) && !name.startsWith(prefixWithVersion)) {
                f.delete();
            }
        }
    }

    boolean initializeAsync() {
        deleteOldFiles();
        if (!modelFile.exists() && !downloadAsync(modelUri, modelFile)) {
            return false;
        }

        // if ruleUri is null, assume there is no rule required
        return ruleUri == null || ruleFile.exists()
                || downloadAsync(ruleUri, ruleFile);
    }

    @Nullable
    File getRuleFile() {
        return ruleFile;
    }

    // return true if weights initialized successful
    private boolean initializeWeights() {
        // TODO: (@linajin T57235101) make it more general and support other use cases
        try {
            InputStream inputStream = new FileInputStream(modelFile);
            int length = inputStream.available();
            DataInputStream dataIs = new DataInputStream(inputStream);
            byte[] allData = new byte[length];
            dataIs.readFully(allData);
            dataIs.close();

            if (length < 4) {
                return false;
            }

            ByteBuffer bb = ByteBuffer.wrap(allData, 0, 4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int jsonLen =  bb.getInt();

            if (length < jsonLen + 4) {
                return false;
            }

            String jsonStr = new String(allData, 4, jsonLen);
            JSONObject info = new JSONObject(jsonStr);

            JSONArray names = info.names();
            String[] keys = new String[names.length()];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = names.getString(i);
            }
            Arrays.sort(keys);

            int offset = 4 + jsonLen;

            Map<String, Weight> weights = new HashMap<>();

            for (String key : keys) {
                int count = 1;
                JSONArray shapes = info.getJSONArray(key);
                int[] shape = new int[shapes.length()];
                for (int i = 0; i < shape.length; i++)  {
                    shape[i] = shapes.getInt(i);
                    count *= shape[i];
                }

                if (offset + count * 4 > length) {
                    return false;
                }

                bb = ByteBuffer.wrap(allData, offset, count * 4);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                float[] data = new float[count];
                bb.asFloatBuffer().get(data, 0, count);
                String finalKey = key;
                if (WEIGHTS_KEY_MAPPING.containsKey(key)) {
                    finalKey = WEIGHTS_KEY_MAPPING.get(key);
                }
                weights.put(finalKey, new Weight(shape, data));
                offset += count * 4;
            }

            embedding = weights.get("embed.weight");
            convs_1_weight = weights.get("convs.0.weight");
            convs_2_weight = weights.get("convs.1.weight");
            convs_3_weight = weights.get("convs.2.weight");
            convs_1_weight.data = Operator.transpose3D(convs_1_weight.data,
                    convs_1_weight.shape[0], convs_1_weight.shape[1], convs_1_weight.shape[2]);
            convs_2_weight.data = Operator.transpose3D(convs_2_weight.data,
                    convs_2_weight.shape[0], convs_2_weight.shape[1], convs_2_weight.shape[2]);
            convs_3_weight.data = Operator.transpose3D(convs_3_weight.data,
                    convs_3_weight.shape[0], convs_3_weight.shape[1], convs_3_weight.shape[2]);
            convs_1_bias = weights.get("convs.0.bias");
            convs_2_bias = weights.get("convs.1.bias");
            convs_3_bias = weights.get("convs.2.bias");
            fc1_weight = weights.get("fc1.weight");
            fc2_weight = weights.get("fc2.weight");
            fc3_weight = weights.get("fc3.weight");
            fc1_weight.data = Operator.transpose2D(fc1_weight.data, fc1_weight.shape[0],
                    fc1_weight.shape[1]);
            fc2_weight.data = Operator.transpose2D(fc2_weight.data, fc2_weight.shape[0],
                    fc2_weight.shape[1]);
            fc3_weight.data = Operator.transpose2D(fc3_weight.data, fc3_weight.shape[0],
                    fc3_weight.shape[1]);
            fc1_bias = weights.get("fc1.bias");
            fc2_bias = weights.get("fc2.bias");
            fc3_bias = weights.get("fc3.bias");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    String predict(float[] dense, String text) {
        int[] x = Utils.vectorize(text, SEQ_LEN);
        float[] embed_x = Operator.embedding(x, embedding.data, 1, SEQ_LEN, EMBEDDING_SIZE);
        float[] c1 = Operator.conv1D(embed_x, convs_1_weight.data, 1, SEQ_LEN, EMBEDDING_SIZE,
                convs_1_weight.shape[2], convs_1_weight.shape[0]);
        float[] c2 = Operator.conv1D(embed_x, convs_2_weight.data, 1, SEQ_LEN, EMBEDDING_SIZE,
                convs_2_weight.shape[2], convs_2_weight.shape[0]);
        float[] c3 = Operator.conv1D(embed_x, convs_3_weight.data, 1, SEQ_LEN, EMBEDDING_SIZE,
                convs_3_weight.shape[2], convs_3_weight.shape[0]);
        Operator.add(c1, convs_1_bias.data, 1, SEQ_LEN - convs_1_weight.shape[2] + 1,
                convs_1_weight.shape[0]);
        Operator.add(c2, convs_2_bias.data, 1, SEQ_LEN - convs_2_weight.shape[2] + 1,
                convs_2_weight.shape[0]);
        Operator.add(c3, convs_3_bias.data, 1, SEQ_LEN - convs_3_weight.shape[2] + 1,
                convs_3_weight.shape[0]);

        Operator.relu(c1, (SEQ_LEN - convs_1_weight.shape[2] + 1) * convs_1_weight.shape[0]);
        Operator.relu(c2, (SEQ_LEN - convs_2_weight.shape[2] + 1) * convs_2_weight.shape[0]);
        Operator.relu(c3, (SEQ_LEN - convs_3_weight.shape[2] + 1) * convs_3_weight.shape[0]);

        float[] ca = Operator.maxPool1D(c1, (SEQ_LEN - convs_1_weight.shape[2] + 1),
                convs_1_weight.shape[0], (SEQ_LEN - convs_1_weight.shape[2] + 1)); // (1, 1, 32)
        float[] cb = Operator.maxPool1D(c2, (SEQ_LEN - convs_2_weight.shape[2] + 1),
                convs_2_weight.shape[0], (SEQ_LEN - convs_2_weight.shape[2] + 1)); // (1, 1, 32)
        float[] cc = Operator.maxPool1D(c3, (SEQ_LEN - convs_3_weight.shape[2] + 1),
                convs_3_weight.shape[0], (SEQ_LEN - convs_3_weight.shape[2] + 1)); // (1, 1, 32)

        float[] concat = Operator.concatenate(Operator.concatenate(Operator.concatenate(ca, cb),
                cc), dense);

        float[] dense1_x = Operator.dense(concat, fc1_weight.data, fc1_bias.data, 1,
                fc1_weight.shape[1],
                fc1_weight.shape[0]);
        Operator.relu(dense1_x, fc1_bias.shape[0]);
        float[] dense2_x = Operator.dense(dense1_x, fc2_weight.data, fc2_bias.data, 1,
                fc2_weight.shape[1],
                fc2_weight.shape[0]);
        Operator.relu(dense2_x, fc2_bias.shape[0]);
        float[] predictedRaw = Operator.dense(dense2_x, fc3_weight.data, fc3_bias.data, 1,
                fc3_weight.shape[1],
                fc3_weight.shape[0]);
        Operator.softmax(predictedRaw, fc3_bias.shape[0]);

        return processPredictionResult(predictedRaw);
    }

    @Nullable
    String processPredictionResult(float[] predictedResult) {
        if (predictedResult.length == 0 || thresholds.length == 0) {
            return null;
        }
        if (useCase.equals(ModelManager.MODEL_SUGGESTED_EVENTS)) {
            return processSuggestedEventResult(predictedResult);
        } else if (useCase.equals(ModelManager.MODEL_ADDRESS_DETECTION)) {
            return processAddressDetectionResult(predictedResult);
        }
        return null;
    }

    @Nullable
    String processSuggestedEventResult(float[] predictedResult) {
        if (thresholds.length != predictedResult.length) {
            return null;
        }
        for (int i = 0; i < thresholds.length; i++) {
            if (predictedResult[i] >= thresholds[i]) {
                return SUGGESTED_EVENTS_PREDICTION.get(i);
            }
        }
        return ViewOnClickListener.OTHER_EVENT;
    }

    @Nullable
    String processAddressDetectionResult(float[] predictedResult) {
        return predictedResult[1] >= thresholds[0] ? SHOULD_FILTER : null;
    }

    static boolean downloadAsync(String uriStr, File destFile) {
        try {
            URL url = new URL(uriStr);
            URLConnection conn = url.openConnection();
            int contentLength = conn.getContentLength();

            DataInputStream stream = new DataInputStream(url.openStream());

            byte[] buffer = new byte[contentLength];
            stream.readFully(buffer);
            stream.close();

            DataOutputStream fos = new DataOutputStream(new FileOutputStream(destFile));
            fos.write(buffer);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            /** no op **/
        }
        return false;
    }

    private static class Weight {
        public int[] shape;
        public float[] data;

        Weight(int[] shape, float[] data)  {
            this.shape = shape;
            this.data = data;
        }
    }
}
