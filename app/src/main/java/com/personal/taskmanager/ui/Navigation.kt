package com.personal.taskmanager.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.personal.taskmanager.ui.calendar.CalendarScreen
import com.personal.taskmanager.ui.settings.SettingsScreen
import com.personal.taskmanager.ui.tasks.TasksScreen

object Routes {
    const val TASKS = "tasks"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            TasksScreen(
                onNavigateToCalendar = { navController.navigate(Routes.CALENDAR) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
