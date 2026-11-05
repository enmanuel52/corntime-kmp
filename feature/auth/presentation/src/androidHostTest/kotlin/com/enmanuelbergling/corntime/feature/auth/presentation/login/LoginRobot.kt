package com.enmanuelbergling.corntime.feature.auth.presentation.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.enmanuelbergling.corntime.feature.auth.presentation.util.setThemedContent

/** Encapsulates every interaction with [LoginScreen] so tests read as intent. */
class LoginRobot(private val rule: ComposeContentTestRule) {
    val actions = mutableListOf<LoginAction>()
    var backs = 0
        private set

    fun show(state: LoginState) = apply {
        rule.setThemedContent {
            LoginScreen(state = state, onAction = { actions += it }, onBack = { backs++ })
        }
    }

    fun assertFormVisible() = apply {
        rule.onNodeWithTag(LoginTags.USERNAME).assertIsDisplayed()
        rule.onNodeWithTag(LoginTags.PASSWORD).assertIsDisplayed()
        rule.onNodeWithTag(LoginTags.SUBMIT).assertIsDisplayed()
    }

    fun assertLoadingVisible() = apply { rule.onNodeWithTag(LoginTags.LOADING).assertIsDisplayed() }
    fun assertFormAbsent() = apply { rule.onNodeWithTag(LoginTags.SUBMIT).assertDoesNotExist() }
    fun assertTextVisible(text: String) = apply { rule.onNodeWithText(text).assertIsDisplayed() }
    fun assertTextAbsent(text: String) = apply { rule.onNodeWithText(text).assertDoesNotExist() }
    fun assertSubmitEnabled() = apply { rule.onNodeWithTag(LoginTags.SUBMIT).assertIsEnabled() }
    fun assertSubmitDisabled() = apply { rule.onNodeWithTag(LoginTags.SUBMIT).assertIsNotEnabled() }

    fun typeUsername(text: String) = apply { rule.onNodeWithTag(LoginTags.USERNAME).performTextInput(text) }
    fun typePassword(text: String) = apply { rule.onNodeWithTag(LoginTags.PASSWORD).performTextInput(text) }
    fun clickSubmit() = apply { rule.onNodeWithTag(LoginTags.SUBMIT).performClick() }
    fun clickPasswordVisibility() = apply {
        rule.onNodeWithContentDescription("password visibility icon").performClick()
    }
    fun clickBack() = apply { rule.onNodeWithContentDescription("arrow back").performClick() }
    fun clickDismissDialog(label: String) = apply { rule.onNodeWithText(label).performClick() }
}
