/// Built-in strings of the stage history, from the `MonarchStageHistory`
/// strings table, with the replacements of a labels value applied.
enum MonarchStageHistoryStrings {
    private static let table = "MonarchStageHistory"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.stageHistory." + key, table: table)
    }

    static func started(_ stage: String, _ labels: MonarchStageHistoryLabels) -> String {
        labels.started?(stage) ?? MonarchStrings.string("monarch.stageHistory.started", table: table, stage)
    }
    static func to(_ labels: MonarchStageHistoryLabels) -> String { labels.to ?? string("to") }
    static func movedBack(_ labels: MonarchStageHistoryLabels) -> String { labels.movedBack ?? string("movedBack") }
    static func empty(_ labels: MonarchStageHistoryLabels) -> String { labels.empty ?? string("empty") }
}
