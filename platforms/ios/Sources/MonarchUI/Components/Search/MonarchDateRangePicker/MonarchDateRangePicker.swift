import MonarchTokens
import SwiftUI

/// Two dates that bound a period, as a start and an end row of system date
/// pickers, with quick picks such as "Last 7 days", a clear button, the
/// earliest and latest day, and a shortest and longest length.
///
/// ```swift
/// MonarchDateRangePicker(
///     "Report period",
///     selection: $period,
///     maximumDate: .now,
///     maximumDays: 366,
///     presets: [.lastDays(7, label: "Last 7 days"), .month(-1, label: "Last month")]
/// )
/// ```
///
/// iOS has no range calendar, so this follows Calendar and Settings: a Start
/// row and an End row, each the system `DatePicker`, which opens the system
/// calendar. The pickers only offer days that keep the rules: the end is
/// never before the start, and the range is never shorter than
/// `minimumDays` or longer than `maximumDays`. Moving the start keeps the
/// length, as Calendar does. The range holds the first moment of the first
/// and last day, both inclusive, in the environment's calendar.
///
/// `isDateDisabled` cannot grey out days in the system calendar; a first or
/// last day it rejects shows the error "That date is not available." and the
/// app keeps the range from being used.
///
/// Accessibility: the label and help text are read with each picker ("Trip
/// dates, Start", then the date), and an error is read in the hint of both
/// and announced when it appears. The error has an icon and text. Quick
/// picks are a system menu whose current choice has a checkmark. Text scales
/// with Dynamic Type.
public struct MonarchDateRangePicker: View {
    private let label: String
    @Binding private var selection: ClosedRange<Date>?
    private let helper: String?
    private let minimumDate: Date?
    private let maximumDate: Date?
    private let minimumDays: Int
    private let maximumDays: Int?
    private let presets: [MonarchDateRangePreset]
    private let today: Date
    private let isRequired: Bool
    private let isClearable: Bool
    private let isDateDisabled: (@MainActor (Date) -> Bool)?
    private let labels: MonarchDateRangePickerLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @Environment(\.isEnabled) private var isEnabled
    @State private var isEditing = false

    /// A date range picker.
    ///
    /// - Parameters:
    ///   - label: The field's label, already localised.
    ///   - selection: The first and last day, inclusive, or `nil` for none.
    ///   - helper: Help text under the label.
    ///   - minimumDate: The earliest day that can be chosen.
    ///   - maximumDate: The latest day that can be chosen.
    ///   - minimumDays: The shortest range in days, counting both ends.
    ///   - maximumDays: The longest range in days, counting both ends, such
    ///     as 366 for a report.
    ///   - presets: Quick picks, shown in a menu under the pickers. A preset
    ///     that breaks a rule is disabled.
    ///   - today: The day relative presets and Choose dates start from.
    ///   - isRequired: Whether the field is marked and read as required.
    ///   - isClearable: Whether a clear button shows while there is a range.
    ///   - isDateDisabled: Returns `true` for a day that cannot be a first or
    ///     last day, such as a closed day.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String,
        selection: Binding<ClosedRange<Date>?>,
        helper: String? = nil,
        minimumDate: Date? = nil,
        maximumDate: Date? = nil,
        minimumDays: Int = 1,
        maximumDays: Int? = nil,
        presets: [MonarchDateRangePreset] = [],
        today: Date = Date(),
        isRequired: Bool = false,
        isClearable: Bool = true,
        isDateDisabled: (@MainActor (Date) -> Bool)? = nil,
        labels: MonarchDateRangePickerLabels = MonarchDateRangePickerLabels()
    ) {
        self.label = label
        _selection = selection
        self.helper = helper
        self.minimumDate = minimumDate
        self.maximumDate = maximumDate
        self.minimumDays = minimumDays
        self.maximumDays = maximumDays
        self.presets = presets
        self.today = today
        self.isRequired = isRequired
        self.isClearable = isClearable
        self.isDateDisabled = isDateDisabled
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        let limits = MonarchDateRangeLimits(
            minimumDate: minimumDate,
            maximumDate: maximumDate,
            minimumDays: minimumDays,
            maximumDays: maximumDays,
            calendar: calendar
        )
        let error = MonarchDateRangePickerTokens.error(
            selection,
            isDisabled: { isDateDisabled?($0) ?? false },
            labels: labels,
            calendar: calendar
        )
        let hint = MonarchFormFieldTokens.accessibilityHint(
            helper: helper,
            error: error,
            isRequired: isRequired,
            labels: MonarchFormFieldLabels()
        )
        VStack(alignment: .leading, spacing: MonarchDateRangePickerTokens.spacing) {
            header
            field(limits: limits, hint: hint, hasError: error != nil)
            if let error {
                errorText(error)
            } else if let helper, !helper.isEmpty {
                Text(helper)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.helperColor(isEnabled: isEnabled)])
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityHidden(true)
            }
        }
        .sheet(isPresented: $isEditing) {
            editor(limits: limits, hint: hint)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: error) { _, newError in
            if let newError {
                AccessibilityNotification.Announcement(
                    MonarchFormFieldTokens.spokenError(newError, labels: MonarchFormFieldLabels())
                ).post()
            }
        }
    }

    // MARK: Parts

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchFormFieldTokens.textSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.markSpacing) {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.labelColor(isEnabled: isEnabled)])
                if isRequired {
                    Text(MonarchFormFieldTokens.requiredMark)
                        .monarchTextStyle(.label)
                        .foregroundStyle(
                            theme.colors[role: MonarchFormFieldTokens.requiredColor(isEnabled: isEnabled)]
                        )
                }
            }
        }
        .accessibilityHidden(true)
    }

    /// The field as the web draws it: the range as numeric dates, a clear button and a calendar icon. Pressing it
    /// opens the sheet that chooses the days.
    private func field(limits: MonarchDateRangeLimits, hint: String, hasError: Bool) -> some View {
        let text = selection.map { rangeText($0) } ?? ""
        let choose = labels.chooseDates ?? MonarchDateRangePickerStrings.chooseDates
        return MonarchPickerField(
            text,
            placeholder: choose,
            leading: { EmptyView() },
            control: {
                Button(action: { open(limits: limits) }) {
                    Color.clear.frame(maxWidth: .infinity, minHeight: 44)
                }
                .accessibilityLabel(label)
                .accessibilityValue(text.isEmpty ? choose : text)
                .accessibilityHint(hint)
            },
            trailing: {
                HStack(spacing: MonarchDateRangePickerTokens.spacing) {
                    if isClearable && selection != nil {
                        Button {
                            selection = nil
                        } label: {
                            MonarchIcon(systemName: "xmark", size: .sm)
                                .frame(width: 32, height: 32)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(labels.clear ?? MonarchDateRangePickerStrings.clear)
                    }
                    Button {
                        open(limits: limits)
                    } label: {
                        MonarchIcon(systemName: MonarchDateRangePickerTokens.calendarSymbol, size: .sm)
                            .frame(width: 32, height: 32)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(choose)
                }
            }
        )
        .disabled(!isEnabled)
    }

    /// The range as numeric dates in the locale's order, such as 06/10/2026 \u{2013} 04/11/2026.
    private func rangeText(_ range: ClosedRange<Date>) -> String {
        let style = Date.FormatStyle(locale: locale, calendar: calendar).day(.twoDigits).month(.twoDigits).year()
        return "\(range.lowerBound.formatted(style)) \u{2013} \(range.upperBound.formatted(style))"
    }

    private func open(limits: MonarchDateRangeLimits) {
        if selection == nil {
            selection = MonarchDateRangePickerTokens.initialRange(today: today, limits: limits)
        }
        isEditing = true
    }

    /// The sheet that chooses the first and last day, with the length, the quick picks and Clear.
    private func editor(limits: MonarchDateRangeLimits, hint: String) -> some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: MonarchDateRangePickerTokens.actionSpacing) {
                if let range = selection {
                    pickers(range, limits: limits, hint: hint)
                }
                actions(limits: limits)
                Spacer(minLength: 0)
            }
            .padding()
            .navigationTitle(label)
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(MonarchDateRangePickerStrings.done) { isEditing = false }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func pickers(_ range: ClosedRange<Date>, limits: MonarchDateRangeLimits, hint: String) -> some View {
        let start = MonarchDateRangePickerTokens.start(labels)
        let end = MonarchDateRangePickerTokens.end(labels)
        let count = MonarchDateRangePickerTokens.dayCount(range, calendar: calendar)
        return VStack(alignment: .leading, spacing: MonarchDateRangePickerTokens.spacing) {
            DatePicker(
                selection: Binding(
                    get: { range.lowerBound },
                    set: {
                        selection = MonarchDateRangePickerTokens.settingStart($0, current: selection, limits: limits)
                    }
                ),
                in: MonarchDateRangePickerTokens.startRange(limits),
                displayedComponents: .date
            ) {
                Text(start)
            }
            .accessibilityLabel(MonarchDateRangePickerTokens.pickerLabel(field: label, part: start))
            .accessibilityHint(hint)
            DatePicker(
                selection: Binding(
                    get: { range.upperBound },
                    set: {
                        selection = MonarchDateRangePickerTokens.settingEnd(
                            $0,
                            start: selection?.lowerBound ?? range.lowerBound,
                            limits: limits
                        )
                    }
                ),
                in: MonarchDateRangePickerTokens.endRange(start: range.lowerBound, limits: limits),
                displayedComponents: .date
            ) {
                Text(end)
            }
            .accessibilityLabel(MonarchDateRangePickerTokens.pickerLabel(field: label, part: end))
            .accessibilityHint(hint)
            Text(MonarchDateRangePickerTokens.countText(count, labels: labels, locale: locale))
                .monarchTextStyle(.caption)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: MonarchDateRangePickerTokens.countColor])
        }
        .monarchTextStyle(.body)
        .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.labelColor(isEnabled: isEnabled)])
    }

    @ViewBuilder
    private func actions(limits: MonarchDateRangeLimits) -> some View {
        let showsClear = isClearable && selection != nil
        if !presets.isEmpty || showsClear {
            HStack(spacing: MonarchDateRangePickerTokens.actionSpacing) {
                if !presets.isEmpty {
                    presetMenu(limits: limits)
                }
                if showsClear {
                    MonarchButton(
                        labels.clear ?? MonarchDateRangePickerStrings.clear,
                        variant: .ghost,
                        size: MonarchDateRangePickerTokens.buttonSize
                    ) {
                        selection = nil
                    }
                }
            }
        }
    }

    private func presetMenu(limits: MonarchDateRangeLimits) -> some View {
        let title = labels.presets ?? MonarchDateRangePickerStrings.presets
        let current = MonarchDateRangePickerTokens.matchingPreset(
            selection,
            presets: presets,
            today: today,
            calendar: calendar
        )
        return Menu {
            Picker(
                title,
                selection: Binding<Int?>(
                    get: { current },
                    set: { index in
                        guard let index, presets.indices.contains(index) else { return }
                        selection = MonarchDateRangePickerTokens.resolve(
                            presets[index],
                            today: today,
                            calendar: calendar
                        )
                    }
                )
            ) {
                ForEach(presets.indices, id: \.self) { index in
                    let range = MonarchDateRangePickerTokens.resolve(presets[index], today: today, calendar: calendar)
                    Text(presets[index].label)
                        .tag(Optional(index))
                        .disabled(!MonarchDateRangePickerTokens.fits(range, limits: limits))
                }
            }
            .pickerStyle(.inline)
        } label: {
            Label(title, systemImage: MonarchDateRangePickerTokens.presetsSymbol)
                .monarchTextStyle(.label)
                .monarchMinimumHitTarget()
        }
    }

    private func errorText(_ error: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchFormFieldTokens.messageIconSpacing) {
            MonarchIcon(systemName: MonarchFormFieldTokens.errorSymbol, size: .sm)
            Text(error)
                .monarchTextStyle(.caption)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.errorColor])
        .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchDateRangePicker") {
        let start = Calendar.current.startOfDay(for: Date())
        let end = Calendar.current.date(byAdding: .day, value: 6, to: start) ?? start
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDateRangePicker(
                    "Report period",
                    selection: .constant(start...end),
                    helper: "Up to a year.",
                    maximumDays: 366,
                    presets: [.lastDays(7, label: "Last 7 days"), .month(0, label: "This month")]
                )
                MonarchDateRangePicker("Leave", selection: .constant(nil), isRequired: true)
                MonarchDateRangePicker(
                    "Booking",
                    selection: .constant(start...end),
                    isDateDisabled: { Calendar.current.isDateInWeekend($0) }
                )
            }
        }
    }
#endif
