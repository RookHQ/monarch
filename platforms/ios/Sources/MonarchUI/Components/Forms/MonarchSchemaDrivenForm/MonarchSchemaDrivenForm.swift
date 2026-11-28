import SwiftUI

/// A form generated from a schema: every question of `fields` rendered by ``MonarchFieldRenderer`` in a
/// system `Form`, with sections, conditional questions, validation and a Save button that shows progress
/// and errors.
///
/// ```swift
/// MonarchSchemaDrivenForm(form, values: $answers) { answers in
///     try await api.send(answers)
/// }
/// ```
///
/// A ``MonarchFormFieldType/section`` question is the header of a `Section` that lasts until the next one;
/// when its conditions do not hold, its questions are hidden with it, and hidden questions are neither
/// validated nor submitted (``MonarchSchemaDrivenFormDefaults``). Errors come from
/// ``MonarchFormFieldDefinition/problem(for:)`` and show when `validateOn` says; `errors`, such as the
/// server's, show at once. Save validates every shown question: with a problem, the form scrolls to the first
/// invalid question and moves focus to it; otherwise `onSubmit` receives the shown answers while the button
/// shows progress and the questions are disabled, and a thrown error is shown in a danger ``MonarchBanner``.
///
/// With `isDirty`, the form is an edit form: Save and, with `onDiscard`, Discard are navigation bar buttons
/// (`.confirmationAction` and `.cancellationAction`), Discard asks for confirmation, and the sheet cannot be
/// swiped away while there are unsaved changes. Put the form in a `NavigationStack`. With `isReadOnly`, the
/// answers are `LabeledContent` rows. Disable the whole form with `disabled(_:)`.
///
/// The form holds no answers: `values` is the app's. The form scrolls itself; do not put it in a
/// `ScrollView`.
///
/// Accessibility: each question keeps the label, hint, required mark and error announcement of
/// ``MonarchFieldRenderer``. Section headings are headings. After a failed Save, VoiceOver focus and
/// keyboard focus move to the first invalid question. A submit failure is announced. Save shows the
/// loading state of ``MonarchButton`` while saving, or a progress indicator named "Saving" in the
/// navigation bar.
public struct MonarchSchemaDrivenForm: View {
    private let fields: [MonarchFormFieldDefinition]
    @Binding private var values: [String: MonarchFormFieldValue]
    private let validateOn: MonarchSchemaDrivenFormValidateOn
    private let isReadOnly: Bool
    private let isDirty: Bool?
    private let errors: [String: String]
    private let labels: MonarchSchemaDrivenFormLabels
    private let onDiscard: (@MainActor () -> Void)?
    private let onSubmit: (@MainActor ([String: MonarchFormFieldValue]) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.calendar) private var calendar

    @State private var visited: Set<String> = []
    @State private var lastEdited: String?
    @State private var isSubmitted = false
    @State private var isPending = false
    @State private var failure: String?
    @State private var isConfirmingDiscard = false
    @FocusState private var focusedID: String?
    @AccessibilityFocusState private var accessibilityFocusedID: String?

    /// A form generated from a schema.
    ///
    /// - Parameters:
    ///   - fields: The questions and section headings, in order.
    ///   - values: The answers, keyed by question id. A missing key is no answer; clearing an answer removes
    ///     its key.
    ///   - validateOn: When a question's own error first shows.
    ///   - isReadOnly: Shows the answers as text instead of inputs.
    ///   - isDirty: `nil` puts the Save button after the questions; a value makes Save (and Discard) navigation
    ///     bar buttons, with Save enabled while it is `true`.
    ///   - errors: Error messages from the app, such as the server's, keyed by question id. Shown at once, in
    ///     place of the question's own error.
    ///   - labels: Replacements for the built-in strings, including the questions'.
    ///   - onSubmit: Receives the shown answers once they are valid; without it there is no Save button.
    ///     Throw to show the error's description. A trailing closure is this parameter.
    ///   - onDiscard: With `isDirty`, shows Discard, which asks for confirmation and then calls this. Label
    ///     both closures when you pass both.
    public init(
        _ fields: [MonarchFormFieldDefinition],
        values: Binding<[String: MonarchFormFieldValue]>,
        validateOn: MonarchSchemaDrivenFormValidateOn = .blur,
        isReadOnly: Bool = false,
        isDirty: Bool? = nil,
        errors: [String: String] = [:],
        labels: MonarchSchemaDrivenFormLabels = MonarchSchemaDrivenFormLabels(),
        onSubmit: (@MainActor ([String: MonarchFormFieldValue]) async throws -> Void)? = nil,
        onDiscard: (@MainActor () -> Void)? = nil
    ) {
        self.fields = fields
        _values = values
        self.validateOn = validateOn
        self.isReadOnly = isReadOnly
        self.isDirty = isDirty
        self.errors = errors
        self.labels = labels
        self.onDiscard = onDiscard
        self.onSubmit = onSubmit
    }

    private var text: MonarchSchemaDrivenFormText { MonarchSchemaDrivenFormText(labels: labels) }

    /// The content of the view.
    public var body: some View {
        let shown = MonarchSchemaDrivenFormDefaults.visibleFields(fields, values: values, calendar: calendar)
        let groups = MonarchSchemaDrivenFormTokens.groups(shown)
        if isReadOnly {
            MonarchSchemaDrivenFormReadOnly(groups: groups, values: values, text: text)
        } else {
            let problems = MonarchSchemaDrivenFormDefaults.problems(fields, values: values, calendar: calendar)
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: MonarchSchemaDrivenFormTokens.sectionGap) {
                        ForEach(Array(groups.enumerated()), id: \.element.id) { index, group in
                            if index > 0 {
                                MonarchDivider()
                            }
                            section(group, problems: problems)
                        }
                        actions(shown: shown, problems: problems, proxy: proxy)
                    }
                    .padding(.horizontal, MonarchSchemaDrivenFormTokens.pagePadding)
                    .padding(.vertical, MonarchSchemaDrivenFormTokens.sectionGap)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .scrollDismissesKeyboard(.interactively)
                .background(theme.colors[role: MonarchSchemaDrivenFormTokens.canvas])
                .onChange(of: focusedID) { old, new in
                    visited = MonarchSchemaDrivenFormTokens.visited(visited, focusFrom: old, to: new)
                }
                .toolbar {
                    if let isDirty, onSubmit != nil {
                        toolbarItems(isDirty: isDirty, shown: shown, problems: problems, proxy: proxy)
                    }
                }
                .confirmationDialog(text.discardTitle, isPresented: $isConfirmingDiscard, titleVisibility: .visible) {
                    Button(text.discardConfirm, role: .destructive) {
                        failure = nil
                        onDiscard?()
                    }
                    Button(text.keepEditing, role: .cancel) {}
                } message: {
                    Text(text.discardMessage)
                }
                .interactiveDismissDisabled(isDirty == true || isPending)
            }
        }
    }

    // MARK: Sections

    private func section(
        _ group: MonarchSchemaDrivenFormTokens.Group,
        problems: [String: MonarchFormFieldProblem]
    ) -> some View {
        VStack(alignment: .leading, spacing: MonarchSchemaDrivenFormTokens.questionGap) {
            if let heading = group.heading {
                MonarchSchemaDrivenFormHeader(field: heading)
            }
            ForEach(group.questions) { field in
                question(field, problem: problems[field.id])
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .disabled(isPending)
    }

    @ViewBuilder
    private func question(_ field: MonarchFormFieldDefinition, problem: MonarchFormFieldProblem?) -> some View {
        let isShown = MonarchSchemaDrivenFormTokens.showsProblem(
            validateOn: validateOn,
            isSubmitted: isSubmitted,
            isVisited: visited.contains(field.id)
        )
        let renderer = MonarchFieldRenderer(
            field,
            value: binding(for: field),
            problem: isShown ? problem : nil,
            error: errors[field.id],
            labels: labels.fields
        )
        .environment(\.monarchHidesConsentMark, true)
        .accessibilityFocused($accessibilityFocusedID, equals: field.id)
        .id(field.id)
        if MonarchSchemaDrivenFormTokens.isTyped(field.type) {
            renderer.focused($focusedID, equals: field.id)
        } else {
            renderer
        }
    }

    @ViewBuilder
    private func actions(
        shown: [MonarchFormFieldDefinition],
        problems: [String: MonarchFormFieldProblem],
        proxy: ScrollViewProxy
    ) -> some View {
        let hasButton = isDirty == nil && onSubmit != nil
        if failure != nil || hasButton {
            VStack(alignment: .leading, spacing: MonarchSchemaDrivenFormTokens.actionGap) {
                if let failure {
                    MonarchBanner(message: failure, tone: .danger)
                }
                if hasButton {
                    MonarchButton(text.submit, variant: .primary, isLoading: isPending) {
                        submit(shown: shown, problems: problems, proxy: proxy)
                    }
                }
            }
        }
    }

    @ToolbarContentBuilder
    private func toolbarItems(
        isDirty: Bool,
        shown: [MonarchFormFieldDefinition],
        problems: [String: MonarchFormFieldProblem],
        proxy: ScrollViewProxy
    ) -> some ToolbarContent {
        if onDiscard != nil {
            ToolbarItem(placement: .cancellationAction) {
                Button(text.discard) { isConfirmingDiscard = true }
                    .disabled(!isDirty || isPending || !isEnabled)
            }
        }
        ToolbarItem(placement: .confirmationAction) {
            if isPending {
                ProgressView().accessibilityLabel(text.saving)
            } else {
                Button(text.submit) { submit(shown: shown, problems: problems, proxy: proxy) }
                    .disabled(
                        !MonarchSchemaDrivenFormTokens.canSave(isDirty: isDirty, isEnabled: isEnabled, isPending: false)
                    )
            }
        }
    }

    // MARK: Behaviour

    private func binding(for field: MonarchFormFieldDefinition) -> Binding<MonarchFormFieldValue?> {
        Binding(
            get: { values[field.id] },
            set: { newValue in
                values[field.id] = newValue
                visited = MonarchSchemaDrivenFormTokens.visited(visited, afterEditing: field, previous: lastEdited)
                lastEdited = field.id
            }
        )
    }

    private func submit(
        shown: [MonarchFormFieldDefinition],
        problems: [String: MonarchFormFieldProblem],
        proxy: ScrollViewProxy
    ) {
        guard let onSubmit, !isPending, isEnabled else { return }
        isSubmitted = true
        failure = nil
        if let first = MonarchSchemaDrivenFormTokens.firstInvalid(shown, problems: problems) {
            proxy.scrollTo(first, anchor: .top)
            if let field = shown.first(where: { $0.id == first }), MonarchSchemaDrivenFormTokens.isTyped(field.type) {
                focusedID = first
            }
            accessibilityFocusedID = first
            return
        }
        isPending = true
        let answers = MonarchSchemaDrivenFormDefaults.submittedValues(fields, values: values, calendar: calendar)
        let fallback = text.submitFailed
        Task {
            do {
                try await onSubmit(answers)
                isPending = false
            } catch is CancellationError {
                isPending = false
            } catch {
                let message = MonarchSchemaDrivenFormTokens.failureText(error, fallback: fallback)
                isPending = false
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchSchemaDrivenForm") {
        MonarchPreviewMatrix {
            MonarchSchemaDrivenForm(
                [
                    MonarchFormFieldDefinition(id: "name", type: .shortText, label: "Full name", isRequired: true),
                    MonarchFormFieldDefinition(id: "email", type: .email, label: "Work email", isRequired: true),
                    MonarchFormFieldDefinition(
                        id: "setup",
                        type: .section,
                        label: "Your setup",
                        description: "Tell us how you work."
                    ),
                    MonarchFormFieldDefinition(id: "remote", type: .yesNo, label: "Do you work remotely?"),
                ],
                values: .constant(["name": .text("Alex Morgan"), "email": .text("alex@")]),
                errors: ["email": "This address is already registered."]
            ) { _ in }
        }
    }
#endif
