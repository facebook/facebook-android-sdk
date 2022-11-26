/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.ml

import com.facebook.FacebookTestCase
import com.facebook.appevents.ml.TensorTestUtils.createModelFile
import com.facebook.appevents.ml.TensorTestUtils.createTestTensor
import java.io.File
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.After
import org.junit.Test
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
class ModelTest : FacebookTestCase() {
  private lateinit var testFileDir: File
  private val testWeights =
      hashMapOf(
          "embedding.weight" to createTestTensor(intArrayOf(256, 32)),
          "convs.0.weight" to createTestTensor(intArrayOf(32, 32, 3)),
          "convs.0.bias" to createTestTensor(intArrayOf(32)),
          "convs.1.weight" to createTestTensor(intArrayOf(64, 32, 3)),
          "convs.1.bias" to createTestTensor(intArrayOf(64)),
          "convs.2.weight" to createTestTensor(intArrayOf(64, 64, 3)),
          "convs.2.bias" to createTestTensor(intArrayOf(64)),
          "dense1.weight" to createTestTensor(intArrayOf(128, 190)),
          "dense1.bias" to createTestTensor(intArrayOf(128)),
          "dense2.weight" to createTestTensor(intArrayOf(64, 128)),
          "dense2.bias" to createTestTensor(intArrayOf(64)),
          "integrity_detect.weight" to createTestTensor(intArrayOf(3, 64)),
          "integrity_detect.bias" to createTestTensor(intArrayOf(3)),
          "app_event_pred.weight" to createTestTensor(intArrayOf(5, 64)),
          "app_event_pred.bias" to createTestTensor(intArrayOf(5)))

  override fun setUp() {
    super.setUp()
    testFileDir = File(UUID.randomUUID().toString())
    testFileDir.mkdirs()
  }

  @After
  fun tearDown() {
    testFileDir.deleteRecursively()
  }

  @Test
  fun `test build model with file`() {
    val modelFile = createModelFile(testWeights, testFileDir)
    val model = checkNotNull(Model.build(modelFile))
    val dense = createTestTensor(intArrayOf(1, 30))
    val result = model.predictOnMTML(dense, arrayOf("test"), "app_event_pred")
    checkNotNull(result)
    assertThat(result.data.sum()).isEqualTo(1.0f, Offset.offset(1e-7f))
  }

  @Test
  fun `test run invalid task`() {
    val modelFile = createModelFile(testWeights, testFileDir)
    val model = checkNotNull(Model.build(modelFile))
    val dense = createTestTensor(intArrayOf(1, 30))
    val result = model.predictOnMTML(dense, arrayOf("test"), "invalid task")
    assertThat(result).isNull()
  }

  @Test
  fun `test build model with invalid model file`() {
    val weights = hashMapOf("embedding.weight" to createTestTensor(intArrayOf(256, 32)))
    val modelFile = createModelFile(weights, testFileDir)
    val model = Model.build(modelFile)
    assertThat(model).isNull()
  }
}
