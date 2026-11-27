import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAvailabilityToggle``.
enum MonarchAvailabilityToggleTokens {
    /// The diameter of the status mark before Dynamic Type is applied.
    static let markSize: CGFloat = MonarchTokens.Space.space2
    /// The width of the ring shown when away, as a part of the mark's diameter.
    static let ringRatio: CGFloat = 0.25
    /// The gap between the mark and the status word.
    static let markSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the status word and the explanation.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the text and the progress indicator shown while pending.
    static let pendingSpacing: CGFloat = MonarchTokens.Space.space2

    /// The colour role of the mark.
    static func mark(isAvailable: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return isAvailable ? \.successSolid : \.warningSolid
    }

    /// What VoiceOver reads after the switch's name and value: the pending
    /// word, then the explanation, joined as two sentences. Empty when there
    /// is neither.
    static func accessibilityHint(hint: String?, pending: String?) -> String {
        [pending, hint]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ". ")
    }
}
