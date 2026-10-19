package com.example.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceEngineMode
import com.example.data.voice.VoiceState
import com.example.data.voice.VoiceTurn
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonBlueAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveVoiceScreen(
    viewModel: LiveVoiceViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.startSession()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.startSession()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.endSession()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.conversationHistory.size, state.currentTranscript, state.assistantSpokenText) {
        if (state.conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(state.conversationHistory.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Back / Privacy Indicator / Engine Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Voice Session", tint = MaterialTheme.colorScheme.onSurface)
                }

                // Microphone Privacy Indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.voiceState.isMicActive) StatusSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.voiceState.isMicActive) StatusSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.voiceState.isMicActive) StatusSuccess else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.voiceState.isMicActive) "MIC LIVE" else "MIC MUTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.voiceState.isMicActive) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mode Indicator Chip
                FilterChip(
                    selected = state.settings.voiceEngineMode == VoiceEngineMode.REALTIME_AUDIO,
                    onClick = {
                        val nextMode = if (state.settings.voiceEngineMode == VoiceEngineMode.REALTIME_AUDIO) {
                            VoiceEngineMode.CONVERSATIONAL_FALLBACK
                        } else {
                            VoiceEngineMode.REALTIME_AUDIO
                        }
                        viewModel.switchMode(nextMode)
                    },
                    label = {
                        Text(
                            text = if (state.settings.voiceEngineMode == VoiceEngineMode.REALTIME_AUDIO) "Mode A: Realtime" else "Mode B: Fallback",
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Animated MAX Orb Centerpiece
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (state.voiceState == VoiceState.SPEAKING || state.voiceState == VoiceState.GENERATING) {
                            viewModel.interrupt()
                        } else {
                            viewModel.pauseOrResume()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedMaxVoiceOrb(
                    voiceState = state.voiceState,
                    amplitude = state.audioAmplitude
                )

                // Center Status Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MAX",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Text(
                        text = state.voiceState.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricCyan
                    )
                }
            }

            // Interrupt Hint banner when speaking
            if (state.voiceState == VoiceState.SPEAKING) {
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.interrupt() },
                    color = ElectricBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "⚡ Tap orb or speak to interrupt MAX",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conversation History & Live Transcript Panel
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.conversationHistory) { turn ->
                        VoiceTurnBubble(turn = turn)
                    }

                    // Live User Transcript Preview
                    if (state.voiceState == VoiceState.LISTENING && state.currentTranscript.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElectricBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "“${state.currentTranscript}...”",
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 13.sp,
                                    color = ElectricCyan,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    // Processing / Generating indicator
                    if (state.voiceState == VoiceState.PROCESSING || state.voiceState == VoiceState.GENERATING) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MAX is thinking and reasoning...",
                                    fontSize = 13.sp,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }

                    if (state.errorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusError.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = state.errorMessage ?: "",
                                    modifier = Modifier.padding(10.dp),
                                    fontSize = 12.sp,
                                    color = StatusError
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Voice Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speaker Mute Output Button
                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (state.isOutputMuted) StatusError.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (state.isOutputMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute Output",
                        tint = if (state.isOutputMuted) StatusError else ElectricCyan
                    )
                }

                // Main Microphone / Hands-free Toggle Button
                Button(
                    onClick = { viewModel.pauseOrResume() },
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.voiceState == VoiceState.LISTENING) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (state.voiceState == VoiceState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone",
                        tint = if (state.voiceState == VoiceState.LISTENING) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.voiceState == VoiceState.LISTENING) "Listening Hands-Free" else "Paused",
                        fontWeight = FontWeight.Bold,
                        color = if (state.voiceState == VoiceState.LISTENING) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Stop / Interrupt Button
                IconButton(
                    onClick = { viewModel.interrupt() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt",
                        tint = ElectricCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedMaxVoiceOrb(
    voiceState: VoiceState,
    amplitude: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val dynamicScale = when (voiceState) {
        VoiceState.LISTENING -> (1f + (amplitude * 0.45f))
        VoiceState.SPEAKING -> pulseScale * 1.08f
        VoiceState.PROCESSING, VoiceState.GENERATING -> pulseScale
        else -> 1f
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = (size.minDimension / 2.6f) * dynamicScale

        // Outer Glow Ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ElectricCyan.copy(alpha = if (voiceState == VoiceState.LISTENING) 0.35f else 0.15f),
                    ElectricBlue.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius * 1.5f
            ),
            radius = baseRadius * 1.4f,
            center = center
        )

        // Middle Concentric Energy Ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ElectricBlue,
                    ElectricCyan.copy(alpha = 0.7f),
                    NeonBlueAccent.copy(alpha = 0.4f)
                ),
                center = center,
                radius = baseRadius
            ),
            radius = baseRadius,
            center = center
        )

        // Orbiting Particles during Processing / Speaking
        if (voiceState == VoiceState.PROCESSING || voiceState == VoiceState.GENERATING || voiceState == VoiceState.SPEAKING) {
            for (i in 0 until 4) {
                val angle = Math.toRadians((rotationAngle + (i * 90)).toDouble())
                val orbitX = (center.x + (baseRadius * 1.15f) * cos(angle)).toFloat()
                val orbitY = (center.y + (baseRadius * 1.15f) * sin(angle)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(orbitX, orbitY)
                )
            }
        }

        // Inner Core Orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    ElectricBlue,
                    Color(0xFF003882)
                ),
                center = center,
                radius = baseRadius * 0.7f
            ),
            radius = baseRadius * 0.7f,
            center = center
        )
    }
}

@Composable
private fun VoiceTurnBubble(turn: VoiceTurn) {
    val isUser = turn.speaker == "user"
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            color = if (isUser) ElectricBlue else MaterialTheme.colorScheme.surface,
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isUser) "You" else "MAX AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) Color.Black.copy(alpha = 0.7f) else ElectricCyan
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = turn.text,
                    fontSize = 13.sp,
                    color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface
                )

                // Action metadata chips
                if (turn.actionType != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚡ Action: ${turn.actionType} (${turn.actionDetail ?: "Executed"})",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}
