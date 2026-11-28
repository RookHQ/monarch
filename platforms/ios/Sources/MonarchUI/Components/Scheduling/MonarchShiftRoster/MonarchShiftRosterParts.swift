import SwiftUI

/// A person: a decorative ``MonarchAvatar``, the name and a muted line with the
/// description and, with totals, the hours. Read as one element, "Alex Kim,
/// Support, Hours: 40 h"; a button when the roster has `onSelectMember`.
struct MonarchShiftRosterPerson: View {
    let model: MonarchShiftRosterModel
    let member: MonarchShiftRosterMember

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        if let select = model.onSelectMember {
            Button {
                select(member)
            } label: {
                content
                    .monarchMinimumHitTarget()
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(model.personLabel(member))
        } else {
            content
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(model.personLabel(member))
        }
    }

    private var content: some View {
        HStack(spacing: MonarchShiftRosterTokens.personSpacing) {
            avatar
            VStack(alignment: .leading, spacing: 0) {
                Text(member.name)
                    .monarchTextStyle(MonarchShiftRosterTokens.nameStyle)
                    .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.nameColor])
                if let supporting = model.supporting(member) {
                    Text(supporting)
                        .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                        .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.mutedColor])
                }
            }
            .multilineTextAlignment(.leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private var avatar: some View {
        if let image = member.image {
            MonarchAvatar(image: image, name: member.name, seed: member.id, size: .sm, isDecorative: true)
        } else {
            MonarchAvatar(url: member.avatarURL, name: member.name, seed: member.id, size: .sm, isDecorative: true)
        }
    }
}

/// A day at the start of a row in the one-person layout: the date and a Today
/// badge. Read as one element, "Wednesday, November 4, Today".
struct MonarchShiftRosterDayLabel: View {
    let model: MonarchShiftRosterModel
    let day: Date

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let isToday = model.isToday(day)
        HStack(spacing: MonarchShiftRosterTokens.summarySpacing) {
            Text(model.formats.fullDay(day))
                .monarchTextStyle(MonarchShiftRosterTokens.nameStyle)
                .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.dayColor(isToday: isToday)])
            if isToday {
                MonarchBadge(model.text.today, tone: MonarchShiftRosterTokens.todayTone)
                    .fixedSize()
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(model.spokenDay(day))
    }
}

/// The line under the day tabs: the date as a heading, a Today badge and,
/// with totals, how many people work. A new count is announced, so VoiceOver
/// says it after an assignment.
struct MonarchShiftRosterDaySummary: View {
    let model: MonarchShiftRosterModel
    let day: Date

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let isToday = model.isToday(day)
        let count = model.working(day)
        HStack(alignment: .firstTextBaseline, spacing: MonarchShiftRosterTokens.summarySpacing) {
            Text(model.formats.fullDay(day))
                .monarchTextStyle(MonarchShiftRosterTokens.summaryStyle)
                .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.dayColor(isToday: isToday)])
                .accessibilityLabel(model.spokenDay(day))
                .accessibilityAddTraits(.isHeader)
            if isToday {
                MonarchBadge(model.text.today, tone: MonarchShiftRosterTokens.todayTone)
                    .fixedSize()
                    .accessibilityHidden(true)
            }
            Spacer(minLength: 0)
            if model.showsTotals {
                Text(model.workingValue(day))
                    .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                    .foregroundStyle(theme.colors[role: MonarchShiftRosterTokens.mutedColor])
            }
        }
        .onChange(of: count) { _, _ in
            if model.showsTotals {
                AccessibilityNotification.Announcement(model.workingValue(day)).post()
            }
        }
    }
}

/// A row of the one-day and one-person layouts: the lead (the person or the
/// day) and the cell side by side, or the cell under the lead at the
/// accessibility text sizes, so neither is squeezed.
struct MonarchShiftRosterRow<Lead: View, Cell: View>: View {
    @ViewBuilder let lead: Lead
    @ViewBuilder let cell: Cell

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        Group {
            if MonarchShiftRosterTokens.stacksRows(dynamicTypeSize) {
                VStack(alignment: .leading, spacing: MonarchShiftRosterTokens.rowPadding) {
                    lead
                    cell
                }
            } else {
                HStack(spacing: MonarchShiftRosterTokens.rowSpacing) {
                    lead.frame(maxWidth: .infinity, alignment: .leading)
                    cell.frame(maxWidth: .infinity)
                }
            }
        }
        .padding(.vertical, MonarchShiftRosterTokens.rowPadding)
    }
}
