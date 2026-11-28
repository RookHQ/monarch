import SwiftUI

/// A block document editor: paragraphs, headings, lists, checklists,
/// quotes, code, callouts, toggles, tables and embeds, each block its own
/// row of a standard `List`.
///
/// ```swift
/// @State private var blocks = [
///     MonarchBlock(kind: .heading(1), text: "Launch plan"),
///     MonarchBlock(text: "Type / for blocks."),
/// ]
///
/// MonarchBlockEditor("Launch plan", blocks: $blocks)
/// ```
///
/// The editor fills the space it is given, like a `List`: make it the main
/// content of a screen. Each block is a standard `TextField` that grows with
/// its text, in the style of its kind; callouts, toggles, tables and embeds
/// are ``MonarchCalloutBlock``, ``MonarchToggleBlock``, ``MonarchTableBlock``
/// and ``MonarchEmbedBlock``.
///
/// - Typing `/` at the start of a word shows ``MonarchSlashMenu`` above the
///   keyboard; choosing an item turns the block into that kind, or inserts
///   the new block after it when the block already has text. Product items
///   from `slashItems` go to `onSlashItem`.
/// - Return in a paragraph, heading, list item or quote starts a new block;
///   Return in an empty list item ends the list. Code and callouts keep line
///   breaks. Markdown shortcuts at the start of a paragraph ("# ", "- ",
///   "1. ", "[ ] ", "> ", three backticks) change its kind.
/// - Each block has an action button, drawn with ``MonarchDragHandle``, with
///   Turn into, the callout tone, Move up, Move down, Duplicate and Delete.
///   Swiping a block offers Duplicate and Delete; touching and holding a
///   block drags it, with the system list reordering.
/// - The toolbar (``MonarchEditorToolbar``) has undo and redo for the whole
///   document, the text styles and lists, and Select blocks, which puts the
///   list in multiple selection to duplicate or delete several blocks.
///
/// Block text is plain: inline bold, italic and links are not stored. For
/// rich text in one field, use ``MonarchRichTextEditor``. Read-only and
/// disabled editors show the blocks as text, without the toolbar, menus or
/// gestures.
///
/// Accessibility: the editor is a container named `label`. Each field is
/// named by its kind ("Heading 2", "Numbered list, item 3") and headings
/// have the header trait. Each action button is named "Block actions:
/// Heading 2" and also offers Move up and Move down as accessibility
/// actions, so moving never needs a drag. Checklist boxes are standard
/// toggles named by the item's text. List markers and the quote bar are
/// decorative. Rows and buttons are at least 44 points high and every text
/// size is followed.
@_spi(Experimental) public struct MonarchBlockEditor: View {
    private let label: String
    @Binding private var blocks: [MonarchBlock]
    private let prompt: String?
    private let kinds: [MonarchBlockKind]
    private let embedProviders: [MonarchEmbedProvider]
    private let isReadOnly: Bool
    private let showsToolbar: Bool
    private let slashItems: [MonarchSlashMenuItem]
    private let labels: MonarchBlockEditorLabels
    private let onSlashItem: (@MainActor (MonarchSlashMenuItem, String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @FocusState private var focusedBlock: String?
    @State private var currentBlock: String?
    @State private var history = MonarchBlockEditorHistory()
    @State private var skipsNextChange = false
    @State private var isSelecting = false
    @State private var selection: Set<String> = []

    private typealias Tokens = MonarchBlockEditorTokens
    private typealias Document = MonarchBlockEditorDocument

    /// A block editor.
    ///
    /// - Parameters:
    ///   - label: The editor's name for VoiceOver, already localised, such as the document's title.
    ///   - blocks: The document. It is never left empty: deleting every block leaves an empty paragraph.
    ///   - prompt: The placeholder of an empty paragraph with the caret. Built in: "Write, or type / for blocks".
    ///   - kinds: The kinds the slash menu, the toolbar and Turn into offer, in order.
    ///     `.callout(tone)` sets the tone a new callout starts with.
    ///   - embedProviders: The sites embed blocks may show in a web view.
    ///   - isReadOnly: Shows the blocks as text, without the toolbar, menus or gestures.
    ///   - showsToolbar: Shows the toolbar above the document.
    ///   - slashItems: Product items added to the slash menu after the block kinds.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSlashItem: Receives a product item and the id of the block it was typed in, after the typed command is removed.
    public init(
        _ label: String,
        blocks: Binding<[MonarchBlock]>,
        prompt: String? = nil,
        kinds: [MonarchBlockKind] = MonarchBlockKind.defaultKinds,
        embedProviders: [MonarchEmbedProvider] = MonarchEmbedProvider.defaults,
        isReadOnly: Bool = false,
        showsToolbar: Bool = true,
        slashItems: [MonarchSlashMenuItem] = [],
        labels: MonarchBlockEditorLabels = MonarchBlockEditorLabels(),
        onSlashItem: (@MainActor (MonarchSlashMenuItem, String) -> Void)? = nil
    ) {
        self.label = label
        _blocks = blocks
        self.prompt = prompt
        self.kinds = kinds
        self.embedProviders = embedProviders
        self.isReadOnly = isReadOnly
        self.showsToolbar = showsToolbar
        self.slashItems = slashItems
        self.labels = labels
        self.onSlashItem = onSlashItem
    }

    private var isEditable: Bool { !isReadOnly && isEnabled }

    private var current: MonarchBlock? {
        guard let currentBlock else { return nil }
        return blocks.first { $0.id == currentBlock }
    }

    /// The slash command typed in the block with the caret.
    private var slashQuery: MonarchSlashMenuQuery? {
        guard isEditable, !isSelecting, let focusedBlock,
            let block = blocks.first(where: { $0.id == focusedBlock }), block.kind.isText, block.kind != .codeBlock
        else { return nil }
        return MonarchSlashMenuQuery.find(in: block.text, position: .wordStart)
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: 0) {
            if isEditable, showsToolbar {
                header
                Rectangle()
                    .fill(theme.colors[role: Tokens.separatorColor])
                    .frame(height: MonarchShapes.borderWidth)
                    .accessibilityHidden(true)
            }
            list
        }
        .background(theme.colors[role: Tokens.background])
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
        .onChange(of: blocks) { old, new in changed(from: old, to: new) }
        .onAppear {
            // The toolbar starts on the first text block, so its tools are ready and show the block's style.
            if currentBlock == nil, isEditable {
                currentBlock = blocks.first { $0.kind.isText }?.id
            }
        }
        .onChange(of: focusedBlock) { _, id in
            if let id { currentBlock = id }
            history.breakCoalescing()
        }
        .onChange(of: isEditable) { _, editable in
            if !editable { isSelecting = false }
        }
    }

    // MARK: Toolbar

    @ViewBuilder private var header: some View {
        if isSelecting {
            selectionBar
        } else {
            let selectTitle = labels.select ?? MonarchBlockEditorStrings.select
            MonarchEditorToolbar(
                groups: Tokens.toolGroups(kinds: kinds, selectTitle: selectTitle),
                active: current.flatMap { Tokens.tool($0.kind) }.map { Set([$0]) } ?? [],
                disabled: disabledTools,
                labels: labels.toolbar,
                action: apply
            )
            .padding(.horizontal, Tokens.barPadding)
        }
    }

    private var disabledTools: Set<MonarchEditorTool> {
        var disabled: Set<MonarchEditorTool> = []
        if !history.canUndo { disabled.insert(.undo) }
        if !history.canRedo { disabled.insert(.redo) }
        if current?.kind.isText != true {
            disabled.formUnion(kinds.compactMap(Tokens.tool))
        }
        return disabled
    }

    private var selectionBar: some View {
        HStack(spacing: Tokens.rowSpacing) {
            Text(Tokens.selectedCount(selection.count, labels: labels))
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(labels.duplicate ?? MonarchBlockEditorStrings.duplicate) {
                duplicate(selection)
                selection = []
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .disabled(selection.isEmpty)
            Button(labels.deleteSelected ?? MonarchBlockEditorStrings.deleteSelected, role: .destructive) {
                let ids = selection
                record { _ = Document.delete(ids, in: &$0, newID: Self.newID) }
                selection = []
            }
            .buttonStyle(.monarch(.danger, size: .sm))
            .disabled(selection.isEmpty)
            Button(labels.done ?? MonarchBlockEditorStrings.done) {
                isSelecting = false
                selection = []
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
        }
        .padding(Tokens.barPadding)
    }

    // MARK: List

    private var listSelection: Binding<Set<String>>? {
        isSelecting ? $selection : nil
    }

    /// The position of each block by id; the first wins when ids repeat.
    private var positions: [String: Int] {
        var result: [String: Int] = [:]
        for (index, block) in blocks.enumerated() where result[block.id] == nil {
            result[block.id] = index
        }
        return result
    }

    private var moveAction: ((IndexSet, Int) -> Void)? {
        guard isEditable else { return nil }
        return { source, destination in moveRows(source, destination) }
    }

    private var list: some View {
        let positions = positions
        return List(selection: listSelection) {
            ForEach($blocks) { (block: Binding<MonarchBlock>) in
                listRow(block, index: positions[block.wrappedValue.id] ?? 0)
            }
            .onMove(perform: moveAction)
            if isEditable, !isSelecting {
                addButton
                    .listRowSeparator(.hidden)
                    .listRowBackground(theme.colors[role: Tokens.background])
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        #if os(iOS)
            .environment(\.editMode, .constant(isSelecting ? .active : .inactive))
        #endif
        .safeAreaInset(edge: .bottom, spacing: 0) { slashMenu }
    }

    private var slashMenu: some View {
        MonarchSlashMenu(
            slashQuery,
            items: Tokens.slashItems(kinds: kinds, extra: slashItems, labels: labels),
            groupOrder: Tokens.groupOrder(labels: labels),
            labels: labels.slashMenu,
            onSelect: chooseSlashItem
        )
    }

    private var rowInsets: EdgeInsets {
        EdgeInsets(
            top: Tokens.rowVerticalPadding,
            leading: Tokens.rowHorizontalPadding,
            bottom: Tokens.rowVerticalPadding,
            trailing: Tokens.rowHorizontalPadding
        )
    }

    private func listRow(_ block: Binding<MonarchBlock>, index: Int) -> some View {
        let id = block.wrappedValue.id
        return row(block: block, index: index)
            .tag(id)
            .listRowInsets(rowInsets)
            .listRowSeparator(.hidden)
            .listRowBackground(theme.colors[role: Tokens.background])
            .swipeActions(edge: .trailing) { swipeButtons(id: id) }
            .moveDisabled(!isEditable)
    }

    @ViewBuilder private func swipeButtons(id: String) -> some View {
        if isEditable, !isSelecting {
            Button(labels.deleteBlock ?? MonarchBlockEditorStrings.deleteBlock, role: .destructive) {
                delete(id)
            }
            Button(labels.duplicate ?? MonarchBlockEditorStrings.duplicate) {
                duplicate([id])
            }
        }
    }

    private func row(block: Binding<MonarchBlock>, index: Int) -> some View {
        let id = block.wrappedValue.id
        let kind = block.wrappedValue.kind
        let rowIsEditable = isEditable && !isSelecting
        return HStack(alignment: .top, spacing: Tokens.rowSpacing) {
            if rowIsEditable {
                actionsMenu(id: id, kind: kind, index: index)
            }
            MonarchBlockEditorRow(
                block: block,
                fieldLabel: Tokens.fieldLabel(at: index, in: blocks, labels: labels),
                marker: Tokens.marker(at: index, in: blocks),
                prompt: rowPrompt(block.wrappedValue),
                isEditable: rowIsEditable,
                focus: $focusedBlock,
                embedProviders: embedProviders,
                labels: labels,
                onDeleteTable: { delete(id) },
                onTone: { tone in record { Document.setKind(.callout(tone), for: id, in: &$0) } }
            )
        }
    }

    private func rowPrompt(_ block: MonarchBlock) -> String? {
        guard block.text.isEmpty, block.kind.isText else { return nil }
        if block.kind == .paragraph {
            return focusedBlock == block.id || blocks.count == 1 ? prompt ?? MonarchBlockEditorStrings.prompt : nil
        }
        return Tokens.name(block.kind, labels: labels)
    }

    private func actionsMenu(id: String, kind: MonarchBlockKind, index: Int) -> some View {
        let moveUp = labels.moveUp ?? MonarchBlockEditorStrings.moveUp
        let moveDown = labels.moveDown ?? MonarchBlockEditorStrings.moveDown
        let handleLabel = Tokens.handleLabel(kind, labels: labels)
        return Menu {
            Menu(labels.turnInto ?? MonarchBlockEditorStrings.turnInto) {
                ForEach(kinds, id: \.self) { option in
                    Button(Tokens.name(option, labels: labels), systemImage: Tokens.symbol(option)) {
                        record { Document.setKind(option, for: id, in: &$0) }
                    }
                }
            }
            if case .callout(let tone) = kind {
                Picker(
                    labels.tone ?? MonarchBlockEditorStrings.tone,
                    selection: Binding(
                        get: { tone },
                        set: { newTone in record { Document.setKind(.callout(newTone), for: id, in: &$0) } }
                    )
                ) {
                    ForEach(MonarchTone.allCases, id: \.self) { option in
                        Text(Tokens.toneName(option, labels: labels)).tag(option)
                    }
                }
            }
            Button(moveUp, systemImage: "arrow.up") { move(id, .up) }
                .disabled(index == 0)
            Button(moveDown, systemImage: "arrow.down") { move(id, .down) }
                .disabled(index >= blocks.count - 1)
            Button(labels.duplicate ?? MonarchBlockEditorStrings.duplicate, systemImage: "plus.square.on.square") {
                duplicate([id])
            }
            Button(
                labels.deleteBlock ?? MonarchBlockEditorStrings.deleteBlock,
                systemImage: "trash",
                role: .destructive
            ) {
                delete(id)
            }
        } label: {
            MonarchDragHandle(handleLabel, size: .sm)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(handleLabel)
        .accessibilityHint(labels.handleHint ?? MonarchBlockEditorStrings.handleHint)
        .accessibilityActions {
            Button(moveUp) { move(id, .up) }
            Button(moveDown) { move(id, .down) }
        }
    }

    private var addButton: some View {
        Button {
            addBlock()
        } label: {
            Label(labels.addBlock ?? MonarchBlockEditorStrings.addBlock, systemImage: "plus")
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
    }

    // MARK: Changes

    private static func newID() -> String { UUID().uuidString }

    /// Runs a document change; the history records it through `changed(from:to:)`.
    private func record(_ change: (inout [MonarchBlock]) -> Void) {
        var updated = blocks
        change(&updated)
        if updated != blocks { blocks = updated }
    }

    private func changed(from old: [MonarchBlock], to new: [MonarchBlock]) {
        if skipsNextChange {
            skipsNextChange = false
            return
        }
        history.record(old, key: Document.coalescingKey(old: old, new: new))
        followUp(after: old, in: new)
    }

    /// Return, Markdown shortcuts and an emptied document, applied right
    /// after the edit that caused them, as part of the same undo step.
    private func followUp(after old: [MonarchBlock], in new: [MonarchBlock]) {
        var updated = new
        var focus: String?
        if updated.isEmpty, isEditable {
            updated = [MonarchBlock(id: Self.newID())]
            focus = updated.first?.id
        } else if let id = focusedBlock, let index = Document.index(of: id, in: updated) {
            let block = updated[index]
            let before = old.first { $0.id == id }
            if before?.text != block.text {
                if let next = Document.applyReturn(in: id, blocks: &updated, newID: Self.newID) {
                    focus = next
                } else if block.kind == .paragraph, let shortcut = Document.shortcut(in: block.text, kinds: kinds) {
                    Document.setKind(shortcut.kind, for: id, in: &updated)
                    updated[index].text = shortcut.text
                }
            }
        }
        guard updated != new else { return }
        skipsNextChange = true
        blocks = updated
        if let focus { focusedBlock = focus }
    }

    private func apply(_ tool: MonarchEditorTool) {
        switch tool {
        case .undo:
            guard let previous = history.undo(blocks) else { return }
            restore(previous)
        case .redo:
            guard let next = history.redo(blocks) else { return }
            restore(next)
        case .custom(let id, _, _) where id == Tokens.selectToolID:
            focusedBlock = nil
            selection = []
            isSelecting = true
        default:
            guard let pressed = Tokens.kind(tool), let block = current, block.kind.isText else { return }
            let kind = Document.toggledKind(current: block.kind, pressed: pressed)
            record { Document.setKind(kind, for: block.id, in: &$0) }
        }
    }

    private func restore(_ document: [MonarchBlock]) {
        guard document != blocks else { return }
        skipsNextChange = true
        blocks = document
    }

    private func duplicate(_ ids: Set<String>) {
        record { _ = Document.duplicate(ids, in: &$0, newID: Self.newID) }
    }

    private func moveRows(_ source: IndexSet, _ destination: Int) {
        record { Document.move(fromOffsets: source, toOffset: destination, in: &$0) }
    }

    private func move(_ id: String, _ direction: Document.Direction) {
        record { _ = Document.move(id, direction, in: &$0) }
    }

    private func delete(_ id: String) {
        var next: String?
        record { next = Document.delete([id], in: &$0, newID: Self.newID) }
        if focusedBlock == id { focusedBlock = next }
    }

    private func addBlock() {
        if let last = blocks.last, last.kind == .paragraph, last.text.isEmpty {
            focusedBlock = last.id
            return
        }
        let block = MonarchBlock(id: Self.newID())
        record { Document.insert(block, after: nil, in: &$0) }
        focusedBlock = block.id
    }

    private func chooseSlashItem(_ item: MonarchSlashMenuItem) {
        guard let id = focusedBlock, let query = slashQuery, let index = Document.index(of: id, in: blocks) else {
            return
        }
        var updated = blocks
        updated[index].text = query.replacing(in: updated[index].text)
        if let kind = Tokens.kind(slashID: item.id, kinds: kinds) {
            let remaining = updated[index].text.trimmingCharacters(in: .whitespacesAndNewlines)
            if kind.isText || remaining.isEmpty {
                if !kind.isText { updated[index].text = "" }
                Document.setKind(kind, for: id, in: &updated)
            } else {
                var block = MonarchBlock(id: Self.newID(), kind: kind)
                if kind == .table { block.table = MonarchTableBlockData() }
                Document.insert(block, after: id, in: &updated)
                focusedBlock = block.id
            }
            blocks = updated
        } else {
            blocks = updated
            onSlashItem?(item, id)
        }
    }
}

#if DEBUG
    #Preview("MonarchBlockEditor") {
        let blocks = [
            MonarchBlock(kind: .heading(1), text: "Launch plan"),
            MonarchBlock(text: "Everything we need before the release."),
            MonarchBlock(kind: .bulletList, text: "Store listing"),
            MonarchBlock(kind: .orderedList, text: "Freeze the build"),
            MonarchBlock(kind: .taskList, text: "Write the changelog", isChecked: true),
            MonarchBlock(kind: .blockquote, text: "Ship small, ship often."),
            MonarchBlock(kind: .callout(.warning), text: "The store review takes two days."),
            MonarchBlock(kind: .codeBlock, text: "make release"),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchBlockEditor("Launch plan", blocks: .constant(blocks))
                MonarchBlockEditor("Published plan", blocks: .constant(blocks), isReadOnly: true)
            }
        }
    }
#endif
