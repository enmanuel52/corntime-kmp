import Foundation
import Shared

extension UiError {
    /// Localized text for a shared failure category (strings live in the String Catalog).
    var message: String {
        switch self {
        case .unauthorized:
            return String(localized: "Unauthorized")
        case .noInternet:
            return String(localized: "No internet connection")
        case .timeout:
            return String(localized: "The request took too long")
        case .server:
            return String(localized: "Server error")
        case .unknown:
            return String(localized: "Unknown error")
        }
    }
}

extension MovieSection {
    var title: String {
        switch self {
        case .popular: return String(localized: "Popular")
        case .topRated: return String(localized: "Top Rated")
        case .nowPlaying: return String(localized: "Now Playing")
        case .upcoming: return String(localized: "Upcoming")
        }
    }
}
