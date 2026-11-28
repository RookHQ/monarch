import MonarchUI
import SwiftUI

struct MonarchCookieConsentBannerSection: CataloguePage {
    static let title = "Cookie consent banner"

    @State private var choice: MonarchCookieConsentChoice?
    @State private var showsChoices = false

    var body: some View {
        CatalogueCaption("Opt-in")
        MonarchCookieConsentBanner(
            policyURL: URL(string: "https://example.com/cookies"),
            onCustomize: { showsChoices = true },
            onDecide: { choice = $0 }
        )
        Text(verbatim: choice.map { "Chose \(String(describing: $0))" } ?? "No choice yet")
            .monarchTextStyle(.caption)

        CatalogueCaption("Opt-out")
        MonarchCookieConsentBanner(regime: .optOut) { choice = $0 }

        CatalogueCaption("Notice")
        MonarchCookieConsentBanner(regime: .notice) { choice = $0 }
            .sheet(isPresented: $showsChoices) {
                ScrollView {
                    MonarchConsentPreferences(
                        categories: [
                            MonarchConsentPreferencesCategory(
                                id: "necessary",
                                label: "Strictly necessary",
                                description: "Keep the site working.",
                                isRequired: true
                            ),
                            MonarchConsentPreferencesCategory(
                                id: "analytics",
                                label: "Analytics",
                                description: "Measure use."
                            ),
                        ]
                    ) { _, _ in
                        showsChoices = false
                    }
                    .padding()
                }
            }
    }
}
