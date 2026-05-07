package com.personal.taskmanager.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.personal.taskmanager.ui.archive.ArchiveScreen
import com.personal.taskmanager.ui.calendar.CalendarScreen
import com.personal.taskmanager.ui.routines.RoutinesScreen
import com.personal.taskmanager.ui.settings.SettingsScreen
import com.personal.taskmanager.ui.tasks.TasksScreen
import com.personal.taskmanager.ui.theme.ThemeViewModel

object Routes {
    const val TASKS = "tasks"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val ROUTINES = "routines"
    const val ARCHIVE = "archive"
}

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.TASKS) {
        composable(Routes.TASKS) {
            TasksScreen(
                onNavigateToCalendar = { navController.navigate(Routes.CALENDAR) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToRoutines = { navController.navigate(Routes.ROUTINES) },
                onNavigateToArchive = { navController.navigate(Routes.ARCHIVE) }
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.ROUTINES) {
            RoutinesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.ARCHIVE) {
            ArchiveScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                themeViewModel = themeViewModel
            )
        }
    }
}
