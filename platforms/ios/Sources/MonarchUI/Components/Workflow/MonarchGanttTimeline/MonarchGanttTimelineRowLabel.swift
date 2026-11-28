import SwiftUI

/// The label of a row in the label column: a group heading, or an item,
/// which shows and hides its children when it has some. The grid places a
/// hidden copy of it in each row, so both columns are as tall as the label.
struct MonarchGanttTimelineRowLabel: View {
    let model: MonarchGanttTimelineModel
    let line: MonarchGanttTimelineLine

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Group {
            switch line {
            case .group(let group):
                texts(group.label, meta: group.meta, caption: nil, extra: model.groupMeta?(group))
                    .padding(.horizontal, MonarchGanttTimelineTokens.labelPadding)
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isHeader)
            case .item(let item, let depth, let children, _):
                if children > 0 {
                    let isExpanded = !model.state.collapsed.contains(item.id)
                    Button {
                        model.update { $0.toggle(item.id) }
                    } label: {
                        HStack(alignment: .firstTextBaseline, spacing: MonarchGanttTimelineTokens.listGap) {
                            // A chevron says the row opens, as on the web; the count is the spoken value.
                            Image(systemName: isExpanded ? "chevron.down" : "chevron.right")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
                                .frame(width: 12)
                                .accessibilityHidden(true)
                            texts(
                                item.label,
                                meta: item.meta,
                                caption: nil,
                                extra: model.itemMeta?(item)
                            )
                        }
                        .padding(.leading, MonarchGanttTimelineTokens.indent(depth: depth))
                        .padding(.trailing, MonarchGanttTimelineTokens.labelPadding)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityValue(
                        "\(model.text.children(children, isExpanded: isExpanded)), "
                            + (isExpanded ? model.text.expanded : model.text.collapsed)
                    )
                } else {
                    texts(item.label, meta: item.meta, caption: nil, extra: model.itemMeta?(item))
                        .padding(.leading, MonarchGanttTimelineTokens.indent(depth: depth))
                        .padding(.trailing, MonarchGanttTimelineTokens.labelPadding)
                        .accessibilityElement(children: .combine)
                }
            }
        }
        .padding(.vertical, MonarchGanttTimelineTokens.labelVerticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchGanttTimelineTokens.rowMinHeight, alignment: .leading)
    }

    private func labelText(_ label: String) -> some View {
        Text(label)
            .monarchTextStyle(.bodySmall)
            .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.textColor])
    }

    private func texts(_ label: String, meta: String?, caption: String?, extra: AnyView?) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchGanttTimelineTokens.listGap) {
                    labelText(label)
                    if let meta { MonarchBadge(meta) }
                    if let extra { extra }
                }
                VStack(alignment: .leading, spacing: MonarchGanttTimelineTokens.listGap) {
                    labelText(label)
                    if let meta { MonarchBadge(meta) }
                    if let extra { extra }
                }
            }
            if let caption {
                Text(caption)
                    .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                    .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.linkColor])
            }
        }
        .multilineTextAlignment(.leading)
        .fixedSize(horizontal: false, vertical: true)
    }
}

/// Two caption lines: how tall the header is at the current text size. Hidden.
struct MonarchGanttTimelineHeaderProbe: View {
    var body: some View {
        VStack(spacing: 0) {
            Text(verbatim: "0")
            Text(verbatim: "0")
        }
        .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
        .padding(.vertical, MonarchGanttTimelineTokens.tickPadding)
        .frame(minHeight: MonarchGanttTimelineTokens.headerMinHeight)
        .hidden()
    }
}

/// The label of the milestone lane.
struct MonarchGanttTimelineLaneLabel: View {
    let text: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text(text)
            .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
            .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, MonarchGanttTimelineTokens.labelPadding)
            .padding(.vertical, MonarchGanttTimelineTokens.labelVerticalPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchGanttTimelineTokens.rowMinHeight, alignment: .leading)
    }
}
