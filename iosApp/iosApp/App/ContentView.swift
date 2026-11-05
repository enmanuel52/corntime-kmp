import Shared
import SwiftUI

enum AppTab {
    case movies, search
}

struct ContentView: View {
    @State private var query = ""
    @State private var selectedTab = AppTab.movies

    // One path per tab: tabs no longer share (and clobber) a single navigation stack.
    @State private var moviesPath = [Route]()
    @State private var searchPath = [Route]()

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Movies", systemImage: "movieclapper", value: .movies) {
                NavigationStack(path: $moviesPath) {
                    MoviesScreen(
                        navToMovieSection: { moviesPath.append(.section($0)) },
                        navToMovieDetails: { moviesPath.append(.movieDetails($0)) }
                    )
                    .navigationDestination(for: Route.self) { route in
                        destination(route) { moviesPath.append($0) }
                    }
                }
                .toolbar(moviesPath.isEmpty ? .visible : .hidden, for: .tabBar)
            }

            Tab(value: .search, role: .search) {
                NavigationStack(path: $searchPath) {
                    SearchScreen(query: query) { searchPath.append(.movieDetails($0)) }
                        .navigationDestination(for: Route.self) { route in
                            destination(route) { searchPath.append($0) }
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
