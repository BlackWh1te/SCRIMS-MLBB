package com.voicechanger.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicechanger.app.audio.VoiceEffect
import com.voicechanger.app.ui.VoiceChangerViewModel
import com.voicechanger.app.ui.theme.VoiceChangerTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission handled in UI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceChangerTheme {
                VoiceChangerApp(
                    onRequestPermission = {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    hasPermission = {
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                )
            }
        }
    }
}

@Composable
fun VoiceChangerApp(
    onRequestPermission: () -> Unit,
    hasPermission: () -> Boolean
) {
    val viewModel: VoiceChangerViewModel = viewModel()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF5F7FA),
                        Color(0xFFEEF2F7),
                        Color(0xFFE8EDF3)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Voice Changer",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    color = Color(0xFF1C1C1E)
                )
            )
            Text(
                text = "Record & transform your voice",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF8E8E93),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Waveform Card
            WaveformCard(
                isRecording = viewModel.isRecording,
                amplitude = viewModel.currentAmplitude,
                hasRecording = viewModel.hasRecording,
                isPlaying = viewModel.isPlaying
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            PlaybackControls(
                hasRecording = viewModel.hasRecording,
                isPlaying = viewModel.isPlaying,
                selectedEffect = viewModel.selectedEffect,
                onPlay = {
                    vibrate(context, 30)
                    viewModel.playEffect()
                },
                onSave = {
                    vibrate(context, 50)
                    viewModel.saveEffectedAudio()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Effects Picker
            EffectsPicker(
                effects = viewModel.effects,
                selected = viewModel.selectedEffect,
                onSelect = {
                    vibrate(context, 15)
                    viewModel.selectEffect(it)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Record Button
            RecordButton(
                isRecording = viewModel.isRecording,
                hasPermission = hasPermission(),
                onRequestPermission = onRequestPermission,
                onStart = {
                    vibrate(context, 60)
                    viewModel.startRecording()
                },
                onStop = {
                    vibrate(context, 60)
                    viewModel.stopRecording()
                }
            )
        }

        // Toast-like saved notification
        AnimatedVisibility(
            visible = viewModel.showSavedToast,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF34C759).copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Saved to VoiceChanger folder",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    LaunchedEffect(viewModel.showSavedToast) {
        if (viewModel.showSavedToast) {
            kotlinx.coroutines.delay(2500)
            viewModel.dismissToast()
        }
    }
}

@Composable
fun WaveformCard(
    isRecording: Boolean,
    amplitude: Float,
    hasRecording: Boolean,
    isPlaying: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color(0xFF000000).copy(alpha = 0.06f)
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!hasRecording && !isRecording) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.MicNone,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap the button below to record",
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (isRecording) {
                RecordingVisualizer(amplitude = amplitude)
            } else {
                StaticWaveform(isPlaying = isPlaying)
            }
        }
    }
}

@Composable
fun RecordingVisualizer(amplitude: Float) {
    val barCount = 24
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    Row(
        modifier = Modifier.fillMaxWidth(0.75f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.85f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + (index % 5) * 120, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )

            val heightFraction = if (amplitude > 0.05f) {
                (animatedHeight * (0.3f + amplitude * 0.7f)).coerceIn(0.1f, 1f)
            } else {
                animatedHeight * 0.3f
            }

            Box(
                modifier = Modifier
                    .width(5.dp)
                    .heightIn(min = 4.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF2D55),
                                Color(0xFFFF9500)
                            )
                        )
                    )
            )
        }
    }

    Text(
        text = "Recording...",
        color = Color(0xFFFF2D55),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
    )
}

@Composable
fun StaticWaveform(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "play")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "phase"
    )

    val barCount = 40
    Row(
        modifier = Modifier.fillMaxWidth(0.85f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val baseHeight = remember(index) {
                listOf(0.3f, 0.5f, 0.8f, 0.4f, 0.6f, 0.9f, 0.35f, 0.55f)[index % 8]
            }
            val animatedHeight = if (isPlaying) {
                (baseHeight + 0.2f * kotlin.math.sin((index + phase * barCount) * 0.8f)).coerceIn(0.15f, 1f)
            } else {
                baseHeight
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (isPlaying) Color(0xFF007AFF)
                        else Color(0xFF007AFF).copy(alpha = 0.35f)
                    )
            )
        }
    }

    if (isPlaying) {
        Text(
            text = "Playing preview...",
            color = Color(0xFF007AFF),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
        )
    }
}

@Composable
fun PlaybackControls(
    hasRecording: Boolean,
    isPlaying: Boolean,
    selectedEffect: VoiceEffect,
    onPlay: () -> Unit,
    onSave: () -> Unit
) {
    AnimatedVisibility(
        visible = hasRecording,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play Button
            IosButton(
                icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                label = if (isPlaying) "Stop" else "Play",
                containerColor = Color(0xFF007AFF),
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onPlay
            )

            // Save Button
            IosButton(
                icon = Icons.Default.Save,
                label = "Save",
                containerColor = Color(0xFF34C759),
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onSave
            )
        }
    }
}

@Composable
fun IosButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = containerColor.copy(alpha = 0.4f)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun EffectsPicker(
    effects: List<VoiceEffect>,
    selected: VoiceEffect,
    onSelect: (VoiceEffect) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Voice Effects",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color(0xFF1C1C1E)
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(effects) { _, effect ->
                val isSelected = effect == selected
                val shape = RoundedCornerShape(99.dp)

                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(
                            if (isSelected) Color(0xFF007AFF)
                            else Color(0xFFFFFFFF)
                        )
                        .border(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = if (isSelected) Color.Transparent else Color(0xFFE5E5EA),
                            shape = shape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(effect) }
                        )
                        .shadow(
                            elevation = if (isSelected) 8.dp else 0.dp,
                            shape = shape,
                            spotColor = Color(0xFF007AFF).copy(alpha = 0.3f)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = effect.displayName,
                        color = if (isSelected) Color.White else Color(0xFF1C1C1E),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecordButton(
    isRecording: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Color(0xFFFF2D55).copy(alpha = 0.18f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (isRecording) Color(0xFFFF2D55) else Color(0xFF007AFF)
                )
                .shadow(
                    elevation = if (isRecording) 24.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = if (isRecording) Color(0xFFFF2D55).copy(alpha = 0.5f)
                    else Color(0xFF007AFF).copy(alpha = 0.4f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!hasPermission) {
                            onRequestPermission()
                            return@clickable
                        }
                        if (isRecording) onStop() else onStart()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun vibrate(context: android.content.Context, durationMs: Long) {
    val vibrator = context.getSystemService(Vibrator::class.java)
    if (vibrator?.hasVibrator() == true) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}
