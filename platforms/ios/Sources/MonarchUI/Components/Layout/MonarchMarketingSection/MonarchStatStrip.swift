import SwiftUI

/// A row of figures with their labels, such as "3 Export formats".
///
/// ```swift
/// MonarchStatStrip(items: [
///     MonarchStatStripItem("3", label: "Export formats"),
///     MonarchStatStripItem("13", label: "Interface languages"),
/// ])
/// ```
///
/// Show only figures that have a source. Values are shown exactly as given.
///
/// Accessibility: each stat is one element, read as its value and its label. The stats wrap onto new rows when
/// they do not fit.
public struct MonarchStatStrip: View {
    private let items: [MonarchStatStripItem]

    @Environment(\.monarchTheme) private var theme

    /// A stat strip.
    ///
    /// - Parameter items: The stats, in reading order.
    public init(items: [MonarchStatStripItem]) {
        self.items = items
    }

    /// The content of the view.
    public var body: some View {
        MonarchFlowLayout(
            spacing: MonarchMarketingSectionTokens.sectionSpacing,
            rowSpacing: MonarchMarketingSectionTokens.gridSpacing,
            alignment: .top
        ) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                VStack(alignment: .leading, spacing: MonarchMarketingSectionTokens.statTextSpacing) {
                    Text(item.value)
                        .monarchTextStyle(MonarchMarketingSectionTokens.statValueStyle)
                        .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.titleColor])
                    Text(item.label)
                        .monarchTextStyle(.bodySmall)
                        .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.supportingColor])
                }
                .accessibilityElement(children: .combine)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
