import MonarchUI
import SwiftUI

struct MonarchDragHandleSection: CataloguePage {
    static let title = "Drag handle"

    @Environment(\.monarchTheme) private var theme
    @State private var items = ["Design review", "Quarterly planning", "Hiring sync", "Retrospective"]
    @State private var dropped: String?

    var body: some View {
        CatalogueCaption("Reorder with the VoiceOver actions Move up and Move down")
        ForEach(items, id: \.self) { item in
            HStack(spacing: theme.spacing.space2) {
                MonarchDragHandle("Reorder \(item)", state: dropped == item ? .dropped : .idle) { direction in
                    move(item, direction)
                }
                Text(item).monarchTextStyle(.body)
            }
        }

        CatalogueCaption("States: idle, grabbed, dropped, disabled")
        HStack(spacing: theme.spacing.space2) {
            ForEach(MonarchDragHandleState.allCases, id: \.self) { state in
                MonarchDragHandle("Reorder", state: state)
            }
            MonarchDragHandle("Reorder").disabled(true)
        }

        CatalogueCaption("Axes: vertical, horizontal, both. Small size.")
        HStack(spacing: theme.spacing.space2) {
            ForEach(MonarchDragHandleAxis.allCases, id: \.self) { axis in
                MonarchDragHandle("Reorder", axis: axis, size: .sm)
            }
        }
    }

    private func move(_ item: String, _ direction: MonarchDragHandleDirection) {
        guard let index = items.firstIndex(of: item) else { return }
        let target = direction == .up ? index - 1 : index + 1
        guard items.indices.contains(target) else { return }
        items.swapAt(index, target)
        dropped = item
        AccessibilityNotification.Announcement("\(item), position \(target + 1) of \(items.count)").post()
    }
}
