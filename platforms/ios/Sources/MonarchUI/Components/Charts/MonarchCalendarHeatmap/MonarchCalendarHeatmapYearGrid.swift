import MonarchTokens
import SwiftUI

/// The year layout: weekday names on the leading side, month names on top
/// and one column of seven days per week, scrolling sideways when wider
/// than the screen. It opens at the latest week.
struct MonarchCalendarHeatmapYearGrid: View {
    let model: MonarchCalendarHeatmapTokens.Model
    let calendar: Calendar
    let scale: MonarchHeatmapTokens.Scale
    let base: MonarchRGBA
    let today: Int?
    let active: Int?
    let onTap: (Int) -> Void

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var monthLabelWidth = MonarchCalendarHeatmapTokens.monthLabelWidth

    private typealias Tokens = MonarchCalendarHeatmapTokens
    /// The width of the whole grid and of the weekday names, measured, so the weeks can shrink to fit the width
    /// the way the web grid fits its card; below `yearMinimumCell` the grid scrolls sideways instead.
    @State private var totalWidth: CGFloat = 0
    @State private var weekdayWidth: CGFloat = 0

    private var fitted: CGFloat {
        guard totalWidth > 0, weekdayWidth > 0, model.weeks > 0 else { return Tokens.yearCell }
        let weeks = CGFloat(model.weeks)
        let room = totalWidth - weekdayWidth - Tokens.labelSpacing - Tokens.gap * (weeks - 1)
        return min(Tokens.yearCell, room / weeks)
    }
    private var cellSide: CGFloat { max(Tokens.yearMinimumCell, fitted) }
    private var overflows: Bool { fitted < Tokens.yearMinimumCell }
    private var step: CGFloat { cellSide + Tokens.gap }

    var body: some View {
        let weekdays = Tokens.weekdaySymbols(calendar)
        let months = Tokens.monthLabels(
            model,
            calendar: calendar,
            minimumColumns: Tokens.minimumLabelColumns(labelWidth: monthLabelWidth, step: step)
        )
        HStack(alignment: .top, spacing: Tokens.labelSpacing) {
            VStack(alignment: .trailing, spacing: Tokens.gap) {
                label(" ").hidden()
                ForEach(0..<Tokens.daysPerWeek, id: \.self) { row in
                    label(Tokens.showsWeekday(row: row) && weekdays.indices.contains(row) ? weekdays[row] : "")
                        .frame(height: cellSide)
                }
            }
            .fixedSize(horizontal: true, vertical: false)
            .onGeometryChange(for: CGFloat.self) {
                $0.size.width
            } action: {
                weekdayWidth = $0
            }
            ScrollView(.horizontal) {
                VStack(alignment: .leading, spacing: Tokens.gap) {
                    HStack(spacing: Tokens.gap) {
                        ForEach(0..<model.weeks, id: \.self) { column in
                            label(months[column] ?? " ")
                                .fixedSize()
                                .frame(width: cellSide, alignment: .leading)
                        }
                    }
                    HStack(spacing: Tokens.gap) {
                        ForEach(0..<model.weeks, id: \.self) { column in
                            VStack(spacing: Tokens.gap) {
                                ForEach(0..<Tokens.daysPerWeek, id: \.self) { row in
                                    cell(place: column * Tokens.daysPerWeek + row)
                                }
                            }
                        }
                    }
                }
            }
            .defaultScrollAnchor(.trailing)
            .scrollIndicators(.hidden)
            // A fade on the leading edge, so a week scrolled partly out of view does not show as a cut cell.
            .mask(
                LinearGradient(
                    stops: [
                        .init(color: overflows ? .clear : .black, location: 0),
                        .init(color: .black, location: Tokens.fadeShare),
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
        }
        .onGeometryChange(for: CGFloat.self) {
            $0.size.width
        } action: {
            totalWidth = $0
        }
    }

    private func label(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(Tokens.textStyle)
            .foregroundStyle(theme.colors.textMuted)
            .lineLimit(1)
            .minimumScaleFactor(Tokens.textMinimumScale)
    }

    @ViewBuilder private func cell(place: Int) -> some View {
        if let offset = model.offset(atPlace: place) {
            let day = model.days[offset]
            MonarchCalendarHeatmapCell(
                style: Tokens.cellStyle(
                    level: scale.level(day?.value),
                    isInactive: day?.isInactive ?? false,
                    isToday: offset == today,
                    isActive: offset == active
                ),
                base: base,
                radius: Tokens.yearRadius,
                markerSize: day?.hasMarker == true ? Tokens.yearMarker : nil
            ) {
                Color.clear.frame(width: cellSide, height: cellSide)
            }
            .contentShape(Rectangle())
            .onTapGesture { onTap(offset) }
        } else {
            Color.clear.frame(width: cellSide, height: cellSide)
        }
    }
}
