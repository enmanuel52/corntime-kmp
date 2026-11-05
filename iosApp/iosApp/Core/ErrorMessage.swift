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

extension TvShowsSection {
    var title: String {
        switch self {
        case .popular: return String(localized: "Popular")
        case .topRated: return String(localized: "Top Rated")
        case .onTheAir: return String(localized: "On The Air")
        case .airingToday: return String(localized: "Airing Today")
        }
    }
}

extension LoginFieldError {
    var message: String {
        switch self {
        case .usernameRequired: return String(localized: "Username is required")
        case .passwordRequired: return String(localized: "Password is required")
        case .passwordTooShort: return String(localized: "Password must be at least 8 characters")
        }
    }
}

extension DarkTheme {
    var title: String {
        switch self {
        case .system: return String(localized: "System")
        case .no: return String(localized: "Light")
        case .yes: return String(localized: "Dark")
        }
    }
}

extension ListFieldError {
    var message: String {
        switch self {
        case .nameRequired: return String(localized: "The list needs a name")
        case .descriptionRequired: return String(localized: "The list needs a description")
        }
    }
}
