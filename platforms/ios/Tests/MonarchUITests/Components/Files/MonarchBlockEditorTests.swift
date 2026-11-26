import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchBlockEditorFixtures: ComponentFixtures {
    static let id = "block-editor"

    /// The height of the editor in fixtures; a List takes the height it is given.
    private static let height: CGFloat = 640

    static let everyKind: [MonarchBlock] = [
        MonarchBlock(id: "h1", kind: .heading(1), text: "Launch plan"),
        MonarchBlock(id: "h2", kind: .heading(2), text: "Before the release"),
        MonarchBlock(id: "h3", kind: .heading(3), text: "Store"),
        MonarchBlock(id: "p", text: "Everything we need before the release, written down in one place."),
        MonarchBlock(id: "b1", kind: .bulletList, text: "Screenshots"),
        MonarchBlock(id: "b2", kind: .bulletList, text: "Description"),
        MonarchBlock(id: "o1", kind: .orderedList, text: "Freeze the build"),
        MonarchBlock(id: "o2", kind: .orderedList, text: "Submit for review"),
        MonarchBlock(id: "t1", kind: .taskList, text: "Write the changelog", isChecked: true),
        MonarchBlock(id: "t2", kind: .taskList, text: ""),
        MonarchBlock(id: "q", kind: .blockquote, text: "Ship small, ship often."),
        MonarchBlock(id: "c", kind: .codeBlock, text: "make release\nmake upload"),
    ]

    static let customBlocks: [MonarchBlock] = [
        MonarchBlock(id: "n", kind: .callout(.neutral), text: "A neutral note."),
        MonarchBlock(id: "i", kind: .callout(.info), text: "Review takes two days."),
        MonarchBlock(id: "s", kind: .callout(.success), text: "Build passed."),
        MonarchBlock(id: "w", kind: .callout(.warning), text: "The certificate expires soon."),
        MonarchBlock(id: "d", kind: .callout(.danger), text: "The upload failed."),
        MonarchBlock(id: "tg", kind: .toggle, text: "Rollback steps", detail: "Revert the tag and redeploy."),
        MonarchBlock(id: "tc", kind: .toggle, text: "Collapsed", detail: "Hidden", isExpanded: false),
        MonarchBlock(
            id: "tb",
            kind: .table,
            table: MonarchTableBlockData(rows: [["Platform", "Status"], ["iOS", "Ready"]])
        ),
        MonarchBlock(id: "tn", kind: .table),
        MonarchBlock(id: "e", kind: .embed, text: "https://example.com/demo", detail: "The demo"),
    ]

    static let fixtures = [
        Fixture("every text kind, editable") {
            MonarchBlockEditor("Launch plan", blocks: .constant(everyKind)).frame(height: height)
        },
        Fixture("custom blocks and every callout tone") {
            MonarchBlockEditor("Notes", blocks: .constant(customBlocks)).frame(height: height)
        },
        Fixture("read-only") {
            MonarchBlockEditor("Published plan", blocks: .constant(everyKind + customBlocks), isReadOnly: true)
                .frame(height: height)
        },
        Fixture("disabled") {
            MonarchBlockEditor("Plan", blocks: .constant(everyKind)).disabled(true).frame(height: height)
        },
        Fixture("empty document with prompt, no toolbar") {
            MonarchBlockEditor(
                "New page",
                blocks: .constant([MonarchBlock(id: "only")]),
                prompt: "Start writing",
                showsToolbar: false
            )
            .frame(height: height)
        },
        Fixture("no blocks at all") {
            MonarchBlockEditor("Empty", blocks: .constant([])).frame(height: height)
        },
        Fixture("fewer kinds, product items, custom labels") {
            MonarchBlockEditor(
                "Seite",
                blocks: .constant([MonarchBlock(id: "a", kind: .bulletList, text: "Punkt")]),
                kinds: [.paragraph, .heading(2), .bulletList],
                slashItems: [MonarchSlashMenuItem(id: "date", label: "Datum", systemImage: "calendar")],
                labels: MonarchBlockEditorLabels(
                    handle: { "Aktionen: \($0)" },
                    blockNames: [.bulletList: "Aufzählung"],
                    addBlock: "Block hinzufügen"
                ),
                onSlashItem: { _, _ in }
            )
            .frame(height: height)
        },
        Fixture("long text") {
            MonarchBlockEditor(
                "Long",
                blocks: .constant([
                    MonarchBlock(
                        id: "long",
                        kind: .heading(2),
                        text: String(repeating: "A heading long enough to wrap onto several lines ", count: 4)
                    ),
                    MonarchBlock(id: "longer", text: String(repeating: "Body text that keeps going. ", count: 30)),
                ])
            )
            .frame(height: height)
        },
    ]
}

@Suite("MonarchBlockEditor")
struct MonarchBlockEditorTests {
    private typealias Document = MonarchBlockEditorDocument
    private typealias Tokens = MonarchBlockEditorTokens
    private let labels = MonarchBlockEditorLabels()

    /// Deterministic ids: "n1", "n2", …
    private final class IDs {
        private var next = 0
        func make() -> String {
            next += 1
            return "n\(next)"
        }
    }

    private func shortcut(_ text: String, kinds: [MonarchBlockKind] = MonarchBlockKind.defaultKinds) -> MonarchBlock? {
        Document.shortcut(in: text, kinds: kinds).map { MonarchBlock(id: "x", kind: $0.kind, text: $0.text) }
    }

    private func doc(_ ids: String...) -> [MonarchBlock] {
        ids.map { MonarchBlock(id: $0, text: $0) }
    }

    // MARK: Document

    @Test("Blocks move one step and stop at the ends")
    func moveStep() {
        var blocks = doc("a", "b", "c")
        let result1 = Document.move("a", .down, in: &blocks)
        #expect(result1)
        #expect(blocks.map(\.id) == ["b", "a", "c"])
        let result2 = Document.move("a", .up, in: &blocks)
        #expect(result2)
        let result3 = Document.move("a", .up, in: &blocks)
        #expect(!result3)
        let result4 = Document.move("c", .down, in: &blocks)
        #expect(!result4)
        let result5 = Document.move("missing", .down, in: &blocks)
        #expect(!result5)
        #expect(blocks.map(\.id) == ["a", "b", "c"])
    }

    @Test("List reordering offsets move blocks like Array.move")
    func moveOffsets() {
        var blocks = doc("a", "b", "c", "d")
        Document.move(fromOffsets: [0], toOffset: 3, in: &blocks)
        #expect(blocks.map(\.id) == ["b", "c", "a", "d"])
        Document.move(fromOffsets: [3], toOffset: 0, in: &blocks)
        #expect(blocks.map(\.id) == ["d", "b", "c", "a"])
        Document.move(fromOffsets: [0, 2], toOffset: 4, in: &blocks)
        #expect(blocks.map(\.id) == ["b", "a", "d", "c"])
    }

    @Test("Duplicates follow their originals with new ids")
    func duplicate() {
        let ids = IDs()
        var blocks = doc("a", "b", "c")
        let copies = Document.duplicate(["a", "c"], in: &blocks, newID: ids.make)
        #expect(copies == ["n1", "n2"])
        #expect(blocks.map(\.id) == ["a", "n1", "b", "c", "n2"])
        #expect(blocks[1].text == "a")
    }

    @Test("Deleting focuses the previous block and never leaves the document empty")
    func delete() {
        let ids = IDs()
        var blocks = doc("a", "b", "c")
        let result6 = Document.delete(["b"], in: &blocks, newID: ids.make)
        #expect(result6 == "a")
        #expect(blocks.map(\.id) == ["a", "c"])
        let result7 = Document.delete(["a"], in: &blocks, newID: ids.make)
        #expect(result7 == "c")
        let result8 = Document.delete(["c"], in: &blocks, newID: ids.make)
        #expect(result8 == "n1")
        #expect(blocks == [MonarchBlock(id: "n1")])
        let result9 = Document.delete(["missing"], in: &blocks, newID: ids.make)
        #expect(result9 == nil)
    }

    @Test("Changing the kind keeps the text and prepares tables, checklists and toggles")
    func setKind() {
        var blocks = [MonarchBlock(id: "a", text: "Hello", isChecked: true, isExpanded: false)]
        Document.setKind(.taskList, for: "a", in: &blocks)
        #expect(blocks[0].kind == .taskList && !blocks[0].isChecked && blocks[0].text == "Hello")
        Document.setKind(.table, for: "a", in: &blocks)
        #expect(blocks[0].table == MonarchTableBlockData())
        Document.setKind(.toggle, for: "a", in: &blocks)
        #expect(blocks[0].isExpanded)
    }

    @Test("Toolbar list, quote and code tools toggle back to a paragraph")
    func toggledKind() {
        #expect(Document.toggledKind(current: .bulletList, pressed: .bulletList) == .paragraph)
        #expect(Document.toggledKind(current: .paragraph, pressed: .bulletList) == .bulletList)
        #expect(Document.toggledKind(current: .codeBlock, pressed: .codeBlock) == .paragraph)
        #expect(Document.toggledKind(current: .heading(2), pressed: .heading(2)) == .heading(2))
        #expect(Document.toggledKind(current: .orderedList, pressed: .paragraph) == .paragraph)
    }

    @Test("Return splits text blocks; list items continue the list")
    func returnSplits() {
        let ids = IDs()
        var blocks = [MonarchBlock(id: "a", kind: .heading(1), text: "Title\nBody")]
        let result10 = Document.applyReturn(in: "a", blocks: &blocks, newID: ids.make)
        #expect(result10 == "n1")
        #expect(blocks.map(\.text) == ["Title", "Body"])
        #expect(blocks.map(\.kind) == [.heading(1), .paragraph])
        var list = [MonarchBlock(id: "l", kind: .taskList, text: "One\n", isChecked: true)]
        let result11 = Document.applyReturn(in: "l", blocks: &list, newID: ids.make)
        #expect(result11 == "n2")
        #expect(list[1].kind == .taskList && !list[1].isChecked && list[1].text.isEmpty)
    }

    @Test("Return in an empty list item ends the list")
    func returnEndsList() {
        let ids = IDs()
        var blocks = [MonarchBlock(id: "l", kind: .bulletList, text: "\n")]
        let result12 = Document.applyReturn(in: "l", blocks: &blocks, newID: ids.make)
        #expect(result12 == "l")
        #expect(blocks == [MonarchBlock(id: "l", kind: .paragraph)])
    }

    @Test("Code and callouts keep line breaks; toggle summaries and links lose them")
    func returnKeeps() {
        let ids = IDs()
        var blocks = [
            MonarchBlock(id: "c", kind: .codeBlock, text: "a\nb"),
            MonarchBlock(id: "k", kind: .callout(.info), text: "a\nb"),
            MonarchBlock(id: "t", kind: .toggle, text: "Sum\nmary"),
            MonarchBlock(id: "e", kind: .embed, text: "https://a.b\n"),
            MonarchBlock(id: "p", text: "no break"),
        ]
        let result13 = Document.applyReturn(in: "c", blocks: &blocks, newID: ids.make)
        #expect(result13 == nil)
        let result14 = Document.applyReturn(in: "k", blocks: &blocks, newID: ids.make)
        #expect(result14 == nil)
        let result15 = Document.applyReturn(in: "t", blocks: &blocks, newID: ids.make)
        #expect(result15 == "t")
        let result16 = Document.applyReturn(in: "e", blocks: &blocks, newID: ids.make)
        #expect(result16 == "e")
        let result17 = Document.applyReturn(in: "p", blocks: &blocks, newID: ids.make)
        #expect(result17 == nil)
        #expect(blocks.map(\.text) == ["a\nb", "a\nb", "Summary", "https://a.b", "no break"])
    }

    @Test("Markdown shortcuts change a paragraph's kind when the kind is offered")
    func shortcuts() {
        #expect(shortcut("# Title") == MonarchBlock(id: "x", kind: .heading(1), text: "Title"))
        #expect(shortcut("## ") == MonarchBlock(id: "x", kind: .heading(2), text: ""))
        #expect(shortcut("### x") == MonarchBlock(id: "x", kind: .heading(3), text: "x"))
        #expect(shortcut("- item") == MonarchBlock(id: "x", kind: .bulletList, text: "item"))
        #expect(shortcut("* item") == MonarchBlock(id: "x", kind: .bulletList, text: "item"))
        #expect(shortcut("1. first") == MonarchBlock(id: "x", kind: .orderedList, text: "first"))
        #expect(shortcut("[ ] todo") == MonarchBlock(id: "x", kind: .taskList, text: "todo"))
        #expect(shortcut("[] todo") == MonarchBlock(id: "x", kind: .taskList, text: "todo"))
        #expect(shortcut("> quote") == MonarchBlock(id: "x", kind: .blockquote, text: "quote"))
        #expect(shortcut("```") == MonarchBlock(id: "x", kind: .codeBlock, text: ""))
        #expect(shortcut("#hashtag") == nil)
        #expect(shortcut("- item", kinds: [.paragraph]) == nil)
    }

    @Test("List positions count the run of items of the same kind")
    func listPositions() {
        let blocks = [
            MonarchBlock(id: "1", kind: .orderedList),
            MonarchBlock(id: "2", kind: .orderedList),
            MonarchBlock(id: "3", kind: .bulletList),
            MonarchBlock(id: "4", kind: .orderedList),
        ]
        #expect((0..<4).map { Document.listPosition(at: $0, in: blocks) } == [1, 2, 1, 1])
        #expect(Tokens.marker(at: 1, in: blocks) == "2.")
        #expect(Tokens.marker(at: 2, in: blocks) == "•")
        #expect(Tokens.marker(at: 9, in: blocks) == nil)
    }

    @Test("Typing in one block coalesces for undo; other changes do not")
    func coalescing() {
        let old = doc("a", "b")
        var typed = old
        typed[0].text = "ab"
        #expect(Document.coalescingKey(old: old, new: typed) == "a")
        var detail = old
        detail[1].detail = "x"
        #expect(Document.coalescingKey(old: old, new: detail) == "b")
        var both = typed
        both[1].text = "bb"
        #expect(Document.coalescingKey(old: old, new: both) == nil)
        var kind = old
        kind[0].kind = .heading(1)
        #expect(Document.coalescingKey(old: old, new: kind) == nil)
        #expect(Document.coalescingKey(old: old, new: Array(old.reversed())) == nil)
        var checked = old
        checked[0].isChecked = true
        #expect(Document.coalescingKey(old: old, new: checked) == nil)
    }

    // MARK: History

    @Test("Undo and redo restore whole documents and typing is one step")
    func history() {
        var history = MonarchBlockEditorHistory()
        #expect(!history.canUndo && !history.canRedo)
        let v0 = doc("a")
        let v1 = [MonarchBlock(id: "a", text: "ab")]
        let v2 = [MonarchBlock(id: "a", text: "abc")]
        history.record(v0, key: "a")
        history.record(v1, key: "a")
        #expect(history.undoStack.count == 1)
        let result18 = history.undo(v2)
        #expect(result18 == v0)
        #expect(history.canRedo)
        let result19 = history.redo(v0)
        #expect(result19 == v2)
        let result20 = history.undo(v2)
        #expect(result20 == v0)
        history.record(v0, key: nil)
        #expect(!history.canRedo)
        let result21 = history.undo([])
        #expect(result21 == v0)
        let result22 = history.undo([])
        #expect(result22 == nil)
    }

    @Test("Breaking coalescing starts a new typing step; the history is capped")
    func historyLimit() {
        var history = MonarchBlockEditorHistory()
        history.record(doc("a"), key: "a")
        history.breakCoalescing()
        history.record(doc("b"), key: "a")
        #expect(history.undoStack.count == 2)
        for index in 0..<(MonarchBlockEditorHistory.limit + 10) {
            history.record(doc("x\(index)"), key: nil)
        }
        #expect(history.undoStack.count == MonarchBlockEditorHistory.limit)
        #expect(history.undoStack.last == doc("x\(MonarchBlockEditorHistory.limit + 9)"))
    }

    // MARK: Menus and toolbar

    @Test("Slash items list every offered kind with names, groups and ids, then product items")
    func slashItems() {
        let extra = MonarchSlashMenuItem(id: "date", label: "Date")
        let items = Tokens.slashItems(kinds: MonarchBlockKind.defaultKinds, extra: [extra], labels: labels)
        #expect(items.count == MonarchBlockKind.defaultKinds.count + 1)
        #expect(items.first?.label == "Text")
        #expect(items.first?.description == "Plain paragraph")
        #expect(items.first?.group == "Text")
        #expect(items.map(\.id).contains("callout-info"))
        #expect(items.first { $0.id == "heading-2" }?.label == "Heading 2")
        #expect(items.first { $0.id == "heading-2" }?.description == "Section heading")
        #expect(items.first { $0.id == "taskList" }?.group == "Lists")
        #expect(items.first { $0.id == "embed" }?.group == "Blocks")
        #expect(items.last == extra)
        #expect(Set(items.map(\.id)).count == items.count)
        #expect(Tokens.kind(slashID: "callout-info", kinds: MonarchBlockKind.defaultKinds) == .callout(.info))
        #expect(Tokens.kind(slashID: "date", kinds: MonarchBlockKind.defaultKinds) == nil)
        #expect(Tokens.groupOrder(labels: labels) == ["Text", "Lists", "Blocks"])
    }

    @Test("Toolbar groups follow the offered kinds and end with Select blocks")
    func toolGroups() {
        let all = Tokens.toolGroups(kinds: MonarchBlockKind.defaultKinds, selectTitle: "Select")
        #expect(all.first == [.undo, .redo])
        #expect(all[1] == [.paragraph, .heading(1), .heading(2), .heading(3)])
        #expect(all[2] == [.bulletList, .orderedList, .taskList])
        #expect(all[3] == [.blockquote, .codeBlock])
        #expect(all.last == [.custom(id: Tokens.selectToolID, title: "Select", systemImage: Tokens.selectSymbol)])
        let few = Tokens.toolGroups(kinds: [.paragraph, .bulletList, .table], selectTitle: "S")
        #expect(few.count == 4)
        #expect(!few.flatMap { $0 }.contains(.bold))
    }

    @Test("Tools and kinds map both ways; headings are clamped")
    func toolMapping() {
        for kind in MonarchBlockKind.defaultKinds where kind.isText {
            #expect(Tokens.kind(Tokens.tool(kind)!) == kind)
        }
        #expect(Tokens.tool(.table) == nil)
        #expect(Tokens.kind(.bold) == nil)
        #expect(Tokens.tool(.heading(9)) == .heading(6))
        #expect(Tokens.textStyle(.heading(1)) == .title)
        #expect(Tokens.textStyle(.heading(2)) == .heading)
        #expect(Tokens.textStyle(.heading(5)) == .subheading)
        #expect(Tokens.textStyle(.codeBlock) == .code)
        #expect(Tokens.textStyle(.callout(.info)) == .body)
    }

    // MARK: Names and accessibility

    @Test("Names come from overrides, the toolbar, then the built-in table")
    func names() {
        #expect(Tokens.name(.paragraph, labels: labels) == "Text")
        #expect(Tokens.name(.heading(3), labels: labels) == "Heading 3")
        #expect(Tokens.name(.callout(.danger), labels: labels) == "Callout")
        #expect(Tokens.name(.toggle, labels: labels) == "Toggle")
        #expect(Tokens.name(.table, labels: labels) == "Table")
        #expect(Tokens.name(.embed, labels: labels) == "Embed")
        let custom = MonarchBlockEditorLabels(
            blockNames: [.callout(.neutral): "Hinweis", .bulletList: "Liste"],
            blockDescriptions: [.callout(.neutral): "Ein Hinweis"],
            toolbar: MonarchEditorToolbarLabels(tools: [.paragraph: "Absatz"])
        )
        #expect(Tokens.name(.callout(.warning), labels: custom) == "Hinweis")
        #expect(Tokens.description(.callout(.info), labels: custom) == "Ein Hinweis")
        #expect(Tokens.name(.bulletList, labels: custom) == "Liste")
        #expect(Tokens.name(.paragraph, labels: custom) == "Absatz")
    }

    @Test("Fields are named by kind, with the position for list items")
    func fieldLabels() {
        let blocks = [
            MonarchBlock(id: "h", kind: .heading(2), text: "Steps"),
            MonarchBlock(id: "1", kind: .orderedList, text: "One"),
            MonarchBlock(id: "2", kind: .orderedList, text: "Two"),
            MonarchBlock(id: "t", kind: .taskList, text: ""),
        ]
        #expect(Tokens.fieldLabel(at: 0, in: blocks, labels: labels) == "Heading 2")
        #expect(Tokens.fieldLabel(at: 2, in: blocks, labels: labels) == "Numbered list, item 2")
        #expect(Tokens.fieldLabel(at: 3, in: blocks, labels: labels) == "Checklist, item 1")
        #expect(Tokens.fieldLabel(at: 7, in: blocks, labels: labels).isEmpty)
        let custom = MonarchBlockEditorLabels(listItem: { "\($0) #\($1)" })
        #expect(Tokens.fieldLabel(at: 1, in: blocks, labels: custom) == "Numbered list #1")
    }

    @Test("Handles and checkboxes have spoken names")
    func spokenNames() {
        #expect(Tokens.handleLabel(.heading(2), labels: labels) == "Block actions: Heading 2")
        #expect(Tokens.handleLabel(.table, labels: MonarchBlockEditorLabels(handle: { "Menu \($0)" })) == "Menu Table")
        #expect(Tokens.checkboxLabel(MonarchBlock(kind: .taskList, text: " Ship "), labels: labels) == "Ship")
        #expect(Tokens.checkboxLabel(MonarchBlock(kind: .taskList), labels: labels) == "Checklist")
        #expect(Tokens.selectedCount(3, labels: labels) == "3 selected")
        #expect(
            Tokens.selectedCount(2, labels: MonarchBlockEditorLabels(selectedCount: { "\($0) gewählt" })) == "2 gewählt"
        )
        #expect(Tokens.toneName(.neutral, labels: labels) == "Neutral")
        #expect(Tokens.toneName(.warning, labels: labels) == MonarchToneStrings.name(.warning))
        #expect(Tokens.toneName(.info, labels: MonarchBlockEditorLabels(toneNames: [.info: "Info!"])) == "Info!")
    }

    // MARK: Colours

    @Test("Colours for every state")
    func colours() {
        #expect(Tokens.textColor(isEnabled: true) == \.textDefault)
        #expect(Tokens.textColor(isEnabled: false) == \.textDisabled)
        #expect(Tokens.taskTextColor(isChecked: false, isEnabled: true) == \.textDefault)
        #expect(Tokens.taskTextColor(isChecked: true, isEnabled: true) == \.textMuted)
        #expect(Tokens.taskTextColor(isChecked: true, isEnabled: false) == \.textDisabled)
        #expect(Tokens.markerColor == \.textMuted)
        #expect(Tokens.quoteBarColor == \.borderStrong)
        #expect(Tokens.codeBackground == \.bgSubtle)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let keys = [
            "prompt", "handleHint", "moveUp", "moveDown", "duplicate", "deleteBlock", "deleteSelected",
            "turnInto", "tone", "toneNeutral", "select", "done", "summaryPrompt", "detailPrompt", "embedPrompt",
            "addBlock", "groupText", "groupLists", "groupBlocks", "callout", "toggle", "table", "embed",
            "paragraphDescription", "heading1Description", "heading2Description", "headingDescription",
            "bulletListDescription", "orderedListDescription", "taskListDescription", "blockquoteDescription",
            "codeBlockDescription", "calloutDescription", "toggleDescription", "tableDescription", "embedDescription",
        ]
        for key in keys {
            #expect(MonarchBlockEditorStrings.string(key) != "monarch.blockEditor.\(key)", "\(key)")
        }
        #expect(MonarchBlockEditorStrings.prompt == "Write, or type / for blocks")
        #expect(MonarchBlockEditorStrings.handle("Quote") == "Block actions: Quote")
        #expect(MonarchBlockEditorStrings.listItem("Bulleted list", position: 4) == "Bulleted list, item 4")
    }

    @Test("Default kinds cover every block kind once")
    func defaultKinds() {
        let kinds = MonarchBlockKind.defaultKinds
        #expect(Set(kinds).count == kinds.count)
        #expect(kinds.filter(\.isText).count == 9)
        #expect(!MonarchBlockKind.table.isText)
    }
}
