import MonarchUI
import SwiftUI

struct MonarchToggleBlockSection: CataloguePage {
    static let title = "Toggle block"

    @State private var isExpanded = true

    var body: some View {
        Text("Import runbook").monarchTextStyle(.heading)
        Text("The importer copies records from the registry every night.").monarchTextStyle(.body)
        MonarchToggleBlock("Why some records are skipped", isExpanded: $isExpanded) {
            Text("Records changed while the import runs are picked up the next night.")
                .monarchTextStyle(.body)
        }
        MonarchToggleBlock("Who to contact") {
            Text("Ask the team that owns the importer.").monarchTextStyle(.body)
        }
        MonarchToggleBlock("Archived notes") { Text("Nothing here.") }
            .disabled(true)
    }
}
