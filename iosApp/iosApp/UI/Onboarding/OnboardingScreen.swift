import SwiftUI

private struct OnboardingStep {
    let symbol: String
    let title: LocalizedStringResource
    let description: LocalizedStringResource
}

private let onboardingSteps = [
    OnboardingStep(
        symbol: "movieclapper",
        title: "Explore Current and Upcoming Movies",
        description: "View a curated selection of movies currently playing, coming soon and trending"
    ),
    OnboardingStep(
        symbol: "magnifyingglass",
        title: "Search Movies by Title",
        description: "Quickly locate any movie by searching its title"
    ),
    OnboardingStep(
        symbol: "bookmark",
        title: "Create Personalized Watchlists",
        description: "Organize your favorite films into custom watchlists"
    ),
    OnboardingStep(
        symbol: "person.2",
        title: "Learn About Actors and Their Films",
        description: "Discover more about actors and the movies they have starred in"
    ),
]

/// First-run walkthrough, shown over the app until it is finished or skipped.
struct OnboardingScreen: View {
    let onFinish: () -> Void
    @State private var page = 0

    private var isLastPage: Bool { page == onboardingSteps.count - 1 }

    var body: some View {
        VStack {
            HStack {
                Spacer()
                Button("Skip", action: onFinish)
                    .accessibilityIdentifier("onboarding.skip")
            }
            .padding()

            TabView(selection: $page) {
                ForEach(onboardingSteps.indices, id: \.self) { index in
                    let step = onboardingSteps[index]
                    VStack(spacing: AppDimensions.large) {
                        Image(systemName: step.symbol)
                            .font(.system(size: 96))
                            .foregroundStyle(.tint)
                            .padding(.bottom, AppDimensions.large)

                        Text(step.title)
                            .font(.title2).bold()
                            .multilineTextAlignment(.center)

                        Text(step.description)
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding(.horizontal, AppDimensions.large)
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))

            FilledButton(label: isLastPage ? String(localized: "Get started") : String(localized: "Next")) {
                if isLastPage {
                    onFinish()
                } else {
                    withAnimation { page += 1 }
                }
            }
            .padding(.horizontal, AppDimensions.large)
            .padding(.bottom)
            .accessibilityIdentifier("onboarding.next")
        }
        .background(Color(.systemBackground))
        .accessibilityIdentifier("onboarding")
    }
}
