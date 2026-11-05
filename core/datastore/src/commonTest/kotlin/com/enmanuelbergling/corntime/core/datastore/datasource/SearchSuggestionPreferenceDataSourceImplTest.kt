package com.enmanuelbergling.corntime.core.datastore.datasource

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.datastore.testPreferencesDataStore
import com.enmanuelbergling.corntime.core.domain.datasource.preferences.SearchSuggestionPreferenceDataSource.Companion.MAX_SUGGESTIONS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SearchSuggestionPreferenceDataSourceImplTest {
    private fun TestScope.dataSource() =
        SearchSuggestionPreferenceDataSourceImpl(testPreferencesDataStore(backgroundScope))

    @Test
    fun givenFreshInstall_whenReading_thenThereAreNoSuggestions() = runTest {
        assertThat(dataSource().getSuggestions().first()).isEmpty()
    }

    @Test
    fun givenSeveralQueries_whenAdded_thenMostRecentIsFirst() = runTest {
        val dataSource = dataSource()

        dataSource.addSuggestion("alien")
        dataSource.addSuggestion("dune")

        assertThat(dataSource.getSuggestions().first()).containsExactly("dune", "alien")
    }

    @Test
    fun givenExistingQuery_whenAddedAgainWithOtherCase_thenItMovesToTheFrontWithoutDuplicating() = runTest {
        val dataSource = dataSource()
        dataSource.addSuggestion("alien")
        dataSource.addSuggestion("dune")

        dataSource.addSuggestion("  ALIEN ")

        assertThat(dataSource.getSuggestions().first()).containsExactly("ALIEN", "dune")
    }

    @Test
    fun givenBlankQuery_whenAdded_thenIgnored() = runTest {
        val dataSource = dataSource()

        dataSource.addSuggestion("   ")

        assertThat(dataSource.getSuggestions().first()).isEmpty()
    }

    @Test
    fun givenMoreThanTheLimit_whenAdded_thenOnlyTheNewestAreKept() = runTest {
        val dataSource = dataSource()

        repeat(MAX_SUGGESTIONS + 3) { dataSource.addSuggestion("query $it") }

        val saved = dataSource.getSuggestions().first()
        assertThat(saved.size).isEqualTo(MAX_SUGGESTIONS)
        assertThat(saved.first()).isEqualTo("query ${MAX_SUGGESTIONS + 2}")
    }

    @Test
    fun givenSuggestions_whenOneIsDeleted_thenOthersRemainInOrder() = runTest {
        val dataSource = dataSource()
        dataSource.addSuggestion("a")
        dataSource.addSuggestion("b")
        dataSource.addSuggestion("c")

        dataSource.deleteSuggestion("b")
        dataSource.deleteSuggestion("missing")

        assertThat(dataSource.getSuggestions().first()).containsExactly("c", "a")
    }

    @Test
    fun givenSuggestions_whenCleared_thenNoneRemain() = runTest {
        val dataSource = dataSource()
        dataSource.addSuggestion("a")

        dataSource.clearSuggestions()

        assertThat(dataSource.getSuggestions().first()).isEmpty()
    }
}
