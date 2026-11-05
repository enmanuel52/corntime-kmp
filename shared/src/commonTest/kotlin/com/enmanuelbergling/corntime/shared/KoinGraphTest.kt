package com.enmanuelbergling.corntime.shared

import assertk.assertThat
import assertk.assertions.isNotNull
import com.enmanuelbergling.corntime.core.data.network.createTmdbHttpClient
import com.enmanuelbergling.corntime.core.datastore.DataStorePathProvider
import com.enmanuelbergling.corntime.core.datastore.secure.SecureStorage
import com.enmanuelbergling.corntime.core.domain.model.MovieSection
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.actor.presentation.details.ActorDetailsViewModel
import com.enmanuelbergling.corntime.feature.actor.presentation.home.ActorsViewModel
import com.enmanuelbergling.corntime.feature.auth.presentation.login.LoginViewModel
import com.enmanuelbergling.corntime.feature.settings.presentation.SettingsViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.details.MovieDetailsViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.home.MoviesViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.search.MovieSearchViewModel
import com.enmanuelbergling.corntime.feature.movies.presentation.section.MovieSectionViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import okio.FileSystem
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import org.koin.core.context.stopKoin

/**
 * Boots the real [initKoin] graph and resolves every ViewModel, so a missing or mis-annotated binding
 * fails here instead of at runtime. Only the network and on-device storage are replaced.
 */
class KoinGraphTest : MainDispatcherTest() {

    @AfterTest
    fun tearDown() = stopKoin()

    private fun startWithTestInfrastructure() = initKoin {
        allowOverride(true)
        modules(
            module {
                single { createTmdbHttpClient(MockEngine { respond("{}", HttpStatusCode.Unauthorized) }, "key", "en-US") }
                single<DataStorePathProvider> {
                    object : DataStorePathProvider {
                        override fun absolutePath(fileName: String) =
                            "${FileSystem.SYSTEM_TEMPORARY_DIRECTORY}/graph-${Random.nextLong()}-$fileName"
                    }
                }
                single<SecureStorage> {
                    object : SecureStorage {
                        override suspend fun read(key: String): String? = null
                        override suspend fun write(key: String, value: String) = Unit
                        override suspend fun delete(key: String) = Unit
                    }
                }
            },
        )
    }

    @Test
    fun givenAppGraph_whenResolvingEveryMoviesViewModel_thenAllDependenciesAreSatisfied() {
        startWithTestInfrastructure()
        val koin = KoinPlatform.getKoin()

        assertThat(koin.get<MoviesViewModel>()).isNotNull()
        assertThat(koin.get<MovieDetailsViewModel> { parametersOf(1) }).isNotNull()
        assertThat(koin.get<MovieSearchViewModel>()).isNotNull()
        MovieSection.entries.forEach { section ->
            assertThat(koin.get<MovieSectionViewModel> { parametersOf(section) }).isNotNull()
        }
    }

    @Test
    fun givenAppGraph_whenResolvingAppLoginAndSettingsViewModels_thenAllDependenciesAreSatisfied() {
        startWithTestInfrastructure()
        val koin = KoinPlatform.getKoin()

        assertThat(koin.get<AppViewModel>()).isNotNull()
        assertThat(koin.get<LoginViewModel>()).isNotNull()
        assertThat(koin.get<SettingsViewModel>()).isNotNull()
        assertThat(koin.get<ActorsViewModel>()).isNotNull()
        assertThat(koin.get<ActorDetailsViewModel> { parametersOf(1) }).isNotNull()
    }
}
