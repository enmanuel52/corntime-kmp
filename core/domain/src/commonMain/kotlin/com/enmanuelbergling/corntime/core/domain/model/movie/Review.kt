package com.enmanuelbergling.corntime.core.domain.model.movie

import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlin.time.Clock
import kotlin.time.Instant

data class Review(
    val id: String,
    val author: String,
    val content: String,
    val createdAt: String,
    val url: String,
    val rating: Float?,
    val authorDetails: ReviewAuthorDetails,
) {
    /**
     * Largest elapsed unit since [createdAt] (e.g. 2 years, 1 week). Each platform formats it
     * with its own localized strings. Unparseable dates are treated as [ElapsedTime.JustNow].
     */
    fun elapsedSincePosted(now: Instant = Clock.System.now()): ElapsedTime {
        val posted = runCatching { Instant.parse(createdAt) }.getOrNull() ?: return ElapsedTime.JustNow
        if (posted >= now) return ElapsedTime.JustNow
        val period: DateTimePeriod = posted.periodUntil(now, TimeZone.UTC)
        val totalDays = (now - posted).inWholeDays
        return when {
            period.years >= 1 -> ElapsedTime.Years(period.years)
            period.months >= 1 -> ElapsedTime.Months(period.months)
            totalDays >= 7 -> ElapsedTime.Weeks((totalDays / 7).toInt())
            totalDays >= 1 -> ElapsedTime.Days(totalDays.toInt())
            period.hours >= 1 -> ElapsedTime.Hours(period.hours)
            period.minutes >= 1 -> ElapsedTime.Minutes(period.minutes)
            else -> ElapsedTime.JustNow
        }
    }
}

sealed interface ElapsedTime {
    data class Years(val amount: Int) : ElapsedTime
    data class Months(val amount: Int) : ElapsedTime
    data class Weeks(val amount: Int) : ElapsedTime
    data class Days(val amount: Int) : ElapsedTime
    data class Hours(val amount: Int) : ElapsedTime
    data class Minutes(val amount: Int) : ElapsedTime
    data object JustNow : ElapsedTime
}
