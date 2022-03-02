#include <jni.h>
#include <gst/gst.h>
#include <gst/video/video.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <pthread.h>
#include "include/cctvmanager.h"

GST_DEBUG_CATEGORY_STATIC(debug_category);
#define GST_CAT_DEFAULT debug_category

extern JavaVM *java_vm;

static pthread_t app_thread;
static pthread_key_t current_jni_env;
static jfieldID play_data_field_id;
static jmethodID on_gstreamer_initialized_method_id;

static JNIEnv *attach_current_thread(void)
{
    JNIEnv *env;
    JavaVMAttachArgs args;
    DEBUG("attach thread %p", g_thread_self());
    args.version = JNI_VERSION_1_4;
    args.name = NULL;
    args.group = NULL;

    if ((*java_vm)->AttachCurrentThread(java_vm, &env, &args) < 0) {
        GST_ERROR("failed to attach current thread");
        return NULL;
    }

    return env;
}

static void detach_current_thread(void *env)
{
    DEBUG("detach thread %p", g_thread_self());
    (*java_vm)->DetachCurrentThread(java_vm);
}

static JNIEnv *get_jni_env()
{
    JNIEnv *env;
    if ((env = pthread_getspecific(current_jni_env)) == NULL) {
        env = attach_current_thread();
        pthread_setspecific(current_jni_env, env);
    }

    return env;
}

static void error_cb(GstBus *bus, GstMessage *msg, struct play_data *data)
{
    GError *err;
    gchar *debug_info;

    gst_message_parse_error(msg, &err, &debug_info);
    DEBUG("error from element %s: %s", GST_OBJECT_NAME(msg->src), err->message);
    g_clear_error(&err);
    g_free(debug_info);
    gst_element_set_state(data->pipeline, GST_STATE_NULL);
}

static void state_changed_cb(GstBus *bus, GstMessage *msg, struct play_data *data)
{
    GstState old_s, new_s, pending_s;
    gst_message_parse_state_changed(msg, &old_s, &new_s, &pending_s);

    if (GST_MESSAGE_SRC(msg) == GST_OBJECT(data->pipeline)) {
        DEBUG("state changed from %s to %s",
                  gst_element_state_get_name(old_s),
                  gst_element_state_get_name(new_s));
    }
}

static void check_initialization_complete(struct play_data *data)
{
    JNIEnv *env = get_jni_env();
    if (!data->initialized && data->native_window && data->main_loop) {
        DEBUG("initialized");

        gst_video_overlay_set_window_handle(GST_VIDEO_OVERLAY(data->video_sink), (guintptr)data->native_window);

        (*env)->CallVoidMethod(env, data->app, on_gstreamer_initialized_method_id);
        if ((*env)->ExceptionCheck(env)) {
            GST_ERROR("failed to call java method");
            (*env)->ExceptionClear(env);
        }
        data->initialized = TRUE;
    }
}

static void *app_main(void *p_data) {
    struct play_data *data = (struct play_data *)p_data;
    GstBus *bus;
    GSource *bus_src;
    GError *err = NULL;

    GMainContext *context = g_main_context_new();
    g_main_context_push_thread_default(context);

    data->pipeline = gst_parse_launch("rtspsrc latency=0 name=rtspsrc0 ! rtph264depay ! h264parse ! amcviddec-omxgoogleh264decoder ! videoconvert ! glimagesink", &err);
    if (err) {
        DEBUG("failed to build pipeline: %s", err->message);
        g_clear_error(&err);
        goto free_pipeline;
    }

    gst_element_set_state(data->pipeline, GST_STATE_READY);

    data->video_sink = gst_bin_get_by_interface(GST_BIN(data->pipeline), GST_TYPE_VIDEO_OVERLAY);
    if (!data->video_sink) {
        GST_ERROR("no video sink");
        return NULL;
    }

    bus = gst_element_get_bus(data->pipeline);
    bus_src = gst_bus_create_watch(bus);
    g_source_set_callback(bus_src, (GSourceFunc) gst_bus_async_signal_func, NULL, NULL);
    g_source_attach(bus_src, context);
    g_source_unref(bus_src);
    g_signal_connect(G_OBJECT(bus), "message::error", (GCallback)error_cb, data);
    g_signal_connect(G_OBJECT(bus), "message::state-changed", (GCallback)state_changed_cb, data);
    gst_object_unref(bus);

    DEBUG("start mainloop data=%p", data);
    data->main_loop = g_main_loop_new(context, FALSE);
    check_initialization_complete(data);
    g_main_loop_run(data->main_loop);
    DEBUG("exit main_loop");
    g_main_loop_unref(data->main_loop);
    data->main_loop = NULL;

    g_main_context_pop_thread_default(context);
    g_main_context_unref(context);
    gst_element_set_state(data->pipeline, GST_STATE_NULL);
free_pipeline:
    gst_object_unref(data->pipeline);
    return NULL;
}

// initialize_class
JNIEXPORT jboolean JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_initializeClass(JNIEnv *env, jclass klass)
{
    play_data_field_id = (*env)->GetFieldID(env, klass, "nativeData", "J");
    on_gstreamer_initialized_method_id = (*env)->GetMethodID(env, klass, "onGstreamerInitialized", "()V");
    pthread_key_create((void *)&env, detach_current_thread);
    if (!play_data_field_id || !on_gstreamer_initialized_method_id) {
        __android_log_print(ANDROID_LOG_ERROR, "CCTVManager", "cannot be initialized");
        return JNI_FALSE;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "CCTVManager", "class initialization complete");
    return JNI_TRUE;
}

// initialize
JNIEXPORT void JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_initialize(JNIEnv *env, jobject thiz)
{
    struct play_data *data = g_new0(struct play_data, 1);
    set_play_data(env, thiz, play_data_field_id, data);
    GST_DEBUG_CATEGORY_INIT(debug_category, "cctvmanager", 0, "CCTVManager");
    gst_debug_set_threshold_for_name("cctvmanager", GST_LEVEL_DEBUG);
    DEBUG("initialize");
    data->app = (*env)->NewGlobalRef(env, thiz);
    pthread_create(&app_thread, NULL, &app_main, data);
}

JNIEXPORT void JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_setPlayUrl0(JNIEnv *env, jobject thiz, jstring url)
{
    GstElement *rtspsrc;
    const gchar *g_url;
    struct play_data *data = get_play_data(env, thiz, play_data_field_id);
    if (!data) {
        DEBUG("no data");
        return;
    }
    g_url = (*env)->GetStringUTFChars(env, url, NULL);
    DEBUG("set url to %s", g_url);
    gst_element_set_state(data->pipeline, GST_STATE_READY);
    rtspsrc = gst_bin_get_by_name(GST_BIN(data->pipeline), "rtspsrc0");
    if (!rtspsrc) {
        DEBUG("rtspsrc null");
        goto free_g_url;
    }
    g_object_set(rtspsrc, "location", g_url, NULL);

    gst_object_unref(rtspsrc);
free_g_url:
    (*env)->ReleaseStringUTFChars(env, url, g_url);
}

JNIEXPORT void JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_play0(JNIEnv *env, jobject thiz)
{
    struct play_data *data = get_play_data(env, thiz, play_data_field_id);
    if (!data)
        return;
    DEBUG("play0");
    gst_element_set_state(data->pipeline, GST_STATE_PLAYING);
}

JNIEXPORT void JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_pause0(JNIEnv *env, jobject thiz)
{
    struct play_data *data = get_play_data(env, thiz, play_data_field_id);
    if (!data)
        return;
    DEBUG("pause0");
    gst_element_set_state(data->pipeline, GST_STATE_PAUSED);
}

JNIEXPORT void JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_destroy0(JNIEnv *env, jobject thiz)
{
    struct play_data *data = get_play_data(env, thiz, play_data_field_id);
    if (!data)
        return;
    DEBUG("destroy0");
    g_main_loop_quit(data->main_loop);
    DEBUG("wait until thread finish");
    pthread_join(app_thread, NULL);
    (*env)->DeleteGlobalRef(env, data->app);
    g_free(data);
    set_play_data(env, thiz, play_data_field_id, NULL);
    DEBUG("destroy0 done");
}

JNIEXPORT void JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_surfaceInit0(JNIEnv *env, jobject thiz, jobject surface)
{
    struct play_data *data = get_play_data(env, thiz, play_data_field_id);
    if (!data)
        return;
    DEBUG("surfaceInit");
    __android_log_print(ANDROID_LOG_DEBUG, "CCTVManager", "surfaceInit");
    ANativeWindow *new_window = ANativeWindow_fromSurface(env, surface);
    if (data->native_window) {
        ANativeWindow_release(data->native_window);
        if (data->native_window == new_window) {
            if (data->video_sink) {
                gst_video_overlay_expose(GST_VIDEO_OVERLAY(data->video_sink));
                gst_video_overlay_expose(GST_VIDEO_OVERLAY(data->video_sink));
            }
            return;
        } else {
            data->initialized = FALSE;
        }
    }
    data->native_window = new_window;

    check_initialization_complete(data);
}

JNIEXPORT void JNICALL Java_kr_ac_kpu_cctvmanager_view_GstSurfaceView_surfaceDestroy0(JNIEnv *env, jobject thiz)
{
    struct play_data *data = get_play_data(env, thiz, play_data_field_id);
    if (!data)
        return;
    DEBUG("surfaceDestroy");

    if (data->video_sink) {
        gst_video_overlay_set_window_handle(GST_VIDEO_OVERLAY(data->video_sink), (guintptr)NULL);
        gst_element_set_state(data->pipeline, GST_STATE_READY);
    }

    ANativeWindow_release(data->native_window);
    data->native_window = NULL;
    data->initialized = FALSE;
}
