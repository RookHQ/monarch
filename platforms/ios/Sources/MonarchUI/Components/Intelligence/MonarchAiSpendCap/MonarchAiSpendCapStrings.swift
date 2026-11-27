/// Built-in strings of the spending limit, from the `MonarchAiSpendCap` strings table.
enum MonarchAiSpendCapStrings {
    private static let table = "MonarchAiSpendCap"

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        let format = MonarchStrings.string("monarch.aiSpendCap.\(key)", table: table)
        return arguments.isEmpty ? format : String(format: format, arguments: arguments)
    }

    static var title: String { string("title") }
    static var meterLabel: String { string("meterLabel") }
    static var breakdownLabel: String { string("breakdownLabel") }
    static var edit: String { string("edit") }
    static var setLimit: String { string("setLimit") }
    static var pausedTitle: String { string("pausedTitle") }
    static var raiseLimit: String { string("raiseLimit") }
    static var requestIncrease: String { string("requestIncrease") }
    static var readOnly: String { string("readOnly") }
    static var amountLabel: String { string("amountLabel") }
    static var errorRequired: String { string("errorRequired") }
    static var errorPositive: String { string("errorPositive") }
    static var warnAtLabel: String { string("warnAtLabel") }
    static var behaviorLabel: String { string("behaviorLabel") }
    static var save: String { string("save") }
    static var saveAnyway: String { string("saveAnyway") }
    static var cancel: String { string("cancel") }
    static var removeLimit: String { string("removeLimit") }
    static var removeConfirmTitle: String { string("removeConfirmTitle") }
    static var removeConfirmBody: String { string("removeConfirmBody") }
    static var removeCancel: String { string("removeCancel") }

    static func period(_ period: MonarchAiSpendCapPeriod) -> String { string("period.\(period.rawValue)") }
    static func state(_ state: MonarchAiSpendCapState) -> String { string("state.\(state.rawValue)") }
    static func behaviorSummary(_ behavior: MonarchAiSpendCapBehavior) -> String {
        string("behaviorSummary.\(behavior.rawValue)")
    }
    static func behaviorOption(_ behavior: MonarchAiSpendCapBehavior) -> String {
        string("behaviorOption.\(behavior.rawValue)")
    }
    static func behaviorDescription(_ behavior: MonarchAiSpendCapBehavior) -> String {
        string("behaviorDescription.\(behavior.rawValue)")
    }
    static func spentOfLimit(_ spent: String, _ limit: String, _ period: String) -> String {
        string("spentOfLimit", spent, limit, period)
    }
    static func spentNoLimit(_ spent: String, _ period: String) -> String { string("spentNoLimit", spent, period) }
    static func remaining(_ amount: String) -> String { string("remaining", amount) }
    static func over(_ amount: String) -> String { string("over", amount) }
    static func warnsAt(_ percent: String) -> String { string("warnsAt", percent) }
    static func resets(_ date: String) -> String { string("resets", date) }
    static func pausedBody(_ limit: String, _ period: String) -> String { string("pausedBody", limit, period) }
    static func amountHint(_ spent: String, _ period: String) -> String { string("amountHint", spent, period) }
    static func errorMax(_ max: String) -> String { string("errorMax", max) }
    static func belowSpent(_ spent: String, _ period: String, _ behavior: MonarchAiSpendCapBehavior) -> String {
        string("belowSpent.\(behavior.rawValue)", spent, period)
    }
    static func warnAtOption(_ percent: String) -> String { string("warnAtOption", percent) }
    static func announce(_ title: String, _ state: String, _ amount: String) -> String {
        string("announce", title, state, amount)
    }
}
