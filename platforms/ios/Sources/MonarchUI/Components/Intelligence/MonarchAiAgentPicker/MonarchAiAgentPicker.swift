import MonarchTokens
import SwiftUI

/// A single choice of AI agent, each shown as a compact
/// ``MonarchAiAgentCard`` with what it is for, its capabilities and how
/// many tools it has.
///
/// ```swift
/// MonarchAiAgentPicker(
///     "Agent",
///     selection: $agentID,
///     agents: [
///         MonarchAiAgentPickerOption(
///             MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources"),
///             description: "Reads documents in this space and answers with citations.",
///             capabilities: ["Summarises", "Cites sources"]
///         ),
///     ]
/// )
/// ```
///
/// Each agent is a button over its card; tapping it chooses the agent. The
/// chosen card has the accent border and a filled check, the others an
/// empty circle, like ``MonarchRadioCardGroup``. Unavailable agents stay
/// visible with an "Unavailable" tag and cannot be chosen.
///
/// Accessibility: the group is a container named by `label` (or "Choose an
/// agent"). Each agent is one button whose label is the name and role line
/// and whose value is its state, description, capabilities and tool count;
/// the chosen one has the selected trait and its mark changes shape, not
/// only colour. Unavailable agents are disabled buttons. Every card is at
/// least 44 points high.
@_spi(Experimental) public struct MonarchAiAgentPicker: View {
    private let label: String?
    @Binding private var selection: String?
    private let agents: [MonarchAiAgentPickerOption]
    private let layout: MonarchAiAgentPickerLayout
    private let labels: MonarchAiAgentPickerLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private typealias Tokens = MonarchAiAgentPickerTokens

    /// An agent picker.
    ///
    /// - Parameters:
    ///   - label: A visible label over the agents, already localised. Without one, the group is named "Choose an agent".
    ///   - selection: The chosen agent's key (its id, or its name without an id), or `nil` for none.
    ///   - agents: The agents, in display order.
    ///   - layout: One agent per row, or a two-column grid on a regular width.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String? = nil,
        selection: Binding<String?>,
        agents: [MonarchAiAgentPickerOption],
        layout: MonarchAiAgentPickerLayout = .list,
        labels: MonarchAiAgentPickerLabels = MonarchAiAgentPickerLabels()
    ) {
        self.label = label
        _selection = selection
        self.agents = agents
        self.layout = layout
        self.labels = labels
    }

    /// The key that identifies an agent in the selection: its id, or its name when it has none.
    public nonisolated static func key(for agent: MonarchAiAgentRef) -> String {
        agent.id ?? agent.name
    }

    /// The content of the view.
    public var body: some View {
        let count = Tokens.columnCount(
            layout: layout,
            isCompactWidth: horizontalSizeClass == .compact,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            if let label, !label.isEmpty {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .accessibilityHidden(true)
            }
            if agents.isEmpty {
                Text(labels.empty ?? MonarchAiAgentPickerStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else if count == 1 {
                VStack(spacing: Tokens.optionSpacing) { options }
            } else {
                let column = GridItem(.flexible(), spacing: Tokens.optionSpacing, alignment: .top)
                LazyVGrid(columns: Array(repeating: column, count: count), spacing: Tokens.optionSpacing) {
                    options
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label ?? labels.label ?? MonarchAiAgentPickerStrings.label)
    }

    private var options: some View {
        ForEach(agents) { option in
            let isSelected = selection == option.id
            Button {
                selection = Tokens.choosing(option, current: selection)
            } label: {
                MonarchAiAgentPickerCard(option: option, labels: labels)
            }
            .buttonStyle(
                MonarchAiAgentPickerButtonStyle(
                    isSelected: isSelected,
                    unavailableText: option.isDisabled
                        ? labels.unavailable ?? MonarchAiAgentPickerStrings.unavailable : nil
                )
            )
            .disabled(option.isDisabled)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.accessibilityLabel(option, labels: labels))
            .accessibilityValue(Tokens.accessibilityValue(option, labels: labels))
            .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
        }
    }
}

/// The compact card of one agent.
private struct MonarchAiAgentPickerCard: View {
    let option: MonarchAiAgentPickerOption
    let labels: MonarchAiAgentPickerLabels

    var body: some View {
        MonarchAiAgentCard(
            option.agent,
            description: option.description,
            capabilities: option.capabilities,
            tools: option.tools,
            state: option.state,
            variant: .compact,
            labels: labels.card
        )
    }
}

/// Draws the selection mark before the card and the selection border around it for each state.
private struct MonarchAiAgentPickerButtonStyle: ButtonStyle {
    let isSelected: Bool
    /// The tag shown under an agent that cannot be chosen, or `nil`.
    let unavailableText: String?

    func makeBody(configuration: Configuration) -> some View {
        MonarchAiAgentPickerSurface(
            configuration: configuration,
            isSelected: isSelected,
            unavailableText: unavailableText
        )
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchAiAgentPickerSurface: View {
    let configuration: ButtonStyleConfiguration
    let isSelected: Bool
    let unavailableText: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchAiAgentPickerTokens.appearance(
            isSelected: isSelected,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        configuration.label
            .monarchCardOverride(
                MonarchCardOverride(
                    fill: isSelected && isEnabled ? appearance.background : nil,
                    border: isSelected && isEnabled ? appearance.border : nil,
                    borderWidth: isSelected && isEnabled ? MonarchTokens.Space.space0_5 : nil
                )
            )
            .environment(\.monarchAiAgentCardTrailingInset, MonarchAiAgentPickerTokens.markInset)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
            .overlay(alignment: .topTrailing) {
                MonarchIcon(systemName: MonarchAiAgentPickerTokens.markSymbol(isSelected: isSelected))
                    .foregroundStyle(theme.colors[role: appearance.mark])
                    .padding(MonarchTokens.Space.space3)
            }
            .opacity(isEnabled ? 1 : MonarchAiAgentPickerTokens.unavailableOpacity)
            .contentShape(Rectangle())
            .monarchAnimation(.fast, value: configuration.isPressed)
            .monarchAnimation(.fast, value: isSelected)
    }
}

#if DEBUG
    #Preview("MonarchAiAgentPicker") {
        MonarchPreviewMatrix {
            MonarchAiAgentPicker(
                "Agent",
                selection: .constant("research"),
                agents: [
                    MonarchAiAgentPickerOption(
                        MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources"),
                        description: "Reads documents in this space and answers with citations.",
                        capabilities: ["Summarises", "Cites sources"],
                        tools: [MonarchAiAgentTool("Search documents"), MonarchAiAgentTool("Read file")]
                    ),
                    MonarchAiAgentPickerOption(
                        MonarchAiAgentRef(id: "writer", name: "Writing agent", role: "Drafts text"),
                        state: .working
                    ),
                    MonarchAiAgentPickerOption(
                        MonarchAiAgentRef(id: "scheduler", name: "Scheduling agent"),
                        isDisabled: true
                    ),
                ]
            )
        }
    }
#endif
