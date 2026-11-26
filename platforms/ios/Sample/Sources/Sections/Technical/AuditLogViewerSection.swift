import MonarchUI
import SwiftUI

/// Recipe: a searchable audit log with a detail sheet.
struct AuditLogViewerSection: CataloguePage {
    static let title = "Audit log viewer (recipe)"

    struct AuditEvent: Identifiable {
        let id: String
        let actor: String
        let actorKind: String
        let action: String
        let target: String
        let origin: String
        let time: Date
        let before: String?
        let after: String?
    }

    private static let now = Date(timeIntervalSince1970: 1_793_534_400)
    private static let events = [
        AuditEvent(
            id: "evt_1",
            actor: "Priya Raman",
            actorKind: "Person",
            action: "changed the role of",
            target: "Sam Lee",
            origin: "203.0.113.7 · Safari on iPhone",
            time: now.addingTimeInterval(-600),
            before: "Member",
            after: "Admin"
        ),
        AuditEvent(
            id: "evt_2",
            actor: "Nightly sync",
            actorKind: "Automation",
            action: "exported",
            target: "Customers",
            origin: "Rook Atlas",
            time: now.addingTimeInterval(-7_200),
            before: nil,
            after: nil
        ),
        AuditEvent(
            id: "evt_3",
            actor: "key_live_4f2",
            actorKind: "API key",
            action: "deleted",
            target: "Webhook 12",
            origin: "198.51.100.4",
            time: now.addingTimeInterval(-86_400),
            before: nil,
            after: nil
        ),
    ]

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""
    @State private var isPeopleOnly = false
    @State private var selected: AuditEvent?

    private var shown: [AuditEvent] {
        Self.events.filter { event in
            (!isPeopleOnly || event.actorKind == "Person")
                && (query.isEmpty
                    || "\(event.actor) \(event.action) \(event.target)".localizedCaseInsensitiveContains(query))
        }
    }

    private func relative(_ date: Date) -> String {
        RelativeDateTimeFormatter().localizedString(for: date, relativeTo: Self.now)
    }

    var body: some View {
        NavigationStack {
            MonarchCursorList(
                shown,
                label: "Audit events",
                hasMore: false,
                onLoadMore: {
                    // Fetch the next page with the cursor of the last event.
                },
                row: { event in
                    Button {
                        selected = event
                    } label: {
                        VStack(alignment: .leading, spacing: theme.spacing.space1) {
                            Text("\(event.actor) \(event.action) \(event.target)")
                                .monarchTextStyle(.body)
                                .foregroundStyle(theme.colors.textDefault)
                            Text("\(event.actorKind) · \(event.time.formatted(date: .abbreviated, time: .shortened))")
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors.textMuted)
                        }
                        .frame(
                            maxWidth: .infinity,
                            minHeight: MonarchAccessibility.minimumHitTarget,
                            alignment: .leading
                        )
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint("Shows the event details")
                }
            )
            .searchable(text: $query, prompt: "Search events")
            .safeAreaInset(edge: .top) {
                HStack {
                    MonarchTag("People only", isSelected: $isPeopleOnly)
                    Spacer()
                }
                .padding(.horizontal, theme.spacing.space4)
            }
            .navigationTitle("Audit log")
            .toolbar {
                ShareLink(
                    item: shown.map { "\($0.time),\($0.actor),\($0.action),\($0.target)" }.joined(separator: "\n")
                ) {
                    Label("Export", systemImage: "square.and.arrow.up")
                }
            }
            .sheet(item: $selected) { event in
                NavigationStack {
                    List {
                        Section {
                            LabeledContent("Actor") {
                                HStack(spacing: theme.spacing.space2) {
                                    Text(event.actor)
                                    MonarchBadge(event.actorKind)
                                }
                            }
                            LabeledContent("Action", value: event.action)
                            LabeledContent("Target", value: event.target)
                            LabeledContent("From", value: event.origin)
                            LabeledContent("When") {
                                Text(relative(event.time))
                                    .accessibilityLabel(event.time.formatted(date: .complete, time: .shortened))
                            }
                        }
                        if let before = event.before, let after = event.after {
                            Section("Changes") {
                                LabeledContent("Before", value: before)
                                LabeledContent("After", value: after)
                            }
                        }
                    }
                    .navigationTitle("Event details")
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Close") { selected = nil }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }
        }
        .frame(minHeight: theme.spacing.space24 * 6)
    }
}
