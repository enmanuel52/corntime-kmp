package com.enmanuelbergling.corntime.feature.movies.presentation.filter

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.movie.MovieFilter
import com.enmanuelbergling.corntime.core.domain.model.movie.SortCriteria
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeMovieData
import com.enmanuelbergling.corntime.feature.movies.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.movies.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class MoviesFilterScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { MoviesFilterRobot(rule) }
    private val genres = FakeMovieData.GENRES
    private val movies = FakeMovieData.MOVIES

    private fun state(
        filter: MovieFilter = MovieFilter(),
        genres: List<com.enmanuelbergling.corntime.core.domain.model.movie.Genre> = this.genres,
        paging: PagingState<com.enmanuelbergling.corntime.core.domain.model.movie.Movie> =
            PagingState(items = movies, status = PagingStatus.EndReached),
        isLoadingGenres: Boolean = false,
        genresError: UiError? = null,
    ) = MoviesFilterState(
        filter = filter,
        genres = genres,
        isLoadingGenres = isLoadingGenres,
        genresError = genresError,
        paging = paging,
    )

    @Test
    fun givenGenresAndMovies_whenShown_thenSortChipsGenresAndMoviesAreVisible() {
        robot.show(state())
            .assertTextVisible("Order By")
            .assertTextVisible("Popularity")
            .assertTextVisible("Genres")
            .assertTagVisible("${MoviesFilterTags.GENRE}${genres[0].id}")
            .assertTextVisible(movies[0].title)
    }

    @Test
    fun givenGenreTapped_whenClicked_thenDispatchesToggle() {
        robot.show(state()).clickGenre(genres[1].id)

        assertThat(robot.actions).containsExactly(MoviesFilterAction.OnToggleGenre(genres[1]))
    }

    @Test
    fun givenSortChipTapped_whenClicked_thenDispatchesThatCriteria() {
        robot.show(state()).clickSort(SortCriteria.Revenue)

        assertThat(robot.actions).containsExactly(MoviesFilterAction.OnPickSortCriteria(SortCriteria.Revenue))
    }

    @Test
    fun givenDefaultFilter_whenShown_thenThereIsNothingToClear() {
        robot.show(state()).assertTagAbsent(MoviesFilterTags.CLEAR)
    }

    @Test
    fun givenActiveFilter_whenClearTapped_thenDispatchesClear() {
        robot.show(state(filter = MovieFilter(genres = listOf(genres[0]))))
            .assertTagVisible(MoviesFilterTags.CLEAR)
            .clickClear()

        assertThat(robot.actions).containsExactly(MoviesFilterAction.OnClearFilter)
    }

    @Test
    fun givenNoMatches_whenFinished_thenEmptyMessage() {
        robot.show(state(paging = PagingState(status = PagingStatus.EndReached)))
            .assertTagVisible(MoviesFilterTags.NO_RESULTS)
    }

    @Test
    fun givenMovieTapped_whenClicked_thenDispatchesItsId() {
        robot.show(state()).clickMovieAt(0)

        assertThat(robot.actions).contains(MoviesFilterAction.OnMovieClick(movies[0].id))
    }

    @Test
    fun givenNextPageFailed_whenShown_thenMoviesStayAndRetryDispatchesLoad() {
        robot.show(state(paging = PagingState(items = movies, status = PagingStatus.Failed(DataError.Network.ServerError))))
            .assertTextVisible(movies[0].title)
            .clickRetry()

        assertThat(robot.actions).contains(MoviesFilterAction.OnLoadNextPage)
    }

    @Test
    fun givenGenresLoading_whenShown_thenNoFilterControlsYet() {
        robot.show(state(genres = emptyList(), isLoadingGenres = true, paging = PagingState()))
            .assertTagAbsent(MoviesFilterTags.CONTENT)
    }

    @Test
    fun givenGenresFailed_whenShown_thenErrorScreenRetriesGenres() {
        robot.show(state(genres = emptyList(), genresError = UiError.NoInternet, paging = PagingState()))
            .assertTextVisible("No internet connection")
            .clickText("Retry")

        assertThat(robot.actions).containsExactly(MoviesFilterAction.OnRetryGenres)
    }

    @Test
    fun givenBackPressed_whenClicked_thenDispatchesBack() {
        robot.show(state()).clickBack()

        assertThat(robot.actions).containsExactly(MoviesFilterAction.OnBack)
    }
}
