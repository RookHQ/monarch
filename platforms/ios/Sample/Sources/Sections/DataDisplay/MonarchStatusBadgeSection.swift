import MonarchUI
import SwiftUI

struct MonarchStatusBadgeSection: CataloguePage {
    static let title = "Status badge"

    @Environment(\.monarchTheme) private var theme
    @State private var variant = MonarchStatusBadgeVariant.subtle

    private let health = ["operational", "degraded", "partial", "down", "maintenance", "unknown"]
    private let ticketStatuses = MonarchStatusRegistry([
        "open": MonarchStatusDefinition("Open", tone: .info, systemImage: "circle"),
        "waiting": MonarchStatusDefinition("Waiting on customer", tone: .warning, systemImage: "clock"),
        "solved": MonarchStatusDefinition("Solved", tone: .success, systemImage: "checkmark"),
    ])

    var body: some View {
        Picker("Variant", selection: $variant) {
            ForEach(MonarchStatusBadgeVariant.allCases, id: \.self) { variant in
                Text(String(describing: variant)).tag(variant)
            }
        }
        .pickerStyle(.segmented)
        CatalogueCaption("Service health")
        MonarchFlowLayout(spacing: theme.spacing.space2) {
            ForEach(health, id: \.self) { status in
                MonarchStatusBadge(status, variant: variant)
            }
        }
        CatalogueCaption("The app's statuses, and an unknown key")
        MonarchFlowLayout(spacing: theme.spacing.space2) {
            MonarchStatusBadge("open", variant: variant, size: .sm)
            MonarchStatusBadge("waiting", variant: variant, size: .sm)
            MonarchStatusBadge("solved", variant: variant, size: .sm)
            MonarchStatusBadge("pending_delete", variant: variant, size: .sm)
        }
        .monarchStatusRegistry(ticketStatuses)
    }
}
