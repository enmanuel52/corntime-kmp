package com.enmanuelbergling.corntime.core.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.countryCode

internal actual fun platformHttpEngine(): HttpClientEngine = Darwin.create()

internal actual fun currentLanguage(): String {
    val locale = NSLocale.currentLocale
    val country = locale.countryCode
    return if (country.isNullOrBlank()) locale.languageCode else "${locale.languageCode}-$country"
}
