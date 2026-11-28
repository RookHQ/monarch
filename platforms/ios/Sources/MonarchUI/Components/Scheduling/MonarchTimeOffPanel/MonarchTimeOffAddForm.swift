import SwiftUI

/// The add form of ``MonarchTimeOffPanel``, shown in a sheet: who, type,
/// all day or timed, the dates in the panel's time zone, and a reason.
struct MonarchTimeOffAddForm: View {
    let people: [MonarchTimeOffOption]
    let allowsClosure: Bool
    let types: [MonarchTimeOffOption]
    let now: Date
    let timeZone: TimeZone
    let labels: MonarchTimeOffPanelLabels
    let onAdd: @MainActor (MonarchTimeOffDraft) async throws -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var environmentCalendar
    @Environment(\.locale) private var locale
    @State private var form: MonarchTimeOffForm?
    @State private var isRunning = false
    @State private var failure: String?

    private typealias Tokens = MonarchTimeOffPanelTokens

    /// The calendar in the panel's time zone, so days and wall times are the zone's.
    private var calendar: Calendar {
        var calendar = environmentCalendar
        calendar.timeZone = timeZone
        return calendar
    }

    var body: some View {
        let current = form ?? Tokens.initialForm(people: people, types: types, now: now, calendar: calendar)
        let problem = Tokens.problem(current, labels: labels, calendar: calendar)
        NavigationStack {
            Form {
                Group {
                    details(current)
                    dates(current, problem: problem)
                    Section {
                        TextField(
                            labels.reason ?? MonarchTimeOffPanelStrings.reason,
                            text: binding(\.reason, current: current),
                            axis: .vertical
                        )
                        .lineLimit(2...)
                        .disabled(isRunning)
                    } footer: {
                        Text(labels.reasonHelper ?? MonarchTimeOffPanelStrings.reasonHelper)
                    }
                    if let failure {
                        Section {
                            MonarchBanner(message: failure, tone: .danger)
                        }
                    }

                }
                .monarchRowsOnSurface()
            }
            .monarchCanvas()
            .navigationTitle(labels.formTitle ?? MonarchTimeOffPanelStrings.formTitle)
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.cancel ?? MonarchTimeOffPanelStrings.cancel) { dismiss() }
                        .disabled(isRunning)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isRunning {
                        ProgressView()
                            .accessibilityLabel(labels.save ?? MonarchTimeOffPanelStrings.save)
                    } else {
                        Button(labels.save ?? MonarchTimeOffPanelStrings.save) { submit(current) }
                            .disabled(problem != nil)
                    }
                }
            }
        }
        .environment(\.timeZone, timeZone)
        .interactiveDismissDisabled(isRunning)
        .onChange(of: current.reason) { _, reason in
            let limited = Tokens.limited(reason)
            if limited != reason { form?.reason = limited }
        }
        .onChange(of: failure) { _, failure in
            if let failure { AccessibilityNotification.Announcement(failure).post() }
        }
    }

    @ViewBuilder private func details(_ current: MonarchTimeOffForm) -> some View {
        let options = Tokens.whoOptions(people: people, allowsClosure: allowsClosure)
        let everyone = labels.everyone ?? MonarchTimeOffPanelStrings.everyone
        Section {
            if options.count > 1 {
                Picker(labels.who ?? MonarchTimeOffPanelStrings.who, selection: binding(\.personID, current: current)) {
                    ForEach(options, id: \.self) { option in
                        Text(option?.label ?? everyone).tag(option?.value)
                    }
                }
            }
            if current.personID == nil, allowsClosure {
                MonarchBanner(message: labels.closureNotice ?? MonarchTimeOffPanelStrings.closureNotice, tone: .info)
            }
            if !types.isEmpty {
                Picker(labels.type ?? MonarchTimeOffPanelStrings.type, selection: binding(\.type, current: current)) {
                    ForEach(types) { type in
                        Text(type.label).tag(Optional(type.value))
                    }
                }
            }
            Toggle(
                labels.allDayToggle ?? MonarchTimeOffPanelStrings.allDayToggle,
                isOn: binding(\.isAllDay, current: current)
            )
        }
        .disabled(isRunning)
    }

    @ViewBuilder private func dates(_ current: MonarchTimeOffForm, problem: String?) -> some View {
        Section {
            if current.isAllDay {
                DatePicker(
                    labels.from ?? MonarchTimeOffPanelStrings.from,
                    selection: Binding(
                        get: { current.fromDay },
                        set: { form = Tokens.settingFromDay($0, form: current, calendar: calendar) }
                    ),
                    displayedComponents: .date
                )
                DatePicker(
                    labels.until ?? MonarchTimeOffPanelStrings.until,
                    selection: binding(\.untilDay, current: current),
                    in: current.fromDay...,
                    displayedComponents: .date
                )
            } else {
                DatePicker(
                    labels.start ?? MonarchTimeOffPanelStrings.start,
                    selection: binding(\.start, current: current)
                )
                DatePicker(labels.end ?? MonarchTimeOffPanelStrings.end, selection: binding(\.end, current: current))
            }
            if let problem {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.lineSpacing) {
                    MonarchIcon(systemName: MonarchFormFieldTokens.errorSymbol, size: .sm)
                    Text(problem).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors[role: Tokens.errorRole])
                .accessibilityElement(children: .combine)
            }
        } footer: {
            Text(Tokens.zoneNote(timeZone, labels: labels, locale: locale))
        }
        .disabled(isRunning)
    }

    private func binding<Value>(_ keyPath: WritableKeyPath<MonarchTimeOffForm, Value>, current: MonarchTimeOffForm)
        -> Binding<Value>
    {
        Binding(
            get: { (form ?? current)[keyPath: keyPath] },
            set: { value in
                var next = form ?? current
                next[keyPath: keyPath] = value
                form = next
            }
        )
    }

    private func submit(_ current: MonarchTimeOffForm) {
        guard !isRunning, Tokens.problem(current, labels: labels, calendar: calendar) == nil else { return }
        let draft = Tokens.draft(current, offersTypes: !types.isEmpty, calendar: calendar)
        failure = nil
        isRunning = true
        Task {
            do {
                try await onAdd(draft)
                isRunning = false
                dismiss()
            } catch {
                isRunning = false
                failure = Tokens.failure(error, labels: labels)
            }
        }
    }
}
