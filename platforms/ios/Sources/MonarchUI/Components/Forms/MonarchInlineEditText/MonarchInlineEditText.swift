import SwiftUI

/// Text that turns into a field in place, with an asynchronous save.
///
/// Touching the text opens the editor. Return saves a single-line value;
/// Save and Cancel are always shown, because a touch keyboard has no Escape
/// key (a hardware Escape cancels too). While `onSave` runs the Save button
/// shows progress; if it throws, the editor stays open with the draft and
/// the error. The app owns persistence: on success the new value is written
/// to `text`.
///
/// ```swift
/// MonarchInlineEditText("Title", text: $title, prompt: "Untitled") { newTitle in
///     try await api.rename(to: newTitle)
/// }
/// ```
///
/// Accessibility: in view mode it is one button whose label is `label`,
/// whose value is the text and whose hint is "Edit". In edit mode the field
/// has the same label, and an error is its hint, spoken as "Error: …", shown
/// with an icon and announced when it appears. The text, the field and the
/// buttons have 44-point touch targets and scale with Dynamic Type.
public struct MonarchInlineEditText: View {
    private let label: String
    @Binding private var text: String
    private let prompt: String?
    private let error: String?
    private let isMultiline: Bool
    private let trimsWhitespace: Bool
    private let savesOnFocusLoss: Bool
    private let editHint: String?
    private let saveTitle: String?
    private let cancelTitle: String?
    private let savingLabel: String?
    private let failureMessage: String?
    private let validate: (@MainActor (String) -> String?)?
    private let onCancel: (@MainActor () -> Void)?
    private let onSave: @MainActor (String) async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchInlineEditTextStyle) private var textStyle
    @State private var session: MonarchInlineEditTextSession
    @FocusState private var isFocused: Bool
    @ScaledMetric(relativeTo: .body) private var editorHeight = MonarchInlineEditTextTokens.editorHeight

    /// Text that is edited in place.
    ///
    /// - Parameters:
    ///   - label: What the text is, for example "Title", already localised. Read by VoiceOver; not drawn.
    ///   - text: The saved value. Written after a successful save.
    ///   - prompt: Shown in view mode while the value is empty, and as the placeholder of the field.
    ///   - error: An error from the app, for example an edit conflict, shown while editing.
    ///   - isMultiline: Whether the editor has several lines. Return then adds a line and Save saves.
    ///   - startsEditing: Whether the editor is open when the view first appears, for example for a new item.
    ///   - trimsWhitespace: Whether to trim the draft before comparing and saving.
    ///   - savesOnFocusLoss: Whether leaving the field saves the draft.
    ///   - editHint: Replaces the built-in "Edit" hint.
    ///   - saveTitle: Replaces the built-in "Save" title.
    ///   - cancelTitle: Replaces the built-in "Cancel" title.
    ///   - savingLabel: Replaces the built-in "Saving" spoken value.
    ///   - failureMessage: Replaces the built-in message of a failed save whose error has no description.
    ///   - validate: Returns a message to refuse a draft before saving, or `nil` to allow it.
    ///   - onCancel: Runs when the person cancels.
    ///   - onSave: Persists a new value. Not called when the value is unchanged. Throw to
    ///     keep the editor open with the error.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        error: String? = nil,
        isMultiline: Bool = false,
        startsEditing: Bool = false,
        trimsWhitespace: Bool = true,
        savesOnFocusLoss: Bool = true,
        editHint: String? = nil,
        saveTitle: String? = nil,
        cancelTitle: String? = nil,
        savingLabel: String? = nil,
        failureMessage: String? = nil,
        validate: (@MainActor (String) -> String?)? = nil,
        onCancel: (@MainActor () -> Void)? = nil,
        onSave: @escaping @MainActor (String) async throws -> Void
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.error = error
        self.isMultiline = isMultiline
        self.trimsWhitespace = trimsWhitespace
        self.savesOnFocusLoss = savesOnFocusLoss
        self.editHint = editHint
        self.saveTitle = saveTitle
        self.cancelTitle = cancelTitle
        self.savingLabel = savingLabel
        self.failureMessage = failureMessage
        self.validate = validate
        self.onCancel = onCancel
        self.onSave = onSave
        let session = MonarchInlineEditTextSession()
        if startsEditing { session.begin(with: text.wrappedValue) }
        _session = State(initialValue: session)
    }

    private var shownError: String? {
        MonarchInlineEditTextTokens.shownError(local: session.errorMessage, external: error)
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if session.isEditing, isEnabled {
                editor
            } else {
                viewer
            }
        }
        .monarchAnnouncesError(
            shownError,
            labels: MonarchFormFieldLabels(error: { MonarchInlineEditTextStrings.error($0) })
        )
    }

    // MARK: View mode

    @ViewBuilder private var viewer: some View {
        let shown = MonarchInlineEditTextTokens.viewText(value: text, prompt: prompt)
        let role = MonarchInlineEditTextTokens.viewForeground(isEmpty: text.isEmpty, isEnabled: isEnabled)
        let value = Text(shown)
            .monarchTextStyle(textStyle)
            .foregroundStyle(theme.colors[role: role])
            .multilineTextAlignment(.leading)
        if isEnabled {
            Button {
                session.begin(with: text)
            } label: {
                HStack(alignment: .firstTextBaseline, spacing: MonarchInlineEditTextTokens.iconSpacing) {
                    // No pencil at rest, as on the web, which shows it only on hover; the whole line is the button.
                    value
                }
                .frame(
                    maxWidth: .infinity,
                    minHeight: MonarchInlineEditTextTokens.minHeight,
                    alignment: .leading
                )
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .accessibilityValue(shown)
            .accessibilityHint(MonarchInlineEditTextTokens.editHint(editHint))
        } else {
            value
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(shown)
        }
    }

    // MARK: Edit mode

    private var editor: some View {
        @Bindable var session = session
        return VStack(alignment: .leading, spacing: MonarchInlineEditTextTokens.stackSpacing) {
            input(draft: $session.draft)
                .monarchFieldText()
                .foregroundStyle(theme.colors.textDefault)
                .focused($isFocused)
                .disabled(session.isSaving)
                .accessibilityLabel(label)
                .accessibilityHint(MonarchInlineEditTextTokens.fieldHint(error: shownError))
                .monarchFieldBox(
                    hasError: shownError != nil,
                    isFocused: isFocused,
                    minHeight: MonarchInlineEditTextTokens.minHeight
                )
                .onAppear { isFocused = true }
                .onKeyPress(.escape) {
                    cancel()
                    return .handled
                }
                .onChange(of: isFocused) { _, focused in
                    if !focused, savesOnFocusLoss, session.savesOnFocusLoss { commit() }
                }
            if let shownError {
                MonarchFieldErrorRow(message: shownError)
            }
            HStack(spacing: MonarchInlineEditTextTokens.actionSpacing) {
                MonarchButton(
                    saveTitle ?? MonarchInlineEditTextStrings.save,
                    variant: .primary,
                    size: .sm,
                    isLoading: session.isSaving,
                    loadingLabel: savingLabel ?? MonarchInlineEditTextStrings.saving
                ) {
                    commit()
                }
                MonarchButton(cancelTitle ?? MonarchInlineEditTextStrings.cancel, variant: .secondary, size: .sm) {
                    cancel()
                }
                .disabled(session.isSaving)
            }
        }
    }

    @ViewBuilder
    private func input(draft: Binding<String>) -> some View {
        if isMultiline {
            // TextEditor, because Return must add a line; a TextField submits on Return.
            TextEditor(text: draft)
                .scrollContentBackground(.hidden)
                .frame(height: editorHeight)
                .overlay(alignment: .topLeading) {
                    if draft.wrappedValue.isEmpty, let prompt {
                        Text(prompt)
                            .foregroundStyle(theme.colors.textSubtle)
                            .padding(.leading, MonarchInlineEditTextTokens.editorPromptLeadingInset)
                            .padding(.top, MonarchInlineEditTextTokens.editorPromptTopInset)
                            .allowsHitTesting(false)
                            .accessibilityHidden(true)
                    }
                }
        } else {
            TextField(label, text: draft, prompt: prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) })
                .textFieldStyle(.plain)
                .onSubmit { commit() }
        }
    }

    // MARK: Actions

    private func commit() {
        let session = session
        Task {
            let outcome = await session.commit(
                value: text,
                trimsWhitespace: trimsWhitespace,
                validate: validate,
                message: { MonarchInlineEditTextTokens.failureMessage(for: $0, override: failureMessage) },
                onSave: onSave
            )
            switch outcome {
            case .saved(let next):
                text = next
            case .invalid, .failed:
                // Keep the person in the editor with their draft.
                isFocused = true
            case .ignored, .unchanged:
                break
            }
        }
    }

    private func cancel() {
        if session.cancel() { onCancel?() }
    }
}

#if DEBUG
    #Preview("MonarchInlineEditText") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchInlineEditText("Title", text: .constant("Quarterly plan")) { _ in }
                MonarchInlineEditText("Description", text: .constant(""), prompt: "Add a description") { _ in }
                MonarchInlineEditText("Owner", text: .constant("Read only")) { _ in }.disabled(true)
                MonarchInlineEditText(
                    "Title",
                    text: .constant("Plan"),
                    error: "Changed by someone else.",
                    startsEditing: true
                ) {
                    _ in
                }
                MonarchInlineEditText(
                    "Notes",
                    text: .constant(""),
                    prompt: "Add notes",
                    isMultiline: true,
                    startsEditing: true
                ) {
                    _ in
                }
            }
        }
    }
#endif

private struct MonarchInlineEditTextStyleKey: EnvironmentKey {
    static let defaultValue: MonarchTextStyle = .body
}

extension EnvironmentValues {
    fileprivate var monarchInlineEditTextStyle: MonarchTextStyle {
        get { self[MonarchInlineEditTextStyleKey.self] }
        set { self[MonarchInlineEditTextStyleKey.self] = newValue }
    }
}

extension View {
    /// Sets the type role of the text of a ``MonarchInlineEditText`` at rest, so it can sit in a page title
    /// or a heading and read in that size and weight, as the web component does by inheriting its context.
    /// The default is `.body`.
    public func monarchInlineEditTextStyle(_ style: MonarchTextStyle) -> some View {
        environment(\.monarchInlineEditTextStyle, style)
    }
}
