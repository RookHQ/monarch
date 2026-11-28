/// Built-in strings of the period selector, from the `MonarchPeriodSelector` strings table.
enum MonarchPeriodSelectorStrings {
    private static let table = "MonarchPeriodSelector"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.periodSelector.\(key)", table: table)
    }

    /// The name of the control.
    static var label: String { string("label") }
    /// The label of the custom date range.
    static var customRange: String { string("customRange") }

    /// The name of a preset.
    static func preset(_ preset: MonarchPeriodPreset) -> String {
        switch preset {
        case .today: string("preset.today")
        case .yesterday: string("preset.yesterday")
        case .last7Days: string("preset.last7Days")
        case .last14Days: string("preset.last14Days")
        case .last30Days: string("preset.last30Days")
        case .last90Days: string("preset.last90Days")
        case .last180Days: string("preset.last180Days")
        case .last365Days: string("preset.last365Days")
        case .thisWeek: string("preset.thisWeek")
        case .thisMonth: string("preset.thisMonth")
        case .lastMonth: string("preset.lastMonth")
        case .thisQuarter: string("preset.thisQuarter")
        case .lastQuarter: string("preset.lastQuarter")
        case .thisYear: string("preset.thisYear")
        case .custom: string("preset.custom")
        }
    }
}
