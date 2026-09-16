package com.yozora.aichat.ui.chat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.PI

internal const val GYRO_MAX_TILT_DEGREES = 12f
internal const val GYRO_SMOOTHING_ALPHA = 0.15f
internal const val GYRO_OUTPUT_DEADBAND = 0.01f
private const val GYRO_SENSOR_PERIOD_US = 33_333
private val PI_F = PI.toFloat()
private val TWO_PI = 2f * PI_F

internal fun smoothGyroValue(
    previous: Float,
    raw: Float,
    alpha: Float = GYRO_SMOOTHING_ALPHA
): Float {
    return previous + (raw - previous) * alpha.coerceIn(0f, 1f)
}

internal fun shortestAngleDeltaRadians(current: Float, baseline: Float): Float {
    if (!current.isFinite() || !baseline.isFinite()) return 0f
    var delta = (current - baseline) % TWO_PI
    if (delta > PI_F) delta -= TWO_PI
    if (delta < -PI_F) delta += TWO_PI
    return delta
}

internal fun normalizeGyroDeltaRadians(
    current: Float,
    baseline: Float,
    maxTiltDegrees: Float = GYRO_MAX_TILT_DEGREES
): Float {
    if (!maxTiltDegrees.isFinite() || maxTiltDegrees <= 0f) return 0f
    val deltaDegrees = shortestAngleDeltaRadians(current, baseline) * 180f / PI.toFloat()
    return (deltaDegrees / maxTiltDegrees).coerceIn(-1f, 1f)
}

internal fun gyroOffsetFromAngles(
    pitchRadians: Float,
    rollRadians: Float,
    baselinePitchRadians: Float,
    baselineRollRadians: Float,
    maxTiltDegrees: Float = GYRO_MAX_TILT_DEGREES
): Offset {
    return Offset(
        x = normalizeGyroDeltaRadians(rollRadians, baselineRollRadians, maxTiltDegrees),
        y = normalizeGyroDeltaRadians(pitchRadians, baselinePitchRadians, maxTiltDegrees)
    )
}

/**
 * Returns a smoothed, clamped phone tilt as an [Offset] in the range -1f..1f.
 * The x axis follows roll and the y axis follows pitch.
 */
@Composable
fun rememberGyroOffset(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val rotationVectorSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }
    val offset = remember { mutableStateOf(Offset.Zero) }
    val exposedOffset = remember(enabled) {
        derivedStateOf { if (enabled) offset.value else Offset.Zero }
    }
    val listener = remember(sensorManager, context) {
        sensorManager?.let { manager ->
            GyroOffsetListener(
                sensorManager = manager,
                displayRotationProvider = { displayRotation(context) },
                onOffset = { offset.value = it }
            )
        }
    }

    DisposableEffect(
        enabled,
        lifecycleOwner,
        sensorManager,
        rotationVectorSensor,
        listener
    ) {
        if (!enabled || sensorManager == null || rotationVectorSensor == null || listener == null) {
            offset.value = Offset.Zero
            onDispose { }
        } else {
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> listener.register(rotationVectorSensor)
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP -> {
                        listener.unregister()
                        offset.value = Offset.Zero
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                listener.register(rotationVectorSensor)
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                listener.unregister()
                offset.value = Offset.Zero
            }
        }
    }

    return exposedOffset
}

private class GyroOffsetListener(
    private val sensorManager: SensorManager,
    private val displayRotationProvider: () -> Int,
    private val onOffset: (Offset) -> Unit
) : SensorEventListener {
    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var baselinePitchRadians: Float? = null
    private var baselineRollRadians: Float? = null
    private var calibratedDisplayRotation: Int? = null
    private var lastEmittedOffset = Offset.Zero
    private var registered = false

    fun register(sensor: Sensor) {
        if (registered) return
        resetState()
        registered = sensorManager.registerListener(this, sensor, GYRO_SENSOR_PERIOD_US)
    }

    fun unregister() {
        if (registered) {
            sensorManager.unregisterListener(this)
            registered = false
        }
        resetState(emitZero = true)
    }

    private fun resetState(emitZero: Boolean = false) {
        val hadEmittedOffset = lastEmittedOffset != Offset.Zero
        smoothedPitch = 0f
        smoothedRoll = 0f
        baselinePitchRadians = null
        baselineRollRadians = null
        calibratedDisplayRotation = null
        lastEmittedOffset = Offset.Zero
        if (emitZero && hadEmittedOffset) onOffset(Offset.Zero)
    }

    private fun publishIfChanged(next: Offset) {
        val stabilized = if (
            kotlin.math.abs(next.x) < GYRO_OUTPUT_DEADBAND &&
            kotlin.math.abs(next.y) < GYRO_OUTPUT_DEADBAND
        ) {
            Offset.Zero
        } else {
            next
        }
        if (
            kotlin.math.abs(stabilized.x - lastEmittedOffset.x) < GYRO_OUTPUT_DEADBAND &&
            kotlin.math.abs(stabilized.y - lastEmittedOffset.y) < GYRO_OUTPUT_DEADBAND
        ) {
            return
        }
        lastEmittedOffset = stabilized
        onOffset(stabilized)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val currentDisplayRotation = displayRotationProvider()
        if (calibratedDisplayRotation != null && calibratedDisplayRotation != currentDisplayRotation) {
            resetState(emitZero = true)
        }

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val matrix = remapForDisplayRotation(
            input = rotationMatrix,
            output = remappedRotationMatrix,
            displayRotation = currentDisplayRotation
        )
        SensorManager.getOrientation(matrix, orientation)

        val currentPitchRadians = orientation[1]
        val currentRollRadians = orientation[2]
        if (!currentPitchRadians.isFinite() || !currentRollRadians.isFinite()) return
        if (baselinePitchRadians == null || baselineRollRadians == null) {
            baselinePitchRadians = currentPitchRadians
            baselineRollRadians = currentRollRadians
            calibratedDisplayRotation = currentDisplayRotation
            return
        }

        val rawOffset = gyroOffsetFromAngles(
            pitchRadians = currentPitchRadians,
            rollRadians = currentRollRadians,
            baselinePitchRadians = baselinePitchRadians ?: currentPitchRadians,
            baselineRollRadians = baselineRollRadians ?: currentRollRadians
        )
        smoothedPitch = smoothGyroValue(smoothedPitch, rawOffset.y)
        smoothedRoll = smoothGyroValue(smoothedRoll, rawOffset.x)
        publishIfChanged(Offset(x = smoothedRoll, y = smoothedPitch))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

private fun remapForDisplayRotation(
    input: FloatArray,
    output: FloatArray,
    displayRotation: Int
): FloatArray {
    val remapped = when (displayRotation) {
        Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
            input,
            SensorManager.AXIS_Y,
            SensorManager.AXIS_MINUS_X,
            output
        )
        Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
            input,
            SensorManager.AXIS_MINUS_X,
            SensorManager.AXIS_MINUS_Y,
            output
        )
        Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
            input,
            SensorManager.AXIS_MINUS_Y,
            SensorManager.AXIS_X,
            output
        )
        else -> false
    }
    if (!remapped) input.copyInto(output)
    return output
}

@Suppress("DEPRECATION")
private fun displayRotation(context: Context): Int {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display?.rotation?.let { return it }
    }
    return (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        ?.defaultDisplay
        ?.rotation
        ?: Surface.ROTATION_0
}
