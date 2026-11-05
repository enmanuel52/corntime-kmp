package com.enmanuelbergling.corntime.feature.watchlists.presentation.created

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.user.WatchList
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.paging.PagingState
import com.enmanuelbergling.corntime.core.presentation.paging.PagingStatus
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.ListFieldError
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.PHONE_QUALIFIERS
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.ROBOLECTRIC_SDK
import com.enmanuelbergling.corntime.feature.watchlists.presentation.util.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = PHONE_QUALIFIERS)
class CreatedWatchListsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val comedy: WatchList = FakeUserData.FAKE_WATCH_LIST.copy(id = 1, name = "Comedy night", description = "Laughs")
    private val horror: WatchList = FakeUserData.FAKE_WATCH_LIST.copy(id = 2, name = "Horror marathon", description = "Scares")
    private val allActions = mutableListOf<CreatedWatchListsAction>()
    private val actions get() = allActions.filter { it != CreatedWatchListsAction.OnLoadNextPage }

    private fun show(
        lists: List<WatchList> = listOf(comedy, horror),
        status: PagingStatus = PagingStatus.Idle,
        form: CreateListForm = CreateListForm(),
        isWorking: Boolean = false,
        error: UiError? = null,
    ) = rule.setThemedContent {
        CreatedWatchListsScreen(
            state = CreatedWatchListsState(PagingState(items = lists, status = status), form, isWorking, error),
            onAction = { allActions += it },
            onBack = {},
        )
    }

    @Test
    fun givenLists_whenShown_thenTheirNamesAreVisible() {
        show()

        rule.onNodeWithText("Comedy night").assertIsDisplayed()
        rule.onNodeWithText("Horror marathon").assertIsDisplayed()
    }

    @Test
    fun givenNoLists_whenLoaded_thenTheEmptyMessageIsShown() {
        show(lists = emptyList(), status = PagingStatus.EndReached)

        rule.onNodeWithText("You have not created any list yet").assertIsDisplayed()
    }

    @Test
    fun givenFirstPageFailed_whenShown_thenRetryRequestsItAgain() {
        show(lists = emptyList(), status = PagingStatus.Failed(DataError.Network.NoInternet))

        rule.onNodeWithText("Retry").performClick()

        assertThat(allActions).contains(CreatedWatchListsAction.OnLoadNextPage)
    }

    @Test
    fun givenAList_whenTappingIt_thenItIsClicked() {
        show()

        rule.onNodeWithText("Horror marathon").performClick()

        assertThat(actions).containsExactly(CreatedWatchListsAction.OnListClick(horror))
    }

    @Test
    fun givenTheAddIcon_whenTapped_thenTheFormIsToggled() {
        show()

        rule.onNodeWithTag(CreatedListsTags.ADD).performClick()

        assertThat(actions).containsExactly(CreatedWatchListsAction.OnToggleForm)
    }

    private fun showFields(form: CreateListForm) = rule.setThemedContent {
        LazyColumn { createListFields(form) { allActions += it } }
    }

    @Test
    fun givenTheLists_whenShown_thenTheAddIconIsAvailable() {
        show(form = CreateListForm(isVisible = false))

        rule.onNodeWithTag(CreatedListsTags.ADD).assertIsDisplayed()
    }

    @Test
    fun givenTheFormFields_whenShown_thenBothHintsAreVisible() {
        showFields(CreateListForm(isVisible = true))

        rule.onNodeWithText("List name*").assertIsDisplayed()
        rule.onNodeWithText("List description*").assertIsDisplayed()
    }

    @Test
    fun givenFieldErrors_whenShown_thenTheMessagesAreLocalized() {
        showFields(
            CreateListForm(
                isVisible = true,
                nameError = ListFieldError.NameRequired,
                descriptionError = ListFieldError.DescriptionRequired,
            ),
        )

        rule.onNodeWithText("The list needs a name").assertIsDisplayed()
        rule.onNodeWithText("The list needs a description").assertIsDisplayed()
    }

    @Test
    fun givenTheFormFields_whenTyping_thenTheTextIsSentPerField() {
        showFields(CreateListForm(isVisible = true))

        rule.onNodeWithText("List name*").performTextInput("Weekend")
        rule.onNodeWithText("List description*").performTextInput("Cozy")

        assertThat(actions).contains(CreatedWatchListsAction.OnNameChange("Weekend"))
        assertThat(actions).contains(CreatedWatchListsAction.OnDescriptionChange("Cozy"))
    }

    @Test
    fun givenAFailure_whenShown_thenADialogShowsItAndAcceptDismissesIt() {
        show(error = UiError.NoInternet)

        rule.onNodeWithText("Accept").performClick()

        assertThat(actions).containsExactly(CreatedWatchListsAction.OnDismissError)
    }

    @Test
    fun givenTheScreen_whenTappingBack_thenNothingIsSentToTheViewModel() {
        show()

        rule.onNodeWithContentDescription("back icon").performClick()

        assertThat(actions.size).isEqualTo(0)
    }
}
