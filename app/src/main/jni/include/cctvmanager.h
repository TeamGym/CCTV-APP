#ifndef _CCTVMANAGER_H
#define _CCTVMANAGER_H

#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>

struct play_data {
    jobject app;
    const gchar *url;
    GstElement *pipeline;
    GstElement *video_sink;
    GMainLoop *main_loop;
    gboolean initialized;
    ANativeWindow *native_window;
};

#if GLIB_SIZEOF_VOID_P == 8
# define set_play_data(env, thiz, field_id, data) \
    (*env)->SetLongField(env, thiz, field_id, (jlong)data)
# define get_play_data(env, thiz, field_id) \
    (struct play_data *)(*env)->GetLongField(env, thiz, field_id)
#else
# define set_play_data(env, thiz, field_id, data) \
    (*env)->SetLongField(env, thiz, field_id, (jlong)(jint)data)
# define get_play_data(env, thiz, field_id) \
    (struct play_data *)(jint)(*env)->GetLongField(env, thiz, field_id)
#endif

#define DEBUG(FMT, ...)                                                            \
    do {                                                                           \
        GST_DEBUG(FMT, ##__VA_ARGS__);                                             \
        __android_log_print(ANDROID_LOG_DEBUG, "cctvmanager_native", FMT, ##__VA_ARGS__); \
    } while (0)

#endif /* _CCTVMANAGER_H */