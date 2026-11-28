#if os(iOS)
    import SwiftUI
    // UIKit, because SwiftUI has no month grid that draws decorations under
    // days or refuses single days: DatePicker with the graphical style offers
    // neither. UICalendarView is the system control that does.
    import UIKit

    /// The system month grid, `UICalendarView`, with Monarch's colours, the
    /// app's bounds and disabled days, and dots as decorations.
    struct MonarchMiniMonthCalendar: UIViewRepresentable {
        @Binding var selection: Date?
        let visibleMonth: Binding<Date>?
        let minimumDate: Date?
        let maximumDate: Date?
        let isDateDisabled: (@MainActor (Date) -> Bool)?
        let dayInfo: (@MainActor (Date) -> MonarchMiniMonthDayInfo?)?

        func makeCoordinator() -> Coordinator {
            Coordinator(self)
        }

        func makeUIView(context: Context) -> UICalendarView {
            let view = UICalendarView()
            view.delegate = context.coordinator
            view.selectionBehavior = UICalendarSelectionSingleDate(delegate: context.coordinator)
            // Let SwiftUI decide the width; the grid then takes the height it needs.
            view.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
            view.setContentHuggingPriority(.defaultHigh, for: .vertical)
            return view
        }

        func updateUIView(_ view: UICalendarView, context: Context) {
            let coordinator = context.coordinator
            let calendar = context.environment.calendar
            let theme = context.environment.monarchTheme
            coordinator.parent = self
            coordinator.calendar = calendar
            coordinator.theme = theme

            if view.calendar != calendar { view.calendar = calendar }
            if view.locale != context.environment.locale { view.locale = context.environment.locale }
            view.tintColor = UIColor(theme.colors[role: MonarchMiniMonthTokens.tint])

            let single = view.selectionBehavior as? UICalendarSelectionSingleDate
            let range = MonarchMiniMonthTokens.availableRange(
                minimumDate: minimumDate,
                maximumDate: maximumDate,
                calendar: calendar
            )
            if view.availableDateRange != range {
                // A selected day outside the new range is cleared first: the
                // system view does not accept one.
                single?.setSelected(nil, animated: false)
                view.availableDateRange = range
            }
            syncSelection(single, calendar: calendar)
            syncVisibleMonth(view, coordinator: coordinator, calendar: calendar, environment: context.environment)

            // The app's closures may answer differently now: ask again.
            single?.updateSelectableDates()
            if let shown = calendar.date(from: view.visibleDateComponents) {
                let days = MonarchMiniMonthTokens.daysOfMonth(containing: shown, calendar: calendar)
                    .map { MonarchMiniMonthTokens.dayComponents($0, calendar: calendar) }
                view.reloadDecorations(forDateComponents: days, animated: false)
            }
        }

        func sizeThatFits(_ proposal: ProposedViewSize, uiView: UICalendarView, context: Context) -> CGSize? {
            guard let width = proposal.width, width.isFinite, width > 0 else { return nil }
            let key = MeasureKey(
                width: width,
                category: uiView.traitCollection.preferredContentSizeCategory,
                calendar: uiView.calendar,
                locale: uiView.locale,
                month: uiView.visibleDateComponents
            )
            return CGSize(width: width, height: context.coordinator.height(of: uiView, for: key))
        }

        /// What the height of the grid depends on.
        struct MeasureKey: Hashable {
            let width: CGFloat
            let category: UIContentSizeCategory
            let calendar: Calendar
            let locale: Locale
            let month: DateComponents
        }

        /// Shows the bound selection, when it is a day the range allows.
        private func syncSelection(_ single: UICalendarSelectionSingleDate?, calendar: Calendar) {
            let wanted = selection.flatMap { date in
                MonarchMiniMonthTokens.isWithinBounds(
                    date,
                    minimumDate: minimumDate,
                    maximumDate: maximumDate,
                    calendar: calendar
                ) ? date : nil
            }
            let shown = single?.selectedDate.flatMap { calendar.date(from: $0) }
            guard !MonarchMiniMonthTokens.isSameDay(wanted, shown, calendar: calendar) else { return }
            let components = wanted.map { MonarchMiniMonthTokens.dayComponents($0, calendar: calendar) }
            single?.setSelected(components, animated: false)
        }

        /// Shows the bound month. Without a binding, the first update shows
        /// the month of the selection.
        private func syncVisibleMonth(
            _ view: UICalendarView,
            coordinator: Coordinator,
            calendar: Calendar,
            environment: EnvironmentValues
        ) {
            let isFirstUpdate = !coordinator.isConfigured
            coordinator.isConfigured = true
            guard let target = visibleMonth?.wrappedValue ?? (isFirstUpdate ? selection : nil) else { return }
            let wanted = MonarchMiniMonthTokens.clamped(
                target,
                minimumDate: minimumDate,
                maximumDate: maximumDate,
                calendar: calendar
            )
            if let shown = calendar.date(from: view.visibleDateComponents),
                MonarchMiniMonthTokens.isSameMonth(shown, wanted, calendar: calendar)
            {
                return
            }
            view.setVisibleDateComponents(
                MonarchMiniMonthTokens.monthComponents(wanted, calendar: calendar),
                animated: !isFirstUpdate && !environment.accessibilityReduceMotion
            )
        }

        /// Answers the system view: which days can be chosen, what was
        /// chosen, which month is shown and which dots a day has.
        @MainActor
        final class Coordinator: NSObject, UICalendarViewDelegate, UICalendarSelectionSingleDateDelegate {
            var parent: MonarchMiniMonthCalendar
            var calendar = Calendar.current
            var theme = MonarchTheme.light
            var isConfigured = false
            /// Measured heights, one per set of inputs.
            private var heights: [MeasureKey: CGFloat] = [:]

            /// The height of the grid for a width, text size, calendar,
            /// locale and month, measured once.
            ///
            /// Measuring lays the calendar out again and invalidates it. In a
            /// parent shorter than its content (a stack taller than the
            /// screen) SwiftUI then measured again on every pass, and layout
            /// never finished. With each height kept, measuring again changes
            /// nothing. (A separate calendar used only for measuring is not an
            /// option: off screen it ignores the text size.)
            func height(of view: UICalendarView, for key: MeasureKey) -> CGFloat {
                if let height = heights[key] { return height }
                let height = view.systemLayoutSizeFitting(
                    CGSize(width: key.width, height: UIView.layoutFittingCompressedSize.height),
                    withHorizontalFittingPriority: .required,
                    verticalFittingPriority: .fittingSizeLevel
                ).height
                heights[key] = height
                return height
            }

            init(_ parent: MonarchMiniMonthCalendar) {
                self.parent = parent
            }

            func dateSelection(
                _ selection: UICalendarSelectionSingleDate,
                canSelectDate dateComponents: DateComponents?
            )
                -> Bool
            {
                // No components means the selected day was tapped again, which clears it.
                guard let dateComponents, let date = calendar.date(from: dateComponents) else { return true }
                return MonarchMiniMonthTokens.isSelectable(
                    date,
                    minimumDate: parent.minimumDate,
                    maximumDate: parent.maximumDate,
                    isDateDisabled: parent.isDateDisabled,
                    calendar: calendar
                )
            }

            func dateSelection(
                _ selection: UICalendarSelectionSingleDate,
                didSelectDate dateComponents: DateComponents?
            ) {
                let date = dateComponents.flatMap { calendar.date(from: $0) }
                guard !MonarchMiniMonthTokens.isSameDay(date, parent.selection, calendar: calendar) else { return }
                parent.selection = date
            }

            func calendarView(
                _ calendarView: UICalendarView,
                didChangeVisibleDateComponentsFrom previousDateComponents: DateComponents
            ) {
                guard let binding = parent.visibleMonth,
                    let shown = calendar.date(from: calendarView.visibleDateComponents),
                    !MonarchMiniMonthTokens.isSameMonth(shown, binding.wrappedValue, calendar: calendar)
                else { return }
                binding.wrappedValue = shown
            }

            func calendarView(_ calendarView: UICalendarView, decorationFor dateComponents: DateComponents)
                -> UICalendarView.Decoration?
            {
                guard let date = calendar.date(from: dateComponents),
                    let info = parent.dayInfo?(MonarchMiniMonthTokens.day(date, calendar: calendar))
                else { return nil }
                let colors = MonarchMiniMonthTokens.dotRoles(info).map { UIColor(theme.colors[role: $0]) }
                guard !colors.isEmpty else { return nil }
                let description = info.description
                return .customView {
                    MonarchMiniMonthDotsView(colors: colors, description: description)
                }
            }
        }
    }

    /// Up to three dots in a row, as the decoration of one day.
    private final class MonarchMiniMonthDotsView: UIView {
        init(colors: [UIColor], description: String?) {
            super.init(frame: .zero)
            let size = MonarchMiniMonthTokens.dotSize
            let stack = UIStackView()
            stack.axis = .horizontal
            stack.spacing = MonarchMiniMonthTokens.dotSpacing
            stack.translatesAutoresizingMaskIntoConstraints = false
            for color in colors {
                let dot = UIView()
                dot.backgroundColor = color
                dot.layer.cornerRadius = size / 2
                dot.translatesAutoresizingMaskIntoConstraints = false
                NSLayoutConstraint.activate([
                    dot.widthAnchor.constraint(equalToConstant: size),
                    dot.heightAnchor.constraint(equalToConstant: size),
                ])
                stack.addArrangedSubview(dot)
            }
            addSubview(stack)
            NSLayoutConstraint.activate([
                stack.centerXAnchor.constraint(equalTo: centerXAnchor),
                stack.centerYAnchor.constraint(equalTo: centerYAnchor),
                stack.topAnchor.constraint(greaterThanOrEqualTo: topAnchor),
                stack.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor),
            ])
            // The dots carry meaning only through the app's description.
            isAccessibilityElement = description != nil
            accessibilityLabel = description
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            return nil
        }
    }
#endif
