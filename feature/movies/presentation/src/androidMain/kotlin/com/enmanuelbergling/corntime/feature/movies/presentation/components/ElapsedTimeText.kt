package com.enmanuelbergling.corntime.feature.movies.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.enmanuelbergling.corntime.core.domain.model.movie.ElapsedTime
import com.enmanuelbergling.corntime.feature.movies.presentation.R

/** "3 weeks ago", localized with plurals. */
@Composable
fun ElapsedTime.asText(): String = when (this) {
    is ElapsedTime.Years -> pluralStringResource(R.plurals.posted_years_ago, amount, amount)
    is ElapsedTime.Months -> pluralStringResource(R.plurals.posted_months_ago, amount, amount)
    is ElapsedTime.Weeks -> pluralStringResource(R.plurals.posted_weeks_ago, amount, amount)
    is ElapsedTime.Days -> pluralStringResource(R.plurals.posted_days_ago, amount, amount)
    is ElapsedTime.Hours -> pluralStringResource(R.plurals.posted_hours_ago, amount, amount)
    is ElapsedTime.Minutes -> pluralStringResource(R.plurals.posted_minutes_ago, amount, amount)
    ElapsedTime.JustNow -> stringResource(R.string.posted_just_now)
}
