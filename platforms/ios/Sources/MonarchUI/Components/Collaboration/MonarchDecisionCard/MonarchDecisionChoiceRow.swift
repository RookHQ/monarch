import SwiftUI

/// One selectable choice of a decision card: a selection glyph, a label and
/// an optional description, on a bordered surface.
///
/// iOS has no radio button, and a `Picker` cannot show a description under
/// each option outside a `List`, so the choices are buttons that carry the
/// selected trait.
struct MonarchDecisionChoiceRow: View {
    let label: String
    let description: String?
    let isSelected: Bool
    let action: @MainActor () -> Void
    /// A tile in a row of choices, as the web draws them: the centred label, no glyph.
    var isTile = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchDecisionCardTokens.choiceAppearance(isSelected: isSelected, isEnabled: isEnabled)
        let shape = MonarchShapes.control
        Button(action: action) {
            if isTile {
                Text(label)
                    .monarchTextStyle(.caption, weight: isSelected ? .semibold : .regular)
                    .foregroundStyle(theme.colors[role: appearance.label])
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, MonarchDecisionCardTokens.glyphSpacing)
                    .padding(.vertical, MonarchDecisionCardTokens.choiceVerticalPadding)
                    .frame(maxHeight: .infinity)
                    .monarchMinimumHitTarget()
                    .background(shape.fill(theme.colors[role: appearance.background]))
                    .overlay(
                        shape.strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchShapes.borderWidth)
                    )
            } else {
                HStack(alignment: .firstTextBaseline, spacing: MonarchDecisionCardTokens.glyphSpacing) {
                    MonarchIcon(systemName: MonarchDecisionCardTokens.choiceSymbol(isSelected: isSelected))
                        .foregroundStyle(theme.colors[role: appearance.glyph])
                    VStack(alignment: .leading, spacing: MonarchDecisionCardTokens.textSpacing) {
                        Text(label)
                            .monarchTextStyle(.bodyStrong)
                            .foregroundStyle(theme.colors[role: appearance.label])
                        if let description, !description.isEmpty {
                            Text(description)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors[role: appearance.description])
                        }
                    }
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, MonarchDecisionCardTokens.choiceHorizontalPadding)
                .padding(.vertical, MonarchDecisionCardTokens.choiceVerticalPadding)
                .monarchMinimumHitTarget()
                .background(shape.fill(theme.colors[role: appearance.background]))
                .overlay(
                    shape.strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchShapes.borderWidth)
                )
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            MonarchDecisionCardTokens.choiceAccessibilityLabel(label: label, description: description)
        )
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .monarchAnimation(.fast, value: isSelected)
    }
}
