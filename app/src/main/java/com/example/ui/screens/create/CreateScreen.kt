package com.example.ui.screens.create

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.components.MaxCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusError
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    viewModel: CreateViewModel,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    val aspects = listOf("1:1", "16:9", "9:16", "4:3")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Prompt Input Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = "Studio", tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Pollinations Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("Describe the image you want Pollinations to generate...") },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Model Picker Chips
                    Text(text = "Pollinations Model:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availableModels.forEach { modelItem ->
                            FilterChip(
                                selected = state.settings.selectedPollinationsModel == modelItem.id,
                                onClick = { viewModel.selectModel(modelItem.id) },
                                label = { Text(modelItem.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Aspect Ratio Chips
                    Text(text = "Aspect Ratio:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        aspects.forEach { aspect ->
                            FilterChip(
                                selected = state.settings.defaultAspectRatio == aspect,
                                onClick = { viewModel.selectAspect(aspect) },
                                label = { Text(aspect, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.generateImage(prompt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        enabled = !state.isGenerating && prompt.isNotBlank()
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating Image...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Generate")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Error Banner
        if (state.errorMessage != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = StatusError.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        modifier = Modifier.padding(14.dp),
                        color = StatusError,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Result Preview Card
        val current = state.currentImageItem
        if (current != null) {
            item {
                Text(text = "Latest Generation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                MaxCard {
                    Column {
                        AsyncImage(
                            model = File(current.localFilePath),
                            contentDescription = current.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = current.prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Model: ${current.model} • Ratio: ${current.aspectRatio}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                Toast.makeText(context, "Saved to ${current.localFilePath}", Toast.LENGTH_LONG).show()
                            }) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "Save Image", tint = ElectricBlue)
                            }
                            IconButton(onClick = {
                                val file = File(current.localFilePath)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Image"))
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Image", tint = ElectricCyan)
                            }
                        }
                    }
                }
            }
        }

        // History Gallery
        if (state.historyItems.isNotEmpty()) {
            item {
                Text(text = "Generation History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.historyItems) { item ->
                        Surface(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .clickable {
                                    Toast.makeText(context, "Selected history item", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            AsyncImage(
                                model = File(item.localFilePath),
                                contentDescription = item.prompt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
