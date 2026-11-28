import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchRichTextEditor``.
enum MonarchRichTextEditorTokens {
    /// The smallest height of the text area before Dynamic Type, about four lines.
    static let minTextHeight: CGFloat = MonarchTokens.Space.space24
    /// Padding around the text.
    static let textPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding around the toolbar.
    static let toolbarPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the box and the count.
    static let footerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The corner radius of the box.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The tools the editor applies itself; others go to `onTool`.
    static let builtInTools: Set<MonarchEditorTool> = [
        .undo, .redo, .paragraph, .heading(1), .heading(2), .heading(3), .bold, .italic, .strikethrough, .code,
        .link, .bulletList, .orderedList, .taskList, .blockquote, .codeBlock,
    ]

    /// The default tools: text style, inline styles, lists and links.
    static let defaultTools: [[MonarchEditorTool]] = [
        [.paragraph, .heading(2), .heading(3)],
        [.bold, .italic, .strikethrough, .code],
        [.bulletList, .orderedList],
        [.link],
    ]

    /// The tools shown: the given ones without checklists, quotes, code
    /// blocks and headings below level 3. A checklist, a quote and a code block are drawn as text: a box or a bar before
    /// the line, or the monospaced font.
    static func supportedTools(_ groups: [[MonarchEditorTool]]) -> [[MonarchEditorTool]] {
        groups.map { group in
            group.filter { tool in
                if case .custom = tool { return true }
                return builtInTools.contains(tool)
            }
        }
        .filter { !$0.isEmpty }
    }

    /// The border of the box.
    static func border(isInvalid: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.borderSubtle }
        return isInvalid ? \.dangerStrong : \.borderInput
    }

    /// The fill of the box.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.bgSurface : \.bgSubtle
    }

    /// The colour of the count: danger over the limit.
    static func countColor(count: Int, limit: Int?) -> MonarchColorRole {
        if let limit, count > limit { return \.dangerStrong }
        return \.textMuted
    }

    /// Whether the count is shown: when asked, else when there is a limit.
    static func showsCount(_ showsCharacterCount: Bool?, limit: Int?) -> Bool {
        showsCharacterCount ?? (limit != nil)
    }

    /// Whether the text is over its limit.
    static func isOverLimit(count: Int, limit: Int?) -> Bool {
        limit.map { count > $0 } ?? false
    }

    /// The visible count.
    static func countText(count: Int, limit: Int?, labels: MonarchRichTextEditorLabels) -> String {
        guard let limit else { return labels.count?(count) ?? MonarchRichTextEditorStrings.count(count) }
        return labels.countOfLimit?(count, limit) ?? MonarchRichTextEditorStrings.countOfLimit(count, limit: limit)
    }

    /// What VoiceOver reads for the count: the characters left or over the
    /// limit, or the count without one.
    static func countAccessibilityText(count: Int, limit: Int?, labels: MonarchRichTextEditorLabels) -> String {
        guard let limit else { return labels.count?(count) ?? MonarchRichTextEditorStrings.count(count) }
        return labels.remaining?(limit - count) ?? MonarchRichTextEditorStrings.remaining(limit - count)
    }
}

extension MonarchRichTextEditorTokens {
    /// Where the prompt sits over the empty text, matching the text view's own inset.
    static let promptTopInset: CGFloat = MonarchTokens.Space.space2
    static let promptLeadingInset: CGFloat = MonarchTokens.Space.space1
    /// The characters that start a mention: people and channels.
    static let mentionTriggers: Set<Character> = ["@", "#"]
}
