import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
private let body = """
    We moved our weekly critique online and attendance dropped by half. People join late, cameras stay off \
    and the same three voices fill the hour. What has worked for your team, and what did you stop doing?
    """

enum MonarchPostCardFixtures: ComponentFixtures {
    static let id = "post-card"
    static let fixtures = [
        Fixture("everything") {
            MonarchPostCard(
                "How do you run design critiques remotely?",
                author: "Ana Silva",
                excerpt: body,
                publishedAt: now.addingTimeInterval(-3 * 3_600),
                now: now,
                tags: [MonarchPostCardTag("Design"), MonarchPostCardTag("In review", tone: .warning)],
                isPinned: true,
                isAnswered: true,
                reactionCount: 12,
                commentCount: 4
            ) {
            } actions: {
                Button("Save", systemImage: "bookmark") {}.labelStyle(.iconOnly)
            }
        },
        Fixture("title only") {
            MonarchPostCard("A post with only a title", author: "Ben Okafor") {}
        },
        Fixture("every tone") {
            MonarchPostCard(
                "Release notes",
                author: "Chen Wei",
                tags: MonarchTone.allCases.map { MonarchPostCardTag("Tag", tone: $0) },
                commentCount: 0
            ) {}
        },
        Fixture("long title and author") {
            MonarchPostCard(
                "A very long title that asks a question about accessibility in data tables with many columns and rows",
                author: "Maximilian Alexander von Hohenberg-Lichtenstein",
                excerpt: body,
                excerptLength: 80,
                publishedAt: now.addingTimeInterval(-86_400 * 40),
                now: now,
                reactionCount: 1_204
            ) {}
        },
    ]
}

@Suite("MonarchPostCard")
@MainActor
struct MonarchPostCardTests {
    typealias Tokens = MonarchPostCardTokens

    @Test("Text that fits is returned trimmed and unchanged")
    func excerptFits() {
        #expect(Tokens.excerpt("  Short text. ", max: 220) == "Short text.")
        #expect(Tokens.excerpt("", max: 220).isEmpty)
    }

    @Test("Long text is cut at a word boundary with an ellipsis")
    func excerptCut() {
        let text = "one two three four five six seven eight nine ten"
        let cut = Tokens.excerpt(text, max: 20)
        #expect(cut == "one two three four…")
        #expect(cut.count <= 21)
    }

    @Test("A word too long for a boundary cut is cut at the limit")
    func excerptHardCut() {
        let text = "a " + String(repeating: "x", count: 40)
        #expect(Tokens.excerpt(text, max: 10) == "a xxxxxxxx…")
    }

    @Test("The time is relative to the injected now")
    func relativeTime() {
        let english = Locale(identifier: "en_US")
        #expect(Tokens.relativeTime(now.addingTimeInterval(-3 * 3_600), now: now, locale: english) == "3 hours ago")
        #expect(Tokens.relativeTime(now.addingTimeInterval(-86_400), now: now, locale: english) == "yesterday")
    }

    @Test("Counts are spoken with their noun, singular and plural")
    func counts() {
        let labels = MonarchPostCardLabels()
        #expect(Tokens.reactions(1, labels: labels) == "1 reaction")
        #expect(Tokens.reactions(12, labels: labels) == "12 reactions")
        #expect(Tokens.comments(1, labels: labels) == "1 comment")
        #expect(Tokens.comments(0, labels: labels) == "0 comments")
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let labels = MonarchPostCardLabels(reactions: { "\($0) Reaktionen" }, comments: { "\($0) Kommentare" })
        #expect(Tokens.reactions(2, labels: labels) == "2 Reaktionen")
        #expect(Tokens.comments(3, labels: labels) == "3 Kommentare")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchPostCardStrings.pinned == "Pinned")
        #expect(MonarchPostCardStrings.answered == "Answered")
    }

    @Test("A tag's id is its label and its tone defaults to neutral")
    func tag() {
        let tag = MonarchPostCardTag("Design")
        #expect(tag.id == "Design")
        #expect(tag.tone == .neutral)
    }

    @Test("The card grows with its content instead of clipping it")
    func grows() {
        let short = Host.layout(MonarchPostCard("Title", author: "Ana") {})
        let long = Host.layout(MonarchPostCard("Title", author: "Ana", excerpt: body, commentCount: 2) {})
        #expect(long.height > short.height)
    }
}
