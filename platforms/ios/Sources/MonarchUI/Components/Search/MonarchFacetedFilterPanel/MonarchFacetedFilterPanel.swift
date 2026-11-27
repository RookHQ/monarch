import MonarchTokens
import SwiftUI

/// A panel of filter groups for a search or catalogue: checkboxes with
/// counts and minimum to maximum ranges, each group collapsible and clearable,
/// with a clear-all.
///
/// ```swift
/// @State private var filters: [String: MonarchFacetSelection] = [:]
///
/// MonarchFacetedFilterPanel(
///     facets: [
///         .options(id: "status", label: "Status", options: [
///             MonarchFacetOption(id: "open", label: "Open", count: 128),
///             MonarchFacetOption(id: "done", label: "Done", count: 1_204),
///         ]),
///         .range(id: "size", label: "File size", bounds: 0...100, unit: "MB"),
///     ],
///     selection: $filters
/// )
/// ```
///
/// On iPhone present it in a `.sheet` from a Filters button, with a Done
/// button in the sheet's toolbar; on iPad it can stay beside the results.
/// Every change is written to `selection` at once; there is no Apply step
/// unless the app adds one.
///
/// Each group is a standard `DisclosureGroup` with the Monarch style, each
/// option a standard `Toggle` with the Monarch checkbox style, and each range
/// a ``MonarchRangeSlider``.
///
/// Accessibility: the panel title is a header. Each group header reads its
/// name, the number of active filters and Expanded or Collapsed. Each option
/// reads its label and count ("Open, 128"). Checked options never hide behind
/// "Show more". A group's clear button is named after the group ("Clear Status
/// filter"). Every control has a 44-point touch target.
public struct MonarchFacetedFilterPanel: View {
    private let facets: [MonarchFacet]
    @Binding private var selection: [String: MonarchFacetSelection]
    private let showsTitle: Bool
    private let labels: MonarchFacetedFilterPanelLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @State private var expanded: [String: Bool] = [:]
    @State private var showingAll: Set<String> = []

    /// A faceted filter panel.
    ///
    /// - Parameters:
    ///   - facets: The groups, in order.
    ///   - selection: The active filters by facet id. A facet without a filter has no entry.
    ///   - showsTitle: Shows the "Filters" heading. Hide it when a sheet's navigation title says it.
    ///   - labels: Replacements for the built-in strings.
    public init(
        facets: [MonarchFacet],
        selection: Binding<[String: MonarchFacetSelection]>,
        showsTitle: Bool = true,
        labels: MonarchFacetedFilterPanelLabels = MonarchFacetedFilterPanelLabels()
    ) {
        self.facets = facets
        _selection = selection
        self.showsTitle = showsTitle
        self.labels = labels
    }

    private var text: MonarchFacetedFilterPanelText { MonarchFacetedFilterPanelText(labels: labels) }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchFacetedFilterPanelTokens.sectionSpacing) {
            header
            ForEach(facets) { facet in
                MonarchDivider()
                group(facet)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text.title)
    }

    private var header: some View {
        HStack {
            if showsTitle {
                Text(text.title)
                    .monarchTextStyle(.heading)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .accessibilityAddTraits(.isHeader)
            }
            Spacer(minLength: 0)
            Button(text.clearAll) { selection = [:] }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .disabled(MonarchFacetedFilterPanelTokens.totalActive(facets, in: selection) == 0)
        }
    }

    private func group(_ facet: MonarchFacet) -> some View {
        let active = MonarchFacetedFilterPanelTokens.activeCount(facet, in: selection)
        return DisclosureGroup(isExpanded: expansion(facet)) {
            VStack(alignment: .leading, spacing: MonarchFacetedFilterPanelTokens.optionSpacing) {
                switch facet.kind {
                case .options(let options, let maxVisible):
                    optionsContent(facet, options: options, maxVisible: maxVisible)
                case .range(let bounds, let step, let unit):
                    rangeContent(facet, bounds: bounds, step: step, unit: unit)
                }
                if active > 0 {
                    Button(text.clearGroup) {
                        selection = MonarchFacetedFilterPanelTokens.clearing(facet.id, in: selection)
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    .accessibilityLabel(text.clearGroupLabel(facet.label))
                }
            }
        } label: {
            VStack(alignment: .leading, spacing: MonarchFacetedFilterPanelTokens.headingSpacing) {
                Text(facet.label)
                if active > 0 {
                    Text(text.activeCount(active))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(
                            theme.colors[role: MonarchFacetedFilterPanelTokens.activeCountColor(isEnabled: isEnabled)]
                        )
                }
            }
            .accessibilityElement(children: .combine)
        }
        .disclosureGroupStyle(.monarchSection)
    }

    @ViewBuilder
    private func optionsContent(_ facet: MonarchFacet, options: [MonarchFacetOption], maxVisible: Int) -> some View {
        if options.isEmpty {
            Text(text.noOptions)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
        } else {
            let isShowingAll = showingAll.contains(facet.id)
            let visible = MonarchFacetedFilterPanelTokens.visibleOptions(
                options,
                checked: MonarchFacetedFilterPanelTokens.checked(facet.id, in: selection),
                maxVisible: maxVisible,
                isShowingAll: isShowingAll
            )
            ForEach(visible.options) { option in
                Toggle(isOn: binding(option, facetID: facet.id)) {
                    optionLabel(option)
                }
                .toggleStyle(.monarchCheckbox)
                .disabled(option.isDisabled)
                .accessibilityLabel(MonarchFacetedFilterPanelTokens.optionAccessibilityLabel(option, locale: locale))
            }
            if visible.hidden > 0 || isShowingAll {
                Button {
                    if isShowingAll { showingAll.remove(facet.id) } else { showingAll.insert(facet.id) }
                } label: {
                    Text(isShowingAll ? text.showFewer : text.showMore(visible.hidden))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textLink)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func optionLabel(_ option: MonarchFacetOption) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchFacetedFilterPanelTokens.countSpacing) {
            Text(option.label)
            if let count = option.count {
                Text(MonarchFacetedFilterPanelTokens.formattedCount(count, locale: locale))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(
                        theme.colors[
                            role: MonarchFacetedFilterPanelTokens.countColor(isEnabled: isEnabled && !option.isDisabled)
                        ]
                    )
            }
            Spacer(minLength: 0)
        }
    }

    /// Minimum and Maximum fields with the unit after them, as the web shows a range. An empty field leaves that
    /// end open, at the facet's bound.
    private func rangeContent(_ facet: MonarchFacet, bounds: ClosedRange<Double>, step: Double, unit: String?)
        -> some View
    {
        let current = MonarchFacetedFilterPanelTokens.range(facet.id, bounds: bounds, in: selection)
        func set(low: Double?, high: Double?) {
            let lower = min(max(low ?? bounds.lowerBound, bounds.lowerBound), bounds.upperBound)
            let upper = min(max(high ?? bounds.upperBound, bounds.lowerBound), bounds.upperBound)
            selection = MonarchFacetedFilterPanelTokens.setting(
                min(lower, upper)...max(lower, upper),
                bounds: bounds,
                facetID: facet.id,
                in: selection
            )
        }
        return HStack(alignment: .bottom, spacing: MonarchFacetedFilterPanelTokens.countSpacing) {
            MonarchNumberInput(
                labels.range.minimum ?? MonarchRangeSliderStrings.minimum,
                value: Binding(
                    get: { current.lowerBound == bounds.lowerBound ? nil : current.lowerBound },
                    set: { set(low: $0, high: current.upperBound) }
                ),
                minimum: bounds.lowerBound,
                maximum: bounds.upperBound,
                step: step,
                showsStepper: false
            )
            Text("\u{2013}")
                .foregroundStyle(theme.colors.textMuted)
                .padding(.bottom, MonarchTokens.Space.space3)
                .accessibilityHidden(true)
            MonarchNumberInput(
                labels.range.maximum ?? MonarchRangeSliderStrings.maximum,
                value: Binding(
                    get: { current.upperBound == bounds.upperBound ? nil : current.upperBound },
                    set: { set(low: current.lowerBound, high: $0) }
                ),
                minimum: bounds.lowerBound,
                maximum: bounds.upperBound,
                step: step,
                showsStepper: false
            )
            if let unit, !unit.isEmpty {
                Text(unit)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(.bottom, MonarchTokens.Space.space3)
            }
        }
    }

    private func expansion(_ facet: MonarchFacet) -> Binding<Bool> {
        Binding(
            get: { expanded[facet.id] ?? facet.isInitiallyExpanded },
            set: { expanded[facet.id] = $0 }
        )
    }

    private func binding(_ option: MonarchFacetOption, facetID: String) -> Binding<Bool> {
        Binding(
            get: { MonarchFacetedFilterPanelTokens.checked(facetID, in: selection).contains(option.id) },
            set: { isOn in
                selection = MonarchFacetedFilterPanelTokens.setting(
                    option.id,
                    isOn: isOn,
                    facetID: facetID,
                    in: selection
                )
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchFacetedFilterPanel") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchFacetedFilterPanel(
                    facets: [
                        .options(
                            id: "status",
                            label: "Status",
                            options: [
                                MonarchFacetOption(id: "open", label: "Open", count: 128),
                                MonarchFacetOption(id: "review", label: "In review", count: 42),
                                MonarchFacetOption(id: "archived", label: "Archived", count: 0, isDisabled: true),
                            ],
                            maxVisible: 2
                        ),
                        .range(id: "size", label: "File size", bounds: 0...100, unit: "MB"),
                        .options(id: "type", label: "Type", options: [], isInitiallyExpanded: false),
                    ],
                    selection: .constant(["status": .options(["open"]), "size": .range(0...25)])
                )
                MonarchFacetedFilterPanel(
                    facets: [.range(id: "price", label: "Price", bounds: 0...500, step: 10)],
                    selection: .constant([:])
                )
                .disabled(true)
            }
        }
    }
#endif
