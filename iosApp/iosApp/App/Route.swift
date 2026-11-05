import Shared

enum Route: Hashable {
    case movieDetails(Int32)
    case section(MovieSection)
    case settings
    case login
}
