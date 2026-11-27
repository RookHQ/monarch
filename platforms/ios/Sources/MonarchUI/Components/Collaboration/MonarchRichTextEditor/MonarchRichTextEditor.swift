import SwiftUI

/// A rich-text editor: the system's `TextEditor` on an `AttributedString`,
/// with a Monarch formatting toolbar, a prompt, a character count and
/// @mention suggestions.
///
/// ```swift
/// @State private var text = AttributedString()
///
/// MonarchRichTextEditor("Description", text: $text, prompt: "What changed?", maxLength: 2_000)
/// ```
///
/// The document is the binding's `AttributedString`. Bold and italic are
/// traits of a run's font, inline code a monospaced font, strikethrough
/// `strikethroughStyle` and links `link`, so `Text(text)` shows it as
/// written. Headings 1 to 3 are paragraphs in the title, heading and
/// subheading type roles. Bulleted and numbered lists are text prefixes
/// ("• ", "1. "); Return continues a list and Return on an empty item ends
/// it. The system's own text editing stays: selection, the edit menu,
/// dictation, the system Format menu, undo with a shake or three fingers.
///
/// The toolbar is ``MonarchEditorToolbar`` above the text. Checklists,
/// quotes and code blocks are not offered, because the system text view
/// has no block structure to draw them; custom tools are passed to
/// `onTool`. With `mentionItems`, typing @ or # shows
/// ``MonarchMentionPicker`` under the text; `onMentionSearch` receives the
/// query to search, and a chosen item is written as "@Name " and passed to
/// `onMention`.
///
/// `maxLength` stops typing at the limit and cuts pasted text that does not
/// fit, as the web and Android editors do. The count shows against the
/// limit, and the danger border shows for a document the app set that is
/// already over it: check ``MonarchRichTextEditor/isOverLimit(_:maxLength:)`` before saving that.
///
/// Requires iOS 26, where `TextEditor` edits an `AttributedString`.
///
/// Accessibility: the text view is the standard one, named by `label`, with
/// its text as the value and all its editing gestures and rotor actions.
/// The toolbar is a container named "Formatting" whose toggles read their
/// state. The count reads the characters left or over the limit. The
/// prompt is not read, as with a text field. The box has a focus ring.
@available(iOS 26.0, macOS 26.0, *)
@_spi(Experimental) public struct MonarchRichTextEditor: View {
    private let label: String
    @Binding private var text: AttributedString
    private let prompt: String?
    private let tools: [[MonarchEditorTool]]
    private let maxLength: Int?
    private let showsCharacterCount: Bool?
    private let isReadOnly: Bool
    private let isInvalid: Bool
    private let mentionItems: [MonarchMentionItem]?
    private let mentionStatus: MonarchMentionPickerStatus
    private let labels: MonarchRichTextEditorLabels
    private let onMentionSearch: (@MainActor (MonarchMentionQuery?) -> Void)?
    private let onMention: (@MainActor (MonarchMentionItem) -> Void)?
    private let onTool: (@MainActor (MonarchEditorTool) -> Void)?

    @State private var selection = AttributedTextSelection()
    @State private var mentionQuery: MonarchMentionQuery?
    @FocusState private var isFocused: Bool
    @Environment(\.monarchTheme) private var theme
    @Environment(\.fontResolutionContext) private var fontContext
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.undoManager) private var undoManager
    @ScaledMetric(relativeTo: .body) private var minTextHeight = MonarchRichTextEditorTokens.minTextHeight

    /// The default tools: body text and headings 2 and 3, bold, italic,
    /// strikethrough and code, bulleted and numbered lists, and links.
    public static var defaultTools: [[MonarchEditorTool]] { MonarchRichTextEditorTokens.defaultTools }

    /// A rich-text editor.
    ///
    /// - Parameters:
    ///   - label: The spoken name of the text, already localised.
    ///   - text: The document.
    ///   - prompt: Shown while the document is empty.
    ///   - tools: The toolbar's tools in groups. Empty for no toolbar.
    ///   - maxLength: The most characters. Typing and pasting stop at the limit; shows the count and flags a longer document set by the app.
    ///   - showsCharacterCount: Shows the count. By default, when there is a `maxLength`.
    ///   - isReadOnly: Shows the document as text, without the toolbar.
    ///   - isInvalid: Draws the danger border, for a validation error shown nearby.
    ///   - mentionItems: The suggestions for the mention being typed. `nil` turns mentions off.
    ///   - mentionStatus: The state of the mention search.
    ///   - labels: Replacements for the built-in strings.
    ///   - onMentionSearch: Receives the mention being typed, or `nil` when there is none; search and update `mentionItems`.
    ///   - onMention: Receives the person or channel chosen.
    ///   - onTool: Receives the custom tools of the toolbar.
    public init(
        _ label: String,
        text: Binding<AttributedString>,
        prompt: String? = nil,
        tools: [[MonarchEditorTool]] = MonarchRichTextEditor.defaultTools,
        maxLength: Int? = nil,
        showsCharacterCount: Bool? = nil,
        isReadOnly: Bool = false,
        isInvalid: Bool = false,
        mentionItems: [MonarchMentionItem]? = nil,
        mentionStatus: MonarchMentionPickerStatus = .ready,
        labels: MonarchRichTextEditorLabels = MonarchRichTextEditorLabels(),
        onMentionSearch: (@MainActor (MonarchMentionQuery?) -> Void)? = nil,
        onMention: (@MainActor (MonarchMentionItem) -> Void)? = nil,
        onTool: (@MainActor (MonarchEditorTool) -> Void)? = nil
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.tools = tools
        self.maxLength = maxLength
        self.showsCharacterCount = showsCharacterCount
        self.isReadOnly = isReadOnly
        self.isInvalid = isInvalid
        self.mentionItems = mentionItems
        self.mentionStatus = mentionStatus
        self.labels = labels
        self.onMentionSearch = onMentionSearch
        self.onMention = onMention
        self.onTool = onTool
    }

    /// Whether a document is longer than a limit, to check before saving.
    ///
    /// - Parameters:
    ///   - text: The document.
    ///   - maxLength: The limit, or `nil` for none.
    /// - Returns: Whether it has more characters than the limit.
    public static func isOverLimit(_ text: AttributedString, maxLength: Int?) -> Bool {
        MonarchRichTextEditorTokens.isOverLimit(count: text.characters.count, limit: maxLength)
    }

    private typealias Tokens = MonarchRichTextEditorTokens
    private typealias Engine = MonarchRichTextEditorEngine

    private var fonts: MonarchDocumentFonts { MonarchDocumentFonts(typography: theme.typography) }

    /// The content of the view.
    public var body: some View {
        let count = text.characters.count
        VStack(alignment: .leading, spacing: Tokens.footerSpacing) {
            if isReadOnly {
                Text(text)
                    .font(fonts.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                box
            }
            if Tokens.showsCount(showsCharacterCount, limit: maxLength) {
                Text(Tokens.countText(count: count, limit: maxLength, labels: labels))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: Tokens.countColor(count: count, limit: maxLength)])
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .accessibilityLabel(Tokens.countAccessibilityText(count: count, limit: maxLength, labels: labels))
            }
        }
        .onChange(of: text) { old, new in
            var updated = new
            var moved = selection
            var changed = false
            if let maxLength, Engine.clamp(&updated, after: old, limit: maxLength, selection: &moved) {
                changed = true
            } else if Engine.continueList(after: old, in: &updated, selection: &moved) {
                changed = true
            }
            if changed {
                text = updated
                selection = moved
            }
            updateMention()
        }
        .onChange(of: selection) { updateMention() }
    }

    private var box: some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius)
        let shownTools = Tokens.supportedTools(tools)
        let isOver = Tokens.isOverLimit(count: text.characters.count, limit: maxLength)
        return VStack(alignment: .leading, spacing: 0) {
            if !shownTools.isEmpty {
                MonarchEditorToolbar(
                    groups: shownTools,
                    active: Engine.active(in: text, selection: selection, fonts: fonts, context: fontContext),
                    disabled: disabledTools,
                    labels: labels.toolbar,
                    onLink: { url in
                        var updated = text
                        Engine.setLink(url, in: &updated, selection: &selection)
                        text = updated
                    },
                    action: apply
                )
                .padding(Tokens.toolbarPadding)
                Rectangle()
                    .fill(theme.colors.borderSubtle)
                    .frame(height: MonarchShapes.borderWidth)
                    .accessibilityHidden(true)
            }
            ZStack(alignment: .topLeading) {
                TextEditor(text: $text, selection: $selection)
                    .font(fonts.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .scrollContentBackground(.hidden)
                    .focused($isFocused)
                    .accessibilityLabel(label)
                    .accessibilityHint(
                        isOver
                            ? Tokens.countAccessibilityText(
                                count: text.characters.count,
                                limit: maxLength,
                                labels: labels
                            ) : ""
                    )
                if text.characters.isEmpty, let prompt {
                    Text(prompt)
                        .font(fonts.body)
                        .foregroundStyle(theme.colors.textMuted)
                        .padding(.top, Tokens.promptTopInset)
                        .padding(.leading, Tokens.promptLeadingInset)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
            }
            .frame(minHeight: minTextHeight)
            .padding(Tokens.textPadding)
            if mentionItems != nil {
                MonarchMentionPicker(
                    mentionQuery,
                    items: mentionItems ?? [],
                    status: mentionStatus,
                    labels: labels.mentions,
                    onRetry: { onMentionSearch?(mentionQuery) },
                    onSelect: insertMention
                )
            }
        }
        .background(shape.fill(theme.colors[role: Tokens.background(isEnabled: isEnabled)]))
        .clipShape(shape)
        .overlay(
            shape.strokeBorder(
                theme.colors[role: Tokens.border(isInvalid: isInvalid || isOver, isEnabled: isEnabled)],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .monarchFocusRing(isFocused, in: shape)
    }

    private var disabledTools: Set<MonarchEditorTool> {
        var disabled = Set<MonarchEditorTool>()
        if undoManager?.canUndo != true { disabled.insert(.undo) }
        if undoManager?.canRedo != true { disabled.insert(.redo) }
        return disabled
    }

    private func apply(_ tool: MonarchEditorTool) {
        switch tool {
        case .undo: undoManager?.undo()
        case .redo: undoManager?.redo()
        case .custom: onTool?(tool)
        default:
            var updated = text
            var moved = selection
            Engine.apply(tool, to: &updated, selection: &moved, fonts: fonts, context: fontContext)
            text = updated
            selection = moved
        }
    }

    private func updateMention() {
        guard mentionItems != nil else { return }
        let found = Engine.mention(in: text, selection: selection, triggers: Tokens.mentionTriggers)?.query
        guard found != mentionQuery else { return }
        mentionQuery = found
        onMentionSearch?(found)
    }

    private func insertMention(_ item: MonarchMentionItem) {
        guard let found = Engine.mention(in: text, selection: selection, triggers: Tokens.mentionTriggers) else {
            return
        }
        var updated = text
        var moved = selection
        Engine.insertMention(
            item,
            trigger: found.query.trigger,
            replacing: found.range,
            in: &updated,
            selection: &moved
        )
        text = updated
        selection = moved
        mentionQuery = nil
        onMentionSearch?(nil)
        onMention?(item)
    }
}

#if DEBUG
    #Preview("MonarchRichTextEditor") {
        if #available(iOS 26.0, macOS 26.0, *) {
            MonarchPreviewMatrix {
                VStack {
                    MonarchRichTextEditor("Notes", text: .constant(AttributedString()), prompt: "Write something")
                    MonarchRichTextEditor(
                        "Reply",
                        text: .constant(AttributedString("Thanks, see the plan.")),
                        tools: MonarchEditorTool.inlineGroups,
                        maxLength: 280
                    )
                    MonarchRichTextEditor("Summary", text: .constant(AttributedString("Read only")), isReadOnly: true)
                }
            }
        }
    }
#endif
