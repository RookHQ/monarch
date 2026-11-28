import MonarchUI
import SwiftUI

struct MonarchDiffViewerSection: CataloguePage {
    static let title = "Diff viewer"

    @State private var mode = MonarchDiffViewerMode.unified
    @State private var restored = false

    private let before = """
        Release notes
        Ship on Friday.
        Tell support.
        Check the dashboards.
        Write the summary.
        Thank the team.
        Keep the old banner.
        """
    private let after = """
        Release notes
        Ship on Monday.
        Tell support.
        Check the dashboards.
        Write the summary.
        Thank the team.
        Update the status page.
        Post in the channel.
        """

    var body: some View {
        CatalogueCaption("Picks the layout by width")
        MonarchDiffViewer(before: before, after: after, context: 1, beforeLabel: "Version 3", afterLabel: "Version 4") {
            MonarchButton(restored ? "Restored" : "Restore", size: .sm) { restored = true }
        }
        CatalogueCaption("Layout chosen by the app")
        Picker("Layout", selection: $mode) {
            Text("Unified").tag(MonarchDiffViewerMode.unified)
            Text("Side by side").tag(MonarchDiffViewerMode.split)
        }
        .pickerStyle(.segmented)
        MonarchDiffViewer(before: before, after: after, mode: $mode, showsLineNumbers: false)
        CatalogueCaption("No changes")
        MonarchDiffViewer(before: "Same text", after: "Same text")
    }
}
