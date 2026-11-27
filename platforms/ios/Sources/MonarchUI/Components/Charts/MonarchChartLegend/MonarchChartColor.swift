/// A series colour of a chart: one of the five categorical data slots of
/// the theme, or neutral for "Other", totals or a comparison period.
///
/// Status colours (danger, success, warning) are never series colours.
public enum MonarchChartColor: Sendable, Hashable, CaseIterable {
    /// The first data slot.
    case data1
    /// The second data slot.
    case data2
    /// The third data slot.
    case data3
    /// The fourth data slot.
    case data4
    /// The fifth data slot.
    case data5
    /// The subtle text colour, for "Other" or a comparison series.
    case neutral
}
