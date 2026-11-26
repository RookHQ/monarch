/// Built-in strings of the gauge, from the `MonarchGauge` strings table.
enum MonarchGaugeStrings {
    private static let table = "MonarchGauge"

    /// The spoken name of a gauge without a label.
    static var name: String { MonarchStrings.string("monarch.gauge.name", table: table) }
    /// Spoken when there is no value.
    static var noValue: String { MonarchStrings.string("monarch.gauge.noValue", table: table) }

    /// The spoken target.
    static func target(_ value: String) -> String {
        MonarchStrings.string("monarch.gauge.target", table: table, value)
    }

    /// The spoken value with the name of its band.
    static func valueInBand(value: String, band: String) -> String {
        MonarchStrings.string("monarch.gauge.valueInBand", table: table, value, band)
    }

    /// The spoken value with the ends of the scale.
    static func valueInRange(value: String, minimum: String, maximum: String) -> String {
        MonarchStrings.string("monarch.gauge.valueInRange", table: table, value, minimum, maximum)
    }
}
