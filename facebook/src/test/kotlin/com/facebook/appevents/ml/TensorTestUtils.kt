package com.facebook.appevents.ml

import org.assertj.core.api.Assertions.assertThat

object TensorTestUtils {
  fun setTensorData(x: MTensor, newData: FloatArray) {
    assertThat(x.data.size).isEqualTo(newData.size)
    val dataSize = x.data.size
    System.arraycopy(newData, 0, x.data, 0, dataSize)
  }
}
