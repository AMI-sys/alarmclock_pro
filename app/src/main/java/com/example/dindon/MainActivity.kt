package com.example.dindon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.dindon.ui.screens.MainScreen
import com.example.dindon.ui.theme.DindonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DindonTheme {
                MainScreen()
            }
        }
    }
}
