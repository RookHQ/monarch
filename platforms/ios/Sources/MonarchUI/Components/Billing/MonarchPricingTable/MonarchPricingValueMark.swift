import SwiftUI

/// A check, a dash or text for one plan and feature. Decorative: the cell
/// or row around it carries the spoken value.
struct MonarchPricingValueMark: View {
    let value: MonarchPricingFeatureValue

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Group {
            if let symbol = MonarchPricingTableTokens.symbol(value) {
                MonarchIcon(systemName: symbol, size: .sm)
                    .foregroundStyle(theme.colors[role: MonarchPricingTableTokens.markRole(value)])
            } else if case .text(let text) = value {
                Text(text)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .multilineTextAlignment(.leading)
            }
        }
        .accessibilityHidden(true)
    }
}
