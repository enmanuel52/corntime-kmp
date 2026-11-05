import Shared
import SwiftUI

enum AppTab {
    case movies, actors, lists, search
}

struct ContentView: View {
    /// Lists belong to the account, so that tab is only there while logged in.
    let isLoggedIn: Bool

    @State private var query = ""
    @State private var selectedTab = AppTab.movies

    @State private var navigation = [Route]()
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
                    SearchScreen(query: query) { searchNavigation.append(.movieDetails($0)) }
                        .navigationDestination(for: Route.self) { route in
                            destination(route) { searchNavigation.append($0) }
                        }
                }
                .searchable(text: $query)
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
        }
    }
}
