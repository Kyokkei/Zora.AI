package com.yozora.aichat.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class GyroParallaxMathTest {
    @Test
    fun shortestAngleDeltaWrapsAcrossPositiveAndNegativePi() {
        val epsilon = 0.1f
        assertEquals(
            0.2f,
            shortestAngleDeltaRadians(-PI.toFloat() + epsilon, PI.toFloat() - epsilon),
            0.0001f
        )
        assertEquals(
            -0.2f,
            shortestAngleDeltaRadians(PI.toFloat() - epsilon, -PI.toFloat() + epsilon),
            0.0001f
        )
        assertEquals(PI.toFloat(), shortestAngleDeltaRadians(PI.toFloat(), 0f), 0.0001f)
        assertEquals(-PI.toFloat(), shortestAngleDeltaRadians(-PI.toFloat(), 0f), 0.0001f)
    }

    @Test
    fun nonZeroAbsolutePostureCalibratesToZeroRelativeOffset() {
        val baselinePitch = 1.2f
        val baselineRoll = -0.8f
        val calibrated = gyroOffsetFromAngles(
            pitchRadians = baselinePitch,
            rollRadians = baselineRoll,
            baselinePitchRadians = baselinePitch,
            baselineRollRadians = baselineRoll
        )

        assertEquals(0f, calibrated.x, 0.0001f)
        assertEquals(0f, calibrated.y, 0.0001f)
    }

    @Test
    fun relativeTiltNormalizesTheShortestDeltaAndClamps() {
        val fiveDegrees = (5f * PI.toFloat() / 180f)
        val offset = gyroOffsetFromAngles(
            pitchRadians = 1.2f + fiveDegrees,
            rollRadians = -0.8f - fiveDegrees,
            baselinePitchRadians = 1.2f,
            baselineRollRadians = -0.8f
        )

        assertEquals(-5f / 12f, offset.x, 0.0001f)
        assertEquals(5f / 12f, offset.y, 0.0001f)
        assertEquals(1f, normalizeGyroDeltaRadians(PI.toFloat(), 0f), 0.0001f)
    }

    @Test
    fun smoothingMovesPartWayTowardRawValue() {
        assertEquals(0.15f, smoothGyroValue(0f, 1f), 0.0001f)
        assertEquals(0.85f, smoothGyroValue(1f, 0f), 0.0001f)
    }

    @Test
    fun invalidTiltInputsReturnNeutralOffsetValue() {
        assertEquals(0f, shortestAngleDeltaRadians(Float.NaN, 1f), 0.0001f)
        assertEquals(0f, shortestAngleDeltaRadians(1f, Float.POSITIVE_INFINITY), 0.0001f)
        assertEquals(0f, normalizeGyroDeltaRadians(1f, 0f, maxTiltDegrees = 0f), 0.0001f)
    }
}
