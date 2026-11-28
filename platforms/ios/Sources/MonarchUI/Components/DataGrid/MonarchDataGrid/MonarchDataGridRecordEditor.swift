import SwiftUI

/// The record editor of ``MonarchDataGrid`` in the list layout: a sheet with
/// a `Form` of the record's fields, editable ones as ``MonarchEditableCell``,
/// the others as `LabeledContent`, and Open details when rows open.
struct MonarchDataGridRecordEditor<Row: Identifiable>: View {
    let row: Row
    let name: String
    let columns: [MonarchDataGridColumn<Row>]
    let labels: MonarchDataGridLabels
    let onOpen: (@MainActor (Row) -> Void)?

    @Environment(\.dismiss) private var dismiss
    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchDataGridTokens

    var body: some View {
        NavigationStack {
            Form {
                Group {
                    ForEach(columns) { column in
                        field(column)
                    }
                    if let onOpen {
                        Button(labels.open ?? MonarchDataGridStrings.open) {
                            dismiss()
                            onOpen(row)
                        }
                    }

                }
                .monarchRowsOnSurface()
            }
            .monarchCanvas()
            .navigationTitle(name.isEmpty ? labels.editRecord ?? MonarchDataGridStrings.editRecord : name)
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(labels.done ?? MonarchDataGridStrings.done) { dismiss() }
                }
            }
        }
    }

    @ViewBuilder private func field(_ column: MonarchDataGridColumn<Row>) -> some View {
        if let edit = column.edit, edit.canEdit(row) {
            LabeledContent {
                MonarchDataGridEditableCell(
                    row: row,
                    edit: edit,
                    label: Tokens.editLabel(column: column.title, row: name, labels: labels),
                    isNumeric: column.isNumeric,
                    labels: labels.editableCell
                )
            } label: {
                Text(column.title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: Tokens.headerText])
            }
        } else {
            let value = column.value(row)
            LabeledContent {
                Text(Tokens.cellText(value))
                    .monarchTextStyle(.body)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: value.isEmpty ? Tokens.muted : Tokens.text])
            } label: {
                Text(column.title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: Tokens.headerText])
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.cellLabel(column: column.title, value: value, labels: labels))
        }
    }
}
