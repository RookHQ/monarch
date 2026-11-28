import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiArtifactVersionsSection: CataloguePage {
    static let title = "AI artifact versions"

    private let versions = [
        MonarchAiArtifactVersion(
            1,
            createdAt: .now.addingTimeInterval(-3_600),
            content: "We ship the new billing page on Friday.\nSupport gets a briefing on Thursday."
        ),
        MonarchAiArtifactVersion(
            2,
            createdAt: .now.addingTimeInterval(-1_800),
            label: "Shorter intro",
            content: "The billing page ships Friday.\nSupport gets a briefing on Thursday."
        ),
        MonarchAiArtifactVersion(
            3,
            createdAt: .now.addingTimeInterval(-300),
            label: "New date",
            content: "The billing page ships Monday.\nSupport gets a briefing on Friday.\nUpdate the status page."
        ),
    ]
    @State private var version = 3
    @State private var isComparing = false

    var body: some View {
        CatalogueCaption("Step, jump or compare")
        MonarchAiArtifactVersions(selection: $version, versions: versions, isComparing: $isComparing)
        if !isComparing, let content = versions.first(where: { $0.version == version })?.content {
            Text(content).monarchTextStyle(.body)
        }

        CatalogueCaption("Line diff, compare already on")
        MonarchAiArtifactVersions(
            selection: .constant(2),
            versions: versions,
            isComparing: .constant(true),
            granularity: .line
        )
    }
}
