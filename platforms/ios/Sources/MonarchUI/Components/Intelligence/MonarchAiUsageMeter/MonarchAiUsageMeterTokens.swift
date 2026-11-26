/// The mapping from AI scopes to the items of ``MonarchUsageMeter``.
enum MonarchAiUsageMeterTokens {
    /// The usage meter items for the scopes: the label, a stable id (the
    /// position when the scope has none), the amounts, and the unit name
    /// for the limit (or for the amount used when there is no limit).
    static func items(scopes: [MonarchAiUsageScope], labels: MonarchAiUsageMeterLabels) -> [MonarchUsageMeterItem] {
        scopes.enumerated().map { index, scope in
            let hasLimit = (scope.limit ?? 0) > 0
            let count = hasLimit ? (scope.limit ?? 0) : max(scope.used, 0)
            return MonarchUsageMeterItem(
                scope.label,
                id: scope.id ?? String(index),
                used: max(scope.used, 0),
                limit: hasLimit ? scope.limit : nil,
                unit: labels.unit(scope.unit, count)
            )
        }
    }
}
