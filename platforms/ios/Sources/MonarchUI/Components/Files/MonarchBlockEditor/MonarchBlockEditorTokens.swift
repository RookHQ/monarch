import MonarchTokens
import SwiftUI

/// Component tokens, names and menu logic for ``MonarchBlockEditor``.
enum MonarchBlockEditorTokens {
    // MARK: Sizes

    /// Padding before and after each block row.
    static let rowHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below each block row.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the handle, a list marker and the text.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the fields inside a toggle or embed block.
    static let innerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of a list marker ("•", "12."), scaled with Dynamic Type.
    static let markerWidth: CGFloat = MonarchTokens.Space.space6
    /// The width of the bar before a quote.
    static let quoteBarWidth: CGFloat = MonarchTokens.Space.space1
    /// Padding inside a code block.
    static let codePadding: CGFloat = MonarchTokens.Space.space3
    /// The corner radius of a code block.
    static let codeCornerRadius: CGFloat = MonarchTokens.Radius.md
    /// Padding around the toolbar and the selection bar.
    static let barPadding: CGFloat = MonarchTokens.Space.space2
    /// The least height of a block row: the touch target of its text.
    static let minRowHeight: CGFloat = MonarchAccessibility.minimumHitTarget

    /// The id of the toolbar's Select blocks control.
    static let selectToolID = "monarch.blockEditor.select"
    /// The SF Symbol of the Select blocks control.
    static let selectSymbol = "checkmark.circle"
    /// The bullet of a bulleted list item.
    static let bullet = "•"

    // MARK: Colours

    /// The colour role of block text for a state.
    static func textColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour role of a checklist item's text: muted when ticked, so
    /// the struck-through text recedes; the checkbox carries the state.
    static func taskTextColor(isChecked: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return isChecked ? \.textMuted : \.textDefault
    }

    /// The colour role of list markers.
    static var markerColor: MonarchColorRole { \.textMuted }
    /// The colour role of placeholders.
    static var promptColor: MonarchColorRole { \.textSubtle }
    /// The colour role of the bar before a quote.
    static var quoteBarColor: MonarchColorRole { \.borderStrong }
    /// The fill role of a code block.
    static var codeBackground: MonarchColorRole { \.bgSubtle }
    /// The fill role of the document.
    static var background: MonarchColorRole { \.bgSurface }
    /// The colour role of the line under the toolbar.
    static var separatorColor: MonarchColorRole { \.borderSubtle }

    // MARK: Kinds, tools and names

    /// The text style of a block's text.
    static func textStyle(_ kind: MonarchBlockKind) -> MonarchTextStyle {
        switch kind {
        case .heading(let level):
            switch clampedLevel(level) {
            case 1: .title
            case 2: .heading
            default: .subheading
            }
        case .codeBlock: .code
        default: .body
        }
    }

    /// A heading level within 1 to 6.
    static func clampedLevel(_ level: Int) -> Int {
        min(max(level, 1), 6)
    }

    /// The toolbar tool of a text kind, or `nil` for other kinds.
    static func tool(_ kind: MonarchBlockKind) -> MonarchEditorTool? {
        switch kind {
        case .paragraph: .paragraph
        case .heading(let level): .heading(clampedLevel(level))
        case .bulletList: .bulletList
        case .orderedList: .orderedList
        case .taskList: .taskList
        case .blockquote: .blockquote
        case .codeBlock: .codeBlock
        case .callout, .toggle, .table, .embed: nil
        }
    }

    /// The text kind of a toolbar tool, or `nil` for undo, redo, inline styles and product tools.
    static func kind(_ tool: MonarchEditorTool) -> MonarchBlockKind? {
        switch tool {
        case .paragraph: .paragraph
        case .heading(let level): .heading(clampedLevel(level))
        case .bulletList: .bulletList
        case .orderedList: .orderedList
        case .taskList: .taskList
        case .blockquote: .blockquote
        case .codeBlock: .codeBlock
        case .undo, .redo, .bold, .italic, .strikethrough, .code, .link, .custom: nil
        }
    }

    /// The toolbar groups for the offered kinds: undo and redo, text
    /// styles, lists, quote and code, then Select blocks. Inline styles are
    /// not offered: block text is plain.
    static func toolGroups(kinds: [MonarchBlockKind], selectTitle: String) -> [[MonarchEditorTool]] {
        let tools = kinds.compactMap(tool)
        let styles = tools.filter { if case .heading = $0 { true } else { $0 == .paragraph } }
        let lists = tools.filter { [.bulletList, .orderedList, .taskList].contains($0) }
        let blocks = tools.filter { [.blockquote, .codeBlock].contains($0) }
        let select = MonarchEditorTool.custom(id: selectToolID, title: selectTitle, systemImage: selectSymbol)
        return [[.undo, .redo], styles, lists, blocks, [select]].filter { !$0.isEmpty }
    }

    /// The key a kind's name and description are looked up by: callouts of every tone share one.
    static func nameKey(_ kind: MonarchBlockKind) -> MonarchBlockKind {
        if case .callout = kind { return .callout(.neutral) }
        if case .heading(let level) = kind { return .heading(clampedLevel(level)) }
        return kind
    }

    /// The name of a kind: the override, the toolbar's name for text kinds, or the built-in name.
    static func name(_ kind: MonarchBlockKind, labels: MonarchBlockEditorLabels) -> String {
        if let name = labels.blockNames[nameKey(kind)] { return name }
        if let tool = tool(kind) { return MonarchEditorToolbarTokens.title(tool, labels: labels.toolbar) }
        switch kind {
        case .callout: return MonarchBlockEditorStrings.string("callout")
        case .toggle: return MonarchBlockEditorStrings.string("toggle")
        case .table: return MonarchBlockEditorStrings.string("table")
        default: return MonarchBlockEditorStrings.string("embed")
        }
    }

    /// The slash menu description of a kind.
    static func description(_ kind: MonarchBlockKind, labels: MonarchBlockEditorLabels) -> String {
        if let description = labels.blockDescriptions[nameKey(kind)] { return description }
        let key: String
        switch kind {
        case .paragraph: key = "paragraphDescription"
        case .heading(let level):
            switch clampedLevel(level) {
            case 1: key = "heading1Description"
            case 2: key = "heading2Description"
            default: key = "headingDescription"
            }
        case .bulletList: key = "bulletListDescription"
        case .orderedList: key = "orderedListDescription"
        case .taskList: key = "taskListDescription"
        case .blockquote: key = "blockquoteDescription"
        case .codeBlock: key = "codeBlockDescription"
        case .callout: key = "calloutDescription"
        case .toggle: key = "toggleDescription"
        case .table: key = "tableDescription"
        case .embed: key = "embedDescription"
        }
        return MonarchBlockEditorStrings.string(key)
    }

    /// The SF Symbol of a kind, the toolbar's for text kinds.
    static func symbol(_ kind: MonarchBlockKind) -> String {
        if let tool = tool(kind) { return MonarchEditorToolbarTokens.symbol(tool) }
        switch kind {
        case .callout: return "info.circle"
        case .toggle: return "chevron.forward.circle"
        case .table: return "tablecells"
        default: return "play.rectangle"
        }
    }

    /// The stable slash menu id of a kind: "paragraph", "heading-2", "callout-info", …
    static func slashID(_ kind: MonarchBlockKind) -> String {
        switch kind {
        case .paragraph: "paragraph"
        case .heading(let level): "heading-\(clampedLevel(level))"
        case .bulletList: "bulletList"
        case .orderedList: "orderedList"
        case .taskList: "taskList"
        case .blockquote: "blockquote"
        case .codeBlock: "codeBlock"
        case .callout(let tone): "callout-\(tone)"
        case .toggle: "toggle"
        case .table: "table"
        case .embed: "embed"
        }
    }

    /// The offered kind with a slash menu id, or `nil` for a product item.
    static func kind(slashID id: String, kinds: [MonarchBlockKind]) -> MonarchBlockKind? {
        kinds.first { slashID($0) == id }
    }

    /// The slash menu group of a kind.
    static func group(_ kind: MonarchBlockKind, labels: MonarchBlockEditorLabels) -> String {
        switch kind {
        case .paragraph, .heading, .blockquote, .codeBlock:
            labels.groupText ?? MonarchBlockEditorStrings.groupText
        case .bulletList, .orderedList, .taskList:
            labels.groupLists ?? MonarchBlockEditorStrings.groupLists
        case .callout, .toggle, .table, .embed:
            labels.groupBlocks ?? MonarchBlockEditorStrings.groupBlocks
        }
    }

    /// The slash menu items: one per offered kind, in order, then the product items.
    static func slashItems(
        kinds: [MonarchBlockKind],
        extra: [MonarchSlashMenuItem],
        labels: MonarchBlockEditorLabels
    ) -> [MonarchSlashMenuItem] {
        kinds.map { kind in
            MonarchSlashMenuItem(
                id: slashID(kind),
                label: name(kind, labels: labels),
                description: description(kind, labels: labels),
                systemImage: symbol(kind),
                group: group(kind, labels: labels),
                keywords: keywords(kind)
            )
        } + extra
    }

    /// The group headings in display order.
    static func groupOrder(labels: MonarchBlockEditorLabels) -> [String] {
        [
            labels.groupText ?? MonarchBlockEditorStrings.groupText,
            labels.groupLists ?? MonarchBlockEditorStrings.groupLists,
            labels.groupBlocks ?? MonarchBlockEditorStrings.groupBlocks,
        ]
    }

    /// Extra words a kind is found by in the slash menu.
    static func keywords(_ kind: MonarchBlockKind) -> [String] {
        switch kind {
        case .paragraph: ["paragraph", "p"]
        case .heading(let level): ["h\(clampedLevel(level))", "title"]
        case .bulletList: ["ul", "bullet"]
        case .orderedList: ["ol", "number"]
        case .taskList: ["todo", "task", "checkbox"]
        case .blockquote: ["quote", "blockquote"]
        case .codeBlock: ["code", "pre"]
        case .callout: ["note", "info", "alert"]
        case .toggle: ["collapse", "details"]
        case .table: ["grid", "cells"]
        case .embed: ["video", "link", "iframe"]
        }
    }

    /// The name of a callout tone in the tone menu.
    static func toneName(_ tone: MonarchTone, labels: MonarchBlockEditorLabels) -> String {
        labels.toneNames[tone] ?? MonarchToneStrings.name(tone) ?? MonarchBlockEditorStrings.toneNeutral
    }

    // MARK: Accessibility

    /// The spoken name of a block's text: the kind's name, with the
    /// position for list items ("Numbered list, item 2").
    static func fieldLabel(at index: Int, in blocks: [MonarchBlock], labels: MonarchBlockEditorLabels) -> String {
        guard blocks.indices.contains(index) else { return "" }
        let kind = blocks[index].kind
        let name = name(kind, labels: labels)
        switch kind {
        case .bulletList, .orderedList, .taskList:
            let position = MonarchBlockEditorDocument.listPosition(at: index, in: blocks)
            return labels.listItem?(name, position) ?? MonarchBlockEditorStrings.listItem(name, position: position)
        default:
            return name
        }
    }

    /// The spoken name of a block's action button.
    static func handleLabel(_ kind: MonarchBlockKind, labels: MonarchBlockEditorLabels) -> String {
        let name = name(kind, labels: labels)
        return labels.handle?(name) ?? MonarchBlockEditorStrings.handle(name)
    }

    /// The spoken name of a checklist item's checkbox: its text, or the list's name when it is empty.
    static func checkboxLabel(_ block: MonarchBlock, labels: MonarchBlockEditorLabels) -> String {
        let text = block.text.trimmingCharacters(in: .whitespacesAndNewlines)
        return text.isEmpty ? name(.taskList, labels: labels) : text
    }

    /// The visible marker of a list item: a bullet, or the number and a full stop.
    static func marker(at index: Int, in blocks: [MonarchBlock]) -> String? {
        guard blocks.indices.contains(index) else { return nil }
        switch blocks[index].kind {
        case .bulletList: return bullet
        case .orderedList: return "\(MonarchBlockEditorDocument.listPosition(at: index, in: blocks))."
        default: return nil
        }
    }

    /// The text of the selection bar.
    static func selectedCount(_ count: Int, labels: MonarchBlockEditorLabels) -> String {
        labels.selectedCount?(count) ?? MonarchBlockEditorStrings.selectedCount(count)
    }

    /// The side of one button of a callout's tone switcher.
    static let toneButtonSize: CGFloat = MonarchTokens.Control.xs
    /// The space around the buttons of a callout's tone switcher.
    static let toneSwitcherPadding: CGFloat = MonarchTokens.Space.space0_5
}
