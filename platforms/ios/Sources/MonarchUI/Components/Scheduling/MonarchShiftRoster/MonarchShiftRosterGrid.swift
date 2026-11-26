import SwiftUI

/// People down the side and days across, with the hours per person and the
/// number working per day. Columns have fixed widths that scale with Dynamic
/// Type, so the automatic layout can test whether the grid fits.
struct MonarchShiftRosterGrid: View {
    let model: MonarchShiftRosterModel

    @ScaledMetric(relativeTo: .caption) private var personWidth = MonarchShiftRosterTokens.personColumnWidth
    @ScaledMetric(relativeTo: .caption) private var dayWidth = MonarchShiftRosterTokens.dayColumnWidth
    @ScaledMetric(relativeTo: .caption) private var hoursWidth = MonarchShiftRosterTokens.hoursColumnWidth
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let width = MonarchShiftRosterTokens.gridWidth(
            person: personWidth,
            day: dayWidth,
            hours: hoursWidth,
            days: model.range.count,
            showsTotals: model.showsTotals
        )
        VStack(alignment: .leading, spacing: 0) {
            header
            MonarchDivider()
            ForEach(Array(model.members.enumerated()), id: \.element.id) { index, member in
                if index > 0 { MonarchDivider() }
                row(member)
            }
            if model.showsTotals {
                MonarchDivider()
                footer
            }
        }
        .frame(width: width, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(model.text.roster)
    }

    private var header: some View {
        HStack(alignment: .bottom, spacing: 0) {
            headerText(model.text.person, width: personWidth, alignment: .leading)
            ForEach(model.range, id: \.self) { day in
                let color = theme.colors[role: MonarchShiftRosterTokens.dayColor(isToday: model.isToday(day))]
                VStack(alignment: .leading, spacing: 0) {
                    if model.isToday(day) {
                        Text(model.text.today)
                            .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                    }
                    Text(model.formats.weekday(day))
                        .monarchTextStyle(MonarchShiftRosterTokens.headerStyle)
                    Text(model.formats.date(day))
                        .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                }
                .foregroundStyle(color)
                .padding(MonarchShiftRosterTokens.headerPadding)
                .frame(width: dayWidth, alignment: .leading)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(model.spokenDay(day))
                .accessibilityAddTraits(.isHeader)
            }
            if model.showsTotals {
                headerText(model.text.hours, width: hoursWidth, alignment: .trailing)
            }
        }
        .background(theme.colors[role: MonarchShiftRosterTokens.headerFill])
    }

    private func row(_ member: MonarchShiftRosterMember) -> some View {
        HStack(spacing: 0) {
            MonarchShiftRosterPerson(model: model, member: member)
                .padding(MonarchShiftRosterTokens.headerPadding)
                .frame(width: personWidth, alignment: .leading)
            ForEach(model.range, id: \.self) { day in
                MonarchShiftRosterCell(model: model, member: member, day: day, isCompact: true)
                    .padding(MonarchShiftRosterTokens.cellPadding)
                    .frame(width: dayWidth)
            }
            if model.showsTotals {
                Text(model.hours(member))
                    .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                    .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.mutedColor])
                    .padding(MonarchShiftRosterTokens.headerPadding)
                    .frame(width: hoursWidth, alignment: .trailing)
                    .accessibilityLabel(model.hoursLabel(member))
            }
        }
    }

    private var footer: some View {
        HStack(spacing: 0) {
            headerText(model.text.working, width: personWidth, alignment: .leading)
            ForEach(model.range, id: \.self) { day in
                Text(model.formats.count(model.working(day)))
                    .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                    .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.mutedColor])
                    .padding(MonarchShiftRosterTokens.headerPadding)
                    .frame(width: dayWidth, alignment: .leading)
                    .accessibilityLabel(model.workingLabel(day))
            }
            Color.clear.frame(width: hoursWidth, height: 0)
        }
        .background(theme.colors[role: MonarchShiftRosterTokens.headerFill])
    }

    private func headerText(_ text: String, width: CGFloat, alignment: Alignment) -> some View {
        Text(text)
            .monarchTextStyle(MonarchShiftRosterTokens.headerStyle)
            .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.mutedColor])
            .padding(MonarchShiftRosterTokens.headerPadding)
            .frame(width: width, alignment: alignment)
            .accessibilityAddTraits(.isHeader)
    }
}
