package com.enmanuelbergling.corntime.core.datastore.datasource

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.datastore.secure.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private class InMemorySecureStorage(initial: Map<String, String> = emptyMap()) : SecureStorage {
    val values = initial.toMutableMap()
    var reads = 0
    override suspend fun read(key: String): String? = values[key].also { reads++ }
    override suspend fun write(key: String, value: String) {
        values[key] = value
    }
    override suspend fun delete(key: String) {
        values.remove(key)
    }
}

class AuthPreferenceDataSourceImplTest {

    @Test
    fun givenEmptyStorage_whenReadingSession_thenEmitsEmptyString() = runTest {
        val dataSource = AuthPreferenceDataSourceImpl(InMemorySecureStorage())

        assertThat(dataSource.getSessionId().first()).isEqualTo("")
    }

    @Test
    fun givenSessionPersistedBefore_whenReading_thenLoadsItFromSecureStorage() = runTest {
        val storage = InMemorySecureStorage(mapOf(SESSION_ID_KEY to "persisted"))

        assertThat(AuthPreferenceDataSourceImpl(storage).getSessionId().first()).isEqualTo("persisted")
    }

    @Test
    fun givenCollector_whenSavingAndClearing_thenEmitsEachChange() = runTest {
        val storage = InMemorySecureStorage()
        val dataSource = AuthPreferenceDataSourceImpl(storage)

        dataSource.getSessionId().test {
            assertThat(awaitItem()).isEqualTo("")

            dataSource.saveSessionId("abc")
            assertThat(awaitItem()).isEqualTo("abc")
            assertThat(storage.values[SESSION_ID_KEY]).isEqualTo("abc")

            dataSource.clear()
            assertThat(awaitItem()).isEqualTo("")
            assertThat(storage.values[SESSION_ID_KEY]).isNull()
        }
    }

    @Test
    fun givenSeveralCollectors_whenReading_thenSecureStorageIsReadOnce() = runTest {
        val storage = InMemorySecureStorage(mapOf(SESSION_ID_KEY to "persisted"))
        val dataSource = AuthPreferenceDataSourceImpl(storage)

        repeat(3) { dataSource.getSessionId().first() }

        assertThat(storage.reads).isEqualTo(1)
    }
}
