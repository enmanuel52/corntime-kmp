import Shared

enum Route: Hashable {
    case movieDetails(Int32)
    case section(MovieSection)
    case actorDetails(Int32, name: String)
    case settings
    case login
}
