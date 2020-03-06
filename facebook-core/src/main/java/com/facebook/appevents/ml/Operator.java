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
    static float[] add(float[] a, float[] b, int m, int n, int p) {
        for (int i = 0; i < m * n; i++) {
            for (int j = 0; j < p; j++) {
                a[i * p + j] += b[j];
            }
        }
        return a;
    }

    static float[] mul(float[] a, float[] b, int m, int n, int p) {
        float[] res = new float[m * p];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < p; j++) {
                res[i * p + j] = 0;
                for (int k = 0; k < n; k++) {
                    res[i * p + j] += a[i * n + k] * b[k * p + j];
                }
            }
        }
        return res;
    }

    static void relu(float[] data, int len) {
        for (int i = 0; i < len; i++) {
            if (data[i] < 0) {
                data[i] = 0;
            }
        }
    }

    static float[] concatenate(float[] a, float[] b) {
        float[] res = new float[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    static void softmax(float[] data, int n) {
        int i,j = 0;
        float max = Float.MIN_VALUE;
        float sum = 0;

        for (i = 0; i < n; i++) {

            if (data[i] > max) {
                max = data[i];
            }
        }

        for (i = 0; i < n; i++) {
            data[i] = (float) Math.exp(data[i] - max);
        }

        for (i = 0; i < n; i++) {
            sum += data[i];
        }

        for (i = 0; i < n; i++) {
            data[i] = data[i] / sum;
        }
    }

    /*
        a shape: n_examples, in_vector_size
        b shape: n_examples, out_vector_size
        c shape: out_vector_size
        return shape: n_examples, out_vector_size
     */
    static float[] dense(float[] a, float[] b, float[] c, int n_examples, int in_vector_size,
                   int out_vector_size) {
        int i, j;
        float[] m_res = mul(a, b, n_examples, in_vector_size, out_vector_size);
        for (i = 0; i < n_examples; i++) {
            for (j = 0; j < out_vector_size; j++) {
                m_res[i * out_vector_size + j] += c[j];
            }
        }
        return m_res;
    }

    /*
        a shape: n_examples, seq_length
        b shape: alphabet_size, embedding_size
        return shape: n_examples, seq_length, embedding_size
     */
    static float[] embedding(int[] a, float[] b, int n_examples, int seq_length,
                            int embedding_size) {
        int i, j, k, val;
        float[] res = new float[n_examples * seq_length * embedding_size];
        for (i = 0; i < n_examples; i++) {
            for (j = 0; j < seq_length; j++) {
                val = a[i * seq_length + j];
                for (k = 0; k < embedding_size; k++) {
                    res[(embedding_size * seq_length) * i + embedding_size * j + k] = b[
                            val * embedding_size + k];
                }
            }
        }
        return res;
    }

    /*
      input shape: m, n
      return shape: n, m
    */
    static float[] transpose2D(float[] input, int m, int n) {
        float[] transposed = new float[m * n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                transposed[j * m + i] = input[i * n + j];
            }
        }
        return transposed;
    }

    /*
       input shape: m, n, p
       return shape: p, n, m
    */
    static float[] transpose3D(float[] input, int m, int n, int p) {
        float[] transposed = new float[m * n * p];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    transposed[k * m * n + j * m + i] = input[i * n * p + j * p + k];
                }
            }
        }
        return transposed;
    }

    /*
        x shape: n_examples, seq_len, input_size
        w shape: kernel_size, input_size, output_size
        return shape: n_examples, seq_len - kernel_size + 1, output_size
     */
    static float[] conv1D(float[] x, float[] w, int n_examples, int seq_len, int input_size,
                                 int kernel_size, int output_size) {
        int n, o, i, k, m;
        float sum;
        float[] res = new float[n_examples * (seq_len - kernel_size + 1) * output_size];
        for (n = 0; n < n_examples; n++) {
            for (o = 0; o < output_size; o++) {
                for (i = 0; i < seq_len - kernel_size + 1; i++) {
                    sum = 0;
                    for (m = 0; m < kernel_size; m++) {
                        for (k = 0; k < input_size; k++) {
                            sum += x[n * (seq_len * input_size) + (m + i) * input_size + k]
                                    * w[(m * input_size + k) * output_size + o];
                        }
                    }
                    res[(n * (output_size * (seq_len - kernel_size + 1)) + i * output_size + o)] = sum;
                }
            }
        }
        return res;
    }

    /*
       x shape: n_examples, length, n_channel
       return shape: n_examples, length - pool_size + 1, n_channel
    */
    static float[] maxPool1D(float[] x, int rows, int cols, int pool_size) {
        int len = rows - pool_size + 1;
        float[] res = new float[len * cols];

        for (int c = 0; c < cols; c++) {
            for (int i = 0; i < len; i++) {
                for (int r = i; r < i + pool_size; r++) {
                    res[i * cols + c] = Math.max(res[i * cols + c], x[r * cols + c]);
                }
            }
        }
        return res;
    }
}
