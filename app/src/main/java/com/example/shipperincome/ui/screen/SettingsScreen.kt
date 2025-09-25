package com.example.shipperincome.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shipperincome.ui.theme.*
import com.example.shipperincome.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {

    // Keyboard controller để ẩn bàn phím
    val keyboardController = LocalSoftwareKeyboardController.current

    // Gradient Background giống với các screen khác
    val gradientBrush = Brush.verticalGradient(colors = listOf(GreenStart, GreenEnd))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Header
        Text(
            "Cài Đặt",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )


        // Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = 4.dp,
            backgroundColor = Beige.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Thông Số Tính Toán",
                    style = MaterialTheme.typography.h6,
                    color = Brown,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Tax Rate Input
                Text(
                    "Tỷ lệ thuế (%)",
                    color = Brown,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = settingsViewModel.taxRateInput,
                    onValueChange = { value -> settingsViewModel.updateTaxRateInput(value) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Brown,
                        unfocusedBorderColor = Brown.copy(alpha = 0.5f),
                        textColor = Color.DarkGray,
                        focusedLabelColor = Brown,
                        unfocusedLabelColor = Brown.copy(alpha = 0.7f)
                    ),
                    placeholder = { Text("5.7", color = Color.Gray) },
                    trailingIcon = { Text("%", color = Brown) }
                )

                // Error message for tax rate
                if (settingsViewModel.taxRateError != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Lỗi", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(settingsViewModel.taxRateError!!, color = Color.Red, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Delivery Fee Input
                Text(
                    "Phí mỗi đơn hàng (₩)",
                    color = Brown,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = settingsViewModel.deliveryFeeInput,
                    onValueChange = { value -> settingsViewModel.updateDeliveryFeeInput(value) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Brown,
                        unfocusedBorderColor = Brown.copy(alpha = 0.5f),
                        textColor = Color.DarkGray,
                        focusedLabelColor = Brown,
                        unfocusedLabelColor = Brown.copy(alpha = 0.7f)
                    ),
                    placeholder = { Text("100", color = Color.Gray) },
                    trailingIcon = { Text("₩", color = Brown) }
                )

                // Error message for delivery fee
                if (settingsViewModel.deliveryFeeError != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Lỗi", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(settingsViewModel.deliveryFeeError!!, color = Color.Red, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Save Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            settingsViewModel.saveSettings()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Brown),
                        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Lưu", tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("LƯU", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Reset Button
                    Button(
                        onClick = { settingsViewModel.showResetDialog() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray),
                        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Khôi phục", tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("KHÔI PHỤC", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Help/Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            backgroundColor = Brown.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Hướng dẫn",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    "• Tỷ lệ thuế: Phần trăm thuế được trừ từ thu nhập gộp",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "• Phí đơn hàng: Số tiền bị trừ cho mỗi đơn hàng (chỉ áp dụng cho ca Plus)",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "• Giá trị mặc định: Thuế 5.7%, Phí đơn hàng 100₩",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Reset Confirmation Dialog
    if (settingsViewModel.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.hideResetDialog() },
            title = {
                Text(
                    "Khôi phục về mặc định",
                    fontWeight = FontWeight.Bold,
                    color = Brown
                )
            },
            text = {
                Text(
                    "Bạn có chắc muốn khôi phục về giá trị mặc định?\n\n• Thuế: 5.7%\n• Phí đơn hàng: 100₩",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = { settingsViewModel.resetToDefaults() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Brown)
                ) {
                    Text("KHÔI PHỤC", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { settingsViewModel.hideResetDialog() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
                ) {
                    Text("HỦY", color = Color.White)
                }
            }
        )
    }
}