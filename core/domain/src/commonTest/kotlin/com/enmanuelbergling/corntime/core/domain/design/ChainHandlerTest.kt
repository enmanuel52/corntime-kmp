package com.enmanuelbergling.corntime.core.domain.design

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ChainHandlerTest {

    private class Request {
        val visited = mutableListOf<String>()
    }

    private class RecordingHandler(
        private val name: String,
        override val next: ChainHandler<Request>? = null,
        private val failure: Throwable? = null,
    ) : ChainHandler<Request> {
        override suspend fun handle(request: Request) {
            failure?.let { throw it }
            request.visited += name
        }
    }

    @Test
    fun givenThreeHandlers_whenInvoked_thenAllRunInOrderOnTheSameRequest() = runTest {
        val chain = RecordingHandler("first", RecordingHandler("second", RecordingHandler("third")))
        val request = Request()

        chain.invoke(request)

        assertThat(request.visited).containsExactly("first", "second", "third")
    }

    @Test
    fun givenSingleHandlerWithoutNext_whenInvoked_thenOnlyItRuns() = runTest {
        val request = Request()

        RecordingHandler("only").invoke(request)

        assertThat(request.visited).containsExactly("only")
    }

    @Test
    fun givenMiddleHandlerFails_whenInvoked_thenChainStopsAndErrorPropagates() = runTest {
        val chain = RecordingHandler(
            name = "first",
            next = RecordingHandler(
                name = "second",
                failure = IllegalStateException("boom"),
                next = RecordingHandler("third"),
            ),
        )
        val request = Request()

        assertFailure { chain.invoke(request) }.isInstanceOf<IllegalStateException>()
        assertThat(request.visited).containsExactly("first")
    }

    @Test
    fun givenFirstHandlerFails_whenInvoked_thenNothingIsRecorded() = runTest {
        val chain = RecordingHandler("first", RecordingHandler("second"), failure = RuntimeException())
        val request = Request()

        assertFailure { chain.invoke(request) }
        assertThat(request.visited).isEmpty()
    }

    @Test
    fun givenDataExceptionInChain_whenInvokedForResult_thenFailureCarriesTypedErrorAndEarlierWorkIsKept() = runTest {
        val chain = RecordingHandler(
            name = "first",
            next = RecordingHandler("second", failure = DataException(DataError.Network.NoInternet)),
        )
        val request = Request()

        val result = chain.invokeForResult(request)

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.NoInternet))
        assertThat(request.visited).containsExactly("first")
    }

    @Test
    fun givenSuccessfulChain_whenInvokedForResult_thenSuccess() = runTest {
        val result = RecordingHandler("only").invokeForResult(Request())

        assertThat(result).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun givenUnexpectedException_whenInvokedForResult_thenItIsNotSwallowed() = runTest {
        val chain = RecordingHandler("boom", failure = IllegalStateException("bug"))

        assertFailure { chain.invokeForResult(Request()) }.isInstanceOf<IllegalStateException>()
    }
}
