package com.enmanuelbergling.corntime.core.domain.datasource.preferences

import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme
import kotlinx.coroutines.flow.Flow

interface SettingsPreferenceDataSource {
    fun getDarkTheme(): Flow<DarkTheme>
    suspend fun setDarkTheme(theme: DarkTheme)

    /** Material You dynamic color; only meaningful on Android 12+. */
    fun getDynamicColor(): Flow<Boolean>
    suspend fun setDynamicColor(active: Boolean)
}
