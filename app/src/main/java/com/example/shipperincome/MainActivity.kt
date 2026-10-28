package com.example.shipperincome

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.shipperincome.ui.navigation.AppNavigation
import com.example.shipperincome.ui.theme.ShipperIncomeTheme
import com.example.shipperincome.ui.viewmodel.ShiftViewModel

class MainActivity : ComponentActivity() {

    private val shiftViewModel: ShiftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Giu man hinh splash toi khi doc xong du lieu ca lam, tranh nhay "chua co ca nao".
        splash.setKeepOnScreenCondition { !shiftViewModel.isLoaded }
        // Dau trang luon la nen gradient xanh dam nen icon thanh trang thai luon de mau sang.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            ShipperIncomeTheme {
                AppNavigation(viewModel = shiftViewModel)
            }
        }
    }
}
