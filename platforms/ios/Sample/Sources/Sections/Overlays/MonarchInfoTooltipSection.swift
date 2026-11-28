import MonarchUI
import SwiftUI

struct MonarchInfoTooltipSection: CataloguePage {
    static let title = "Info tooltip"

    @State private var showsTerms = false

    var body: some View {
        CatalogueCaption("Tap the button to open the explanation, tap again or outside to close it")
        HStack {
            Text("Visibility score").monarchTextStyle(.label)
            MonarchInfoTooltip(
                "How often your listing appears in search, from 0 to 100. It is updated every night.",
                label: "About visibility score"
            )
        }
        HStack {
            Text("Two-step verification").monarchTextStyle(.label)
            MonarchInfoTooltip(
                "A code from your phone is asked for after your password.",
                label: "Help with two-step verification",
                icon: .help,
                arrowEdge: .bottom
            )
        }

        CatalogueCaption("Opened by the app")
        HStack {
            Text("Billing terms").monarchTextStyle(.label)
            MonarchInfoTooltip("Invoices are due 30 days after they are issued.", isPresented: $showsTerms)
        }
        MonarchButton(showsTerms ? "Close the explanation" : "Explain the billing terms") { showsTerms.toggle() }
    }
}
