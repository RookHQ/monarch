import Foundation
import SwiftUI

/// The editing logic of ``MonarchRichTextEditor``: pure functions on the
/// document, its selection and a font resolution context, so they can be
/// tested without a text view.
///
/// Inline formats are attributes: bold and italic are traits of the run's
/// font, code is a monospaced font, strikethrough is `strikethroughStyle`
/// and a link is `link`. A heading is a paragraph whose runs use a heading
/// font. Lists are text: a paragraph that starts with "• " or "1. ".
@available(iOS 26.0, macOS 26.0, *)
enum MonarchRichTextEditorEngine {
    /// The inline traits carried by a run's font.
    struct Traits: OptionSet, Hashable {
        let rawValue: Int
        static let bold = Traits(rawValue: 1 << 0)
        static let italic = Traits(rawValue: 1 << 1)
        static let code = Traits(rawValue: 1 << 2)
    }

    /// The prefix of a bulleted list item.
    static let bullet = "• "
    /// The prefix of a checklist item, open and ticked.
    static let taskOpen = "☐ "
    static let taskDone = "☑ "
    /// The prefix of a line of a block quote.
    static let quoteBar = "▎ "

    // MARK: Fonts

    /// The block style of a font: the heading level whose size it has, or 0 for body text.
    static func level(of font: Font?, fonts: MonarchDocumentFonts, context: Font.Context) -> Int {
        guard let font else { return 0 }
        let size = font.resolve(in: context).pointSize
        return MonarchDocumentFonts.levels.first { fonts.base($0).resolve(in: context).pointSize == size } ?? 0
    }

    /// The traits of a font. Bold is not a trait in a heading, which is bold already.
    static func traits(of font: Font?, fonts: MonarchDocumentFonts, context: Font.Context) -> Traits {
        guard let font else { return [] }
        let resolved = font.resolve(in: context)
        var traits: Traits = []
        if resolved.isBold, level(of: font, fonts: fonts, context: context) == 0 { traits.insert(.bold) }
        if resolved.isItalic { traits.insert(.italic) }
        if resolved.isMonospaced { traits.insert(.code) }
        return traits
    }

    /// The font for a block style and traits; `nil` for plain body text.
    static func font(level: Int, traits: Traits, fonts: MonarchDocumentFonts) -> Font? {
        if level == 0, traits.isEmpty { return nil }
        var font = fonts.base(level)
        if traits.contains(.code) { font = font.monospaced() }
        if traits.contains(.bold) { font = font.bold() }
        if traits.contains(.italic) { font = font.italic() }
        return font
    }

    // MARK: Ranges

    /// The selected range, or an empty range at the insertion point.
    static func bounds(_ selection: AttributedTextSelection, in text: AttributedString) -> Range<AttributedString.Index>
    {
        switch selection.indices(in: text) {
        case .insertionPoint(let index):
            return index..<index
        case .ranges(let set):
            guard let first = set.ranges.first, let last = set.ranges.last else {
                return text.endIndex..<text.endIndex
            }
            return first.lowerBound..<last.upperBound
        }
    }

    /// The paragraphs that a range touches, each without its line break.
    static func paragraphs(
        covering range: Range<AttributedString.Index>,
        in text: AttributedString
    ) -> [Range<AttributedString.Index>] {
        let characters = text.characters
        var start =
            characters[..<range.lowerBound].lastIndex(of: "\n").map { characters.index(after: $0) }
            ?? characters.startIndex
        var result: [Range<AttributedString.Index>] = []
        while true {
            let end = characters[start...].firstIndex(of: "\n") ?? characters.endIndex
            result.append(start..<end)
            guard end < characters.endIndex, end < range.upperBound else { break }
            start = characters.index(after: end)
        }
        return result
    }

    /// The plain text of a range.
    static func string(_ range: Range<AttributedString.Index>, in text: AttributedString) -> String {
        String(text.characters[range])
    }

    // MARK: State

    /// The tools that are on at the selection: inline formats when every
    /// selected run has them (or the typing attributes have them at the
    /// insertion point), the text style of the first paragraph, and the
    /// list of the first paragraph.
    static func active(
        in text: AttributedString,
        selection: AttributedTextSelection,
        fonts: MonarchDocumentFonts,
        context: Font.Context
    ) -> Set<MonarchEditorTool> {
        let containers = Array(selection.attributes(in: text))
        var active = Set<MonarchEditorTool>()
        if !containers.isEmpty {
            let found: [Traits] = containers.map { Self.traits(of: $0.font, fonts: fonts, context: context) }
            if found.allSatisfy({ $0.contains(.bold) }) { active.insert(.bold) }
            if found.allSatisfy({ $0.contains(.italic) }) { active.insert(.italic) }
            if found.allSatisfy({ $0.contains(.code) }) { active.insert(.code) }
            if containers.allSatisfy({ $0.strikethroughStyle != nil }) { active.insert(.strikethrough) }
            if containers.allSatisfy({ $0.link != nil }) { active.insert(.link) }
        }
        let range = Self.bounds(selection, in: text)
        if let paragraph = paragraphs(covering: range, in: text).first {
            let font: Font? =
                paragraph.isEmpty
                ? selection.typingAttributes(in: text).font
                : text[paragraph].runs.first?.font
            let style = Self.level(of: font, fonts: fonts, context: context)
            active.insert(style == 0 ? .paragraph : .heading(style))
            switch listKind(string(paragraph, in: text)) {
            case .bullet: active.insert(.bulletList)
            case .ordered: active.insert(.orderedList)
            case .task: active.insert(.taskList)
            case .quote: active.insert(.blockquote)
            case nil: break
            }
        }
        return active
    }

    // MARK: Commands

    /// Applies a tool to the selection. Inline formats toggle on the
    /// selected runs, or on what is typed next at an insertion point. Text
    /// styles and lists apply to the paragraphs the selection touches.
    /// Other tools are left to the caller.
    static func apply(
        _ tool: MonarchEditorTool,
        to text: inout AttributedString,
        selection: inout AttributedTextSelection,
        fonts: MonarchDocumentFonts,
        context: Font.Context
    ) {
        let active = Self.active(in: text, selection: selection, fonts: fonts, context: context)
        switch tool {
        case .bold:
            toggle(.bold, on: !active.contains(.bold), in: &text, selection: &selection, fonts: fonts, context: context)
        case .italic:
            toggle(
                .italic,
                on: !active.contains(.italic),
                in: &text,
                selection: &selection,
                fonts: fonts,
                context: context
            )
        case .code:
            toggle(.code, on: !active.contains(.code), in: &text, selection: &selection, fonts: fonts, context: context)
        case .strikethrough:
            let isOn = !active.contains(.strikethrough)
            text.transformAttributes(in: &selection) { $0.strikethroughStyle = isOn ? Text.LineStyle.single : nil }
        case .paragraph: setLevel(0, in: &text, selection: &selection, fonts: fonts, context: context)
        case .heading(let level):
            setLevel(
                active.contains(tool) ? 0 : level,
                in: &text,
                selection: &selection,
                fonts: fonts,
                context: context
            )
        case .bulletList: setList(active.contains(.bulletList) ? nil : .bullet, in: &text, selection: &selection)
        case .orderedList: setList(active.contains(.orderedList) ? nil : .ordered, in: &text, selection: &selection)
        case .taskList: setList(active.contains(.taskList) ? nil : .task, in: &text, selection: &selection)
        case .blockquote: setList(active.contains(.blockquote) ? nil : .quote, in: &text, selection: &selection)
        case .codeBlock:
            // A code block is the paragraphs set in the monospaced font.
            let range = Self.bounds(selection, in: text)
            let paragraphs = Self.paragraphs(covering: range, in: text)
            let isOn = !paragraphs.allSatisfy { paragraph in
                text[paragraph].runs.allSatisfy {
                    Self.traits(of: $0.font, fonts: fonts, context: context).contains(.code)
                }
            }
            text.transform(updating: &selection) { document in
                for paragraph in paragraphs where !paragraph.isEmpty {
                    for run in document[paragraph].runs {
                        var current = Self.traits(of: run.font, fonts: fonts, context: context)
                        if isOn { current.insert(.code) } else { current.remove(.code) }
                        let style = Self.level(of: run.font, fonts: fonts, context: context)
                        document[run.range].font = Self.font(level: style, traits: current, fonts: fonts)
                    }
                }
            }
        default: break
        }
    }

    private static func toggle(
        _ trait: Traits,
        on isOn: Bool,
        in text: inout AttributedString,
        selection: inout AttributedTextSelection,
        fonts: MonarchDocumentFonts,
        context: Font.Context
    ) {
        text.transformAttributes(in: &selection) { container in
            let style = Self.level(of: container.font, fonts: fonts, context: context)
            var current = Self.traits(of: container.font, fonts: fonts, context: context)
            if isOn { current.insert(trait) } else { current.remove(trait) }
            container.font = Self.font(level: style, traits: current, fonts: fonts)
        }
    }

    private static func setLevel(
        _ level: Int,
        in text: inout AttributedString,
        selection: inout AttributedTextSelection,
        fonts: MonarchDocumentFonts,
        context: Font.Context
    ) {
        let range = Self.bounds(selection, in: text)
        let paragraphs = Self.paragraphs(covering: range, in: text)
        text.transform(updating: &selection) { document in
            for paragraph in paragraphs where !paragraph.isEmpty {
                for run in document[paragraph].runs {
                    let kept = Self.traits(of: run.font, fonts: fonts, context: context)
                        .subtracting(level > 0 ? .bold : [])
                    document[run.range].font = Self.font(level: level, traits: kept, fonts: fonts)
                }
            }
        }
        if range.isEmpty, paragraphs.first?.isEmpty == true {
            var typing = selection.typingAttributes(in: text)
            typing.font = font(level: level, traits: [], fonts: fonts)
            selection = AttributedTextSelection(insertionPoint: range.lowerBound, typingAttributes: typing)
        }
    }

    // MARK: Lists

    /// The kind of list a paragraph belongs to.
    enum ListKind: Hashable {
        case bullet
        case ordered
        /// A checklist item, "☐ " or, ticked, "☑ ".
        case task
        /// A line of a block quote, "▎ ".
        case quote
    }

    /// The list kind of a paragraph's text, from its prefix.
    static func listKind(_ paragraph: String) -> ListKind? {
        if paragraph.hasPrefix(bullet) { return .bullet }
        if paragraph.hasPrefix(taskOpen) || paragraph.hasPrefix(taskDone) { return .task }
        if paragraph.hasPrefix(quoteBar) { return .quote }
        return orderedNumber(paragraph) != nil ? .ordered : nil
    }

    /// The number of a numbered item ("12. " gives 12).
    static func orderedNumber(_ paragraph: String) -> Int? {
        let digits = paragraph.prefix { $0.isASCII && $0.isNumber }
        guard !digits.isEmpty, paragraph.dropFirst(digits.count).hasPrefix(". ") else { return nil }
        return Int(digits)
    }

    /// The length of a paragraph's list prefix in characters, 0 when it is not an item.
    static func prefixLength(_ paragraph: String) -> Int {
        switch listKind(paragraph) {
        case .bullet: bullet.count
        case .ordered: (orderedNumber(paragraph).map { String($0).count } ?? 0) + 2
        case .task: taskOpen.count
        case .quote: quoteBar.count
        case nil: 0
        }
    }

    /// The prefix of an item.
    static func prefix(_ kind: ListKind, number: Int) -> String {
        switch kind {
        case .bullet: bullet
        case .ordered: "\(number). "
        case .task: taskOpen
        case .quote: quoteBar
        }
    }

    /// Makes the paragraphs of the selection items of a list, numbered from
    /// 1, or plain paragraphs with `nil`.
    static func setList(_ kind: ListKind?, in text: inout AttributedString, selection: inout AttributedTextSelection) {
        let range = Self.bounds(selection, in: text)
        let paragraphs = Self.paragraphs(covering: range, in: text)
        let numbered = paragraphs.enumerated().map { ($0.offset + 1, $0.element) }
        text.transform(updating: &selection) { document in
            for (number, paragraph) in numbered.reversed() {
                let existing = prefixLength(string(paragraph, in: document))
                let end = document.characters.index(paragraph.lowerBound, offsetBy: existing)
                let attributes = document[paragraph].runs.first?.attributes ?? AttributeContainer()
                let replacement = kind.map { AttributedString(prefix($0, number: number), attributes: attributes) }
                document.replaceSubrange(paragraph.lowerBound..<end, with: replacement ?? AttributedString())
            }
        }
    }

    /// After a line break was typed at the end of a list item, starts the
    /// next item, or ends the list when the item was empty.
    ///
    /// - Returns: Whether the text changed.
    @discardableResult
    static func continueList(
        after old: AttributedString,
        in text: inout AttributedString,
        selection: inout AttributedTextSelection
    ) -> Bool {
        let caret = Self.bounds(selection, in: text)
        guard caret.isEmpty, text.characters.count == old.characters.count + 1,
            caret.lowerBound > text.startIndex
        else { return false }
        let breakIndex = text.characters.index(before: caret.lowerBound)
        guard text.characters[breakIndex] == "\n",
            let previous = paragraphs(covering: breakIndex..<breakIndex, in: text).first
        else { return false }
        let line = string(previous, in: text)
        guard let kind = listKind(line) else { return false }
        if line.count == prefixLength(line) {
            let offset = text.characters.distance(from: text.startIndex, to: previous.lowerBound)
            text.replaceSubrange(previous.lowerBound..<caret.lowerBound, with: AttributedString())
            selection = AttributedTextSelection(
                insertionPoint: text.characters.index(text.startIndex, offsetBy: offset)
            )
            return true
        }
        let next = prefix(kind, number: (orderedNumber(line) ?? 0) + 1)
        let offset = text.characters.distance(from: text.startIndex, to: caret.lowerBound)
        text.insert(AttributedString(next), at: caret.lowerBound)
        selection = AttributedTextSelection(
            insertionPoint: text.characters.index(text.startIndex, offsetBy: offset + next.count)
        )
        return true
    }

    // MARK: Length limit

    /// Keeps a document within `limit` characters after an edit: the text
    /// typed or pasted past the limit is cut from the end of the inserted
    /// run, and the insertion point moves to the cut. A document that was
    /// already over the limit (set by the app) may shrink but not grow.
    ///
    /// - Returns: Whether the text was cut.
    @discardableResult
    static func clamp(
        _ text: inout AttributedString,
        after old: AttributedString,
        limit: Int,
        selection: inout AttributedTextSelection
    ) -> Bool {
        let total = text.characters.count
        let before = old.characters.count
        guard limit >= 0, total > limit, total > before else { return false }
        let target = max(limit, before)
        let excess = total - target
        let newChars = Array(text.characters)
        let oldChars = Array(old.characters)
        var prefix = 0
        while prefix < min(newChars.count, oldChars.count), newChars[prefix] == oldChars[prefix] { prefix += 1 }
        var suffix = 0
        let room = min(newChars.count, oldChars.count) - prefix
        while suffix < room, newChars[newChars.count - 1 - suffix] == oldChars[oldChars.count - 1 - suffix] {
            suffix += 1
        }
        let insertedEnd = total - suffix
        let cutStart = max(prefix, insertedEnd - excess)
        let start = text.characters.index(text.startIndex, offsetBy: cutStart)
        let end = text.characters.index(text.startIndex, offsetBy: insertedEnd)
        text.removeSubrange(start..<end)
        selection = AttributedTextSelection(insertionPoint: text.characters.index(text.startIndex, offsetBy: cutStart))
        return true
    }

    // MARK: Links and mentions

    /// Links the selected text to a URL, or removes the link with `nil`.
    /// At an insertion point a URL is inserted as linked text.
    static func setLink(_ url: URL?, in text: inout AttributedString, selection: inout AttributedTextSelection) {
        let range = Self.bounds(selection, in: text)
        if range.isEmpty, let url {
            let offset = text.characters.distance(from: text.startIndex, to: range.lowerBound)
            var link = AttributedString(url.absoluteString)
            link.link = url
            text.insert(link, at: range.lowerBound)
            selection = AttributedTextSelection(
                insertionPoint: text.characters.index(text.startIndex, offsetBy: offset + link.characters.count)
            )
            return
        }
        text.transformAttributes(in: &selection) { $0.link = url }
    }

    /// The mention being typed before the insertion point, with its range in the document.
    static func mention(
        in text: AttributedString,
        selection: AttributedTextSelection,
        triggers: Set<Character>
    ) -> (query: MonarchMentionQuery, range: Range<AttributedString.Index>)? {
        let caret = Self.bounds(selection, in: text)
        guard caret.isEmpty else { return nil }
        let before = string(text.startIndex..<caret.lowerBound, in: text)
        guard let query = MonarchMentionQuery.find(in: before, triggers: triggers) else { return nil }
        let offset = before.distance(from: before.startIndex, to: query.range.lowerBound)
        return (query, text.characters.index(text.startIndex, offsetBy: offset)..<caret.lowerBound)
    }

    /// Replaces a mention being typed with the trigger, the label and a space.
    static func insertMention(
        _ item: MonarchMentionItem,
        trigger: Character,
        replacing range: Range<AttributedString.Index>,
        in text: inout AttributedString,
        selection: inout AttributedTextSelection
    ) {
        let offset = text.characters.distance(from: text.startIndex, to: range.lowerBound)
        let inserted = "\(trigger)\(item.label) "
        text.replaceSubrange(range, with: AttributedString(inserted))
        selection = AttributedTextSelection(
            insertionPoint: text.characters.index(text.startIndex, offsetBy: offset + inserted.count)
        )
    }
}
