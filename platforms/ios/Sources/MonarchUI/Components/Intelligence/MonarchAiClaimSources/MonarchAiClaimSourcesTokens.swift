import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiClaimSources``.
enum MonarchAiClaimSourcesTokens {
    /// The gap between the header, the filter and the claims.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between claims, around the divider.
    static let claimSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of an open claim.
    static let bodySpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a claim or a passage.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the icon and the text, and between buttons.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding inside a passage.
    static let passagePadding: CGFloat = MonarchTokens.Space.space3
    /// The width of the bar before a quotation.
    static let quoteBarWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The corner radius of a passage.
    static let passageRadius: CGFloat = MonarchTokens.Radius.md
    /// The SF Symbol of the open source button.
    static let openSymbol = "arrow.up.forward.square"
    /// The SF Symbol of the flag button.
    static let flagSymbol = "flag"
    /// The SF Symbol of the verify button.
    static let verifySymbol = "arrow.clockwise"

    /// The tone of a support level.
    static func tone(_ support: MonarchAiClaimSupport) -> MonarchTone {
        switch support {
        case .supported: .success
        case .partial: .warning
        case .unsupported: .danger
        case .conflicting: .warning
        case .unchecked: .neutral
        }
    }

    /// The SF Symbol of a support level. Each level has its own, so partial
    /// and conflicting differ without colour.
    static func symbol(_ support: MonarchAiClaimSupport) -> String {
        switch support {
        case .supported: "checkmark.circle.fill"
        case .partial: "circle.lefthalf.filled"
        case .unsupported: "xmark.circle.fill"
        case .conflicting: "exclamationmark.triangle.fill"
        case .unchecked: "questionmark.circle"
        }
    }

    /// The colour of a support level's icon.
    /// The outlined symbol of the status badge.
    static func badgeSymbol(_ support: MonarchAiClaimSupport) -> String {
        switch support {
        case .supported: "checkmark.circle"
        case .partial: "circle.lefthalf.filled"
        case .unsupported: "xmark.circle"
        case .conflicting: "exclamationmark.triangle"
        case .unchecked: "questionmark.circle"
        }
    }

    static func symbolColor(_ support: MonarchAiClaimSupport) -> MonarchColorRole {
        let tone = tone(support)
        return tone == .neutral ? \.textMuted : MonarchTonePalette.palette(for: tone).strong
    }

    /// The tone of a stance badge.
    static func tone(_ stance: MonarchAiClaimEvidenceStance) -> MonarchTone {
        stance == .supports ? .success : .danger
    }

    /// The claims the filter shows, in order.
    static func shown(_ claims: [MonarchAiClaim], filter: MonarchAiClaimSourcesFilter) -> [MonarchAiClaim] {
        filter == .all ? claims : claims.filter(\.needsChecking)
    }

    /// Whether the filter is offered: there are claims to filter.
    static func showsFilter(_ counts: MonarchAiClaimCounts) -> Bool {
        counts.total > 0
    }

    /// The source a passage points at, or `nil` when it is not in the list.
    static func source(_ evidence: MonarchAiClaimEvidence, in sources: [MonarchAiClaimSource]) -> MonarchAiClaimSource?
    {
        sources.first { $0.id == evidence.sourceID }
    }

    /// The line under a passage: kind, location and publication date, leaving out what is missing.
    static func sourceDetail(
        _ source: MonarchAiClaimSource?,
        evidence: MonarchAiClaimEvidence,
        kind: (MonarchAiSourceKind) -> String,
        published: (String) -> String,
        locale: Locale,
        calendar: Calendar
    ) -> String {
        var parts: [String] = []
        if let value = source?.kind { parts.append(kind(value)) }
        if let location = evidence.location, !location.isEmpty { parts.append(location) }
        if let date = source?.published {
            var style = Date.FormatStyle(date: .abbreviated, time: .omitted)
            style.locale = locale
            style.calendar = calendar
            style.timeZone = calendar.timeZone
            parts.append(published(date.formatted(style)))
        }
        return parts.joined(separator: " · ")
    }

    /// Whether a claim's source can be opened: there is a handler or a safe address.
    static func canOpen(_ source: MonarchAiClaimSource?, hasHandler: Bool) -> Bool {
        guard let source else { return false }
        return hasHandler || MonarchAiSourceFormat.safeURL(source.url) != nil
    }

    /// What VoiceOver reads for a claim's header: the support level and the passage count.
    static func claimValue(support: String, passages: String) -> String {
        [support, passages].joined(separator: ", ")
    }

    /// What VoiceOver reads for a passage: the stance, the quote, the source and its detail.
    static func passageLabel(stance: String?, quote: String, source: String, detail: String) -> String {
        [stance, quote, source, detail].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
