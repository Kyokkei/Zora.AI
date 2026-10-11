package com.yozora.aichat.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEnvelopeTest {

    private val epsilon = 0.0001f

    // ~30 Hz UI cadence.
    private val deltaMs = 1000f / 30f

    // ---- PCM16 helpers ------------------------------------------------------

    private fun pcm16(samples: IntArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = samples[i]
            out[i * 2] = (v and 0xFF).toByte()
            out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }

    /** Full-scale square wave: alternating +amp / -amp, RMS ~= amp / 32768. */
    private fun loudTone(frameCount: Int, amplitude: Int = 32767): IntArray {
        val samples = IntArray(frameCount)
        for (i in samples.indices) {
            samples[i] = if (i % 2 == 0) amplitude else -amplitude
        }
        return samples
    }

    private fun silence(frameCount: Int): IntArray = IntArray(frameCount)

    private fun constant(frameCount: Int, value: Int): IntArray = IntArray(frameCount) { value }

    /** Advance the UI clock, returning the final smoothed mouth value. */
    private fun settle(env: AudioEnvelope, frame: Long, steps: Int, dt: Float = deltaMs): Float {
        var v = 0f
        repeat(steps) { v = env.mouthAt(frame, dt) }
        return v
    }

    // ---- Tests --------------------------------------------------------------

    @Test
    fun silenceInputReturnsZeroMouth() {
        val env = AudioEnvelope()
        val windows = 10
        env.submitChunk(pcm16(silence(windows * 480)), 0L)
        assertEquals(0f, settle(env, frame = 2400, steps = 10), epsilon)
    }

    @Test
    fun loudFullScaleToneOpensTheMouth() {
        val env = AudioEnvelope()
        val windows = 10
        env.submitChunk(pcm16(loudTone(windows * 480)), 0L)
        val settled = settle(env, frame = 2400, steps = 20)
        assertTrue("expected > 0.7 but was $settled", settled > 0.7f)
    }

    @Test
    fun mouthDoesNotJumpInstantlyOnLoudInput() {
        val env = AudioEnvelope()
        val windows = 10
        env.submitChunk(pcm16(loudTone(windows * 480)), 0L)

        // A single step must only move part-way toward the ~1.0 target (attack smoothing).
        val firstStep = env.mouthAt(2400, deltaMs)
        assertTrue("expected partial rise in (0, 0.7) but was $firstStep", firstStep > 0f && firstStep < 0.7f)

        // After enough time it converges high.
        val settled = settle(env, frame = 2400, steps = 20)
        assertTrue("expected > 0.7 but was $settled", settled > 0.7f)
    }

    @Test
    fun resetAfterLoudInputReturnsZero() {
        val env = AudioEnvelope()
        val windows = 10
        env.submitChunk(pcm16(loudTone(windows * 480)), 0L)
        val loud = settle(env, frame = 2400, steps = 20)
        assertTrue("expected loud value > 0.7 but was $loud", loud > 0.7f)

        env.reset()
        assertEquals(0f, env.mouthAt(2400, deltaMs), epsilon)
    }

    @Test
    fun signalBelowNoiseGateIsGatedToZero() {
        val env = AudioEnvelope()
        val windows = 10
        // amplitude 100 => RMS 100/32768 ~= 0.003 < 0.01 noise gate.
        env.submitChunk(pcm16(constant(windows * 480, 100)), 0L)
        assertEquals(0f, settle(env, frame = 2400, steps = 10), epsilon)
    }

    @Test
    fun outOfRangeFrameLookupsReturnZeroWithoutCrashing() {
        val env = AudioEnvelope()
        // No data submitted yet.
        assertEquals(0f, env.mouthAt(0L, deltaMs), epsilon)
        assertEquals(0f, env.mouthAt(-1L, deltaMs), epsilon)

        val windows = 10
        env.submitChunk(pcm16(loudTone(windows * 480)), 0L)

        // Negative frame.
        assertEquals(0f, env.mouthAt(-480L, deltaMs), epsilon)
        // Far beyond the written data.
        assertEquals(0f, env.mouthAt(Long.MAX_VALUE / 2, deltaMs), epsilon)
        // Just past the last written window.
        assertEquals(0f, env.mouthAt((windows * 480).toLong(), deltaMs), epsilon)
    }

    @Test
    fun ringBufferWraparoundKeepsRecentWindowsAndEvictsOld() {
        val env = AudioEnvelope() // ringBufferSize = 512 windows
        val totalWindows = 600 // > ringBufferSize
        env.submitChunk(pcm16(loudTone(totalWindows * 480)), 0L)

        // Most recent window (index 599) is still present and loud.
        val recent = settle(env, frame = (599L * 480), steps = 20)
        assertTrue("expected recent window loud > 0.7 but was $recent", recent > 0.7f)

        // A very old window (index 0) has been evicted from the ring buffer -> target 0, so the
        // smoothed mouth decays to 0 (no crash). Release is ~0.28/step; 40 steps -> ~0.
        val evicted = settle(env, frame = 0L, steps = 40)
        assertEquals(0f, evicted, epsilon)
    }

    @Test
    fun releaseDecaysGraduallyTowardZero() {
        val env = AudioEnvelope()
        val windows = 10
        env.submitChunk(pcm16(loudTone(windows * 480)), 0L)
        val loud = settle(env, frame = 2400, steps = 20)
        assertTrue("expected > 0.7 but was $loud", loud > 0.7f)

        // Jump the playback head beyond the written data so the target drops to 0.
        val afterOneStep = env.mouthAt((windows * 480).toLong(), deltaMs)
        assertTrue("release should not drop instantly, was $afterOneStep", afterOneStep > 0f)
        assertTrue("release should be decreasing, was $afterOneStep >= $loud", afterOneStep < loud)

        val decayed = settle(env, frame = (windows * 480).toLong(), steps = 30)
        assertEquals(0f, decayed, epsilon)
    }
}
