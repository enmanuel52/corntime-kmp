package com.enmanuelbergling.corntime.shared

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.testing.datasource.FakeOnboardingPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeSettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AppViewModelTest : MainDispatcherTest() {
    private val settings = FakeSettingsPreferenceDataSource()
    private val user = FakeUserPreferenceDataSource()
    private val onboarding = FakeOnboardingPreferenceDataSource()

    private fun viewModel() = AppViewModel(settings, user, onboarding)

    @Test
    fun givenFreshInstall_whenCreated_thenLoadedWithDefaultsLoggedOutAndOnboardingPending() {
        val state = viewModel().state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.darkTheme).isEqualTo(DarkTheme.System)
        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.isOnboarding).isTrue()
    }

    @Test
    fun givenSavedPreferences_whenCreated_thenStateReflectsAllOfThem() {
        settings.darkTheme.value = DarkTheme.Yes
        settings.dynamicColor.value = true
        user.user.value = UserDetails(id = 3, username = "tim")
        onboarding.onboarding.value = false

        val state = viewModel().state.value

        assertThat(state.darkTheme).isEqualTo(DarkTheme.Yes)
        assertThat(state.dynamicColor).isTrue()
        assertThat(state.user?.username).isEqualTo("tim")
        assertThat(state.isOnboarding).isFalse()
    }

    @Test
    fun givenLogin_whenUserIsSaved_thenIsLoggedInFlipsAndLogoutFlipsItBack() = runTest {
        val viewModel = viewModel()

        user.updateUser(UserDetails(id = 1, username = "tim"))
        assertThat(viewModel.state.value.isLoggedIn).isTrue()

        user.clear()
        assertThat(viewModel.state.value.user).isNull()
    }

    @Test
    fun givenThemeChangedInSettings_whenObserving_thenAppStateFollows() = runTest {
        val viewModel = viewModel()

        settings.setDarkTheme(DarkTheme.No)

        assertThat(viewModel.state.value.darkTheme).isEqualTo(DarkTheme.No)
    }

    @Test
    fun givenOnboarding_whenFinished_thenItIsNoLongerPendingEvenAfterRestart() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(AppAction.OnFinishOnboarding)

        assertThat(viewModel.state.value.isOnboarding).isFalse()
        // a new ViewModel (app restart) reads the persisted value
        assertThat(AppViewModel(settings, user, onboarding).state.value.isOnboarding).isFalse()
    }
}
