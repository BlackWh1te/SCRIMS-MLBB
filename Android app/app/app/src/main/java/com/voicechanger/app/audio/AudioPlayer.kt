package com.voicechanger.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*

class AudioPlayer {

    companion object {
        const val SAMPLE_RATE = AudioRecorder.SAMPLE_RATE
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var isPlaying: Boolean = false
        private set

    fun playSamples(samples: ShortArray, onComplete: (() -> Unit)? = null): Boolean {
        if (samples.isEmpty()) return false
        stop()

        return try {
            val minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer.coerceAtLeast(samples.size * 2))
                .build()

            audioTrack?.play()
            isPlaying = true

            playbackJob = scope.launch {
                try {
                    val chunkSize = minBuffer / 2
                    var offset = 0
                    while (offset < samples.size && isActive) {
                        val end = (offset + chunkSize).coerceAtMost(samples.size)
                        val chunk = samples.copyOfRange(offset, end)
                        audioTrack?.write(chunk, 0, chunk.size)
                        offset += chunkSize
                    }
                } finally {
                    isPlaying = false
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke()
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio", e)
            isPlaying = false
            false
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
