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

import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;

@AutoHandleExceptions
final class Operator {

  static void addmv(MTensor x, MTensor b) {
    int n_example = x.getShape(0);
    int seq_len = x.getShape(1);
    int input_size = x.getShape(2);
    float[] x_data = x.getData();
    float[] b_data = b.getData();

    for (int i = 0; i < n_example; i++) {
      for (int j = 0; j < seq_len; j++) {
        for (int k = 0; k < input_size; k++) {
          x_data[i * seq_len * input_size + j * input_size + k] += b_data[k];
        }
      }
    }
  }

  static MTensor mul(MTensor x, MTensor w) {
    int n_examples = x.getShape(0);
    int input_size = w.getShape(0);
    int output_size = w.getShape(1);
    MTensor y = new MTensor(new int[] {n_examples, output_size});
    float[] x_data = x.getData();
    float[] w_data = w.getData();
    float[] y_data = y.getData();

    for (int i = 0; i < n_examples; i++) {
      for (int j = 0; j < output_size; j++) {
        y_data[i * output_size + j] = 0;
        for (int k = 0; k < input_size; k++) {
          y_data[i * output_size + j] += x_data[i * input_size + k] * w_data[k * output_size + j];
        }
      }
    }
    return y;
  }

  static void relu(MTensor x) {
    float[] x_data = x.getData();
    for (int i = 0; i < x_data.length; i++) {
      if (x_data[i] < 0) {
        x_data[i] = 0;
      }
    }
  }

  static void flatten(MTensor x, int start_dim) {
    if (start_dim >= x.getShapeSize()) {
      return;
    }
    int output_size = 1;
    for (int i = start_dim; i < x.getShapeSize(); i++) {
      output_size *= x.getShape(i);
    }
    int[] new_shape = new int[start_dim + 1];
    for (int i = 0; i < start_dim; i++) {
      new_shape[i] = x.getShape(i);
    }
    new_shape[start_dim] = output_size;
    x.reshape(new_shape);
  }

  static MTensor concatenate(MTensor[] tensors) {
    int n_examples = tensors[0].getShape(0);
    int output_size = 0;
    for (int i = 0; i < tensors.length; i++) {
      output_size += tensors[i].getShape(1);
    }
    MTensor y = new MTensor(new int[] {n_examples, output_size});
    float[] y_data = y.getData();

    for (int n = 0; n < n_examples; n++) {
      int desPos = n * output_size;
      for (int i = 0; i < tensors.length; i++) {
        float[] x_data = tensors[i].getData();
        int input_size = tensors[i].getShape(1);
        System.arraycopy(x_data, n * input_size, y_data, desPos, input_size);
        desPos += input_size;
      }
    }
    return y;
  }

  static void softmax(MTensor x) {
    int n_examples = x.getShape(0);
    int input_size = x.getShape(1);
    float[] x_data = x.getData();
    for (int n = 0; n < n_examples; n++) {
      int start_idx = n * input_size;
      int end_idx = start_idx + input_size;
      float max = Float.MIN_VALUE;
      float sum = 0;

      for (int i = start_idx; i < end_idx; i++) {
        if (x_data[i] > max) {
          max = x_data[i];
        }
      }

      for (int i = start_idx; i < end_idx; i++) {
        x_data[i] = (float) Math.exp(x_data[i] - max);
      }

      for (int i = start_idx; i < end_idx; i++) {
        sum += x_data[i];
      }

      for (int i = start_idx; i < end_idx; i++) {
        x_data[i] = x_data[i] / sum;
      }
    }
  }

  static MTensor dense(MTensor x, MTensor w, MTensor b) {
    int n_examples = x.getShape(0);
    int output_size = b.getShape(0);
    MTensor y = mul(x, w);
    float[] b_data = b.getData();
    float[] y_data = y.getData();

    for (int i = 0; i < n_examples; i++) {
      for (int j = 0; j < output_size; j++) {
        y_data[i * output_size + j] += b_data[j];
      }
    }
    return y;
  }

  static MTensor embedding(String[] texts, int seq_len, MTensor w) {
    int n_examples = texts.length;
    int embedding_size = w.getShape(1);
    MTensor y = new MTensor(new int[] {n_examples, seq_len, embedding_size});
    float[] y_data = y.getData();
    float[] w_data = w.getData();

    for (int i = 0; i < n_examples; i++) {
      int[] vectorize_text = Utils.vectorize(texts[i], seq_len);
      for (int j = 0; j < seq_len; j++) {
        System.arraycopy(
            w_data,
            vectorize_text[j] * embedding_size,
            y_data,
            embedding_size * seq_len * i + embedding_size * j,
            embedding_size);
      }
    }
    return y;
  }

  static MTensor transpose2D(MTensor x) {
    int m = x.getShape(0);
    int n = x.getShape(1);
    MTensor y = new MTensor((new int[] {n, m}));
    float[] x_data = x.getData();
    float[] y_data = y.getData();

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        y_data[j * m + i] = x_data[i * n + j];
      }
    }
    return y;
  }

  static MTensor transpose3D(MTensor x) {
    int m = x.getShape(0);
    int n = x.getShape(1);
    int p = x.getShape(2);
    MTensor y = new MTensor(new int[] {p, n, m});
    float[] x_data = x.getData();
    float[] y_data = y.getData();

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        for (int k = 0; k < p; k++) {
          y_data[k * m * n + j * m + i] = x_data[i * n * p + j * p + k];
        }
      }
    }
    return y;
  }

  static MTensor conv1D(MTensor x, MTensor w) {
    int n_examples = x.getShape(0);
    int input_seq_len = x.getShape(1);
    int input_size = x.getShape(2);
    int kernel_size = w.getShape(0);
    int output_seq_len = input_seq_len - kernel_size + 1;
    int output_size = w.getShape(2);
    MTensor y = new MTensor(new int[] {n_examples, output_seq_len, output_size});
    float[] x_data = x.getData();
    float[] y_data = y.getData();
    float[] w_data = w.getData();

    for (int n = 0; n < n_examples; n++) {
      for (int o = 0; o < output_size; o++) {
        for (int i = 0; i < output_seq_len; i++) {
          float sum = 0;
          for (int m = 0; m < kernel_size; m++) {
            for (int k = 0; k < input_size; k++) {
              sum +=
                  x_data[n * (input_seq_len * input_size) + (m + i) * input_size + k]
                      * w_data[(m * input_size + k) * output_size + o];
            }
          }
          y_data[(n * (output_seq_len * output_size) + i * output_size + o)] = sum;
        }
      }
    }
    return y;
  }

  static MTensor maxPool1D(MTensor x, int pool_size) {
    int n_examples = x.getShape(0);
    int input_seq_len = x.getShape(1);
    int input_size = x.getShape(2);
    int output_seq_len = input_seq_len - pool_size + 1;
    MTensor y = new MTensor(new int[] {n_examples, output_seq_len, input_size});
    float[] x_data = x.getData();
    float[] y_data = y.getData();

    for (int n = 0; n < n_examples; n++) {
      for (int c = 0; c < input_size; c++) {
        for (int i = 0; i < output_seq_len; i++) {
          int y_index = n * output_seq_len * input_size + i * input_size + c;
          int x_index = n * input_seq_len * input_size + i * input_size + c;
          y_data[y_index] = Float.MIN_VALUE;
          for (int r = 0; r < pool_size; r++) {
            y_data[y_index] = Math.max(y_data[y_index], x_data[x_index + r * input_size]);
          }
        }
      }
    }
    return y;
  }
}
