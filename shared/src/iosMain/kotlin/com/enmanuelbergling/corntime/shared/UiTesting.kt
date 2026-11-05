package com.enmanuelbergling.corntime.shared

import com.enmanuelbergling.corntime.core.domain.datasource.preferences.AuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeSettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Launch mode for UI tests: every network and storage dependency is an in-memory fake, so flows are
 * deterministic and need no network. Started with [start], which the iOS app calls for the
 * `-uiTesting` launch argument. The user starts logged out.
 */
object UiTesting {
    private val movies = FakeMovieRemoteDataSource().apply {
        totalPages = 3
        fail(MovieRemoteFunction.GetMovieAccountStates, DataError.Network.Unauthorized)
    }
    private val user = FakeUserRemoteDataSource().apply {
        fail(UserRemoteFunction.GetAccount, DataError.Network.Unauthorized)
    }

    /** How many times [function] (a [MovieRemoteFunction] name, e.g. "GetUpcomingMovies") was requested. */
    fun movieCalls(function: String): Int = movies.callCount(MovieRemoteFunction.valueOf(function))

    /** Starts the app's Koin graph with the fakes replacing the network and storage. */
    fun start() = initKoin {
        allowOverride(true)
        modules(module())
    }

    private fun module(): Module = module {
        single<MovieRemoteDataSource> { movies }
        single<UserRemoteDataSource> { user }
        single<TvRemoteDataSource> { FakeTvRemoteDataSource() }
        single<UserPreferenceDataSource> { FakeUserPreferenceDataSource() }
        single<AuthPreferenceDataSource> { FakeAuthPreferenceDataSource() }
        single<SettingsPreferenceDataSource> { FakeSettingsPreferenceDataSource() }
    }
}
