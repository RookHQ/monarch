/// Built-in strings of the world map, from the `MonarchWorldMap` strings table.
enum MonarchWorldMapStrings {
    private static let table = "MonarchWorldMap"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.worldMap.\(key)", table: table)
    }

    /// The table heading of the country column.
    static var country: String { string("country") }
    /// The table heading of the value column.
    static var value: String { string("value") }
    /// The spoken name of the tiles under the map.
    static var extras: String { string("extras") }
    /// The note for codes that are not on the map: names, total.
    static func unplaced(_ names: String, _ total: String) -> String {
        MonarchStrings.string("monarch.worldMap.unplaced", table: table, names, total)
    }
    /// The summary of one country: count, top values.
    static func summaryOne(_ count: String, _ top: String) -> String {
        MonarchStrings.string("monarch.worldMap.summaryOne", table: table, count, top)
    }
    /// The summary of several countries: count, top values.
    static func summary(_ count: String, _ top: String) -> String {
        MonarchStrings.string("monarch.worldMap.summary", table: table, count, top)
    }
    /// One country as read: name, values.
    static func countryValue(_ name: String, _ values: String) -> String {
        MonarchStrings.string("monarch.worldMap.countryValue", table: table, name, values)
    }
}
