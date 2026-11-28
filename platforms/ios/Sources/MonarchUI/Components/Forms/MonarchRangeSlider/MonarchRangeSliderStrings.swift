/// Built-in strings of the range slider, from the `MonarchRangeSlider` strings table.
enum MonarchRangeSliderStrings {
    /// The name of the lower thumb, after the label.
    static var minimum: String {
        MonarchStrings.string("monarch.rangeSlider.minimum", table: "MonarchRangeSlider")
    }

    /// The name of the upper thumb, after the label.
    static var maximum: String {
        MonarchStrings.string("monarch.rangeSlider.maximum", table: "MonarchRangeSlider")
    }

    /// The spoken name of one thumb: the label and the thumb's name.
    static func thumb(label: String, name: String) -> String {
        MonarchStrings.string("monarch.rangeSlider.thumb", table: "MonarchRangeSlider", label, name)
    }

    /// The shown range.
    static func range(_ lower: String, _ upper: String) -> String {
        MonarchStrings.string("monarch.rangeSlider.range", table: "MonarchRangeSlider", lower, upper)
    }
}
