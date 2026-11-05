package com.enmanuelbergling.corntime.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

private val DARK_THEME_KEY = stringPreferencesKey("dark_theme")
private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")

@Single
internal class SettingsPreferenceDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsPreferenceDataSource {

    override fun getDarkTheme(): Flow<DarkTheme> = dataStore.data
        .map { preferences ->
            preferences[DARK_THEME_KEY]
                ?.let { stored -> DarkTheme.entries.firstOrNull { it.name == stored } }
                ?: DarkTheme.System
        }
        .distinctUntilChanged()

    override suspend fun setDarkTheme(theme: DarkTheme) {
        dataStore.edit { it[DARK_THEME_KEY] = theme.name }
    }

    override fun getDynamicColor(): Flow<Boolean> = dataStore.data
        .map { it[DYNAMIC_COLOR_KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun setDynamicColor(active: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = active }
    }
}
