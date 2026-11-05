import Foundation
import Shared

extension ElapsedTime {
    /// "3 weeks ago", localized with plural rules from the String Catalog.
    var text: String {
        switch onEnum(of: self) {
        case .years(let value): return String(localized: "\(Int(value.amount)) years ago")
        case .months(let value): return String(localized: "\(Int(value.amount)) months ago")
        case .weeks(let value): return String(localized: "\(Int(value.amount)) weeks ago")
        case .days(let value): return String(localized: "\(Int(value.amount)) days ago")
        case .hours(let value): return String(localized: "\(Int(value.amount)) hours ago")
        case .minutes(let value): return String(localized: "\(Int(value.amount)) minutes ago")
        case .justNow: return String(localized: "Just now")
        }
    }
}
