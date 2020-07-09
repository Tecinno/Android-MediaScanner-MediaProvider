#include <jni.h>
#include <string>
#include <vector>
#include <stdio.h>
#include "Scan.h"

extern "C" JNIEXPORT jintArray JNICALL
Java_com_czy_jni_MainActivity_stringFromJNI(//Java_com_czy_jni_MediaProvider_stringFromJNI    Java_com_czy_jni_MainActivity_stringFromJNI
        JNIEnv *env, jobject thiz, jobjectArray titleArray) {//Java_com_czy_jni_MediaProvider_stringFromJNI
    std::string hello = "Hello from C++";
    int size = 5;
    jintArray jarrp = env->NewIntArray(size);
     jint arrp[size] ;
    for(int i = 0; i < size; i++){
        arrp[i] = i;
    }
     env->SetIntArrayRegion(jarrp, 0, size, arrp);
    android::Scan *scan = new android::Scan();
    if (scan->ProcessDirectory("/data/data/com.czy.jni/") == 0) {///data/data/com.czy.jni/   mnt/sdcard /udisk ///sdcard/android_ubuntu/不同媒体类型
        delete(scan);
        return NULL;
    } else  {
        delete(scan);
        return jarrp;
    }
}

