import MonarchUI
import SwiftUI

/// Native adoption: a `List` with `onMove(perform:)` in edit mode, plus
/// accessibility actions that announce every step.
struct ReorderableListSection: CataloguePage {
    static let title = "Reorderable list (List with onMove)"

    @State private var stages = ["New", "Contacted", "Proposal sent", "Negotiation"]
    @State private var editMode: EditMode = .active

    var body: some View {
        CatalogueCaption("Drag the reorder control on each row")
        EditButton()
        List {
            ForEach(stages, id: \.self) { stage in
                HStack {
                    Text(stage)
                    Spacer()
                    if stage == "New" {
                        MonarchBadge("12")
                    }
                }
                .accessibilityElement(children: .combine)
                .accessibilityActions {
                    Button("Move up") { move(stage, by: -1) }
                    Button("Move down") { move(stage, by: 1) }
                }
            }
            .onMove { stages.move(fromOffsets: $0, toOffset: $1) }
        }
        .environment(\.editMode, $editMode)
        .frame(minHeight: 260)
        Text("Order: \(stages.joined(separator: ", "))").monarchTextStyle(.caption)
    }

    private func move(_ stage: String, by step: Int) {
        guard let index = stages.firstIndex(of: stage), stages.indices.contains(index + step) else { return }
        stages.swapAt(index, index + step)
        AccessibilityNotification.Announcement("\(stage) moved to position \(index + step + 1) of \(stages.count)")
            .post()
    }
}
