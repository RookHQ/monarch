import Foundation

/// Built-in strings of ``MonarchBusinessHoursEditor``, from the `MonarchBusinessHoursEditor` strings table.
enum MonarchBusinessHoursEditorStrings {
    private static let table = "MonarchBusinessHoursEditor"

    /// Every key of the table, for the tests.
    static let keys = [
        "name", "namePlaceholder", "nameRequired", "timeZone", "timeZoneDescription", "noOpenHours",
        "effectiveFrom", "effectiveTo", "effectiveOrder", "exceptions", "exceptionsDescription", "noExceptions",
        "addException", "exceptionDate", "exceptionName", "exceptionNamePlaceholder", "exceptionKind", "closed",
        "specialHours", "exceptionStart", "exceptionEnd", "remove", "removeNamed", "removePosition", "errorDate",
        "errorName", "errorDuplicate", "errorHours", "defaultTitle", "defaultDescription", "endOfDay", "chooseDate",
        "clearDate", "clearDateItem", "noDate",
    ]

    /// The string for a key without placeholders, such as `name`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.businessHoursEditor.\(key)", table: table)
    }

    /// The string for a key with placeholders, such as "Date %ld".
    static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }
}

/// The resolved strings of ``MonarchBusinessHoursEditor``: the app's replacements, else the built-in ones.
struct MonarchBusinessHoursEditorText {
    let labels: MonarchBusinessHoursEditorLabels

    private func pick(_ value: String?, _ key: String) -> String {
        value ?? MonarchBusinessHoursEditorStrings.string(key)
    }

    var name: String { pick(labels.name, "name") }
    var namePlaceholder: String { pick(labels.namePlaceholder, "namePlaceholder") }
    var nameRequired: String { pick(labels.nameRequired, "nameRequired") }
    var timeZone: String { pick(labels.timeZone, "timeZone") }
    var timeZoneDescription: String { pick(labels.timeZoneDescription, "timeZoneDescription") }
    var noOpenHours: String { pick(labels.noOpenHours, "noOpenHours") }
    var effectiveFrom: String { pick(labels.effectiveFrom, "effectiveFrom") }
    var effectiveTo: String { pick(labels.effectiveTo, "effectiveTo") }
    var effectiveOrder: String { pick(labels.effectiveOrder, "effectiveOrder") }
    var exceptions: String { pick(labels.exceptions, "exceptions") }
    var exceptionsDescription: String { pick(labels.exceptionsDescription, "exceptionsDescription") }
    var noExceptions: String { pick(labels.noExceptions, "noExceptions") }
    var addException: String { pick(labels.addException, "addException") }
    var exceptionNamePlaceholder: String { pick(labels.exceptionNamePlaceholder, "exceptionNamePlaceholder") }
    var closed: String { pick(labels.closed, "closed") }
    var specialHours: String { pick(labels.specialHours, "specialHours") }
    var remove: String { pick(labels.remove, "remove") }
    var defaultTitle: String { pick(labels.defaultTitle, "defaultTitle") }
    var defaultDescription: String { pick(labels.defaultDescription, "defaultDescription") }
    var endOfDay: String { pick(labels.endOfDay, "endOfDay") }
    var chooseDate: String { pick(labels.chooseDate, "chooseDate") }
    var clearDate: String { pick(labels.clearDate, "clearDate") }
    var noDate: String { pick(labels.noDate, "noDate") }

    func exceptionDate(_ position: Int) -> String {
        labels.exceptionDate?(position) ?? MonarchBusinessHoursEditorStrings.string("exceptionDate", position)
    }

    func exceptionName(_ position: Int) -> String {
        labels.exceptionName?(position) ?? MonarchBusinessHoursEditorStrings.string("exceptionName", position)
    }

    func exceptionKind(_ position: Int) -> String {
        labels.exceptionKind?(position) ?? MonarchBusinessHoursEditorStrings.string("exceptionKind", position)
    }

    func exceptionStart(_ position: Int) -> String {
        labels.exceptionStart?(position) ?? MonarchBusinessHoursEditorStrings.string("exceptionStart", position)
    }

    func exceptionEnd(_ position: Int) -> String {
        labels.exceptionEnd?(position) ?? MonarchBusinessHoursEditorStrings.string("exceptionEnd", position)
    }

    /// "Remove Christmas Day", or "Remove date 2" for a row without a name.
    func removeException(_ position: Int, name: String) -> String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if let custom = labels.removeException { return custom(position, trimmed) }
        return trimmed.isEmpty
            ? MonarchBusinessHoursEditorStrings.string("removePosition", position)
            : MonarchBusinessHoursEditorStrings.string("removeNamed", trimmed)
    }

    func clearDateItem(_ label: String) -> String {
        labels.clearDateItem?(label) ?? MonarchBusinessHoursEditorStrings.string("clearDateItem", label)
    }

    func exceptionError(_ error: MonarchBusinessHoursExceptionError) -> String {
        if let custom = labels.exceptionError { return custom(error) }
        switch error {
        case .date: return MonarchBusinessHoursEditorStrings.string("errorDate")
        case .name: return MonarchBusinessHoursEditorStrings.string("errorName")
        case .duplicate: return MonarchBusinessHoursEditorStrings.string("errorDuplicate")
        case .hours: return MonarchBusinessHoursEditorStrings.string("errorHours")
        }
    }

    /// The heading of a row: its trimmed name, else "Date 2".
    func rowTitle(_ exception: MonarchBusinessHoursException, position: Int) -> String {
        let trimmed = exception.name.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? exceptionDate(position) : trimmed
    }
}
