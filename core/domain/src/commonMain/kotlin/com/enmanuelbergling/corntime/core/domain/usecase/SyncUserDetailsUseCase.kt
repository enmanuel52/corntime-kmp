package com.enmanuelbergling.corntime.core.domain.usecase

import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.domain.util.onSuccess
import org.koin.core.annotation.Factory

/** Fetches the TMDB account (id, username, avatar) and caches it locally on success. */
@Factory
class SyncUserDetailsUseCase(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val userPreferenceDataSource: UserPreferenceDataSource,
) {
    suspend operator fun invoke(): NetworkResult<UserDetails> =
        userRemoteDataSource.getAccount().onSuccess { userPreferenceDataSource.updateUser(it) }
}
