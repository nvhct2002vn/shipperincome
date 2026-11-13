package com.example.shipperincome.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shipperincome.ui.components.LocalSnackbarHostState
import com.example.shipperincome.ui.screen.InputScreen
import com.example.shipperincome.ui.screen.NotesScreen
import com.example.shipperincome.ui.screen.RankScreen
import com.example.shipperincome.ui.screen.SettingsScreen
import com.example.shipperincome.ui.screen.StatsScreen
import com.example.shipperincome.ui.viewmodel.NotesViewModel
import com.example.shipperincome.ui.viewmodel.RankViewModel
import com.example.shipperincome.ui.viewmodel.SettingsViewModel
import com.example.shipperincome.ui.viewmodel.ShiftViewModel

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val destinations = listOf(
    TopLevelDestination(Routes.INPUT, "Nhập liệu", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    TopLevelDestination(Routes.STATS, "Thống kê", Icons.Filled.Insights, Icons.Outlined.Insights),
    TopLevelDestination(Routes.RANK, "Rank", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    TopLevelDestination(Routes.NOTES, "Ghi chú", Icons.Filled.Place, Icons.Outlined.Place),
    TopLevelDestination(Routes.SETTINGS, "Cài đặt", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun AppNavigation(viewModel: ShiftViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // Khong de Scaffold chiem inset thanh trang thai; neu khong HeroHeader se khong
            // con inset de day noi dung xuong va chu bi de len dong ho.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToScreen(navController, destination.route) },
                            icon = {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            // Chi chua cho thanh dieu huong ben duoi; tung man hinh tu xu ly phan tren
            // de nen gradient tran ra sau thanh trang thai.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .consumeWindowInsets(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.INPUT,
                    enterTransition = { fadeIn(tween(220)) },
                    exitTransition = { fadeOut(tween(160)) }
                ) {
                    composable(Routes.INPUT) { InputScreen(viewModel = viewModel) }
                    composable(Routes.STATS) { StatsScreen(viewModel = viewModel) }
                    composable(Routes.RANK) {
                        val rankViewModel: RankViewModel = viewModel()
                        RankScreen(
                            viewModel = rankViewModel,
                            onOpenSettings = { navigateToScreen(navController, Routes.SETTINGS) }
                        )
                    }
                    composable(Routes.NOTES) {
                        val notesViewModel: NotesViewModel = viewModel()
                        NotesScreen(notesViewModel = notesViewModel)
                    }
                    composable(Routes.SETTINGS) {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        val notesViewModel: NotesViewModel = viewModel()
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            shiftViewModel = viewModel,
                            notesViewModel = notesViewModel
                        )
                    }
                }
            }
        }
    }
}

private fun navigateToScreen(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
