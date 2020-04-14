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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.facebook.appevents.ml.ModelManager.Task.*;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Model {

    private MTensor embedding;
    private MTensor convs_0_weight, convs_1_weight, convs_2_weight;
    private MTensor convs_0_bias, convs_1_bias, convs_2_bias;
    private MTensor fc1_weight, fc2_weight;
    private MTensor fc1_bias, fc2_bias;
    private final Map<String, MTensor> final_weights = new HashMap<>();

    private static final int SEQ_LEN = 128;

    private Model(Map<String, MTensor> weights) {
        embedding = weights.get("embed.weight");
        convs_0_weight = Operator.transpose3D(weights.get("convs.0.weight"));
        convs_1_weight = Operator.transpose3D(weights.get("convs.1.weight"));
        convs_2_weight = Operator.transpose3D(weights.get("convs.2.weight"));
        convs_0_bias = weights.get("convs.0.bias");
        convs_1_bias = weights.get("convs.1.bias");
        convs_2_bias = weights.get("convs.2.bias");
        fc1_weight = Operator.transpose2D(weights.get("fc1.weight"));
        fc2_weight = Operator.transpose2D(weights.get("fc2.weight"));
        fc1_bias = weights.get("fc1.bias");
        fc2_bias = weights.get("fc2.bias");

        Set<String> tasks = new HashSet<String>() {{
            add(MTML_ADDRESS_DETECTION.toKey());
            add(MTML_APP_EVENT_PREDICTION.toKey());
        }};
        for (String task : tasks) {
            String weightKey = task + ".weight";
            String biasKey = task + ".bias";
            MTensor weight = weights.get(weightKey);
            MTensor bias = weights.get(biasKey);
            if (weight != null) {
                weight = Operator.transpose2D(weight);
                final_weights.put(weightKey, weight);
            }
            if (bias != null) {
                final_weights.put(biasKey, bias);
            }
        }
    }

    @Nullable
    public MTensor predictOnMTML(MTensor dense, String[] texts, String task) {
        MTensor embed_x = Operator.embedding(texts, SEQ_LEN, embedding);

        MTensor c0 = Operator.conv1D(embed_x, convs_0_weight);
        Operator.addmv(c0, convs_0_bias);
        Operator.relu(c0);

        MTensor c1 = Operator.conv1D(c0, convs_1_weight);
        Operator.addmv(c1, convs_1_bias);
        Operator.relu(c1);
        c1 = Operator.maxPool1D(c1, 2);

        MTensor c2 = Operator.conv1D(c1, convs_2_weight);
        Operator.addmv(c2, convs_2_bias);
        Operator.relu(c2);

        c0 = Operator.maxPool1D(c0, c0.getShape(1));
        c1 = Operator.maxPool1D(c1, c1.getShape(1));
        c2 = Operator.maxPool1D(c2, c2.getShape(1));

        Operator.flatten(c0, 1);
        Operator.flatten(c1, 1);
        Operator.flatten(c2, 1);

        MTensor concat = Operator.concatenate(new MTensor[]{c0, c1, c2, dense});

        MTensor dense1_x = Operator.dense(concat, fc1_weight, fc1_bias);
        Operator.relu(dense1_x);
        MTensor dense2_x = Operator.dense(dense1_x, fc2_weight, fc2_bias);
        Operator.relu(dense2_x);

        MTensor fc3_weight = final_weights.get(task + ".weight");
        MTensor fc3_bias = final_weights.get(task + ".bias");
        if (fc3_weight == null || fc3_bias == null) {
            return null;
        }

        MTensor res = Operator.dense(dense2_x, fc3_weight, fc3_bias);
        Operator.softmax(res);

        return res;
    }

    @Nullable
    public static Model build(File file) {
        Map<String, MTensor> weights = parse(file);
        try {
            return new Model(weights);
        } catch (Exception e) { /* no op */ }
        return null;
    }

    @Nullable
    private static Map<String, MTensor> parse(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            int length = inputStream.available();
            DataInputStream dataIs = new DataInputStream(inputStream);
            byte[] allData = new byte[length];
            dataIs.readFully(allData);
            dataIs.close();

            if (length < 4) {
                return null;
            }

            ByteBuffer bb = ByteBuffer.wrap(allData, 0, 4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int jsonLen = bb.getInt();

            if (length < jsonLen + 4) {
                return null;
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

            Map<String, MTensor> weights = new HashMap<>();
            Map<String, String> mapping = getMapping();

            for (String key : keys) {
                int count = 1;
                JSONArray shapes = info.getJSONArray(key);
                int[] shape = new int[shapes.length()];
                for (int i = 0; i < shape.length; i++) {
                    shape[i] = shapes.getInt(i);
                    count *= shape[i];
                }

                if (offset + count * 4 > length) {
                    return null;
                }

                bb = ByteBuffer.wrap(allData, offset, count * 4);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                MTensor tensor = new MTensor(shape);
                bb.asFloatBuffer().get(tensor.getData(), 0, count);
                String finalKey = key;
                if (mapping.containsKey(key)) {
                    finalKey = mapping.get(key);
                }
                weights.put(finalKey, tensor);
                offset += count * 4;
            }

            return weights;
        } catch (Exception e) { /* no op */ }
        return null;
    }

    private static Map<String, String> getMapping() {
        return new HashMap<String, String>() {{
            put("embedding.weight", "embed.weight");
            put("dense1.weight", "fc1.weight");
            put("dense2.weight", "fc2.weight");
            put("dense3.weight", "fc3.weight");
            put("dense1.bias", "fc1.bias");
            put("dense2.bias", "fc2.bias");
            put("dense3.bias", "fc3.bias");
        }};
    }
}
