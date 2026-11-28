import Foundation

/// A group of rules and nested groups joined by one combinator: the value of a ``MonarchQueryBuilder``.
public struct MonarchQueryGroup: Identifiable, Hashable, Sendable {
    /// A stable, unique id.
    public var id: String
    /// Whether every rule and group must hold, or at least one.
    public var combinator: MonarchQueryCombinator
    /// The rules, shown first.
    public var rules: [MonarchCondition]
    /// The nested groups, shown after the rules.
    public var groups: [MonarchQueryGroup]

    /// A group.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - combinator: How the rules and groups combine.
    ///   - rules: The rules.
    ///   - groups: The nested groups.
    public init(
        id: String,
        combinator: MonarchQueryCombinator = .and,
        rules: [MonarchCondition] = [],
        groups: [MonarchQueryGroup] = []
    ) {
        self.id = id
        self.combinator = combinator
        self.rules = rules
        self.groups = groups
    }

    /// Every rule in the query, depth first.
    public var allRules: [MonarchCondition] {
        rules + groups.flatMap(\.allRules)
    }

    /// The problem of every incomplete rule, by rule id. Empty when the query is complete.
    public func problems(fields: [MonarchConditionField]) -> [String: MonarchConditionProblem] {
        var out: [String: MonarchConditionProblem] = [:]
        for rule in allRules {
            if let problem = rule.problem(fields: fields) { out[rule.id] = problem }
        }
        return out
    }

    /// Whether a record passes the query. An empty group matches. A useful default for previews;
    /// the server stays the authority.
    ///
    /// - Parameters:
    ///   - values: The record's values by field id. A missing field is ``MonarchConditionValue/none``.
    ///   - calendar: The calendar that decides what a day is.
    /// - Returns: Whether the record passes.
    public func matches(_ values: [String: MonarchConditionValue], calendar: Calendar = .autoupdatingCurrent) -> Bool {
        if rules.isEmpty && groups.isEmpty { return true }
        let results =
            rules.map { $0.matches(values[$0.field] ?? .none, calendar: calendar) }
            + groups.map { $0.matches(values, calendar: calendar) }
        return combinator == .and ? results.allSatisfy { $0 } : results.contains(true)
    }
}
