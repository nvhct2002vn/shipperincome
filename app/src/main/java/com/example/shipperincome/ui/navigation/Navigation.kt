package com.example.shipperincome.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.* // Material 2, nếu dùng M3 thì import khác
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit // Hoặc Home
import androidx.compose.material.icons.filled.List // Hoặc Assessment
import androidx.compose.material.icons.filled.Settings // Settings icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import màu
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shipperincome.ui.screen.InputScreen
import com.example.shipperincome.ui.screen.StatsScreen
import com.example.shipperincome.ui.screen.SettingsScreen
import com.example.shipperincome.ui.viewmodel.SettingsViewModel
import com.example.shipperincome.ui.theme.* // Import Theme colors
import com.example.shipperincome.ui.viewmodel.ShiftViewModel


@Composable
fun AppNavigation(viewModel: ShiftViewModel) {
    val navController = rememberNavController() // Tạo NavController cho BottomNav và NavHost

    // Màu sắc cho Bottom Nav (Lấy từ Theme hoặc định nghĩa ở đây)
    val bottomNavBackgroundColor = Brown // Ví dụ
    val selectedItemColor = Color.White // Ví dụ
    val unselectedItemColor = Beige.copy(alpha = 0.7f) // Ví dụ


    Scaffold(
        bottomBar = {
            BottomNavigation(
                backgroundColor = bottomNavBackgroundColor,
                contentColor = selectedItemColor // Màu mặc định cho icon và text được chọn
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Item cho màn hình Input
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Edit, contentDescription = "Nhập liệu") },
                    label = { Text("Nhập Liệu") },
                    selected = currentDestination?.hierarchy?.any { it.route == Routes.INPUT } == true,
                    onClick = {
                        navigateToScreen(navController, Routes.INPUT)
                    },
                    selectedContentColor = selectedItemColor,
                    unselectedContentColor = unselectedItemColor
                )
                 // Item cho màn hình Stats
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Thống kê") },
                    label = { Text("Thống Kê") },
                    selected = currentDestination?.hierarchy?.any { it.route == Routes.STATS } == true,
                    onClick = {
                        navigateToScreen(navController, Routes.STATS)
                    },
                    selectedContentColor = selectedItemColor,
                    unselectedContentColor = unselectedItemColor
                )
                // Item cho màn hình Settings
                BottomNavigationItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Cài đặt") },
                    label = { Text("Cài Đặt") },
                    selected = currentDestination?.hierarchy?.any { it.route == Routes.SETTINGS } == true,
                    onClick = {
                        navigateToScreen(navController, Routes.SETTINGS)
                    },
                    selectedContentColor = selectedItemColor,
                    unselectedContentColor = unselectedItemColor
                )
            }
        }
    ) { innerPadding ->
        // NavHost chứa nội dung màn hình, áp dụng padding từ Scaffold
        NavHost(
             navController = navController,
             startDestination = Routes.INPUT,
             modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.INPUT) { InputScreen(viewModel = viewModel, navController = navController) } // Truyền navController nếu cần
            composable(Routes.STATS) { StatsScreen(viewModel = viewModel, navController = navController) } // Truyền navController nếu cần
            composable(Routes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel() // Tạo SettingsViewModel instance
                SettingsScreen(settingsViewModel = settingsViewModel)
            }
        }
    }
}

// Helper function để tránh lặp code điều hướng
private fun navigateToScreen(navController: NavHostController, route: String) {
    navController.navigate(route) {
        // Pop up to the start destination of the graph to avoid building up a large back stack
        // when selecting items
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
} 