import SwiftUI

/// One row of the log of ``MonarchAiComputerView``: a button that selects
/// the action, so its point is marked on the screenshot.
struct MonarchAiComputerViewRow: View {
    let action: MonarchAiComputerAction
    let isSelected: Bool
    let showsTypedText: Bool
    let time: String
    let labels: MonarchAiComputerViewLabels
    let onSelect: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiComputerViewTokens

    var body: some View {
        let appearance = Tokens.rowAppearance(isSelected: isSelected)
        let detail = Tokens.detail(action, showsTypedText: showsTypedText)
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        Button(action: onSelect) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.innerSpacing) {
                MonarchIcon(systemName: Tokens.symbol(action.kind), size: .sm)
                    .foregroundStyle(theme.colors[role: appearance.icon])
                VStack(alignment: .leading, spacing: Tokens.rowLineSpacing) {
                    Text(Tokens.actionLabel(action, labels: labels))
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                    if let text = detail.text {
                        Text(text)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    } else if detail.isHidden {
                        Text(labels.hidden ?? MonarchAiComputerViewStrings.hidden)
                            .monarchTextStyle(.caption)
                            .italic()
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(time)
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textMuted)
                if isSelected {
                    MonarchIcon(systemName: Tokens.selectedSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: appearance.icon])
                }
            }
            .multilineTextAlignment(.leading)
            .padding(Tokens.rowPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
            .background(shape.fill(theme.colors[role: appearance.background]))
            .overlay(shape.strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchShapes.borderWidth))
            .contentShape(shape)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.rowLabel(action, labels: labels))
        .accessibilityValue(Tokens.rowValue(action, showsTypedText: showsTypedText, time: time, labels: labels))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}
