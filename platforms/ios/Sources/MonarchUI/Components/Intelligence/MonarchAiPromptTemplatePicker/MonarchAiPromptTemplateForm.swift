import SwiftUI

/// The page of one template in ``MonarchAiPromptTemplatePicker``: its
/// description and body, one field per blank, a live preview and the
/// "Use template" button.
struct MonarchAiPromptTemplateForm: View {
    let template: MonarchAiPromptTemplate
    let labels: MonarchAiPromptTemplatePickerLabels
    let onUse: @MainActor (_ text: String, _ template: MonarchAiPromptTemplate, _ values: [String: String]) -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var values: [String: String]
    @State private var showsErrors = false

    private typealias Tokens = MonarchAiPromptTemplatePickerTokens

    init(
        template: MonarchAiPromptTemplate,
        labels: MonarchAiPromptTemplatePickerLabels,
        onUse:
            @escaping @MainActor (_ text: String, _ template: MonarchAiPromptTemplate, _ values: [String: String]) ->
            Void
    ) {
        self.template = template
        self.labels = labels
        self.onUse = onUse
        _values = State(initialValue: Tokens.initialValues(template.resolvedVariables))
    }

    var body: some View {
        let variables = template.resolvedVariables
        let missing = showsErrors ? Tokens.missing(variables, values: values) : []
        ScrollView {
            VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
                if let description = template.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if variables.isEmpty {
                    block(labels.template, text: template.body)
                } else {
                    VStack(alignment: .leading, spacing: Tokens.spacing) {
                        ForEach(variables) { variable in
                            field(
                                variable,
                                error: missing.contains(variable.name) ? labels.missing(variable.label) : nil
                            )
                        }
                    }
                    block(labels.preview, text: Tokens.preview(template.body, variables: variables, values: values))
                }
                MonarchButton(labels.use, variant: .primary, fullWidth: true) { use(variables) }
            }
            .padding(Tokens.pagePadding)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .navigationTitle(template.title)
    }

    private func block(_ heading: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            Text(heading)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            Text(text)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .textSelection(.enabled)
                .fixedSize(horizontal: false, vertical: true)
                .padding(Tokens.blockPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: Tokens.blockRadius, style: .continuous)
                        .fill(theme.colors.bgSubtle)
                )
        }
    }

    @ViewBuilder private func field(_ variable: MonarchAiPromptTemplateVariable, error: String?) -> some View {
        let binding = Binding(
            get: { values[variable.name] ?? "" },
            set: { values[variable.name] = $0 }
        )
        let label = variable.isRequired ? variable.label : variable.label + " (" + labels.optional + ")"
        if variable.isMultiline {
            MonarchAiPromptTemplateMultilineField(
                label: label,
                spokenLabel: Tokens.fieldLabel(variable, optional: labels.optional),
                text: binding,
                prompt: variable.placeholder,
                helper: variable.helper,
                error: error
            )
        } else {
            MonarchTextField(label, text: binding, prompt: variable.placeholder, helper: variable.helper, error: error)
        }
    }

    private func use(_ variables: [MonarchAiPromptTemplateVariable]) {
        let missing = Tokens.missing(variables, values: values)
        guard missing.isEmpty else {
            showsErrors = true
            if let first = variables.first(where: { missing.contains($0.name) }) {
                AccessibilityNotification.Announcement(labels.missing(first.label)).post()
            }
            return
        }
        let submitted = Tokens.submitted(variables, values: values)
        onUse(MonarchAiPromptTemplate.fill(template.body, values: submitted), template, submitted)
    }
}
