@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiToolApprovalSection: CataloguePage {
    static let title = "AI tool approval"

    @Environment(\.monarchTheme) private var theme
    @State private var result = "Waiting for a decision"
    @State private var round = 0

    var body: some View {
        CatalogueCaption("Medium risk, with Always allow")
        MonarchAiToolApproval(
            MonarchAiToolCallPart(
                id: "email-\(round)",
                name: "send_email",
                input: ["to": ["ada@example.com", "sam@example.com"], "subject": "Q3 plan"],
                state: .pendingApproval
            ),
            summary: "Send email to 2 people",
            argumentLabels: ["to": "Recipients"],
            onAlwaysAllow: { result = "Always allow \($0)" },
            onDeny: { reason in result = "Denied" + (reason.map { ": \($0)" } ?? "") },
            onApprove: { input in result = input == nil ? "Approved" : "Approved with edits" }
        )
        Text(result)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        MonarchButton("Ask again", variant: .ghost, size: .sm) { round += 1 }

        CatalogueCaption("Destructive")
        MonarchAiToolApproval(
            MonarchAiToolCallPart(
                id: "delete",
                name: "delete_records",
                input: ["count": 12, "table": "invoices"],
                state: .pendingApproval,
                risk: .destructive
            ),
            summary: "Delete 12 records",
            onDeny: { _ in },
            onApprove: { _ in }
        )

        CatalogueCaption("Already decided")
        MonarchAiToolApproval(
            MonarchAiToolCallPart(id: "done", name: "send_email", input: ["to": "ada@example.com"], state: .denied),
            onDeny: { _ in },
            onApprove: { _ in }
        )
    }
}
