#include <jni.h>
#include <android/log.h>
#include <gst/gst.h>

JavaVM *java_vm;

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	java_vm = vm;

	return JNI_VERSION_1_4;
}

JNIEXPORT jstring JNICALL Java_kr_ac_kpu_cctvmanager_MainActivity_getGstreamerVersion(JNIEnv *env, jobject thiz) {
	char *version_utf8 = gst_version_string();
	jstring *version_jstring = (*env)->NewStringUTF(env, version_utf8);
	g_free(version_utf8);
	return version_jstring;
}
