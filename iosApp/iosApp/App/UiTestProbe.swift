import Shared
import SwiftUI

/// Hidden label that only exists in UI-test launches: how often "upcoming movies" was requested.
/// It lets tests prove a screen creates exactly one ViewModel (one request), not one per re-render.
struct UiTestProbe: View {
    var body: some View {
        if CornTimeApp.isUiTesting {
            // Re-read on a timer: the request finishes after the first render.
            TimelineView(.periodic(from: .now, by: 0.25)) { _ in
                Text("\(UiTesting.shared.movieCalls(function: "GetUpcomingMovies"))")
                    .font(.system(size: 1))
                    .opacity(0.01)
                    .accessibilityIdentifier("probe.upcomingCalls")
            }
        }
    }
}
