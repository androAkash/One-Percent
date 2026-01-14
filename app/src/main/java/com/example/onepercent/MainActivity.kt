package com.example.onepercent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.onepercent.database.TaskDatabase
import com.example.onepercent.local.repository.TaskRepository
import com.example.onepercent.navigation.ArchiveScreen
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
                    entryProvider = { key ->
                        when (key) {

                            is DashboardScreen -> NavEntry(key) {
                                DashboardScreenUi(
                                    viewModel = viewModel,
                                    backStack = backStack
                                )
                            }

                            is HistoryScreen -> NavEntry(key) {
                                HistoryScreenUi(
                                    viewModel = viewModel,
                                    backStack = backStack
                                )
                            }

                            else -> error("Unknown NavKey")
                        }
                    },
                    transitionSpec = {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(1000)
                        ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                    },
                    popTransitionSpec = {
                        EnterTransition.None togetherWith slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(1000)
                        )
                    }
                )
            }
        }
    }
    override fun onResume() {
        super.onResume()
        viewModel.normalizeTasksIfDayChanged()
    }

}