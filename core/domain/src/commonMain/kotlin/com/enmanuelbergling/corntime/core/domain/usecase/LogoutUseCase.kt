package com.enmanuelbergling.corntime.core.domain.usecase

import com.enmanuelbergling.corntime.core.domain.datasource.preferences.AuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import org.koin.core.annotation.Factory

/** Clears every trace of the logged-in account: the cached user and the session id. */
@Factory
class LogoutUseCase(
    private val userPreferenceDataSource: UserPreferenceDataSource,
    private val authPreferenceDataSource: AuthPreferenceDataSource,
) {
    suspend operator fun invoke() {
        userPreferenceDataSource.clear()
        authPreferenceDataSource.clear()
    }
}
