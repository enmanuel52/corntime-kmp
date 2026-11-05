import Shared
import SwiftUI

struct SettingsScreen: View {
    @State private var model = ScreenModels.settings()
    let navToLogin: () -> Void

    private func send(_ action: SettingsAction) {
        model.viewModel.onAction(action: action)
    }

    private var theme: Binding<DarkTheme> {
        Binding(
            get: { model.state.darkTheme },
            set: { send(SettingsActionOnDarkThemeChange(theme: $0)) }
        )
    }

    var body: some View {
        let state = model.state

        Form {
            Section("Account") {
                if let user = state.user {
                    UserRow(user: user)
                } else {
                    Button("Log in", action: navToLogin)
                        .accessibilityIdentifier("settings.login")
                }
            }

            Section("Appearance") {
                Picker("Appearance", selection: theme) {
                    ForEach([DarkTheme.system, .no, .yes], id: \.self) {
                        Text($0.title)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
                .accessibilityIdentifier("settings.theme")
            }

            if state.user != nil {
                Section {
                    Button {
                        send(SettingsActionOnLogoutClick.shared)
                    } label: {
                        Text("Log out").foregroundColor(.red)
                    }
                    .accessibilityIdentifier("settings.logout")
                } header: {
                    Text("Danger").foregroundColor(.red.opacity(0.5))
                }
            }
        }
        .navigationTitle("Settings")
        .scrollBounceBehavior(.basedOnSize)
        .onAppear { send(SettingsActionOnRefreshAccount.shared) }
    }
}

struct UserRow: View {
    var user: UserUi
    var body: some View {
        HStack(spacing: AppDimensions.large) {
            AppImage(
                url: BASE_IMAGE_URL + user.avatarPath,
                placeholder: "personPoster"
            )
            .aspectRatio(1.0, contentMode: .fit)
            .frame(width: 60)
            .cornerRadius(50)

            Text(user.username).font(.headline)
        }
        .accessibilityIdentifier("settings.user")
    }
}
