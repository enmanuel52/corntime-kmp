import Shared
import SwiftUI

@main
struct CornTimeApp: App {
    /// UI tests launch with `-uiTesting`: the app then runs on in-memory fakes instead of the network.
    static let isUiTesting = ProcessInfo.processInfo.arguments.contains("-uiTesting")

    init() {
        if Self.isUiTesting {
            UiTesting.shared.start()
        } else {
            KoinInitializerKt.doInitKoin()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
