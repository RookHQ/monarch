import MonarchUI
import SwiftUI

struct MonarchListRowSection: CataloguePage {
    static let title = "List row"

    var body: some View {
        VStack(spacing: 0) {
            MonarchListRow("Ana Lima", subtitle: "Owner", systemImage: "person", showsChevron: true)
            MonarchListRow("Notifications", systemImage: "bell") {
                MonarchBadge("3", tone: .info)
            }
            MonarchListRow("Billing", subtitle: "Plan and invoices", showsDivider: false)
        }

        CatalogueCaption("Disabled")
        MonarchListRow("Archived project", subtitle: "Read only", showsChevron: true, showsDivider: false)
            .disabled(true)
    }
}
