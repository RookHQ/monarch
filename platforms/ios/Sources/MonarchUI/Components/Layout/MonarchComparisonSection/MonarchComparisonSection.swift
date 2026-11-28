import SwiftUI

/// The product beside its alternatives: a header, a ``MonarchPricingTable`` without prices that compares them
/// feature by feature, cards that say who each option suits, a note on sources and a call to action.
///
/// ```swift
/// MonarchComparisonSection(
///     "How it compares",
///     eyebrow: "Compare",
///     columns: [
///         MonarchComparisonColumn("product", name: "Product A", badge: "This product", isHighlighted: true),
///         MonarchComparisonColumn("sheets", name: "Spreadsheets"),
///     ],
///     rows: [MonarchPricingFeature("history", label: "Change history", values: ["product": true, "sheets": "Per file"])]
/// ) {
///     MonarchButton("Try Product A", variant: .primary) { start() }
/// }
/// ```
///
/// Compare only what you can source, say when it was checked, and credit trademarks to their owners in the note.
///
/// Accessibility: the title is a heading and the table a container named by the title (or `caption`). Each cell is
/// read as the option, the feature and "Included", "Not included" or the text, so nothing depends on the check
/// or dash alone. Best-for cards are read as one element each.
public struct MonarchComparisonSection<Actions: View>: View {
    private let title: String
    private let eyebrow: String?
    private let description: String?
    private let alignment: MonarchMarketingAlignment
    private let columns: [MonarchComparisonColumn]
    private let rows: [MonarchPricingFeature]
    private let caption: String?
    private let bestFor: [MonarchComparisonBestFor]
    private let note: String?
    private let actions: Actions

    @State private var tableWidth: CGFloat = 0
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A comparison section with actions.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - eyebrow: A short label above the title.
    ///   - description: One or two sentences under the title.
    ///   - alignment: Leading (the default) or centred header text.
    ///   - columns: The product and its alternatives, in display order.
    ///   - rows: The features: a label, an optional group and a value per column id (true, false or text).
    ///   - caption: The spoken name of the table. The title by default.
    ///   - bestFor: Cards under the table that say who each option suits.
    ///   - note: Sources, the date of the check and trademark credits.
    ///   - actions: One or two buttons at the end.
    public init(
        _ title: String,
        eyebrow: String? = nil,
        description: String? = nil,
        alignment: MonarchMarketingAlignment = .leading,
        columns: [MonarchComparisonColumn],
        rows: [MonarchPricingFeature],
        caption: String? = nil,
        bestFor: [MonarchComparisonBestFor] = [],
        note: String? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.eyebrow = eyebrow
        self.description = description
        self.alignment = alignment
        self.columns = columns
        self.rows = rows
        self.caption = caption
        self.bestFor = bestFor
        self.note = note
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchComparisonSectionTokens.spacing) {
            MonarchMarketingSectionHeader(title, eyebrow: eyebrow, description: description, alignment: alignment)
            // The table keeps readable columns and scrolls sideways when the options do not fit the width, as on
            // the web; on a wide screen it fills the width.
            ScrollView(.horizontal) {
                MonarchPricingTable(
                    plans: columns.map {
                        MonarchPricingPlan($0.id, name: $0.name, badge: $0.badge, isHighlighted: $0.isHighlighted)
                    },
                    features: rows,
                    layout: .grid,
                    isPriceShown: false,
                    labels: MonarchPricingTableLabels(caption: caption ?? title)
                )
                .frame(
                    width: max(tableWidth, MonarchComparisonSectionTokens.tableWidth(columns: columns.count)),
                    alignment: .leading
                )
            }
            .scrollIndicators(.hidden)
            .onGeometryChange(for: CGFloat.self) {
                $0.size.width
            } action: {
                tableWidth = $0
            }
            if !bestFor.isEmpty {
                if MonarchComparisonSectionTokens.placesCardsInRow(
                    count: bestFor.count,
                    isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
                ) {
                    HStack(alignment: .top, spacing: MonarchComparisonSectionTokens.cardSpacing) {
                        ForEach(bestFor) { card($0) }
                    }
                    .fixedSize(horizontal: false, vertical: true)
                } else {
                    VStack(spacing: MonarchComparisonSectionTokens.cardSpacing) {
                        ForEach(bestFor) { card($0) }
                    }
                }
            }
            if let note {
                Text(note)
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors[role: MonarchComparisonSectionTokens.supportingColor])
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(MonarchComparisonSectionTokens.cardPadding)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        theme.colors[role: MonarchComparisonSectionTokens.noteFill],
                        in: MonarchShapes.container
                    )
            }
            MonarchFlowLayout(
                spacing: MonarchComparisonSectionTokens.actionSpacing,
                justification: alignment == .center ? .center : .leading
            ) {
                actions
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private func card(_ item: MonarchComparisonBestFor) -> some View {
        VStack(alignment: .leading, spacing: MonarchComparisonSectionTokens.cardTextSpacing) {
            Text(item.title)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            if let description = item.description {
                Text(description)
                    .monarchTextStyle(.bodySmall)
                    .foregroundStyle(theme.colors[role: MonarchComparisonSectionTokens.supportingColor])
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(MonarchComparisonSectionTokens.cardPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(
            theme.colors[role: MonarchComparisonSectionTokens.cardFill(isHighlighted: item.isHighlighted)],
            in: MonarchShapes.container
        )
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchComparisonSectionTokens.cardBorder(isHighlighted: item.isHighlighted)],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .combine)
    }
}

extension MonarchComparisonSection where Actions == EmptyView {
    /// A comparison section without actions.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - eyebrow: A short label above the title.
    ///   - description: One or two sentences under the title.
    ///   - alignment: Leading (the default) or centred header text.
    ///   - columns: The product and its alternatives, in display order.
    ///   - rows: The features: a label, an optional group and a value per column id.
    ///   - caption: The spoken name of the table. The title by default.
    ///   - bestFor: Cards under the table that say who each option suits.
    ///   - note: Sources, the date of the check and trademark credits.
    public init(
        _ title: String,
        eyebrow: String? = nil,
        description: String? = nil,
        alignment: MonarchMarketingAlignment = .leading,
        columns: [MonarchComparisonColumn],
        rows: [MonarchPricingFeature],
        caption: String? = nil,
        bestFor: [MonarchComparisonBestFor] = [],
        note: String? = nil
    ) {
        self.init(
            title,
            eyebrow: eyebrow,
            description: description,
            alignment: alignment,
            columns: columns,
            rows: rows,
            caption: caption,
            bestFor: bestFor,
            note: note
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchComparisonSection") {
        MonarchPreviewMatrix {
            MonarchComparisonSection(
                "How it compares",
                eyebrow: "Compare",
                columns: [
                    MonarchComparisonColumn("product", name: "Product A", badge: "This product", isHighlighted: true),
                    MonarchComparisonColumn("sheets", name: "Spreadsheets"),
                ],
                rows: [
                    MonarchPricingFeature(
                        "history",
                        label: "Change history",
                        values: ["product": true, "sheets": "Per file"]
                    )
                ],
                bestFor: [MonarchComparisonBestFor("product", title: "Product A suits", isHighlighted: true)]
            ) {
                MonarchButton("Try Product A", variant: .primary) {}
            }
        }
    }
#endif
