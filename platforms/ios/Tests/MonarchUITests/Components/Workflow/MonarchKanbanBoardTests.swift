import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private struct BoardCard: Identifiable, Hashable {
    let id: String
    let title: String
}

private enum BoardSamples {
    static let todo = MonarchKanbanColumn(
        id: "todo",
        title: "To do",
        items: [
            BoardCard(id: "A-1", title: "Fix login"),
            BoardCard(id: "A-2", title: "Export CSV"),
            BoardCard(id: "A-3", title: "Rate limit the API"),
        ]
    )
    static let doing = MonarchKanbanColumn(
        id: "doing",
        title: "Doing",
        items: [BoardCard(id: "A-4", title: "Billing page")],
        wipLimit: 1
    )
    static let done = MonarchKanbanColumn(id: "done", title: "Done", items: [BoardCard]())
    static let all = [todo, doing, done]

    static func ids(_ columns: [MonarchKanbanColumn<BoardCard>]) -> [[String]] {
        columns.map { $0.items.map(\.id) }
    }
}

enum MonarchKanbanBoardFixtures: ComponentFixtures {
    static let id = "kanban-board"
    static let fixtures = [
        Fixture("three columns, at the limit, empty column") {
            MonarchKanbanBoard(
                columns: .constant(BoardSamples.all),
                itemLabel: { $0.id },
                card: { card, _ in Text(card.title).monarchTextStyle(.body) }
            )
            .frame(height: 600)
        },
        Fixture("issue cards, over the limit, locked card, enforced limits") {
            MonarchKanbanBoard(
                columns: .constant([
                    BoardSamples.todo,
                    MonarchKanbanColumn(
                        id: "doing",
                        title: "Doing",
                        items: [BoardCard(id: "A-4", title: "Billing page"), BoardCard(id: "A-5", title: "Passkeys")],
                        wipLimit: 1,
                        systemImage: "circle.dotted"
                    ),
                ]),
                itemLabel: { $0.id },
                canMove: { _, to, _ in to != "done" },
                isWipLimitEnforced: true,
                isItemEnabled: { $0.id != "A-2" },
                card: { card, state in
                    MonarchIssueCard(card.title, key: card.id, isDragging: state.isDropTarget)
                }
            )
            .frame(height: 700)
        },
        Fixture("read only, long titles, own empty text, wide") {
            MonarchKanbanBoard(
                columns: .constant([
                    MonarchKanbanColumn(
                        id: "review",
                        title: "In review by the platform security group before release",
                        items: [
                            BoardCard(
                                id: "ENG-1042",
                                title: "Migrate billing webhooks to the new delivery queue with retries"
                            )
                        ]
                    ),
                    MonarchKanbanColumn(
                        id: "released",
                        title: "Released",
                        items: [BoardCard](),
                        emptyText: "Nothing released this week."
                    ),
                ]),
                itemLabel: { $0.id },
                isReadOnly: true,
                minimumColumnWidth: 120,
                card: { card, _ in Text(card.title).monarchTextStyle(.body) }
            )
            .frame(width: 800, height: 500)
        },
        Fixture("disabled board, second column in view") {
            MonarchKanbanBoard(
                columns: .constant(BoardSamples.all),
                itemLabel: { $0.id },
                scrollPosition: .constant("doing"),
                card: { card, _ in Text(card.title).monarchTextStyle(.body) }
            )
            .disabled(true)
            .frame(height: 500)
        },
    ]
}

@Suite("MonarchKanbanBoard")
@MainActor
struct MonarchKanbanBoardTests {
    private typealias Model = MonarchKanbanBoardModel
    private typealias Tokens = MonarchKanbanBoardTokens
    private let text = MonarchKanbanBoardText(labels: MonarchKanbanBoardLabels())

    @Test("Moving takes the card out, then puts it at the clamped place")
    func moving() throws {
        let across = try #require(Model.moving(BoardSamples.all, itemID: "A-2", toColumnID: "done", toIndex: 0))
        #expect(BoardSamples.ids(across.columns) == [["A-1", "A-3"], ["A-4"], ["A-2"]])
        #expect(
            across.move
                == MonarchKanbanMove(itemID: "A-2", fromColumnID: "todo", fromIndex: 1, toColumnID: "done", toIndex: 0)
        )
        let clamped = try #require(Model.moving(BoardSamples.all, itemID: "A-1", toColumnID: "doing", toIndex: 99))
        #expect(clamped.move.toIndex == 1)
        #expect(BoardSamples.ids(clamped.columns)[1] == ["A-4", "A-1"])
        let within = try #require(Model.moving(BoardSamples.all, itemID: "A-1", toColumnID: "todo", toIndex: 2))
        #expect(BoardSamples.ids(within.columns)[0] == ["A-2", "A-3", "A-1"])
        #expect(Model.moving(BoardSamples.all, itemID: "A-1", toColumnID: "todo", toIndex: 0) == nil)
        #expect(Model.moving(BoardSamples.all, itemID: "nope", toColumnID: "todo", toIndex: 0) == nil)
        #expect(Model.moving(BoardSamples.all, itemID: "A-1", toColumnID: "nope", toIndex: 0) == nil)
    }

    @Test("A failed move goes back to its column and position")
    func reverting() throws {
        let result = try #require(Model.moving(BoardSamples.all, itemID: "A-2", toColumnID: "done", toIndex: 0))
        #expect(BoardSamples.ids(Model.reverting(result.columns, result.move)) == BoardSamples.ids(BoardSamples.all))
        // Another card moved meanwhile: the failed card still goes back to its place, clamped.
        let meanwhile = try #require(
            Model.moving(result.columns, itemID: "A-3", toColumnID: "doing", toIndex: 0)
        ).columns
        #expect(BoardSamples.ids(Model.reverting(meanwhile, result.move)) == [["A-1", "A-2"], ["A-3", "A-4"], []])
        // The card is gone: nothing changes.
        var gone = result.columns
        gone[2].items = []
        #expect(BoardSamples.ids(Model.reverting(gone, result.move)) == BoardSamples.ids(gone))
    }

    @Test("A card dropped on a card takes its place; on the end it goes last")
    func dropIndex() {
        #expect(Model.dropIndex(isSameColumn: true, onto: 2, count: 3) == 2)
        #expect(Model.dropIndex(isSameColumn: true, onto: 0, count: 3) == 0)
        #expect(Model.dropIndex(isSameColumn: true, onto: nil, count: 3) == 2)
        #expect(Model.dropIndex(isSameColumn: false, onto: 1, count: 3) == 1)
        #expect(Model.dropIndex(isSameColumn: false, onto: nil, count: 3) == 3)
        #expect(Model.dropIndex(isSameColumn: false, onto: nil, count: 0) == 0)
        #expect(Model.dropIndex(isSameColumn: true, onto: 9, count: 3) == 2)
    }

    @Test("Columns refuse cards by canMove and full limits, never from their own column")
    func allows() {
        let card = BoardSamples.todo.items[0]
        let refuseDone: (BoardCard, String, String) -> Bool = { _, to, _ in to != "done" }
        func allows(
            _ to: String,
            from: String = "todo",
            canMove: ((BoardCard, String, String) -> Bool)? = nil,
            enforce: Bool
        )
            -> Bool
        {
            Model.allows(
                card,
                toColumnID: to,
                fromColumnID: from,
                columns: BoardSamples.all,
                canMove: canMove,
                isWipLimitEnforced: enforce
            )
        }
        #expect(allows("doing", enforce: false))
        #expect(!allows("doing", enforce: true))
        #expect(allows("doing", from: "doing", enforce: true))
        #expect(allows("done", enforce: true))
        #expect(!allows("done", canMove: refuseDone, enforce: false))
        #expect(allows("todo", canMove: { _, _, _ in false }, enforce: true))
        #expect(!allows("nope", enforce: false))
    }

    @Test("Move up and down stay in the column and stop at its ends")
    func stepping() {
        #expect(Model.step(BoardSamples.all, itemID: "A-1", up: true) == nil)
        #expect(Model.step(BoardSamples.all, itemID: "A-1", up: false) == 1)
        #expect(Model.step(BoardSamples.all, itemID: "A-3", up: true) == 1)
        #expect(Model.step(BoardSamples.all, itemID: "A-3", up: false) == nil)
        #expect(Model.step(BoardSamples.all, itemID: "A-4", up: false) == nil)
        #expect(Model.step(BoardSamples.all, itemID: "nope", up: false) == nil)
    }

    @Test("Columns per page: one on a phone, as many as fit on wider screens")
    func columnsPerPage() {
        let width = Tokens.minimumColumnWidth
        let spacing = Tokens.columnSpacing
        #expect(Model.columnsPerPage(width: 375, minimumWidth: width, spacing: spacing, count: 3) == 1)
        #expect(Model.columnsPerPage(width: 1024, minimumWidth: width, spacing: spacing, count: 5) == 3)
        #expect(Model.columnsPerPage(width: 1024, minimumWidth: width, spacing: spacing, count: 2) == 2)
        #expect(Model.columnsPerPage(width: 0, minimumWidth: width, spacing: spacing, count: 3) == 1)
        #expect(Model.columnsPerPage(width: 100, minimumWidth: width, spacing: spacing, count: 0) == 1)
        #expect(Tokens.minimumColumnWidth == 288)
    }

    @Test("Payloads name a card of this board only")
    func payloads() {
        let payload = Model.payload(namespace: "board", itemID: "A-3")
        #expect(Model.itemID(payload: payload, namespace: "board", in: BoardSamples.all) == "A-3")
        #expect(Model.itemID(payload: payload, namespace: "other", in: BoardSamples.all) == nil)
        #expect(Model.itemID(payload: "board#A-9", namespace: "board", in: BoardSamples.all) == nil)
        #expect(Model.itemID(payload: "A-3", namespace: "board", in: BoardSamples.all) == nil)
        let numbered = [MonarchKanbanColumn(id: "c", title: "C", items: [NumberedCard(id: 7)])]
        #expect(Model.itemID(payload: Model.payload(namespace: "n", itemID: 7), namespace: "n", in: numbered) == 7)
    }

    @Test("Limit state and its colour and symbol")
    func limits() {
        #expect(Tokens.limitState(count: 3, limit: nil) == .none)
        #expect(Tokens.limitState(count: 1, limit: 2) == .under)
        #expect(Tokens.limitState(count: 2, limit: 2) == .at)
        #expect(Tokens.limitState(count: 3, limit: 2) == .over)
        #expect(Tokens.countRole(.none) == \MonarchColorTokens.textMuted)
        #expect(Tokens.countRole(.under) == \MonarchColorTokens.textMuted)
        #expect(Tokens.countRole(.at) == \MonarchColorTokens.warningStrong)
        #expect(Tokens.countRole(.over) == \MonarchColorTokens.dangerStrong)
        #expect(!Tokens.showsLimitSymbol(.none) && !Tokens.showsLimitSymbol(.under))
        #expect(Tokens.showsLimitSymbol(.at) && Tokens.showsLimitSymbol(.over))
    }

    @Test("Targets, tabs and borders")
    func appearance() {
        #expect(Tokens.columnBorder(isTargeted: true) == \MonarchColorTokens.accentStrong)
        #expect(Tokens.columnBorder(isTargeted: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.columnBorderWidth(isTargeted: true) > Tokens.columnBorderWidth(isTargeted: false))
        #expect(Tokens.tab(isSelected: true) == \MonarchColorTokens.accentStrong)
        #expect(Tokens.tab(isSelected: false) == \MonarchColorTokens.textMuted)
        var feedback = MonarchKanbanBoardDropFeedback()
        let start = feedback
        feedback.record(isAccepted: true)
        #expect(feedback != start && feedback.isAccepted)
        let accepted = feedback
        feedback.record(isAccepted: true)
        #expect(feedback != accepted)
        feedback.record(isAccepted: false)
        #expect(!feedback.isAccepted)
    }

    @Test("Spoken and visible counts")
    func counts() {
        #expect(text.countText(2, limit: nil) == "2")
        #expect(text.countText(2, limit: 3) == "2 / 3")
        #expect(text.count(1, limit: nil) == "1 card")
        #expect(text.count(0, limit: nil) == "0 cards")
        #expect(text.count(1, limit: 2) == "1 card, limit 2")
        #expect(text.count(2, limit: 2) == "2 cards, limit 2, at the limit")
        #expect(text.count(3, limit: 2) == "3 cards, limit 2, over the limit")
    }

    @Test("Menu names and announcements")
    func announcements() {
        #expect(text.moveMenu("Fix login") == "Move Fix login to…")
        #expect(text.moveTo("Doing") == "Move to Doing")
        #expect(text.moveToSection == "Move to")
        #expect(text.moveUp == "Move up")
        #expect(text.moveDown == "Move down")
        #expect(text.empty == "No cards")
        #expect(text.columns == "Columns")
        #expect(text.moved("Fix login", column: "Doing", position: 1, total: 2) == "Fix login: Doing, position 1 of 2.")
        #expect(
            text.dropped("Fix login", column: "Doing", position: 1, total: 2)
                == "Fix login dropped in Doing, position 1 of 2."
        )
        #expect(text.notAllowed("Fix login", column: "Done") == "Fix login cannot move to Done.")
        #expect(text.failed("Fix login", column: "To do") == "Could not move Fix login. It is back in To do.")
    }

    @Test("Every built-in string can be replaced")
    func overrides() {
        let custom = MonarchKanbanBoardText(
            labels: MonarchKanbanBoardLabels(
                moveMenu: { "M \($0)" },
                moveToSection: "S",
                moveTo: { "T \($0)" },
                moveUp: "U",
                moveDown: "D",
                countText: { count, _ in "#\(count)" },
                count: { count, limit in "\(count) of \(limit ?? 0)" },
                empty: "E",
                columns: "C",
                moved: { card, column, position, total in "\(card) \(column) \(position) \(total)" },
                dropped: { card, _, _, _ in "dropped \(card)" },
                notAllowed: { card, column in "no \(card) \(column)" },
                failed: { card, column in "fail \(card) \(column)" }
            )
        )
        #expect(custom.moveMenu("A") == "M A")
        #expect(custom.moveToSection == "S")
        #expect(custom.moveTo("B") == "T B")
        #expect(custom.moveUp == "U")
        #expect(custom.moveDown == "D")
        #expect(custom.countText(3, limit: 4) == "#3")
        #expect(custom.count(3, limit: 4) == "3 of 4")
        #expect(custom.empty == "E")
        #expect(custom.columns == "C")
        #expect(custom.moved("A", column: "B", position: 1, total: 2) == "A B 1 2")
        #expect(custom.dropped("A", column: "B", position: 1, total: 2) == "dropped A")
        #expect(custom.notAllowed("A", column: "B") == "no A B")
        #expect(custom.failed("A", column: "B") == "fail A B")
    }

    @Test("Built-in strings resolve")
    func strings() {
        let keys = [
            "moveMenu", "moveToSection", "moveTo", "moveUp", "moveDown", "countText", "countTextLimit", "cards.one",
            "cards.other", "limit", "atLimit", "overLimit", "empty", "columns", "moved", "dropped", "notAllowed",
            "failed",
        ]
        for key in keys {
            let value = MonarchKanbanBoardStrings.string(key)
            #expect(!value.isEmpty)
            #expect(value != "monarch.kanbanBoard.\(key)")
        }
    }

    @Test("withItems keeps everything but the cards")
    func withItems() {
        let next = BoardSamples.doing.withItems([BoardCard(id: "Z", title: "Z")])
        #expect(next.id == "doing")
        #expect(next.wipLimit == 1)
        #expect(next.items.map(\.id) == ["Z"])
    }
}

private struct NumberedCard: Identifiable, Hashable {
    let id: Int
}
