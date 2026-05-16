package com.mlbb.scrim.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*

// ============================================
// PREMIUM IOS-STYLE GLASS CARD
// ============================================
// Advanced glassmorphism with layered depth, subtle borders, and sophisticated shadows

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentColor: Color = GoldPrimary.copy(alpha = 0.12f),
    elevation: Float = 12f,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glassCardScale"
    )
    
    val cardElevation by animateFloatAsState(
        targetValue = if (isPressed) elevation * 0.6f else elevation,
        animationSpec = tween(200),
        label = "glassCardElevation"
    )
    
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.2f else 0.12f,
        animationSpec = tween(200),
        label = "borderAlpha"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = cardElevation
                shape = RoundedCornerShape(24.dp)
                clip = true
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SurfaceElevated.copy(alpha = 0.85f),
                        SurfaceBase.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        White.copy(alpha = borderAlpha),
                        accentColor,
                        White.copy(alpha = borderAlpha * 0.5f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(1.dp)
    ) {
        // Inner glow effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 100f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

// ============================================
// PREMIUM IOS-STYLE ELEVATED CARD
// ============================================
// Refined shadows with multiple layers for depth

@Composable
fun PremiumElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shadowColor: Color = BluePrimary.copy(alpha = 0.15f),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevatedCardScale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 16.dp,
        animationSpec = tween(200),
        label = "elevatedCardElevation"
    )
    
    Card(
        onClick = onClick ?: {},
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (onClick == null) Modifier.clickable(enabled = false, onClick = {}) else Modifier),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
            hoveredElevation = elevation
        ),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    onDrawBehind {
                        // Subtle top highlight
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    shadowColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.3f
                            ),
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                content = content
            )
        }
    }
}

// ============================================
// PREMIUM IOS-STYLE PRIMARY BUTTON
// ============================================
// Full-width, gradient background, refined shadows

@Composable
fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = BluePrimary,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "primaryButtonScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(200),
        label = "buttonAlpha"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.85f)
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .drawWithCache {
                onDrawBehind {
                    // Subtle inner highlight at top
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                White.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.4f
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (icon != null) 8.dp else 0.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = PremiumButton,
                color = White.copy(alpha = alpha),
                maxLines = 1
            )
        }
    }
}

// ============================================
// PREMIUM IOS-STYLE SECONDARY BUTTON
// ============================================
// Outlined style with gradient border

@Composable
fun PremiumSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = GoldPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "secondaryButtonScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(200),
        label = "buttonAlpha"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceGlass)
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.6f),
                        accentColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = PremiumButton,
            color = accentColor.copy(alpha = alpha)
        )
    }
}

// ============================================
// PREMIUM IOS-STYLE TEXT BUTTON
// ============================================
// Minimal, elegant text-only button

@Composable
fun PremiumTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = BluePrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else if (enabled) 1f else 0.5f,
        animationSpec = tween(150),
        label = "textButtonAlpha"
    )
    
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor.copy(alpha = alpha)
        )
    ) {
        Text(
            text = text,
            style = PremiumButtonSmall,
            color = textColor.copy(alpha = alpha)
        )
    }
}

// ============================================
// PREMIUM IOS-STYLE INPUT FIELD
// ============================================
// Refined input with focus states and subtle borders

@Composable
fun PremiumInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    accentColor: Color = GoldPrimary,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.5f else 0.12f,
        animationSpec = tween(200),
        label = "inputBorderAlpha"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) SurfaceOverlay else SurfaceElevated,
        animationSpec = tween(200),
        label = "inputBackground"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color = backgroundColor)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = borderAlpha),
                        accentColor.copy(alpha = borderAlpha * 0.5f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MidGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { 
                    Text(
                        text = placeholder,
                        style = iOSBody,
                        color = DimGray
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = accentColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = iOSBody,
                singleLine = true,
                visualTransformation = visualTransformation
            )
            
            trailingIcon?.let {
                IconButton(
                    onClick = onTrailingIconClick ?: {},
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (onTrailingIconClick != null) accentColor else DimGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// PREMIUM IOS-STYLE CHIP
// ============================================
// Elegant filter chip with gradient border

@Composable
fun PremiumChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = GoldPrimary
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chipScale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.15f) else SurfaceGlass,
        animationSpec = tween(200),
        label = "chipBackground"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) accentColor else LightGray,
        animationSpec = tween(200),
        label = "chipTextColor"
    )
    
    val borderAlpha by animateFloatAsState(
        targetValue = if (selected) 0.4f else 0.12f,
        animationSpec = tween(200),
        label = "chipBorderAlpha"
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(36.dp),
        shape = RoundedCornerShape(9999.dp),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = borderAlpha),
                            accentColor.copy(alpha = borderAlpha * 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(9999.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = iOSCallout.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = textColor
            )
        }
    }
}

// ============================================
// PREMIUM IOS-STYLE AVATAR
// ============================================
// Refined avatar with gradient ring and subtle glow

@Composable
fun PremiumAvatar(
    initials: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    accentColor: Color = GoldPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.6f)
                    )
                ),
                shape = CircleShape
            )
            .padding(2.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SurfaceOverlay,
                        SurfaceElevated
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = iOSTitle2.copy(
                fontSize = (size.value * 0.4).sp,
                fontWeight = FontWeight.Bold
            ),
            color = White
        )
    }
}
