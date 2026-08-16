package com.sumit.pocketgpt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sumit.pocketgpt.presentation.navigation.PocketGptNavHost
import com.sumit.pocketgpt.ui.theme.PocketGPTTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketGPTTheme {
                PocketGptNavHost()
            }
        }
    }
}
