import Shared
import SwiftUI

enum AppTab {
    case movies, tvShows, actors, lists, search
}

struct ContentView: View {
    /// Lists belong to the account, so that tab is only there while logged in.
    let isLoggedIn: Bool

    @State private var query = ""
    @State private var searchScope = SearchScopeKind.movies
    @State private var selectedTab = AppTab.movies

    @State private var navigation = [Route]()
    @State private var tvNavigation = [Route]()
    @State private var actorsNavigation = [Route]()
    @State private var listsNavigation = [Route]()
    @State private var searchNavigation = [Route]()

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Movies", systemImage: "movieclapper", value: .movies) {
                NavigationStack(path: $navigation) {
                    MoviesScreen(
                        navToMovieSection: { navigation.append(.section($0)) },
                        navToMovieDetails: { navigation.append(.movieDetails($0)) },
                        navToSettings: { navigation.append(.settings) },
                        isLoggedIn: isLoggedIn,
                        navToFavorites: { navigation.append(.favorites) }
                    )
                    .navigationDestination(for: Route.self) { route in
                        destination(route) { navigation.append($0) }
                    }
                }
                .toolbar(navigation.isEmpty ? .visible : .hidden, for: .tabBar)
            }

            Tab("TV Shows", systemImage: "tv", value: .tvShows) {
                NavigationStack(path: $tvNavigation) {
                    TvShowsScreen(
                        navToTvShowSection: { tvNavigation.append(.tvShowSection($0)) },
                        navToTvShowDetails: { tvNavigation.append(.tvShowDetails($0)) },
                        isLoggedIn: isLoggedIn,
                        navToFavorites: { tvNavigation.append(.favoriteTvShows) }
                    )
                    .navigationDestination(for: Route.self) { route in
                        destination(route) { tvNavigation.append($0) }
                    }
                }
                .toolbar(tvNavigation.isEmpty ? .visible : .hidden, for: .tabBar)
            }

            Tab("Actors", systemImage: "person.2", value: .actors) {
                NavigationStack(path: $actorsNavigation) {
                    ActorsScreen { actorsNavigation.append(.actorDetails($0, name: $1)) }
                        .navigationDestination(for: Route.self) { route in
                            destination(route) { actorsNavigation.append($0) }
                        }
                }
                .toolbar(actorsNavigation.isEmpty ? .visible : .hidden, for: .tabBar)
            }

            if isLoggedIn {
                Tab("Lists", systemImage: "bookmark", value: .lists) {
                    NavigationStack(path: $listsNavigation) {
                        WatchlistScreen(
                            navToMovieDetails: { listsNavigation.append(.movieDetails($0)) },
                            navToTvShowDetails: { listsNavigation.append(.tvShowDetails($0)) },
                            navToLists: { listsNavigation.append(.createdLists) }
                        )
                        .navigationDestination(for: Route.self) { route in
                            destination(route) { listsNavigation.append($0) }
                        }
                    }
                    .toolbar(listsNavigation.isEmpty ? .visible : .hidden, for: .tabBar)
                }
            }

            Tab(value: .search, role: .search) {
                NavigationStack(path: $searchNavigation) {
                    SearchScreen(
                        query: query,
                        scope: searchScope,
                        navToMovieDetails: { searchNavigation.append(.movieDetails($0)) },
                        navToTvShowDetails: { searchNavigation.append(.tvShowDetails($0)) }
                    )
                        .navigationDestination(for: Route.self) { route in
                            destination(route) { searchNavigation.append($0) }
                        }
                }
                .searchable(text: $query)
                .searchScopes($searchScope) {
                    Text("Movies").tag(SearchScopeKind.movies)
                    Text("TV Shows").tag(SearchScopeKind.tvShows)
                }
                .toolbar(searchNavigation.isEmpty ? .visible : .hidden, for: .tabBar)
            }
        }
        .overlay(alignment: .topLeading) { UiTestProbe() }
        .onChange(of: isLoggedIn) { _, loggedIn in
            // logging out while on a tab that needs the account would leave nothing selected
            if !loggedIn && selectedTab == .lists { selectedTab = .movies }
            if !loggedIn { listsNavigation = [] }
        }
    }

    @ViewBuilder
    private func destination(_ route: Route, navigate: @escaping (Route) -> Void) -> some View {
        switch route {
        case .movieDetails(let movieId):
            MovieDetailsScreen(movieId: movieId) { navigate(.actorDetails($0, name: $1)) }
        case .actorDetails(let actorId, let name):
            ActorDetailsScreen(actorId: actorId, name: name) { navigate(.movieDetails($0)) }
        case .section(let section):
            SectionScreen(section: section) { navigate(.movieDetails($0)) }
        case .favorites:
            FavoriteMoviesScreen { navigate(.movieDetails($0)) }
        case .createdLists:
            CreatedListsScreen { navigate(.listDetails($0, name: $1)) }
        case .listDetails(let listId, let name):
            ListDetailsScreen(listId: listId, name: name) { navigate(.movieDetails($0)) }
        case .settings:
            SettingsScreen { navigate(.login) }
        case .login:
            LoginScreen(onLoggedIn: {})
        case .tvShowDetails(let tvShowId):
            TvShowDetailsScreen(
                tvShowId: tvShowId,
                navToSeasons: { navigate(.seasons(tvShowId: tvShowId)) },
                navToEpisodes: { navigate(.episodes(tvShowId: tvShowId, seasonNumber: $0)) },
                navToActorDetails: { navigate(.actorDetails($0, name: $1)) }
            )
        case .tvShowSection(let section):
            TvShowSectionScreen(section: section) { navigate(.tvShowDetails($0)) }
        case .seasons(let tvShowId):
            SeasonsScreen(tvShowId: tvShowId) { navigate(.episodes(tvShowId: tvShowId, seasonNumber: $0)) }
        case .episodes(let tvShowId, let seasonNumber):
            EpisodesScreen(tvShowId: tvShowId, seasonNumber: seasonNumber) {
                navigate(.episodeDetails(tvShowId: tvShowId, seasonNumber: seasonNumber, episodeNumber: $0))
            }
        case .episodeDetails(let tvShowId, let seasonNumber, let episodeNumber):
            EpisodeDetailsScreen(
                tvShowId: tvShowId,
                seasonNumber: seasonNumber,
                episodeNumber: episodeNumber
            ) { navigate(.actorDetails($0, name: $1)) }
        case .favoriteTvShows:
            FavoriteTvShowsScreen { navigate(.tvShowDetails($0)) }
        }
    }
}
