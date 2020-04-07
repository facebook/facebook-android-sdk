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

    static MTensor embedding(String[] texts, int seq_len, MTensor w) {
        int n_examples = texts.length;
        int embedding_size = w.getShape(1);
        MTensor y = new MTensor(new int[]{n_examples, seq_len, embedding_size});
        float[] y_data = y.getData();
        float[] w_data = w.getData();
        for (int i = 0; i < n_examples; i++) {
            int[] vectorize_text = Utils.vectorize(texts[i], seq_len);
            for (int j = 0; j < seq_len; j++) {
                for (int k = 0; k < embedding_size; k++) {
                    y_data[(embedding_size * seq_len) * i + embedding_size * j + k] = w_data[vectorize_text[j] * embedding_size + k];
                }
            }
        }
        return y;
    }

    static MTensor transpose2D(MTensor x) {
        int m = x.getShape(0);
        int n = x.getShape(1);
        MTensor y = new MTensor((new int[]{n, m}));
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
        MTensor y = new MTensor(new int[]{p, n, m});
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
