import SwiftUI

/// The read-only view of a ``MonarchSchemaDrivenForm``: the shown answers as `LabeledContent` rows in the
/// same sections as the form, with the ``MonarchLabeledContentStyle``.
struct MonarchSchemaDrivenFormReadOnly: View {
    let groups: [MonarchSchemaDrivenFormTokens.Group]
    let values: [String: MonarchFormFieldValue]
    let text: MonarchSchemaDrivenFormText

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone

    var body: some View {
        Form {
            ForEach(groups) { group in
                Section {
                    ForEach(group.questions) { field in
                        row(field)
                    }
                } header: {
                    if let heading = group.heading {
                        MonarchSchemaDrivenFormHeader(field: heading)
                    }
                }
                .listRowBackground(theme.colors[role: MonarchSchemaDrivenFormTokens.row])
            }
        }
        .formStyle(.grouped)
        .scrollContentBackground(.hidden)
        .background(theme.colors[role: MonarchSchemaDrivenFormTokens.canvas])
        .labeledContentStyle(.monarch)
    }

    private func row(_ field: MonarchFormFieldDefinition) -> some View {
        let display = MonarchSchemaDrivenFormTokens.display(
            field,
            values[field.id],
            locale: locale,
            calendar: calendar,
            timeZone: timeZone,
            yes: text.yes,
            no: text.no
        )
        return LabeledContent(field.label) {
            if let display {
                Text(display)
                    .foregroundStyle(theme.colors[role: MonarchSchemaDrivenFormTokens.text])
                    .fixedSize(horizontal: false, vertical: true)
            } else {
                Text(MonarchSchemaDrivenFormTokens.emptyValue)
                    .foregroundStyle(theme.colors[role: MonarchSchemaDrivenFormTokens.muted])
                    .accessibilityLabel(text.empty)
            }
        }
    }
}

/// The header of a section: the section question's label as a heading, and its help.
struct MonarchSchemaDrivenFormHeader: View {
    let field: MonarchFormFieldDefinition

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchSchemaDrivenFormTokens.headerGap) {
            Text(field.label)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors[role: MonarchSchemaDrivenFormTokens.text])
                .accessibilityAddTraits(.isHeader)
            if let description = field.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchSchemaDrivenFormTokens.muted])
            }
        }
        .textCase(nil)
        .fixedSize(horizontal: false, vertical: true)
    }
}
