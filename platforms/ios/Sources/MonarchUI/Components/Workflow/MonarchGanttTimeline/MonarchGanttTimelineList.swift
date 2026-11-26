import SwiftUI

/// The list view: a row per group and item in chart order, then the
/// unscheduled items and the milestones. Every date, length, progress,
/// dependency and conflict is written; editable items have Change dates.
struct MonarchGanttTimelineList: View {
    let model: MonarchGanttTimelineModel

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(model.lines.lines.enumerated()), id: \.element.key) { index, line in
                if index > 0 { MonarchDivider() }
                switch line {
                case .group(let group):
                    groupRow(group)
                case .item(let item, let depth, let children, _):
                    MonarchGanttTimelineItemRow(model: model, item: item, depth: depth, children: children)
                }
            }
            if !model.lines.unscheduled.isEmpty {
                heading(model.text.unscheduled(model.lines.unscheduled.count))
                ForEach(Array(model.lines.unscheduled.enumerated()), id: \.element.id) { index, item in
                    if index > 0 { MonarchDivider() }
                    MonarchGanttTimelineItemRow(model: model, item: item, depth: 0, children: 0)
                }
            }
            if !model.milestones.isEmpty {
                heading(model.text.milestones)
                ForEach(Array(model.milestones.enumerated()), id: \.element.id) { index, milestone in
                    if index > 0 { MonarchDivider() }
                    milestoneRow(milestone)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(model.text.timeline)
    }

    private func heading(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.subheading)
            .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.textColor])
            .fixedSize(horizontal: false, vertical: true)
            .padding(.top, MonarchGanttTimelineTokens.spacing)
            .padding(.bottom, MonarchGanttTimelineTokens.listGap)
            .accessibilityAddTraits(.isHeader)
    }

    private func groupRow(_ group: MonarchGanttTimelineGroup) -> some View {
        let range = MonarchGanttTimelineRules.span(group.start, group.end, days: model.days)
            .map { model.text.range(model.date($0.start), model.date($0.end)) }
        let details = [group.meta, range].compactMap { $0 }
        return VStack(alignment: .leading, spacing: 0) {
            Text(group.label)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.textColor])
            if !details.isEmpty {
                Text(details.joined(separator: " · "))
                    .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                    .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
            }
            if let extra = model.groupMeta?(group) {
                extra
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(MonarchGanttTimelineTokens.listRowPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchGanttTimelineTokens.rowMinHeight, alignment: .leading)
        .background(theme.colors[role: MonarchGanttTimelineTokens.shadeColor])
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    private func milestoneRow(_ milestone: MonarchGanttTimelineMilestone) -> some View {
        let on = milestone.itemID.flatMap { model.itemsByID[$0]?.label }
        return HStack(spacing: MonarchGanttTimelineTokens.spacing) {
            MonarchGanttTimelineDiamond(isDone: milestone.isDone)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 0) {
                Text(model.describe(milestone))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.textColor])
                if let on {
                    Text(on)
                        .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                        .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.vertical, MonarchGanttTimelineTokens.listRowPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchGanttTimelineTokens.rowMinHeight, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

/// An item of the list: its name and written facts (a button that opens the
/// item with `onOpen`), its conflict, Change dates when editable, and Hide or
/// Show for a parent.
struct MonarchGanttTimelineItemRow: View {
    let model: MonarchGanttTimelineModel
    let item: MonarchGanttTimelineItem
    let depth: Int
    let children: Int

    @Environment(\.monarchTheme) private var theme
    @State private var isPicking = false

    var body: some View {
        let isEditable = model.isEditable(item)
        VStack(alignment: .leading, spacing: MonarchGanttTimelineTokens.listGap) {
            VStack(alignment: .leading, spacing: 0) {
                Text(item.label)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.textColor])
                ForEach(model.facts(item), id: \.self) { fact in
                    caption(fact, role: MonarchGanttTimelineTokens.mutedColor)
                }
                if let conflict = item.conflict {
                    caption(model.text.conflict(conflict), role: MonarchGanttTimelineTokens.conflictColor)
                }
                if let extra = model.itemMeta?(item) {
                    extra
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
            .accessibilityElement(children: .combine)
            .modifier(MonarchGanttTimelineOpenModifier(item: item, onOpen: model.onOpen))
            if isEditable || children > 0 {
                HStack(spacing: MonarchGanttTimelineTokens.listGap) {
                    if isEditable {
                        MonarchButton(model.text.changeDates, size: .sm) { isPicking = true }
                    }
                    if children > 0 {
                        let isExpanded = !model.state.collapsed.contains(item.id)
                        MonarchButton(model.text.children(children, isExpanded: isExpanded), variant: .ghost, size: .sm)
                        {
                            model.update { $0.toggle(item.id) }
                        }
                        .accessibilityValue(isExpanded ? model.text.expanded : model.text.collapsed)
                    }
                }
            }
        }
        .padding(.vertical, MonarchGanttTimelineTokens.listRowPadding)
        .padding(.leading, MonarchGanttTimelineTokens.listIndent * CGFloat(depth))
        .frame(maxWidth: .infinity, minHeight: MonarchGanttTimelineTokens.rowMinHeight, alignment: .leading)
        .sheet(isPresented: $isPicking) {
            MonarchGanttTimelineDatesSheet(model: model, item: item)
        }
    }

    private func caption(_ text: String, role: MonarchColorRole) -> some View {
        Text(text)
            .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
            .foregroundStyle(theme.colors[role: role])
    }
}
