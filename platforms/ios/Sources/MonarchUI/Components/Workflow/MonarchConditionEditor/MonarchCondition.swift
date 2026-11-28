import Foundation

/// One rule of a ``MonarchQueryGroup``: a field, an operator and a value.
public struct MonarchCondition: Identifiable, Hashable, Sendable {
    /// A stable, unique id.
    public var id: String
    /// The ``MonarchConditionField/id`` of the field tested. Empty while none is chosen.
    public var field: String
    /// How the value is compared.
    public var `operator`: MonarchConditionOperator
    /// The value compared with; ``MonarchConditionValue/none`` for operators that take none.
    public var value: MonarchConditionValue

    /// A rule.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - field: The id of the field tested.
    ///   - operator: How the value is compared.
    ///   - value: The value compared with.
    public init(id: String, field: String, operator: MonarchConditionOperator, value: MonarchConditionValue = .none) {
        self.id = id
        self.field = field
        self.operator = `operator`
        self.value = value
    }

    /// A new rule on a field, with the field's first operator and an empty value.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - field: The field tested, or `nil` to leave it to choose.
    public init(id: String, field: MonarchConditionField?) {
        let first = field?.availableOperators.first ?? .equals
        self.init(id: id, field: field?.id ?? "", operator: first, value: field?.blankValue(for: first) ?? .none)
    }

    /// What is missing, or `nil` when the rule is complete.
    public func problem(fields: [MonarchConditionField]) -> MonarchConditionProblem? {
        guard let field = fields.first(where: { $0.id == self.field }) else { return .field }
        guard self.operator.takesValue else { return nil }
        if value.isEmpty { return .value }
        if case .select(let options) = field.kind, case .option(let id?) = value,
            !options.contains(where: { $0.id == id })
        {
            return .option
        }
        if case .select(let options) = field.kind, case .options(let ids) = value,
            !ids.allSatisfy({ id in options.contains { $0.id == id } })
        {
            return .option
        }
        if case .tags(let options) = field.kind, case .options(let ids) = value,
            !ids.allSatisfy({ id in options.contains { $0.id == id } })
        {
            return .option
        }
        return nil
    }

    /// Whether a record's value passes the rule. Text ignores case; dates compare by day in `calendar`.
    public func matches(_ actual: MonarchConditionValue, calendar: Calendar = .autoupdatingCurrent) -> Bool {
        switch self.operator {
        case .isEmpty: return actual.isEmpty
        case .isNotEmpty: return !actual.isEmpty
        case .isTrue: return actual == .boolean(true)
        case .isFalse: return actual == .boolean(false)
        case .anyOf, .noneOf:
            guard case .options(let wanted) = value else { return self.operator == .noneOf }
            let has = Self.ids(actual).contains { wanted.contains($0) }
            return self.operator == .anyOf ? has : !has
        case .equals: return Self.equal(actual, value, calendar: calendar)
        case .notEquals: return !Self.equal(actual, value, calendar: calendar)
        case .contains, .notContains:
            guard case .text(let wanted) = value else { return self.operator == .notContains }
            let has = Self.text(actual).localizedLowercase.contains(wanted.localizedLowercase)
            return self.operator == .contains ? has : !has
        case .greaterThan, .atLeast, .lessThan, .atMost:
            guard case .number(let a?) = actual, case .number(let b?) = value else { return false }
            switch self.operator {
            case .greaterThan: return a > b
            case .atLeast: return a >= b
            case .lessThan: return a < b
            default: return a <= b
            }
        case .before, .after:
            guard case .date(let a?) = actual, case .date(let b?) = value else { return false }
            let dayA = calendar.startOfDay(for: a)
            let dayB = calendar.startOfDay(for: b)
            return self.operator == .before ? dayA < dayB : dayA > dayB
        }
    }

    private static func ids(_ value: MonarchConditionValue) -> [String] {
        switch value {
        case .options(let ids): ids
        case .option(let id?): [id]
        case .text(let text): [text]
        default: []
        }
    }

    private static func text(_ value: MonarchConditionValue) -> String {
        switch value {
        case .text(let text): text
        case .option(let id): id ?? ""
        default: ""
        }
    }

    private static func equal(_ a: MonarchConditionValue, _ b: MonarchConditionValue, calendar: Calendar) -> Bool {
        switch (a, b) {
        case (.number(let x?), .number(let y?)): return x == y
        case (.date(let x?), .date(let y?)): return calendar.isDate(x, inSameDayAs: y)
        case (.boolean(let x), .boolean(let y)): return x == y
        case (.text, .text), (.option, .option), (.text, .option), (.option, .text):
            return text(a).localizedLowercase == text(b).localizedLowercase
        default: return false
        }
    }
}
