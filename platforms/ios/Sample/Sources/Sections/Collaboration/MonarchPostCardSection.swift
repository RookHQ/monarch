import MonarchUI
import SwiftUI

struct MonarchPostCardSection: CataloguePage {
    static let title = "Post card"

    @State private var opened = ""
    @State private var isSaved = false

    var body: some View {
        MonarchPostCard(
            "How do you run design critiques remotely?",
            author: "Ana Silva",
            excerpt: """
                We moved our weekly critique online and attendance dropped by half. People join late, cameras \
                stay off and the same three voices fill the hour. What has worked for your team?
                """,
            publishedAt: Date().addingTimeInterval(-3 * 3_600),
            tags: [MonarchPostCardTag("Design"), MonarchPostCardTag("In review", tone: .warning)],
            isPinned: true,
            isAnswered: true,
            reactionCount: 12,
            commentCount: 4
        ) {
            opened = "Opened the critique post"
        } actions: {
            Toggle("Save", systemImage: isSaved ? "bookmark.fill" : "bookmark", isOn: $isSaved)
                .toggleStyle(.monarchButton(.ghost, size: .sm))
                .labelStyle(.iconOnly)
        }
        if !opened.isEmpty {
            Text(opened).monarchTextStyle(.caption)
        }
        CatalogueCaption("Title only")
        MonarchPostCard("Welcome to the community", author: "Ben Okafor", publishedAt: Date()) {
            opened = "Opened the welcome post"
        }
    }
}
