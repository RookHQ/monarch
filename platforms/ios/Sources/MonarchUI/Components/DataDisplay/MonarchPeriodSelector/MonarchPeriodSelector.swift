import SwiftUI

/// Picks the period a dashboard or report covers: today, the last 7 or 30
/// days, this quarter and other presets, or a custom range through
/// ``MonarchDateRangePicker``.
///
/// ```swift
/// @State private var period = MonarchPeriod(.last30Days)
///
/// MonarchPeriodSelector(
///     selection: $period,
///     presets: [.last7Days, .last30Days, .last90Days, .thisMonth, .lastMonth, .custom],
///     maximumDays: 366
/// )
/// .onChange(of: period) { _, period in load(period.resolved(today: .now, calendar: .current)) }
/// ```
///
/// The presets are the system segmented control; when the segments do not
/// fit, and at the accessibility text sizes, they become a system pop-up
/// menu. Choosing Custom shows a ``MonarchDateRangePicker`` that starts from
/// the dates already shown. The app gets dates, not labels, through
/// ``MonarchPeriod/resolved(today:calendar:)``, so every app computes periods
/// the same way.
///
/// Accessibility: the picker is named by `label` ("Period") and read as the
/// system reads a segmented control or pop-up button, with the chosen preset
/// as its value. The custom range is read as "Custom period, Start" and
/// "Custom period, End". The resolved dates are static text.
public struct MonarchPeriodSelector: View {
    private let label: String?
    @Binding private var selection: MonarchPeriod
    private let presets: [MonarchPeriodPreset]
    private let variant: MonarchPeriodSelectorVariant
    private let showsRange: Bool
    private let today: Date
    private let minimumDate: Date?
    private let maximumDate: Date?
    private let maximumDays: Int?
    private let labels: MonarchPeriodSelectorLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A period selector.
    ///
    /// - Parameters:
    ///   - label: The name of the control, already localised. Built in: "Period".
    ///   - selection: The chosen period.
    ///   - presets: The presets offered, in order. By default today, last 7
    ///     days, last 30 days, this quarter and custom.
    ///   - variant: Segments, or a pop-up menu.
    ///   - showsRange: Whether the resolved dates are shown under the control,
    ///     such as "Oct 1 – 30, 2026".
    ///   - today: The day presets count from. Pass the workspace's day to match the server.
    ///   - minimumDate: The earliest day a custom period can start.
    ///   - maximumDate: The latest day a custom period can end. By default `today`.
    ///   - maximumDays: The longest custom period in days, such as 366 for reports.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String? = nil,
        selection: Binding<MonarchPeriod>,
        presets: [MonarchPeriodPreset] = [.today, .last7Days, .last30Days, .thisQuarter, .custom],
        variant: MonarchPeriodSelectorVariant = .segmented,
        showsRange: Bool = false,
        today: Date = Date(),
        minimumDate: Date? = nil,
        maximumDate: Date? = nil,
        maximumDays: Int? = nil,
        labels: MonarchPeriodSelectorLabels = MonarchPeriodSelectorLabels()
    ) {
        self.label = label
        _selection = selection
        self.presets = presets
        self.variant = variant
        self.showsRange = showsRange
        self.today = today
        self.minimumDate = minimumDate
        self.maximumDate = maximumDate
        self.maximumDays = maximumDays
        self.labels = labels
    }

    private typealias Tokens = MonarchPeriodSelectorTokens

    private var title: String { label ?? MonarchPeriodSelectorStrings.label }

    private var presetBinding: Binding<MonarchPeriodPreset> {
        Binding(
            get: { selection.preset },
            set: { selection = Tokens.picking($0, current: selection, today: today, calendar: calendar) }
        )
    }

    /// The content of the view.
    public var body: some View {
        let resolved = selection.resolved(today: today, calendar: calendar)
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            switch Tokens.resolvedVariant(variant, isAccessibilitySize: dynamicTypeSize.isAccessibilitySize) {
            case .segmented:
                // Five presets with names do not fit the system segmented
                // control on a phone; the Monarch one scrolls instead of
                // falling back to a menu.
                MonarchSegmentedControl(
                    title,
                    selection: presetBinding,
                    items: presets.map { MonarchSegmentedItem(Tokens.name($0, labels: labels), value: $0) },
                    fillsWidth: false,
                    wraps: true
                )
            case .menu:
                MonarchSelectField(
                    title,
                    selection: presetBinding,
                    valueText: Tokens.name(selection.preset, labels: labels),
                    style: .compact
                ) {
                    ForEach(presets) { preset in
                        Text(Tokens.name(preset, labels: labels)).tag(preset)
                    }
                }
            }
            if selection.preset == .custom {
                MonarchDateRangePicker(
                    labels.customRange ?? MonarchPeriodSelectorStrings.customRange,
                    selection: Binding(
                        get: { selection.range },
                        set: { selection = MonarchPeriod(.custom, range: $0) }
                    ),
                    minimumDate: minimumDate,
                    maximumDate: maximumDate ?? today,
                    maximumDays: maximumDays,
                    today: today,
                    isClearable: false,
                    labels: labels.dateRange
                )
            }
            if Tokens.showsDates(selection, showsRange: showsRange, resolved: resolved), let resolved {
                Text(Tokens.rangeText(resolved, locale: locale, calendar: calendar))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors[role: Tokens.rangeColor])
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#if DEBUG
    #Preview("MonarchPeriodSelector") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPeriodSelector(selection: .constant(MonarchPeriod(.last30Days)), showsRange: true)
                MonarchPeriodSelector(
                    selection: .constant(MonarchPeriod(.lastMonth)),
                    presets: [.last30Days, .last90Days, .last180Days, .last365Days, .lastMonth],
                    variant: .menu,
                    showsRange: true
                )
                MonarchPeriodSelector(selection: .constant(MonarchPeriod(.custom)), maximumDays: 366)
            }
        }
    }
#endif
