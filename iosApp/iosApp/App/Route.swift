import Shared

enum Route: Hashable {
    case movieDetails(Int32)
    case section(MovieSection)
    case actorDetails(Int32, name: String)
    case favorites
    case createdLists
    case listDetails(Int32, name: String)
    case settings
    case login
    case tvShowDetails(Int32)
    case tvShowSection(TvShowsSection)
    case seasons(tvShowId: Int32)
    case episodes(tvShowId: Int32, seasonNumber: Int32)
    case episodeDetails(tvShowId: Int32, seasonNumber: Int32, episodeNumber: Int32)
    case favoriteTvShows
}
