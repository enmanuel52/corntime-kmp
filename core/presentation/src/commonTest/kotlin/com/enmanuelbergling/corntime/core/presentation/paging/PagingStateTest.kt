package com.enmanuelbergling.corntime.core.presentation.paging

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import kotlin.test.Test

class PagingStateTest {

    @Test
    fun givenFailedStatus_whenReadingUiError_thenMapsTheNetworkError() {
        val state = PagingState<Int>(status = PagingStatus.Failed(DataError.Network.NoInternet))

        assertThat(state.uiError).isEqualTo(UiError.NoInternet)
    }

    @Test
    fun givenNonFailedStatuses_whenReadingUiError_thenNull() {
        listOf(
            PagingStatus.Idle,
            PagingStatus.LoadingFirstPage,
            PagingStatus.LoadingMore,
            PagingStatus.EndReached,
        ).forEach { assertThat(PagingState<Int>(status = it).uiError).isNull() }
    }

    @Test
    fun givenItemsWithoutEnd_whenCheckingEmpty_thenNotEmpty() {
        assertThat(PagingState<Int>().isEmpty).isEqualTo(false)
        assertThat(PagingState<Int>(status = PagingStatus.EndReached).isEmpty).isEqualTo(true)
        assertThat(PagingState(items = listOf(1), status = PagingStatus.EndReached).isEmpty).isEqualTo(false)
    }
}
