package com.enmanuelbergling.corntime.core.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.Locale

internal actual fun platformHttpEngine(): HttpClientEngine = OkHttp.create()

internal actual fun currentLanguage(): String = Locale.getDefault().toLanguageTag()
