package com.enmanuelbergling.corntime.core.datastore.datasource

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.datastore.testPreferencesDataStore
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UserPreferenceDataSourceImplTest {

    @Test
    fun givenNoUser_whenReading_thenEmitsNull() = runTest {
        val dataSource = UserPreferenceDataSourceImpl(testPreferencesDataStore(backgroundScope))

        assertThat(dataSource.getCurrentUser().first()).isNull()
    }

    @Test
    fun givenUserUpdated_whenReading_thenAllFieldsRoundTrip() = runTest {
        val dataSource = UserPreferenceDataSourceImpl(testPreferencesDataStore(backgroundScope))

        dataSource.updateUser(FakeUserData.FAKE_USER)

        assertThat(dataSource.getCurrentUser().first()).isEqualTo(FakeUserData.FAKE_USER)
    }

    @Test
    fun givenStoredUser_whenCleared_thenEmitsNull() = runTest {
        val dataSource = UserPreferenceDataSourceImpl(testPreferencesDataStore(backgroundScope))
        dataSource.updateUser(FakeUserData.FAKE_USER)

        dataSource.clear()

        assertThat(dataSource.getCurrentUser().first()).isNull()
    }

    @Test
    fun givenCorruptedStoredJson_whenReading_thenEmitsNullInsteadOfCrashing() = runTest {
        val store = testPreferencesDataStore(backgroundScope)
        store.edit { it[stringPreferencesKey("user")] = "{not json" }

        assertThat(UserPreferenceDataSourceImpl(store).getCurrentUser().first()).isNull()
    }
}
