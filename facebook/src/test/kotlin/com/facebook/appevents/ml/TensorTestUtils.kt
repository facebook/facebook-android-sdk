package com.facebook.appevents.ml

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject

object TensorTestUtils {
  fun setTensorData(x: MTensor, newData: FloatArray) {
    assertThat(x.data.size).isEqualTo(newData.size)
    val dataSize = x.data.size
    System.arraycopy(newData, 0, x.data, 0, dataSize)
  }

  fun createModelFile(model: Map<String, MTensor>, dir: File): File {
    val modelFile = File(dir, "testModelFile")
    val keys = model.keys.toList().sorted()

    modelFile.createNewFile()
    val modelStream = FileOutputStream(modelFile)
    val metadata = JSONObject()
    for (key in keys) {
      val weight = checkNotNull(model[key])
      val shape = JSONArray()
      for (s in 0 until weight.shapeSize) {
        shape.put(weight.getShape(s))
      }
      metadata.put(key, shape)
    }
    val metadataString = metadata.toString()

    val metadataStringLengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
    metadataStringLengthBuffer.putInt(metadataString.length)

    modelStream.write(metadataStringLengthBuffer.array())
    modelStream.write(metadataString.toByteArray())
    for (key in keys) {
      val tensor = model[key] ?: continue
      val dataBuffer = ByteBuffer.allocate(tensor.data.size * 4).order(ByteOrder.LITTLE_ENDIAN)
      dataBuffer.asFloatBuffer().put(tensor.data)
      modelStream.write(dataBuffer.array())
    }
    modelStream.close()
    return modelFile
  }

  fun createTestTensor(shape: IntArray): MTensor {
    val tensor = MTensor(shape)
    for (idx in tensor.data.indices) {
      tensor.data[idx] = idx.toFloat()
    }
    return tensor
  }
}
