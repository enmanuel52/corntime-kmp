package com.enmanuelbergling.corntime.shared

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isNotNull
import com.enmanuelbergling.corntime.core.datastore.DataStorePathProvider
import com.enmanuelbergling.corntime.core.datastore.secure.SecureStorage
import com.enmanuelbergling.corntime.core.domain.sync.MovieSyncScheduler
import com.enmanuelbergling.corntime.core.domain.sync.TvShowSyncScheduler
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.Test

/** The platform bindings that the common graph test replaces: Keychain storage and the Documents path. */
class IosKoinBindingsTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun givenIosApp_whenStarted_thenRealStorageBindingsResolve() {
        initKoin()
        val koin = KoinPlatform.getKoin()

        assertThat(koin.get<SecureStorage>()).isNotNull()
        assertThat(koin.get<MovieSyncScheduler>()).isNotNull()
        assertThat(koin.get<TvShowSyncScheduler>()).isNotNull()
        assertThat(koin.get<DataStorePathProvider>().absolutePath("x.preferences_pb")).endsWith("/x.preferences_pb")
    }
}
