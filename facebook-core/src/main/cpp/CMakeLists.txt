cmake_minimum_required(VERSION 3.4.1)

add_library(cpufeatures STATIC
        ${ANDROID_NDK}/sources/android/cpufeatures/cpu-features.c)

if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "armeabi")
    set(neon_SRCS neon_predictor.cpp)
    set_property(SOURCE ${neon_SRCS}
            APPEND_STRING PROPERTY COMPILE_FLAGS " -mfpu=neon")
    add_definitions("-DHAVE_NEON=1")
else()
    set(neon_SRCS)
endif ()

add_library(
        facebook-core-model-runtime
        SHARED
        model_runtime.cpp
)
target_include_directories(
        facebook-core-model-runtime PRIVATE
        ${ANDROID_NDK}/sources/android/cpufeatures)

target_link_libraries(
        facebook-core-model-runtime
        cpufeatures
)