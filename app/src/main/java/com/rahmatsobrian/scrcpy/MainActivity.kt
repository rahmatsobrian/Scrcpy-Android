package com.rahmatsobrian.scrcpy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rahmatsobrian.scrcpy.ui.AppRoot
import com.rahmatsobrian.scrcpy.ui.theme.ScrcpyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScrcpyTheme {
                AppRoot()
            }
        }
    }
}
