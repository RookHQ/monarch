import MonarchUI
import SwiftUI

struct MonarchAiMessageBranchSection: CataloguePage {
    static let title = "AI message branch"

    @State private var version = 1
    @State private var single = 0

    private let versions = [
        "The report is due on Friday.",
        "Friday is the deadline for the report.",
        "You need to hand in the report by Friday.",
    ]

    var body: some View {
        CatalogueCaption("Three versions of a reply")
        Text(versions[min(max(version, 0), versions.count - 1)])
            .monarchTextStyle(.body)
        MonarchAiMessageBranch(selection: $version, count: versions.count)

        CatalogueCaption("Disabled, as while a reply streams")
        MonarchAiMessageBranch(selection: $version, count: versions.count)
            .disabled(true)

        CatalogueCaption("One version, shown on request")
        MonarchAiMessageBranch(selection: $single, count: 1, showsSingle: true)
    }
}
