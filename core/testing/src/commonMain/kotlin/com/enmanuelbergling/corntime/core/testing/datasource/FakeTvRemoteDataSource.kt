package com.enmanuelbergling.corntime.core.testing.datasource

import com.enmanuelbergling.corntime.core.domain.datasource.remote.TvRemoteDataSource
import com.enmanuelbergling.corntime.core.domain.model.user.WatchResponse
import com.enmanuelbergling.corntime.core.testing.data.FakeTvData
import com.enmanuelbergling.corntime.core.testing.data.fakePage
import com.enmanuelbergling.corntime.core.testing.util.FakeBehavior

enum class TvRemoteFunction {
    GetPopularTv, GetTopRatedTv, GetOnTheAirTv, GetAiringTodayTv, GetTvDetails, GetTvCredits,
    GetSeasonDetails, GetEpisodeDetails, SearchTv, GetTvAccountStates, GetAccountFavoriteTv,
    AddTvToFavorites, RemoveTvFromFavorites, GetAccountWatchlistTv, AddTvToAccountWatchlist,
    RemoveTvFromAccountWatchlist,
}

class FakeTvRemoteDataSource : FakeBehavior<TvRemoteFunction>(), TvRemoteDataSource {
    var totalPages: Int = 1
    var accountStates = FakeTvData.DEFAULT_TV_ACCOUNT_STATES

    private fun tvPage(page: Int) = fakePage(FakeTvData.TV_SHOWS, page, totalPages) { copy(id = it) }
    private val ok = WatchResponse("Success.")

    override suspend fun getPopularTv(page: Int) = respond(TvRemoteFunction.GetPopularTv, page) { tvPage(page) }
    override suspend fun getTopRatedTv(page: Int) = respond(TvRemoteFunction.GetTopRatedTv, page) { tvPage(page) }
    override suspend fun getOnTheAirTv(page: Int) = respond(TvRemoteFunction.GetOnTheAirTv, page) { tvPage(page) }
    override suspend fun getAiringTodayTv(page: Int) = respond(TvRemoteFunction.GetAiringTodayTv, page) { tvPage(page) }

    override suspend fun getTvDetails(tvShowId: Int) =
        respond(TvRemoteFunction.GetTvDetails, tvShowId) { FakeTvData.DEFAULT_TV_SHOW_DETAILS.copy(id = tvShowId) }

    override suspend fun getTvCredits(tvShowId: Int) =
        respond(TvRemoteFunction.GetTvCredits, tvShowId) { FakeTvData.DEFAULT_TV_CREDITS.copy(id = tvShowId) }

    override suspend fun getSeasonDetails(tvShowId: Int, seasonNumber: Int) =
        respond(TvRemoteFunction.GetSeasonDetails, tvShowId, seasonNumber) {
            FakeTvData.DEFAULT_SEASON_DETAILS.copy(seasonNumber = seasonNumber)
        }

    override suspend fun getEpisodeDetails(tvShowId: Int, seasonNumber: Int, episodeNumber: Int) =
        respond(TvRemoteFunction.GetEpisodeDetails, tvShowId, seasonNumber, episodeNumber) {
            FakeTvData.DEFAULT_EPISODE_DETAILS.copy(seasonNumber = seasonNumber, episodeNumber = episodeNumber)
        }

    override suspend fun searchTv(query: String, page: Int) =
        respond(TvRemoteFunction.SearchTv, query, page) { tvPage(page) }

    override suspend fun getTvAccountStates(tvShowId: Int) =
        respond(TvRemoteFunction.GetTvAccountStates, tvShowId) { accountStates.copy(id = tvShowId) }

    override suspend fun getAccountFavoriteTv(page: Int) =
        respond(TvRemoteFunction.GetAccountFavoriteTv, page) { tvPage(page) }

    override suspend fun addTvToFavorites(tvShowId: Int) = respond(TvRemoteFunction.AddTvToFavorites, tvShowId) { ok }
    override suspend fun removeTvFromFavorites(tvShowId: Int) = respond(TvRemoteFunction.RemoveTvFromFavorites, tvShowId) { ok }

    override suspend fun getAccountWatchlistTv(page: Int) =
        respond(TvRemoteFunction.GetAccountWatchlistTv, page) { tvPage(page) }

    override suspend fun addTvToAccountWatchlist(tvShowId: Int) =
        respond(TvRemoteFunction.AddTvToAccountWatchlist, tvShowId) { ok }

    override suspend fun removeTvFromAccountWatchlist(tvShowId: Int) =
        respond(TvRemoteFunction.RemoveTvFromAccountWatchlist, tvShowId) { ok }
}
