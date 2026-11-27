import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchFacetedFilterPanel``: the app's replacements, else the built-in ones.
struct MonarchFacetedFilterPanelText {
    let labels: MonarchFacetedFilterPanelLabels

    private func pick(_ value: String?, _ key: String) -> String {
        value ?? MonarchFacetedFilterPanelStrings.string(key)
    }

    var title: String { pick(labels.title, "title") }
    var clearAll: String { pick(labels.clearAll, "clearAll") }
    var clearGroup: String { pick(labels.clearGroup, "clearGroup") }
    var showFewer: String { pick(labels.showFewer, "showFewer") }
    var noOptions: String { pick(labels.noOptions, "noOptions") }

    func clearGroupLabel(_ group: String) -> String {
        labels.clearGroupLabel?(group) ?? MonarchFacetedFilterPanelStrings.clearGroupLabel(group)
    }

    func activeCount(_ count: Int) -> String {
        labels.activeCount?(count) ?? MonarchFacetedFilterPanelStrings.activeCount(count)
    }

    func showMore(_ hidden: Int) -> String {
        labels.showMore?(hidden) ?? MonarchFacetedFilterPanelStrings.showMore(hidden)
    }
}

/// Component tokens and selection logic for ``MonarchFacetedFilterPanel``.
enum MonarchFacetedFilterPanelTokens {
    /// The gap between the header and the groups, and between groups.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the options of a group.
    static let optionSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the label and the count of an option.
    static let countSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a group's heading and its active count.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space0_5

    /// The colour role of an option's count.
    static func countColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour role of a group's active count. Accent, because it marks a group that filters.
    static func activeCountColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.accentStrong : \.textDisabled
    }

    /// The number of active filters in one facet: the checked options, or 1 for a range.
    static func activeCount(_ facet: MonarchFacet, in selection: [String: MonarchFacetSelection]) -> Int {
        switch (facet.kind, selection[facet.id]) {
        case (.options, .options(let ids)?): ids.count
        case (.range, .range?): 1
        default: 0
        }
    }

    /// The number of active filters in every facet.
    static func totalActive(_ facets: [MonarchFacet], in selection: [String: MonarchFacetSelection]) -> Int {
        facets.reduce(0) { $0 + activeCount($1, in: selection) }
    }

    /// The checked option ids of a facet.
    static func checked(_ facetID: String, in selection: [String: MonarchFacetSelection]) -> Set<String> {
        if case .options(let ids)? = selection[facetID] { return ids }
        return []
    }

    /// The selection with one option checked or cleared. A facet left with nothing checked is removed.
    static func setting(
        _ optionID: String,
        isOn: Bool,
        facetID: String,
        in selection: [String: MonarchFacetSelection]
    ) -> [String: MonarchFacetSelection] {
        var ids = checked(facetID, in: selection)
        if isOn { ids.insert(optionID) } else { ids.remove(optionID) }
        var next = selection
        next[facetID] = ids.isEmpty ? nil : .options(ids)
        return next
    }

    /// The range a facet shows: the chosen range, or the whole bounds when there is none.
    static func range(
        _ facetID: String,
        bounds: ClosedRange<Double>,
        in selection: [String: MonarchFacetSelection]
    ) -> ClosedRange<Double> {
        if case .range(let range)? = selection[facetID] { return range.clamped(to: bounds) }
        return bounds
    }

    /// The selection with a facet's range set. The whole bounds means no filter, so the facet is removed.
    static func setting(
        _ range: ClosedRange<Double>,
        bounds: ClosedRange<Double>,
        facetID: String,
        in selection: [String: MonarchFacetSelection]
    ) -> [String: MonarchFacetSelection] {
        var next = selection
        next[facetID] = range == bounds ? nil : .range(range)
        return next
    }

    /// The selection without one facet's filter.
    static func clearing(_ facetID: String, in selection: [String: MonarchFacetSelection]) -> [String:
        MonarchFacetSelection]
    {
        var next = selection
        next[facetID] = nil
        return next
    }

    /// The options shown and how many are hidden. Checked options are always shown, even past the limit.
    static func visibleOptions(
        _ options: [MonarchFacetOption],
        checked: Set<String>,
        maxVisible: Int,
        isShowingAll: Bool
    ) -> (options: [MonarchFacetOption], hidden: Int) {
        if isShowingAll { return (options, 0) }
        let shown = options.enumerated().filter { index, option in
            index < max(maxVisible, 0) || checked.contains(option.id)
        }
        .map(\.element)
        return (shown, options.count - shown.count)
    }

    /// An option's count, formatted for the locale.
    static func formattedCount(_ count: Int, locale: Locale) -> String {
        count.formatted(.number.locale(locale))
    }

    /// The name VoiceOver reads for an option: its label, then its count.
    static func optionAccessibilityLabel(_ option: MonarchFacetOption, locale: Locale) -> String {
        guard let count = option.count else { return option.label }
        return "\(option.label), \(formattedCount(count, locale: locale))"
    }

    /// A value of a range facet with its unit, such as "25 MB".
    static func rangeValueText(_ value: Double, unit: String?, locale: Locale) -> String {
        let number = value.formatted(.number.locale(locale))
        guard let unit, !unit.isEmpty else { return number }
        return "\(number) \(unit)"
    }
}
