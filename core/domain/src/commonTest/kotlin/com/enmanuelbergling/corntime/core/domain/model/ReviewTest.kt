package com.enmanuelbergling.corntime.core.domain.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.core.domain.model.movie.ElapsedTime
import com.enmanuelbergling.corntime.core.domain.model.movie.Review
import com.enmanuelbergling.corntime.core.domain.model.movie.ReviewAuthorDetails
import kotlin.test.Test
import kotlin.time.Instant

class ReviewTest {
    private val now = Instant.parse("2026-06-15T12:00:00Z")

    private fun review(createdAt: String) = Review(
        id = "1",
        author = "Tim",
        content = "",
        createdAt = createdAt,
        url = "",
        rating = null,
        authorDetails = ReviewAuthorDetails(name = "Tim", username = "tim", avatarPath = null),
    )

    private fun elapsed(createdAt: String) = review(createdAt).elapsedSincePosted(now)

    @Test
    fun givenPostedYearsAgo_whenMeasuring_thenWholeYears() {
        assertThat(elapsed("2024-06-15T12:00:00Z")).isEqualTo(ElapsedTime.Years(2))
    }

    @Test
    fun givenPostedMonthsAgo_whenMeasuring_thenWholeMonths() {
        assertThat(elapsed("2026-03-10T12:00:00Z")).isEqualTo(ElapsedTime.Months(3))
    }

    @Test
    fun givenPostedWeeksAgo_whenMeasuring_thenWholeWeeks() {
        assertThat(elapsed("2026-06-01T12:00:00Z")).isEqualTo(ElapsedTime.Weeks(2))
    }

    @Test
    fun givenPostedDaysAgo_whenMeasuring_thenWholeDays() {
        assertThat(elapsed("2026-06-12T12:00:00Z")).isEqualTo(ElapsedTime.Days(3))
    }

    @Test
    fun givenPostedHoursAgo_whenMeasuring_thenWholeHours() {
        assertThat(elapsed("2026-06-15T09:00:00Z")).isEqualTo(ElapsedTime.Hours(3))
    }

    @Test
    fun givenPostedMinutesAgo_whenMeasuring_thenWholeMinutes() {
        assertThat(elapsed("2026-06-15T11:45:00Z")).isEqualTo(ElapsedTime.Minutes(15))
    }

    @Test
    fun givenPostedSecondsAgo_whenMeasuring_thenJustNow() {
        assertThat(elapsed("2026-06-15T11:59:30Z")).isEqualTo(ElapsedTime.JustNow)
    }

    @Test
    fun givenPostedInTheFuture_whenMeasuring_thenJustNow() {
        assertThat(elapsed("2027-01-01T00:00:00Z")).isEqualTo(ElapsedTime.JustNow)
    }

    @Test
    fun givenUnparseableDate_whenMeasuring_thenJustNow() {
        assertThat(elapsed("yesterday-ish")).isEqualTo(ElapsedTime.JustNow)
    }

    @Test
    fun givenNoClockArgument_whenMeasuring_thenUsesTheCurrentTime() {
        assertThat(review("2000-01-01T00:00:00Z").elapsedSincePosted() is ElapsedTime.Years).isEqualTo(true)
    }
}
