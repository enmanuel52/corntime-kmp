package com.enmanuelbergling.corntime.core.datastore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SearchSuggestionPreferenceDataSource
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SearchSuggestionPreferenceDataSource.Companion.MAX_SUGGESTIONS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

private val SUGGESTIONS_KEY = stringPreferencesKey("search_suggestions")
private val suggestionsSerializer = ListSerializer(String.serializer())

/** Pop-Corns kept an unordered string set; a JSON list keeps the recency order. */
@Single
internal class SearchSuggestionPreferenceDataSourceImpl(
    private val dataStore: DataStore<Preferences>,
) : SearchSuggestionPreferenceDataSource {

    override suspend fun addSuggestion(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        dataStore.edit { preferences ->
            val updated = (listOf(clean) + preferences.suggestions().filterNot { it.equals(clean, ignoreCase = true) })
                .take(MAX_SUGGESTIONS)
            preferences[SUGGESTIONS_KEY] = Json.encodeToString(suggestionsSerializer, updated)
        }
    }

    override suspend fun deleteSuggestion(query: String) {
        dataStore.edit { preferences ->
            val updated = preferences.suggestions() - query
            preferences[SUGGESTIONS_KEY] = Json.encodeToString(suggestionsSerializer, updated)
        }
    }

    override suspend fun clearSuggestions() {
        dataStore.edit { it.remove(SUGGESTIONS_KEY) }
    }

    override fun getSuggestions(): Flow<List<String>> = dataStore.data
        .map { it.suggestions() }
        .distinctUntilChanged()

    private fun Preferences.suggestions(): List<String> = this[SUGGESTIONS_KEY]
        ?.let { runCatching { Json.decodeFromString(suggestionsSerializer, it) }.getOrNull() }
        .orEmpty()
}
