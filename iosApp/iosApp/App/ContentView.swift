import Shared
import SwiftUI

enum AppTab {
    case movies, search
}

struct ContentView: View {
    @State private var query = ""
    @State private var selectedTab = AppTab.movies

    @State private var navigation = [Route]()

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Movies", systemImage: "movieclapper", value: .movies) {
                NavigationStack(path: $navigation) {
                    MoviesScreen(
                        navToMovieSection: { navigation.append(.section($0)) },
                        navToMovieDetails: { navigation.append(.movieDetails($0)) }
                    )
                    .navigationDestination(for: Route.self) { route in
                        destination(route) { navigation.append($0) }
                    }
                }
                .toolbar(navigation.isEmpty ? .visible : .hidden, for: .tabBar)
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
            // Actor details arrive with the actor feature.
            MovieDetailsScreen(movieId: movieId, navToActorDetails: { _ in })
        case .section(let section):
            SectionScreen(section: section) { navigate(.movieDetails($0)) }
        }
    }
}
