package com.enmanuelbergling.corntime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.theme.CornTimeTheme
import com.enmanuelbergling.corntime.navigation.rememberCornTimeAppState
import com.enmanuelbergling.corntime.shared.AppAction
import com.enmanuelbergling.corntime.shortcut.ListShortcutManager
import com.enmanuelbergling.corntime.shortcut.ListTarget
import com.enmanuelbergling.corntime.shared.AppViewModel
import com.enmanuelbergling.corntime.ui.CornTimeApp
import com.enmanuelbergling.corntime.ui.OnboardingScreen
import com.enmanuelbergling.corntime.ui.observeOnline
import org.koin.androidx.viewmodel.ext.android.viewModel

class CornTimeActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModel()
    private val shortcuts by lazy { ListShortcutManager(this) }

    /** A list asked for by a launcher shortcut, opened once the app is on screen. */
    private var pendingList by mutableStateOf<ListTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingList = ListShortcutManager.targetOf(intent)

        // keep the splash until the theme, account and onboarding flag are known: no flash of defaults
        splash.setKeepOnScreenCondition { viewModel.state.value.isLoading }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val isOnline by observeOnline().collectAsStateWithLifecycle(initialValue = true)

            if (!state.isLoading) {
                val appState = rememberCornTimeAppState(isOnline = isOnline, isLoggedIn = state.isLoggedIn)

                LaunchedEffect(pendingList) {
                    pendingList?.let { target ->
                        appState.openList(target.listId, target.listName)
                        pendingList = null
                    }
                }

                CornTimeTheme(darkTheme = state.darkTheme, dynamicColor = state.dynamicColor) {
                    CornTimeApp(
                        state = appState,
                        shortcuts = shortcuts,
                        onLoggedOut = shortcuts::removeAll,
                    )

                    if (state.isOnboarding) {
                        OnboardingScreen(onFinish = { viewModel.onAction(AppAction.OnFinishOnboarding) })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingList = ListShortcutManager.targetOf(intent)
    }
}
