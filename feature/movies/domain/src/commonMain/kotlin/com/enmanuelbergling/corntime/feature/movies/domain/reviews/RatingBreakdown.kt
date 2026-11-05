package com.enmanuelbergling.corntime.feature.movies.domain.reviews

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

/** How many of the simulated voters gave [stars] (1..5); [fraction] is its share of all voters, 0..1. */
data class RatingBucket(
    val stars: Int,
    val count: Int,
    val fraction: Float,
)

/**
 * TMDB only reports an average and a vote count, not how votes spread over the stars. This simulates a
 * plausible spread whose average matches the real one, so the screens can draw a breakdown chart.
 *
 * Ratings are drawn at random weighted towards the average (so the chart looks natural, not a two-value
 * split), then nudged up or down until their sum matches the target. The random source is seeded, so a
 * movie always gets the same chart.
 */
object RatingBreakdownSimulator {
    private const val SEED = 42
    private val STARS = 1..5

    /**
     * Buckets from 5 stars down to 1, or an empty list when nobody voted. [average] is on the 1..5 scale;
     * values outside it (an unrated movie reports 0) are clamped.
     */
    fun get(average: Double, count: Int): List<RatingBucket> {
        if (count <= 0) return emptyList()
        val ratings = simulate(average.coerceIn(STARS.first.toDouble(), STARS.last.toDouble()), count, Random(SEED))
        return STARS.reversed().map { stars ->
            val votes = ratings.count { it == stars }
            RatingBucket(stars = stars, count = votes, fraction = votes.toFloat() / count)
        }
    }

    private fun simulate(average: Double, count: Int, random: Random): IntArray {
        val values = STARS.toList()
        val weights = values.map { 1.0 / (1.0 + abs(it - average)) }
        val cumulative = DoubleArray(weights.size)
        var running = 0.0
        weights.forEachIndexed { index, weight ->
            running += weight
            cumulative[index] = running
        }

        val ratings = IntArray(count) {
            val draw = random.nextDouble() * running
            values[cumulative.indexOfFirst { it >= draw }.takeIf { it >= 0 } ?: values.lastIndex]
        }

        // nudge the sum onto the target, one star at a time over a shuffled order
        val targetSum = (average * count).roundToLong()
            .coerceIn(STARS.first.toLong() * count, STARS.last.toLong() * count)
        var diff = targetSum - ratings.sumOf { it.toLong() }
        val order = ratings.indices.shuffled(random)
        repeat(STARS.last - STARS.first + 1) {
            for (index in order) {
                if (diff == 0L) return ratings
                when {
                    diff > 0 && ratings[index] < STARS.last -> { ratings[index]++; diff-- }
                    diff < 0 && ratings[index] > STARS.first -> { ratings[index]--; diff++ }
                }
            }
        }
        return ratings
    }
}
