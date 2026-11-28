import SwiftUI

/// A table inside ``MonarchProse``: a header row and data rows in columns of equal width, with a subtle line
/// under every row, as the web's article table.
///
/// ```swift
/// MonarchProseTable(
///     columns: ["Option", "Default", "Description"],
///     rows: [["size", "base", "Base font size of the content."]]
/// )
/// ```
///
/// The text takes the prose size and the theme's type. Rows are grouped for VoiceOver: the table is
/// announced with its row and column counts, and each cell is read with its column name.
public struct MonarchProseTable: View {
    private let columns: [String]
    private let rows: [[String]]

    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchProseStyle) private var style

    /// A table.
    ///
    /// - Parameters:
    ///   - columns: The column headings, already localised.
    ///   - rows: The rows. A row with fewer cells than `columns` leaves the rest empty.
    public init(columns: [String], rows: [[String]]) {
        self.columns = columns
        self.rows = rows
    }

    /// The content of the view.
    public var body: some View {
        let text: MonarchTextStyle = style.size == .lg ? .body : .bodySmall
        VStack(spacing: 0) {
            line(columns, style: .label, role: \.textMuted, isHeader: true)
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                line(row, style: text, role: \.textDefault, isHeader: false)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private func line(_ cells: [String], style: MonarchTextStyle, role: MonarchColorRole, isHeader: Bool)
        -> some View
    {
        VStack(spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: theme.spacing.space3) {
                ForEach(columns.indices, id: \.self) { index in
                    Text(index < cells.count ? cells[index] : "")
                        .monarchTextStyle(style, weight: isHeader ? .semibold : .regular)
                        .foregroundStyle(theme.colors[role: role])
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityLabel(
                            index < cells.count ? (isHeader ? cells[index] : "\(columns[index]), \(cells[index])") : ""
                        )
                        .accessibilityAddTraits(isHeader ? .isHeader : [])
                }
            }
            .padding(.vertical, theme.spacing.space2)
            Rectangle().fill(theme.colors.borderSubtle).frame(height: 1)
        }
    }
}
