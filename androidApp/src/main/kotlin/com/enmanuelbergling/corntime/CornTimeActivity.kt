package com.enmanuelbergling.corntime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.enmanuelbergling.corntime.core.designsystem.theme.CornTimeTheme
import com.enmanuelbergling.corntime.navigation.CornTimeNavHost

class CornTimeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CornTimeTheme {
                CornTimeNavHost()
            }
        }
    }
}
