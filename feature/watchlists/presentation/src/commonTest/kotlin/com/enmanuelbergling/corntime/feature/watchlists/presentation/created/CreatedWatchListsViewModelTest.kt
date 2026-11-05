package com.enmanuelbergling.corntime.feature.watchlists.presentation.created

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.model.user.CreateListPost
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import com.enmanuelbergling.corntime.core.testing.util.MainDispatcherTest
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.ListFieldError
import com.enmanuelbergling.corntime.feature.watchlists.domain.lists.ValidateListFormUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class CreatedWatchListsViewModelTest : MainDispatcherTest() {
    private val user = FakeUserRemoteDataSource()

    private fun viewModel() = CreatedWatchListsViewModel(user, ValidateListFormUseCase())

    private fun CreatedWatchListsViewModel.fill(name: String = "Weekend", description: String = "Cozy movies") {
        onAction(CreatedWatchListsAction.OnToggleForm)
        onAction(CreatedWatchListsAction.OnNameChange(name))
        onAction(CreatedWatchListsAction.OnDescriptionChange(description))
    }

    @Test
    fun givenNewViewModel_whenCreated_thenTheUsersListsAreLoadedAndTheFormIsClosed() {
        val state = viewModel().state.value

        assertThat(state.lists.size).isEqualTo(1)
        assertThat(state.form.isVisible).isFalse()
    }

    @Test
    fun givenAClosedForm_whenToggled_thenItOpensAndTogglingAgainClosesItDiscardingWhatWasTyped() {
        val viewModel = viewModel()

        viewModel.fill(name = "Half typed", description = "")
        assertThat(viewModel.state.value.form.isVisible).isTrue()

        viewModel.onAction(CreatedWatchListsAction.OnToggleForm)

        assertThat(viewModel.state.value.form).isEqualTo(CreateListForm())
    }

    @Test
    fun givenAnEmptyForm_whenSubmitting_thenBothFieldsAreFlaggedAndNothingIsSent() {
        val viewModel = viewModel()
        viewModel.onAction(CreatedWatchListsAction.OnToggleForm)

        viewModel.onAction(CreatedWatchListsAction.OnSubmitForm)

        assertThat(viewModel.state.value.form.nameError).isEqualTo(ListFieldError.NameRequired)
        assertThat(viewModel.state.value.form.descriptionError).isEqualTo(ListFieldError.DescriptionRequired)
        assertThat(user.callCount(UserRemoteFunction.CreateWatchList)).isEqualTo(0)
    }

    @Test
    fun givenFieldErrors_whenEditingAField_thenOnlyThatErrorClears() {
        val viewModel = viewModel()
        viewModel.onAction(CreatedWatchListsAction.OnToggleForm)
        viewModel.onAction(CreatedWatchListsAction.OnSubmitForm)

        viewModel.onAction(CreatedWatchListsAction.OnNameChange("n"))

        assertThat(viewModel.state.value.form.nameError).isNull()
        assertThat(viewModel.state.value.form.descriptionError).isEqualTo(ListFieldError.DescriptionRequired)
    }

    @Test
    fun givenAValidForm_whenSubmitting_thenTheListIsCreatedTrimmedTheFormResetsAndTheListsReload() = runTest {
        val viewModel = viewModel()
        viewModel.fill(name = "  Weekend ", description = " Cozy ")

        viewModel.onAction(CreatedWatchListsAction.OnSubmitForm)

        assertThat(user.createdLists).containsExactly(CreateListPost("Weekend", "Cozy"))
        assertThat(viewModel.state.value.form).isEqualTo(CreateListForm())
        assertThat(viewModel.state.value.isWorking).isFalse()
        assertThat(user.callCount(UserRemoteFunction.GetWatchLists)).isEqualTo(2)
    }

    @Test
    fun givenCreatingFails_whenSubmitting_thenTheErrorShowsAndTheFormKeepsWhatWasTyped() = runTest {
        user.fail(UserRemoteFunction.CreateWatchList, DataError.Network.NoInternet)
        val viewModel = viewModel()
        viewModel.fill()

        viewModel.onAction(CreatedWatchListsAction.OnSubmitForm)

        assertThat(viewModel.state.value.error).isEqualTo(UiError.NoInternet)
        assertThat(viewModel.state.value.form.name).isEqualTo("Weekend")
        assertThat(viewModel.state.value.isWorking).isFalse()
    }

    @Test
    fun givenARequestInFlight_whenSubmittingAgain_thenOnlyOneListIsCreated() = runTest {
        user.hold()
        val viewModel = viewModel()
        viewModel.fill()

        viewModel.onAction(CreatedWatchListsAction.OnSubmitForm)
        viewModel.onAction(CreatedWatchListsAction.OnSubmitForm)

        assertThat(viewModel.state.value.isWorking).isTrue()
        user.release()
        assertThat(user.createdLists.size).isEqualTo(1)
    }

    @Test
    fun givenAList_whenDeleted_thenAnEventAnnouncesItAndTheListsReload() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(CreatedWatchListsAction.OnDeleteList(7))

            assertThat(awaitItem()).isEqualTo(CreatedWatchListsEvent.ListDeleted(7))
        }
        assertThat(user.callsTo(UserRemoteFunction.DeleteList).map { it.single() }).containsExactly(7)
        assertThat(user.callCount(UserRemoteFunction.GetWatchLists)).isEqualTo(2)
    }

    @Test
    fun givenDeletingFails_whenDeleting_thenAnErrorShowsAndNoEventIsSent() = runTest {
        user.fail(UserRemoteFunction.DeleteList, DataError.Network.ServerError)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(CreatedWatchListsAction.OnDeleteList(7))

            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isEqualTo(UiError.Server)

        viewModel.onAction(CreatedWatchListsAction.OnDismissError)
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun givenAList_whenClicked_thenNavigationCarriesItsIdAndName() = runTest {
        val viewModel = viewModel()
        val list = FakeUserData.FAKE_WATCH_LIST.copy(id = 4, name = "comedy")

        viewModel.events.test {
            viewModel.onAction(CreatedWatchListsAction.OnListClick(list))

            assertThat(awaitItem()).isEqualTo(CreatedWatchListsEvent.NavigateToDetails(4, "comedy"))
        }
    }

    @Test
    fun givenTheFirstTimeTheScreenIsShown_whenReportingIt_thenNothingIsReloaded() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(CreatedWatchListsAction.OnScreenShown)

        assertThat(user.callCount(UserRemoteFunction.GetWatchLists)).isEqualTo(1)
    }

    @Test
    fun givenTheScreenCameBack_whenReportingItAgain_thenTheListReloadsFromTheFirstPage() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(CreatedWatchListsAction.OnScreenShown)

        viewModel.onAction(CreatedWatchListsAction.OnScreenShown)

        assertThat(user.callCount(UserRemoteFunction.GetWatchLists)).isEqualTo(2)
    }
}
