import Shared
import SwiftUI

extension View {
    /// Error alert for a shared `UiError?`; "Retry" is optional and only shown when a handler is given.
    func errorAlert(
        _ error: UiError?,
        onDismiss: @escaping () -> Void,
        onRetry: (() -> Void)? = nil,
        message: @escaping (UiError) -> String = { $0.message }
    ) -> some View {
        alert(
            "Error",
            isPresented: Binding(
                get: { error != nil },
                set: { if !$0 { onDismiss() } }
            )
        ) {
            Button("Accept", role: .cancel, action: onDismiss)
            if let onRetry {
                Button("Retry", action: onRetry)
            }
        } message: {
            if let error {
                Text(message(error))
            }
        }
    }
}
