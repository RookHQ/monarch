import SwiftUI

/// One feature of a plan card: a mark, the label and an optional hint.
struct MonarchPlanCardFeatureRow: View {
    let feature: MonarchPlanCardFeature
    let notIncludedLabel: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let roles = MonarchPlanCardTokens.featureRoles(isIncluded: feature.isIncluded)
        HStack(alignment: .firstTextBaseline, spacing: MonarchPlanCardTokens.markSpacing) {
            MonarchIcon(systemName: MonarchPlanCardTokens.markSymbol(isIncluded: feature.isIncluded), size: .sm)
                .foregroundStyle(theme.colors[role: roles.mark])
            text
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: roles.text])
                .multilineTextAlignment(.leading)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchPlanCardTokens.featureAccessibilityLabel(feature, notIncludedLabel: notIncludedLabel)
        )
    }

    private var text: Text {
        guard let hint = feature.hint, !hint.isEmpty else { return Text(feature.label) }
        return Text(feature.label) + Text(" \(hint)").foregroundStyle(theme.colors.textMuted)
    }
}
