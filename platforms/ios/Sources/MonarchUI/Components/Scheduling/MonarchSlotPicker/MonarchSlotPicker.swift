import SwiftUI

/// Pick a day on a month calendar, then one of that day's open times, shown
/// in the visitor's time zone with an optional zone picker.
///
/// ```swift
/// MonarchSlotPicker(
///     selection: $start,
///     day: $day,
///     slots: slots,
///     timeZone: zone,
///     minimumDate: .now,
///     isLoading: isLoading,
///     onTimeZoneChange: { zone = $0 }
/// )
/// .task(id: day) { slots = await api.slots(on: day) }
/// ```
///
/// Data in, choices out: the app loads `slots` for `day` and keeps
/// `selection`. The calendar is ``MonarchMiniMonth`` (the system calendar
/// view), so days follow the environment's calendar and locale; `day` is
/// the first moment of the chosen day. The times are toggle buttons in a
/// grid that uses fewer columns as text grows, formatted in `timeZone`.
/// While loading, placeholders show; an `errorMessage` shows a danger banner
/// with Try again; a day without times says so and offers
/// `onChooseOutside`. When `selection` is on the chosen day but is not one
/// of its open times, a warning says so. Pressing the selected time again
/// clears it only with `allowsDeselect`.
///
/// With `onTimeZoneChange` the zone is a ``MonarchTimezoneSelect``;
/// without it, a note says which zone the times are in. A note also says
/// when the business works in another zone.
///
/// Accessibility: the day is a heading. The times are a container named
/// "Available times on Wednesday, November 4"; each is a toggle read as its
/// time and the places left, selected when chosen, dimmed when disabled.
/// Loading and the number of times are announced. Times are at least 44
/// points.
public struct MonarchSlotPicker: View {
    @Binding private var selection: Date?
    @Binding private var day: Date?
    private let slots: [MonarchSlotPickerSlot]
    private let timeZone: String
    private let visibleMonth: Binding<Date>?
    private let minimumDate: Date?
    private let today: Date?
    private let maximumDate: Date?
    private let availableDays: [Date]?
    private let businessTimeZone: String?
    private let pinnedTimeZones: [String]
    private let isLoading: Bool
    private let errorMessage: String?
    private let allowsDeselect: Bool
    private let labels: MonarchSlotPickerLabels
    private let isDayDisabled: (@MainActor (Date) -> Bool)?
    private let onTimeZoneChange: (@MainActor (String) -> Void)?
    private let onRetry: (@MainActor () -> Void)?
    private let onChooseOutside: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var minimumSlotWidth = MonarchSlotPickerTokens.minimumSlotWidth

    /// A slot picker.
    ///
    /// - Parameters:
    ///   - selection: The start of the chosen time, or `nil`.
    ///   - day: The day whose times are shown: the first moment of the day in
    ///     the environment's calendar, or `nil` before one is chosen. Load
    ///     `slots` for it. The selection is not cleared when it changes.
    ///   - slots: The times of `day`, in display order.
    ///   - timeZone: The IANA zone the times are shown in, such as the visitor's.
    ///   - visibleMonth: Any date in the month on screen, to load its bookable days.
    ///   - minimumDate: The first bookable day.
    ///   - maximumDate: The last bookable day.
    ///   - today: The day drawn as today in the calendar. The clock by default; fixes it for tests and previews.
    ///   - availableDays: The bookable days. When given, every other day is disabled.
    ///   - businessTimeZone: The zone the business works in. When it differs
    ///     from `timeZone`, a note says the times are converted.
    ///   - pinnedTimeZones: Zones listed first in the zone picker.
    ///   - isLoading: Whether the times of `day` are loading.
    ///   - errorMessage: Why the times could not be loaded. Shown as a danger banner.
    ///   - allowsDeselect: Whether pressing the selected time again clears it.
    ///   - labels: Replacements for the built-in strings.
    ///   - isDayDisabled: Disables more days, such as days the app knows are full.
    ///   - onTimeZoneChange: Shows the zone picker and receives the new zone.
    ///   - onRetry: Shows Try again in the error banner.
    ///   - onChooseOutside: Offers "Choose another time" when no time fits.
    public init(
        selection: Binding<Date?>,
        day: Binding<Date?>,
        slots: [MonarchSlotPickerSlot],
        timeZone: String,
        visibleMonth: Binding<Date>? = nil,
        minimumDate: Date? = nil,
        maximumDate: Date? = nil,
        today: Date? = nil,
        availableDays: [Date]? = nil,
        businessTimeZone: String? = nil,
        pinnedTimeZones: [String] = [],
        isLoading: Bool = false,
        errorMessage: String? = nil,
        allowsDeselect: Bool = false,
        labels: MonarchSlotPickerLabels = MonarchSlotPickerLabels(),
        isDayDisabled: (@MainActor (Date) -> Bool)? = nil,
        onTimeZoneChange: (@MainActor (String) -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil,
        onChooseOutside: (@MainActor () -> Void)? = nil
    ) {
        _selection = selection
        _day = day
        self.slots = slots
        self.timeZone = timeZone
        self.visibleMonth = visibleMonth
        self.minimumDate = minimumDate
        self.maximumDate = maximumDate
        self.today = today
        self.availableDays = availableDays
        self.businessTimeZone = businessTimeZone
        self.pinnedTimeZones = pinnedTimeZones
        self.isLoading = isLoading
        self.errorMessage = errorMessage
        self.allowsDeselect = allowsDeselect
        self.labels = labels
        self.isDayDisabled = isDayDisabled
        self.onTimeZoneChange = onTimeZoneChange
        self.onRetry = onRetry
        self.onChooseOutside = onChooseOutside
    }

    private typealias Tokens = MonarchSlotPickerTokens

    private var zone: TimeZone { TimeZone(identifier: timeZone) ?? calendar.timeZone }

    private var phase: Tokens.Phase {
        Tokens.phase(hasDay: day != nil, isLoading: isLoading, hasError: errorMessage != nil, slotCount: slots.count)
    }

    private var dayTitle: String { day.map { Tokens.dayTitle($0, calendar: calendar, locale: locale) } ?? "" }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            MonarchMiniMonth(
                selection: $day,
                visibleMonth: visibleMonth,
                minimumDate: minimumDate,
                maximumDate: maximumDate,
                today: today,
                isDateDisabled: isDateDisabled
            )
            .padding(theme.spacing.space3)
            .overlay(MonarchShapes.container.stroke(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth))
            times
            zoneNotes
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: phase) { _, newPhase in
            if let text = Tokens.announcement(
                newPhase,
                openCount: Tokens.openCount(slots),
                dayTitle: dayTitle,
                labels: labels
            ) {
                AccessibilityNotification.Announcement(text).post()
            }
        }
    }

    private var isDateDisabled: (@MainActor (Date) -> Bool)? {
        guard availableDays != nil || isDayDisabled != nil else { return nil }
        let availableDays = availableDays
        let calendar = calendar
        let custom = isDayDisabled
        return { date in
            Tokens.isDayDisabled(date, availableDays: availableDays, calendar: calendar, custom: custom?(date) ?? false)
        }
    }

    @ViewBuilder private var times: some View {
        if phase == .chooseDay {
            note(labels.chooseDay ?? MonarchSlotPickerStrings.chooseDay)
        } else {
            VStack(alignment: .leading, spacing: Tokens.gridSpacing) {
                Text(dayTitle)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                switch phase {
                case .loading:
                    grid(placeholders).redacted(reason: .placeholder).allowsHitTesting(false).accessibilityHidden(true)
                case .failed:
                    MonarchBanner(
                        message: errorMessage ?? "",
                        tone: .danger,
                        actionTitle: onRetry == nil ? nil : labels.retry ?? MonarchSlotPickerStrings.retry,
                        action: onRetry
                    )
                case .empty:
                    note(labels.empty ?? MonarchSlotPickerStrings.empty)
                    chooseOutsideButton
                default:
                    grid(slots)
                        .accessibilityElement(children: .contain)
                        .accessibilityLabel(labels.times?(dayTitle) ?? MonarchSlotPickerStrings.times(dayTitle))
                }
                if phase == .times || phase == .empty, let selection,
                    Tokens.isSelectionOffList(selection, slots: slots, day: day, calendar: calendar, timeZone: zone)
                {
                    let time = Tokens.timeText(selection, timeZone: zone, calendar: calendar, locale: locale)
                    MonarchBanner(
                        message: labels.notOpen?(time) ?? MonarchSlotPickerStrings.notOpen(time),
                        tone: .warning,
                        actionTitle: onChooseOutside == nil
                            ? nil : labels.chooseOutside ?? MonarchSlotPickerStrings.chooseOutside,
                        action: onChooseOutside
                    )
                }
            }
        }
    }

    private var placeholders: [MonarchSlotPickerSlot] {
        (0..<Tokens.placeholderCount).map {
            MonarchSlotPickerSlot(start: Date(timeIntervalSinceReferenceDate: Double($0) * 1_800))
        }
    }

    private func grid(_ shown: [MonarchSlotPickerSlot]) -> some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: minimumSlotWidth), spacing: Tokens.gridSpacing)],
            alignment: .leading,
            spacing: Tokens.gridSpacing
        ) {
            ForEach(shown) { slot in
                slotToggle(slot)
            }
        }
    }

    private func slotToggle(_ slot: MonarchSlotPickerSlot) -> some View {
        let time = Tokens.timeText(slot.start, timeZone: zone, calendar: calendar, locale: locale)
        return Toggle(
            isOn: Binding(
                get: { selection == slot.start },
                set: { _ in
                    selection = Tokens.selection(
                        afterPressing: slot,
                        current: selection,
                        allowsDeselect: allowsDeselect
                    )
                }
            )
        ) {
            VStack(spacing: 0) {
                Text(time).monarchTextStyle(.label).monospacedDigit().strikethrough(slot.isDisabled)
                    .lineLimit(1).minimumScaleFactor(0.8)
                if let seats = slot.seatsLeft {
                    Text(Tokens.seatsText(seats, labels: labels)).monarchTextStyle(.caption)
                }
            }
            .frame(maxWidth: .infinity)
            .modifier(
                MonarchSlotTextColor(
                    color: selection == slot.start || slot.isDisabled ? nil : theme.colors.textDefault
                )
            )
        }
        .toggleStyle(.monarchButton(Tokens.slotVariant))
        .disabled(slot.isDisabled)
        .accessibilityLabel(Tokens.slotLabel(time: time, seatsLeft: slot.seatsLeft, labels: labels))
    }

    @ViewBuilder private var chooseOutsideButton: some View {
        if let onChooseOutside {
            MonarchButton(labels.chooseOutside ?? MonarchSlotPickerStrings.chooseOutside, size: .sm) {
                onChooseOutside()
            }
        }
    }

    @ViewBuilder private var zoneNotes: some View {
        VStack(alignment: .leading, spacing: Tokens.gridSpacing) {
            if let onTimeZoneChange {
                MonarchTimezoneSelect(
                    labels.timeZone ?? MonarchSlotPickerStrings.timeZone,
                    selection: Binding(get: { timeZone }, set: { if let zone = $0 { onTimeZoneChange(zone) } }),
                    pinned: pinnedTimeZones,
                    now: day,
                    isClearable: false
                )
            } else {
                note(Tokens.shownInText(timeZone, locale: locale, at: day ?? Date(), labels: labels))
            }
            if let converted = Tokens.convertedText(
                business: businessTimeZone,
                timeZone: timeZone,
                locale: locale,
                at: day ?? Date(),
                labels: labels
            ) {
                note(converted)
            }
        }
    }

    private func note(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: Tokens.noteText])
            .lineLimit(nil)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The times read in the default text colour, as on the web, until one is chosen or unavailable, where the
/// toggle style's own colours apply.
private struct MonarchSlotTextColor: ViewModifier {
    let color: Color?

    @ViewBuilder func body(content: Content) -> some View {
        if let color {
            content.foregroundStyle(color)
        } else {
            content
        }
    }
}

#if DEBUG
    #Preview("MonarchSlotPicker") {
        let day = Calendar.current.startOfDay(for: .now)
        MonarchPreviewMatrix {
            MonarchSlotPicker(
                selection: .constant(day.addingTimeInterval(9.5 * 3_600)),
                day: .constant(day),
                slots: [9, 9.5, 10, 13.5].map { MonarchSlotPickerSlot(start: day.addingTimeInterval($0 * 3_600)) }
                    + [MonarchSlotPickerSlot(start: day.addingTimeInterval(15 * 3_600), seatsLeft: 2)],
                timeZone: TimeZone.autoupdatingCurrent.identifier
            )
        }
    }
#endif
