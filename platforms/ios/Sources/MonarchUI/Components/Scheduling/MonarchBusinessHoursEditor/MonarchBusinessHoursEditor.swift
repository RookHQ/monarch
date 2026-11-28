import Foundation
import SwiftUI

private typealias Tokens = MonarchBusinessHoursEditorTokens

/// Edits a business-hours calendar: name, time zone, weekly hours, optional effective dates,
/// holidays and special-hours dates, and whether it is the default. Put it in a `Form`.
///
/// ```swift
/// @State private var hours = MonarchBusinessHoursValue(
///     timeZone: "Europe/London",
///     hours: (1...5).map { MonarchWeeklyHoursInterval(day: $0, start: 540, end: 1020) },
///     name: "Support hours"
/// )
///
/// Form {
///     MonarchBusinessHoursEditor(value: $hours, showsDefault: true, isNameRequired: true)
/// }
/// ```
///
/// The editor is a series of `Section`s, so it sits among the app's own sections: the name
/// (a `TextField`), the effective dates, the time zone (``MonarchTimezoneSelect``), the weekly
/// hours (``MonarchWeeklyHoursEditor``), one section per holiday or special date (a compact
/// `DatePicker`, a name field, a closed or open `Picker` menu and, while open, opening and
/// closing menus every `step` minutes), "Add date", and the default `Toggle`. Each date is
/// removed with its button or the trailing swipe action.
///
/// Every change writes the whole ``MonarchBusinessHoursValue``. Call
/// ``MonarchBusinessHoursValue/validate(isNameRequired:isOpenRequired:)`` when saving; the
/// editor shows the same problems, each once its part has been changed, or all of them when
/// `showsErrors` is set (after a save attempt). Dates are days in UTC. Disable the editor with
/// `.disabled(true)`.
///
/// Accessibility: section headings are headers. Each date's controls are named by its position
/// ("Date 1", "Name of date 1", "Hours on date 1", "Date 1 opens"); a date not chosen yet is a
/// button read as "Date 1, Choose a date". The remove button is read as "Remove Christmas Day",
/// or "Remove date 1" without a name, and the swipe action is also in the VoiceOver actions.
/// Problems are text with an icon in the danger colour, and each new one is announced. Touch
/// targets are at least 44 points.
public struct MonarchBusinessHoursEditor<Content: View>: View {
    @Binding private var value: MonarchBusinessHoursValue
    private let showsName: Bool
    private let showsDefault: Bool
    private let showsEffectiveDates: Bool
    private let allowsSpecialHours: Bool
    private let isNameRequired: Bool
    private let isOpenRequired: Bool
    private let showsErrors: Bool
    private let pinnedTimeZones: [String]
    private let firstWeekday: Int?
    private let uses24HourClock: Bool?
    private let step: Int
    private let isReadOnly: Bool
    private let text: MonarchBusinessHoursEditorText
    private let weeklyHoursLabels: MonarchWeeklyHoursEditorLabels
    private let content: Content

    @Environment(\.locale) private var locale
    @Environment(\.monarchTheme) private var theme
    @State private var touched: Set<MonarchBusinessHoursEditorField> = []
    @AccessibilityFocusState private var focus: Tokens.FocusTarget?

    /// A business-hours editor with the app's own sections at the end.
    ///
    /// - Parameters:
    ///   - value: The calendar. Every change writes the whole value.
    ///   - showsName: Shows the name field.
    ///   - showsDefault: Shows the Default switch.
    ///   - showsEffectiveDates: Shows Effective from and Effective until, each with a clear button.
    ///   - allowsSpecialHours: Lets a date be open at other times, not only closed all day.
    ///   - isNameRequired: An empty name is a problem.
    ///   - isOpenRequired: A week without an open interval is a problem.
    ///   - showsErrors: Shows every problem now; otherwise each shows once its part is changed.
    ///   - pinnedTimeZones: Zones listed first in the time zone picker.
    ///   - firstWeekday: The first weekday shown, as a `Calendar` weekday (1 Sunday … 7
    ///     Saturday). By default the environment calendar's `firstWeekday`.
    ///   - uses24HourClock: Shows times on a 24-hour clock; `nil` follows the locale.
    ///   - step: Minutes between time options, 1 to 60; zero or less falls back to 15.
    ///   - isReadOnly: Shows everything as text, without controls.
    ///   - labels: Replacements for the built-in strings.
    ///   - weeklyHoursLabels: Replacements for the strings of the weekly hours.
    ///   - content: Sections added at the end, such as product-specific settings.
    public init(
        value: Binding<MonarchBusinessHoursValue>,
        showsName: Bool = true,
        showsDefault: Bool = false,
        showsEffectiveDates: Bool = false,
        allowsSpecialHours: Bool = true,
        isNameRequired: Bool = false,
        isOpenRequired: Bool = false,
        showsErrors: Bool = false,
        pinnedTimeZones: [String] = [],
        firstWeekday: Int? = nil,
        uses24HourClock: Bool? = nil,
        step: Int = 15,
        isReadOnly: Bool = false,
        labels: MonarchBusinessHoursEditorLabels = MonarchBusinessHoursEditorLabels(),
        weeklyHoursLabels: MonarchWeeklyHoursEditorLabels = MonarchWeeklyHoursEditorLabels(),
        @ViewBuilder content: () -> Content
    ) {
        _value = value
        self.showsName = showsName
        self.showsDefault = showsDefault
        self.showsEffectiveDates = showsEffectiveDates
        self.allowsSpecialHours = allowsSpecialHours
        self.isNameRequired = isNameRequired
        self.isOpenRequired = isOpenRequired
        self.showsErrors = showsErrors
        self.pinnedTimeZones = pinnedTimeZones
        self.firstWeekday = firstWeekday
        self.uses24HourClock = uses24HourClock
        self.step = MonarchWeeklyHoursEditorTokens.step(step)
        self.isReadOnly = isReadOnly
        self.text = MonarchBusinessHoursEditorText(labels: labels)
        self.weeklyHoursLabels = weeklyHoursLabels
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        let errors = value.validate(isNameRequired: isNameRequired, isOpenRequired: isOpenRequired)
        let problems = Tokens.problems(
            errors,
            touched: touched,
            showsErrors: showsErrors,
            showsName: showsName,
            showsEffectiveDates: showsEffectiveDates,
            text: text
        )
        let formats = MonarchWeeklyHoursEditorFormats(
            locale: locale,
            uses24HourClock: uses24HourClock,
            midnight: text.endOfDay
        )
        if showsName {
            nameSection(problem: problems.name)
        }
        if showsEffectiveDates {
            effectiveSection(problem: problems.effective)
        }
        // The announcements hang on one section that is always there, so each is posted once.
        zoneSection
            .onChange(of: problems.all) { old, new in
                for message in Tokens.newMessages(from: old, to: new) {
                    AccessibilityNotification.Announcement(message).post()
                }
            }
        hoursSection(problem: problems.open)
        exceptionsSections(problems: problems, formats: formats)
        if showsDefault {
            defaultSection
        }
        content
    }

    private func update(_ field: MonarchBusinessHoursEditorField?, _ change: (inout MonarchBusinessHoursValue) -> Void)
    {
        var next = value
        change(&next)
        value = next
        if let field { touched.insert(field) }
    }

    private func nameSection(problem: String?) -> some View {
        Section {
            if isReadOnly {
                Text(value.name.isEmpty ? text.noDate : value.name)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityLabel(text.name)
                    .accessibilityValue(value.name.isEmpty ? text.noDate : value.name)
            } else {
                TextField(
                    text.name,
                    text: Binding(
                        get: { value.name },
                        set: { name in update(.name) { $0.name = Tokens.limited(name) } }
                    ),
                    prompt: Text(text.namePlaceholder)
                )
                .monarchTextStyle(.body)
                .monarchMinimumHitTarget()
            }
        } header: {
            // The red required mark after the name, as on the web; VoiceOver hears the name only.
            (Text(text.name)
                + Text(isNameRequired && !isReadOnly ? " *" : "")
                .foregroundStyle(theme.colors.dangerStrong))
                .accessibilityLabel(text.name)
        } footer: {
            notice(problem)
        }
    }

    private func effectiveSection(problem: String?) -> some View {
        Section {
            MonarchBusinessHoursDateRow(
                label: text.effectiveFrom,
                date: value.effectiveFrom,
                minimum: nil,
                isClearable: true,
                isReadOnly: isReadOnly,
                text: text,
                onChange: { date in update(.effective) { $0.effectiveFrom = date } }
            )
            MonarchBusinessHoursDateRow(
                label: text.effectiveTo,
                date: value.effectiveTo,
                minimum: value.effectiveFrom,
                isClearable: true,
                isReadOnly: isReadOnly,
                text: text,
                onChange: { date in update(.effective) { $0.effectiveTo = date } }
            )
        } footer: {
            notice(problem)
        }
    }

    private var zoneSection: some View {
        Section {
            if isReadOnly {
                LabeledContent(text.timeZone, value: value.timeZone)
            } else {
                MonarchTimezoneSelect(
                    text.timeZone,
                    selection: Binding(
                        get: { value.timeZone },
                        set: { zone in
                            guard let zone else { return }
                            update(nil) { $0.timeZone = zone }
                        }
                    ),
                    pinned: pinnedTimeZones,
                    isClearable: false
                )
            }
        } footer: {
            Text(text.timeZoneDescription)
        }
    }

    private func hoursSection(problem: String?) -> some View {
        Section {
            MonarchWeeklyHoursEditor(
                value: Binding(
                    get: { value.hours },
                    set: { hours in update(.hours) { $0.hours = hours } }
                ),
                firstWeekday: firstWeekday,
                uses24HourClock: uses24HourClock,
                step: step,
                isReadOnly: isReadOnly,
                labels: weeklyHoursLabels
            ) {
                Text(verbatim: value.timeZone)
            }
        } footer: {
            notice(problem)
        }
    }

    @ViewBuilder private func exceptionsSections(
        problems: MonarchBusinessHoursEditorProblems,
        formats: MonarchWeeklyHoursEditorFormats
    ) -> some View {
        Section {
            if value.exceptions.isEmpty {
                Text(text.noExceptions)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.muted])
            }
        } header: {
            Text(text.exceptions)
        } footer: {
            Text(text.exceptionsDescription)
        }
        let weeklyText = MonarchWeeklyHoursEditorText(labels: weeklyHoursLabels)
        ForEach(Array(value.exceptions.enumerated()), id: \.offset) { index, exception in
            MonarchBusinessHoursExceptionSection(
                exception: exception,
                position: index + 1,
                problem: problems.exceptions[index],
                allowsSpecialHours: allowsSpecialHours,
                isReadOnly: isReadOnly,
                step: step,
                formats: formats,
                text: text,
                weeklyText: weeklyText,
                onChange: { next in
                    update(.exception(index)) { $0.exceptions = Tokens.replacing($0.exceptions, at: index, with: next) }
                },
                onRemove: { remove(at: index) },
                focus: $focus
            )
        }
        if !isReadOnly {
            Section {
                Button(text.addException, systemImage: Tokens.addSymbol) {
                    update(nil) { $0.exceptions.append(MonarchBusinessHoursException(date: nil, name: "")) }
                }
                .buttonStyle(.monarch(.secondary, size: .sm))
                .accessibilityFocused($focus, equals: .add)
            }
        }
    }

    private var defaultSection: some View {
        Section {
            Toggle(
                isOn: Binding(
                    get: { value.isDefault },
                    set: { isOn in update(nil) { $0.isDefault = isOn } }
                )
            ) {
                VStack(alignment: .leading, spacing: Tokens.textGap) {
                    Text(text.defaultTitle)
                        .monarchTextStyle(.body)
                    Text(text.defaultDescription)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.muted])
                }
            }
            .disabled(isReadOnly)
            .monarchMinimumHitTarget()
        }
    }

    @ViewBuilder private func notice(_ problem: String?) -> some View {
        if let problem {
            MonarchInlineNotice(problem, tone: .danger, variant: .plain)
        }
    }

    private func remove(at index: Int) {
        let next = Tokens.focusAfterRemoval(removedIndex: index, count: value.exceptions.count)
        focus = next
        touched = Tokens.touched(touched, removing: index)
        update(nil) { $0.exceptions = Tokens.removing($0.exceptions, at: index) }
    }
}

extension MonarchBusinessHoursEditor where Content == EmptyView {
    /// A business-hours editor. Each parameter is described on
    /// ``init(value:showsName:showsDefault:showsEffectiveDates:allowsSpecialHours:isNameRequired:isOpenRequired:showsErrors:pinnedTimeZones:firstWeekday:uses24HourClock:step:isReadOnly:labels:weeklyHoursLabels:content:)``.
    public init(
        value: Binding<MonarchBusinessHoursValue>,
        showsName: Bool = true,
        showsDefault: Bool = false,
        showsEffectiveDates: Bool = false,
        allowsSpecialHours: Bool = true,
        isNameRequired: Bool = false,
        isOpenRequired: Bool = false,
        showsErrors: Bool = false,
        pinnedTimeZones: [String] = [],
        firstWeekday: Int? = nil,
        uses24HourClock: Bool? = nil,
        step: Int = 15,
        isReadOnly: Bool = false,
        labels: MonarchBusinessHoursEditorLabels = MonarchBusinessHoursEditorLabels(),
        weeklyHoursLabels: MonarchWeeklyHoursEditorLabels = MonarchWeeklyHoursEditorLabels()
    ) {
        self.init(
            value: value,
            showsName: showsName,
            showsDefault: showsDefault,
            showsEffectiveDates: showsEffectiveDates,
            allowsSpecialHours: allowsSpecialHours,
            isNameRequired: isNameRequired,
            isOpenRequired: isOpenRequired,
            showsErrors: showsErrors,
            pinnedTimeZones: pinnedTimeZones,
            firstWeekday: firstWeekday,
            uses24HourClock: uses24HourClock,
            step: step,
            isReadOnly: isReadOnly,
            labels: labels,
            weeklyHoursLabels: weeklyHoursLabels,
            content: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchBusinessHoursEditor") {
        MonarchPreviewMatrix {
            Form {
                Group {
                    MonarchBusinessHoursEditor(
                        value: .constant(
                            MonarchBusinessHoursValue(
                                timeZone: "Europe/London",
                                hours: (1...5).map { MonarchWeeklyHoursInterval(day: $0, start: 540, end: 1020) },
                                exceptions: [
                                    MonarchBusinessHoursException(
                                        date: Date(timeIntervalSince1970: 1_798_156_800),
                                        name: "Christmas Day"
                                    ),
                                    MonarchBusinessHoursException(
                                        date: Date(timeIntervalSince1970: 1_798_675_200),
                                        name: "New Year’s Eve",
                                        intervals: [MonarchBusinessHoursInterval(start: 540, end: 780)]
                                    ),
                                    MonarchBusinessHoursException(date: nil, name: ""),
                                ],
                                name: "Support hours"
                            )
                        ),
                        showsDefault: true,
                        showsEffectiveDates: true,
                        isNameRequired: true,
                        showsErrors: true,
                        firstWeekday: 2
                    )

                }
                .monarchRowsOnSurface()
            }
            .monarchCanvas()
            .frame(minHeight: 1_200)
        }
    }
#endif
