/// Built-in strings of the stage journey, from the `MonarchStageJourney`
/// strings table, with the replacements of a labels value applied.
enum MonarchStageJourneyStrings {
    private static let table = "MonarchStageJourney"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.stageJourney." + key, table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.stageJourney." + key, table: table, argument)
    }

    static func stages(_ labels: MonarchStageJourneyLabels) -> String { labels.stages ?? string("stages") }
    static func now(_ stage: String, _ labels: MonarchStageJourneyLabels) -> String {
        labels.now?(stage) ?? string("now", stage)
    }
    static func since(_ time: String, _ labels: MonarchStageJourneyLabels) -> String {
        labels.since?(time) ?? string("since", time)
    }
    static func done(_ labels: MonarchStageJourneyLabels) -> String { labels.done ?? string("done") }
    static func current(_ labels: MonarchStageJourneyLabels) -> String { labels.current ?? string("current") }
    static func success(_ labels: MonarchStageJourneyLabels) -> String { labels.success ?? string("success") }
    static func closed(_ labels: MonarchStageJourneyLabels) -> String { labels.closed ?? string("closed") }
    static func nextSteps(_ labels: MonarchStageJourneyLabels) -> String {
        labels.nextSteps ?? string("nextSteps")
    }
    static func moveTo(_ stage: String, _ labels: MonarchStageJourneyLabels) -> String {
        labels.moveTo?(stage) ?? string("moveTo", stage)
    }
    static func backTo(_ stage: String, _ labels: MonarchStageJourneyLabels) -> String {
        labels.backTo?(stage) ?? string("backTo", stage)
    }
    static func needsInput(_ labels: MonarchStageJourneyLabels) -> String {
        labels.needsInput ?? string("needsInput")
    }
    static func moved(_ stage: String, _ labels: MonarchStageJourneyLabels) -> String {
        labels.moved?(stage) ?? string("moved", stage)
    }
    static func failed(_ labels: MonarchStageJourneyLabels) -> String { labels.failed ?? string("failed") }
}
