package com.example.shipperincome.ui.theme

import androidx.compose.material.Typography // Material 2
// import androidx.compose.material3.Typography // Material 3
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
// (Sử dụng Typography của Material 2)
val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    h5 = TextStyle(
         fontFamily = FontFamily.Default, // Có thể đổi font khác nếu muốn
         fontWeight = FontWeight.Bold,
         fontSize = 24.sp,
         lineHeight = 28.sp,
         letterSpacing = 0.sp
    ),
     h6 = TextStyle(
         fontFamily = FontFamily.Default,
         fontWeight = FontWeight.SemiBold, // Hơi đậm
         fontSize = 20.sp,
         lineHeight = 24.sp,
         letterSpacing = 0.15.sp
     ),
     subtitle1 = TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Medium,
          fontSize = 16.sp,
          lineHeight = 24.sp,
          letterSpacing = 0.15.sp
      ),
     button = TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Bold, // Chữ button đậm
          fontSize = 14.sp,
          lineHeight = 16.sp,
          letterSpacing = 1.25.sp // Khoảng cách chữ hơi rộng
      ),
      caption = TextStyle(
           fontFamily = FontFamily.Default,
           fontWeight = FontWeight.Normal,
           fontSize = 12.sp,
           lineHeight = 16.sp,
           letterSpacing = 0.4.sp
       )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)