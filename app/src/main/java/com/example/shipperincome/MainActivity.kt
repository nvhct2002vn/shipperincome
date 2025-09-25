package com.example.shipperincome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme // Material 2
import androidx.compose.material.Surface // Material 2
import androidx.compose.ui.Modifier
import com.example.shipperincome.ui.navigation.AppNavigation
import com.example.shipperincome.ui.theme.ShipperIncomeTheme
import com.example.shipperincome.ui.viewmodel.ShiftViewModel

class MainActivity : ComponentActivity() {

    // Khởi tạo ViewModel sử dụng activity-ktx
    private val shiftViewModel: ShiftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShipperIncomeTheme { // Áp dụng Theme đã định nghĩa
                // A surface container using the 'background' color from the theme (M2)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background // Lấy màu nền từ Theme
                ) {
                    // Gọi Composable chứa Navigation chính của ứng dụng
                    AppNavigation(viewModel = shiftViewModel)
                }
            }
        }
    }
}