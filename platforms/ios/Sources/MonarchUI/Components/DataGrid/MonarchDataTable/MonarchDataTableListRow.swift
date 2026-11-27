import SwiftUI

/// One record of ``MonarchDataTable`` in the list layout: an optional
/// checkbox, the row's name, a labelled line per other column, and the
/// actions menu. The row also offers its actions as swipe actions, a context
/// menu and VoiceOver actions.
struct MonarchDataTableListRow<Row>: View {
    let row: Row
    let name: String
    let columns: [MonarchDataTableColumn<Row>]
    let isSelected: Binding<Bool>?
    let actions: [MonarchDataTableRowAction]
    let labels: MonarchDataTableLabels
    let onOpen: (@MainActor (Row) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private typealias Tokens = MonarchDataTableTokens

    var body: some View {
        HStack(alignment: .top, spacing: Tokens.spacing) {
            if let isSelected {
                let selectLabel = labels.selectRow?(name) ?? MonarchDataTableStrings.selectRow(name)
                Toggle(isOn: isSelected) { Text(selectLabel) }
                    .toggleStyle(.monarchCheckbox)
                    .labelsHidden()
                    .accessibilityLabel(selectLabel)
            }
            summary
            if !actions.isEmpty {
                MonarchRowActionsMenu(
                    actions: actions,
                    label: labels.rowActions?(name) ?? MonarchDataTableStrings.rowActions(name)
                )
            }
        }
        .contextMenu {
            if !actions.isEmpty {
                MonarchRowActionButtons(actions: actions)
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            MonarchRowActionButtons(actions: actions)
        }
    }

    @ViewBuilder private var summary: some View {
        let label = Tokens.rowSummary(row, name: name, columns: columns, labels: labels)
        if let onOpen {
            Button {
                onOpen(row)
            } label: {
                fields
                    .contentShape(Rectangle())
                    .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityHint(labels.openHint ?? MonarchDataTableStrings.openHint)
            .accessibilityAddTraits(.isButton)
            .accessibilityActions { actionButtons }
        } else {
            fields
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityActions { actionButtons }
        }
    }

    @ViewBuilder private var actionButtons: some View {
        ForEach(actions.filter { !$0.isDisabled }) { action in
            Button(action.title) { action.action() }
        }
    }

    private var fields: some View {
        let layout =
            dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 0))
            : AnyLayout(HStackLayout(alignment: .firstTextBaseline, spacing: Tokens.spacing))
        return VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            // The default text colour even when the row opens: the whole row
            // is the button, so the name need not look like a link.
            Text(name)
                .monarchTextStyle(.body, weight: Tokens.nameWeight)
                .foregroundStyle(theme.colors[role: Tokens.textRole])
                .fixedSize(horizontal: false, vertical: true)
            ForEach(columns.dropFirst()) { column in
                let value = column.value(row)
                layout {
                    Text(column.title)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.mutedRole])
                    Spacer(minLength: 0)
                    if !value.isEmpty, let tone = column.tone?(row) {
                        MonarchBadge(value, tone: tone, size: .sm)
                    } else {
                        Text(Tokens.cellText(value, labels: labels))
                            .monarchTextStyle(.caption)
                            .monospacedDigit()
                            .foregroundStyle(theme.colors[role: value.isEmpty ? Tokens.mutedRole : Tokens.textRole])
                            .multilineTextAlignment(.trailing)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
