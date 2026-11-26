/// Built-in strings of the stacked proportion bar, from the `MonarchStackedProportionBar` strings table.
enum MonarchStackedProportionBarStrings {
    private static let table = "MonarchStackedProportionBar"

    /// Shown instead of the legend when every value is 0.
    static var empty: String { MonarchStrings.string("monarch.stackedProportionBar.empty", table: table) }

    /// A value followed by its share.
    static func valueAndShare(_ value: String, _ share: String) -> String {
        MonarchStrings.string("monarch.stackedProportionBar.valueAndShare", table: table, value, share)
    }

    /// A share too small to write, such as "<1%".
    static func lessThan(_ share: String) -> String {
        MonarchStrings.string("monarch.stackedProportionBar.lessThan", table: table, share)
    }

    /// A share too close to the whole to write, such as ">99%".
    static func moreThan(_ share: String) -> String {
        MonarchStrings.string("monarch.stackedProportionBar.moreThan", table: table, share)
    }
}
