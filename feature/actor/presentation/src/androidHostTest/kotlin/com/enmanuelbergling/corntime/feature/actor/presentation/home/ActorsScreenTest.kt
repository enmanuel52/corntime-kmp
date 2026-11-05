package com.enmanuelbergling.corntime.feature.actor.presentation.home

import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.actor.Actor
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeActorData
import com.enmanuelbergling.corntime.feature.actor.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.actor.presentation.util.ROBOLECTRIC_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class ActorsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val robot by lazy { ActorsRobot(rule) }
    private val tim = FakeActorData.ACTOR.copy(id = 1, originalName = "Tim Bergling")
    private val ann = FakeActorData.ACTOR.copy(id = 2, originalName = "Ann Actor")

    private fun state(
        items: List<Actor> = listOf(tim, ann),
        status: PagingStatus = PagingStatus.Idle,
    ) = ActorsState(PagingState(items = items, status = status))

    @Test
    fun givenLoadedActors_whenShown_thenTheirNamesAreVisible() {
        robot.show(state())
            .assertTextVisible("Tim Bergling")
            .assertTextVisible("Ann Actor")
            .assertTextAbsent("No actors found")
    }

    @Test
    fun givenFirstPageLoading_whenShown_thenNoEmptyMessageOrError() {
        robot.show(state(items = emptyList(), status = PagingStatus.LoadingFirstPage))
            .assertTextAbsent("No actors found")
            .assertTextAbsent("Retry")
    }

    @Test
    fun givenFinishedWithNothing_whenShown_thenTheEmptyMessageIsShown() {
        robot.show(state(items = emptyList(), status = PagingStatus.EndReached))
            .assertTextVisible("No actors found")
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenAnErrorScreenWithRetryAndRetrySendsTheAction() {
        robot.show(state(items = emptyList(), status = PagingStatus.Failed(DataError.Network.NoInternet)))
            .assertTextVisible("Retry")
            .clickRetry()

        assertThat(robot.pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenALaterPageFailed_whenShown_thenActorsStayAndAFooterRetryIsOffered() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.RequestTimeout)))
            .assertTextVisible("Tim Bergling")
            .assertRetryFooterVisible()
            .clickRetryFooter()

        assertThat(robot.pagingRequests).isEqualTo(1)
    }

    @Test
    fun givenAShortList_whenShown_thenTheNextPageIsRequestedBecauseTheEndIsVisible() {
        robot.show(state())

        assertThat(robot.pagingRequests).isGreaterThan(0)
    }

    @Test
    fun givenAFailedPage_whenShown_thenNoAutomaticPageRequestIsMadeUntilTheUserRetries() {
        robot.show(state(status = PagingStatus.Failed(DataError.Network.NoInternet)))

        assertThat(robot.pagingRequests).isEqualTo(0)
    }

    @Test
    fun givenNoFailure_whenShown_thenThereIsNoRetryFooter() {
        robot.show(state()).assertRetryFooterAbsent()
    }

    @Test
    fun givenActors_whenTappingTheSecondPhoto_thenThatActorIsClicked() {
        robot.show(state()).clickActorAt(1)

        assertThat(robot.actions).containsExactly(ActorsAction.OnActorClick(ann))
    }

    @Test
    fun givenTheScreen_whenTappingTheMenu_thenTheDrawerOpensWithoutAnyAction() {
        robot.show(state()).clickMenu()

        assertThat(robot.drawerOpens).isEqualTo(1)
        assertThat(robot.actions).isEmpty()
    }
}
