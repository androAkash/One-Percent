package com.example.onepercent.ui.screens.Home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onepercent.ui.components.AddTaskDialog
import com.example.onepercent.ui.components.TaskItem
import com.example.onepercent.ui.viewModel.TaskViewModel
import com.example.onepercent.utils.getCurrentDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenUi(
    viewModel: TaskViewModel,
    onNavigateToHistory: () -> Unit
) {
    val priorityTasks by viewModel.priorityTask.collectAsState()
    val normalTasks by viewModel.normalTask.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Habit Tracker", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = getCurrentDate(),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Priority Tasks Section
            item {
                Text(
                    text = "Priority Tasks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (priorityTasks.isEmpty()) {
                item {
                    Text(
                        text = "No priority tasks yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                itemsIndexed(priorityTasks) { index, task ->
                    TaskItem(
                        task = task,
                        index = index,
                        totalCount = priorityTasks.size,
                        onDelete = { viewModel.deleteTask(task) },
                        onToggleComplete = { /*viewModel.toggleTaskCompletion(task)*/ }
                    )
                }
            }

            // Normal Tasks Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Normal Tasks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (normalTasks.isEmpty()) {
                item {
                    Text(
                        text = "No normal tasks yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                itemsIndexed(normalTasks) { index, task ->
                    TaskItem(
                        task = task,
                        index = index,
                        totalCount = normalTasks.size,
                        onDelete = { viewModel.deleteTask(task) },
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                    )
                }
            }
            item {
                TextButton(
                    onClick = onNavigateToHistory
                ) {
                    Text("View History")
                }
            }
        }
    }

    if (showDialog) {
        AddTaskDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, isPriority ->
                viewModel.addTask(name, isPriority)
                showDialog = false
            }
        )
    }
}