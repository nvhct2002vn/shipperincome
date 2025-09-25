package com.example.shipperincome.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// Define Color Schemes using Material 2 colors
private val DarkColorPalette = darkColors(
    primary = Brown, // Màu nâu làm màu chính tối
    primaryVariant = GreenEnd, // Biến thể xanh đậm
    secondary = Beige, // Màu be làm màu phụ
    background = Color(0xFF121212), // Nền tối chuẩn
    surface = Color(0xFF202020), // Bề mặt tối hơn chút
    onPrimary = Color.White,
    onSecondary = Brown,
    onBackground = Beige,
    onSurface = Beige.copy(alpha = 0.9f),
    error = RedDark
    // Bạn có thể tùy chỉnh thêm các màu khác onError, secondaryVariant...
)

private val LightColorPalette = lightColors(
    primary = Brown, // Màu nâu làm màu chính sáng
    primaryVariant = GreenEnd, // Biến thể xanh đậm
    secondary = GreenStart, // Xanh lá nhạt làm màu phụ
    background = Color.White, // Nền trắng sáng
    surface = Beige, // Bề mặt màu Be
    onPrimary = Color.White,
    onSecondary = Brown, // Chữ/icon trên nền xanh lá nhạt
    onBackground = Color.Black, // Chữ đen trên nền trắng
    onSurface = Brown, // Chữ/icon trên nền Be
    error = RedDark

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ShipperIncomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ - Tắt dynamic color vì dùng màu tùy chỉnh
    // dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    // Tắt Dynamic Color nếu dùng Material 3
    /*
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    */

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Đặt màu thanh trạng thái (status bar) - Có thể tùy chỉnh màu khác
            window.statusBarColor = colors.primaryVariant.toArgb() // Hoặc màu gradient? (Khó hơn)
            // Đặt icon thanh trạng thái sáng hay tối
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme // true nếu nền sáng, false nếu nền tối
        }
    }

    // Sử dụng MaterialTheme của Material 2
    MaterialTheme(
        colors = colors,
        typography = Typography, // Sử dụng Typography từ Type.kt
        shapes = Shapes, // Sử dụng Shapes từ Shape.kt (Mặc định hoặc tùy chỉnh)
        content = content
    )

    // Nếu dùng Material 3
    /*
    androidx.compose.material3.MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
    */
}