#include "pch.h"
#include "com_github_coderodde_util_FastBitCount.h"
#include <intrin.h>

JNIEXPORT jint JNICALL Java_com_github_coderodde_util_FastBitCount_popcnt(JNIEnv*, jclass, jlong value) {
    return (jint) __popcnt64(value);
}