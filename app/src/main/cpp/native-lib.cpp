#include <jni.h>
#include <string>
#include <vector>
#include <stdio.h>
#include "Scan.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_czy_jni_MainActivity_scan(//Java_com_czy_jni_MediaProvider_stringFromJNI    Java_com_czy_jni_MainActivity_stringFromJNI
        JNIEnv *env, jobject thiz, jstring scanPath) {//Java_com_czy_jni_MediaProvider_stringFromJNI
    const char *path =NULL;
    path = env->GetStringUTFChars(scanPath, 0);
    android::Scan *scan = new android::Scan();
    scan->ProcessDirectory("/sdcard/android_ubuntu/不同媒体类型", true /*first scan*/);
    scan->ProcessDirectory("/sdcard/android_ubuntu/不同媒体类型", false);
    delete(scan);
    env->ReleaseStringUTFChars(scanPath, path);
    return NULL;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_czy_jni_MediaProvider_scan(//Java_com_czy_jni_MediaProvider_stringFromJNI    Java_com_czy_jni_MainActivity_stringFromJNI
        JNIEnv *env, jobject thiz, jstring scanPath) {//Java_com_czy_jni_MediaProvider_stringFromJNI
    const char *path =NULL;
    path = env->GetStringUTFChars(scanPath, 0);
    android::Scan *scan = new android::Scan();
    scan->ProcessDirectory("/sdcard/android_ubuntu/不同媒体类型", true /*first scan*/);
    scan->ProcessDirectory("/sdcard/android_ubuntu/不同媒体类型", false);
    delete(scan);
    env->ReleaseStringUTFChars(scanPath, path);
    return NULL;
}
