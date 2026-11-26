import Foundation

/// The resolved strings of ``MonarchKanbanBoard``: the app's replacements, else the built-in ones.
struct MonarchKanbanBoardText {
    let labels: MonarchKanbanBoardLabels

    private typealias Strings = MonarchKanbanBoardStrings

    private func pick(_ value: String?, _ key: String) -> String {
        value ?? Strings.string(key)
    }

    var moveToSection: String { pick(labels.moveToSection, "moveToSection") }
    var moveUp: String { pick(labels.moveUp, "moveUp") }
    var moveDown: String { pick(labels.moveDown, "moveDown") }
    var empty: String { pick(labels.empty, "empty") }
    var columns: String { pick(labels.columns, "columns") }

    /// "Move Fix login to…"
    func moveMenu(_ card: String) -> String {
        labels.moveMenu?(card) ?? Strings.string("moveMenu", card)
    }

    /// "Move to Doing"
    func moveTo(_ column: String) -> String {
        labels.moveTo?(column) ?? Strings.string("moveTo", column)
    }

    /// "2", or "2 / 3" with a limit.
    func countText(_ count: Int, limit: Int?) -> String {
        if let custom = labels.countText { return custom(count, limit) }
        guard let limit else { return Strings.string("countText", count) }
        return Strings.string("countTextLimit", count, limit)
    }

    /// "1 card", "2 cards, limit 3", "2 cards, limit 2, at the limit", "3 cards, limit 2, over the limit".
    func count(_ count: Int, limit: Int?) -> String {
        if let custom = labels.count { return custom(count, limit) }
        var parts = [Strings.plural("cards", count)]
        if let limit { parts.append(Strings.string("limit", limit)) }
        switch MonarchKanbanBoardTokens.limitState(count: count, limit: limit) {
        case .at: parts.append(Strings.string("atLimit"))
        case .over: parts.append(Strings.string("overLimit"))
        case .none, .under: break
        }
        return parts.joined(separator: ", ")
    }

    /// "Fix login: Doing, position 1 of 2."
    func moved(_ card: String, column: String, position: Int, total: Int) -> String {
        labels.moved?(card, column, position, total) ?? Strings.string("moved", card, column, position, total)
    }

    /// "Fix login dropped in Doing, position 1 of 2."
    func dropped(_ card: String, column: String, position: Int, total: Int) -> String {
        labels.dropped?(card, column, position, total) ?? Strings.string("dropped", card, column, position, total)
    }

    /// "Fix login cannot move to Done."
    func notAllowed(_ card: String, column: String) -> String {
        labels.notAllowed?(card, column) ?? Strings.string("notAllowed", card, column)
    }

    /// "Could not move Fix login. It is back in To do."
    func failed(_ card: String, column: String) -> String {
        labels.failed?(card, column) ?? Strings.string("failed", card, column)
    }
}
