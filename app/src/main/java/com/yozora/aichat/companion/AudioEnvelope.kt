package com.yozora.aichat.companion

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Pure-Kotlin audio envelope follower used to drive the Companion's mouth animation from the
 * raw PCM16 audio produced by the live API (Gemini 24 kHz output).
 *
 * This class has ZERO Android dependencies so it can be unit-tested on the JVM without
 * Robolectric or an emulator.
 *
 * Threading model:
 *  - [submitChunk] is called from the audio write thread as each PCM chunk arrives.
 *  - [mouthAt] is called from the UI thread (~30 Hz) using AudioTrack.playbackHeadPosition.
 *  - [reset] may be called from either thread on interrupt / call-end / error.
 *
 * The single shared synchronization point is [headWindowIndex]: it is written *after* the ring
 * buffer slot in [submitChunk] and read *before* the ring buffer slot in [mouthAt]. Because it
 * is [Volatile], its write/read establishes the happens-before edge that makes the preceding
 * ring-buffer write (and the following read) visible across threads.
 *
 * @param sampleRate output sample rate in Hz (Gemini emits 24 kHz).
 * @param windowSamples number of PCM frames per envelope window (~20 ms at 24 kHz => 480).
 * @param ringBufferSize number of envelope windows retained in the ring buffer.
 */
class AudioEnvelope(
    val sampleRate: Int = 24000,
    val windowSamples: Int = 480,
    val ringBufferSize: Int = 512
) {

    // Per-window normalized RMS (0f..1f). Plain array; cross-thread visibility is provided by
    // the [Volatile] [headWindowIndex] handshake rather than by volatile array elements.
    private val rms = FloatArray(ringBufferSize)

    // Number of windows committed so far: windows with index in [0, headWindowIndex) hold data.
    // Declared last in submitChunk so the volatile write publishes the rms[] writes to readers.
    @Volatile
    private var headWindowIndex: Long = 0L

    // Smoothed mouth opening in 0f..1f. Mutated only by mouthAt on the UI thread, snapped to 0 by
    // reset(). Volatile so a reset() from any thread is immediately visible to readers.
    @Volatile
    private var mouth: Float = 0f

    /**
     * Feed one PCM16 (little-endian) chunk to the envelope tracker.
     *
     * @param pcm interleaved 16-bit PCM bytes; [pcm].size / 2 frames are consumed.
     * @param baseFrameOffset cumulative number of frames written before this chunk. The chunk
     *   therefore covers global frames [baseFrameOffset, baseFrameOffset + pcm.size / 2).
     */
    fun submitChunk(pcm: ByteArray, baseFrameOffset: Long) {
        val frameCount = pcm.size / BYTES_PER_SAMPLE
        if (frameCount <= 0) return

        val firstFrame = baseFrameOffset
        val lastFrame = baseFrameOffset + frameCount // exclusive

        // Walk every window that overlaps the chunk. Using global frame math makes this correct
        // for any alignment of baseFrameOffset relative to window boundaries.
        var windowIndex = firstFrame / windowSamples
        while (windowIndex * windowSamples < lastFrame) {
            val windowStart = windowIndex * windowSamples
            val windowEnd = windowStart + windowSamples
            val rangeStart = if (windowStart > firstFrame) windowStart else firstFrame
            val rangeEnd = if (windowEnd < lastFrame) windowEnd else lastFrame

            var sumSquares = 0.0
            var count = 0
            var frame = rangeStart
            while (frame < rangeEnd) {
                val sample = readSample(pcm, (frame - firstFrame).toInt())
                val d = sample.toDouble()
                sumSquares += d * d
                count++
                frame++
            }

            if (count > 0) {
                val value = (sqrt(sumSquares / count) / MAX_AMPLITUDE).toFloat()
                rms[(windowIndex % ringBufferSize).toInt()] = value
                if (windowIndex + 1 > headWindowIndex) {
                    headWindowIndex = windowIndex + 1
                }
            }
            windowIndex++
        }
    }


    /**
     * Read the current mouth 'aa' opening for a playback head position, with attack/release
     * smoothing applied. Safe to call from the UI thread concurrently with [submitChunk].
     *
     * @param playbackHeadFrame AudioTrack.playbackHeadPosition in frames.
     * @param deltaMs milliseconds elapsed since the previous call (drives the smoothing rate).
     * @return the smoothed mouth opening in 0f..1f.
     */
    fun mouthAt(playbackHeadFrame: Long, deltaMs: Float): Float {
        val target = rawTarget(playbackHeadFrame)
        val dt = if (deltaMs.isFinite() && deltaMs > 0f) deltaMs else 0f

        // Attack while rising, release while falling (release is slower => smoother decay).
        val coefficient = if (target >= mouth) {
            1f - exp(-dt / ATTACK_MS)
        } else {
            1f - exp(-dt / RELEASE_MS)
        }

        var next = mouth + (target - mouth) * coefficient
        if (next < 0f) next = 0f
        if (next > 1f) next = 1f
        mouth = next
        return next
    }

    /** Clear all state: ring buffer, indices, and snap the smoothed mouth back to 0. */
    fun reset() {
        rms.fill(0f)
        headWindowIndex = 0L
        mouth = 0f
    }

    /** Raw (pre-smoothing) target for [playbackHeadFrame]; 0 when gated or no data is available. */
    private fun rawTarget(playbackHeadFrame: Long): Float {
        val windowIndex = playbackHeadFrame / windowSamples
        if (windowIndex < 0L) return 0f
        // Not yet written (future) or evicted from the ring buffer (older than the retained span).
        if (windowIndex >= headWindowIndex) return 0f
        if (windowIndex < headWindowIndex - ringBufferSize) return 0f

        val value = rms[(windowIndex % ringBufferSize).toInt()]
        if (value < NOISE_GATE) return 0f
        return value
    }

    /** Decode one little-endian signed 16-bit PCM sample at [sampleIndex]. */
    private fun readSample(pcm: ByteArray, sampleIndex: Int): Int {
        val offset = sampleIndex * BYTES_PER_SAMPLE
        val lo = pcm[offset].toInt() and 0xFF
        val hi = pcm[offset + 1].toInt() // sign-extends the high byte
        return (hi shl 8) or lo
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2      // PCM16
        const val MAX_AMPLITUDE = 32768.0   // signed 16-bit full scale
        const val NOISE_GATE = 0.01f
        const val ATTACK_MS = 50f
        const val RELEASE_MS = 100f
    }
}
