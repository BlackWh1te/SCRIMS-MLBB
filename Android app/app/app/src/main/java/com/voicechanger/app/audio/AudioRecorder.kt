package com.voicechanger.app.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_FACTOR = 2
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR

    private val _recordedSamples = mutableListOf<Short>()
    val recordedSamples: List<Short> get() = _recordedSamples.toList()

    var isRecording: Boolean = false
        private set

    private var recordingFile: File? = null

    fun startRecording(): Boolean {
        if (isRecording) return false

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord initialization failed")
                return false
            }

            _recordedSamples.clear()
            isRecording = true
            audioRecord?.startRecording()

            recordingJob = scope.launch {
                val buffer = ShortArray(minBufferSize)
                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        for (i in 0 until read) {
                            _recordedSamples.add(buffer[i])
                        }
                    }
                }
            }

            return true
        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "Permission denied", e)
            return false
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording", e)
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null

        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        return saveToFile()
    }

    private fun saveToFile(): File? {
        if (_recordedSamples.isEmpty()) return null

        return try {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "VoiceChanger")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "recording_${System.currentTimeMillis()}.pcm")
            FileOutputStream(file).use { fos ->
                val byteBuffer = ByteBuffer.allocate(_recordedSamples.size * 2)
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                _recordedSamples.forEach { byteBuffer.putShort(it) }
                fos.write(byteBuffer.array())
            }
            recordingFile = file
            file
        } catch (e: IOException) {
            Log.e("AudioRecorder", "Failed to save recording", e)
            null
        }
    }

    fun clear() {
        _recordedSamples.clear()
    }

    fun release() {
        stopRecording()
        scope.cancel()
    }
}
