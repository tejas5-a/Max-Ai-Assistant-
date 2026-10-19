package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.DarkShadowColor
import com.example.ui.theme.DeepOrangePressed
import com.example.ui.theme.LightOrangeHighlight
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusError

@Composable
fun RaisedNeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.05f else 0.12f,
        label = "ShadowAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isPressed) elevation / 2 else elevation,
                shape = shape,
                ambientColor = DarkShadowColor,
                spotColor = DarkShadowColor.copy(alpha = shadowAlpha)
            )
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun PressedNeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentElevation = if (isPressed || !enabled) 2.dp else 5.dp
    val color = if (isPressed) MaterialTheme.colorScheme.surfaceVariant else containerColor

    Surface(
        modifier = modifier
            .shadow(
                elevation = currentElevation,
                shape = shape,
                ambientColor = DarkShadowColor,
                spotColor = DarkShadowColor.copy(alpha = 0.2f)
            )
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = color,
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun InsetNeumorphicField(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    borderColor: Color = DarkShadowColor.copy(alpha = 0.3f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Composable
fun OrangePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        isPressed -> DeepOrangePressed
        else -> PrimaryOrange
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = if (isPressed || !enabled) 2.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = PrimaryOrange.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = Color.White
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun NeumorphicIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentElevation = if (isPressed || !enabled) 2.dp else 5.dp
    val color = if (isPressed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = currentElevation,
                shape = shape,
                ambientColor = DarkShadowColor,
                spotColor = DarkShadowColor
            )
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun MAXVoiceOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    isMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceOrbPulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening || isSpeaking) 1.18f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 800 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse"
    )

    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = if (isListening || isSpeaking) 1.35f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraPulse"
    )

    val orbColor = when {
        isMuted -> DarkShadowColor
        isThinking -> LightOrangeHighlight
        isSpeaking -> PrimaryOrange
        isListening -> PrimaryOrange
        else -> PrimaryOrange.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier.size(size * 1.4f),
        contentAlignment = Alignment.Center
    ) {
        // Outer Neumorphic Glow Aura Ring
        if (!isMuted && (isListening || isSpeaking || isThinking)) {
            Box(
                modifier = Modifier
                    .size(size * auraScale)
                    .clip(CircleShape)
                    .background(orbColor.copy(alpha = 0.2f))
            )
        }

        // Mid Pulse Ring
        Box(
            modifier = Modifier
                .size(size * pulseScale)
                .clip(CircleShape)
                .background(orbColor.copy(alpha = 0.35f))
        )

        // Core Neumorphic Orb Button
        NeumorphicIconButton(
            onClick = onClick,
            size = size,
            shape = CircleShape,
            modifier = Modifier.scale(if (isListening || isSpeaking) pulseScale else 1.0f)
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(orbColor)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "MAX",
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            isMuted -> "MUTED"
                            isThinking -> "THINKING..."
                            isSpeaking -> "SPEAKING..."
                            isListening -> "LISTENING..."
                            else -> "TAP TO SPEAK"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun NeumorphicBottomNavigation(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = DarkShadowColor,
                spotColor = DarkShadowColor.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                val interactionSource = remember { MutableInteractionSource() }

                val itemColor = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                val backgroundColor = if (selected) SoftOrangeContainer else Color.Transparent

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onNavigate(screen) }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) screen.activeIcon else screen.inactiveIcon,
                            contentDescription = screen.title,
                            tint = itemColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = screen.title,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = itemColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NeumorphicProviderBadge(
    providerName: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor = if (isEnabled) PrimaryOrange else StatusError
    val statusText = if (isEnabled) "Active" else "Off"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$providerName: $statusText",
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeumorphicTopAppBar(
    isOpenRouterEnabled: Boolean,
    isPollinationsEnabled: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MAX",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "AI ASSISTANT",
                        fontWeight = FontWeight.Light,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    NeumorphicProviderBadge(providerName = "OpenRouter", isEnabled = isOpenRouterEnabled)
                    Spacer(modifier = Modifier.width(6.dp))
                    NeumorphicProviderBadge(providerName = "Pollinations", isEnabled = isPollinationsEnabled)
                }
            }
        },
        navigationIcon = {
            NeumorphicIconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                size = 38.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            NeumorphicIconButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(end = 12.dp),
                size = 38.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = PrimaryOrange
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
