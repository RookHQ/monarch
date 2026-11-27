import SwiftUI

/// Suggested ways to start a conversation, as cards in a grid or rows in a list.
///
/// ```swift
/// MonarchAiStarterPrompts(starters) { starter in
///     conversation.send(starter.prompt)
/// }
/// ```
///
/// Each starter is a standard `Button`. Choosing one calls `onSelect` with
/// the starter; send its `prompt` or place it in the input. With no starters
/// nothing is shown. Disable every starter with `disabled(_:)`, for example
/// while a reply is being generated.
///
/// Accessibility: the starters are a group named "Suggested prompts". Each
/// is a button named by its title, with the prompt as its hint, and a touch
/// target at least 44 points high. The icon is decorative. At the
/// accessibility text sizes the grid becomes one column.
public struct MonarchAiStarterPrompts: View {
    private let prompts: [MonarchAiStarterPrompt]
    private let layout: MonarchAiStarterPromptsLayout
    private let hidesPrompt: Bool
    private let label: String?
    private let onSelect: @MainActor (MonarchAiStarterPrompt) -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var columnMinWidth = MonarchAiStarterPromptsTokens.columnMinWidth

    /// Starter prompts.
    ///
    /// - Parameters:
    ///   - prompts: The starters, most useful first. Four to six work best.
    ///   - layout: Cards in columns, or full-width rows.
    ///   - hidesPrompt: Whether the prompt text under each title is hidden.
    ///   - label: Replaces the built-in spoken name "Suggested prompts".
    ///   - onSelect: Runs with the starter that was chosen.
    public init(
        _ prompts: [MonarchAiStarterPrompt],
        layout: MonarchAiStarterPromptsLayout = .grid,
        hidesPrompt: Bool = false,
        label: String? = nil,
        onSelect: @escaping @MainActor (MonarchAiStarterPrompt) -> Void
    ) {
        self.prompts = prompts
        self.layout = layout
        self.hidesPrompt = hidesPrompt
        self.label = label
        self.onSelect = onSelect
    }

    private var columns: [GridItem] {
        let spacing = MonarchAiStarterPromptsTokens.spacing(layout)
        let isSingleColumn = MonarchAiStarterPromptsTokens.isSingleColumn(
            layout: layout,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
        if isSingleColumn {
            return [GridItem(.flexible(), spacing: spacing, alignment: .top)]
        }
        return [GridItem(.adaptive(minimum: columnMinWidth), spacing: spacing, alignment: .top)]
    }

    /// The content of the view.
    public var body: some View {
        if !prompts.isEmpty {
            LazyVGrid(columns: columns, alignment: .leading, spacing: MonarchAiStarterPromptsTokens.spacing(layout)) {
                ForEach(prompts) { starter in
                    Button {
                        onSelect(starter)
                    } label: {
                        MonarchAiStarterPromptLabel(starter: starter, hidesPrompt: hidesPrompt)
                    }
                    .buttonStyle(MonarchAiStarterPromptButtonStyle(layout: layout))
                    .accessibilityLabel(starter.title)
                    .accessibilityHint(
                        MonarchAiStarterPromptsTokens.accessibilityHint(
                            prompt: starter.prompt,
                            hidesPrompt: hidesPrompt
                        )
                    )
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(label ?? MonarchAiStarterPromptsStrings.label)
        }
    }
}

/// The title row and the prompt of one starter.
private struct MonarchAiStarterPromptLabel: View {
    let starter: MonarchAiStarterPrompt
    let hidesPrompt: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchAiStarterPromptsTokens.appearance(isPressed: false, isEnabled: isEnabled)
        VStack(alignment: .leading, spacing: MonarchAiStarterPromptsTokens.textSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiStarterPromptsTokens.iconSpacing) {
                if let systemImage = starter.systemImage {
                    MonarchIcon(systemName: systemImage, size: .sm)
                        .foregroundStyle(theme.colors[role: appearance.icon])
                }
                Text(starter.title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: appearance.title])
            }
            if !hidesPrompt {
                Text(starter.prompt)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: appearance.prompt])
                    .lineLimit(MonarchAiStarterPromptsTokens.promptLineLimit)
            }
        }
        .multilineTextAlignment(.leading)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The surface of one starter: a bordered card that takes the AI tint while pressed.
private struct MonarchAiStarterPromptButtonStyle: ButtonStyle {
    let layout: MonarchAiStarterPromptsLayout

    func makeBody(configuration: Configuration) -> some View {
        MonarchAiStarterPromptButtonStyleBody(configuration: configuration, layout: layout)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchAiStarterPromptButtonStyleBody: View {
    let configuration: ButtonStyleConfiguration
    let layout: MonarchAiStarterPromptsLayout

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchAiStarterPromptsTokens.appearance(
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let shape = RoundedRectangle(cornerRadius: MonarchAiStarterPromptsTokens.cornerRadius, style: .continuous)
        configuration.label
            .padding(.horizontal, MonarchAiStarterPromptsTokens.horizontalPadding)
            .padding(.vertical, MonarchAiStarterPromptsTokens.verticalPadding(layout))
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            .background(shape.fill(theme.colors[role: appearance.background]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: appearance.border],
                    lineWidth: MonarchAiStarterPromptsTokens.borderWidth
                )
            )
            .contentShape(Rectangle())
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchAiStarterPrompts") {
        let starters = [
            MonarchAiStarterPrompt(
                id: "summarise",
                title: "Summarise a document",
                prompt: "Summarise the attached report in five bullet points.",
                systemImage: "doc.text"
            ),
            MonarchAiStarterPrompt(id: "draft", title: "Draft a reply", prompt: "Write a polite reply to this email."),
            MonarchAiStarterPrompt(
                id: "plan",
                title: "Plan my week",
                prompt: "Turn my open tasks into a plan for the week.",
                systemImage: "calendar"
            ),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiStarterPrompts(starters) { _ in }
                MonarchAiStarterPrompts(starters, layout: .list, hidesPrompt: true) { _ in }
                MonarchAiStarterPrompts(starters, layout: .list) { _ in }.disabled(true)
            }
        }
    }
#endif
