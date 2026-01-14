package com.example.onepercent.ui.screens.Home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.onepercent.navigation.ArchiveScreen
import com.example.onepercent.navigation.HistoryScreen
import com.example.onepercent.ui.components.AddTaskDialog
import com.example.onepercent.ui.components.HeatMapCalendar
import com.example.onepercent.ui.components.HeatMapEntry
import com.example.onepercent.ui.components.TaskItem
import com.example.onepercent.ui.viewModel.TaskViewModel
import com.example.onepercent.utils.getCurrentDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenUi(
    viewModel: TaskViewModel,
    backStack: NavBackStack<NavKey>
) {
    val priorityTasks by viewModel.priorityTask.collectAsState()
    val normalTasks by viewModel.normalTask.collectAsState()
    val priorityCompletions by viewModel.priorityCompletions.collectAsState()

    var isHeatMapExpanded by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    val heatMapData = remember(priorityCompletions) {
        priorityCompletions
            .groupBy { it.completedDate }
            .map { (date, completions) ->
                HeatMapEntry(
                    epochMillis = date,
                    importantTaskCount = completions.size
                )
            }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("1 percent", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = getCurrentDate(),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    /*TextButton(onClick = { *//*backStack.add(ArchiveScreen())*//* }) {
                        Text("Archived", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }*/
                    TextButton(onClick = { viewModel.forceResetPriorityTasks() }) {
                        Text("Reset Now", color = MaterialTheme.colorScheme.error)
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
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHeatMapExpanded = !isHeatMapExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Priority Task Streak",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isHeatMapExpanded)
                                    Icons.Default.ExpandLess
                                else
                                    Icons.Default.ExpandMore,
                                contentDescription = if (isHeatMapExpanded) "Collapse" else "Expand"
                            )
                        }

                        if (isHeatMapExpanded) {
                            HeatMapCalendar(
                                data = heatMapData,
                                year = LocalDate.now().year
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.clearHeatmapData() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Clear Heatmap Data", fontSize = 12.sp)
                                }
                            }
                        }
                    }
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
                    onClick = {backStack.add(HistoryScreen())}
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