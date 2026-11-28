import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiHandoff``.
enum MonarchAiHandoffTokens {
    /// Space above and below the marker, between messages.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the side lines and the body.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The least length of a side line.
    static let lineMinWidth: CGFloat = MonarchTokens.Space.space4
    /// The thickness of the lines and the border.
    static let lineWidth: CGFloat = MonarchShapes.borderWidth
    /// The length of a dash and of a gap.
    static let dash: CGFloat = MonarchTokens.Space.space1
    /// The gap between the pictures and the text.
    static let bodySpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the pictures and the arrow.
    static let pictureSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the lines of text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// Padding before and after the body inside its border.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the body inside its border.
    static let bodyVerticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The corner radius of the body's border.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The size of the pictures.
    static let avatarSize: MonarchAvatarSize = .sm
    /// The arrow between the pictures; `forward` follows the writing direction.
    static let arrowSymbol = "arrow.forward"
    /// The weight of the sentence.
    static let sentenceWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.regular)
    /// The colour of the sentence.
    static var sentence: MonarchColorRole { \.textDefault }
    /// The colour of the reason.
    static var reason: MonarchColorRole { \.textMuted }
    /// The colour of the time.
    static var time: MonarchColorRole { \.textSubtle }

    /// The colour of the side lines and the border: AI for an agent, neutral for a person.
    static func lineRole(isPerson: Bool) -> MonarchColorRole {
        isPerson ? \.borderDefault : \.aiBorder
    }

    /// The colour of the arrow.
    static func arrowRole(isPerson: Bool) -> MonarchColorRole {
        isPerson ? \.textMuted : \.aiStrong
    }

    /// Whether the side lines are drawn. They are dropped at accessibility
    /// text sizes so the sentence keeps the width.
    static func showsLines(isAccessibilitySize: Bool) -> Bool {
        !isAccessibilitySize
    }

    /// The visible sentence: "Research agent handed off to Sam Lee".
    static func sentence(from: String, to: MonarchAiHandoffTarget, labels: MonarchAiHandoffLabels) -> String {
        if let handedOff = labels.handedOff { return handedOff(from, to.name, to.isPerson) }
        return MonarchAiHandoffStrings.handedOff(from, to.name)
    }

    /// The spoken sentence: agent names carry "(AI agent)"; a person's name does not.
    static func spokenSentence(from: String, to: MonarchAiHandoffTarget, labels: MonarchAiHandoffLabels) -> String {
        let kind = labels.agentKind ?? MonarchAiHandoffStrings.agentKind
        let fromName = MonarchAiHandoffStrings.agentName(from, kind: kind)
        let toName = to.isPerson ? to.name : MonarchAiHandoffStrings.agentName(to.name, kind: kind)
        if let handedOff = labels.handedOff { return handedOff(fromName, toName, to.isPerson) }
        return MonarchAiHandoffStrings.handedOff(fromName, toName)
    }

    /// The time, relative to `now`, in the given locale: "3 minutes ago", "yesterday".
    static func timeText(_ date: Date, now: Date, locale: Locale, calendar: Calendar) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar, allowsFuture: true)
    }

    /// What VoiceOver reads for the whole marker: the name of the marker,
    /// the sentence, the reason after its prefix, then the time.
    static func accessibilityLabel(
        label: String,
        sentence: String,
        reasonPrefix: String,
        reason: String?,
        time: String?
    ) -> String {
        var parts = [label, sentence].filter { !$0.isEmpty }
        if let reason, !reason.isEmpty {
            parts.append(reasonPrefix.isEmpty ? reason : "\(reasonPrefix) \(reason)")
        }
        if let time, !time.isEmpty { parts.append(time) }
        return parts.joined(separator: ". ")
    }
}
