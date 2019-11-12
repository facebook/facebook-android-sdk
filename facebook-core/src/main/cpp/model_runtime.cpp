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
#if defined(HAVE_NEON)
#include "neon_predictor.cpp"
    #include <cpu-features.h>
#endif

void initializeWeights(JNIEnv *env, jobject obj,
                       jfloatArray embedding,
                       jfloatArray convs_1_weight, jfloatArray convs_2_weight,
                       jfloatArray convs_3_weight, jfloatArray convs_1_bias,
                       jfloatArray convs_2_bias, jfloatArray convs_3_bias,
                       jfloatArray fc1_weight, jfloatArray fc2_weight,
                       jfloatArray fc3_weight, jfloatArray fc1_bias,
                       jfloatArray fc2_bias, jfloatArray fc3_bias) {
    #if defined(HAVE_NEON)
        initialize(env, obj, embedding, convs_1_weight, convs_2_weight, convs_3_weight, convs_1_bias, convs_2_bias, convs_3_bias, fc1_weight, fc2_weight, fc3_weight, fc1_bias, fc2_bias, fc3_bias);
    #endif
}

jfloatArray predictEvent(JNIEnv *env, jobject obj, jstring bytesFeature, jfloatArray denseFeature) {
    #if defined(HAVE_NEON)
        return predict(env, obj, bytesFeature, denseFeature);
    #else
        return (env)->NewFloatArray(0);
    #endif
}

jboolean hasNeon() {
    #if defined(HAVE_NEON)
        return ((android_getCpuFamily() == ANDROID_CPU_FAMILY_ARM64) || (android_getCpuFamily() == ANDROID_CPU_FAMILY_ARM))
                    && (android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON);
    #else
        return JNI_FALSE;
    #endif
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Find your class. JNI_OnLoad is called from the correct class loader context for this to work.
    jclass c = env->FindClass("com/facebook/appevents/ml/InferencerWrapper");
    if (c == nullptr) return JNI_ERR;

    // Register your class' native methods.
    static const JNINativeMethod methods[] = {
            {"predict", "(Ljava/lang/String;[F)[F", reinterpret_cast<void *>(predictEvent)},
            {"initializeWeights", "([F[F[F[F[F[F[F[F[F[F[F[F[F)V", reinterpret_cast<void *>(initializeWeights)},
            {"hasNeon", "()Z", reinterpret_cast<void *>(hasNeon)},
    };
    int rc = env->RegisterNatives(c, methods, sizeof(methods)/sizeof(JNINativeMethod));
    if (rc != JNI_OK) return rc;

    return JNI_VERSION_1_6;
}
