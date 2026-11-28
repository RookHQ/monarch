import MonarchUI
import SwiftUI

struct MonarchInlineErrorRetrySection: CataloguePage {
    static let title = "InlineErrorRetry"

    @State private var isRetrying = false
    @State private var attempts = 0

    var body: some View {
        CatalogueCaption("Inline, inside a card")
        MonarchCard {
            Text("Earnings").monarchTextStyle(.subheading)
            MonarchInlineErrorRetry("Your earnings didn't load. Tried \(attempts) times.", isRetrying: isRetrying) {
                retry()
            }
        }
        CatalogueCaption("Banner, above stale data")
        MonarchInlineErrorRetry(
            "Showing data from 10 minutes ago.",
            tone: .warning,
            variant: .banner,
            isRetrying: isRetrying
        ) {
            retry()
        }
    }

    private func retry() {
        isRetrying = true
        attempts += 1
        Task {
            try? await Task.sleep(for: .seconds(1))
            isRetrying = false
        }
    }
}
