import MonarchTokens
import SwiftUI

/// The starters of a narrow assistant panel as the web draws them: small bordered chips, centred, wrapping onto
/// new rows, each with the title alone. A starter chosen runs `onSelect` like ``MonarchAiStarterPrompts``.
struct MonarchAiStarterChips: View {
    let prompts: [MonarchAiStarterPrompt]
    let label: String
    let onSelect: @MainActor (MonarchAiStarterPrompt) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        if !prompts.isEmpty {
            MonarchFlowLayout(spacing: theme.spacing.space2, justification: .center) {
                ForEach(prompts) { starter in
                    Button {
                        onSelect(starter)
                    } label: {
                        HStack(spacing: theme.spacing.space1_5) {
                            if let systemImage = starter.systemImage {
                                MonarchIcon(systemName: systemImage, size: .sm)
                                    .foregroundStyle(isEnabled ? theme.colors.aiStrong : theme.colors.textDisabled)
                            }
                            Text(starter.title)
                                .monarchTextStyle(.label)
                                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                        }
                        .padding(.horizontal, theme.spacing.space3)
                        .padding(.vertical, theme.spacing.space2)
                        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                        .background(MonarchShapes.control.fill(theme.colors.bgSurface))
                        .overlay(
                            MonarchShapes.control.strokeBorder(
                                isEnabled ? theme.colors.borderDefault : theme.colors.borderSubtle,
                                lineWidth: MonarchTokens.Space.px
                            )
                        )
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(starter.title)
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(label)
        }
    }
}
