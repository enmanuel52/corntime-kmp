package com.enmanuelbergling.corntime.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.UserPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Serializable
internal data class StoredUser(
    val id: Int,
    val username: String,
    val avatarPath: String,
    val name: String,
)

private val USER_KEY = stringPreferencesKey("user")

/** Replaces Pop-Corns' proto user with JSON in the shared preferences file. */
@Single
internal class UserPreferenceDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : UserPreferenceDataSource {

    override fun getCurrentUser(): Flow<UserDetails?> = dataStore.data
        .map { preferences ->
            preferences[USER_KEY]
                ?.let { runCatching { Json.decodeFromString<StoredUser>(it) }.getOrNull() }
                ?.let { UserDetails(id = it.id, username = it.username, avatarPath = it.avatarPath, name = it.name) }
        }
        .distinctUntilChanged()

    override suspend fun updateUser(userDetails: UserDetails) {
        val stored = StoredUser(userDetails.id, userDetails.username, userDetails.avatarPath, userDetails.name)
        dataStore.edit { it[USER_KEY] = Json.encodeToString(stored) }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(USER_KEY) }
    }
}
