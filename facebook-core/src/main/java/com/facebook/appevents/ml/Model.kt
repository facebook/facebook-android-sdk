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

import androidx.annotation.RestrictTo
import com.facebook.appevents.ml.Operator.addmv
import com.facebook.appevents.ml.Operator.concatenate
import com.facebook.appevents.ml.Operator.conv1D
import com.facebook.appevents.ml.Operator.dense
import com.facebook.appevents.ml.Operator.embedding
import com.facebook.appevents.ml.Operator.flatten
import com.facebook.appevents.ml.Operator.maxPool1D
import com.facebook.appevents.ml.Operator.relu
import com.facebook.appevents.ml.Operator.softmax
import com.facebook.appevents.ml.Operator.transpose2D
import com.facebook.appevents.ml.Operator.transpose3D
import com.facebook.appevents.ml.Utils.parseModelWeights
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.File

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Model private constructor(weights: Map<String, MTensor>) {
  private val embedding: MTensor = checkNotNull(weights["embed.weight"])
  private val convs0Weight: MTensor = transpose3D(checkNotNull(weights["convs.0.weight"]))
  private val convs1Weight: MTensor = transpose3D(checkNotNull(weights["convs.1.weight"]))
  private val convs2Weight: MTensor = transpose3D(checkNotNull(weights["convs.2.weight"]))
  private val convs0Bias: MTensor = checkNotNull(weights["convs.0.bias"])
  private val convs1Bias: MTensor = checkNotNull(weights["convs.1.bias"])
  private val convs2Bias: MTensor = checkNotNull(weights["convs.2.bias"])
  private val fc1Weight: MTensor = transpose2D(checkNotNull(weights["fc1.weight"]))
  private val fc2Weight: MTensor = transpose2D(checkNotNull(weights["fc2.weight"]))
  private val fc1Bias: MTensor = checkNotNull(weights["fc1.bias"])
  private val fc2Bias: MTensor = checkNotNull(weights["fc2.bias"])
  private val finalWeights: MutableMap<String, MTensor> = HashMap()

  init {
    val tasks =
        setOf(
            ModelManager.Task.MTML_INTEGRITY_DETECT.toKey(),
            ModelManager.Task.MTML_APP_EVENT_PREDICTION.toKey())
    for (task in tasks) {
      val weightKey = "$task.weight"
      val biasKey = "$task.bias"
      var weight = weights[weightKey]
      val bias = weights[biasKey]
      if (weight != null) {
        weight = transpose2D(weight)
        finalWeights[weightKey] = weight
      }
      if (bias != null) {
        finalWeights[biasKey] = bias
      }
    }
  }
  fun predictOnMTML(dense: MTensor, texts: Array<String>, task: String): MTensor? {
    val embed = embedding(texts, SEQ_LEN, embedding)
    var c0 = conv1D(embed, convs0Weight)
    addmv(c0, convs0Bias)
    relu(c0)
    var c1 = conv1D(c0, convs1Weight)
    addmv(c1, convs1Bias)
    relu(c1)
    c1 = maxPool1D(c1, 2)
    var c2 = conv1D(c1, convs2Weight)
    addmv(c2, convs2Bias)
    relu(c2)
    c0 = maxPool1D(c0, c0.getShape(1))
    c1 = maxPool1D(c1, c1.getShape(1))
    c2 = maxPool1D(c2, c2.getShape(1))
    flatten(c0, 1)
    flatten(c1, 1)
    flatten(c2, 1)
    val concat = concatenate(arrayOf(c0, c1, c2, dense))
    val dense1 = dense(concat, fc1Weight, fc1Bias)
    relu(dense1)
    val dense2 = dense(dense1, fc2Weight, fc2Bias)
    relu(dense2)
    val fc3Weight = finalWeights["$task.weight"]
    val fc3Bias = finalWeights["$task.bias"]
    if (fc3Weight == null || fc3Bias == null) {
      return null
    }
    val res = dense(dense2, fc3Weight, fc3Bias)
    softmax(res)
    return res
  }

  companion object {
    private const val SEQ_LEN = 128
    fun build(file: File): Model? {
      val weights = parse(file) ?: return null
      try {
        return Model(weights)
      } catch (e: Exception) {
        /* no op */
      }
      return null
    }

    private fun parse(file: File): Map<String, MTensor>? {
      val originalWeights = parseModelWeights(file)
      if (originalWeights != null) {
        val weights: MutableMap<String, MTensor> = HashMap()
        val mapping = mapping
        for (entry in originalWeights.entries) {
          var finalKey: String = entry.key
          if (mapping.containsKey(entry.key)) {
            finalKey = mapping[entry.key] ?: return null
          }
          weights[finalKey] = entry.value
        }
        return weights
      }
      return null
    }

    private val mapping: Map<String, String> =
        hashMapOf(
            "embedding.weight" to "embed.weight",
            "dense1.weight" to "fc1.weight",
            "dense2.weight" to "fc2.weight",
            "dense3.weight" to "fc3.weight",
            "dense1.bias" to "fc1.bias",
            "dense2.bias" to "fc2.bias",
            "dense3.bias" to "fc3.bias")
  }
}
