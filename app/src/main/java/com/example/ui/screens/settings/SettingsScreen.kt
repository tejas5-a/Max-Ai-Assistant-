package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusError
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceEngineMode
import com.example.data.voice.VoiceAssistantHelper
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusError
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProviders: () -> Unit = {},
    onNavigateToModels: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var apiKeyInput by remember(settings.openRouterApiKey) { mutableStateOf(settings.openRouterApiKey) }
    var realtimeKeyInput by remember(settings.realtimeVoiceApiKey) { mutableStateOf(settings.realtimeVoiceApiKey) }
    var isClearDataDialogOpen by remember { mutableStateOf(false) }
    var isAssistantInfoDialogOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Navigation to Provider Config & Model Management Screens
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToProviders,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.PrimaryOrange)
                ) {
                    Text("AI Providers Config", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToModels,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Model Management", fontSize = 12.sp)
                }
            }
        }

        // MAX Live Voice Assistant Settings Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Live Voice", tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "MAX Live Voice Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Choose your conversational voice engine:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mode Selection Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceEngineMode.values().forEach { mode ->
                            FilterChip(
                                selected = settings.voiceEngineMode == mode,
                                onClick = { viewModel.setVoiceEngineMode(mode) },
                                label = { Text(if (mode == VoiceEngineMode.REALTIME_AUDIO) "Mode A: Realtime" else "Mode B: Fallback", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = settings.voiceEngineMode.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (settings.voiceEngineMode == VoiceEngineMode.REALTIME_AUDIO) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = realtimeKeyInput,
                            onValueChange = {
                                realtimeKeyInput = it
                                viewModel.setRealtimeVoiceKey(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Realtime Audio Provider API Key") },
                            placeholder = { Text("sk-...") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hands-Free Loop Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Hands-Free Conversation Loop", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Automatically listens for next sentence when assistant stops speaking", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.isHandsFreeLoopEnabled,
                            onCheckedChange = { viewModel.setHandsFreeLoopEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interruption Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Allow Instant Speech Interruption", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Interrupt assistant speaking immediately when you tap or talk", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.isInterruptionEnabled,
                            onCheckedChange = { viewModel.setInterruptionEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue)
                        )
                    }
                }
            }
        }

        // Android System Default Assistant Setup Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = "Default Assistant", tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Android System Assistant Integration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You can designate MAX AI as your primary Assist app in Android system preferences.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val opened = VoiceAssistantHelper.openDefaultAssistantSettings(context)
                                if (!opened) {
                                    Toast.makeText(context, "Please open Android Settings -> Default Apps -> Assist", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open Assist Settings", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { isAssistantInfoDialogOpen = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Details & Limits", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // OpenRouter AI Text Provider Settings Card
        item {
            val availableModels by viewModel.availableModels.collectAsState()
            val isFetchingModels by viewModel.isFetchingModels.collectAsState()
            val fetchStatus by viewModel.fetchStatus.collectAsState()
            var isModelMenuExpanded by remember { mutableStateOf(false) }

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
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "OpenRouter", tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "OpenRouter Text AI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Switch(
                            checked = settings.isOpenRouterEnabled,
                            onCheckedChange = { viewModel.setOpenRouterEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Independent control for OpenRouter AI chat models (Llama 3.3, Gemini 2.0, DeepSeek R1, GPT-4o).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            viewModel.setOpenRouterKey(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OpenRouter API Key (Optional for free models)") },
                        placeholder = { Text("sk-or-v1-...") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Model Summary
                    Text(
                        text = "Active AI Model:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = settings.selectedOpenRouterModelName.ifBlank { settings.selectedOpenRouterModel.substringAfterLast("/") },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ElectricCyan
                            )
                            Text(
                                text = "ID: ${settings.selectedOpenRouterModel}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons: Fetch Models & Model Selector Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.fetchOpenRouterModels() },
                            enabled = !isFetchingModels,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetching...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Fetch Models",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch Models", fontSize = 12.sp)
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { isModelMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Select Model", fontSize = 12.sp)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Model"
                                )
                            }

                            DropdownMenu(
                                expanded = isModelMenuExpanded,
                                onDismissRequest = { isModelMenuExpanded = false }
                            ) {
                                availableModels.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    if (item.isFree) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = ElectricCyan.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "FREE",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ElectricCyan,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(text = item.id, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (item.description.isNotBlank()) {
                                                    Text(text = item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectOpenRouterModel(item.id, item.name)
                                            isModelMenuExpanded = false
                                            Toast.makeText(context, "Selected ${item.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (!fetchStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fetchStatus!!,
                            fontSize = 11.sp,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // Pollinations Image Provider Settings Card
        item {
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
                            Icon(imageVector = Icons.Default.Image, contentDescription = "Pollinations", tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Pollinations Image AI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Switch(
                            checked = settings.isPollinationsEnabled,
                            onCheckedChange = { viewModel.setPollinationsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Fully isolated provider toggle for Pollinations image generation. Toggling this does NOT affect OpenRouter.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Appearance / Theme Mode Settings
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Theme", tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Theme & Appearance", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.name.lowercase().capitalize(), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Language Settings
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = "Language", tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Preferred Language", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppLanguage.values().forEach { lang ->
                            FilterChip(
                                selected = settings.selectedLanguage == lang,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Speech Rate Controls
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Voice", tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Speech Rate & Voice Options", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Speaking Speed: ${settings.speechRate}x", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.85f, 1.0f, 1.15f).forEach { rate ->
                                FilterChip(
                                    selected = settings.speechRate == rate,
                                    onClick = { viewModel.setSpeechRate(rate) },
                                    label = { Text("${rate}x", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transparency & Cost Reporting Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Transparency & Rate Limits", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• Mode B (Conversational Fallback) uses on-device STT/TTS combined with OpenRouter text reasoning.\n" +
                                "• Free models on OpenRouter (:free suffix) have zero cost with standard rate limits.\n" +
                                "• Pollinations image generation is free and requires no key.\n" +
                                "• All conversations and productivity data are stored strictly locally on your device.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Clear Local History Action
        item {
            OutlinedButton(
                onClick = { isClearDataDialogOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
            ) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear Data")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Local Data & Chat History", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (isAssistantInfoDialogOpen) {
        val permissions = viewModel.getPermissionDashboard(context)
        val capabilities = viewModel.getCapabilityMatrix()

        AlertDialog(
            onDismissRequest = { isAssistantInfoDialogOpen = false },
            title = { Text("MAX Capability & Permission Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    item {
                        Text("PERMISSION DASHBOARD", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    items(permissions) { perm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(perm.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(perm.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (perm.isGranted) "GRANTED" else "NOT GRANTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (perm.isGranted) StatusSuccess else StatusError
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("CAPABILITY MATRIX", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    items(capabilities) { cap ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cap.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = cap.status.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }
                            Text("Category: ${cap.category} | Class: ${cap.implementationClass.substringAfterLast('.')}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(cap.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { isAssistantInfoDialogOpen = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (isClearDataDialogOpen) {
        AlertDialog(
            onDismissRequest = { isClearDataDialogOpen = false },
            title = { Text("Clear All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all conversations, tasks, and generated images from your phone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        isClearDataDialogOpen = false
                        Toast.makeText(context, "All local history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isClearDataDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
