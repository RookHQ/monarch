import MonarchUI
import SwiftUI

/// Recipe: a record detail page, composed from Monarch components. The side
/// column sits beside the main column when there is room and moves below it
/// on a phone.
struct RecordPageSection: CataloguePage {
    static let title = "Record page (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var tab = "overview"
    @State private var lastAction = ""

    private let now = Date(timeIntervalSince1970: 1_772_000_000)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    header
                    MonarchTabs(
                        "Record sections",
                        selection: $tab,
                        items: [
                            MonarchTabsItem("Overview", value: "overview"),
                            MonarchTabsItem("Activity", value: "activity", count: "3"),
                            MonarchTabsItem("Files", value: "files", isDisabled: true),
                        ]
                    )
                    ViewThatFits(in: .horizontal) {
                        HStack(alignment: .top, spacing: theme.spacing.space6) {
                            main.frame(minWidth: theme.spacing.space24 * 4)
                            aside.frame(width: theme.spacing.space24 * 3)
                        }
                        VStack(alignment: .leading, spacing: theme.spacing.space6) {
                            main
                            aside
                        }
                    }
                }
                .padding(theme.spacing.space4)
            }
            .navigationTitle("Acme renewal")
            .toolbar {
                ToolbarItemGroup(placement: .primaryAction) {
                    Button("Edit", systemImage: "pencil") { lastAction = "Edit" }
                    Menu("More actions", systemImage: "ellipsis.circle") {
                        Button("Duplicate", systemImage: "plus.square.on.square") { lastAction = "Duplicate" }
                        Button("Delete", systemImage: "trash", role: .destructive) { lastAction = "Delete" }
                    }
                }
            }
        }
        .frame(minHeight: theme.spacing.space24 * 7)
        if !lastAction.isEmpty {
            Text("Last action: \(lastAction)").monarchTextStyle(.caption)
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: theme.spacing.space3) {
            MonarchAvatar(image: nil, name: "Acme Inc.", shape: .square, size: .lg, isDecorative: true)
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                Text("Acme renewal")
                    .monarchTextStyle(.title)
                    .accessibilityAddTraits(.isHeader)
                MonarchFlowLayout(spacing: theme.spacing.space2) {
                    MonarchStatusBadge(MonarchStatusDefinition("Negotiation", tone: .info))
                    MonarchBadge("Enterprise", tone: .neutral)
                }
            }
        }
    }

    @ViewBuilder private var main: some View {
        switch tab {
        case "activity":
            MonarchActivityTimeline(
                [
                    MonarchActivityTimelineEvent(
                        id: "1",
                        actor: MonarchActivityTimelineActor("Ben Okafor"),
                        verb: "moved the deal to",
                        object: "Negotiation",
                        time: now.addingTimeInterval(-3_600)
                    ),
                    MonarchActivityTimelineEvent(
                        id: "2",
                        actor: MonarchActivityTimelineActor("Ada Lovelace"),
                        verb: "added a note",
                        time: now.addingTimeInterval(-86_400),
                        body: "They want a two-year term."
                    ),
                    MonarchActivityTimelineEvent(
                        id: "3",
                        actor: MonarchActivityTimelineActor("Ben Okafor"),
                        verb: "created the deal",
                        time: now.addingTimeInterval(-172_800)
                    ),
                ],
                now: now
            )
        default:
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                Text("Summary").monarchTextStyle(.subheading).accessibilityAddTraits(.isHeader)
                Text("Renewal of the enterprise plan for 250 seats, due at the end of the quarter.")
                    .monarchTextStyle(.body)
            }
        }
    }

    private var aside: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            Text("Record details").monarchTextStyle(.subheading).accessibilityAddTraits(.isHeader)
            LabeledContent("Owner", value: "Ben Okafor")
            LabeledContent("Value", value: "$48,000")
            LabeledContent("Close date", value: "30 June")
        }
        .labeledContentStyle(.monarch)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Record details")
    }
}
