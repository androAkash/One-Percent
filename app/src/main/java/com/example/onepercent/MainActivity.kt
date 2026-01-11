package com.example.onepercent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.onepercent.database.TaskDatabase
import com.example.onepercent.local.repository.TaskRepository
import com.example.onepercent.navigation.DashboardScreen
import com.example.onepercent.navigation.HistoryScreen
import com.example.onepercent.ui.screens.Home.DashboardScreenUi
import com.example.onepercent.ui.screens.Home.HistoryScreenUi
import com.example.onepercent.ui.theme.OnePercentTheme
import com.example.onepercent.ui.viewModel.TaskViewModel
import com.example.onepercent.ui.viewModel.TaskViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels {
        val database = TaskDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao(),
            database.priorityCompletionDao())
        TaskViewModelFactory(repository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnePercentTheme {
                val backStack = rememberNavBackStack(DashboardScreen())
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { key ->
                        when (key) {

                            is DashboardScreen -> NavEntry(key) {
                                DashboardScreenUi(
                                    viewModel = viewModel,
                                    onNavigateToHistory = {
                                        backStack.add(HistoryScreen())
                                    }
                                )
                            }

                            is HistoryScreen -> NavEntry(key) {
                                HistoryScreenUi(
                                    viewModel = viewModel,
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            }

                            else -> error("Unknown NavKey")
                        }
                    }
                )
            }
        }
    }
}