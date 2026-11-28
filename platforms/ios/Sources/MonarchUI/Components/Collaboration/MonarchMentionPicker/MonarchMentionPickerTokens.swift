import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchMentionPicker``.
enum MonarchMentionPickerTokens {
    /// The gap between the avatar and the text of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding before and after the content of a row.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content of a row.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The size of the avatars.
    static let avatarSize: MonarchAvatarSize = .md
    /// The SF Symbol of a channel.
    static let channelSymbol = "number"
    /// The number of rows shown before the list scrolls; half a row more
    /// is shown so it is clear that it scrolls.
    static let visibleRows: CGFloat = 4.5
    /// The height of one row before Dynamic Type, for the scrolling limit.
    static let rowHeight: CGFloat = MonarchTokens.Space.space12
    /// The number of placeholder rows while searching.
    static let placeholderRows = 3
    /// The colours of the panel.
    static var background: MonarchColorRole { \.bgSurface }
    static var border: MonarchColorRole { \.borderSubtle }

    /// The colours of a row's label and detail.
    static func textColors(isDisabled: Bool) -> (label: MonarchColorRole, detail: MonarchColorRole) {
        let disabled: MonarchColorRole = \.textDisabled
        if isDisabled { return (disabled, disabled) }
        let label: MonarchColorRole = \.textDefault
        let detail: MonarchColorRole = \.textMuted
        return (label, detail)
    }

    /// The items in groups: one group, or people then channels when both are present.
    static func groups(_ items: [MonarchMentionItem]) -> [(kind: MonarchMentionKind, items: [MonarchMentionItem])] {
        let people = items.filter { $0.kind == .person }
        let channels = items.filter { $0.kind == .channel }
        if people.isEmpty || channels.isEmpty {
            return [(items.first?.kind ?? .person, items)]
        }
        return [(.person, people), (.channel, channels)]
    }

    /// The heading of a group, or `nil` when there is only one group.
    static func heading(_ kind: MonarchMentionKind, groupCount: Int, labels: MonarchMentionPickerLabels) -> String? {
        guard groupCount > 1 else { return nil }
        switch kind {
        case .person: return labels.people ?? MonarchMentionPickerStrings.people
        case .channel: return labels.channels ?? MonarchMentionPickerStrings.channels
        }
    }

    /// What is announced for a state of the search.
    static func announcement(
        status: MonarchMentionPickerStatus,
        count: Int,
        labels: MonarchMentionPickerLabels
    ) -> String {
        switch status {
        case .loading: labels.loading ?? MonarchMentionPickerStrings.loading
        case .failed: labels.error ?? MonarchMentionPickerStrings.error
        case .ready:
            count == 0
                ? labels.empty ?? MonarchMentionPickerStrings.empty
                : labels.count?(count) ?? MonarchMentionPickerStrings.count(count)
        }
    }

    /// Whether the list scrolls in a fixed height.
    static func scrolls(itemCount: Int) -> Bool {
        CGFloat(itemCount) > visibleRows
    }
}
