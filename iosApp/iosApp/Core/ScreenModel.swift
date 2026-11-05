import Observation
import Shared
import SwiftUI

/// Bridges a shared Kotlin ViewModel to SwiftUI.
///
/// - The ViewModel is created lazily, on first read of `state` or `viewModel`. SwiftUI may build a
///   view's `@State` initial value many times and keep only the first, so merely constructing a
///   `ScreenModel` must stay free: discarded copies never create a ViewModel (or start network calls).
/// - It owns the `ScopedViewModel`, so the ViewModel lives as long as the screen holding this model,
///   and its coroutines are cancelled when the screen goes away (same as popping an Android back stack entry).
/// - It mirrors the ViewModel's `StateFlow` into an observable `state` that views read directly.
@MainActor
@Observable
final class ScreenModel<VM: AnyObject, State: AnyObject> {

    private struct Core {
        let scoped: ScopedViewModel<VM>
        let initial: State
        let observation: Task<Void, Never>
    }

    @ObservationIgnored private let make: () -> ScopedViewModel<VM>
    @ObservationIgnored private let stateFlow: (VM) -> SkieSwiftStateFlow<State>
    @ObservationIgnored private var core: Core?

    /// Latest value pushed by the flow. Only set after the first body, so reading it from `body` is safe.
    private var latest: State?

    init(
        make: @escaping () -> ScopedViewModel<VM>,
        state stateFlow: @escaping (VM) -> SkieSwiftStateFlow<State>
    ) {
        self.make = make
        self.stateFlow = stateFlow
    }

    var viewModel: VM { start().scoped.viewModel }

    var state: State { latest ?? start().initial }

    private func start() -> Core {
        if let core { return core }
        let scoped = make()
        let flow = stateFlow(scoped.viewModel)
        let observation = Task { [weak self] in
            for await next in flow {
                self?.latest = next
            }
        }
        let started = Core(scoped: scoped, initial: flow.value, observation: observation)
        core = started
        return started
    }

    isolated deinit {
        core?.observation.cancel()
        core?.scoped.clear()
    }
}
