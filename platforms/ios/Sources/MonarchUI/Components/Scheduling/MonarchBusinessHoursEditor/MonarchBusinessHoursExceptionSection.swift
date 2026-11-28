import SwiftUI

private typealias Tokens = MonarchBusinessHoursEditorTokens

/// One holiday or special-hours date of ``MonarchBusinessHoursEditor`` as a form `Section`,
/// headed by its name: the date, the name field, the closed or open menu, the opening and
/// closing menus while open, and a remove button (also a trailing swipe action). The problem
/// shows in the footer. Read-only, the date and hours are text.
struct MonarchBusinessHoursExceptionSection: View {
    let exception: MonarchBusinessHoursException
    let position: Int
    let problem: String?
    let allowsSpecialHours: Bool
    let isReadOnly: Bool
    let step: Int
    let formats: MonarchWeeklyHoursEditorFormats
    let text: MonarchBusinessHoursEditorText
    let weeklyText: MonarchWeeklyHoursEditorText
    let onChange: @MainActor (MonarchBusinessHoursException) -> Void
    let onRemove: @MainActor () -> Void
    let focus: AccessibilityFocusState<MonarchBusinessHoursEditorTokens.FocusTarget?>.Binding

    var body: some View {
        Section {
            if isReadOnly {
                MonarchBusinessHoursDateRow(
                    label: text.exceptionDate(position),
                    date: exception.date,
                    minimum: nil,
                    isClearable: false,
                    isReadOnly: true,
                    text: text,
                    onChange: { _ in }
                )
                LabeledContent(
                    text.exceptionKind(position),
                    value: Tokens.hoursText(exception, text: text, weeklyText: weeklyText, formats: formats)
                )
            } else {
                // A Group passes the swipe action to each row of the date.
                Group { editing }
                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                        Button(role: .destructive) {
                            onRemove()
                        } label: {
                            Label(text.remove, systemImage: Tokens.removeSymbol)
                        }
                    }
            }
        } header: {
            Text(text.rowTitle(exception, position: position))
                .accessibilityFocused(focus, equals: .date(position - 1))
        } footer: {
            if let problem {
                MonarchInlineNotice(problem, tone: .danger, variant: .plain)
            }
        }
    }

    @ViewBuilder private var editing: some View {
        MonarchBusinessHoursDateRow(
            label: text.exceptionDate(position),
            date: exception.date,
            minimum: nil,
            isClearable: false,
            isReadOnly: false,
            text: text,
            onChange: { date in
                var next = exception
                next.date = date
                onChange(next)
            }
        )
        TextField(
            text.exceptionName(position),
            text: Binding(
                get: { exception.name },
                set: { name in
                    var next = exception
                    next.name = Tokens.limited(name)
                    onChange(next)
                }
            ),
            prompt: Text(text.exceptionNamePlaceholder)
        )
        .monarchTextStyle(.body)
        .monarchMinimumHitTarget()
        if allowsSpecialHours {
            Picker(
                text.exceptionKind(position),
                selection: Binding(
                    get: { !exception.intervals.isEmpty },
                    set: { onChange(Tokens.settingOpen(exception, isOpen: $0)) }
                )
            ) {
                Text(text.closed).tag(false)
                Text(text.specialHours).tag(true)
            }
            .pickerStyle(.menu)
            .monarchMinimumHitTarget()
        }
        if let first = exception.intervals.first {
            timePicker(
                text.exceptionStart(position),
                options: MonarchWeeklyHoursEditorTokens.startOptions(step: step, current: first.start),
                selection: Binding(
                    get: { first.start },
                    set: { onChange(Tokens.settingFirst(exception, start: $0)) }
                )
            )
            timePicker(
                text.exceptionEnd(position),
                options: MonarchWeeklyHoursEditorTokens.endOptions(step: step, current: first.end),
                selection: Binding(
                    get: { first.end },
                    set: { onChange(Tokens.settingFirst(exception, end: $0)) }
                )
            )
        }
        Button(text.remove, systemImage: Tokens.removeSymbol, role: .destructive) { onRemove() }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .accessibilityLabel(text.removeException(position, name: exception.name))
    }

    private func timePicker(_ title: String, options: [Int], selection: Binding<Int>) -> some View {
        Picker(title, selection: selection) {
            ForEach(options, id: \.self) { minutes in
                Text(formats.time(minutes)).tag(minutes)
            }
        }
        .pickerStyle(.menu)
        .monarchMinimumHitTarget()
    }
}
