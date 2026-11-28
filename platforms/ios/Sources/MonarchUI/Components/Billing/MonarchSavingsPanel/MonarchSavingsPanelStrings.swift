/// Built-in strings of the savings panel, from the `MonarchSavingsPanel` strings table.
enum MonarchSavingsPanelStrings {
    private static let table = "MonarchSavingsPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.savingsPanel.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.savingsPanel.\(key)", table: table, argument)
    }

    private static func string(_ key: String, count: Int) -> String {
        MonarchStrings.string("monarch.savingsPanel.\(key).\(count == 1 ? "one" : "other")", table: table, count)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The headline, from its parts: the count, the cost and the unpriced count.
    static func headline(_ count: Int, cost: String?, unpriced: Int) -> String {
        var sentence = string("unused", count: count)
        if let cost { sentence += string("cost", cost) }
        if unpriced > 0 { sentence += string("withoutPrice", count: unpriced) }
        return sentence + string("end")
    }
    /// The spoken name of the categories.
    static var categories: String { string("categories") }
    /// Read before the decision counts.
    static var decisions: String { string("decisions") }
    /// The potential savings.
    static func potentialSavings(_ amount: String) -> String { string("potentialSavings", amount) }
    /// The spoken name of the item list.
    static var items: String { string("items") }
    /// After the shortened list.
    static func more(_ count: Int) -> String {
        MonarchStrings.string("monarch.savingsPanel.more", table: table, count)
    }
    /// An item without a price.
    static var unpriced: String { string("unpriced") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }
    /// Read while loading.
    static var loading: String { string("loading") }
}
