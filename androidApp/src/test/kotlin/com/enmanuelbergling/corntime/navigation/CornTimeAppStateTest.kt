package com.enmanuelbergling.corntime.navigation

import androidx.navigation3.runtime.NavKey
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.feature.actor.presentation.navigation.ActorDetailsKey
import com.enmanuelbergling.corntime.feature.actor.presentation.navigation.ActorsKey
import com.enmanuelbergling.corntime.feature.auth.presentation.navigation.LoginKey
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.MovieDetailsKey
import com.enmanuelbergling.corntime.feature.movies.presentation.navigation.MoviesHomeKey
import com.enmanuelbergling.corntime.feature.settings.presentation.navigation.SettingsKey
import com.enmanuelbergling.corntime.feature.tvshows.presentation.navigation.EpisodeDetailsKey
import com.enmanuelbergling.corntime.feature.tvshows.presentation.navigation.TvShowDetailsKey
import com.enmanuelbergling.corntime.feature.tvshows.presentation.navigation.TvShowsHomeKey
import com.enmanuelbergling.corntime.feature.watchlists.presentation.navigation.ListDetailsKey
import com.enmanuelbergling.corntime.feature.watchlists.presentation.navigation.WatchlistKey
import kotlin.test.Test

class CornTimeAppStateTest {
    private fun state(
        vararg stack: NavKey = arrayOf(MoviesHomeKey),
        isLoggedIn: Boolean = false,
    ) = CornTimeAppState(stack.toMutableList(), isLoggedIn = isLoggedIn)

    @Test
    fun givenMoviesHome_whenCheckingTheDrawer_thenMoviesIsSelectedAndTheGestureIsEnabled() {
        val state = state()

        assertThat(state.isSelected(TopDestination.Movies)).isTrue()
        assertThat(state.isSelected(TopDestination.Settings)).isFalse()
        assertThat(state.mainDrawerEnabled).isTrue()
    }

    @Test
    fun givenAMovieDetails_whenCheckingTheDrawer_thenNothingIsSelectedAndTheGestureIsOff() {
        val state = state(MoviesHomeKey, MovieDetailsKey(1))

        assertThat(state.currentTopDestination).isNull()
        assertThat(state.mainDrawerEnabled).isFalse()
    }

    @Test
    fun givenSettings_whenCheckingTheDrawer_thenTheGestureIsOffEvenThoughItIsATopDestination() {
        val state = state(MoviesHomeKey, SettingsKey)

        assertThat(state.isSelected(TopDestination.Settings)).isTrue()
        assertThat(state.mainDrawerEnabled).isFalse()
    }

    @Test
    fun givenDetailsOpen_whenChoosingSettingsInTheDrawer_thenTheStackIsMoviesThenSettings() {
        val state = state(MoviesHomeKey, MovieDetailsKey(1), MovieDetailsKey(2))

        state.navigateToDrawerDestination(TopDestination.Settings)

        assertThat(state.backStack).containsExactly(MoviesHomeKey, SettingsKey)
    }

    @Test
    fun givenSettings_whenChoosingMoviesInTheDrawer_thenTheStackIsJustMovies() {
        val state = state(MoviesHomeKey, SettingsKey)

        state.navigateToDrawerDestination(TopDestination.Movies)

        assertThat(state.backStack).containsExactly(MoviesHomeKey)
    }

    @Test
    fun givenSettings_whenGoingBack_thenReturnsToMovies() {
        val state = state(MoviesHomeKey, SettingsKey)

        state.back()

        assertThat(state.currentKey).isEqualTo(MoviesHomeKey)
    }

    @Test
    fun givenOnlyTheRoot_whenGoingBack_thenTheStackIsNeverEmptied() {
        val state = state()

        state.back()

        assertThat(state.backStack).containsExactly(MoviesHomeKey)
    }

    @Test
    fun givenSettings_whenLoggingInAndSucceeding_thenTheAppRestartsFromMoviesWithoutLoginInTheStack() {
        val state = state(MoviesHomeKey, SettingsKey)

        state.navigateToLogin()
        assertThat(state.currentKey).isEqualTo(LoginKey)

        state.onLoginSucceeded()
        assertThat(state.backStack).containsExactly(MoviesHomeKey)
    }

    @Test
    fun givenLoginAbandoned_whenGoingBack_thenReturnsToSettings() {
        val state = state(MoviesHomeKey, SettingsKey)
        state.navigateToLogin()

        state.back()

        assertThat(state.currentKey).isEqualTo(SettingsKey)
    }

    @Test
    fun givenLoggedOut_whenListingDrawerDestinations_thenListsIsHidden() {
        val loggedOut = state(isLoggedIn = false).drawerDestinations

        assertThat(loggedOut.contains(TopDestination.Lists)).isFalse()
        assertThat(loggedOut.none { it.loginRequired }).isTrue()
    }

    @Test
    fun givenLoggedIn_whenListingDrawerDestinations_thenListsIsOffered() {
        assertThat(state(isLoggedIn = true).drawerDestinations).isEqualTo(TopDestination.entries)
    }

    @Test
    fun givenMovies_whenChoosingListsInTheDrawer_thenTheStackIsMoviesThenTheWatchlistWithoutTheDrawerGesture() {
        val state = state(isLoggedIn = true)

        state.navigateToDrawerDestination(TopDestination.Lists)

        assertThat(state.backStack).containsExactly(MoviesHomeKey, WatchlistKey)
        assertThat(state.mainDrawerEnabled).isFalse()
    }

    @Test
    fun givenAShortcut_whenOpeningAList_thenTheStackIsMoviesThenThatListAndBackReturnsToMovies() {
        val state = state(MoviesHomeKey, SettingsKey, LoginKey)

        state.openList(listId = 4, listName = "Comedy")

        assertThat(state.backStack).containsExactly(MoviesHomeKey, ListDetailsKey(4, "Comedy"))
        state.back()
        assertThat(state.currentKey).isEqualTo(MoviesHomeKey)
    }

    @Test
    fun givenMovies_whenChoosingActorsInTheDrawer_thenTheStackIsMoviesThenActorsAndTheDrawerGestureStaysOn() {
        val state = state()

        state.navigateToDrawerDestination(TopDestination.Actors)

        assertThat(state.backStack).containsExactly(MoviesHomeKey, ActorsKey)
        assertThat(state.isSelected(TopDestination.Actors)).isTrue()
        assertThat(state.mainDrawerEnabled).isTrue()
    }

    @Test
    fun givenActorDetails_whenGoingBack_thenReturnsToTheActorsList() {
        val state = state(MoviesHomeKey, ActorsKey)

        state.navigate(ActorDetailsKey(id = 5, imagePath = "/a.jpg", name = "Tim"))
        assertThat(state.mainDrawerEnabled).isFalse()

        state.back()
        assertThat(state.currentKey).isEqualTo(ActorsKey)
    }

    @Test
    fun givenTheDrawer_whenListingItsDestinations_thenTvShowsComesRightAfterMovies() {
        val destinations = state(isLoggedIn = false).drawerDestinations

        assertThat(destinations.take(2)).containsExactly(TopDestination.Movies, TopDestination.TvShows)
        assertThat(TopDestination.TvShows.loginRequired).isFalse()
    }

    @Test
    fun givenMovies_whenChoosingTvShowsInTheDrawer_thenTheStackIsMoviesThenTvShowsAndTheDrawerGestureStaysOn() {
        val state = state()

        state.navigateToDrawerDestination(TopDestination.TvShows)

        assertThat(state.backStack).containsExactly(MoviesHomeKey, TvShowsHomeKey)
        assertThat(state.isSelected(TopDestination.TvShows)).isTrue()
        assertThat(state.isSelected(TopDestination.Movies)).isFalse()
        assertThat(state.mainDrawerEnabled).isTrue()
    }

    @Test
    fun givenTvShowsHome_whenGoingBack_thenReturnsToMovies() {
        val state = state(MoviesHomeKey, TvShowsHomeKey)

        state.back()

        assertThat(state.currentKey).isEqualTo(MoviesHomeKey)
    }

    @Test
    fun givenTvShowDetailsOpen_whenCheckingTheDrawer_thenNothingIsSelectedAndTheGestureIsOff() {
        val state = state(MoviesHomeKey, TvShowsHomeKey, TvShowDetailsKey(1))

        assertThat(state.currentTopDestination).isNull()
        assertThat(state.mainDrawerEnabled).isFalse()
    }

    @Test
    fun givenTvShowDetailsOpen_whenChoosingMoviesInTheDrawer_thenTheStackIsJustMovies() {
        val state = state(MoviesHomeKey, TvShowsHomeKey, TvShowDetailsKey(1), EpisodeDetailsKey(1, 1, 1))

        state.navigateToDrawerDestination(TopDestination.Movies)

        assertThat(state.backStack).containsExactly(MoviesHomeKey)
    }

    @Test
    fun givenAnActorOpenedFromAnEpisode_whenGoingBack_thenReturnsToTheEpisode() {
        val state = state(MoviesHomeKey, TvShowsHomeKey, TvShowDetailsKey(1), EpisodeDetailsKey(1, 1, 1))

        state.navigate(ActorDetailsKey(id = 5, imagePath = null, name = "Tim"))
        state.back()

        assertThat(state.currentKey).isEqualTo(EpisodeDetailsKey(1, 1, 1))
    }
}
