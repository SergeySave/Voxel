package com.sergeysav.voxel.common.noise

import org.joml.Random
import org.joml.SimplexNoise
import kotlin.math.pow

/**
 * @author sergeys
 */
fun noiseGenerator(seed: Long, amplitudeScale: Float = 1f, frequencyScale: Float = 1f, octaves: Int = 1, aScaling: Float = 0.5f, fScaling: Float = 2.0f): (Float, Float, Float)->Float {
    val max = ((1.0 - aScaling.toDouble().pow(octaves.toDouble()))/(1 - aScaling)).toFloat()
    val r1 = Random(seed)
    val seeds = FloatArray(octaves) { (2 * r1.nextFloat() - 1) * 1e5f }
    return { x, y, z ->
        var total = 0f
        var amp = amplitudeScale
        var freq = frequencyScale
        for (i in 0 until octaves) {
            total += amp * SimplexNoise.noise(x * freq, y * freq, z * freq, seeds[i])
            amp *= aScaling
            freq *= fScaling
        }
        total / max
    }
}

fun noiseGenerator2d(seed: Long, amplitudeScale: Float = 1f, frequencyScale: Float = 1f, octaves: Int = 1, aScaling: Float = 0.5f, fScaling: Float = 2.0f): (Float, Float)->Float {
    val max = ((1.0 - aScaling.toDouble().pow(octaves.toDouble()))/(1 - aScaling)).toFloat()
    val r1 = Random(seed)
    val seeds = FloatArray(octaves) { (2 * r1.nextFloat() - 1) * 1e5f }
    return { x, y ->
        var total = 0f
        var amp = amplitudeScale
        var freq = frequencyScale
        for (i in 0 until octaves) {
            total += amp * SimplexNoise.noise(x * freq, y * freq, seeds[i])
            amp *= aScaling
            freq *= fScaling
        }
        total / max
    }
}
