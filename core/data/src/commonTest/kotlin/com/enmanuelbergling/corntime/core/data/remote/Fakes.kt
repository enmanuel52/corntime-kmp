package com.enmanuelbergling.corntime.core.data.remote

import com.enmanuelbergling.corntime.core.data.network.MockHttp
import com.enmanuelbergling.corntime.core.data.network.SessionProvider
import com.enmanuelbergling.corntime.core.domain.model.user.UserDetails
import com.enmanuelbergling.corntime.core.testing.datasource.FakeAuthPreferenceDataSource
import com.enmanuelbergling.corntime.core.testing.datasource.FakeUserPreferenceDataSource

class Session(sessionId: String = "", user: UserDetails? = null) {
    val auth = FakeAuthPreferenceDataSource(sessionId)
    val user = FakeUserPreferenceDataSource(user)
    val provider = SessionProvider(auth, this.user)

    companion object {
        val LoggedIn get() = Session("session-1", UserDetails(id = 42, username = "timcorn"))
        val LoggedOut get() = Session()
    }
}

fun MockHttp.sentRequests() = requests.map { "${it.method.value} ${it.url.encodedPath}" }
