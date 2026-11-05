package com.enmanuelbergling.corntime.feature.watchlists.domain.lists

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ListUseCasesTest {
    private val user = FakeUserRemoteDataSource()

    @Test
    fun givenAFilledForm_whenValidating_thenNoErrors() {
        val result = ValidateListFormUseCase()("Weekend", "Movies for the weekend")

        assertThat(result.isValid).isEqualTo(true)
    }

    @Test
    fun givenBlankOrWhitespaceFields_whenValidating_thenEachIsReported() {
        val validate = ValidateListFormUseCase()

        assertThat(validate("", "d").nameError).isEqualTo(ListFieldError.NameRequired)
        assertThat(validate("   ", "d").nameError).isEqualTo(ListFieldError.NameRequired)
        assertThat(validate("n", "").descriptionError).isEqualTo(ListFieldError.DescriptionRequired)
        assertThat(validate("n", "  \n").descriptionError).isEqualTo(ListFieldError.DescriptionRequired)
        assertThat(validate("n", "d").nameError).isNull()
    }

    @Test
    fun givenFromTheWatchlist_whenMovingToFavorites_thenItIsAddedThenRemovedFromTheWatchlist() = runTest {
        val result = MoveMovieToFavoritesUseCase(user)(movieId = 5)

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(user.callsTo(UserRemoteFunction.AddMovieToFavorites).map { it.single() }).containsExactly(5)
        assertThat(user.callsTo(UserRemoteFunction.RemoveMovieFromAccountWatchlist).map { it.single() }).containsExactly(5)
        assertThat(user.callCount(UserRemoteFunction.DeleteMovieFromList)).isEqualTo(0)
    }

    @Test
    fun givenFromAList_whenMovingToFavorites_thenItLeavesThatListNotTheWatchlist() = runTest {
        MoveMovieToFavoritesUseCase(user)(movieId = 5, listId = 9)

        assertThat(user.callsTo(UserRemoteFunction.DeleteMovieFromList)).containsExactly(listOf<Any?>(5, 9))
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenAddingToFavoritesFails_whenMoving_thenTheMovieIsNotRemovedFromWhereItWas() = runTest {
        user.fail(UserRemoteFunction.AddMovieToFavorites, DataError.Network.NoInternet)

        val result = MoveMovieToFavoritesUseCase(user)(movieId = 5)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.NoInternet))
        assertThat(user.callCount(UserRemoteFunction.RemoveMovieFromAccountWatchlist)).isEqualTo(0)
    }

    @Test
    fun givenRemovingFails_whenMoving_thenTheFailureIsReportedSoARetryRepeatsBothSteps() = runTest {
        user.fail(UserRemoteFunction.RemoveMovieFromAccountWatchlist, DataError.Network.ServerError)

        val result = MoveMovieToFavoritesUseCase(user)(movieId = 5)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.ServerError))
        assertThat(user.callCount(UserRemoteFunction.AddMovieToFavorites)).isEqualTo(1)
    }
}
