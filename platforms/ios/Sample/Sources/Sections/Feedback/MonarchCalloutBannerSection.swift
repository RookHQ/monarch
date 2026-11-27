import MonarchUI
import SwiftUI

struct MonarchCalloutBannerSection: CataloguePage {
    static let title = "CalloutBanner"

    @State private var showsTrial = true
    @State private var showsScope = true
    @State private var upgrades = 0

    var body: some View {
        CatalogueCaption("Page placement, dismissible")
        if showsTrial {
            MonarchCalloutBanner(
                "Your trial ends in 3 days",
                message: "Add a payment method to keep your workspace. Pressed \(upgrades) times.",
                tone: .warning,
                placement: .page,
                actionTitle: "Add payment method",
                action: { upgrades += 1 },
                onDismiss: { showsTrial = false }
            )
        } else {
            MonarchButton("Show the trial banner", variant: .ghost, size: .sm) { showsTrial = true }
        }
        CatalogueCaption("Section placement, every tone")
        ForEach(MonarchTone.allCases, id: \.self) { tone in
            MonarchCalloutBanner(
                "Callout, \(String(describing: tone))",
                message: "A banner inside the content.",
                tone: tone
            )
        }
        CatalogueCaption("Scope")
        if showsScope {
            MonarchCalloutBanner(
                message: "Showing tickets in Support",
                variant: .scope,
                actionTitle: "Clear",
                action: { showsScope = false }
            )
        } else {
            MonarchButton("Narrow the view again", variant: .ghost, size: .sm) { showsScope = true }
        }
    }
}
