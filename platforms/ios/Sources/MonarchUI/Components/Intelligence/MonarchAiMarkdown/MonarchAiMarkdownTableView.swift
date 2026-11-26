import SwiftUI

/// A Markdown table as a `Grid` that scrolls sideways inside its own border,
/// so a wide table never widens the answer. Columns take the width of their
/// widest cell up to ``MonarchAiMarkdownTokens/maxCellWidth`` (scaled with
/// the text size), then wrap. VoiceOver reads the table's name and size, and
/// header cells have the header trait.
struct MonarchAiMarkdownTableView: View {
    let table: MonarchAiMarkdownTableContent
    let context: MonarchAiMarkdownContext
    let isMuted: Bool

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var maxCellWidth = MonarchAiMarkdownTokens.maxCellWidth

    private typealias Tokens = MonarchAiMarkdownTokens

    var body: some View {
        let rows = [table.header] + table.rows
        let columns = table.alignments.count
        let shape = RoundedRectangle(cornerRadius: Tokens.tableRadius, style: .continuous)
        ScrollView(.horizontal) {
            Grid(alignment: .topLeading, horizontalSpacing: 0, verticalSpacing: 0) {
                ForEach(rows.indices, id: \.self) { row in
                    GridRow {
                        ForEach(0..<columns, id: \.self) { column in
                            cell(column < rows[row].count ? rows[row][column] : [], row: row, column: column)
                        }
                    }
                    if row < rows.count - 1 {
                        Rectangle()
                            .fill(theme.colors[role: Tokens.Role.tableBorder])
                            .frame(height: Tokens.borderWidth)
                            .gridCellUnsizedAxes(.horizontal)
                            .accessibilityHidden(true)
                    }
                }
            }
        }
        .clipShape(shape)
        .overlay {
            shape.strokeBorder(theme.colors[role: Tokens.Role.tableBorder], lineWidth: Tokens.borderWidth)
                .allowsHitTesting(false)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(context.labels.table)
        .accessibilityValue(context.labels.tableSize(rows.count, columns))
    }

    private func cell(_ content: [MonarchAiMarkdownInline], row: Int, column: Int) -> some View {
        let alignment = table.alignments[column]
        let isHeader = row == 0
        return MonarchAiMarkdownCellLayout(maxWidth: maxCellWidth) {
            Text(context.renderer.attributed(content))
                .monarchTextStyle(isHeader ? .bodyStrong : .body)
                .foregroundStyle(theme.colors[role: Tokens.textRole(isMuted: isMuted)])
                .multilineTextAlignment(Tokens.textAlignment(alignment))
        }
        .padding(.horizontal, Tokens.cellHorizontalPadding)
        .padding(.vertical, Tokens.cellVerticalPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: Tokens.alignment(alignment))
        .background(isHeader ? theme.colors[role: Tokens.Role.tableHeader] : Color.clear)
        .accessibilityAddTraits(isHeader ? .isHeader : [])
    }
}

/// Lays out one cell at its ideal width, capped so long text wraps instead
/// of making the column wider.
struct MonarchAiMarkdownCellLayout: Layout {
    let maxWidth: CGFloat

    private func width(of subview: LayoutSubview) -> CGFloat {
        min(subview.sizeThatFits(.unspecified).width, maxWidth)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard let subview = subviews.first else { return .zero }
        return subview.sizeThatFits(ProposedViewSize(width: width(of: subview), height: nil))
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard let subview = subviews.first else { return }
        subview.place(
            at: bounds.origin,
            anchor: .topLeading,
            proposal: ProposedViewSize(width: bounds.width, height: nil)
        )
    }
}
