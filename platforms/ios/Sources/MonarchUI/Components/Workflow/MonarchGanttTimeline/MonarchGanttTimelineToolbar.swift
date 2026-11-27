import SwiftUI

/// The toolbar: a ``MonarchCalendarToolbar`` with Today, Earlier and Later
/// and the visible range as its title, then a zoom menu, the chart and list
/// switch (a segmented picker) and the app's actions.
struct MonarchGanttTimelineToolbar<Actions: View>: View {
    let model: MonarchGanttTimelineModel
    let zoomLevels: [MonarchGanttTimelineZoom]
    let actions: Actions

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let text = model.text
        // Zoom, then the earlier / Today / later buttons and the range on one row, the chart and list switch under
        // them, as on the web.
        VStack(alignment: .leading, spacing: MonarchGanttTimelineTokens.listGap) {
            MonarchFlowLayout(spacing: MonarchGanttTimelineTokens.listGap) {
                if MonarchGanttTimelineToolbar.showsZoomMenu(zoomLevels) {
                    MonarchSegmentedControl(
                        text.zoom,
                        selection: zoomBinding,
                        items: zoomLevels.map { MonarchSegmentedItem(text.zoomName($0), value: $0) },
                        size: .sm,
                        fillsWidth: false
                    )
                }
                HStack(spacing: MonarchGanttTimelineTokens.listGap) {
                    MonarchButton(
                        text.earlier,
                        systemImage: "chevron.left",
                        variant: .secondary,
                        size: .sm,
                        isIconOnly: true
                    ) {
                        model.setState(model.stepped(forward: false))
                    }
                    MonarchButton(text.today, variant: .secondary, size: .sm) {
                        if let next = model.todayState { model.setState(next) }
                    }
                    .disabled(model.todayState == nil)
                    MonarchButton(
                        text.later,
                        systemImage: "chevron.right",
                        variant: .secondary,
                        size: .sm,
                        isIconOnly: true
                    ) {
                        model.setState(model.stepped(forward: true))
                    }
                }
                Text(model.range)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            MonarchFlowLayout(spacing: MonarchGanttTimelineTokens.listGap) {
                MonarchSegmentedControl(
                    text.view,
                    selection: viewBinding,
                    items: MonarchGanttTimelineView.allCases.map { MonarchSegmentedItem(text.viewName($0), value: $0) },
                    size: .sm,
                    fillsWidth: false
                )
                actions
            }
        }
    }

    /// The zoom menu is shown only when there is more than one level to choose.
    static func showsZoomMenu(_ levels: [MonarchGanttTimelineZoom]) -> Bool {
        levels.count > 1
    }

    private var viewBinding: Binding<MonarchGanttTimelineView> {
        Binding(get: { model.state.view }, set: { view in model.update { $0.view = view } })
    }

    private var zoomBinding: Binding<MonarchGanttTimelineZoom> {
        Binding(get: { model.state.zoom }, set: { zoom in model.update { $0.zoom = zoom } })
    }

    private var zoomMenu: some View {
        let text = model.text
        return Menu {
            Picker(text.zoom, selection: zoomBinding) {
                ForEach(zoomLevels, id: \.self) { level in
                    Text(text.zoomName(level)).tag(level)
                }
            }
            .pickerStyle(.inline)
        } label: {
            HStack(spacing: MonarchGanttTimelineTokens.listGap) {
                Text(text.zoomName(model.zoom))
                MonarchIcon(systemName: "chevron.down", size: .sm)
            }
        }
        .buttonStyle(.monarch(.secondary, size: .sm))
        .fixedSize()
        .accessibilityLabel(text.zoom)
        .accessibilityValue(text.zoomName(model.zoom))
    }
}
