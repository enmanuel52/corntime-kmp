import Shared
import SwiftUI

/// App-wide state from the shared `AppViewModel`: the theme, and the first-run walkthrough over the app.
struct RootView: View {
    @State private var model = ScreenModels.app()

    private var showOnboarding: Binding<Bool> {
        Binding(
            get: { model.state.isOnboarding },
            // finishing is the only way out; swiping the cover away must not skip it silently
            set: { _ in }
        )
    }

    var body: some View {
        let state = model.state

        Group {
            if state.isLoading {
                // preferences are read in a moment; no flash of the default theme
                Color(.systemBackground)
            } else {
                ContentView()
                    .fullScreenCover(isPresented: showOnboarding) {
                        OnboardingScreen {
                            model.viewModel.onAction(action: AppActionOnFinishOnboarding.shared)
                        }
                    }
            }
        }
        .preferredColorScheme(state.darkTheme.colorScheme)
    }
}

extension DarkTheme {
    /// `nil` follows the system setting.
    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .no: return .light
        case .yes: return .dark
        }
    }
}
