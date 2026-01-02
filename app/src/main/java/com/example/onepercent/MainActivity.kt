package com.example.onepercent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.onepercent.database.TaskDatabase
import com.example.onepercent.local.repository.TaskRepository
import com.example.onepercent.ui.screens.Home.DashboardScreen
import com.example.onepercent.ui.theme.OnePercentTheme
import com.example.onepercent.ui.viewModel.TaskViewModel
import com.example.onepercent.ui.viewModel.TaskViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels {
        val database = TaskDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnePercentTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}