import Foundation

/// Built-in strings of the funnel chart, from the `MonarchFunnelChart` strings table.
enum MonarchFunnelChartStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.funnelChart.\(key)", table: "MonarchFunnelChart")
    }

    /// The table heading of the stage column.
    static var stage: String { string("stage") }
    /// The table heading of the value column.
    static var value: String { string("value") }
    /// The table heading of the step conversion.
    static var fromPrevious: String { string("fromPrevious") }
    /// The table heading of the share of the first stage.
    static var ofFirst: String { string("ofFirst") }
    /// The line between two stages: rate, dropped.
    static var step: String { string("step") }
    /// The summary of one stage: label.
    static var oneStage: String { string("oneStage") }
    /// The summary of several stages: count, first, last.
    static var stages: String { string("stages") }
    /// The overall conversion: rate.
    static var overall: String { string("overall") }
    /// The largest drop: from, to, lost.
    static var largestDrop: String { string("largestDrop") }
    /// The first stage for VoiceOver: label, value.
    static var describeFirst: String { string("describeFirst") }
    /// A later stage for VoiceOver: label, value, of previous, of first.
    static var describe: String { string("describe") }

    /// Fills a format's `%@` placeholders.
    static func fill(_ format: String, _ arguments: String...) -> String {
        String(format: format, arguments: arguments)
    }
}
