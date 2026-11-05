package com.enmanuelbergling.corntime.core.presentation.error

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.error.DataError
import com.enmanuelbergling.corntime.core.domain.error.Error
import kotlin.test.Test

class UiErrorTest {

    @Test
    fun givenEveryNetworkError_whenMapped_thenUsesTheIntendedCategory() {
        val expected = mapOf(
            DataError.Network.Unauthorized to UiError.Unauthorized,
            DataError.Network.Forbidden to UiError.Unauthorized,
            DataError.Network.NoInternet to UiError.NoInternet,
            DataError.Network.RequestTimeout to UiError.Timeout,
            DataError.Network.ServerError to UiError.Server,
            DataError.Network.ServiceUnavailable to UiError.Server,
            DataError.Network.BadRequest to UiError.Unknown,
            DataError.Network.NotFound to UiError.Unknown,
            DataError.Network.Conflict to UiError.Unknown,
            DataError.Network.TooManyRequests to UiError.Unknown,
            DataError.Network.Serialization to UiError.Unknown,
            DataError.Network.Unknown to UiError.Unknown,
        )
        assertThat(expected.keys).isEqualTo(DataError.Network.entries.toSet())
        expected.forEach { (error, ui) -> assertThat(error.toUiError()).isEqualTo(ui) }
    }

    @Test
    fun givenLocalError_whenMapped_thenUnknown() {
        assertThat(DataError.Local.DiskFull.toUiError()).isEqualTo(UiError.Unknown)
    }

    @Test
    fun givenForeignErrorType_whenMapped_thenUnknown() {
        val custom = object : Error {}

        assertThat(custom.toUiError()).isEqualTo(UiError.Unknown)
    }
}
