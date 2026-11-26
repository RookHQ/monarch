import MonarchUI
import SwiftUI

/// Recipe: release notes as a What's new screen, a plain `List` of entries
/// made of MonarchBadge, MonarchProse and a Read more link.
struct ChangelogFeedSection: CataloguePage {
    static let title = "Changelog feed (What's new recipe)"

    private struct Release: Identifiable {
        let id: String
        let date: Date
        let version: String
        let title: String
        let tags: [(label: String, tone: MonarchTone)]
        let notes: String
        let url: URL?
    }

    private static let releases = [
        Release(
            id: "2.4",
            date: Date(timeIntervalSince1970: 1_762_992_000),
            version: "2.4",
            title: "Shared inboxes",
            tags: [("New", .success), ("Inbox", .neutral)],
            notes: "Answer customer mail as a team. **Assign** a thread, leave internal notes and see who is replying.",
            url: URL(string: "https://example.com/changelog/2-4")
        ),
        Release(
            id: "2.3.1",
            date: Date(timeIntervalSince1970: 1_761_782_400),
            version: "2.3.1",
            title: "Faster search",
            tags: [("Fix", .info)],
            notes: "Search results appear as you type, and filters keep their order.",
            url: nil
        ),
    ]

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var isOpen = false

    var body: some View {
        CatalogueCaption("A What's new screen, opened in a sheet")
        Button("What's new") { isOpen = true }
            .buttonStyle(.monarch(.secondary))
            .sheet(isPresented: $isOpen) {
                NavigationStack {
                    feed(Self.releases)
                        .navigationTitle("What's new")
                        .toolbar {
                            ToolbarItem(placement: .confirmationAction) {
                                Button("Done") { isOpen = false }
                            }
                        }
                }
            }
        CatalogueCaption("No releases yet")
        MonarchEmptyState("No updates yet.", kind: .firstRun, size: .sm)
    }

    private func feed(_ releases: [Release]) -> some View {
        List(releases) { release in
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                Text(dateLine(release))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                Text(release.title)
                    .monarchTextStyle(.subheading)
                    .accessibilityAddTraits(.isHeader)
                MonarchFlowLayout {
                    ForEach(release.tags, id: \.label) { tag in
                        MonarchBadge(tag.label, tone: tag.tone)
                    }
                }
                MonarchProse(MonarchProseStyle(size: .sm, limitsLineLength: false)) {
                    Text(MonarchProseStyle(size: .sm).markdown(release.notes, in: theme))
                }
                if let url = release.url {
                    Link("Read more", destination: url)
                        .buttonStyle(.monarchLink(.standalone, isExternal: true))
                }
            }
            .padding(.vertical, theme.spacing.space2)
        }
        .listStyle(.plain)
    }

    private func dateLine(_ release: Release) -> String {
        let utc = TimeZone(identifier: "UTC") ?? .current
        let date = release.date.formatted(Date.FormatStyle(date: .long, time: .omitted, locale: locale, timeZone: utc))
        return date + " \u{00B7} " + release.version
    }
}
