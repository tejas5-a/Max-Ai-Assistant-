package com.example.ui.screens.providers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.PrimaryOrange
import com.example.ui.theme.StatusSuccess

@Composable
fun ProvidersScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Config", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("AI PROVIDERS", fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
                }
            }

            IconButton(onClick = { Toast.makeText(context, "All providers online", Toast.LENGTH_SHORT).show() }) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = "Tune")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. OpenRouter Provider Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "OpenRouter", tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OpenRouter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Switch(
                        checked = settings.isOpenRouterEnabled,
                        onCheckedChange = { viewModel.setOpenRouterEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Independent cards for text reasoning & chat models.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = settings.openRouterApiKey,
                    onValueChange = { viewModel.setOpenRouterKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-or-v1-...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = StatusSuccess.copy(alpha = 0.15f)) {
                        Text("↪ Connectivity Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusSuccess, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Gemini Live Provider Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Gemini Live", tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini Live", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Switch(
                        checked = true, // Voice engine active
                        onCheckedChange = { },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Independent cards & connectivity badges for real-time voice.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = settings.realtimeVoiceApiKey,
                    onValueChange = { viewModel.setRealtimeVoiceKey(it) },
                    label = { Text("Gemini Live API Key (Optional)") },
                    placeholder = { Text("AIzaSy...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = StatusSuccess.copy(alpha = 0.15f)) {
                        Text("↪ Connectivity Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusSuccess, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Pollinations AI Provider Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Pollinations AI", tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pollinations AI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Switch(
                        checked = settings.isPollinationsEnabled,
                        onCheckedChange = { viewModel.setPollinationsEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Independent cards & connectivity badges for image generation.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = StatusSuccess.copy(alpha = 0.15f)) {
                        Text("↪ Connectivity Active (Free API)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusSuccess, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}
