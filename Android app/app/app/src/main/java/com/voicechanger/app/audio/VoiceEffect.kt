package com.voicechanger.app.audio

sealed class VoiceEffect(val displayName: String) {
    object Normal : VoiceEffect("Normal")
    object Chipmunk : VoiceEffect("Chipmunk")
    object Deep : VoiceEffect("Deep")
    object Robot : VoiceEffect("Robot")
    object Echo : VoiceEffect("Echo")
    object Reverb : VoiceEffect("Reverb")
    object Narrator : VoiceEffect("Narrator")
    object Helium : VoiceEffect("Helium")
    object Cave : VoiceEffect("Cave")

    companion object {
        fun values(): List<VoiceEffect> = listOf(
            Normal, Chipmunk, Deep, Robot, Echo, Reverb, Narrator, Helium, Cave
        )
    }
}
