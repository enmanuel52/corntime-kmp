package com.enmanuelbergling.corntime.feature.auth.domain.login

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.enmanuelbergling.corntime.core.domain.design.invoke
import com.enmanuelbergling.corntime.core.domain.design.invokeForResult
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.DataException
import com.enmanuelbergling.corntime.core.domain.model.auth.CreateSessionPost
import com.enmanuelbergling.corntime.core.domain.usecase.LogoutUseCase
import com.enmanuelbergling.corntime.core.domain.usecase.SyncUserDetailsUseCase
import com.enmanuelbergling.corntime.core.domain.util.Result
import com.enmanuelbergling.corntime.core.testing.data.FakeUserData
import com.enmanuelbergling.corntime.core.testing.datasource.AuthRemoteFunction
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserRemoteDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.UserRemoteFunction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LoginChainTest {
    private val remote = FakeAuthRemoteDataSource()
    private val auth = FakeAuthPreferenceDataSource()
    private val user = FakeUserPreferenceDataSource()
    private val account = FakeUserRemoteDataSource()
    private val chain
        get() = LoginChain(
            remote,
            auth,
            SyncUserDetailsUseCase(account, user),
            LogoutUseCase(user, auth),
        ).create()

    private fun request() = LoginRequest(username = "tim", password = "secret123")

    @Test
    fun givenEveryStepSucceeds_whenInvoked_thenSessionIsStoredAndAccountIsSynced() = runTest {
        val request = request()

        chain.invoke(request)

        assertThat(request.sessionId).isEqualTo("session-id")
        assertThat(auth.getSessionId().first()).isEqualTo("session-id")
        assertThat(user.getCurrentUser().first()).isEqualTo(FakeUserData.FAKE_USER)
    }

    @Test
    fun givenTheSteps_whenInvoked_thenTheTokenIsThreadedThroughInOrderWithTheCredentials() = runTest {
        remote.requestToken = "t1"

        chain.invoke(request())

        assertThat(remote.callsTo(AuthRemoteFunction.ValidateWithLogin))
            .isEqualTo(listOf(listOf<Any?>(CreateSessionPost("tim", "secret123", "t1"))))
        // the validated token (same value the fake echoes back) is what is exchanged for a session
        assertThat(remote.callsTo(AuthRemoteFunction.CreateSessionId)).isEqualTo(listOf(listOf<Any?>("t1")))
    }

    @Test
    fun givenWrongCredentials_whenInvoked_thenUnauthorizedAndNoSessionIsCreatedOrStored() = runTest {
        remote.fail(AuthRemoteFunction.ValidateWithLogin, DataError.Network.Unauthorized)

        val result = chain.invokeForResult(request())

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.Unauthorized))
        assertThat(remote.callCount(AuthRemoteFunction.CreateSessionId)).isEqualTo(0)
        assertThat(auth.getSessionId().first()).isEqualTo("")
        assertThat(account.callCount(UserRemoteFunction.GetAccount)).isEqualTo(0)
    }

    @Test
    fun givenTokenCreationFails_whenInvoked_thenNothingElseIsRequested() = runTest {
        remote.fail(AuthRemoteFunction.CreateRequestToken, DataError.Network.NoInternet)

        assertFailure { chain.invoke(request()) }.isInstanceOf<DataException>()

        assertThat(remote.callCount(AuthRemoteFunction.ValidateWithLogin)).isEqualTo(0)
    }

    @Test
    fun givenSessionCreationFails_whenInvoked_thenNothingIsStored() = runTest {
        remote.fail(AuthRemoteFunction.CreateSessionId, DataError.Network.ServerError)

        val request = request()
        assertFailure { chain.invoke(request) }.isInstanceOf<DataException>()

        assertThat(request.sessionId).isNull()
        assertThat(auth.getSessionId().first()).isEqualTo("")
    }

    @Test
    fun givenAccountSyncFails_whenInvoked_thenTheStoredSessionIsRolledBack() = runTest {
        account.fail(UserRemoteFunction.GetAccount, DataError.Network.RequestTimeout)

        val result = chain.invokeForResult(request())

        assertThat(result).isEqualTo(Result.Failure(DataError.Network.RequestTimeout))
        assertThat(auth.getSessionId().first()).isEqualTo("")
        assertThat(user.getCurrentUser().first()).isNull()
    }

    @Test
    fun givenAnEarlierSession_whenLoginFailsBeforeStoring_thenTheEarlierSessionIsUntouched() = runTest {
        auth.saveSessionId("old")
        remote.fail(AuthRemoteFunction.CreateRequestToken)

        chain.invokeForResult(request())

        assertThat(auth.getSessionId().first()).isEqualTo("old")
    }

    @Test
    fun givenSuccess_whenInvoked_thenEachEndpointIsCalledExactlyOnce() = runTest {
        chain.invoke(request())

        AuthRemoteFunction.entries.forEach { assertThat(remote.callCount(it)).isEqualTo(1) }
        assertThat(remote.callsTo(AuthRemoteFunction.CreateRequestToken).size).isEqualTo(1)
    }
}
