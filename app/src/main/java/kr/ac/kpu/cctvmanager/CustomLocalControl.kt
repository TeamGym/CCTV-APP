/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.ac.kpu.cctvmanager

import android.os.Handler
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.ExoTrackSelection
import com.google.android.exoplayer2.upstream.Allocator
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.util.PriorityTaskManager

/**
 * The default [LoadControl] implementation.
 */
class CustomLoadControl @JvmOverloads constructor(
    private val allocator: DefaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
    minBufferMs: Int = DEFAULT_MIN_BUFFER_MS,
    maxBufferMs: Int = DEFAULT_MAX_BUFFER_MS,
    bufferForPlaybackMs: Long = DEFAULT_BUFFER_FOR_PLAYBACK_MS.toLong(),
    bufferForPlaybackAfterRebufferMs: Long =
        DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS.toLong(),
    priorityTaskManager: PriorityTaskManager? =
        null
) : LoadControl {
    private var bufferedDurationListener: EventListener? = null
    private var eventHandler: Handler? = null
    private val minBufferUs: Long
    private val maxBufferUs: Long
    private val bufferForPlaybackUs: Long
    private val bufferForPlaybackAfterRebufferUs: Long
    private val priorityTaskManager: PriorityTaskManager?
    private var targetBufferSize = 0
    private var isBuffering = false
    private val backBufferDurationUs = 0L
    private val retainBackBufferFromKeyframe = false

    /**
     * Constructs a new instance, using the `DEFAULT_*` constants defined in this class.
     */
    constructor(listener: EventListener?, handler: Handler?) : this(
        DefaultAllocator(
            true,
            C.DEFAULT_BUFFER_SEGMENT_SIZE
        )
    ) {
        bufferedDurationListener = listener
        eventHandler = handler
    }

    override fun onPrepared() {
        reset(false)
    }

    override fun onTracksSelected(
        renderers: Array<out Renderer>, trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection>
    ) {
        targetBufferSize = 0
        for (i in renderers.indices) {
            if (trackSelections[i] != null) {
                targetBufferSize += getDefaultBufferSize(
                    renderers[i].trackType
                )
                if (renderers[i].trackType == C.TRACK_TYPE_VIDEO) targetBufferSize *= VIDEO_BUFFER_SCALE_UP_FACTOR /*Added by Sri to control buffer size */
            }
        }
        //allocator.setTargetBufferSize(targetBufferSize)
        allocator.setTargetBufferSize(1)
    }

    fun getDefaultBufferSize(trackType: Int): Int {
        return when (trackType) {
            C.TRACK_TYPE_DEFAULT -> DEFAULT_MUXED_BUFFER_SIZE
            C.TRACK_TYPE_AUDIO -> DEFAULT_AUDIO_BUFFER_SIZE
            C.TRACK_TYPE_VIDEO -> DEFAULT_VIDEO_BUFFER_SIZE
            C.TRACK_TYPE_TEXT -> DEFAULT_TEXT_BUFFER_SIZE
            C.TRACK_TYPE_METADATA -> DEFAULT_METADATA_BUFFER_SIZE
            else -> throw IllegalStateException()
        }
    }

    override fun onStopped() {
        reset(true)
    }

    override fun onReleased() {
        reset(true)
    }

    override fun getAllocator(): Allocator {
        return allocator
    }

    override fun getBackBufferDurationUs(): Long {
        return backBufferDurationUs
    }

    override fun retainBackBufferFromKeyframe(): Boolean {
        return retainBackBufferFromKeyframe
    }

    override fun shouldStartPlayback(bufferedDurationUs: Long, playbackSpeed: Float, rebuffering: Boolean, targetLiveOffsetUs: Long): Boolean {
        val minBufferDurationUs =
            if (rebuffering) bufferForPlaybackAfterRebufferUs else bufferForPlaybackUs
        //return minBufferDurationUs <= 0 || bufferedDurationUs >= minBufferDurationUs
        return true
    }

    override fun shouldContinueLoading(playbackPositionUs: Long, bufferedDurationUs: Long, playbackSpeed: Float): Boolean {
        val bufferTimeState = getBufferTimeState(bufferedDurationUs)
        val targetBufferSizeReached = allocator.totalBytesAllocated >= targetBufferSize
        val wasBuffering = isBuffering
        isBuffering = (bufferTimeState == BELOW_LOW_WATERMARK
                || (bufferTimeState == BETWEEN_WATERMARKS /*
             * commented below line to achieve drip-feeding method for better caching. once you are below maxBufferUs, do fetch immediately.
             * Added by Sri
             */
                /* && isBuffering */
                && !targetBufferSizeReached))
        if (priorityTaskManager != null && isBuffering != wasBuffering) {
            if (isBuffering) {
                //priorityTaskManager.add(LOADING_PRIORITY)
            } else {
                //priorityTaskManager.remove(LOADING_PRIORITY)
            }
        }
        if (null != bufferedDurationListener && null != eventHandler) eventHandler!!.post {
            bufferedDurationListener!!.onBufferedDurationSample(
                bufferedDurationUs
            )
        }
        //    Log.e("DLC","current buff Dur: "+bufferedDurationUs+",max buff:" + maxBufferUs +" shouldContinueLoading: "+isBuffering);
        //return isBuffering
        return false
    }

    private fun getBufferTimeState(bufferedDurationUs: Long): Int {
        return if (bufferedDurationUs > maxBufferUs) ABOVE_HIGH_WATERMARK else if (bufferedDurationUs < minBufferUs) BELOW_LOW_WATERMARK else BETWEEN_WATERMARKS
    }

    private fun reset(resetAllocator: Boolean) {
        targetBufferSize = 0
        if (priorityTaskManager != null && isBuffering) {
            priorityTaskManager.remove(LOADING_PRIORITY)
        }
        isBuffering = false
        if (resetAllocator) {
            allocator.reset()
        }
    }

    interface EventListener {
        fun onBufferedDurationSample(bufferedDurationUs: Long)
    }

    companion object {

        /**
         * The default target buffer size in bytes. The value ([C.LENGTH_UNSET]) means that the load
         * control will calculate the target buffer size based on the selected tracks.
         */
        const val DEFAULT_TARGET_BUFFER_BYTES = C.LENGTH_UNSET

        /** The default prioritization of buffer time constraints over size constraints.  */
        const val DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS = false

        /** The default back buffer duration in milliseconds.  */
        const val DEFAULT_BACK_BUFFER_DURATION_MS = 0

        /** The default for whether the back buffer is retained from the previous keyframe.  */
        const val DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME = false

        /** A default size in bytes for a video buffer.  */
        const val DEFAULT_VIDEO_BUFFER_SIZE = 2000 * C.DEFAULT_BUFFER_SEGMENT_SIZE
        //const val DEFAULT_VIDEO_BUFFER_SIZE = 100 * C.DEFAULT_BUFFER_SEGMENT_SIZE

        /** A default size in bytes for an audio buffer.  */
        const val DEFAULT_AUDIO_BUFFER_SIZE = 200 * C.DEFAULT_BUFFER_SEGMENT_SIZE

        /** A default size in bytes for a text buffer.  */
        const val DEFAULT_TEXT_BUFFER_SIZE = 2 * C.DEFAULT_BUFFER_SEGMENT_SIZE

        /** A default size in bytes for a metadata buffer.  */
        const val DEFAULT_METADATA_BUFFER_SIZE = 2 * C.DEFAULT_BUFFER_SEGMENT_SIZE

        /** A default size in bytes for a camera motion buffer.  */
        const val DEFAULT_CAMERA_MOTION_BUFFER_SIZE = 2 * C.DEFAULT_BUFFER_SEGMENT_SIZE

        /** A default size in bytes for an image buffer.  */
        const val DEFAULT_IMAGE_BUFFER_SIZE = 2 * C.DEFAULT_BUFFER_SEGMENT_SIZE

        /** A default size in bytes for a muxed buffer (e.g. containing video, audio and text).  */
        const val DEFAULT_MUXED_BUFFER_SIZE =
            DEFAULT_VIDEO_BUFFER_SIZE + DEFAULT_AUDIO_BUFFER_SIZE + DEFAULT_TEXT_BUFFER_SIZE

        /**
         * The buffer size in bytes that will be used as a minimum target buffer in all cases. This is
         * also the default target buffer before tracks are selected.
         */
        const val DEFAULT_MIN_BUFFER_SIZE = 200 * C.DEFAULT_BUFFER_SEGMENT_SIZE
        /**
         * The default minimum duration of media that the player will attempt to ensure is buffered at all
         * times, in milliseconds.
         */
        const val DEFAULT_MIN_BUFFER_MS = 1000

        /**
         * The default maximum duration of media that the player will attempt to buffer, in milliseconds.
         */
        const val DEFAULT_MAX_BUFFER_MS = 1000

        /**
         * The default duration of media that must be buffered for playback to start or resume following a
         * user action such as a seek, in milliseconds.
         */
        const val DEFAULT_BUFFER_FOR_PLAYBACK_MS = 0

        /**
         * The default duration of media that must be buffered for playback to resume after a rebuffer,
         * in milliseconds. A rebuffer is defined to be caused by buffer depletion rather than a user
         * action.
         */
        const val DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 0

        /**
         * To increase buffer time and size.
         * Added by Sri
         */
        var VIDEO_BUFFER_SCALE_UP_FACTOR = 4

        /**
         * Priority for media loading.
         */
        const val LOADING_PRIORITY = 0
        private const val ABOVE_HIGH_WATERMARK = 0
        private const val BETWEEN_WATERMARKS = 1
        private const val BELOW_LOW_WATERMARK = 2
    }
    /**
     * Constructs a new instance.
     *
     * @param allocator The [DefaultAllocator] used by the loader.
     * @param minBufferMs The minimum duration of media that the player will attempt to ensure is
     * buffered at all times, in milliseconds.
     * @param maxBufferMs The maximum duration of media that the player will attempt buffer, in
     * milliseconds.
     * @param bufferForPlaybackMs The duration of media that must be buffered for playback to start or
     * resume following a user action such as a seek, in milliseconds.
     * @param bufferForPlaybackAfterRebufferMs The default duration of media that must be buffered for
     * playback to resume after a rebuffer, in milliseconds. A rebuffer is defined to be caused by
     * buffer depletion rather than a user action.
     * @param priorityTaskManager If not null, registers itself as a task with priority
     * [.LOADING_PRIORITY] during loading periods, and unregisters itself during draining
     * periods.
     */
    /**
     * Constructs a new instance.
     *
     * @param allocator The [DefaultAllocator] used by the loader.
     * @param minBufferMs The minimum duration of media that the player will attempt to ensure is
     * buffered at all times, in milliseconds.
     * @param maxBufferMs The maximum duration of media that the player will attempt buffer, in
     * milliseconds.
     * @param bufferForPlaybackMs The duration of media that must be buffered for playback to start or
     * resume following a user action such as a seek, in milliseconds.
     * @param bufferForPlaybackAfterRebufferMs The default duration of media that must be buffered for
     * playback to resume after a rebuffer, in milliseconds. A rebuffer is defined to be caused by
     * buffer depletion rather than a user action.
     */
    /**
     * Constructs a new instance, using the `DEFAULT_*` constants defined in this class.
     *
     * @param allocator The [DefaultAllocator] used by the loader.
     */
    /**
     * Constructs a new instance, using the `DEFAULT_*` constants defined in this class.
     */
    init {
        minBufferUs =
            VIDEO_BUFFER_SCALE_UP_FACTOR /*Added by Sri to control buffer size */ * minBufferMs * 1000L
        maxBufferUs =
            VIDEO_BUFFER_SCALE_UP_FACTOR /*Added by Sri to control buffer size */ * maxBufferMs * 1000L
        bufferForPlaybackUs = bufferForPlaybackMs * 1000L
        bufferForPlaybackAfterRebufferUs = bufferForPlaybackAfterRebufferMs * 1000L
        this.priorityTaskManager = priorityTaskManager
    }
}