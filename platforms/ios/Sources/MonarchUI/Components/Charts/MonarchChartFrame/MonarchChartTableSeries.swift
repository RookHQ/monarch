/// One series of a ``MonarchChartTable``: its name and one value per category.
public struct MonarchChartTableSeries: Sendable, Hashable {
    /// The series name, already localised, such as "Revenue".
    public let name: String
    /// One value per category, in category order. `nil` is a gap.
    public let values: [Double?]
    /// The values as the app writes them, such as "$1,200", in the same
    /// order. Without it, values are formatted with the current locale.
    public let formattedValues: [String]?
    /// Whether the series holds text cells only, made with `init(_:texts:)`.
    let isText: Bool

    /// A series.
    ///
    /// - Parameters:
    ///   - name: The series name, already localised.
    ///   - values: One value per category, in category order; `nil` is a gap.
    ///   - formattedValues: The values as the app writes them, in the same order.
    public init(_ name: String, values: [Double?], formattedValues: [String]? = nil) {
        self.name = name
        self.values = values
        self.formattedValues = formattedValues
        self.isText = false
    }

    /// A series of text cells with no numbers, such as a status or a band
    /// ("High", "Low") per category. The table shows the text; the audio
    /// graph leaves the series out, as it has nothing to plot.
    ///
    /// - Parameters:
    ///   - name: The series name, already localised.
    ///   - texts: One text per category, in category order; `nil` is a gap.
    public init(_ name: String, texts: [String?]) {
        self.name = name
        self.values = texts.map { _ in nil }
        self.formattedValues = texts.map { $0 ?? "" }
        self.isText = true
    }
}
