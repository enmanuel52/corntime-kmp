import Shared
import SwiftUI

struct LoginScreen: View {
    @State private var model = ScreenModels.login()
    let onLoggedIn: () -> Void
    @Environment(\.openURL) private var openURL
    @Environment(\.dismiss) private var dismiss

    private func send(_ action: LoginAction) {
        model.viewModel.onAction(action: action)
    }

    private var username: Binding<String> {
        Binding(
            get: { model.state.username },
            set: { send(LoginActionOnUsernameChange(value: $0)) }
        )
    }

    private var password: Binding<String> {
        Binding(
            get: { model.state.password },
            set: { send(LoginActionOnPasswordChange(value: $0)) }
        )
    }

    var body: some View {
        let state = model.state

        ScrollView(showsIndicators: false) {
            if state.isLoading {
                ProgressView("Logging in")
                    .accessibilityIdentifier("login.loading")
            }

            VStack {
                VStack(spacing: AppDimensions.large) {
                    Lottie()
                        .scaledToFit()
                        .frame(maxHeight: 80)

                    Text("Log In").font(.title2).bold()

                    Text(
                        "To log in into your account in the application, enter your username and password"
                    ).textParragraph()

                    BasicField(
                        text: username,
                        label: String(localized: "Username"),
                        leadingImage: "envelope",
                        error: state.usernameError?.message
                    )
                    .accessibilityIdentifier("login.username")

                    PassworField(
                        text: password,
                        label: String(localized: "Password"),
                        leadingImage: "lock.rectangle",
                        error: state.passwordError?.message
                    )
                    .accessibilityIdentifier("login.password")

                    FilledButton(label: String(localized: "Log In")) {
                        send(LoginActionOnLoginClick.shared)
                    }
                    .disabled(
                        state.usernameError != nil || state.passwordError != nil || state.isLoading
                    )
                    .accessibilityIdentifier("login.submit")

                    CreateAccountSectionLabel()

                    FilledButton(label: String(localized: "Create an account")) {
                        openURL(URL(string: "https://www.themoviedb.org/signup")!)
                    }
                    .tint(.textFieldBackground)
                    .foregroundColor(.blue)
                    .accessibilityIdentifier("login.signup")

                    FilledButton(
                        label: String(localized: "Sign in with Apple"),
                        leadingImage: "apple.logo"
                    ) {
                        // Sign in with Apple is not supported by TMDB; the button stays as in the original design.
                    }
                    .tint(.textFieldBackground)
                    .foregroundColor(.primary)
                }
            }
            .formGroup()

            FooterText()
        }
        .formBackground()
        .scrollBounceBehavior(.basedOnSize)
        .errorAlert(
            state.error,
            onDismiss: { send(LoginActionOnDismissError.shared) },
            message: { error in
                // TMDB answers a wrong username or password with "unauthorized"
                error == .unauthorized ? String(localized: "Wrong username or password") : error.message
            }
        )
        .task {
            for await event in model.viewModel.events {
                switch onEnum(of: event) {
                case .loginSuccess:
                    onLoggedIn()
                    dismiss()
                }
            }
        }
    }
}

private struct FooterText: View {
    var body: some View {
        Text(
            """
            By clicking \"Continue\" I have read and agree
            with the [Terms and Privacy Policy](https://github.com)
            """
        )
        .textParragraph()
    }
}

private struct CreateAccountSectionLabel: View {
    var body: some View {
        HStack(spacing: AppDimensions.medium) {
            Color.secondary
                .frame(maxHeight: 1)

            Text("Don't have an account yet?")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .lineLimit(1)
                .layoutPriority(1)

            Color.secondary
                .frame(maxHeight: 1)
        }
    }
}

extension View {
    fileprivate func formGroup() -> some View {
        padding()
            .background(.formGroupBackground)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .padding()
    }

    fileprivate func formBackground() -> some View {
        frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(.formBackground)
    }

    fileprivate func textParragraph() -> some View {
        font(.subheadline)
            .foregroundColor(.secondary)
            .lineLimit(3)
            .multilineTextAlignment(.center)
    }
}
