import Foundation

/// The document operations of ``MonarchBlockEditor``, as pure functions on
/// an array of blocks, so they are tested without a view. New blocks take
/// their id from `newID`, which tests pass to stay deterministic.
enum MonarchBlockEditorDocument {
    /// A direction to move a block in.
    enum Direction: Equatable {
        /// Towards the start of the document.
        case up
        /// Towards the end of the document.
        case down
    }

    /// The position of a block, or `nil`.
    static func index(of id: String, in blocks: [MonarchBlock]) -> Int? {
        blocks.firstIndex { $0.id == id }
    }

    /// Moves a block one step. Returns whether it moved.
    @discardableResult
    static func move(_ id: String, _ direction: Direction, in blocks: inout [MonarchBlock]) -> Bool {
        guard let index = index(of: id, in: blocks) else { return false }
        let target = direction == .up ? index - 1 : index + 1
        guard blocks.indices.contains(target) else { return false }
        blocks.swapAt(index, target)
        return true
    }

    /// Moves blocks the way `List`'s `onMove` reports it.
    static func move(fromOffsets source: IndexSet, toOffset destination: Int, in blocks: inout [MonarchBlock]) {
        let moving = source.filter { blocks.indices.contains($0) }.map { blocks[$0] }
        guard !moving.isEmpty else { return }
        let before = source.filter { $0 < destination }.count
        for offset in source.sorted(by: >) where blocks.indices.contains(offset) {
            blocks.remove(at: offset)
        }
        let insertAt = min(max(destination - before, 0), blocks.count)
        blocks.insert(contentsOf: moving, at: insertAt)
    }

    /// Duplicates blocks, each copy right after its original. Returns the ids of the copies, in document order.
    @discardableResult
    static func duplicate(_ ids: Set<String>, in blocks: inout [MonarchBlock], newID: () -> String) -> [String] {
        var result: [MonarchBlock] = []
        var copies: [String] = []
        for block in blocks {
            result.append(block)
            if ids.contains(block.id) {
                var copy = block
                copy.id = newID()
                copies.append(copy.id)
                result.append(copy)
            }
        }
        blocks = result
        return copies
    }

    /// Deletes blocks. A document is never empty: deleting every block
    /// leaves one empty paragraph. Returns the id of the block to focus
    /// next: the one before the first deleted block, or else the first block.
    @discardableResult
    static func delete(_ ids: Set<String>, in blocks: inout [MonarchBlock], newID: () -> String) -> String? {
        guard let first = blocks.firstIndex(where: { ids.contains($0.id) }) else { return nil }
        let previous = blocks[..<first].last { !ids.contains($0.id) }
        blocks.removeAll { ids.contains($0.id) }
        if blocks.isEmpty { blocks = [MonarchBlock(id: newID())] }
        return previous?.id ?? blocks.first?.id
    }

    /// Inserts a block after another, or at the end when `id` is `nil` or unknown.
    static func insert(_ block: MonarchBlock, after id: String?, in blocks: inout [MonarchBlock]) {
        if let id, let index = index(of: id, in: blocks) {
            blocks.insert(block, at: index + 1)
        } else {
            blocks.append(block)
        }
    }

    /// Changes what a block is. Its text is kept; a table gets an empty
    /// table when it has none; a checklist item starts unticked.
    static func setKind(_ kind: MonarchBlockKind, for id: String, in blocks: inout [MonarchBlock]) {
        guard let index = index(of: id, in: blocks) else { return }
        if kind == .taskList, blocks[index].kind != .taskList { blocks[index].isChecked = false }
        if kind == .table, blocks[index].table == nil { blocks[index].table = MonarchTableBlockData() }
        if kind == .toggle { blocks[index].isExpanded = true }
        blocks[index].kind = kind
    }

    /// The kind a block becomes when a text tool is pressed on it: lists,
    /// quote and code toggle back to a paragraph; text styles apply.
    static func toggledKind(current: MonarchBlockKind, pressed: MonarchBlockKind) -> MonarchBlockKind {
        switch pressed {
        case .bulletList, .orderedList, .taskList, .blockquote, .codeBlock:
            current == pressed ? .paragraph : pressed
        default:
            pressed
        }
    }

    /// The kinds whose Return starts a new block. Code and callouts keep
    /// line breaks; toggle summaries and embed links are one line.
    static func splitsOnReturn(_ kind: MonarchBlockKind) -> Bool {
        switch kind {
        case .paragraph, .heading, .bulletList, .orderedList, .taskList, .blockquote: true
        case .codeBlock, .callout, .toggle, .table, .embed: false
        }
    }

    /// Whether a kind's text is one line, so a line break is removed.
    static func isSingleLine(_ kind: MonarchBlockKind) -> Bool {
        kind == .toggle || kind == .embed
    }

    /// What Return in a block does. A line break in a paragraph, heading,
    /// list item or quote splits it: the text before stays, the text after
    /// starts a new block (another item for lists, a paragraph otherwise).
    /// Return in an empty list item ends the list: it becomes a paragraph.
    /// Toggle summaries and embed links lose the line break. Returns the id
    /// of the block to focus, or `nil` when nothing changed.
    @discardableResult
    static func applyReturn(in id: String, blocks: inout [MonarchBlock], newID: () -> String) -> String? {
        guard let index = index(of: id, in: blocks) else { return nil }
        let block = blocks[index]
        guard let breakIndex = block.text.firstIndex(where: \.isNewline) else { return nil }
        if isSingleLine(block.kind) {
            blocks[index].text = block.text.filter { !$0.isNewline }
            return id
        }
        guard splitsOnReturn(block.kind) else { return nil }
        let before = String(block.text[..<breakIndex])
        let after = String(block.text[block.text.index(after: breakIndex)...])
        let isList = block.kind == .bulletList || block.kind == .orderedList || block.kind == .taskList
        if isList, before.isEmpty, after.isEmpty {
            blocks[index].text = ""
            blocks[index].kind = .paragraph
            return id
        }
        blocks[index].text = before
        let next = MonarchBlock(id: newID(), kind: isList ? block.kind : .paragraph, text: after)
        blocks.insert(next, at: index + 1)
        return next.id
    }

    /// The Markdown shortcut at the start of a paragraph: "# " to "### "
    /// for headings, "- " or "* " for a bulleted list, "1. " for a numbered
    /// list, "[] " or "[ ] " for a checklist, "> " for a quote and three
    /// backticks for code. Returns the new kind and the text without the
    /// shortcut, or `nil` when there is none or the kind is not offered.
    static func shortcut(in text: String, kinds: [MonarchBlockKind]) -> (kind: MonarchBlockKind, text: String)? {
        let table: [(String, MonarchBlockKind)] = [
            ("### ", .heading(3)), ("## ", .heading(2)), ("# ", .heading(1)),
            ("- ", .bulletList), ("* ", .bulletList), ("1. ", .orderedList),
            ("[ ] ", .taskList), ("[] ", .taskList), ("> ", .blockquote), ("```", .codeBlock),
        ]
        for (prefix, kind) in table where text.hasPrefix(prefix) && kinds.contains(kind) {
            return (kind, String(text.dropFirst(prefix.count)))
        }
        return nil
    }

    /// The position of a list item in its run of items of the same kind,
    /// from 1. Numbered lists show it; every list speaks it.
    static func listPosition(at index: Int, in blocks: [MonarchBlock]) -> Int {
        guard blocks.indices.contains(index) else { return 1 }
        let kind = blocks[index].kind
        var position = 1
        var cursor = index - 1
        while cursor >= 0, blocks[cursor].kind == kind {
            position += 1
            cursor -= 1
        }
        return position
    }

    /// What the change from `old` to `new` is for undo: the id of the one
    /// block whose text or detail changed (so typing in it is undone as one
    /// step), or `nil` for any other change.
    static func coalescingKey(old: [MonarchBlock], new: [MonarchBlock]) -> String? {
        guard old.count == new.count, old.map(\.id) == new.map(\.id) else { return nil }
        var changed: String?
        for (before, after) in zip(old, new) where before != after {
            guard changed == nil, before.kind == after.kind, before.isChecked == after.isChecked,
                before.isExpanded == after.isExpanded, before.table == after.table
            else { return nil }
            changed = after.id
        }
        return changed
    }
}
