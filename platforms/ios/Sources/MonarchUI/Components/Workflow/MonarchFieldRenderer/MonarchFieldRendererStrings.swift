import Foundation

/// Built-in strings of ``MonarchFieldRenderer``, from the `MonarchFieldRenderer` strings table.
enum MonarchFieldRendererStrings {
    private static let table = "MonarchFieldRenderer"

    /// The string for a key without placeholders, such as `required`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.fieldRenderer.\(key)", table: table)
    }

    /// The string for a key with a text placeholder, such as "Remove cv.pdf".
    static func string(_ key: String, _ text: String) -> String {
        MonarchStrings.string("monarch.fieldRenderer.\(key)", table: table, text)
    }

    /// The string for a key with a number placeholder, such as "Choose at least 2."
    static func string(_ key: String, _ number: Int) -> String {
        MonarchStrings.string("monarch.fieldRenderer.\(key)", table: table, number)
    }
}

/// The strings a ``MonarchFieldRenderer`` shows and speaks: each override from the labels, or the built-in
/// string. Numbers in messages are formatted for `locale`.
struct MonarchFieldRendererText {
    let labels: MonarchFieldRendererLabels
    let locale: Locale

    private func string(_ override: String?, _ key: String) -> String {
        override ?? MonarchFieldRendererStrings.string(key)
    }

    var yes: String { string(labels.yes, "yes") }
    var no: String { string(labels.no, "no") }
    var choose: String { string(labels.choose, "choose") }
    var chooseDate: String { string(labels.chooseDate, "chooseDate") }
    var chooseTime: String { string(labels.chooseTime, "chooseTime") }
    var clear: String { string(labels.clear, "clear") }
    var aiFilled: String { string(labels.aiFilled, "aiFilled") }
    var aiCheck: String { string(labels.aiCheck, "aiCheck") }
    var remove: String { string(labels.remove, "remove") }

    /// The spoken name of a file's remove button.
    func removeFile(_ name: String) -> String {
        labels.removeFile?(name) ?? MonarchFieldRendererStrings.string("removeFile", name)
    }

    /// A limit formatted for the locale: whole numbers without a fraction.
    func number(_ value: Double) -> String {
        value.formatted(.number.locale(locale))
    }

    /// The message of a problem.
    func message(_ problem: MonarchFormFieldProblem) -> String {
        switch problem {
        case .required: string(labels.required, "required")
        case .mustAgree: string(labels.mustAgree, "mustAgree")
        case .email: string(labels.email, "email")
        case .url: string(labels.url, "url")
        case .notANumber: string(labels.number, "number")
        case .min(let min):
            labels.min?(number(min)) ?? MonarchFieldRendererStrings.string("min", number(min))
        case .max(let max):
            labels.max?(number(max)) ?? MonarchFieldRendererStrings.string("max", number(max))
        case .minLength(let min): labels.minLength?(min) ?? MonarchFieldRendererStrings.string("minLength", min)
        case .maxLength(let max): labels.maxLength?(max) ?? MonarchFieldRendererStrings.string("maxLength", max)
        case .minSelections(let min):
            labels.minSelections?(min) ?? MonarchFieldRendererStrings.string("minSelections", min)
        case .maxSelections(let max):
            labels.maxSelections?(max) ?? MonarchFieldRendererStrings.string("maxSelections", max)
        }
    }
}
