import SwiftUI

struct AppImage: View {
    let url: String?
    let placeholder: String

    var body: some View {
        AsyncImage(url: URL(string: url ?? "")) { phase in
            switch phase {
            case .empty:
                ZStack {
                    Image(placeholder).resizable().opacity(0.1)
                    ProgressView()
                }
            case .success(let image):
                image.resizable()
            case .failure:
                Image(placeholder).resizable()
            @unknown default:
                Image(placeholder).resizable()
            }
        }
    }
}
