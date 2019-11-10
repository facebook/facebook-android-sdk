package com.facebook.appevents.ml;

public final class InferencerWrapper {
    static void initialize() {
        System.loadLibrary("facebook-core-model-runtime");
    }
    static native String predict(String bytes, float[] dense);
    static native void initializeWeights(float[] embedding,
                                                 float[] convs_1_weight, float[] convs_2_weight,
                                                 float[] convs_3_weight, float[] convs_1_bias,
                                                 float[] convs_2_bias, float[] convs_3_bias,
                                                 float[] fc1_weight, float[] fc2_weight,
                                                 float[] fc3__weight, float[] fc1_bias,
                                                 float[] fc2_bias, float[] fc3_bias);
    static native boolean hasNeon();
}
