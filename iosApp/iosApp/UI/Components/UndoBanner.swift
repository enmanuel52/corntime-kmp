import Shared
import SwiftUI

extension View {
    /// "Removed - Undo" for the latest removal still inside its undo window. The shared ViewModel owns the
    /// timing, so the banner stays until the removal is confirmed (or taken back) and then goes away.
    func undoBanner(
        _ undoable: PendingRemoval?,
        removed: String,
        movedToFavorites: String,
        onUndo: @escaping (Int32) -> Void
    ) -> some View {
        safeAreaInset(edge: .bottom) {
            if let undoable {
                HStack {
                    Text(undoable.kind == .moveToFavorites ? movedToFavorites : removed)
                        .font(.subheadline)
                    Spacer()
                    Button("Undo") { onUndo(undoable.id) }
                        .bold()
                        .accessibilityIdentifier("undo.button")
                }
                .padding()
                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
                .padding(.horizontal)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .accessibilityIdentifier("undo.banner")
            }
        }
        .animation(.default, value: undoable?.id)
    }
}
