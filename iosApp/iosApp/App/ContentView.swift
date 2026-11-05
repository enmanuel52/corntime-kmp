import Shared
import SwiftUI

enum AppTab {
    case movies, actors, search
}

struct ContentView: View {
    @State private var query = ""
    @State private var selectedTab = AppTab.movies

    @State private var navigation = [Route]()
    @State private var actorsNavigation = [Route]()

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Movies", systemImage: "movieclapper", value: .movies) {
                NavigationStack(path: $navigation) {
                    MoviesScreen(
                        navToMovieSection: { navigation.append(.section($0)) },
                        navToMovieDetails: { navigation.append(.movieDetails($0)) },
                        navToSettings: { navigation.append(.settings) }
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

            Tab(value: .search, role: .search) {
                NavigationStack(path: $navigation) {
                    SearchScreen(query: query) { navigation.append(.movieDetails($0)) }
                        .navigationDestination(for: Route.self) { route in
                            destination(route) { navigation.append($0) }
                        }
                }
                .searchable(text: $query)
            }
        }
        .overlay(alignment: .topLeading) { UiTestProbe() }
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
        case .settings:
            SettingsScreen { navigate(.login) }
        case .login:
            LoginScreen(onLoggedIn: {})
        }
    }
}
