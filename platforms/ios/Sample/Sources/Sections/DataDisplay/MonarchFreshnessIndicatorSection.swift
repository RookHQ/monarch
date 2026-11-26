import MonarchUI
import SwiftUI

struct MonarchFreshnessIndicatorSection: CataloguePage {
    static let title = "Freshness indicator"

    @State private var lastSync = Date.now.addingTimeInterval(-2 * 3600)

    var body: some View {
        CatalogueCaption("Stale, with a refresh that takes a second")
        MonarchFreshnessIndicator(
            updatedAt: lastSync,
            source: "Directory",
            staleAfter: .seconds(3600),
            expiredAfter: .seconds(86_400)
        ) {
            try await Task.sleep(for: .seconds(1))
            lastSync = .now
        }
        CatalogueCaption("Banner")
        MonarchFreshnessIndicator(
            updatedAt: .now.addingTimeInterval(-3 * 86_400),
            source: "Registry",
            details: ["Events on"],
            staleAfter: .seconds(3600),
            expiredAfter: .seconds(86_400),
            variant: .banner
        )
        CatalogueCaption("Never, checking and error")
        MonarchFreshnessIndicator(updatedAt: nil)
        MonarchFreshnessIndicator(updatedAt: nil, status: .checking)
        MonarchFreshnessIndicator(
            updatedAt: .now.addingTimeInterval(-600),
            error: "Sync failed",
            status: .error
        ) {}
    }
}
