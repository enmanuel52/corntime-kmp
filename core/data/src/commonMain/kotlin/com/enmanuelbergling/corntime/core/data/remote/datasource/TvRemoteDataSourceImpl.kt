package com.enmanuelbergling.corntime.core.data.remote.datasource

import com.enmanuelbergling.corntime.core.data.network.SessionProvider
import com.enmanuelbergling.corntime.core.data.network.safeCall
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.EpisodeDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.SeasonDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvAccountStatesDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvCreditsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvShowDetailsDto
import com.enmanuelbergling.corntime.core.data.remote.dto.tv.TvShowPageDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.FavoriteBody
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.WatchResponseDto
import com.enmanuelbergling.corntime.core.data.remote.dto.user.watch.WatchlistBody
import com.enmanuelbergling.corntime.core.data.remote.mapper.toModel
import com.enmanuelbergling.corntime.core.domain.datasource.remote.NetworkResult
import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.model.tv.EpisodeDetails
import com.enmanuelbergling.corntime.core.domain.model.tv.SeasonDetails
import com.enmanuelbergling.corntime.core.domain.model.tv.TvAccountStates
import com.enmanuelbergling.corntime.core.domain.model.tv.TvCredits
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShowDetails
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse
import com.enmanuelbergling.corntime.core.domain.util.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.koin.core.annotation.Single

private const val MEDIA_TYPE_TV = "tv"

@Single
internal class TvRemoteDataSourceImpl(
    private val httpClient: HttpClient,
    private val sessionProvider: SessionProvider,
) : TvRemoteDataSource {

    override suspend fun getPopularTv(page: Int) = tvPage("tv/popular", page)

    override suspend fun getTopRatedTv(page: Int) = tvPage("tv/top_rated", page)

    override suspend fun getOnTheAirTv(page: Int) = tvPage("tv/on_the_air", page)

    override suspend fun getAiringTodayTv(page: Int) = tvPage("tv/airing_today", page)

    override suspend fun getTvDetails(tvShowId: Int): NetworkResult<TvShowDetails> =
        safeCall<TvShowDetailsDto> { httpClient.get("tv/$tvShowId") }.map { it.toModel() }

    override suspend fun getTvCredits(tvShowId: Int): NetworkResult<TvCredits> =
        safeCall<TvCreditsDto> { httpClient.get("tv/$tvShowId/credits") }.map { it.toModel() }

    override suspend fun getSeasonDetails(tvShowId: Int, seasonNumber: Int): NetworkResult<SeasonDetails> =
        safeCall<SeasonDetailsDto> { httpClient.get("tv/$tvShowId/season/$seasonNumber") }
            .map { it.toModel() }

    override suspend fun getEpisodeDetails(
        tvShowId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): NetworkResult<EpisodeDetails> = safeCall<EpisodeDetailsDto> {
        httpClient.get("tv/$tvShowId/season/$seasonNumber/episode/$episodeNumber")
    }.map { it.toModel() }

    override suspend fun searchTv(query: String, page: Int) = tvPage("search/tv", page, "query" to query)

    override suspend fun getTvAccountStates(tvShowId: Int): NetworkResult<TvAccountStates> =
        sessionProvider.withSession { sessionId ->
            safeCall<TvAccountStatesDto> {
                httpClient.get("tv/$tvShowId/account_states") { parameter("session_id", sessionId) }
            }.map { it.toModel() }
        }

    override suspend fun getAccountFavoriteTv(page: Int): NetworkResult<PageModel<TvShow>> =
        sessionProvider.withAccount { sessionId, accountId ->
            tvPage("account/$accountId/favorite/tv", page, "session_id" to sessionId)
        }

    override suspend fun addTvToFavorites(tvShowId: Int) = markFavorite(tvShowId, favorite = true)

    override suspend fun removeTvFromFavorites(tvShowId: Int) = markFavorite(tvShowId, favorite = false)

    override suspend fun getAccountWatchlistTv(page: Int): NetworkResult<PageModel<TvShow>> =
        sessionProvider.withAccount { sessionId, accountId ->
            tvPage("account/$accountId/watchlist/tv", page, "session_id" to sessionId)
        }

    override suspend fun addTvToAccountWatchlist(tvShowId: Int) = markWatchlist(tvShowId, watchlist = true)

    override suspend fun removeTvFromAccountWatchlist(tvShowId: Int) = markWatchlist(tvShowId, watchlist = false)

    private suspend fun markFavorite(tvShowId: Int, favorite: Boolean): NetworkResult<WatchResponse> =
        sessionProvider.withAccount { sessionId, accountId ->
            safeCall<WatchResponseDto> {
                httpClient.post("account/$accountId/favorite") {
                    parameter("session_id", sessionId)
                    setBody(FavoriteBody(mediaType = MEDIA_TYPE_TV, mediaId = tvShowId, favorite = favorite))
                }
            }.map { it.toModel() }
        }

    private suspend fun markWatchlist(tvShowId: Int, watchlist: Boolean): NetworkResult<WatchResponse> =
        sessionProvider.withAccount { sessionId, accountId ->
            safeCall<WatchResponseDto> {
                httpClient.post("account/$accountId/watchlist") {
                    parameter("session_id", sessionId)
                    setBody(WatchlistBody(mediaType = MEDIA_TYPE_TV, mediaId = tvShowId, watchlist = watchlist))
                }
            }.map { it.toModel() }
        }

    private suspend fun tvPage(
        path: String,
        page: Int,
        vararg query: Pair<String, Any>,
    ): NetworkResult<PageModel<TvShow>> = safeCall<TvShowPageDto> {
        httpClient.get(path) {
            parameter("page", page)
            query.forEach { (key, value) -> parameter(key, value) }
        }
    }.map { dto -> PageModel(dto.results.map { it.toModel() }, dto.totalPages) }
}
