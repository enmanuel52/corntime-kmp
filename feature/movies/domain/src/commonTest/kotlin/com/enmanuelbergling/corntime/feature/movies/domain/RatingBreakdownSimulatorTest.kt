package com.enmanuelbergling.corntime.feature.movies.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBreakdownSimulator
import com.enmanuelbergling.corntime.feature.movies.domain.reviews.RatingBucket
import kotlin.math.abs
import kotlin.test.Test

class RatingBreakdownSimulatorTest {

    private fun List<RatingBucket>.average() =
        sumOf { it.stars * it.count }.toDouble() / sumOf { it.count }

    @Test
    fun givenNobodyVoted_whenSimulating_thenThereIsNoBreakdown() {
        assertThat(RatingBreakdownSimulator.get(average = 4.0, count = 0)).isEmpty()
        assertThat(RatingBreakdownSimulator.get(average = 4.0, count = -3)).isEmpty()
    }

    @Test
    fun givenVotes_whenSimulating_thenBucketsGoFromFiveStarsToOne() {
        val stars = RatingBreakdownSimulator.get(average = 4.3, count = 250).map { it.stars }

        assertThat(stars).containsExactly(5, 4, 3, 2, 1)
    }

    @Test
    fun givenVotes_whenSimulating_thenCountsAddUpToTheVoteCount() {
        val buckets = RatingBreakdownSimulator.get(average = 3.2, count = 137)

        assertThat(buckets.sumOf { it.count }).isEqualTo(137)
    }

    @Test
    fun givenVotes_whenSimulating_thenFractionsAddUpToOne() {
        val total = RatingBreakdownSimulator.get(average = 2.5, count = 80).sumOf { it.fraction.toDouble() }

        assertThat(total).isBetween(0.999, 1.001)
    }

    @Test
    fun givenAverageAndManyVotes_whenSimulating_thenTheSimulatedAverageMatches() {
        listOf(1.5, 2.8, 3.5, 4.3, 4.9).forEach { average ->
            val simulated = RatingBreakdownSimulator.get(average, count = 500).average()

            assertThat(abs(simulated - average)).isBetween(0.0, 0.01)
        }
    }

    @Test
    fun givenSameInput_whenSimulatingTwice_thenTheResultIsIdentical() {
        assertThat(RatingBreakdownSimulator.get(3.9, 120)).isEqualTo(RatingBreakdownSimulator.get(3.9, 120))
    }

    @Test
    fun givenUnratedMovieAverageOfZero_whenSimulating_thenItIsClampedInsteadOfCrashing() {
        val buckets = RatingBreakdownSimulator.get(average = 0.0, count = 10)

        assertThat(buckets.sumOf { it.count }).isEqualTo(10)
        assertThat(buckets.average()).isBetween(1.0, 1.5)
    }

    @Test
    fun givenAverageAboveTheScale_whenSimulating_thenItIsClamped() {
        val buckets = RatingBreakdownSimulator.get(average = 8.0, count = 10)

        assertThat(buckets.average()).isBetween(4.5, 5.0)
    }

    @Test
    fun givenSingleVote_whenSimulating_thenTheVoteLandsOnTheClosestStar() {
        val buckets = RatingBreakdownSimulator.get(average = 4.0, count = 1)

        assertThat(buckets.single { it.count == 1 }.stars).isEqualTo(4)
    }
}
