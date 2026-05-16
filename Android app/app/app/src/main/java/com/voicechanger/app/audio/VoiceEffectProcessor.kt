package com.voicechanger.app.audio

import kotlin.math.*

object VoiceEffectProcessor {

    fun applyEffect(samples: ShortArray, effect: VoiceEffect): ShortArray {
        return when (effect) {
            is VoiceEffect.Normal -> samples
            is VoiceEffect.Chipmunk -> pitchShift(samples, 1.8f)
            is VoiceEffect.Deep -> pitchShift(samples, 0.55f)
            is VoiceEffect.Robot -> robotEffect(samples)
            is VoiceEffect.Echo -> echoEffect(samples, delayMs = 250, decay = 0.45f)
            is VoiceEffect.Reverb -> reverbEffect(samples)
            is VoiceEffect.Narrator -> narratorEffect(samples)
            is VoiceEffect.Helium -> heliumEffect(samples)
            is VoiceEffect.Cave -> caveEffect(samples)
        }
    }

    /**
     * Pitch shift by resampling with linear interpolation.
     * factor > 1 = higher pitch (shorter duration)
     * factor < 1 = lower pitch (longer duration)
     */
    fun pitchShift(samples: ShortArray, factor: Float): ShortArray {
        if (factor == 1.0f) return samples
        val newLength = (samples.size / factor).toInt()
        val result = ShortArray(newLength)
        for (i in result.indices) {
            val srcIndex = i * factor
            val idx = srcIndex.toInt()
            val frac = srcIndex - idx
            val s0 = samples.getOrElse(idx) { 0 }
            val s1 = samples.getOrElse(idx + 1) { s0 }
            val value = s0 * (1 - frac) + s1 * frac
            result[i] = value.toInt().toShort()
        }
        return result
    }

    /**
     * Ring modulation for robot/metallic sound
     */
    fun robotEffect(samples: ShortArray): ShortArray {
        val result = ShortArray(samples.size)
        val freq = 120.0
        val twoPiF = 2.0 * PI * freq / AudioRecorder.SAMPLE_RATE
        for (i in samples.indices) {
            val carrier = cos(i * twoPiF).toFloat()
            val mod = samples[i] * carrier
            result[i] = mod.toInt().coerceIn(-32768, 32767).toShort()
        }
        // Add slight quantization for more robot character
        for (i in result.indices) {
            result[i] = ((result[i] / 256) * 256).toShort()
        }
        return result
    }

    /**
     * Echo using a circular delay buffer
     */
    fun echoEffect(samples: ShortArray, delayMs: Int, decay: Float, repeats: Int = 2): ShortArray {
        val delaySamples = (AudioRecorder.SAMPLE_RATE * delayMs / 1000)
        val result = samples.copyOf()
        val buffer = FloatArray(samples.size + delaySamples * repeats)

        for (i in samples.indices) {
            buffer[i] = samples[i].toFloat()
        }

        for (r in 1..repeats) {
            val d = delaySamples * r
            val amp = decay.pow(r)
            for (i in samples.indices) {
                if (i + d < buffer.size) {
                    buffer[i + d] += samples[i] * amp
                }
            }
        }

        for (i in result.indices) {
            result[i] = buffer[i].toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Simple reverb using multiple comb filters and an all-pass
     */
    fun reverbEffect(samples: ShortArray): ShortArray {
        val combDelays = intArrayOf(1112, 1356, 1422, 1690)
        val combGains = floatArrayOf(0.62f, 0.58f, 0.55f, 0.52f)
        val allpassDelays = intArrayOf(225, 556)
        val allpassGains = floatArrayOf(0.5f, 0.5f)

        var output = samples.copyOf()

        // Comb filters in parallel
        val combSum = FloatArray(samples.size)
        for (c in combDelays.indices) {
            val delay = combDelays[c]
            val gain = combGains[c]
            val buf = FloatArray(delay)
            var idx = 0
            for (i in samples.indices) {
                val delayed = buf[idx]
                val v = samples[i] + delayed * gain
                buf[idx] = v
                combSum[i] += delayed
                idx = (idx + 1) % delay
            }
        }

        // All-pass filters in series
        var ap = combSum.copyOf()
        for (a in allpassDelays.indices) {
            val delay = allpassDelays[a]
            val gain = allpassGains[a]
            val buf = FloatArray(delay)
            var idx = 0
            val next = FloatArray(ap.size)
            for (i in ap.indices) {
                val delayed = buf[idx]
                val v = ap[i] + delayed * gain
                next[i] = delayed - gain * v
                buf[idx] = v
                idx = (idx + 1) % delay
            }
            ap = next
        }

        // Mix dry and wet
        val wet = 0.35f
        val dry = 0.65f
        for (i in output.indices) {
            val v = samples[i] * dry + ap[i] * wet
            output[i] = v.toInt().coerceIn(-32768, 32767).toShort()
        }

        return output
    }

    /**
     * Narrator: slight pitch down + subtle echo + boost bass
     */
    fun narratorEffect(samples: ShortArray): ShortArray {
        val lowered = pitchShift(samples, 0.82f)
        val echoed = echoEffect(lowered, 180, 0.25f, 1)
        return boostBass(echoed)
    }

    /**
     * Helium: very high pitch + speed up slightly
     */
    fun heliumEffect(samples: ShortArray): ShortArray {
        return pitchShift(samples, 2.2f)
    }

    /**
     * Cave: deep pitch + heavy reverb + echo
     */
    fun caveEffect(samples: ShortArray): ShortArray {
        val deep = pitchShift(samples, 0.5f)
        val rev = reverbEffect(deep)
        return echoEffect(rev, 400, 0.55f, 3)
    }

    /**
     * Simple bass boost using a basic low-pass approximation
     */
    private fun boostBass(samples: ShortArray): ShortArray {
        val result = ShortArray(samples.size)
        var prev = 0f
        val alpha = 0.3f
        for (i in samples.indices) {
            val current = samples[i] * (1 - alpha) + prev * alpha
            result[i] = (current * 1.15f).toInt().coerceIn(-32768, 32767).toShort()
            prev = current
        }
        return result
    }

    /**
     * Normalize audio to prevent clipping after effects
     */
    fun normalize(samples: ShortArray): ShortArray {
        var max = 1
        for (s in samples) {
            val abs = kotlin.math.abs(s.toInt())
            if (abs > max) max = abs
        }
        if (max <= 0 || max >= 32000) return samples
        val scale = 32000.0 / max
        return ShortArray(samples.size) { i ->
            (samples[i] * scale).toInt().coerceIn(-32768, 32767).toShort()
        }
    }
}
