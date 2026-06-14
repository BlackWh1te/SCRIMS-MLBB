package com.scrimslegends.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun SwipeToAction(
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    threshold: Int = 120,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val revealThreshold = -threshold

    Box(modifier = modifier.fillMaxWidth()) {
        // Background actions
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
        }

        // Foreground content (draggable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val currentOffset = offsetX.value
                                when {
                                    currentOffset < revealThreshold -> {
                                        // Reveal actions
                                        offsetX.animateTo(
                                            targetValue = revealThreshold.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    else -> {
                                        // Snap back
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val original = change.positionChange()
                            val newOffset = offsetX.value + original.x
                            // Only allow swipe to the left (negative offset)
                            val clamped = newOffset.coerceIn(-300f, 0f)
                            scope.launch {
                                offsetX.snapTo(clamped)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun SwipeToDismiss(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val dismissThreshold = -200f

    LaunchedEffect(offsetX.value) {
        if (offsetX.value <= dismissThreshold) {
            onDismiss()
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.CenterStart)
        ) {
            background()
        }

        // Foreground content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < dismissThreshold / 2) {
                                    // Dismiss
                                    offsetX.animateTo(
                                        targetValue = -1000f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    onDismiss()
                                } else {
                                    // Snap back
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val original = change.positionChange()
                            val newOffset = offsetX.value + original.x
                            val clamped = newOffset.coerceIn(-800f, 0f)
                            scope.launch {
                                offsetX.snapTo(clamped)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
