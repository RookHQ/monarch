import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchChartLegend``.
enum MonarchChartLegendTokens {
    /// The number of data slots before series fall back to neutral.
    static let paletteSize = 5
    /// The gap between two entries in a row.
    static let entrySpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between two entries in a column.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the marker, the label and the value.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding before and after the content of a toggle entry.
    static let togglePadding: CGFloat = MonarchTokens.Space.space1_5
    /// The side of a square or dot marker, and the height of the line
    /// marker's frame, before Dynamic Type.
    static let markerSize: CGFloat = MonarchTokens.Space.space2_5
    /// The length of a line marker before Dynamic Type.
    static let lineLength: CGFloat = MonarchTokens.Space.space4
    /// The thickness of a line marker and of an outlined marker's border.
    static let strokeWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The dash and gap lengths of a dashed marker.
    static let dash: [CGFloat] = [MonarchTokens.Space.space1, MonarchTokens.Space.space0_5]
    /// The corner radius of a square marker.
    static let squareRadius: CGFloat = MonarchTokens.Space.space0_5
    /// The type role of labels and values.
    static let textStyle: MonarchTextStyle = .caption

    /// The colour slot of an entry: its own, or the slot of its position.
    /// Slots are given in order and never cycled; the sixth series and
    /// later are neutral.
    static func color(_ explicit: MonarchChartColor?, index: Int) -> MonarchChartColor {
        if let explicit { return explicit }
        let slots: [MonarchChartColor] = [.data1, .data2, .data3, .data4, .data5]
        return index >= 0 && index < paletteSize ? slots[index] : .neutral
    }

    /// The theme role of a colour slot.
    static func role(_ color: MonarchChartColor) -> MonarchColorRole {
        switch color {
        case .data1: \.data1
        case .data2: \.data2
        case .data3: \.data3
        case .data4: \.data4
        case .data5: \.data5
        case .neutral: \.textSubtle
        }
    }

    /// The role of a marker: the series colour, or the disabled text colour
    /// while the series is hidden.
    static func markerRole(_ color: MonarchChartColor, isHidden: Bool) -> MonarchColorRole {
        isHidden ? \.textDisabled : role(color)
    }

    /// The role of a label: default, or muted while the series is hidden.
    static func labelRole(isHidden: Bool) -> MonarchColorRole {
        isHidden ? \.textMuted : \.textDefault
    }

    /// Whether the entry is the last visible series and may not be hidden.
    static func isLocked(_ key: String, hidden: Set<String>, keys: [String], keepsOneVisible: Bool) -> Bool {
        guard keepsOneVisible, !hidden.contains(key) else { return false }
        return keys.filter { !hidden.contains($0) }.count <= 1
    }

    /// The hidden set after an entry is switched. A locked entry stays visible.
    static func toggled(
        _ hidden: Set<String>,
        key: String,
        isVisible: Bool,
        keys: [String],
        keepsOneVisible: Bool
    ) -> Set<String> {
        var next = hidden
        if isVisible {
            next.remove(key)
        } else if !isLocked(key, hidden: hidden, keys: keys, keepsOneVisible: keepsOneVisible) {
            next.insert(key)
        }
        return next
    }

    /// The hidden set after Show all: the legend's own keys are shown again;
    /// keys it does not list stay hidden.
    static func showingAll(_ hidden: Set<String>, keys: [String]) -> Set<String> {
        hidden.subtracting(keys)
    }

    /// Whether the Show all button is shown: only while one of the legend's
    /// own series is hidden.
    static func showsShowAll(hidden: Set<String>, keys: [String], isEnabled: Bool) -> Bool {
        isEnabled && keys.contains { hidden.contains($0) }
    }

    /// What VoiceOver reads for an entry: the label, then the value.
    static func accessibilityLabel(label: String, value: String?) -> String {
        guard let value, !value.isEmpty else { return label }
        return "\(label), \(value)"
    }
}
