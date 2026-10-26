package com.example.notificationlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notificationlog.ui.AppRoot
import com.example.notificationlog.ui.theme.NotificationLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NotificationLogTheme {
                AppRoot()
            }
        }
    }
}
