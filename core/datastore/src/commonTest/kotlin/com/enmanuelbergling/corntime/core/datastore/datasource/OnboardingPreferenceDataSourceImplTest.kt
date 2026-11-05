package com.enmanuelbergling.corntime.core.datastore.datasource

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.datastore.testPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class OnboardingPreferenceDataSourceImplTest {
    private fun TestScope.dataSource() = OnboardingPreferenceDataSourceImpl(testPreferencesDataStore(backgroundScope))

    @Test
    fun givenFreshInstall_whenReading_thenOnboardingIsPending() = runTest {
        assertThat(dataSource().isOnboarding().first()).isTrue()
    }

    @Test
    fun givenOnboardingFinished_whenReading_thenItStaysFinished() = runTest {
        val dataSource = dataSource()

        dataSource.finishOnboarding()
        dataSource.finishOnboarding()

        assertThat(dataSource.isOnboarding().first()).isFalse()
    }
}
