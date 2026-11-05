package com.enmanuelbergling.corntime.feature.settings.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.domain.usecase.LogoutUseCase
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeSettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SettingsViewModelTest : MainDispatcherTest() {
    private val settings = FakeSettingsPreferenceDataSource()
    private val user = FakeUserPreferenceDataSource()
    private val auth = FakeAuthPreferenceDataSource("session")
    private val account = FakeUserRemoteDataSource()

    private fun viewModel() = SettingsViewModel(
        settings,
        user,
        LogoutUseCase(user, auth),
        SyncUserDetailsUseCase(account, user),
    )

    @Test
    fun givenNothingSaved_whenCreated_thenLoggedOutWithSystemThemeAndNoDynamicColor() {
        val state = viewModel().state.value

        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.darkTheme).isEqualTo(DarkTheme.System)
        assertThat(state.dynamicColor).isFalse()
    }

    @Test
    fun givenSavedPreferences_whenCreated_thenStateStartsFromThem() {
        settings.darkTheme.value = DarkTheme.Yes
        settings.dynamicColor.value = true
        user.user.value = UserDetails(id = 1, username = "tim", avatarPath = "/a.jpg")

        val state = viewModel().state.value

        assertThat(state.darkTheme).isEqualTo(DarkTheme.Yes)
        assertThat(state.dynamicColor).isTrue()
        assertThat(state.user).isEqualTo(UserUi("tim", "/a.jpg"))
    }

    @Test
    fun givenEachTheme_whenChosen_thenItIsPersistedAndReflected() = runTest {
        val viewModel = viewModel()

        DarkTheme.entries.forEach { theme ->
            viewModel.onAction(SettingsAction.OnDarkThemeChange(theme))

            assertThat(settings.darkTheme.value).isEqualTo(theme)
            assertThat(viewModel.state.value.darkTheme).isEqualTo(theme)
        }
    }

    @Test
    fun givenDynamicColor_whenToggled_thenPersistedBothWays() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(SettingsAction.OnDynamicColorChange(true))
        assertThat(viewModel.state.value.dynamicColor).isTrue()

        viewModel.onAction(SettingsAction.OnDynamicColorChange(false))
        assertThat(settings.dynamicColor.value).isFalse()
    }

    @Test
    fun givenLoggedIn_whenTheUserChangesElsewhere_thenStateFollows() = runTest {
        val viewModel = viewModel()
        user.updateUser(UserDetails(id = 2, username = "ann"))
        assertThat(viewModel.state.value.user?.username).isEqualTo("ann")

        user.updateUser(UserDetails(id = 2, username = "ann2"))
        assertThat(viewModel.state.value.user?.username).isEqualTo("ann2")
    }

    @Test
    fun givenLoggedIn_whenLoggingOut_thenSessionAndUserAreClearedButThemeKept() = runTest {
        user.user.value = UserDetails(id = 1, username = "tim")
        settings.darkTheme.value = DarkTheme.Yes
        val viewModel = viewModel()

        viewModel.onAction(SettingsAction.OnLogoutClick)

        assertThat(viewModel.state.value.user).isNull()
        assertThat(auth.sessionId.value).isEqualTo("")
        assertThat(viewModel.state.value.darkTheme).isEqualTo(DarkTheme.Yes)
    }

    @Test
    fun givenLoggedOut_whenRefreshingAccount_thenNoRequestIsMade() = runTest {
        viewModel().onAction(SettingsAction.OnRefreshAccount)

        assertThat(account.callCount(UserRemoteFunction.GetAccount)).isEqualTo(0)
    }

    @Test
    fun givenLoggedIn_whenRefreshingAccount_thenCachedUserIsUpdated() = runTest {
        user.user.value = UserDetails(id = 1, username = "old")
        account.account = UserDetails(id = 1, username = "renamed")
        val viewModel = viewModel()

        viewModel.onAction(SettingsAction.OnRefreshAccount)

        assertThat(viewModel.state.value.user?.username).isEqualTo("renamed")
    }

    @Test
    fun givenLoggedIn_whenRefreshFails_thenTheCachedUserIsKept() = runTest {
        user.user.value = UserDetails(id = 1, username = "tim")
        account.fail(UserRemoteFunction.GetAccount, DataError.Network.NoInternet)
        val viewModel = viewModel()

        viewModel.onAction(SettingsAction.OnRefreshAccount)

        assertThat(viewModel.state.value.user?.username).isEqualTo("tim")
    }
}
