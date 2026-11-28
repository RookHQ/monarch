/// Built-in strings of the rating picker, from the `MonarchRatingPicker` strings table.
enum MonarchRatingPickerStrings {
    private static let table = "MonarchRatingPicker"

    /// The spoken value of a rating, such as "3 of 5".
    static func value(_ value: Int, of maximum: Int) -> String {
        MonarchStrings.string("monarch.ratingPicker.value", table: table, String(value), String(maximum))
    }

    /// The spoken value of a rating that has a label, such as "3 of 5, Good".
    static func value(_ value: Int, of maximum: Int, label: String) -> String {
        MonarchStrings.string(
            "monarch.ratingPicker.valueWithLabel",
            table: table,
            String(value),
            String(maximum),
            label
        )
    }

    /// The spoken value while nothing is chosen.
    static var none: String { MonarchStrings.string("monarch.ratingPicker.none", table: table) }

    /// The name of the VoiceOver action that clears the rating.
    static var clear: String { MonarchStrings.string("monarch.ratingPicker.clear", table: table) }
}
