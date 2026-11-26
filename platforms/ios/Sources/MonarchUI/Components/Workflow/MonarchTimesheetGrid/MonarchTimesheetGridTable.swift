import SwiftUI

/// The regular-width layout of ``MonarchTimesheetGrid``: a SwiftUI `Grid` of
/// rows against days with row, day and period totals, scrolling sideways
/// when it does not fit.
struct MonarchTimesheetGridTable: View {
    let model: MonarchTimesheetGridModel

    @FocusState private var focus: MonarchTimesheetGridCellKey?
    @ScaledMetric private var dayWidth = MonarchTimesheetGridTokens.dayColumnWidth
    @ScaledMetric private var rowHeaderWidth = MonarchTimesheetGridTokens.rowHeaderWidth
    @ScaledMetric private var totalWidth = MonarchTimesheetGridTokens.totalColumnWidth
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    private typealias Tokens = MonarchTimesheetGridTokens
    private typealias Strings = MonarchTimesheetGridStrings

    private var labels: MonarchTimesheetGridLabels { model.labels }

    var body: some View {
        ScrollView(.horizontal) {
            Grid(alignment: .leading, horizontalSpacing: 0, verticalSpacing: 0) {
                headerRow
                separator
                if model.rows.isEmpty {
                    GridRow {
                        Text(model.emptyText)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textMuted)
                            .padding(Tokens.headerPadding)
                            .frame(maxWidth: .infinity, minHeight: Tokens.minCellHeight, alignment: .leading)
                            .gridCellColumns(model.days.count + 2)
                    }
                    separator
                } else {
                    ForEach(Array(model.rows.enumerated()), id: \.element.id) { index, row in
                        rowView(row, index: index)
                        separator
                    }
                }
                footerRow
                ForEach(model.summaryRows) { summary in
                    summaryRow(summary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Strings.caption(labels))
    }

    private var separator: some View {
        Rectangle()
            .fill(theme.colors.borderSubtle)
            .frame(height: MonarchShapes.borderWidth)
            .gridCellUnsizedAxes(.horizontal)
            .accessibilityHidden(true)
    }

    // MARK: Header

    private var headerRow: some View {
        GridRow {
            headingText(Strings.rowHeader(labels))
                .frame(width: rowHeaderWidth, alignment: .leading)
            ForEach(model.days) { day in
                dayHeading(day)
            }
            headingText(Strings.total(labels))
                .frame(minWidth: totalWidth, alignment: .trailing)
        }
    }

    private func headingText(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors.textMuted)
            .padding(Tokens.headerPadding)
            .frame(minHeight: Tokens.minCellHeight)
            .accessibilityAddTraits(.isHeader)
    }

    private func dayHeading(_ day: MonarchTimesheetGridDay) -> some View {
        VStack(spacing: Tokens.lineSpacing) {
            if let onDayActivate = model.onDayActivate {
                Button {
                    onDayActivate(day.date)
                } label: {
                    dayTitle(day).monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            } else {
                dayTitle(day)
            }
            if day.isLocked {
                HStack(spacing: Tokens.iconSpacing) {
                    MonarchIcon(systemName: Tokens.lockSymbol, size: .sm)
                    if let reason = day.lockReason, !reason.isEmpty {
                        Text(reason)
                    }
                }
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            }
            if let note = day.note, !note.isEmpty {
                Text(note)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(Tokens.headerPadding)
        .frame(minWidth: dayWidth, maxWidth: .infinity, minHeight: Tokens.minCellHeight)
        .background(
            theme.colors[
                role: Tokens.cellAppearance(
                    isEditable: false,
                    isLocked: day.isLocked,
                    isNonWorking: day.isNonWorking,
                    hasError: false,
                    isSaving: false
                ).background
            ]
        )
        .overlay(alignment: .bottom) {
            if day.isToday {
                Rectangle()
                    .fill(theme.colors.accentSolid)
                    .frame(height: Tokens.todayBarHeight)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.dayHeadingLabel(day, labels: labels, locale: locale, timeZone: timeZone))
        .accessibilityAddTraits(model.onDayActivate == nil ? .isHeader : [.isHeader, .isButton])
        .accessibilityAction {
            model.onDayActivate?(day.date)
        }
    }

    private func dayTitle(_ day: MonarchTimesheetGridDay) -> some View {
        Text(Tokens.shortDayText(day, labels: labels, locale: locale, timeZone: timeZone))
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors[role: Tokens.headingForeground(isToday: day.isToday)])
            .multilineTextAlignment(.center)
    }

    // MARK: Rows

    private func rowView(_ row: MonarchTimesheetGridRow, index: Int) -> some View {
        GridRow {
            rowHeading(row)
            ForEach(Array(model.days.enumerated()), id: \.element.id) { dayIndex, day in
                cell(row: row, rowIndex: index, day: day, dayIndex: dayIndex)
            }
            let total = Tokens.rowTotal(row, days: model.days)
            Text(Tokens.durationText(total, labels: labels))
                .monarchTextStyle(.label)
                .monarchTabularNumber()
                .foregroundStyle(theme.colors.textDefault)
                .padding(Tokens.headerPadding)
                .frame(minWidth: totalWidth, maxWidth: .infinity, minHeight: Tokens.minCellHeight, alignment: .trailing)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    Tokens.cellLabel(rowName: row.label, dayText: Strings.total(labels), labels: labels)
                )
                .accessibilityValue(Tokens.spokenDuration(total, labels: labels, locale: locale))
        }
    }

    private func rowHeading(_ row: MonarchTimesheetGridRow) -> some View {
        HStack(alignment: .top, spacing: Tokens.iconSpacing) {
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                Text(row.label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                if let description = row.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
            let actions = model.actions(for: row)
            if !actions.isEmpty {
                MonarchRowActionsMenu(actions: actions, label: Strings.rowActions(row.label, labels))
            }
        }
        .padding(Tokens.headerPadding)
        .frame(width: rowHeaderWidth, alignment: .leading)
        .frame(minHeight: Tokens.minCellHeight)
    }

    @ViewBuilder
    private func cell(
        row: MonarchTimesheetGridRow,
        rowIndex: Int,
        day: MonarchTimesheetGridDay,
        dayIndex: Int
    ) -> some View {
        let key = MonarchTimesheetGridCellKey(row: row.id, day: day.date)
        let minutes = row.minutes(on: day.date)
        let isEditable = model.isCellEditable(row, day)
        let dayText = Tokens.longDayText(day, labels: labels, locale: locale, timeZone: timeZone)
        let label = Tokens.cellLabel(rowName: row.label, dayText: dayText, labels: labels)
        let appearance = Tokens.cellAppearance(
            isEditable: isEditable,
            isLocked: day.isLocked,
            isNonWorking: day.isNonWorking,
            hasError: model.errors[key] != nil,
            isSaving: model.saving.contains(key)
        )
        Group {
            if isEditable {
                MonarchTimesheetGridCellField(
                    key: key,
                    minutes: minutes,
                    label: label,
                    error: model.errors[key],
                    isSaving: model.saving.contains(key),
                    isNonWorking: day.isNonWorking,
                    maximum: model.maximum,
                    labels: labels,
                    focus: $focus,
                    onCommit: { model.commit(key, $0) },
                    onProblem: { model.report(key, $0) },
                    onMove: { move(from: (rowIndex, dayIndex), rowStep: $0, dayStep: $1) }
                )
            } else {
                readOnlyCell(row: row, day: day, minutes: minutes, label: label, foreground: appearance.foreground)
            }
        }
        .padding(Tokens.cellPadding)
        .frame(minWidth: dayWidth, maxWidth: .infinity, minHeight: Tokens.minCellHeight)
        .background(theme.colors[role: appearance.background])
    }

    @ViewBuilder
    private func readOnlyCell(
        row: MonarchTimesheetGridRow,
        day: MonarchTimesheetGridDay,
        minutes: Int,
        label: String,
        foreground: MonarchColorRole
    ) -> some View {
        let value = Tokens.cellValue(
            minutes,
            lockText: Tokens.lockText(day, labels: labels),
            labels: labels,
            locale: locale
        )
        let text = Text(Tokens.valueText(minutes, labels: labels))
            .monarchTextStyle(.body)
            .monarchTabularNumber()
            .frame(maxWidth: .infinity, minHeight: Tokens.minCellHeight)
        if let onCellActivate = model.onCellActivate, minutes > 0 {
            Button {
                onCellActivate(row.id, day.date)
            } label: {
                text
                    .foregroundStyle(theme.colors.textLink)
                    .underline()
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .accessibilityValue(value)
        } else {
            text
                .foregroundStyle(theme.colors[role: foreground])
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(value)
        }
    }

    private func move(from origin: (row: Int, day: Int), rowStep: Int, dayStep: Int) {
        let next = Tokens.nextCell(
            row: origin.row,
            day: origin.day,
            rowStep: rowStep,
            dayStep: dayStep,
            rowCount: model.rows.count,
            dayCount: model.days.count
        ) { model.isCellEditable(model.rows[$0], model.days[$1]) }
        guard let next else { return }
        focus = MonarchTimesheetGridCellKey(row: model.rows[next.row].id, day: model.days[next.day].date)
    }

    // MARK: Footer

    private var footerRow: some View {
        GridRow {
            Text(Strings.dayTotal(labels))
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
                .padding(Tokens.headerPadding)
                .frame(minHeight: Tokens.minCellHeight)
                .accessibilityAddTraits(.isHeader)
            ForEach(model.days) { day in
                dayTotal(day)
            }
            let total = Tokens.grandTotal(rows: model.rows, days: model.days)
            Text(Tokens.durationText(total, labels: labels))
                .monarchTextStyle(.label)
                .monarchTabularNumber()
                .foregroundStyle(theme.colors.textDefault)
                .padding(Tokens.headerPadding)
                .frame(minWidth: totalWidth, maxWidth: .infinity, minHeight: Tokens.minCellHeight, alignment: .trailing)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Strings.total(labels))
                .accessibilityValue(Tokens.spokenDuration(total, labels: labels, locale: locale))
        }
    }

    private func dayTotal(_ day: MonarchTimesheetGridDay) -> some View {
        let total = Tokens.dayTotal(day, rows: model.rows)
        let isOver = Tokens.isOverLimit(total, maximum: model.maximum)
        let dayText = Tokens.longDayText(day, labels: labels, locale: locale, timeZone: timeZone)
        return VStack(spacing: Tokens.lineSpacing) {
            HStack(spacing: Tokens.iconSpacing) {
                if isOver {
                    MonarchIcon(systemName: Tokens.warningSymbol, size: .sm)
                }
                Text(Tokens.valueText(total, labels: labels))
                    .monarchTextStyle(.label)
                    .monarchTabularNumber()
            }
            .foregroundStyle(theme.colors[role: Tokens.totalForeground(isOverLimit: isOver)])
            if let expected = Tokens.expectedText(day, labels: labels) {
                Text(expected)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .padding(Tokens.headerPadding)
        .frame(minWidth: dayWidth, maxWidth: .infinity, minHeight: Tokens.minCellHeight)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.cellLabel(rowName: Strings.dayTotal(labels), dayText: dayText, labels: labels))
        .accessibilityValue(
            Tokens.dayTotalValue(day, total: total, maximum: model.maximum, labels: labels, locale: locale)
        )
    }

    private func summaryRow(_ summary: MonarchTimesheetGridSummaryRow) -> some View {
        GridRow {
            Text(summary.label)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .padding(Tokens.headerPadding)
                .accessibilityAddTraits(.isHeader)
            ForEach(model.days) { day in
                let value = summary.cells[day.date]
                Text(value ?? Tokens.emptyMark)
                    .monarchTextStyle(.caption)
                    .monarchTabularNumber()
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(Tokens.headerPadding)
                    .frame(minWidth: dayWidth, maxWidth: .infinity)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(
                        Tokens.cellLabel(
                            rowName: summary.label,
                            dayText: Tokens.longDayText(day, labels: labels, locale: locale, timeZone: timeZone),
                            labels: labels
                        )
                    )
                    .accessibilityValue(value ?? Strings.noTime(labels))
            }
            Text(summary.total ?? "")
                .monarchTextStyle(.caption)
                .monarchTabularNumber()
                .foregroundStyle(theme.colors.textMuted)
                .padding(Tokens.headerPadding)
                .frame(minWidth: totalWidth, maxWidth: .infinity, alignment: .trailing)
                .accessibilityHidden(summary.total == nil)
        }
    }
}
