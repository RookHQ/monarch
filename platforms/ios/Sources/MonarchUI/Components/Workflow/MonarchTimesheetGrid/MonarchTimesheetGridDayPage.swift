import SwiftUI

/// The compact-width layout of ``MonarchTimesheetGrid``: one day per page,
/// with previous and next buttons, a ``MonarchDurationInput`` per editable
/// row, and the day and period totals.
struct MonarchTimesheetGridDayPage: View {
    let model: MonarchTimesheetGridModel
    @Binding var selectedDay: Date?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    private typealias Tokens = MonarchTimesheetGridTokens
    private typealias Strings = MonarchTimesheetGridStrings

    private var labels: MonarchTimesheetGridLabels { model.labels }

    var body: some View {
        if model.days.isEmpty {
            emptyText
        } else {
            let index = Tokens.dayIndex(selected: selectedDay, days: model.days)
            let day = model.days[index]
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                header(day, index: index)
                if model.rows.isEmpty {
                    emptyText
                } else {
                    VStack(alignment: .leading, spacing: Tokens.pageRowSpacing) {
                        ForEach(model.rows) { row in
                            rowView(row, day: day)
                        }
                    }
                }
                separator
                footer(day)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(Strings.caption(labels))
        }
    }

    private var emptyText: some View {
        Text(model.emptyText)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var separator: some View {
        Rectangle()
            .fill(theme.colors.borderSubtle)
            .frame(height: MonarchShapes.borderWidth)
            .accessibilityHidden(true)
    }

    // MARK: Header

    private func header(_ day: MonarchTimesheetGridDay, index: Int) -> some View {
        HStack(spacing: Tokens.iconSpacing) {
            stepButton(
                symbol: Tokens.previousSymbol,
                label: Strings.previousDay(labels),
                target: index > 0 ? model.days[index - 1].date : nil
            )
            VStack(spacing: Tokens.lineSpacing) {
                title(day)
                if day.isLocked || day.note != nil {
                    HStack(spacing: Tokens.iconSpacing) {
                        if day.isLocked {
                            MonarchIcon(systemName: Tokens.lockSymbol, size: .sm)
                            if let reason = day.lockReason, !reason.isEmpty {
                                Text(reason)
                            }
                        }
                        if let note = day.note, !note.isEmpty {
                            Text(note)
                        }
                    }
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .multilineTextAlignment(.center)
                }
            }
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.dayHeadingLabel(day, labels: labels, locale: locale, timeZone: timeZone))
            .accessibilityAddTraits(model.onDayActivate == nil ? .isHeader : [.isHeader, .isButton])
            .accessibilityAction {
                model.onDayActivate?(day.date)
            }
            .accessibilityAdjustableAction { direction in
                switch direction {
                case .increment where index + 1 < model.days.count: selectedDay = model.days[index + 1].date
                case .decrement where index > 0: selectedDay = model.days[index - 1].date
                default: break
                }
            }
            stepButton(
                symbol: Tokens.nextSymbol,
                label: Strings.nextDay(labels),
                target: index + 1 < model.days.count ? model.days[index + 1].date : nil
            )
        }
        .padding(.vertical, Tokens.pagePadding)
        .background(
            theme.colors[
                role: Tokens.cellAppearance(
                    isEditable: false,
                    isLocked: day.isLocked,
                    isNonWorking: day.isNonWorking,
                    hasError: false,
                    isSaving: false
                ).background
            ],
            in: MonarchShapes.container
        )
    }

    @ViewBuilder
    private func title(_ day: MonarchTimesheetGridDay) -> some View {
        let text = Text(Tokens.longDayText(day, labels: labels, locale: locale, timeZone: timeZone))
            .monarchTextStyle(.heading)
            .foregroundStyle(theme.colors[role: Tokens.headingForeground(isToday: day.isToday)])
            .multilineTextAlignment(.center)
        VStack(spacing: Tokens.lineSpacing) {
            if let onDayActivate = model.onDayActivate {
                Button {
                    onDayActivate(day.date)
                } label: {
                    text.monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            } else {
                text
            }
            if day.isToday {
                Rectangle()
                    .fill(theme.colors.accentSolid)
                    .frame(width: Tokens.todayBarWidth, height: Tokens.todayBarHeight)
            }
        }
    }

    private func stepButton(symbol: String, label: String, target: Date?) -> some View {
        Button {
            if let target { selectedDay = target }
        } label: {
            MonarchIcon(systemName: symbol, label: label)
                .foregroundStyle(theme.colors[role: target == nil ? \.textDisabled : \.accentStrong])
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .disabled(target == nil)
    }

    // MARK: Rows

    @ViewBuilder
    private func rowView(_ row: MonarchTimesheetGridRow, day: MonarchTimesheetGridDay) -> some View {
        let key = MonarchTimesheetGridCellKey(row: row.id, day: day.date)
        let minutes = row.minutes(on: day.date)
        let helper = Tokens.pageRowHelper(row, days: model.days, labels: labels)
        let actions = model.actions(for: row)
        HStack(alignment: .top, spacing: Tokens.iconSpacing) {
            if model.isCellEditable(row, day) {
                MonarchDurationInput(
                    row.label,
                    minutes: Binding(
                        get: { minutes > 0 ? minutes : nil },
                        set: { model.commit(key, $0 ?? 0) }
                    ),
                    maximum: model.maximum,
                    helper: helper,
                    error: model.errors[key],
                    labels: MonarchDurationInputLabels(
                        invalid: Strings.invalid(labels),
                        range: { [labels] _, maximum in
                            Strings.tooLong(
                                Tokens.durationText(maximum ?? Tokens.defaultMaximum, labels: labels),
                                labels
                            )
                        }
                    ),
                    format: labels.duration
                )
                .disabled(model.saving.contains(key))
                .id(key)
            } else {
                readOnlyRow(row, day: day, minutes: minutes, helper: helper)
            }
            if !actions.isEmpty {
                MonarchRowActionsMenu(actions: actions, label: Strings.rowActions(row.label, labels))
            }
        }
    }

    @ViewBuilder
    private func readOnlyRow(
        _ row: MonarchTimesheetGridRow,
        day: MonarchTimesheetGridDay,
        minutes: Int,
        helper: String
    ) -> some View {
        let value = Tokens.cellValue(
            minutes,
            lockText: Tokens.lockText(day, labels: labels),
            labels: labels,
            locale: locale
        )
        let content = HStack(alignment: .firstTextBaseline, spacing: Tokens.iconSpacing) {
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                Text(row.label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                Text(helper)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if day.isLocked {
                MonarchIcon(systemName: Tokens.lockSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
            Text(Tokens.valueText(minutes, labels: labels))
                .monarchTextStyle(.body)
                .monarchTabularNumber()
        }
        .frame(minHeight: Tokens.minCellHeight)
        if let onCellActivate = model.onCellActivate, minutes > 0 {
            Button {
                onCellActivate(row.id, day.date)
            } label: {
                content
                    .foregroundStyle(theme.colors.textLink)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(row.label)
            .accessibilityValue(value)
            .accessibilityAddTraits(.isButton)
        } else {
            content
                .foregroundStyle(theme.colors[role: day.isLocked ? \.textMuted : \.textDefault])
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(row.label)
                .accessibilityValue(value)
        }
    }

    // MARK: Footer

    private func footer(_ day: MonarchTimesheetGridDay) -> some View {
        let total = Tokens.dayTotal(day, rows: model.rows)
        let isOver = Tokens.isOverLimit(total, maximum: model.maximum)
        let grandTotal = Tokens.grandTotal(rows: model.rows, days: model.days)
        return VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.iconSpacing) {
                Text(Strings.dayTotal(labels))
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if isOver {
                    MonarchIcon(systemName: Tokens.warningSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: Tokens.totalForeground(isOverLimit: true)])
                }
                VStack(alignment: .trailing, spacing: Tokens.lineSpacing) {
                    Text(Tokens.valueText(total, labels: labels))
                        .monarchTextStyle(.label)
                        .monarchTabularNumber()
                        .foregroundStyle(theme.colors[role: Tokens.totalForeground(isOverLimit: isOver)])
                    if let expected = Tokens.expectedText(day, labels: labels) {
                        Text(expected)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Strings.dayTotal(labels))
            .accessibilityValue(
                Tokens.dayTotalValue(day, total: total, maximum: model.maximum, labels: labels, locale: locale)
            )
            ForEach(model.summaryRows) { summary in
                line(summary.label, value: summary.cells[day.date], spokenValue: nil)
            }
            line(
                Strings.total(labels),
                value: Tokens.durationText(grandTotal, labels: labels),
                spokenValue: Tokens.spokenDuration(grandTotal, labels: labels, locale: locale)
            )
        }
    }

    private func line(_ label: String, value: String?, spokenValue: String?) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.iconSpacing) {
            Text(label)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(value ?? Tokens.emptyMark)
                .monarchTabularNumber()
        }
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(spokenValue ?? value ?? Strings.noTime(labels))
    }
}
