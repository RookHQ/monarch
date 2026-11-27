import SwiftUI

private typealias Tokens = MonarchBusinessHoursEditorTokens

/// One optional date of ``MonarchBusinessHoursEditor`` as a form row: a compact `DatePicker`
/// once a date is chosen (with a clear button when it may be empty), a "Choose a date" button
/// before, and the date as text when read-only. The picker runs in UTC so the chosen day is
/// the stored day.
struct MonarchBusinessHoursDateRow: View {
    let label: String
    let date: Date?
    let minimum: Date?
    let isClearable: Bool
    let isReadOnly: Bool
    let text: MonarchBusinessHoursEditorText
    let onChange: @MainActor (Date?) -> Void

    @Environment(\.calendar) private var calendar
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    var body: some View {
        if isReadOnly {
            LabeledContent(
                label,
                value: date.map { Tokens.dateText($0, locale: locale, calendar: calendar) } ?? text.noDate
            )
        } else if let date {
            HStack {
                DatePicker(
                    label,
                    selection: Binding(get: { date }, set: { onChange(Tokens.day($0)) }),
                    in: (minimum.map(Tokens.day) ?? .distantPast)...,
                    displayedComponents: .date
                )
                .datePickerStyle(.compact)
                .environment(\.timeZone, Tokens.utc)
                .environment(\.calendar, Tokens.pickerCalendar(calendar))
                .monarchMinimumHitTarget()
                if isClearable {
                    Button(text.clearDate, systemImage: Tokens.clearSymbol) { onChange(nil) }
                        .labelStyle(.iconOnly)
                        .buttonStyle(.monarch(.ghost, size: .sm))
                        .accessibilityLabel(text.clearDateItem(label))
                }
            }
        } else {
            LabeledContent(label) {
                Button(text.chooseDate) {
                    onChange(Tokens.firstChoice(today: Tokens.today(now: Date(), in: timeZone), minimum: minimum))
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .accessibilityLabel(label)
                .accessibilityValue(text.chooseDate)
            }
        }
    }
}
