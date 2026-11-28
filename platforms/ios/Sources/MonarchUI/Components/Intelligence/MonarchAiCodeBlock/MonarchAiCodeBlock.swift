import SwiftUI

/// Code written by an assistant: ``MonarchCodeBlock`` with copy, wrap,
/// diffs and line numbers, plus optional Apply and Insert actions that hand
/// the code back to the app.
///
/// ```swift
/// MonarchAiCodeBlock(
///     patch,
///     language: "diff",
///     title: "server.ts",
///     isIncomplete: isStreaming,
///     onApply: { code in editor.apply(code) }
/// )
/// ```
///
/// While the answer is still streaming, pass `isIncomplete`: Apply and
/// Insert stay disabled until the code is complete, and Copy keeps working.
/// Apply is the primary action, Insert a secondary one; app controls passed
/// in `actions` come first. The actions sit in a row under the code.
///
/// Accessibility: the code is read as ``MonarchCodeBlock`` reads it, line by
/// line, with added and removed lines named. Apply and Insert are buttons
/// named with the code ("Apply server.ts"), so several blocks in one answer
/// can be told apart; while disabled their hint says why. Every button has a
/// 44-point touch target.
public struct MonarchAiCodeBlock<Actions: View>: View {
    private let code: String
    private let language: String?
    private let title: String?
    private let isDiff: Bool?
    private let isHighlighted: Bool
    private let showsLineNumbers: Bool
    private let showsWrapToggle: Bool
    private let isCopyable: Bool
    private let isIncomplete: Bool
    private let isWrapped: Binding<Bool>?
    private let labels: MonarchAiCodeBlockLabels
    private let onCopy: (@MainActor (String) -> Void)?
    private let onInsert: (@MainActor (String) -> Void)?
    private let onApply: (@MainActor (String) -> Void)?
    private let actions: Actions

    /// An AI code block with app controls in the action row.
    ///
    /// - Parameters:
    ///   - code: The code, exactly as it should be copied and applied.
    ///   - language: A language id ("ts", "python", "bash", "diff"), which picks the
    ///     highlighter and the header text.
    ///   - title: The header text, such as a file name, already localised. It also names the code.
    ///   - isDiff: Whether `+` and `-` lines are drawn as a diff. Defaults to true for "diff" and "patch".
    ///   - isHighlighted: Whether syntax colouring is on.
    ///   - showsLineNumbers: Whether line numbers are shown.
    ///   - showsWrapToggle: Whether the wrap toggle is shown.
    ///   - isCopyable: Whether the copy button is shown.
    ///   - isIncomplete: Whether the code is still arriving. Apply and Insert are disabled meanwhile.
    ///   - isWrapped: Whether long lines wrap. Without it the block keeps the state itself.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied code after each copy.
    ///   - onInsert: Adds an Insert button that hands over the code.
    ///   - onApply: Adds an Apply button that hands over the code.
    ///   - actions: App controls placed before Insert and Apply.
    public init(
        _ code: String,
        language: String? = nil,
        title: String? = nil,
        isDiff: Bool? = nil,
        isHighlighted: Bool = true,
        showsLineNumbers: Bool = false,
        showsWrapToggle: Bool = true,
        isCopyable: Bool = true,
        isIncomplete: Bool = false,
        isWrapped: Binding<Bool>? = nil,
        labels: MonarchAiCodeBlockLabels = MonarchAiCodeBlockLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil,
        onInsert: (@MainActor (String) -> Void)? = nil,
        onApply: (@MainActor (String) -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.code = code
        self.language = language
        self.title = title
        self.isDiff = isDiff
        self.isHighlighted = isHighlighted
        self.showsLineNumbers = showsLineNumbers
        self.showsWrapToggle = showsWrapToggle
        self.isCopyable = isCopyable
        self.isIncomplete = isIncomplete
        self.isWrapped = isWrapped
        self.labels = labels
        self.onCopy = onCopy
        self.onInsert = onInsert
        self.onApply = onApply
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiCodeBlockTokens.spacing) {
            MonarchCodeBlock(
                code,
                language: language,
                title: title,
                isDiff: isDiff,
                isHighlighted: isHighlighted,
                showsLineNumbers: showsLineNumbers,
                showsWrapToggle: showsWrapToggle,
                isCopyable: isCopyable,
                isWrapped: isWrapped,
                labels: labels.codeBlock,
                onCopy: onCopy
            )
            if MonarchAiCodeBlockTokens.showsActionRow(
                hasApply: onApply != nil,
                hasInsert: onInsert != nil,
                hasCustomActions: Actions.self != EmptyView.self
            ) {
                MonarchFlowLayout(spacing: MonarchAiCodeBlockTokens.actionSpacing, justification: .trailing) {
                    actions
                    if let onInsert {
                        actionButton(
                            MonarchAiCodeBlockTokens.insertTitle(labels),
                            variant: .secondary,
                            action: onInsert
                        )
                    }
                    if let onApply {
                        actionButton(MonarchAiCodeBlockTokens.applyTitle(labels), variant: .primary, action: onApply)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
    }

    private func actionButton(
        _ title: String,
        variant: MonarchButtonVariant,
        action: @escaping @MainActor (String) -> Void
    ) -> some View {
        let target = MonarchAiCodeBlockTokens.name(title: self.title, language: language, labels: labels)
        let code = code
        return MonarchButton(title, variant: variant, size: .sm) { action(code) }
            .disabled(!MonarchAiCodeBlockTokens.actionsEnabled(isIncomplete: isIncomplete))
            .accessibilityLabel(MonarchAiCodeBlockTokens.actionLabel(title, target: target, labels: labels))
            .accessibilityHint(MonarchAiCodeBlockTokens.actionHint(isIncomplete: isIncomplete, labels: labels))
    }
}

extension MonarchAiCodeBlock where Actions == EmptyView {
    /// An AI code block.
    ///
    /// - Parameters:
    ///   - code: The code, exactly as it should be copied and applied.
    ///   - language: A language id ("ts", "python", "bash", "diff"), which picks the
    ///     highlighter and the header text.
    ///   - title: The header text, such as a file name, already localised. It also names the code.
    ///   - isDiff: Whether `+` and `-` lines are drawn as a diff. Defaults to true for "diff" and "patch".
    ///   - isHighlighted: Whether syntax colouring is on.
    ///   - showsLineNumbers: Whether line numbers are shown.
    ///   - showsWrapToggle: Whether the wrap toggle is shown.
    ///   - isCopyable: Whether the copy button is shown.
    ///   - isIncomplete: Whether the code is still arriving. Apply and Insert are disabled meanwhile.
    ///   - isWrapped: Whether long lines wrap. Without it the block keeps the state itself.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied code after each copy.
    ///   - onInsert: Adds an Insert button that hands over the code.
    ///   - onApply: Adds an Apply button that hands over the code.
    public init(
        _ code: String,
        language: String? = nil,
        title: String? = nil,
        isDiff: Bool? = nil,
        isHighlighted: Bool = true,
        showsLineNumbers: Bool = false,
        showsWrapToggle: Bool = true,
        isCopyable: Bool = true,
        isIncomplete: Bool = false,
        isWrapped: Binding<Bool>? = nil,
        labels: MonarchAiCodeBlockLabels = MonarchAiCodeBlockLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil,
        onInsert: (@MainActor (String) -> Void)? = nil,
        onApply: (@MainActor (String) -> Void)? = nil
    ) {
        self.init(
            code,
            language: language,
            title: title,
            isDiff: isDiff,
            isHighlighted: isHighlighted,
            showsLineNumbers: showsLineNumbers,
            showsWrapToggle: showsWrapToggle,
            isCopyable: isCopyable,
            isIncomplete: isIncomplete,
            isWrapped: isWrapped,
            labels: labels,
            onCopy: onCopy,
            onInsert: onInsert,
            onApply: onApply,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiCodeBlock") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiCodeBlock(
                    "@@ -1,2 +1,2 @@\n-const port = 3000;\n+const port = 8080;\n export default port;",
                    language: "diff",
                    title: "server.ts",
                    onInsert: { _ in },
                    onApply: { _ in }
                )
                MonarchAiCodeBlock(
                    "def greet(name):\n    return f\"Hi {name}\"",
                    language: "python",
                    showsLineNumbers: true,
                    isIncomplete: true,
                    onApply: { _ in }
                )
            }
        }
    }
#endif
