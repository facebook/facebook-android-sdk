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

import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import kotlin.math.exp
import kotlin.math.max

@AutoHandleExceptions
internal object Operator {
  @JvmStatic
  fun addmv(x: MTensor, b: MTensor) {
    val exampleSize = x.getShape(0)
    val seqLength = x.getShape(1)
    val inputSize = x.getShape(2)
    val xData = x.data
    val bData = b.data
    for (i in 0 until exampleSize) {
      for (j in 0 until seqLength) {
        for (k in 0 until inputSize) {
          xData[i * seqLength * inputSize + j * inputSize + k] += bData[k]
        }
      }
    }
  }

  @JvmStatic
  fun mul(x: MTensor, w: MTensor): MTensor {
    val exampleSize = x.getShape(0)
    val inputSize = w.getShape(0)
    val outputSize = w.getShape(1)
    val y = MTensor(intArrayOf(exampleSize, outputSize))
    val xData = x.data
    val wData = w.data
    val yData = y.data
    for (i in 0 until exampleSize) {
      for (j in 0 until outputSize) {
        yData[i * outputSize + j] = 0f
        for (k in 0 until inputSize) {
          yData[i * outputSize + j] += xData[i * inputSize + k] * wData[k * outputSize + j]
        }
      }
    }
    return y
  }

  @JvmStatic
  fun relu(x: MTensor) {
    val xData = x.data
    for (i in xData.indices) {
      if (xData[i] < 0) {
        xData[i] = 0f
      }
    }
  }

  @JvmStatic
  fun flatten(x: MTensor, startDim: Int) {
    if (startDim >= x.shapeSize) {
      return
    }
    var outputSize = 1
    for (i in startDim until x.shapeSize) {
      outputSize *= x.getShape(i)
    }
    val newShape = IntArray(startDim + 1)
    for (i in 0 until startDim) {
      newShape[i] = x.getShape(i)
    }
    newShape[startDim] = outputSize
    x.reshape(newShape)
  }

  @JvmStatic
  fun concatenate(tensors: Array<MTensor>): MTensor {
    val exampleSize = tensors[0].getShape(0)
    var outputSize = 0
    for (i in tensors.indices) {
      outputSize += tensors[i].getShape(1)
    }
    val y = MTensor(intArrayOf(exampleSize, outputSize))
    val yData = y.data
    for (n in 0 until exampleSize) {
      var desPos = n * outputSize
      for (i in tensors.indices) {
        val xData = tensors[i].data
        val inputSize = tensors[i].getShape(1)
        System.arraycopy(xData, n * inputSize, yData, desPos, inputSize)
        desPos += inputSize
      }
    }
    return y
  }

  @JvmStatic
  fun softmax(x: MTensor) {
    val exampleSize = x.getShape(0)
    val inputSize = x.getShape(1)
    val xData = x.data
    for (n in 0 until exampleSize) {
      val startIndex = n * inputSize
      val endIndex = startIndex + inputSize
      var max = Float.MIN_VALUE
      var sum = 0f
      for (i in startIndex until endIndex) {
        if (xData[i] > max) {
          max = xData[i]
        }
      }
      for (i in startIndex until endIndex) {
        xData[i] = exp((xData[i] - max).toDouble()).toFloat()
        sum += xData[i]
      }
      for (i in startIndex until endIndex) {
        xData[i] = xData[i] / sum
      }
    }
  }

  @JvmStatic
  fun dense(x: MTensor, w: MTensor, b: MTensor): MTensor {
    val exampleSize = x.getShape(0)
    val outputSize = b.getShape(0)
    val y = mul(x, w)
    val bData = b.data
    val yData = y.data
    for (i in 0 until exampleSize) {
      for (j in 0 until outputSize) {
        yData[i * outputSize + j] += bData[j]
      }
    }
    return y
  }

  @JvmStatic
  fun embedding(texts: Array<String>, seqLength: Int, w: MTensor): MTensor {
    val exampleSize = texts.size
    val embeddingSize = w.getShape(1)
    val y = MTensor(intArrayOf(exampleSize, seqLength, embeddingSize))
    val yData = y.data
    val wData = w.data
    for (i in 0 until exampleSize) {
      val vectorizedText = Utils.vectorize(texts[i], seqLength)
      for (j in 0 until seqLength) {
        System.arraycopy(
            wData,
            vectorizedText[j] * embeddingSize,
            yData,
            embeddingSize * seqLength * i + embeddingSize * j,
            embeddingSize)
      }
    }
    return y
  }

  @JvmStatic
  fun transpose2D(x: MTensor): MTensor {
    val m = x.getShape(0)
    val n = x.getShape(1)
    val y = MTensor(intArrayOf(n, m))
    val xData = x.data
    val yData = y.data
    for (i in 0 until m) {
      for (j in 0 until n) {
        yData[j * m + i] = xData[i * n + j]
      }
    }
    return y
  }

  @JvmStatic
  fun transpose3D(x: MTensor): MTensor {
    val m = x.getShape(0)
    val n = x.getShape(1)
    val p = x.getShape(2)
    val y = MTensor(intArrayOf(p, n, m))
    val xData = x.data
    val yData = y.data
    for (i in 0 until m) {
      for (j in 0 until n) {
        for (k in 0 until p) {
          yData[k * m * n + j * m + i] = xData[i * n * p + j * p + k]
        }
      }
    }
    return y
  }

  @JvmStatic
  fun conv1D(x: MTensor, w: MTensor): MTensor {
    val exampleSize = x.getShape(0)
    val inputSeqLength = x.getShape(1)
    val inputSize = x.getShape(2)
    val kernelSize = w.getShape(0)
    val outputSeqLength = inputSeqLength - kernelSize + 1
    val outputSize = w.getShape(2)
    val y = MTensor(intArrayOf(exampleSize, outputSeqLength, outputSize))
    val xData = x.data
    val yData = y.data
    val wData = w.data
    for (n in 0 until exampleSize) {
      for (o in 0 until outputSize) {
        for (i in 0 until outputSeqLength) {
          var sum = 0f
          for (m in 0 until kernelSize) {
            for (k in 0 until inputSize) {
              sum +=
                  xData[n * (inputSeqLength * inputSize) + (m + i) * inputSize + k] *
                      wData[(m * inputSize + k) * outputSize + o]
            }
          }
          yData[n * (outputSeqLength * outputSize) + i * outputSize + o] = sum
        }
      }
    }
    return y
  }

  @JvmStatic
  fun maxPool1D(x: MTensor, poolSize: Int): MTensor {
    val exampleSize = x.getShape(0)
    val inputSeqLength = x.getShape(1)
    val inputSize = x.getShape(2)
    val outputSeqLength = inputSeqLength - poolSize + 1
    val y = MTensor(intArrayOf(exampleSize, outputSeqLength, inputSize))
    val xData = x.data
    val yData = y.data
    for (n in 0 until exampleSize) {
      for (c in 0 until inputSize) {
        for (i in 0 until outputSeqLength) {
          val yIndex = n * outputSeqLength * inputSize + i * inputSize + c
          val xIndex = n * inputSeqLength * inputSize + i * inputSize + c
          yData[yIndex] = Float.MIN_VALUE
          for (r in 0 until poolSize) {
            yData[yIndex] = max(yData[yIndex], xData[xIndex + r * inputSize])
          }
        }
      }
    }
    return y
  }
}
