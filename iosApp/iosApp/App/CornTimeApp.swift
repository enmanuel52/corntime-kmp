import Shared
import SwiftUI

@main
struct CornTimeApp: App {
    /// UI tests launch with `-uiTesting`: the app then runs on in-memory fakes instead of the network.
    static let isUiTesting = ProcessInfo.processInfo.arguments.contains("-uiTesting")

    init() {
        if Self.isUiTesting {
            UiTesting.shared.start(showOnboarding: ProcessInfo.processInfo.arguments.contains("-showOnboarding"))
        } else {
            KoinInitializerKt.doInitKoin()
            // must happen before the app finishes launching
            IosSync.shared.register()
        }
    }

    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .onChange(of: scenePhase) { _, phase in
            // send whatever is still queued (removals made offline or just before the app was closed)
            if phase == .active && !Self.isUiTesting { IosSync.shared.flush() }
        }
    }
}
