package com.enmanuelbergling.corntime.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.OnboardingPreferenceDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

private val ONBOARDING_KEY = booleanPreferencesKey("onboarding")

@Single
internal class OnboardingPreferenceDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : OnboardingPreferenceDataSource {

    override fun isOnboarding(): Flow<Boolean> = dataStore.data
        .map { it[ONBOARDING_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun finishOnboarding() {
        dataStore.edit { it[ONBOARDING_KEY] = false }
    }
}
