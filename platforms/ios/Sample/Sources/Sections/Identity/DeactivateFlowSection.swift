import MonarchUI
import SwiftUI

/// Recipe: confirm a reversible pause in a sheet, and confirm reactivation.
struct DeactivateFlowSection: CataloguePage {
    static let title = "Deactivate flow (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isPresented = false
    @State private var isDeactivated = false
    @State private var reason = ""
    @State private var isWorking = false
    @State private var failure: String?

    private func confirm() {
        isWorking = true
        failure = nil
        Task {
            // The app's request, with reason. A thrown error goes in failure and the sheet stays open.
            try? await Task.sleep(for: .milliseconds(300))
            isWorking = false
            isDeactivated.toggle()
            isPresented = false
        }
    }

    var body: some View {
        CatalogueCaption(isDeactivated ? "Deactivated" : "Active")
        MonarchButton(isDeactivated ? "Reactivate" : "Deactivate", variant: .secondary) { isPresented = true }
            .sheet(isPresented: $isPresented) {
                ScrollView {
                    VStack(alignment: .leading, spacing: theme.spacing.space4) {
                        Text(
                            isDeactivated
                                ? "Reactivate the Northwind partnership?" : "Deactivate the Northwind partnership?"
                        )
                        .monarchTextStyle(.title)
                        .accessibilityAddTraits(.isHeader)
                        Text(
                            isDeactivated
                                ? "It starts working again straight away."
                                : "It stops working until you turn it back on. Nothing is deleted."
                        )
                        .monarchTextStyle(.body)
                        if !isDeactivated {
                            Label(
                                "You can reactivate it at any time from Settings › Partners.",
                                systemImage: "arrow.uturn.backward"
                            )
                            .monarchTextStyle(.body)
                        }
                        MonarchConsequenceList(
                            isDeactivated
                                ? [MonarchConsequence(.changed, "Shared reports update again")]
                                : [
                                    MonarchConsequence(.changed, "Shared reports stop updating"),
                                    MonarchConsequence(.changed, "Partner users can’t sign in"),
                                    MonarchConsequence(.kept, "Contracts and history are kept"),
                                ],
                            heading: isDeactivated ? "Once it is reactivated" : "While it is deactivated"
                        )
                        MonarchTextField("Reason (optional)", text: $reason, helper: "Kept with the change history.")
                        if let failure {
                            MonarchInlineNotice(failure, tone: .danger)
                        }
                        MonarchButton(
                            isDeactivated ? "Reactivate" : "Deactivate",
                            variant: .primary,
                            isLoading: isWorking,
                            fullWidth: true,
                            action: confirm
                        )
                        MonarchButton("Cancel", variant: .ghost, fullWidth: true) { isPresented = false }
                    }
                    .padding(theme.spacing.space4)
                }
                .presentationDetents([.medium, .large])
                .interactiveDismissDisabled(isWorking)
            }
    }
}
