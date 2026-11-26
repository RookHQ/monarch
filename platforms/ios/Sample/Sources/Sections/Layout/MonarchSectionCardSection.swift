import MonarchUI
import SwiftUI

struct MonarchSectionCardSection: CataloguePage {
    static let title = "Section card"

    @State private var state = MonarchSectionCardState.ready
    @State private var isRetrying = false

    var body: some View {
        Picker("State", selection: $state) {
            ForEach(MonarchSectionCardState.allCases, id: \.self) { state in
                Text(String(describing: state)).tag(state)
            }
        }
        .pickerStyle(.segmented)

        MonarchSectionCard(
            "Invoices",
            description: "Sent in the last 30 days",
            meta: "12 invoices",
            systemImage: "doc.text",
            state: state,
            emptyMessage: "Invoices you send appear here.",
            errorMessage: "Check your connection and try again.",
            isRetrying: isRetrying,
            onRetry: retry,
            content: {
                Text("Invoice 1042 · Due in 3 days").monarchTextStyle(.body)
                Text("Invoice 1043 · Paid").monarchTextStyle(.body)
            },
            actions: {
                MonarchButton("New", size: .sm) {}
            },
            footer: {
                Button("View all invoices") {}.buttonStyle(.monarchLink(.standalone))
            }
        )

        CatalogueCaption("Collapsible, danger tone")
        MonarchSectionCard(
            "Danger zone",
            description: "These actions cannot be undone.",
            tone: .danger,
            isCollapsible: true
        ) {
            MonarchButton("Delete workspace", variant: .danger) {}
        }

        CatalogueCaption("Refreshing in place, flush list")
        MonarchSectionCard("Members", meta: "3 people", isBusy: true, isFlush: true) {
            List {
                Text("Ana Flores")
                Text("Ben Okafor")
                Text("Chen Wei")
            }
            .listStyle(.plain)
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 4)
        }
    }

    private func retry() {
        isRetrying = true
        Task {
            try? await Task.sleep(for: .seconds(1))
            isRetrying = false
            state = .ready
        }
    }
}
