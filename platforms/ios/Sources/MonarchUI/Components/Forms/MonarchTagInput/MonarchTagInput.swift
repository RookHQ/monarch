import SwiftUI

/// A field that edits a list of short values as removable tags: skills,
/// labels, keywords, recipients.
///
/// Return or a separator (a comma by default) adds the typed text. Pasted
/// text that contains separators, new lines or tabs is split into several
/// tags. Each tag is cleaned (trimmed by default), then checked: duplicates,
/// the maximum and the app's `validate` can reject it. A rejected value stays
/// in the field with a message, so it can be fixed instead of retyped.
///
/// ```swift
/// MonarchTagInput(
///     "Skills",
///     tags: $skills,
///     prompt: "Add a skill",
///     helper: "Separate skills with commas.",
///     suggestions: ["Accessibility", "Research", "TypeScript"]
/// )
/// ```
///
/// Accessibility: the text field is labelled with `label`; its hint is the
/// rejection or error (read as "Error: …"), otherwise the help text. Each tag
/// is read as its text, and its remove button is a separate element named
/// "Remove" and the tag with a 44-point touch target. Additions, removals and
/// rejections are announced. Suggestions are buttons named "Add" and the
/// suggestion. Errors have an icon and text, not colour alone.
public struct MonarchTagInput: View {
    private let label: String
    @Binding private var tags: [String]
    private let prompt: String?
    private let helper: String?
    private let error: String?
    private let suggestions: [String]
    private let isReadOnly: Bool
    private let isRequired: Bool
    private let rules: MonarchTagInputModel.Rules

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchHelperAbove) private var helperAbove
    @FocusState private var isFocused: Bool
    @State private var text = ""
    @State private var rejection: String?

    /// A tag input.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Also names the text field.
    ///   - tags: The tags, in order.
    ///   - prompt: Placeholder text shown while there are no tags, as on the web.
    ///   - helper: Help text under the field, such as how values are separated.
    ///   - error: An error from the app, shown under the field while no tag was rejected.
    ///   - suggestions: Values offered as buttons while typing. Already added ones are left out.
    ///   - separators: Characters that add the typed text, besides Return. New lines and tabs always split pasted text.
    ///   - maxCount: The most tags. Further additions are rejected with a message.
    ///   - allowsDuplicates: Whether the same tag may appear twice. Compared without case.
    ///   - isReadOnly: Shows the tags without remove buttons and without the text field.
    ///   - labels: Replacements for the built-in strings.
    ///   - normalize: Cleans typed text before it is checked. Trims white space by default.
    ///   - validate: Returns a message to reject a tag, or `nil` to accept it. Runs after the built-in checks.
    public init(
        _ label: String,
        tags: Binding<[String]>,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        suggestions: [String] = [],
        separators: Set<Character> = [","],
        maxCount: Int? = nil,
        allowsDuplicates: Bool = false,
        isReadOnly: Bool = false,
        labels: MonarchTagInputLabels = MonarchTagInputLabels(),
        normalize: ((String) -> String)? = nil,
        validate: ((String, [String]) -> String?)? = nil
    ) {
        self.init(
            label,
            tags: tags,
            isRequired: false,
            prompt: prompt,
            helper: helper,
            error: error,
            suggestions: suggestions,
            separators: separators,
            maxCount: maxCount,
            allowsDuplicates: allowsDuplicates,
            isReadOnly: isReadOnly,
            labels: labels,
            normalize: normalize,
            validate: validate
        )
    }

    /// A tag input whose label carries the required mark.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. Also names the text field.
    ///   - tags: The tags, in order.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - prompt: Placeholder text shown while there are no tags, as on the web.
    ///   - helper: Help text, under the field or above it with `monarchHelperAbove()`.
    ///   - error: An error from the app, shown under the field while no tag was rejected.
    ///   - suggestions: Values offered as buttons while typing. Already added ones are left out.
    ///   - separators: Characters that add the typed text, besides Return. New lines and tabs always split pasted text.
    ///   - maxCount: The most tags. Further additions are rejected with a message.
    ///   - allowsDuplicates: Whether the same tag may appear twice. Compared without case.
    ///   - isReadOnly: Shows the tags without remove buttons and without the text field.
    ///   - labels: Replacements for the built-in strings.
    ///   - normalize: Cleans typed text before it is checked. Trims white space by default.
    ///   - validate: Returns a message to reject a tag, or `nil` to accept it. Runs after the built-in checks.
    public init(
        _ label: String,
        tags: Binding<[String]>,
        isRequired: Bool,
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        suggestions: [String] = [],
        separators: Set<Character> = [","],
        maxCount: Int? = nil,
        allowsDuplicates: Bool = false,
        isReadOnly: Bool = false,
        labels: MonarchTagInputLabels = MonarchTagInputLabels(),
        normalize: ((String) -> String)? = nil,
        validate: ((String, [String]) -> String?)? = nil
    ) {
        self.label = label
        _tags = tags
        self.isRequired = isRequired
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.suggestions = suggestions
        self.isReadOnly = isReadOnly
        self.rules = MonarchTagInputModel.Rules(
            separators: separators,
            maxCount: maxCount,
            allowsDuplicates: allowsDuplicates,
            normalize: normalize,
            validate: validate,
            labels: labels
        )
    }

    private var canEdit: Bool { isEnabled && !isReadOnly }
    private var message: String? { MonarchTagInputTokens.message(rejection: rejection, error: error) }

    private var editedText: Binding<String> {
        Binding(
            get: { text },
            set: { newText in
                let change = MonarchTagInputModel.process(old: text, new: newText, rules: rules)
                if change.complete.isEmpty {
                    text = newText
                    rejection = nil
                } else {
                    commit(change.complete, remainder: change.remainder)
                }
            }
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchTagInputTokens.stackSpacing) {
            if isRequired {
                MonarchFieldLabelRow(label: label, isRequired: true)
            } else {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: MonarchTagInputTokens.labelColor(isEnabled: isEnabled)])
                    .accessibilityHidden(true)
            }
            if helperAbove, message == nil { helperText }
            box
            if canEdit {
                suggestionButtons
            }
            supportingText
        }
        .onChange(of: message) { _, newMessage in
            if let newMessage {
                AccessibilityNotification.Announcement(newMessage).post()
            }
        }
    }

    private var box: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTextFieldTokens.cornerRadius, style: .continuous)
        let border = MonarchTextFieldTokens.border(hasError: message != nil, isEnabled: isEnabled)
        return VStack(alignment: .leading, spacing: MonarchTagInputTokens.boxSpacing) {
            if !tags.isEmpty {
                MonarchFlowLayout(spacing: MonarchTagInputTokens.tagSpacing) {
                    ForEach(Array(tags.enumerated()), id: \.offset) { index, tag in
                        if canEdit {
                            MonarchTag(tag, size: .sm, removeLabel: rules.labels.remove?(tag)) {
                                remove(at: index)
                            }
                        } else {
                            MonarchTag(tag, size: .sm)
                        }
                    }
                }
            }
            if !isReadOnly {
                field
            }
        }
        .padding(.horizontal, MonarchTextFieldTokens.horizontalPadding)
        .padding(.vertical, MonarchTextFieldTokens.verticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchTextFieldTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchTextFieldTokens.background(isEnabled: isEnabled)]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchTextFieldTokens.borderWidth))
        .monarchFocusRing(isFocused, in: shape)
    }

    private var field: some View {
        TextField(
            label,
            text: editedText,
            prompt: tags.isEmpty ? prompt.map { Text($0).foregroundStyle(theme.colors.textSubtle) } : nil
        )
        .textFieldStyle(.plain)
        .monarchTextStyle(.body)
        .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
        .submitLabel(.done)
        .focused($isFocused)
        .onSubmit {
            commit([text], remainder: "")
            isFocused = true
        }
        .accessibilityLabel(label)
        .accessibilityHint(
            isRequired
                ? MonarchFieldChrome.accessibilityHint(helper: helper, error: message, isRequired: true)
                : MonarchTagInputTokens.accessibilityHint(helper: helper, message: message)
        )
        .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
        .contentShape(Rectangle())
    }

    @ViewBuilder private var suggestionButtons: some View {
        let matches = MonarchTagInputModel.matches(
            suggestions,
            query: text,
            tags: tags,
            limit: MonarchTagInputTokens.suggestionLimit
        )
        if !matches.isEmpty {
            MonarchFlowLayout(spacing: MonarchTagInputTokens.tagSpacing) {
                ForEach(matches, id: \.self) { suggestion in
                    MonarchButton(
                        suggestion,
                        systemImage: MonarchTagInputTokens.suggestionSymbol,
                        variant: .ghost,
                        size: .sm
                    ) {
                        commit([suggestion], remainder: "")
                        isFocused = true
                    }
                    .accessibilityLabel(MonarchTagInputTokens.suggestionLabel(suggestion, labels: rules.labels))
                }
            }
        }
    }

    @ViewBuilder private var helperText: some View {
        if let helper, !helper.isEmpty {
            Text(helper)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder private var supportingText: some View {
        if let message {
            HStack(alignment: .firstTextBaseline, spacing: MonarchTagInputTokens.messageIconSpacing) {
                MonarchIcon(systemName: MonarchTagInputTokens.errorSymbol, size: .sm)
                Text(message)
                    .monarchTextStyle(.caption)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .foregroundStyle(theme.colors.dangerStrong)
            .accessibilityHidden(true)
        } else if !helperAbove {
            helperText
        }
    }

    private func commit(_ parts: [String], remainder: String) {
        guard canEdit else { return }
        let outcome = MonarchTagInputModel.add(parts, to: tags, rules: rules)
        if outcome.tags != tags { tags = outcome.tags }
        text = MonarchTagInputModel.leftover(rejected: outcome.rejected, remainder: remainder)
        rejection = outcome.message
        if outcome.message == nil,
            let announcement = MonarchTagInputModel.addedAnnouncement(outcome.added, labels: rules.labels)
        {
            AccessibilityNotification.Announcement(announcement).post()
        }
    }

    private func remove(at index: Int) {
        guard canEdit, tags.indices.contains(index) else { return }
        let tag = tags.remove(at: index)
        rejection = nil
        AccessibilityNotification.Announcement(rules.labels.removed?(tag) ?? MonarchTagInputStrings.removed(tag)).post()
        isFocused = true
    }
}

#if DEBUG
    #Preview("MonarchTagInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTagInput("Skills", tags: .constant(["Research", "TypeScript"]), prompt: "Add a skill")
                MonarchTagInput("Recipients", tags: .constant([]), helper: "Separate addresses with commas.")
                MonarchTagInput("Labels", tags: .constant(["Billing"]), error: "Add at least two labels.")
                MonarchTagInput("Read only", tags: .constant(["Billing", "Priority"]), isReadOnly: true)
                MonarchTagInput("Disabled", tags: .constant(["Billing"])).disabled(true)
            }
        }
    }
#endif
