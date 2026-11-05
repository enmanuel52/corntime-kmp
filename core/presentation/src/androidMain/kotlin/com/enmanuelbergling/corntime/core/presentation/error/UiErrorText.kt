package com.enmanuelbergling.corntime.core.presentation.error

import androidx.annotation.StringRes
import com.enmanuelbergling.corntime.core.presentation.R

@get:StringRes
val UiError.messageResource: Int
    get() = when (this) {
        UiError.Unauthorized -> R.string.user_unauthorized_message
        UiError.NoInternet -> R.string.no_internet_message
        UiError.Timeout -> R.string.net_time_out_exception_message
        UiError.Server, UiError.Unknown -> R.string.default_net_exception_message
    }
