import MonarchUI
import SwiftUI

struct MonarchSlaIndicatorSection: CataloguePage {
    static let title = "SLA indicator"

    @State private var dueAt = Date.now.addingTimeInterval(150)
    @State private var lastChange = "No change yet"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Live: at risk 90 seconds before it is due, then breached")
        MonarchSlaIndicator(
            .onTrack,
            dueAt: dueAt,
            warnBefore: 90,
            target: "First response",
            announcesChanges: true,
            onStateChange: { state in
                lastChange = "Changed to \(String(describing: state))"
            }
        )
        Text(lastChange)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
        Button("Restart") {
            dueAt = Date.now.addingTimeInterval(150)
            lastChange = "No change yet"
        }
        .buttonStyle(.monarch(.secondary, size: .sm))

        ForEach(MonarchSlaIndicatorVariant.allCases, id: \.self) { variant in
            CatalogueCaption("Every state, \(String(describing: variant))")
            MonarchFlowLayout(spacing: theme.spacing.space2) {
                MonarchSlaIndicator(.onTrack, dueAt: Date.now.addingTimeInterval(8100), variant: variant)
                MonarchSlaIndicator(.atRisk, dueAt: Date.now.addingTimeInterval(1500), variant: variant)
                MonarchSlaIndicator(.breached, dueAt: Date.now.addingTimeInterval(-2400), variant: variant)
                MonarchSlaIndicator(.paused, variant: variant)
                MonarchSlaIndicator(.met, variant: variant)
                MonarchSlaIndicator(.notApplicable, variant: variant)
                MonarchSlaIndicator(.notApplicable, variant: variant, showsNotApplicable: true)
            }
        }

        CatalogueCaption("The targets of one item")
        LabeledContent("First response") {
            MonarchSlaIndicator(.met, variant: .text)
        }
        .monarchTextStyle(.caption)
        LabeledContent("Next response") {
            MonarchSlaIndicator(.onTrack, dueAt: Date.now.addingTimeInterval(5400), warnBefore: 3600, variant: .text)
        }
        .monarchTextStyle(.caption)
        LabeledContent("Resolution") {
            MonarchSlaIndicator(.paused, variant: .text, hasEarlierBreach: true)
        }
        .monarchTextStyle(.caption)
    }
}
