import MonarchUI
import SwiftUI

struct MonarchLifecycleBadgeSection: CataloguePage {
    static let title = "Lifecycle badge"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Built-in stages")
        ForEach(MonarchLifecycle.allCases, id: \.self) { lifecycle in
            MonarchLifecycleBadge(lifecycle)
        }
        CatalogueCaption("Beside a name")
        HStack(spacing: theme.spacing.space2) {
            Text("Invoices API")
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            MonarchLifecycleBadge(.preview)
        }
        CatalogueCaption("A stage of your own")
        MonarchLifecycleBadge(stage: "early_access", tone: .info)
        MonarchLifecycleBadge(stage: "sunset", label: "Sunset on 1 March", tone: .danger)
        CatalogueCaption("Without the dot")
        MonarchLifecycleBadge(.stable, showsDot: false)
    }
}
