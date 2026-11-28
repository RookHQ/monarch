import MonarchUI
import SwiftUI

/// Recipe: the header of a record screen, composed from Monarch components,
/// with the title and actions in the navigation bar.
struct RecordHeaderSection: CataloguePage {
    static let title = "Record header (recipe)"

    private let tickets = ["Printer offline on floor 3", "Invoice shows the wrong address", "Reset two-factor"]

    @State private var index = 0
    @State private var subject = "Printer offline on floor 3"
    @State private var section = "activity"
    @State private var isMerged = false
    @State private var lastAction = ""

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        NavigationStack {
            ScrollView {
                header.padding(theme.spacing.space4)
            }
            .navigationTitle(subject)
            .toolbar {
                ToolbarItemGroup(placement: .primaryAction) {
                    Button("Previous ticket", systemImage: "chevron.up") { move(-1) }
                        .disabled(index == 0)
                    Button("Next ticket", systemImage: "chevron.down") { move(1) }
                        .disabled(index == tickets.count - 1)
                    Menu("More actions for \(subject)", systemImage: "ellipsis.circle") {
                        Button("Assign to me", systemImage: "person.crop.circle.badge.checkmark") {
                            lastAction = "Assigned to you"
                        }
                        Button("Merge into another ticket", systemImage: "arrow.triangle.merge") { isMerged = true }
                        Divider()
                        Button("Delete ticket", systemImage: "trash", role: .destructive) { lastAction = "Deleted" }
                    }
                }
            }
        }
        .frame(minHeight: 520)
        if !lastAction.isEmpty {
            Text(lastAction).monarchTextStyle(.caption)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("\(index + 1) of \(tickets.count)")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            HStack(alignment: .top, spacing: theme.spacing.space3) {
                MonarchAvatar(image: nil, name: "Ada Lovelace", size: .lg, isDecorative: true)
                VStack(alignment: .leading, spacing: theme.spacing.space2) {
                    MonarchInlineEditText("Subject", text: $subject) { newSubject in
                        lastAction = "Saved \"\(newSubject)\""
                    }
                    .accessibilityAddTraits(.isHeader)
                    MonarchFlowLayout(spacing: theme.spacing.space2) {
                        MonarchStatusBadge(MonarchStatusDefinition("Open", tone: .info))
                        MonarchBadge("High priority", tone: .warning)
                    }
                    Text("#1042 · Opened 3 days ago by Ada Lovelace")
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            MonarchKeyValue([
                MonarchKeyValueItem("Requester", value: "Ada Lovelace"),
                MonarchKeyValueItem("Email", value: "ada@example.com", isCopyable: true),
                MonarchKeyValueItem("Assignee", value: nil),
            ])
            if isMerged {
                MonarchCalloutBanner(
                    message: "This ticket was merged into #1051. It is read-only.",
                    tone: .warning,
                    actionTitle: "Undo",
                    action: { isMerged = false }
                )
            }
            MonarchTabs(
                "Ticket sections",
                selection: $section,
                items: [
                    MonarchTabsItem("Activity", value: "activity"),
                    MonarchTabsItem("Details", value: "details"),
                    MonarchTabsItem("Related", value: "related", count: "3"),
                ]
            )
        }
    }

    private func move(_ step: Int) {
        index = min(max(0, index + step), tickets.count - 1)
        subject = tickets[index]
    }
}
