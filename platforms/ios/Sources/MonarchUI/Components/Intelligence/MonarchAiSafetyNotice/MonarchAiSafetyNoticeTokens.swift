import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiSafetyNotice``.
enum MonarchAiSafetyNoticeTokens {
    /// The gap between the title, the message, the reason and the next steps.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the text and the actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the edit button and the link.
    static let actionGap: CGFloat = MonarchTokens.Space.space3
    /// The SF Symbol on the edit prompt button.
    static let editSymbol = "pencil"

    /// The tone of a kind: a warning when no answer was given, information
    /// when an answer is still shown.
    static func tone(_ kind: MonarchAiSafetyNoticeKind) -> MonarchTone {
        kind == .blocked ? .warning : .info
    }

    /// The SF Symbol of a kind: a lock, a pencil or a filter, so the kind does
    /// not depend on colour.
    static func symbol(_ kind: MonarchAiSafetyNoticeKind) -> String {
        switch kind {
        case .blocked: "lock.fill"
        case .modified: "pencil"
        case .filtered: "line.3.horizontal.decrease.circle"
        }
    }

    /// The colour roles of a kind, from the banner of its tone.
    static func appearance(_ kind: MonarchAiSafetyNoticeKind) -> MonarchBannerTokens.Appearance {
        MonarchBannerTokens.appearance(tone: tone(kind))
    }

    /// The link to the guidelines when it is safe to open: `http`, `https`
    /// or `mailto` only. Any other scheme (`javascript:`, `file:`, a custom
    /// app scheme) is dropped, as on the web.
    static func safeURL(_ url: URL?) -> URL? {
        url.flatMap(MonarchSafeURL.url)
    }

    /// The text shown for an optional value: `nil` when it is empty or only spaces.
    static func nonEmpty(_ text: String?) -> String? {
        guard let text, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return text
    }

    /// What VoiceOver reads for the text of the notice, in reading order:
    /// the title, the message, the reason with its label, then the next steps.
    static func accessibilityLabel(
        title: String,
        message: String,
        reason: String?,
        nextSteps: String?,
        reasonLabel: String
    ) -> String {
        var parts = [title, message]
        if let reason = nonEmpty(reason) { parts.append(reasonLabel + " " + reason) }
        if let nextSteps = nonEmpty(nextSteps) { parts.append(nextSteps) }
        return parts.joined(separator: ", ")
    }
}
