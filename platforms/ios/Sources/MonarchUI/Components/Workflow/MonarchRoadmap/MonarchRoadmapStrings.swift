/// Built-in strings of ``MonarchRoadmap``, from the `MonarchRoadmap` strings table.
enum MonarchRoadmapStrings {
    private static let table = "MonarchRoadmap"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.roadmap.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    static var timeline: String { string("timeline") }
    static var rowHeader: String { string("rowHeader") }
    static func health(_ health: MonarchRoadmapHealth) -> String { string("health.\(health.rawValue)") }
    static func count(_ count: Int) -> String { count == 1 ? string("count.one") : string("count.other", count) }
    static func meta(_ first: String, _ second: String) -> String { string("meta", first, second) }
}
