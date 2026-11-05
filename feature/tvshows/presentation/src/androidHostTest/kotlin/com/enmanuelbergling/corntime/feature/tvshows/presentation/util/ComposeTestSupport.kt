package com.enmanuelbergling.corntime.feature.tvshows.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.enmanuelbergling.corntime.core.designsystem.theme.CornTimeTheme

/** Sets screen content inside the app theme (static colors, so tests don't depend on the device wallpaper). */
fun ComposeContentTestRule.setThemedContent(content: @Composable () -> Unit) = setContent {
    CornTimeTheme(dynamicColor = false) { content() }
}

/** A typical phone (411x891dp, xxhdpi); Robolectric's default 320dp screen would lay lists out in one column. */
const val PHONE_QUALIFIERS = "w411dp-h891dp-xxhdpi"

const val ROBOLECTRIC_SDK = 34
