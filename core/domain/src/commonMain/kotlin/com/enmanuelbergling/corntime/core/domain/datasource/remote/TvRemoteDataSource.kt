package com.enmanuelbergling.corntime.core.domain.datasource.remote

import com.enmanuelbergling.corntime.core.domain.model.core.PageModel
import com.enmanuelbergling.corntime.core.domain.model.tv.EpisodeDetails
import com.enmanuelbergling.corntime.core.domain.model.tv.SeasonDetails
import com.enmanuelbergling.corntime.core.domain.model.tv.TvAccountStates
import com.enmanuelbergling.corntime.core.domain.model.tv.TvCredits
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShow
import com.enmanuelbergling.corntime.core.domain.model.tv.TvShowDetails
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse

interface TvRemoteDataSource {
    suspend fun getPopularTv(page: Int = 1): NetworkResult<PageModel<TvShow>>
    suspend fun getTopRatedTv(page: Int = 1): NetworkResult<PageModel<TvShow>>
    suspend fun getOnTheAirTv(page: Int = 1): NetworkResult<PageModel<TvShow>>
    suspend fun getAiringTodayTv(page: Int = 1): NetworkResult<PageModel<TvShow>>
    suspend fun getTvDetails(tvShowId: Int): NetworkResult<TvShowDetails>
    suspend fun getTvCredits(tvShowId: Int): NetworkResult<TvCredits>
    suspend fun getSeasonDetails(tvShowId: Int, seasonNumber: Int): NetworkResult<SeasonDetails>
    suspend fun getEpisodeDetails(tvShowId: Int, seasonNumber: Int, episodeNumber: Int): NetworkResult<EpisodeDetails>
    suspend fun searchTv(query: String, page: Int): NetworkResult<PageModel<TvShow>>

    // Account related, require a logged-in session
    suspend fun getTvAccountStates(tvShowId: Int): NetworkResult<TvAccountStates>
    suspend fun getAccountFavoriteTv(page: Int = 1): NetworkResult<PageModel<TvShow>>
    suspend fun addTvToFavorites(tvShowId: Int): NetworkResult<WatchResponse>
    suspend fun removeTvFromFavorites(tvShowId: Int): NetworkResult<WatchResponse>
    suspend fun getAccountWatchlistTv(page: Int = 1): NetworkResult<PageModel<TvShow>>
    suspend fun addTvToAccountWatchlist(tvShowId: Int): NetworkResult<WatchResponse>
    suspend fun removeTvFromAccountWatchlist(tvShowId: Int): NetworkResult<WatchResponse>
}
