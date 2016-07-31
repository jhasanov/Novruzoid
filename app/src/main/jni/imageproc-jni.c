#include <jni.h>
#include <android/log.h>
#include <stddef.h>

JNIEXPORT jstring JNICALL
Java_andir_novruzoid_ImageProc_getMessageFromJni(JNIEnv *env, jobject instance) {
    return (*env)->NewStringUTF(env, "JNI loves you!");
}

JNIEXPORT jintArray JNICALL
Java_andir_novruzoid_ImageProc_getSegments(JNIEnv *env, jobject instance, void* pixels_, jint w,
                                           jint h) {
    jint *pixels = (*env)->GetIntArrayElements(env, pixels_, NULL);

     __android_log_print("", "Novruzoid-JNI", "Image size %d x %d", w,h);

    (*env)->ReleaseIntArrayElements(env, pixels_, pixels, 0);
}