package com.enmanuelbergling.corntime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.theme.CornTimeTheme
import com.enmanuelbergling.corntime.navigation.rememberCornTimeAppState
import com.enmanuelbergling.corntime.shared.AppAction
import com.enmanuelbergling.corntime.shared.AppViewModel
import com.enmanuelbergling.corntime.ui.CornTimeApp
import com.enmanuelbergling.corntime.ui.OnboardingScreen
import com.enmanuelbergling.corntime.ui.observeOnline
import org.koin.androidx.viewmodel.ext.android.viewModel

class CornTimeActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // keep the splash until the theme, account and onboarding flag are known: no flash of defaults
        splash.setKeepOnScreenCondition { viewModel.state.value.isLoading }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val isOnline by observeOnline().collectAsStateWithLifecycle(initialValue = true)

            if (!state.isLoading) {
                CornTimeTheme(darkTheme = state.darkTheme, dynamicColor = state.dynamicColor) {
                    CornTimeApp(
                        state = rememberCornTimeAppState(isOnline = isOnline, isLoggedIn = state.isLoggedIn),
                    )

                    if (state.isOnboarding) {
                        OnboardingScreen(onFinish = { viewModel.onAction(AppAction.OnFinishOnboarding) })
                    }
                }
            }
        }
    }
}
