package com.enmanuelbergling.corntime.feature.favorites.presentation.tvshows

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.presentation.removal.FailedRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.PendingRemoval
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalKind
import com.enmanuelbergling.corntime.core.presentation.removal.RemovalState
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.feature.favorites.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.favorites.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class FavoriteTvShowsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { FavoriteTvShowsRobot(rule) }
    private val tvShows: List<TvShow> = FakeTvData.TV_SHOWS.mapIndexed { index, tvShow -> tvShow.copy(id = index + 1, name = "Show ${'A' + index}") }

    private fun state(
        items: List<TvShow> = tvShows,
        status: PagingStatus = PagingStatus.Idle,
        removals: RemovalState = RemovalState(),
    ) = FavoriteTvShowsState(PagingState(items = items, status = status), removals)

    @Test
    fun givenLoadedFavorites_whenShown_thenTheirTitlesAreVisible() {
        robot.show(state()).assertTextVisible(tvShows.first().name).assertTextAbsent("No favorite TV shows yet")
    }

    @Test
    fun givenNoFavorites_whenLoaded_thenTheEmptyMessageIsShown() {
        robot.show(state(items = emptyList(), status = PagingStatus.EndReached)).assertTextVisible("No favorite TV shows yet")
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenAnErrorScreenWithRetry() {
        robot.show(state(items = emptyList(), status = PagingStatus.Failed(DataError.Network.NoInternet)))
            .assertTextVisible("Retry")
            .clickRetry()

        assertThat(robot.pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenALaterPageFailed_whenShown_thenTvShowsStayWithAFooterRetry() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.RequestTimeout)))
            .assertTextVisible(tvShows.first().name)
            .assertRetryFooterVisible()
            .clickRetryFooter()

        assertThat(robot.pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenAShortList_whenShown_thenTheNextPageIsRequestedAutomatically() {
        robot.show(state())

        assertThat(robot.pagingRequests).isGreaterThan(0)
    }

    @Test
    fun givenAFailedPage_whenShown_thenNoAutomaticRequestUntilTheUserRetries() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.NoInternet)))

        assertThat(robot.pagingRequests).isEqualTo(0)
    }

    @Test
    fun givenATvShow_whenTappingItsPoster_thenItIsClicked() {
        robot.show(state()).clickTvShowAt(1)

        assertThat(robot.actions).containsExactly(FavoriteTvShowsAction.OnTvShowClick(tvShows[1].id))
    }

    @Test
    fun givenAPendingRemoval_whenShown_thenTheUndoSnackbarOffersToTakeItBack() {
        val removed = PendingRemoval(tvShows.first().id, RemovalKind.Remove)

        robot.show(state(removals = RemovalState(pending = listOf(removed))))
            .assertTextVisible("TV show removed from favorites")
            .clickUndo()

        assertThat(robot.actions).containsExactly(FavoriteTvShowsAction.OnUndoRemove(removed.id))
    }

    @Test
    fun givenAFailedRemoval_whenShown_thenADialogOffersRetryOrDismissing() {
        val failed = FailedRemoval(PendingRemoval(tvShows.first().id, RemovalKind.Remove), UiError.NoInternet)

        robot.show(state(removals = RemovalState(failed = listOf(failed))))
            .clickAccept()

        assertThat(robot.actions).containsExactly(FavoriteTvShowsAction.OnDismissRemovalError)
    }

    @Test
    fun givenTheScreen_whenTappingBack_thenBackIsSent() {
        robot.show(state()).clickBack()

        assertThat(robot.actions).containsExactly(FavoriteTvShowsAction.OnBack)
    }
}
