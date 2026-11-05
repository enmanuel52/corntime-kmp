package com.enmanuelbergling.corntime.core.designsystem.core

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.enmanuelbergling.corntime.core.designsystem.theme.Dimen

val LocalDimen: ProvidableCompositionLocal<Dimen> = compositionLocalOf { Dimen() }