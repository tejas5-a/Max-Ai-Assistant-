package com.example.ui.screens.search

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SearchSource
import com.example.ui.components.MaxCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StatusError

@Composable
fun SearchScreen(
    viewModel: SearchViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var queryInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Query Card
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
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Web Search & Research", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        FilterChip(
                            selected = state.isDeepResearchMode,
                            onClick = { viewModel.toggleDeepResearch() },
                            label = { Text("Deep Research", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter topic or query to search real web sources...") },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.performSearch(queryInput) }) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Run Search", tint = ElectricBlue)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Searching State
        if (state.isSearching) {
            item {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricCyan)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Fetching live web sources...", fontSize = 14.sp, color = ElectricCyan)
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

        // AI Research Synthesis Card
        val synthesis = state.researchSynthesis
        if (synthesis != null) {
            item {
                MaxCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Research", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "AI Research Synthesis", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ElectricCyan)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = synthesis, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Real Sources List Header
        if (state.sources.isNotEmpty()) {
            item {
                Text(text = "Retrieved Web Sources (${state.sources.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(state.sources) { source ->
                SourceResultCard(
                    source = source,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun SourceResultCard(
    source: SearchSource,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = source.domain,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open Link", tint = ElectricCyan, modifier = Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = source.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = source.snippet, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
