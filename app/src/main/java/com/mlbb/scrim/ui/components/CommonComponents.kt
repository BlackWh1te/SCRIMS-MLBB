package com.mlbb.scrim.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.utils.HapticFeedback
import androidx.compose.ui.platform.LocalContext

// ============================================
// iOS-Style Primary Button
// ============================================

@Composable
fun iOSPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    backgroundColor: Color = iOSBlue,
    contentColor: Color = White,
    height: androidx.compose.ui.unit.Dp = 50.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.98f,
        label = "buttonScale",
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val context = LocalContext.current

    Button(
        onClick = {
            HapticFeedback.performClick(context)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        shape = iOSButtonShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

// ============================================
// iOS-Style Secondary Button (Outlined)
// ============================================

@Composable
fun iOSSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = iOSBlue,
    contentColor: Color = iOSBlue,
    height: androidx.compose.ui.unit.Dp = 50.dp
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            HapticFeedback.performClick(context)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = SolidColor(borderColor),
            width = 1.5.dp
        ),
        shape = iOSButtonShape
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
    }
}

// ============================================
// iOS-Style Text Button
// ============================================

@Composable
fun iOSTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = iOSBlue
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor
        )
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
    }
}

// ============================================
// Gradient Button (Primary Action - Legacy)
// ============================================

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradient: List<Color> = GoldGradient,
    contentColor: Color = DarkBlue,
    height: androidx.compose.ui.unit.Dp = 52.dp
) {
    val brush = Brush.horizontalGradient(colors = gradient)
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        label = "buttonAlpha"
    )
    val context = LocalContext.current

    Button(
        onClick = {
            HapticFeedback.performClick(context)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = LightGray
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush, shape = RoundedCornerShape(12.dp))
                .graphicsLayer { this.alpha = alpha },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ============================================
// Ghost Button (Secondary Action)
// ============================================

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = GoldPrimary,
    contentColor: Color = GoldPrimary
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            HapticFeedback.performClick(context)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = SolidColor(borderColor)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

// ============================================
// iOS-Style Glass Card with Blur Effect
// ============================================

@Composable
fun iOSGlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = iOSCardShape,
    borderColor: Color? = GlassBorder,
    backgroundColor: Color = SurfaceGlass,
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    Card(
        modifier = modifier
            .drawWithContent {
                drawContent()
                if (borderColor != null) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = with(density) { 0.5.dp.toPx() }
                            color = borderColor.toArgb()
                            maskFilter = android.graphics.BlurMaskFilter(
                                with(density) { 1.dp.toPx() },
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        val rect = androidx.compose.ui.geometry.Rect(
                            0f, 0f, size.width, size.height
                        )
                        val roundedRect = android.graphics.RectF(
                            rect.left, rect.top, rect.right, rect.bottom
                        )
                        val corners = with(density) { 20.dp.toPx() }
                        canvas.nativeCanvas.drawRoundRect(roundedRect, corners, corners, paint)
                    }
                }
            }
            .graphicsLayer {
                shadowElevation = with(density) { 4.dp.toPx() }
                spotShadowColor = ShadowMedium
                ambientShadowColor = ShadowLight
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

// ============================================
// iOS-Style Elevated Card
// ============================================

@Composable
fun iOSElevatedCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = iOSCardShape,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 6.dp.toPx()
                spotShadowColor = ShadowMedium
                ambientShadowColor = ShadowLight
            },
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

// ============================================
// Glow Card with Optional Border (Legacy - kept for compatibility)
// ============================================

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    glowColor: Color = BlueGlow,
    borderColor: Color? = null,
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val elevation = 8.dp

    Card(
        modifier = modifier
            .drawWithContent {
                drawContent()
                if (borderColor != null) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = with(density) { 1.5.dp.toPx() }
                            color = borderColor.toArgb()
                            maskFilter = android.graphics.BlurMaskFilter(
                                with(density) { 2.dp.toPx() },
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        val rect = androidx.compose.ui.geometry.Rect(
                            0f, 0f, size.width, size.height
                        )
                        val roundedRect = android.graphics.RectF(
                            rect.left, rect.top, rect.right, rect.bottom
                        )
                        val corners = with(density) { 16.dp.toPx() }
                        canvas.nativeCanvas.drawRoundRect(roundedRect, corners, corners, paint)
                    }
                }
            }
            .graphicsLayer {
                shadowElevation = with(density) { elevation.toPx() }
                spotShadowColor = glowColor
                ambientShadowColor = glowColor.copy(alpha = 0.3f)
            },
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = shape
    ) {
        content()
    }
}

// ============================================
// Animated Entrance for List Items
// ============================================

@Composable
fun AnimatedEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300, delayMillis = 0)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 300, easing = AppEaseOutCubic),
            initialOffsetY = { it / 8 }
        ),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        content()
    }
}


// ============================================
// iOS-Style Chip/Filter
// ============================================

@Composable
fun iOSChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) iOSBlue else GlassLight,
        animationSpec = tween(200),
        label = "chipBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) White else MidGray,
        animationSpec = tween(200),
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = iOSChipShape,
        color = backgroundColor,
        border = if (!selected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                GlassBorder
            )
        } else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

// ============================================
// iOS-Style Input/TextField
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun iOSInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: () -> Unit = {},
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardOptions = KeyboardOptions.Default
) {
    val borderColor = when {
        isError -> ErrorRed
        else -> Separator
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceElevated, shape = iOSInputShape),
        placeholder = {
            Text(
                text = placeholder,
                style = iOSBody.copy(color = DimGray)
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = DimGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else null,
        trailingIcon = if (trailingIcon != null) {
            {
                IconButton(onClick = onTrailingIconClick) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = DimGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardType,
        keyboardActions = KeyboardActions(
            onDone = { /* Handle IME action */ }
        ),
        shape = iOSInputShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iOSBlue,
            unfocusedBorderColor = borderColor,
            errorBorderColor = ErrorRed,
            cursorColor = iOSBlue,
            focusedTextColor = White,
            unfocusedTextColor = White
        ),
        textStyle = iOSBody
    )
}

// ============================================
// Enhanced Info Chip with Icon
// ============================================

@Composable
fun EnhancedInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = LightGray,
    backgroundColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = tint,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================
// Hero Section Background
// ============================================

@Composable
fun HeroBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = HeroGradient
                )
            )
    ) {
        content()
    }
}

// ============================================
// Status Badge (Enhanced)
// ============================================

@Composable
fun EnhancedStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (text.equals("open", ignoreCase = true)) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                if (text.equals("open", ignoreCase = true)) {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ============================================
// Section Header
// ============================================

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        )
        action?.invoke()
    }
}

// ============================================
// Empty State Illustration
// ============================================

@Composable
fun EmptyState(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    title   : String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action  : @Composable (() -> Unit)? = null
) {
    AnimatedEntrance {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceOverlay)
                    .drawWithContent {
                        drawContent()
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias  = true
                                style        = android.graphics.Paint.Style.STROKE
                                strokeWidth  = 1f
                                color        = GlassBorder.toArgb()
                            }
                            canvas.nativeCanvas.drawRoundRect(
                                android.graphics.RectF(0f, 0f, size.width, size.height),
                                with(androidx.compose.ui.platform.LocalDensity) { 24.dp.value },
                                with(androidx.compose.ui.platform.LocalDensity) { 24.dp.value },
                                paint
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = title,
                    tint               = DimGray,
                    modifier           = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text       = title,
                style      = iOSTitle3.copy(color = TextPrimary),
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = subtitle,
                style     = iOSCallout.copy(color = TextSecondary),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (action != null) {
                Spacer(Modifier.height(32.dp))
                action()
            }
        }
    }
}

// ============================================
// iOS-Style Navigation Bar with Large Title
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun iOSNavigationBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    largeTitle: Boolean = true
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    TopAppBar(
        title = {
            if (largeTitle) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                )
            }
        },
        modifier = modifier,
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = iOSBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = White,
            navigationIconContentColor = iOSBlue,
            actionIconContentColor = iOSBlue
        ),
        scrollBehavior = scrollBehavior
    )
}

// ============================================
// iOS-Style Large Title Header
// ============================================

@Composable
fun iOSLargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                letterSpacing = (-0.5).sp
            )
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    color = MidGray
                )
            )
        }
    }
}

// ============================================
// Back Button with Glass Effect
// ============================================

@Composable
fun GlassBackButton(
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
    tint    : Color = TextPrimary
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceOverlay)
            .drawWithContent {
                drawContent()
                drawLine(
                    brush       = SolidColor(GlassBorder),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 1f
                )
                drawLine(
                    brush       = SolidColor(GlassBorder),
                    start       = Offset(0f, 0f),
                    end         = Offset(0f, size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    brush       = SolidColor(Color.Transparent),
                    start       = Offset(size.width, 0f),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    brush       = SolidColor(Color.Transparent),
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint               = tint,
            modifier           = Modifier.size(20.dp)
        )
    }
}

// ============================================
// iOS-Style Bottom Sheet
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun iOSBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = iOSSheetShape,
        containerColor = SurfaceElevated,
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .background(
                            color = Separator,
                            shape = RoundedCornerShape(9999.dp)
                        )
                )
            }
        },
        windowInsets = WindowInsets(0.dp)
    ) {
        content()
    }
}

// ============================================
// iOS-Style Action Sheet (Simple bottom sheet with actions)
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun iOSActionSheet(
    title: String? = null,
    actions: List<@Composable () -> Unit>,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    iOSBottomSheet(
        onDismissRequest = onCancel,
        modifier = modifier
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.sp,
                    color = MidGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
            Divider(
                color = Separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            actions.forEach { action ->
                action()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        iOSTextButton(
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            textColor = iOSRed
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ============================================
// Floating Action Button with Gradient
// ============================================

@Composable
fun GradientFAB(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    gradient: List<Color> = BlueGradient
) {
    val brush = Brush.linearGradient(colors = gradient)

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// Shimmer Loading Skeletons
// ============================================

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        MidGray.copy(alpha = 0.15f),
        MidGray.copy(alpha = 0.3f),
        MidGray.copy(alpha = 0.15f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun ScrimListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(140.dp)
                                .height(18.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(200.dp)
                                .height(14.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(120.dp)
                                .height(18.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(100.dp)
                                .height(14.dp)
                        )
                    }
                    ShimmerBox(
                        modifier = Modifier
                            .width(56.dp)
                            .height(28.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(itemCount) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                colors = CardDefaults.cardColors(containerColor = DarkNavy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(180.dp)
                                .height(16.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                        )
                    }
                    ShimmerBox(
                        modifier = Modifier
                            .width(40.dp)
                            .height(12.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// Error Snackbar — Shows ViewModel errors as a dismissable banner
// ============================================

/**
 * Displays an error message as a snackbar-style banner at the bottom of the screen.
 * Automatically hides when [error] becomes null. Shows a dismiss button.
 *
 * Usage: Collect ViewModel error state and pass it here.
 * ```
 * val error by viewModel.error.collectAsState()
 * ErrorSnackbar(error = error, onDismiss = { viewModel.clearError() })
 * ```
 */
@Composable
fun ErrorSnackbar(
    error: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = error != null
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ErrorRed.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = error,
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
