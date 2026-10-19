package com.example.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.voice.VoiceState
import com.example.ui.components.InsetNeumorphicField
import com.example.ui.components.NeumorphicIconButton
import com.example.ui.theme.DarkShadowColor
import com.example.ui.theme.DeepOrangePressed
import com.example.ui.theme.LightOrangeHighlight
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusSuccess
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToLiveVoice: () -> Unit,
    onNavigateToChat: (String?) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProductivity: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var quickPrompt by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Minimal Header Status Area Above Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MAX LIVE VOICE READY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap Orb to Talk with MAX",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hands-free continuous conversational AI",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Central Focus - Prominent Large Circular Animated Live Voice Orb
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HomeLiveVoiceOrb(
                    onClick = onNavigateToLiveVoice,
                    voiceState = VoiceState.IDLE,
                    orbSize = 210.dp
                )
            }

            // Bottom Section - Clean Spacious Quick Destination Pills & Minimal Prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // Quick Ask Bar
                InsetNeumorphicField(
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Quick AI",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = quickPrompt,
                            onValueChange = { quickPrompt = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask MAX anything...", fontSize = 14.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryOrange,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        NeumorphicIconButton(
                            onClick = {
                                if (quickPrompt.isNotBlank()) {
                                    onNavigateToChat(quickPrompt)
                                    quickPrompt = ""
                                }
                            },
                            size = 36.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "QUICK DESTINATIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 2x2 Neumorphic Pill Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeumorphicDestinationPill(
                        modifier = Modifier.weight(1f),
                        title = "Chat",
                        icon = Icons.Default.ChatBubble,
                        onClick = { onNavigateToChat(null) }
                    )
                    NeumorphicDestinationPill(
                        modifier = Modifier.weight(1f),
                        title = "Create",
                        icon = Icons.Default.Image,
                        onClick = onNavigateToCreate
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeumorphicDestinationPill(
                        modifier = Modifier.weight(1f),
                        title = "Search",
                        icon = Icons.Default.Search,
                        onClick = onNavigateToSearch
                    )
                    NeumorphicDestinationPill(
                        modifier = Modifier.weight(1f),
                        title = "Tasks",
                        icon = Icons.Default.CheckCircle,
                        onClick = onNavigateToProductivity
                    )
                }
            }
        }
    }
}

@Composable
fun HomeLiveVoiceOrb(
    onClick: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    orbSize: Dp = 210.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HomeVoiceOrbTransition")

    // Slow, soothing breathing scale animation when idle
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    // Gentle rotating aura rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraRotation"
    )

    // Neumorphic Outer Raised Container
    Surface(
        modifier = Modifier
            .size(orbSize * 1.35f)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = DarkShadowColor,
                spotColor = PrimaryOrange.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Soft Radial Glow & Audio Reactive Rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val radius = (minOf(size.width, size.height) / 2f) * breathingScale

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryOrange.copy(alpha = 0.28f),
                            LightOrangeHighlight.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = radius * 1.28f
                    ),
                    radius = radius * 1.28f,
                    center = centerOffset
                )

                // Soft Orbiting Dots
                for (i in 0 until 3) {
                    val angle = Math.toRadians((rotationAngle + (i * 120)).toDouble())
                    val orbitX = (centerOffset.x + (radius * 1.06f) * cos(angle)).toFloat()
                    val orbitY = (centerOffset.y + (radius * 1.06f) * sin(angle)).toFloat()
                    drawCircle(
                        color = PrimaryOrange.copy(alpha = 0.7f),
                        radius = 4.dp.toPx(),
                        center = Offset(orbitX, orbitY)
                    )
                }
            }

            // Inner Gradient Neumorphic Orb Core
            Box(
                modifier = Modifier
                    .size(orbSize * breathingScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LightOrangeHighlight,
                                PrimaryOrange,
                                DeepOrangePressed
                            )
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Tap to talk with MAX",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "MAX",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "LIVE VOICE",
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
private fun NeumorphicDestinationPill(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = modifier
            .height(52.dp)
            .shadow(
                elevation = if (isPressed) 2.dp else 5.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = DarkShadowColor,
                spotColor = DarkShadowColor.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(SoftOrangeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
