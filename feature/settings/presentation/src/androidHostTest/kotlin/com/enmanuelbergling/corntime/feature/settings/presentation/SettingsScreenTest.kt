package com.enmanuelbergling.corntime.feature.settings.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import com.enmanuelbergling.corntime.feature.settings.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.settings.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class SettingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { SettingsRobot(rule) }
    private val tim = UserUi(username = "tim", avatarPath = "")

    @Test
    fun givenLoggedOut_whenShown_thenLoginIsOfferedAndLogoutIsNot() {
        robot.show(SettingsState())
            .assertLoginVisible()
            .assertLogoutAbsent()
    }

    @Test
    fun givenLoggedIn_whenShown_thenGreetingAndLogoutAreShownAndLoginIsNot() {
        robot.show(SettingsState(user = tim))
            .assertTextVisible("Hello tim")
            .assertLogoutVisible()
            .assertLoginAbsent()
    }

    @Test
    fun givenCurrentSettings_whenShown_thenTheirValuesAreListed() {
        robot.show(SettingsState(darkTheme = DarkTheme.No, dynamicColor = true))
            .assertTextVisible("Light")
            .assertTextVisible("On")
    }

    @Test
    fun givenLoggedOut_whenTappingLoginAndBack_thenCallbacksRunAndNoActionIsSent() {
        robot.show(SettingsState())
            .clickLogin()
            .clickBack()

        assertThat(robot.logins).isEqualTo(1)
        assertThat(robot.backs).isEqualTo(1)
        assertThat(robot.actions).isEmpty()
    }

    @Test
    fun givenLoggedIn_whenTappingLogout_thenLogoutActionIsSent() {
        robot.show(SettingsState(user = tim)).clickLogout()

        assertThat(robot.actions).containsExactly(SettingsAction.OnLogoutClick)
    }

    @Test
    fun givenThemePicker_whenChoosingDark_thenThemeActionIsSentAndTheSheetClosesItself() {
        robot.show(SettingsState(darkTheme = DarkTheme.No))
            .openDarkThemeMenu()
            .assertTextVisible("Dark")
            .choose("Dark")

        assertThat(robot.actions).containsExactly(SettingsAction.OnDarkThemeChange(DarkTheme.Yes))
    }

    @Test
    fun givenDynamicColorPicker_whenChoosingOn_thenDynamicColorActionIsSent() {
        robot.show(SettingsState(dynamicColor = false))
            .openDynamicColorMenu()
            .choose("On")

        assertThat(robot.actions).containsExactly(SettingsAction.OnDynamicColorChange(true))
    }

    @Test
    fun givenAPicker_whenOpenedWithoutChoosing_thenNothingIsSent() {
        robot.show(SettingsState()).openDarkThemeMenu().openDarkThemeMenu()

        assertThat(robot.actions).isEmpty()
    }
}
