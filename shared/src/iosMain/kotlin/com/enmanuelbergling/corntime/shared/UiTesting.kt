package com.enmanuelbergling.corntime.shared

import com.enmanuelbergling.corntime.core.domain.datasource.preferences.AuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.OnboardingPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.AuthRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.MovieRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.remote.UserRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.auth.CreateSessionPost
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeOnboardingPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeMovieRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeSettingsPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeTvRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.MovieRemoteFunction
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Launch mode for UI tests: every network and storage dependency is an in-memory fake, so flows are
 * deterministic and need no network. Started with [start], which the iOS app calls for the
 * `-uiTesting` launch argument. The user starts logged out and past the walkthrough unless asked
 * otherwise.
 */
object UiTesting {
    /** The one username the fake login service rejects, to test the wrong-credentials path. */
    const val REJECTED_USERNAME = "intruder"

    private val movies = FakeMovieRemoteDataSource().apply {
        totalPages = 3
        fail(MovieRemoteFunction.GetMovieAccountStates, DataError.Network.Unauthorized)
    }

    private val session = FakeAuthPreferenceDataSource()
    private val account = FakeUserRemoteDataSource()
    private val login = FakeAuthRemoteDataSource()

    /** Like the real API: the account is only available once a session exists. */
    private val user = object : UserRemoteDataSource by account {
        override suspend fun getAccount() =
            if (session.sessionId.value.isBlank()) Result.Failure(DataError.Network.Unauthorized)
            else account.getAccount()
    }

    private val auth = object : AuthRemoteDataSource by login {
        override suspend fun validateWithLogin(sessionPost: CreateSessionPost) =
            if (sessionPost.username == REJECTED_USERNAME) Result.Failure(DataError.Network.Unauthorized)
            else login.validateWithLogin(sessionPost)
    }

    /** How many times [function] (a [MovieRemoteFunction] name, e.g. "GetUpcomingMovies") was requested. */
    fun movieCalls(function: String): Int = movies.callCount(MovieRemoteFunction.valueOf(function))

    /** Starts the app's Koin graph with the fakes replacing the network and storage. */
    fun start() = start(showOnboarding = false)

    /** [showOnboarding] launches with the first-run walkthrough pending. */
    fun start(showOnboarding: Boolean) = initKoin {
        allowOverride(true)
        modules(module(showOnboarding))
    }

    private fun module(showOnboarding: Boolean): Module = module {
        single<MovieRemoteDataSource> { movies }
        single<UserRemoteDataSource> { user }
        single<AuthRemoteDataSource> { auth }
        single<OnboardingPreferenceDataSource> { FakeOnboardingPreferenceDataSource(initiallyOnboarding = showOnboarding) }
        single<TvRemoteDataSource> { FakeTvRemoteDataSource() }
        single<UserPreferenceDataSource> { FakeUserPreferenceDataSource() }
        single<AuthPreferenceDataSource> { session }
        single<SettingsPreferenceDataSource> { FakeSettingsPreferenceDataSource() }
    }
}
