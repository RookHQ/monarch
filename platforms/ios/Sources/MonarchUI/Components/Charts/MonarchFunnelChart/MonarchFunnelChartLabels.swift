/// Replacements for the built-in strings of ``MonarchFunnelChart``.
/// A property that is `nil` keeps the built-in English string.
///
/// Sentences with values are formats: `%1$@`, `%2$@`… stand for the values
/// named in each property's description, already formatted.
public struct MonarchFunnelChartLabels: Sendable, Hashable {
    /// The strings of the chart frame: the table toggle, loading, empty and error.
    public var frame: MonarchChartFrameLabels
    /// The table heading of the stage column. Built in: "Stage".
    public var stage: String?
    /// The table heading of the value column. Built in: "Count".
    public var value: String?
    /// The table heading of the step conversion. Built in: "From previous".
    public var fromPrevious: String?
    /// The table heading of the share of the first stage. Built in: "Of first".
    public var ofFirst: String?
    /// The line between two stages: `%1$@` the rate, `%2$@` the number dropped.
    /// Built in: "%1$@ continued · %2$@ dropped".
    public var step: String?
    /// The summary of one stage: `%1$@` its label. Built in: "1 stage, %1$@".
    public var oneStage: String?
    /// The summary of several stages: `%1$@` the count, `%2$@` the first, `%3$@` the last.
    /// Built in: "%1$@ stages from %2$@ to %3$@".
    public var stages: String?
    /// The overall conversion: `%1$@` the rate. Built in: "Overall conversion %1$@".
    public var overall: String?
    /// The largest drop: `%1$@` from, `%2$@` to, `%3$@` the share lost.
    /// Built in: "Largest drop: %1$@ to %2$@, %3$@ lost".
    public var largestDrop: String?
    /// What VoiceOver reads for the first stage: `%1$@` the label, `%2$@` the value.
    /// Built in: "%1$@: %2$@".
    public var describeFirst: String?
    /// What VoiceOver reads for a later stage: `%1$@` the label, `%2$@` the value,
    /// `%3$@` the share of the previous stage, `%4$@` the share of the first.
    /// Built in: "%1$@: %2$@, %3$@ of previous, %4$@ of first".
    public var describe: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        frame: MonarchChartFrameLabels = MonarchChartFrameLabels(),
        stage: String? = nil,
        value: String? = nil,
        fromPrevious: String? = nil,
        ofFirst: String? = nil,
        step: String? = nil,
        oneStage: String? = nil,
        stages: String? = nil,
        overall: String? = nil,
        largestDrop: String? = nil,
        describeFirst: String? = nil,
        describe: String? = nil
    ) {
        self.frame = frame
        self.stage = stage
        self.value = value
        self.fromPrevious = fromPrevious
        self.ofFirst = ofFirst
        self.step = step
        self.oneStage = oneStage
        self.stages = stages
        self.overall = overall
        self.largestDrop = largestDrop
        self.describeFirst = describeFirst
        self.describe = describe
    }
}
