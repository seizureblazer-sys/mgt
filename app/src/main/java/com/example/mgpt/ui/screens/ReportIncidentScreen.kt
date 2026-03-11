package com.example.mgpt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mgpt.data.IncidentPriority
import com.example.mgpt.data.IncidentType
import com.example.mgpt.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    user: User,
    onReport: (IncidentType, IncidentPriority, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(IncidentType.HOSTILE) }
    var selectedPriority by remember { mutableStateOf(IncidentPriority.HIGH) }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "INTEL SUBMISSION",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "REPORTER: ${user.username} | SECTOR: ${user.hqId ?: "UNKNOWN"}",
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("INCIDENT CLASSIFICATION", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IncidentType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("THREAT LEVEL", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IncidentPriority.entries.forEach { priority ->
                FilterChip(
                    selected = selectedPriority == priority,
                    onClick = { selectedPriority = priority },
                    label = { Text(priority.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when(priority) {
                            IncidentPriority.CRITICAL -> MaterialTheme.colorScheme.error
                            IncidentPriority.HIGH -> MaterialTheme.colorScheme.tertiary
                            IncidentPriority.LOW -> MaterialTheme.colorScheme.secondary
                        }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("SITUATION DESCRIPTION") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onReport(selectedType, selectedPriority, description) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text("BROADCAST TO COMMAND", fontWeight = FontWeight.Bold)
        }
    }
}
