package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.SearchSource
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusError

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    initialPrompt: String? = null,
    onNavigateToLiveVoice: () -> Unit = {},
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(initialPrompt ?: "") }
    var isModelMenuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrEmpty()) {
            viewModel.sendMessage(initialPrompt)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Model Selector & Action Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dropdown Model Picker
                Box {
                    Surface(
                        onClick = { isModelMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.settings.selectedOpenRouterModelName.ifBlank { state.settings.selectedOpenRouterModel.substringAfterLast("/") },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isModelMenuExpanded,
                        onDismissRequest = { isModelMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Fetch Models",
                                        modifier = Modifier.size(16.dp),
                                        tint = ElectricCyan
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fetch Available Models", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                                }
                            },
                            onClick = {
                                viewModel.fetchModels()
                                Toast.makeText(context, "Fetching latest models...", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()
                        state.availableModels.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    viewModel.selectModel(item.id)
                                    isModelMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Controls: Live Voice, Web Search Toggle, New Conv, Clear History
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = false,
                        onClick = onNavigateToLiveVoice,
                        label = { Text("Live Voice", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Live Voice",
                                modifier = Modifier.size(14.dp),
                                tint = ElectricCyan
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ElectricCyan.copy(alpha = 0.15f),
                            labelColor = ElectricCyan
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    FilterChip(
                        selected = state.isWebSearchEnabled,
                        onClick = { viewModel.toggleWebSearch() },
                        label = { Text("Web Search", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat", tint = ElectricBlue)
                    }

                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages) { msg ->
                MessageBubble(
                    message = msg,
                    onSpeak = { viewModel.speakMessage(msg.content) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("MAX AI Answer", msg.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onOpenUrl = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }

            if (state.isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ElectricCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "MAX AI is generating response...", fontSize = 13.sp, color = ElectricCyan)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Voice Listening Overlay Banner
        if (state.isVoiceListening) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ElectricBlue.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Listening", tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (state.voiceTranscript.isNotBlank()) state.voiceTranscript else "Listening... Speak your prompt",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Bottom Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (state.isVoiceListening) viewModel.stopVoiceInput() else viewModel.startVoiceInput()
                    }
                ) {
                    Icon(
                        imageVector = if (state.isVoiceListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (state.isVoiceListening) StatusError else ElectricBlue
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message MAX AI...") },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !state.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = ElectricBlue
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val isUser = message.role == "user"
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isUser && message.model != null) {
                    Text(
                        text = message.model.substringAfterLast("/"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (message.isError) StatusError else if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )

                // Web Search Sources Cards
                if (message.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Retrieved Web Sources:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.sources.forEach { source ->
                            SourceChip(source = source, onClick = { onOpenUrl(source.url) })
                        }
                    }
                }

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Response", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceChip(
    source: SearchSource,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = ElectricBlue.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = source.domain, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open Source", tint = ElectricCyan, modifier = Modifier.size(10.dp))
        }
    }
}
