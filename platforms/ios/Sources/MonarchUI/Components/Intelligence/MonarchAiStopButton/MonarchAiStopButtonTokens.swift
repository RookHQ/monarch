import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiStopButton``.
enum MonarchAiStopButtonTokens {
    /// One piece of the control.
    enum Part: Hashable {
        /// The Stop button.
        case stop
        /// The "Stopped." text.
        case status
        /// The Continue button.
        case resume
    }

    /// The gap between the status text and the Continue button.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the stop symbol and its title.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The SF Symbol of the Stop button: a filled square.
    static let symbol = "stop.fill"
    /// The type role of the status text.
    static let statusTextStyle: MonarchTextStyle = .label
    /// The weight of the status text.
    static let statusWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.regular)
    /// The colour role of the status text.
    static var statusColor: MonarchColorRole { \.textMuted }

    /// What is shown in a state, in reading order. Stop and the stopped
    /// state never appear together.
    static func parts(isStopped: Bool, hasContinue: Bool) -> [Part] {
        guard isStopped else { return [.stop] }
        return hasContinue ? [.status, .resume] : [.status]
    }
}
