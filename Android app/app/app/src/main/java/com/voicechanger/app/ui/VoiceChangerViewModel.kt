package com.voicechanger.app.ui

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicechanger.app.audio.AudioPlayer
import com.voicechanger.app.audio.AudioRecorder
import com.voicechanger.app.audio.VoiceEffect
import com.voicechanger.app.audio.VoiceEffectProcessor
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceChangerViewModel(application: Application) : AndroidViewModel(application) {

    private val recorder = AudioRecorder(application.applicationContext)
    private val player = AudioPlayer()

    var isRecording by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var hasRecording by mutableStateOf(false)
        private set
    var selectedEffect by mutableStateOf<VoiceEffect>(VoiceEffect.Normal)
        private set
    var showSavedToast by mutableStateOf(false)
        private set
    var currentAmplitude by mutableStateOf(0f)
        private set

    private var rawSamples: ShortArray = ShortArray(0)
    private var amplitudeJob: Job? = null

    val effects = VoiceEffect.values()

    fun startRecording(): Boolean {
        val success = recorder.startRecording()
        if (success) {
            isRecording = true
            hasRecording = false
            rawSamples = ShortArray(0)
            startAmplitudeMonitoring()
        }
        return success
    }

    fun stopRecording() {
        recorder.stopRecording()
        isRecording = false
        stopAmplitudeMonitoring()
        currentAmplitude = 0f

        val samples = recorder.recordedSamples
        if (samples.isNotEmpty()) {
            rawSamples = samples.toShortArray()
            hasRecording = true
        }
    }

    private fun startAmplitudeMonitoring() {
        amplitudeJob = viewModelScope.launch {
            while (isActive) {
                if (recorder.recordedSamples.isNotEmpty()) {
                    val recent = recorder.recordedSamples.takeLast(1024)
                    val sum = recent.sumOf { kotlin.math.abs(it.toInt()).toDouble() }
                    currentAmplitude = (sum / recent.size / 32768.0).toFloat().coerceIn(0f, 1f)
                }
                delay(50)
            }
        }
    }

    private fun stopAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = null
    }

    fun selectEffect(effect: VoiceEffect) {
        if (isPlaying) {
            stopPlayback()
        }
        selectedEffect = effect
    }

    fun playEffect() {
        if (rawSamples.isEmpty()) return
        if (isPlaying) {
            stopPlayback()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val processed = VoiceEffectProcessor.applyEffect(rawSamples, selectedEffect)
            val normalized = VoiceEffectProcessor.normalize(processed)

            withContext(Dispatchers.Main) {
                isPlaying = true
                player.playSamples(normalized) {
                    isPlaying = false
                }
            }
        }
    }

    fun stopPlayback() {
        player.stop()
        isPlaying = false
    }

    fun saveEffectedAudio() {
        if (rawSamples.isEmpty()) return

        viewModelScope.launch(Dispatchers.Default) {
            val processed = VoiceEffectProcessor.applyEffect(rawSamples, selectedEffect)
            val normalized = VoiceEffectProcessor.normalize(processed)

            try {
                val dir = File(getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_MUSIC), "VoiceChanger")
                if (!dir.exists()) dir.mkdirs()

                val fileName = "effect_${selectedEffect.displayName.lowercase()}_${System.currentTimeMillis()}.pcm"
                val file = File(dir, fileName)

                FileOutputStream(file).use { fos ->
                    val byteBuffer = ByteBuffer.allocate(normalized.size * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    normalized.forEach { byteBuffer.putShort(it) }
                    fos.write(byteBuffer.array())
                }

                withContext(Dispatchers.Main) {
                    showSavedToast = true
                }
            } catch (e: Exception) {
                Log.e("VoiceChangerViewModel", "Failed to save audio", e)
            }
        }
    }

    fun dismissToast() {
        showSavedToast = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        recorder.release()
        player.release()
    }
}
