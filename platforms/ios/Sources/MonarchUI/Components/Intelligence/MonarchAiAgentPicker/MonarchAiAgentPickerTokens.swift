import MonarchTokens
import SwiftUI

/// Layout tokens and the pure logic of ``MonarchAiAgentPicker``.
enum MonarchAiAgentPickerTokens {
    /// The gap between the label and the agents.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between agents.
    static let optionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the selection mark and the card.
    static let markSpacing: CGFloat = MonarchTokens.Space.space3
    /// The columns of the grid layout on a regular width.
    static let gridColumns = 2
    /// The SF Symbol of the chosen agent's mark.
    static let selectedSymbol = "checkmark.circle.fill"
    /// The SF Symbol of an agent that is not chosen.
    static let unselectedSymbol = "circle"

    /// The number of columns: the grid's two on a regular width, otherwise one.
    static func columnCount(
        layout: MonarchAiAgentPickerLayout,
        isCompactWidth: Bool,
        isAccessibilitySize: Bool
    ) -> Int {
        guard layout == .grid else { return 1 }
        return MonarchRadioCardGroupTokens.columnCount(
            requested: gridColumns,
            isCompactWidth: isCompactWidth,
            isAccessibilitySize: isAccessibilitySize
        )
    }

    /// The border and mark roles of an option, the same as a radio card's.
    static func appearance(isSelected: Bool, isPressed: Bool, isEnabled: Bool) -> MonarchRadioCardGroupTokens.Appearance
    {
        let base = MonarchRadioCardGroupTokens.appearance(
            isSelected: isSelected,
            isPressed: isPressed,
            isEnabled: isEnabled
        )
        guard isEnabled, isSelected else { return base }
        // The chosen agent is drawn in the AI accent, as on the web: tinted card, solid border, solid mark.
        return MonarchRadioCardGroupTokens.Appearance(
            background: isPressed ? \.aiSubtleHover : \.aiSubtle,
            border: \.aiSolid,
            title: base.title,
            secondary: base.secondary,
            icon: \.aiStrong,
            mark: \.aiSolid
        )
    }

    /// The room the mark takes at the end of a card's first line.
    static let markInset: CGFloat = MonarchTokens.Space.space8
    /// How faded a card is that cannot be chosen.
    static let unavailableOpacity: Double = 0.5

    /// The selection mark's symbol: a filled check when chosen, an empty circle otherwise.
    static func markSymbol(isSelected: Bool) -> String {
        isSelected ? selectedSymbol : unselectedSymbol
    }

    /// What VoiceOver reads as an option's label: the name and the role line.
    static func accessibilityLabel(_ option: MonarchAiAgentPickerOption, labels: MonarchAiAgentPickerLabels) -> String {
        [option.agent.name, MonarchAiAgentCardTokens.subtitle(option.agent, labels: labels.card)]
            .joined(separator: ", ")
    }

    /// What VoiceOver reads as an option's value: the state when the agent is
    /// busy, the description, the capabilities, the tool count, and
    /// "Unavailable" when it cannot be chosen.
    static func accessibilityValue(_ option: MonarchAiAgentPickerOption, labels: MonarchAiAgentPickerLabels) -> String {
        var parts: [String] = []
        if option.state != .idle { parts.append(MonarchAiAgentAvatarStrings.state(option.state)) }
        if let description = option.description, !description.isEmpty { parts.append(description) }
        parts.append(contentsOf: option.capabilities.filter { !$0.isEmpty })
        if let count = MonarchAiAgentCardTokens.toolCount(option.tools.count, variant: .compact, labels: labels.card) {
            parts.append(count)
        }
        if option.isDisabled { parts.append(labels.unavailable ?? MonarchAiAgentPickerStrings.unavailable) }
        return parts.joined(separator: ", ")
    }

    /// The selection after an option is tapped: a disabled agent changes nothing.
    static func choosing(_ option: MonarchAiAgentPickerOption, current: String?) -> String? {
        option.isDisabled ? current : option.id
    }
}
