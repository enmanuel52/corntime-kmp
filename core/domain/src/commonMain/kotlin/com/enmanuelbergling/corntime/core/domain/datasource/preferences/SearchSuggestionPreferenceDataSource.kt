package com.enmanuelbergling.corntime.core.domain.datasource.preferences

import kotlinx.coroutines.flow.Flow

/** Recent searches the user can pick again, most recent first. */
interface SearchSuggestionPreferenceDataSource {
    /** Remembers [query]; an existing equal one moves to the front, and only the newest [MAX_SUGGESTIONS] are kept. */
    suspend fun addSuggestion(query: String)
    suspend fun deleteSuggestion(query: String)
    suspend fun clearSuggestions()

    fun getSuggestions(): Flow<List<String>>

    companion object {
        const val MAX_SUGGESTIONS = 10
    }
}
