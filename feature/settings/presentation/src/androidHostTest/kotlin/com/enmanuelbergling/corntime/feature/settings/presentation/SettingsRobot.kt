package com.enmanuelbergling.corntime.feature.settings.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enmanuelbergling.corntime.feature.settings.presentation.util.setThemedContent

/** Encapsulates every interaction with [SettingsScreen] so tests read as intent. */
class SettingsRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<SettingsAction>()
    var backs = 0
        private set
    var logins = 0
        private set

    fun show(state: SettingsState) = apply {
        rule.setThemedContent {
            SettingsScreen(
                state = state,
                onAction = { actions += it },
                onBack = { backs++ },
                onLogin = { logins++ },
            )
        }
    }

    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text).assertIsDisplayed() }
    fun assertLoginVisible() = apply { rule.onNodeWithTag(SettingsTags.LOGIN).assertIsDisplayed() }
    fun assertLoginAbsent() = apply { rule.onNodeWithTag(SettingsTags.LOGIN).assertDoesNotExist() }
    fun assertLogoutVisible() = apply { rule.onNodeWithTag(SettingsTags.LOGOUT).assertIsDisplayed() }
    fun assertLogoutAbsent() = apply { rule.onNodeWithTag(SettingsTags.LOGOUT).assertDoesNotExist() }

    fun clickLogin() = apply { rule.onNodeWithTag(SettingsTags.LOGIN).performClick() }
    fun clickLogout() = apply { rule.onNodeWithTag(SettingsTags.LOGOUT).performClick() }
    fun clickBack() = apply { rule.onNodeWithContentDescription("icon back").performClick() }
    fun openDarkThemeMenu() = apply { rule.onNodeWithTag(SettingsTags.DARK_THEME).performClick() }
    fun openDynamicColorMenu() = apply { rule.onNodeWithTag(SettingsTags.DYNAMIC_COLOR).performClick() }
    fun choose(option: String) = apply { rule.onNodeWithText(option).performClick() }
}
