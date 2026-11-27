import MonarchUI
import SwiftUI

struct MonarchConsequenceListSection: CataloguePage {
    static let title = "ConsequenceList"

    @State private var isGrouped = false
    @State private var isLoading = false

    private let items = [
        MonarchConsequence(.removed, "tasks will be deleted", detail: "Including 3 that are overdue", count: 1200),
        MonarchConsequence(.removed, "files will be deleted", count: 48),
        MonarchConsequence(.changed, "Members lose access immediately"),
        MonarchConsequence(.warning, "This cannot be undone"),
        MonarchConsequence(.kept, "Invoices stay in Billing"),
    ]

    var body: some View {
        Toggle("Grouped by kind", isOn: $isGrouped)
            .monarchTextStyle(.body)
        Toggle("Loading", isOn: $isLoading)
            .monarchTextStyle(.body)
        MonarchConsequenceList(items, heading: "What happens", isGrouped: isGrouped, isLoading: isLoading)
    }
}
