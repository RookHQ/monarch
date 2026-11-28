import MonarchTokens
import SwiftUI

/// The month layout: a calendar of weeks as rows, with weekday names on top
/// and each day's number and value. Each day is its own VoiceOver element.
struct MonarchCalendarHeatmapMonthGrid: View {
    let model: MonarchCalendarHeatmapTokens.Model
    let calendar: Calendar
    let locale: Locale
    let scale: MonarchHeatmapTokens.Scale
    let base: MonarchRGBA
    let today: Int?
    let active: Int?
    let showsValues: Bool
    let isSelectable: Bool
    let format: (Double) -> String
    let dateText: (Int) -> String
    let spokenValue: (Int) -> String
    let onTap: (Int) -> Void

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var minimumCell = MonarchCalendarHeatmapTokens.monthMinimumCell

    private typealias Tokens = MonarchCalendarHeatmapTokens

    var body: some View {
        Grid(horizontalSpacing: Tokens.gap, verticalSpacing: Tokens.gap) {
            GridRow {
                ForEach(0..<Tokens.daysPerWeek, id: \.self) { column in
                    weekday(column)
                }
            }
            .accessibilityHidden(true)
            ForEach(0..<model.weeks, id: \.self) { row in
                GridRow {
                    ForEach(0..<Tokens.daysPerWeek, id: \.self) { column in
                        cell(place: row * Tokens.daysPerWeek + column)
                    }
                }
            }
        }
        .frame(maxWidth: CGFloat(Tokens.daysPerWeek) * (Tokens.monthMaximumCell + Tokens.gap), alignment: .leading)
    }

    private func weekday(_ column: Int) -> some View {
        let short = Tokens.weekdaySymbols(calendar)
        let narrow = Tokens.weekdaySymbols(calendar, veryShort: true)
        return ViewThatFits(in: .horizontal) {
            Text(short.indices.contains(column) ? short[column] : "")
            Text(narrow.indices.contains(column) ? narrow[column] : "")
        }
        .monarchTextStyle(Tokens.textStyle)
        .foregroundStyle(theme.colors.textMuted)
        .lineLimit(1)
        .frame(maxWidth: Tokens.monthMaximumCell)
    }

    @ViewBuilder private func cell(place: Int) -> some View {
        if let offset = model.offset(atPlace: place) {
            let day = model.day(offset, calendar: calendar)
            let level = scale.level(model.days[offset]?.value)
            let usesChip = MonarchHeatmapTokens.usesChip(level: level)
            MonarchCalendarHeatmapCell(
                style: Tokens.cellStyle(
                    level: level,
                    isInactive: day.isInactive,
                    isToday: offset == today,
                    isActive: offset == active
                ),
                base: base,
                radius: Tokens.monthRadius,
                markerSize: day.hasMarker ? Tokens.monthMarker : nil
            ) {
                VStack(alignment: .leading, spacing: 0) {
                    text(
                        Tokens.dayNumber(day.date, calendar: calendar, locale: locale),
                        role: Tokens.dayNumberRole,
                        chip: usesChip
                    )
                    .fontWeight(.semibold)
                    Spacer(minLength: 0)
                    if showsValues, model.days[offset] != nil, let value = Tokens.monthCellText(day, format: format) {
                        text(value, role: Tokens.valueRole, chip: usesChip)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(Tokens.monthPadding)
                .frame(maxWidth: Tokens.monthMaximumCell, minHeight: minimumCell, alignment: .topLeading)
            }
            .contentShape(Rectangle())
            .onTapGesture { onTap(offset) }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(dateText(offset))
            .accessibilityValue(spokenValue(offset))
            .accessibilityAddTraits(isSelectable ? .isButton : [])
            .accessibilityAddTraits(offset == active ? .isSelected : [])
            .accessibilityAction { onTap(offset) }
        } else {
            Color.clear
                .frame(maxWidth: Tokens.monthMaximumCell, minHeight: minimumCell)
                .accessibilityHidden(true)
        }
    }

    private func text(_ string: String, role: MonarchColorRole, chip: Bool) -> some View {
        Text(string)
            .monarchTextStyle(Tokens.textStyle)
            .monospacedDigit()
            .lineLimit(1)
            .minimumScaleFactor(Tokens.textMinimumScale)
            .foregroundStyle(theme.colors[role: role])
            .padding(.horizontal, chip ? MonarchHeatmapTokens.chipPadding : 0)
            .background {
                if chip {
                    MonarchShapes.mark.fill(theme.colors[role: Tokens.chipRole])
                }
            }
    }
}
