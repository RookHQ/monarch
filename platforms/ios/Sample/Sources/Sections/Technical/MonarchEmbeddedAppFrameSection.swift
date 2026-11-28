import MonarchUI
import SwiftUI

struct MonarchEmbeddedAppFrameSection: CataloguePage {
    static let title = "Embedded app frame"

    @State private var assigned = true
    @State private var summary = false
    @State private var mentions = true
    @State private var state = MonarchEmbeddedAppFrameState.failed

    var body: some View {
        CatalogueCaption("Ready")
        MonarchEmbeddedAppFrame("Example Desk: Email notifications") {
            VStack(alignment: .leading) {
                Text("Email notifications").monarchTextStyle(.bodyStrong)
                Text("Choose which updates reach your inbox.").monarchTextStyle(.bodySmall)
                Toggle("New tickets assigned to me", isOn: $assigned).toggleStyle(.monarchCheckbox)
                Toggle("Daily summary", isOn: $summary).toggleStyle(.monarchCheckbox)
                Toggle("Mentions", isOn: $mentions).toggleStyle(.monarchCheckbox)
            }
        }

        CatalogueCaption("Failed, then loading after Try again")
        MonarchEmbeddedAppFrame(
            "Example Desk: Billing",
            state: state,
            openURL: URL(string: "https://example.com/billing"),
            labels: MonarchEmbeddedAppFrameLabels(open: "Open in Example Desk"),
            onRetry: { state = .loading },
            content: { EmptyView() }
        )
    }
}
