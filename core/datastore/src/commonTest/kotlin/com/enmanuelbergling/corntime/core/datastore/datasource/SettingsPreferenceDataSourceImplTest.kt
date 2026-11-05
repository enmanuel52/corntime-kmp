package com.enmanuelbergling.corntime.core.datastore.datasource

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.datastore.testPreferencesDataStore
import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SettingsPreferenceDataSourceImplTest {

    private fun TestScope.dataSource() = testPreferencesDataStore(backgroundScope).let {
        it to SettingsPreferenceDataSourceImpl(it)
    }

    @Test
    fun givenNothingStored_whenReadingTheme_thenDefaultsToSystem() = runTest {
        val (_, dataSource) = dataSource()

        assertThat(dataSource.getDarkTheme().first()).isEqualTo(DarkTheme.System)
    }

    @Test
    fun givenThemeSet_whenReading_thenItIsPersisted() = runTest {
        val (_, dataSource) = dataSource()

        dataSource.setDarkTheme(DarkTheme.Yes)

        assertThat(dataSource.getDarkTheme().first()).isEqualTo(DarkTheme.Yes)
    }

    @Test
    fun givenUnknownStoredTheme_whenReading_thenFallsBackToSystem() = runTest {
        val (store, dataSource) = dataSource()
        store.edit { it[stringPreferencesKey("dark_theme")] = "Sepia" }

        assertThat(dataSource.getDarkTheme().first()).isEqualTo(DarkTheme.System)
    }

    @Test
    fun givenDynamicColorToggled_whenReading_thenReflectsLatestValue() = runTest {
        val (_, dataSource) = dataSource()
        assertThat(dataSource.getDynamicColor().first()).isFalse()

        dataSource.setDynamicColor(true)
        assertThat(dataSource.getDynamicColor().first()).isTrue()

        dataSource.setDynamicColor(false)
        assertThat(dataSource.getDynamicColor().first()).isFalse()
    }
}
