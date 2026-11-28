import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchMarketingConsentCheckbox``.
enum MonarchMarketingConsentCheckboxTokens {
    /// The gap between the question, the notes and the checkboxes.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the lines of the notes.
    static let notesSpacing: CGFloat = MonarchTokens.Space.space1

    /// The colour of the question above several checkboxes.
    static func question(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour of the notes.
    static func notes(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The channels to show: the given ones without repeats, in order, or email when there are none.
    static func shownChannels(_ channels: [MonarchMarketingChannel]) -> [MonarchMarketingChannel] {
        var seen = Set<MonarchMarketingChannel>()
        let unique = channels.filter { seen.insert($0).inserted }
        return unique.isEmpty ? [.email] : unique
    }

    /// The question: the app's label, or the built-in one for one channel or for several.
    static func question(label: String?, channels: [MonarchMarketingChannel]) -> String {
        if let label, !label.isEmpty { return label }
        let shown = shownChannels(channels)
        if shown.count == 1, let only = shown.first {
            return MonarchMarketingConsentCheckboxStrings.question(only)
        }
        return MonarchMarketingConsentCheckboxStrings.groupQuestion
    }

    /// The label of a channel's checkbox when several are shown.
    static func channelLabel(
        _ channel: MonarchMarketingChannel,
        labels: [MonarchMarketingChannel: String]
    ) -> String {
        if let custom = labels[channel], !custom.isEmpty { return custom }
        return MonarchMarketingConsentCheckboxStrings.channel(channel)
    }

    /// The unsubscribe note to show, or `nil` when it is hidden.
    static func unsubscribeNote(shows: Bool, override: String?) -> String? {
        guard shows else { return nil }
        if let override, !override.isEmpty { return override }
        return MonarchMarketingConsentCheckboxStrings.unsubscribe
    }

    /// The exact wording on screen in reading order, whitespace collapsed:
    /// the question, the description and the unsubscribe note, the title of
    /// the policy link, and the channel labels when several are shown. This
    /// is what the consent record carries.
    static func wording(
        question: String,
        channelLabels: [String],
        description: String?,
        unsubscribeNote: String?,
        policyTitle: String?
    ) -> String {
        ([question, description, unsubscribeNote, policyTitle] + channelLabels)
            .compactMap { $0 }
            .map { $0.split(whereSeparator: \.isWhitespace).joined(separator: " ") }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
    }

    /// The record of a choice: every shown channel with its state.
    static func consent(
        shown: [MonarchMarketingChannel],
        selection: Set<MonarchMarketingChannel>,
        text: String,
        date: Date
    ) -> MonarchMarketingConsent {
        var channels: [MonarchMarketingChannel: Bool] = [:]
        for channel in shown { channels[channel] = selection.contains(channel) }
        return MonarchMarketingConsent(channels: channels, text: text, date: date)
    }

    /// The selection after a checkbox changes.
    static func selection(
        _ selection: Set<MonarchMarketingChannel>,
        setting channel: MonarchMarketingChannel,
        to isOn: Bool
    ) -> Set<MonarchMarketingChannel> {
        var next = selection
        if isOn { next.insert(channel) } else { next.remove(channel) }
        return next
    }
}
