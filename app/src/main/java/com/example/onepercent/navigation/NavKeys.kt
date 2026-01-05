package com.example.onepercent.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class DashboardScreen(val refreshTrigger: Int = 0) : NavKey
@Serializable
data class ArchiveScreen(val refreshTrigger: Int = 0) : NavKey
@Serializable
data class HistoryScreen(val refreshTrigger: Int = 0) : NavKey
