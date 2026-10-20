package com.example.ui.screens.models

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OpenRouterModelItem
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.PrimaryOrange

@Composable
fun ModelsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterChip by remember { mutableStateOf("All") }

    val filteredModels = availableModels.filter { item ->
        val matchesQuery = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true) || item.id.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilterChip) {
            "Free" -> item.isFree
            "Llama" -> item.id.contains("llama", ignoreCase = true)
            "Gemini" -> item.id.contains("gemini", ignoreCase = true)
            "Claude" -> item.id.contains("claude", ignoreCase = true)
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("MODELS", fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search models...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Filters
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val chips = listOf("All", "Free", "Llama", "Gemini", "Claude")
            items(chips) { chip ->
                FilterChip(
                    selected = selectedFilterChip == chip,
                    onClick = { selectedFilterChip = chip },
                    label = { Text(chip, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Models List (${filteredModels.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(8.dp))

        // Models List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredModels) { item ->
                val isSelected = settings.selectedOpenRouterModel == item.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectOpenRouterModel(item.id, item.name)
                            Toast.makeText(context, "Selected ${item.name}", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) PrimaryOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Model",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (item.isFree) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PrimaryOrange.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "FREE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryOrange,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(item.id, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = PrimaryOrange
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fetch Models Button
        Button(
            onClick = { viewModel.fetchOpenRouterModels() },
            enabled = !isFetchingModels,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
        ) {
            if (isFetchingModels) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fetching Models...")
            } else {
                Text("Fetch Models", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
