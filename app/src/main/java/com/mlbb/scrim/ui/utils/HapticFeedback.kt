package com.mlbb.scrim.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

object HapticFeedback {

    private const val TAG = "HapticFeedback"

    fun performClick(context: Context) {
        safeVibrate(context) { vibrator ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10)
                }
            }
        }
    }

    fun performSuccess(context: Context) {
        safeVibrate(context) { vibrator ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, 180))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }
    }

    fun performError(context: Context) {
        safeVibrate(context) { vibrator ->
            val pattern = longArrayOf(0, 30, 50, 30)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        }
    }

    fun performSwipe(context: Context) {
        safeVibrate(context) { vibrator ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(5, 80))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(5)
                }
            }
        }
    }

    fun performToggle(context: Context) {
        safeVibrate(context) { vibrator ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val pattern = longArrayOf(0, 10, 30, 10)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 10, 30, 10), -1)
                }
            }
        }
    }

    fun performBottomSheetOpen(context: Context) {
        safeVibrate(context) { vibrator ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, 120))
                }
                else -> {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(15)
                }
            }
        }
    }

    private inline fun safeVibrate(context: Context, block: (Vibrator) -> Unit) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            block(vibrator)
        } catch (e: SecurityException) {
            Log.w(TAG, "Vibrate permission missing", e)
        } catch (e: Exception) {
            Log.w(TAG, "Vibrate failed", e)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            }
            else -> {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
}

@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val context = LocalContext.current
    val view = LocalView.current
    return HapticFeedbackHelper(context, view)
}

class HapticFeedbackHelper(
    private val context: Context,
    private val view: View
) {
    fun click() = HapticFeedback.performClick(context)
    fun success() = HapticFeedback.performSuccess(context)
    fun error() = HapticFeedback.performError(context)
    fun swipe() = HapticFeedback.performSwipe(context)
    fun toggle() = HapticFeedback.performToggle(context)
    fun bottomSheetOpen() = HapticFeedback.performBottomSheetOpen(context)

    fun viewHapticFeedback(feedbackType: Int) {
        view.performHapticFeedback(feedbackType)
    }
}
