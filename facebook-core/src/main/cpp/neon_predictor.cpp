// Copyright 2004-present Facebook. All Rights Reserved.
//
// You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
// copy, modify, and distribute this software in source code or binary form for use
// in connection with the web services and APIs provided by Facebook.
//
// As with any software that integrates with the Facebook platform, your use of
// this software is subject to the Facebook Developer Principles and Policies
// [http://developers.facebook.com/policy/]. This copyright notice shall be
// included in all copies or substantial portions of the software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#include <jni.h>
#include <stddef.h>
#include <stdint.h>
#include <cassert>
#include <cmath>
#include <cstring>
#include <memory>
#include <unordered_map>
#include <vector>
#include <iostream>

// minimal aten implementation
#define MAT_ALWAYS_INLINE inline __attribute__((always_inline))
#define SEQ_LEN 128
#define ALPHABET_SIZE 256
#define EMBEDDING_SIZE 64
#define DENSE_FEATURE_LEN 30

namespace mat {

    template <typename T, size_t N>
    class MTensorAccessor {
    public:
        MAT_ALWAYS_INLINE
        MTensorAccessor(T* data, const int64_t* sizes, const int64_t* strides)
                : data_(data), sizes_(sizes), strides_(strides) {}

        MAT_ALWAYS_INLINE MTensorAccessor<T, N - 1> operator[](int64_t i) {
            return MTensorAccessor<T, N - 1>(
                    this->data_ + this->strides_[0] * i,
                    this->sizes_ + 1,
                    this->strides_ + 1);
        }

    private:
        T* data_;
        const int64_t* sizes_;
        const int64_t* strides_;
    };

    template <typename T>
    class MTensorAccessor<T, 1> {
    public:
        MAT_ALWAYS_INLINE
        MTensorAccessor(T* data, const int64_t* sizes, const int64_t* strides)
                : data_(data), sizes_(sizes), strides_(strides) {}

        MAT_ALWAYS_INLINE T& operator[](int64_t i) {
            // assume stride==1 in innermost dimension.
            // DCHECK_EQ(strides_[0], 1);
            return this->data_[i];
        }

    private:
        T* data_;
        const int64_t* sizes_;
        const int64_t* strides_;
    };

    void* MAllocateMemory(size_t nbytes) {
        void* ptr = nullptr;
        assert(nbytes > 0);
#ifdef __ANDROID__
        ptr = memalign(64, nbytes);
#else
        const int ret = posix_memalign(&ptr, 64, nbytes);
        (void)ret;
        assert(ret == 0);
#endif
        return ptr;
    }

    void MFreeMemory(void* ptr) {
        free(ptr);
    }

    class MTensor {
    public:
        MTensor(){};
        MTensor(const std::vector<int64_t>& sizes) {
            auto strides = std::vector<int64_t>(sizes.size());
            strides[strides.size() - 1] = 1;
            for (auto i = static_cast<int32_t>(strides.size()) - 2; i >= 0; --i) {
                strides[i] = strides[i + 1] * sizes[i + 1];
            }
            strides_ = strides;
            sizes_ = sizes;
            // assume float32 storage.
            size_t nbytes = sizeof(float);
            for (auto size : sizes) {
                nbytes *= size;
            }
            storage_ = std::shared_ptr<void>(MAllocateMemory(nbytes), [](void* ptr) {
                if (ptr) {
                    MFreeMemory(ptr);
                }
            });
        }

        int64_t size(int dim) {
            return sizes_[dim];
        }

        const std::vector<int64_t>& sizes() const {
            return sizes_;
        }

        const std::vector<int64_t>& strides() const {
            return strides_;
        }

        template <typename T>
        T* data() {
            return static_cast<T*>(storage_.get());
        }

        template <typename T, size_t N>
        MTensorAccessor<T, N> accessor() {
            return MTensorAccessor<T, N>(data<T>(), sizes().data(), strides().data());
        }

    private:
        std::vector<int64_t> sizes_;
        std::vector<int64_t> strides_;
        std::shared_ptr<void> storage_;
    };

    MTensor mempty(const std::vector<int64_t>& sizes) {
        return MTensor(sizes);
    }

        void relu(float *data, int len) {
            float min = 0;
            float max = FLT_MAX;
            for (int i = 0; i < len; i++){
                if (data[i] < 0){
                    data[i] = 0;
                }
            }
        }

    void concatenate(float *dst, float *a, float *b, int a_len, int b_len) {
        memcpy(dst, a, a_len * sizeof(float));
        memcpy(dst + a_len, b, b_len * sizeof(float));
    }

    void softmax(float *data, int n) {
        int i,j = 0;
        float max = FLT_MIN;
        float sum = 0;

        for (i = 0; i < n; i++) {

            if (data[i] > max) {
                max = data[i];
            }
        }

        for (i = 0; i < n; i++){
            data[i] = expf(data[i] - max);
        }

        for (i = 0; i < n; i++){
            sum += data[i];
        }

        for (i = 0; i < n; i++){
            data[i] = data[i] / sum;
        }
    }

    /*
        a shape: n_examples, seq_length
        b shape: alphabet_size, embedding_size
        return shape: n_examples, seq_length, embedding_size
     */
    float *embedding(int *a, float *b, int n_examples, int seq_length, int embedding_size) {
        int i, j, k, val;
        float *res = (float *) malloc(sizeof(float) * (n_examples * seq_length * embedding_size));
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

    float* mull(float *a, float *b, int m, int n, int p) {
        float* res = (float *)malloc(sizeof(float) * (m * p));
        for (int i = 0; i < m; i++){
            for (int j = 0; j < p; j++){
                res[i * p + j] = 0;
                for (int k = 0; k < n; k++) {
                    res[i * p + j] += a[i * n + k] * b[k * p + j];
                }
            }
        }
        return res;
    }

    /*
        a shape: n_examples, in_vector_size
        b shape: n_examples, out_vector_size
        c shape: out_vector_size
        return shape: n_examples, out_vector_size
     */
    float *dense(float *a, float *b, float *c, int n_examples, int in_vector_size, int out_vector_size) {
        int i, j;
        float *m_res = mull(a, b, n_examples, in_vector_size, out_vector_size);
        for (i = 0; i < n_examples; i++) {
            for (j = 0; j < out_vector_size; j++) {
                m_res[i * out_vector_size + j] += c[j];
            }
        }
        return m_res;
    }


    /*
        x shape: n_examples, seq_len, input_size
        w shape: kernel_size, input_size, output_size
        return shape: n_examples, seq_len - kernel_size + 1, output_size
     */
    float *conv1D(float *x, float *w, int n_examples, int seq_len, int input_size, int kernel_size,
                  int output_size) {
        int n, o, i, j, k, p;
        float sum;
        float *res = (float *) malloc(
                sizeof(float) * (n_examples * (seq_len - kernel_size + 1) * output_size));
        float *temp_x = (float *) malloc(sizeof(float) * (kernel_size * input_size));
        float *temp_w = (float *) malloc(sizeof(float) * (kernel_size * input_size));
        for (n = 0; n < n_examples; n++) {
            for (o = 0; o < output_size; o++) {
                for (i = 0; i < seq_len - kernel_size + 1; i++) {
                    for (j = i; j < i + kernel_size; j++) {
                        for (k = 0; k < input_size; k++) {
                            temp_x[(j - i) * input_size + k] = x[n * (seq_len * input_size) +
                                                                 j * input_size + k];
                        }
                    }
                    sum = 0;
                    for (p = 0; p < kernel_size * input_size; p++) {
                        temp_w[p] = w[p * output_size + o];
                        sum += temp_x[p] * temp_w[p];
                    }
                    res[(n * (output_size * (seq_len - kernel_size + 1)) + i * output_size +
                         o)] = sum;
                }
            }
        }
        free(temp_x);
        free(temp_w);
        return res;
    }

    /*
       x shape: n_examples, length, n_channel
       return shape: n_examples, length - pool_size + 1, n_channel
    */
    float *maxPool1D(float *x, int rows, int cols, int pool_size) {
        int len = rows - pool_size + 1;
        float *res = (float *)calloc(len * cols, sizeof(float));

        for (int c = 0; c < cols; c++) {
            for (int i = 0; i < len; i++) {
                for (int r = i; r < i + pool_size; r++) {
                    res[i * cols + c] = std::max(res[i * cols + c], x[r * cols + c]);
                }
            }
        }

        return res;
    }

    int *vectorize(const char *texts, int str_len, int max_len) {
        int *res = (int *) malloc(sizeof(int) * max_len);
        for (int i = 0; i < max_len; i++) {
            if (i < str_len) {
                res[i] = (int) texts[i];
            } else {
                res[i] = 0;
            }
        }
        return res;
    }

    float *expand(float *input, int n, int m, int c, int new_c) {
        float *res = (float *) malloc(sizeof(float) * m * n * new_c);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < new_c; k++) {
                    if (k < c) {
                        res[i * m * new_c + j * new_c + k] = input[i * m * c + j * c + k];
                    } else {
                        res[i * m * new_c + j * new_c + k] = 0;
                    }
                }
            }
        }
        return res;
    }

    float *slice(float *input, int row, int col) {
        float *first_column = (float *) malloc(sizeof(float) * row);
        for (int i = 0; i < row; i++) {
            first_column[i] = input[i * col];
        }
        return first_column;
    }

    /*
       input shape: m, n
       return shape: n, m
    */
    float *transpose2D(float *input, int m, int n) {
        float *transposed = (float *) malloc(sizeof(float) * m * n);
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
    float *transpose3D(float *input, int m, int n, int p) {
        float *transposed = (float *) malloc(sizeof(float) * m * n * p);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    transposed[k * m * n + j * m + i] = input[i * n * p + j * p + k];
                }
            }
        }
        return transposed;
    }

    float *add(float *a, float *b, int m, int n, int p) {
        for (int i = 0; i < m * n; i++) {
            for (int j = 0; j < p; j++) {
                a[i * p + j] += b[j];
            }
        }
        return a;
    }

    float *predictOnText(const char *texts, std::unordered_map <std::string, mat::MTensor> &weights,
                         float *df) {
        int *x;
        float *embed_x;
        float *dense1_x;
        float *dense2_x;
        float *dense3_x;
        float *c1;
        float *c2;
        float *c3;
        float *ca;
        float *cb;
        float *cc;

        mat::MTensor &embed_t = weights.at("embed.weight");
        mat::MTensor &conv1w_t = weights.at("convs.0.weight"); // (32, 64, 2)
        mat::MTensor &conv2w_t = weights.at("convs.1.weight");
        mat::MTensor &conv3w_t = weights.at("convs.2.weight");
        mat::MTensor &conv1b_t = weights.at("convs.0.bias");
        mat::MTensor &conv2b_t = weights.at("convs.1.bias");
        mat::MTensor &conv3b_t = weights.at("convs.2.bias");
        mat::MTensor &fc1w_t = weights.at("fc1.weight"); // (128, 126)
        mat::MTensor &fc1b_t = weights.at("fc1.bias");  // 128
        mat::MTensor &fc2w_t = weights.at("fc2.weight"); // (64, 128)
        mat::MTensor &fc2b_t = weights.at("fc2.bias"); // 64
        mat::MTensor &fc3w_t = weights.at("fc3.weight"); // (2, 64) or (4, 64)
        mat::MTensor &fc3b_t = weights.at("fc3.bias"); // 2 or 4

        float *embed_weight = embed_t.data<float>();
        float *convs_0_weight = transpose3D(conv1w_t.data<float>(), conv1w_t.size(0),
                                            conv1w_t.size(1), conv1w_t.size(2)); // (2, 64, 32)
        float *convs_1_weight = transpose3D(conv2w_t.data<float>(), conv2w_t.size(0),
                                            conv2w_t.size(1), conv2w_t.size(2));
        float *convs_2_weight = transpose3D(conv3w_t.data<float>(), conv3w_t.size(0),
                                            conv3w_t.size(1), conv3w_t.size(2));
        float *convs_0_bias = conv1b_t.data<float>();
        float *convs_1_bias = conv2b_t.data<float>();
        float *convs_2_bias = conv3b_t.data<float>();
        float *fc1_weight = transpose2D(fc1w_t.data<float>(), fc1w_t.size(0), fc1w_t.size(1));
        float *fc2_weight = transpose2D(fc2w_t.data<float>(), fc2w_t.size(0), fc2w_t.size(1));
        float *fc3_weight = transpose2D(fc3w_t.data<float>(), fc3w_t.size(0), fc3w_t.size(1));
        float *fc1_bias = fc1b_t.data<float>();
        float *fc2_bias = fc2b_t.data<float>();
        float *fc3_bias = fc3b_t.data<float>();

        // vectorize text
        x = vectorize(texts, (int) strlen(texts), SEQ_LEN);

        // embedding
        embed_x = embedding(x, embed_weight, 1, SEQ_LEN, EMBEDDING_SIZE); // (1, 128, 64)
        free(x);

        // conv1D
        c1 = conv1D(embed_x, convs_0_weight, 1, SEQ_LEN, EMBEDDING_SIZE, conv1w_t.size(2),
                    conv1w_t.size(0)); // (1, 127, 32)
        c2 = conv1D(embed_x, convs_1_weight, 1, SEQ_LEN, EMBEDDING_SIZE, conv2w_t.size(2),
                    conv2w_t.size(0)); // (1, 126, 32)
        c3 = conv1D(embed_x, convs_2_weight, 1, SEQ_LEN, EMBEDDING_SIZE, conv3w_t.size(2),
                    conv3w_t.size(0)); // (1, 124, 32)
        free(embed_x);

        // add bias
        add(c1, convs_0_bias, 1, SEQ_LEN - conv1w_t.size(2) + 1, conv1w_t.size(0));
        add(c2, convs_1_bias, 1, SEQ_LEN - conv2w_t.size(2) + 1, conv2w_t.size(0));
        add(c3, convs_2_bias, 1, SEQ_LEN - conv3w_t.size(2) + 1, conv3w_t.size(0));

        // relu
        relu(c1, (SEQ_LEN - conv1w_t.size(2) + 1) * conv1w_t.size(0));
        relu(c2, (SEQ_LEN - conv2w_t.size(2) + 1) * conv2w_t.size(0));
        relu(c3, (SEQ_LEN - conv3w_t.size(2) + 1) * conv3w_t.size(0));

        // max pooling
        ca = maxPool1D(c1, SEQ_LEN - conv1w_t.size(2) + 1, conv1w_t.size(0),
                       SEQ_LEN - conv1w_t.size(2) + 1); // (1, 1, 32)
        cb = maxPool1D(c2, SEQ_LEN - conv2w_t.size(2) + 1, conv2w_t.size(0),
                       SEQ_LEN - conv2w_t.size(2) + 1); // (1, 1, 32)
        cc = maxPool1D(c3, SEQ_LEN - conv3w_t.size(2) + 1, conv3w_t.size(0),
                       SEQ_LEN - conv3w_t.size(2) + 1); // (1, 1, 32)
        free(c1);
        free(c2);
        free(c3);

        float ff[30] = {1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0};
        // concatenate
        float *concat = (float *) malloc(
                sizeof(float) * (conv1w_t.size(0) + conv2w_t.size(0) + conv3w_t.size(0) + 30));
        concatenate(concat, ca, cb, conv1w_t.size(0), conv2w_t.size(0));
        concatenate(concat + conv1w_t.size(0) + conv2w_t.size(0), cc, df, conv3w_t.size(0), 30);
        free(ca);
        free(cb);
        free(cc);

        // dense + relu
        dense1_x = dense(concat, fc1_weight, fc1_bias, 1, fc1w_t.size(1), fc1w_t.size(0));
        free(concat);
        relu(dense1_x, fc1b_t.size(0));
        dense2_x = dense(dense1_x, fc2_weight, fc2_bias, 1, fc2w_t.size(1), fc2w_t.size(0));
        relu(dense2_x, fc2b_t.size(0));
        free(dense1_x);
        dense3_x = dense(dense2_x, fc3_weight, fc3_bias, 1, fc3w_t.size(1), fc3w_t.size(0));
        free(dense2_x);
        softmax(dense3_x, fc3b_t.size(0));
        return dense3_x;
    }
} // namespace mat

mat::MTensor get_tensor(JNIEnv *env, jfloatArray floatArray, std::vector<int64_t> shpae) {
    mat::MTensor tensor = mat::mempty(shpae);
    float *data = env->GetFloatArrayElements(floatArray, 0);
    long len = env->GetArrayLength(floatArray);
    float *tensor_data = tensor.data<float>();
    memcpy(tensor_data, data, sizeof(float) * (int)len);

    return tensor;
}

static std::unordered_map<std::string, mat::MTensor> _weights;
static std::unordered_map<std::string, mat::MTensor> _new_weights;

void initialize(JNIEnv *env, jobject obj,
                jfloatArray embedding,
                jfloatArray convs_1_weight, jfloatArray convs_2_weight,
                jfloatArray convs_3_weight, jfloatArray convs_1_bias,
                jfloatArray convs_2_bias, jfloatArray convs_3_bias,
                jfloatArray fc1_weight, jfloatArray fc2_weight,
                jfloatArray fc3_weight, jfloatArray fc1_bias,
                jfloatArray fc2_bias, jfloatArray fc3_bias) {
    _new_weights["embed.weight"] = get_tensor(env, embedding, {256, 64});
    _new_weights["convs.0.weight"] = get_tensor(env, convs_1_weight, {32, 64, 2});
    _new_weights["convs.0.bias"] = get_tensor(env, convs_1_bias, {32});
    _new_weights["convs.1.weight"] = get_tensor(env, convs_2_weight, {32, 64, 3});
    _new_weights["convs.1.bias"] = get_tensor(env, convs_2_bias, {32});
    _new_weights["convs.2.weight"] = get_tensor(env, convs_3_weight, {32, 64, 5});
    _new_weights["convs.2.bias"] = get_tensor(env, convs_3_bias, {32});
    _new_weights["fc1.weight"] = get_tensor(env, fc1_weight, {128, 126});
    _new_weights["fc1.bias"] = get_tensor(env, fc1_bias, {128});
    _new_weights["fc2.weight"] = get_tensor(env, fc2_weight, {64, 128});
    _new_weights["fc2.bias"] = get_tensor(env, fc2_bias, {64});
    _new_weights["fc3.weight"] = get_tensor(env, fc3_weight, {4, 64});
    _new_weights["fc3.bias"] = get_tensor(env, fc3_bias, {4});
}

jfloatArray predict(JNIEnv *env, jobject obj, jstring bytesFeature, jfloatArray denseFeature) {
    const char *bytes = env->GetStringUTFChars(bytesFeature, NULL);
    float *dense_data = env->GetFloatArrayElements(denseFeature, 0);

    // Get bytes tensor
    int bytes_length = strlen(bytes);
    int *bytes_data = (int *)malloc(sizeof(int) * bytes_length);
    memset(bytes_data, 0, sizeof(int) * bytes_length);
    for (int i = 0; i < bytes_length; i++) {
        bytes_data[i] = bytes[i];
    }
    mat::MTensor bytes_tensor = mat::mempty({1, (int64_t)bytes_length});
    int *bytes_tensor_data = bytes_tensor.data<int>();
    memcpy(bytes_tensor_data, bytes_data, sizeof(int) * bytes_length);
    free(bytes_data);

    // Get dense tensor
    mat::MTensor dense_tensor = mat::mempty({1, 30});
    float *dense_tensor_data = dense_tensor.data<float>();
    memcpy(dense_tensor_data, dense_data, sizeof(float) * 30);

    std::string output = "";
    std::unordered_map<std::string, float> p_result;
    auto res = mat::predictOnText(bytes, _new_weights, dense_tensor.data<float>());

    jfloatArray result;
    result = env->NewFloatArray(sizeof(res));
    env->SetFloatArrayRegion(result, 0, sizeof(res), res);
    return result;
}