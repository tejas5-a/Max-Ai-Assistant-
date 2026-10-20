package com.example.ui.screens.automation

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.automation.WorkflowParser
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.PrimaryOrange

@Composable
fun AutomationBuilderScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var workflowTitle by remember { mutableStateOf("Morning Briefing") }
    var triggerTime by remember { mutableStateOf("7:00 AM") }
    var selectedAction by remember { mutableStateOf("Fetch weather") }
    var isActionMenuExpanded by remember { mutableStateOf(false) }

    val actionOptions = listOf("Fetch weather", "Send morning briefing", "Toggle flashlight", "Summarize daily tasks")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BUILDER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Workflow Name
            OutlinedTextField(
                value = workflowTitle,
                onValueChange = { workflowTitle = it },
                label = { Text("Automation Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // WHEN Trigger Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time Trigger",
                                tint = PrimaryOrange
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("WHEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        Text(triggerTime, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Flow Connector Arrow
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Flow",
                    tint = PrimaryOrange,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // THEN Action Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Action",
                                tint = PrimaryOrange
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("THEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        Text(selectedAction, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isActionMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Select Action: $selectedAction")
                }
                DropdownMenu(
                    expanded = isActionMenuExpanded,
                    onDismissRequest = { isActionMenuExpanded = false }
                ) {
                    actionOptions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action) },
                            onClick = {
                                selectedAction = action
                                isActionMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // SAVE Action Button
        Button(
            onClick = {
                val promptText = "$workflowTitle at $triggerTime to $selectedAction"
                val workflow = WorkflowParser.parsePrompt(promptText)
                viewModel.saveWorkflow(workflow)
                Toast.makeText(context, "Saved automation '$workflowTitle'", Toast.LENGTH_SHORT).show()
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
        ) {
            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
