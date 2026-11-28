import MonarchUI
import SwiftUI

/// The web's primary example, each tone with its default symbol, and a banner without a title.
struct MonarchBannerSection: CataloguePage {
    static let title = "Banner"

    var body: some View {
        MonarchBanner("Sync scheduled", message: "Rook ID members refresh every 15 minutes.", tone: .info)
        MonarchBanner("Saved", tone: .success)
        MonarchBanner("Domain not verified", tone: .warning) {
            MonarchButton("Verify", variant: .secondary, size: .sm) {}
        }
        MonarchBanner("Payment failed", message: "Update the card to keep the workspace active.", tone: .danger)
        MonarchBanner(message: "Exports run overnight.", tone: .neutral)
    }
}
