import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private enum StreamingSamples {
    static let answer = """
        ## Plan
        Ship the **beta** on Monday. Then tell support.

        1. Freeze the branch
        2. Run `make release`

        ```swift
        let total = items.reduce(0, +)
        ```

        | Plan | Seats |
        | --- | --- |
        | Team | 10 |
        """
}

enum MonarchAiStreamingTextFixtures: ComponentFixtures {
    static let id = "ai-streaming-text"
    static let fixtures = [
        Fixture("streaming, open emphasis and a caret") {
            MonarchAiStreamingText("## Plan\nShip the **beta on Mon", isStreaming: true)
        },
        Fixture("streaming inside a list") {
            MonarchAiStreamingText("Steps:\n\n1. Freeze the branch\n2. Run the *release", isStreaming: true)
        },
        Fixture("streaming an unclosed code block") {
            MonarchAiStreamingText("Run this:\n\n```swift\nlet total = items", isStreaming: true)
        },
        Fixture("table header held back") {
            MonarchAiStreamingText("Seats by plan:\n\n| Plan | Seats |\n| --", isStreaming: true)
        },
        Fixture("complete, every block") {
            MonarchAiStreamingText(StreamingSamples.answer)
        },
        Fixture("no caret, no announcements, custom labels") {
            MonarchAiStreamingText(
                "A [link](https://exa",
                isStreaming: true,
                announces: false,
                showsCaret: false,
                labels: MonarchAiStreamingTextLabels(complete: "Done", codeBlock: "Code", table: "Grid")
            )
        },
        Fixture("empty") {
            MonarchAiStreamingText("", isStreaming: true)
        },
        Fixture("long text") {
            MonarchAiStreamingText(
                String(repeating: "A long sentence that wraps across several lines at large text sizes. ", count: 12),
                isStreaming: true
            )
        },
    ]
}

@Suite("MonarchAiStreamingText")
@MainActor
struct MonarchAiStreamingTextTests {
    private typealias Completion = MonarchAiStreamingTextCompletion
    private typealias Tokens = MonarchAiStreamingTextTokens

    @Test("Open emphasis and code spans are closed for display")
    func closesOpenSyntax() {
        #expect(Completion.complete("Ship the **beta") == "Ship the **beta**")
        #expect(Completion.complete("Ship *the **beta") == "Ship *the **beta***")
        #expect(Completion.complete("A ~~gone") == "A ~~gone~~")
        #expect(Completion.complete("snake_case_name") == "snake_case_name")
        #expect(Completion.complete("Run `make rel") == "Run `make rel`")
        #expect(Completion.complete("Run ``a ` b") == "Run ``a ` b``")
        #expect(Completion.complete("Closed **bold** text") == "Closed **bold** text")
        #expect(Completion.complete("Escaped \\*star") == "Escaped \\*star")
        #expect(Completion.complete("Inside `code *star` text") == "Inside `code *star` text")
    }

    @Test("A trailing delimiter run with nothing after it is hidden")
    func hidesTrailingDelimiters() {
        #expect(Completion.complete("Ship the *") == "Ship the ")
        #expect(Completion.complete("Ship the **") == "Ship the ")
        #expect(Completion.complete("`") == "")
    }

    @Test("A link whose target is still arriving shows its text only")
    func pendingLinks() {
        #expect(Completion.complete("Read [the notes](https://exa") == "Read the notes")
        #expect(Completion.complete("Read [the notes]") == "Read the notes")
        #expect(Completion.complete("Read [the no") == "Read the no")
        #expect(Completion.complete("See ![chart](https://ex") == "See chart")
        #expect(Completion.complete("A note[^1") == "A note")
        let finished = "Read [the notes](https://example.com) now"
        #expect(Completion.complete(finished) == finished)
    }

    @Test("Table rows are held back until the delimiter row arrives")
    func holdsBackTables() {
        #expect(Completion.complete("| Plan | Seats |") == "")
        #expect(Completion.complete("| Plan | Seats |\n| --") == "")
        let table = "| Plan | Seats |\n| --- | --- |\n| Team"
        #expect(Completion.complete(table) == table)
    }

    @Test("Code and display maths are left alone")
    func leavesCodeAlone() {
        #expect(Completion.complete("```swift\nlet a = *b") == "```swift\nlet a = *b")
        #expect(Completion.complete("    indented *code") == "    indented *code")
        #expect(Completion.complete("$$\nx^2 *") == "$$\nx^2 *")
    }

    @Test("Finished blocks keep their parse and only the tail is completed")
    func display() {
        let first = MonarchAiMarkdownParser.parseIncremental("One.\n\nTwo **bo", previous: nil)
        let shown = Tokens.display(first, isStreaming: true)
        #expect(shown.finished == MonarchAiMarkdownParser.parse("One."))
        #expect(shown.tail == MonarchAiMarkdownParser.parse("Two **bo**"))

        let second = MonarchAiMarkdownParser.parseIncremental("One.\n\nTwo **bold**.", previous: first)
        let next = Tokens.display(second, isStreaming: true)
        #expect(next.finished == shown.finished)

        let done = Tokens.display(second, isStreaming: false)
        #expect(done.tail == MonarchAiMarkdownParser.parse("Two **bold**."))

        let code = MonarchAiMarkdownParser.parseIncremental("```\nlet a = *b", previous: nil)
        #expect(Tokens.display(code, isStreaming: true).tail == code.blocks)
        let empty = MonarchAiMarkdownParser.parseIncremental("", previous: nil)
        #expect(Tokens.display(empty, isStreaming: true).all.isEmpty)
    }

    @Test("Rows keep their position as id and only finished rows are marked finished")
    func rows() {
        let finished = MonarchAiMarkdownParser.parse("## Title\n\nOne.")
        let tail = MonarchAiMarkdownParser.parse("Two.")
        let rows = MonarchAiStreamingTextRow.rows(finished: finished, tail: tail)
        #expect(rows.map(\.id) == [0, 1, 2])
        #expect(rows.map(\.isFinished) == [true, true, false])
        #expect(rows[0].gap == 0)
        #expect(rows[1].gap == MonarchAiMarkdownTokens.afterHeadingGap)
        #expect(rows[2].gap == MonarchAiMarkdownTokens.blockGap)
    }

    @Test("The caret follows the last text, inside lists and quotes, and never after code")
    func caret() {
        let caret = MonarchAiMarkdownInline.text(Tokens.caret)
        #expect(Tokens.withCaret([.paragraph([.text("Hi")])]) == [.paragraph([.text("Hi"), caret])])
        let heading = Tokens.withCaret([.heading(level: 2, content: [.text("Plan")])])
        #expect(heading == [.heading(level: 2, content: [.text("Plan"), caret])])
        let list = MonarchAiMarkdownParser.parse("1. One\n2. Two")
        guard case .list(_, _, _, let items)? = Tokens.withCaret(list)?.first else {
            Issue.record("The caret did not go into the list")
            return
        }
        #expect(items.last?.children == [.paragraph([.text("Two"), caret])])
        #expect(items.first?.children == [.paragraph([.text("One")])])
        guard case .quote(let children)? = Tokens.withCaret(MonarchAiMarkdownParser.parse("> Quoted"))?.first else {
            Issue.record("The caret did not go into the quote")
            return
        }
        #expect(children == [.paragraph([.text("Quoted"), caret])])
        #expect(Tokens.withCaret(MonarchAiMarkdownParser.parse("```\ncode")) == nil)
        #expect(Tokens.withCaret(MonarchAiMarkdownParser.parse("---")) == nil)
        #expect(Tokens.withCaret([]) == nil)
    }

    @Test("Code blocks and tables are spoken by name, not read out")
    func spokenBlocks() {
        let blocks = MonarchAiMarkdownParser.parse(StreamingSamples.answer)
        let spoken = Tokens.spoken(blocks, isStreaming: false, codeBlock: "Code block", table: "Table")
        let expected = [
            "Plan", "Ship the beta on Monday. Then tell support.", "Freeze the branch", "Run make release",
            "Code block", "Table",
        ]
        #expect(spoken.text == expected.joined(separator: "\n\n"))
        #expect(spoken.done == spoken.text.utf16.count)
        let streaming = Tokens.spoken(blocks, isStreaming: true, codeBlock: "Code block", table: "Table")
        #expect(streaming.text == spoken.text)
        #expect(streaming.done == (spoken.text.utf16.count - "\n\nTable".utf16.count))
    }

    @Test("The model caches finished blocks and speaks the same text as the pure function")
    func modelSpoken() {
        let model = MonarchAiStreamingTextModel()
        let first = model.display("One.\n\nTwo", isStreaming: true)
        let spoken = model.spoken(first, isStreaming: true, codeBlock: "Code", table: "Table")
        #expect(spoken.text == "One.\n\nTwo")
        #expect(spoken.done == "One.".utf16.count)
        let second = model.display("Other.\n\nTwo", isStreaming: false)
        let again = model.spoken(second, isStreaming: false, codeBlock: "Code", table: "Table")
        #expect(again.text == Tokens.spoken(second.all, isStreaming: false, codeBlock: "Code", table: "Table").text)
        #expect(again.text == "Other.\n\nTwo")
    }

    @Test("Whole sentences are announced, never partial ones")
    func announcesSentences() {
        var announcer = MonarchAiStreamingTextAnnouncer()
        let start = Date(timeIntervalSince1970: 1_000)
        #expect(
            announcer.update(
                spoken: "Ship the be",
                done: 0,
                isStreaming: true,
                announcesCompletion: true,
                complete: "Done",
                interval: 1,
                now: start
            ) == .silent
        )
        #expect(
            announcer.update(
                spoken: "Ship the beta. Then te",
                done: 0,
                isStreaming: true,
                announcesCompletion: true,
                complete: "Done",
                interval: 1,
                now: start.addingTimeInterval(0.1)
            ) == .announce("Ship the beta.")
        )
    }

    @Test("Announcements wait for the interval, then say the rest and Response complete")
    func throttlesAndCompletes() {
        var announcer = MonarchAiStreamingTextAnnouncer()
        let start = Date(timeIntervalSince1970: 1_000)
        _ = announcer.update(
            spoken: "One. Tw",
            done: 0,
            isStreaming: true,
            announcesCompletion: true,
            complete: "Done",
            interval: 1,
            now: start
        )
        #expect(
            announcer.update(
                spoken: "One. Two. Th",
                done: 0,
                isStreaming: true,
                announcesCompletion: true,
                complete: "Done",
                interval: 1,
                now: start.addingTimeInterval(0.5)
            ) == .wait(until: start.addingTimeInterval(1))
        )
        #expect(
            announcer.update(
                spoken: "One. Two. Three",
                done: 0,
                isStreaming: false,
                announcesCompletion: true,
                complete: "Done",
                interval: 1,
                now: start.addingTimeInterval(1)
            ) == .announce("Two. Three\n\nDone")
        )
        #expect(announcer.wasStreaming == false)
    }

    @Test("Finished blocks are announced whole while the next one streams")
    func announcesFinishedBlocks() {
        var announcer = MonarchAiStreamingTextAnnouncer()
        let outcome = announcer.update(
            spoken: "Plan\n\nShip it",
            done: "Plan".utf16.count,
            isStreaming: true,
            announcesCompletion: true,
            complete: "Done",
            interval: 1,
            now: Date(timeIntervalSince1970: 0)
        )
        #expect(outcome == .announce("Plan"))
    }

    @Test("Response complete waits for the reply when announcesCompletion is false")
    func owedCompletion() {
        var announcer = MonarchAiStreamingTextAnnouncer()
        let start = Date(timeIntervalSince1970: 1_000)
        _ = announcer.update(
            spoken: "Hi",
            done: 0,
            isStreaming: true,
            announcesCompletion: false,
            complete: "Done",
            interval: 1,
            now: start
        )
        #expect(
            announcer.update(
                spoken: "Hi there.",
                done: 0,
                isStreaming: false,
                announcesCompletion: false,
                complete: "Done",
                interval: 1,
                now: start
            ) == .announce("Hi there.")
        )
        #expect(announcer.owesCompletion)
        #expect(
            announcer.update(
                spoken: "Hi there.",
                done: 0,
                isStreaming: false,
                announcesCompletion: false,
                complete: "Done",
                interval: 1,
                now: start.addingTimeInterval(5)
            ) == .silent
        )
        #expect(
            announcer.update(
                spoken: "Hi there.",
                done: 0,
                isStreaming: false,
                announcesCompletion: true,
                complete: "Done",
                interval: 1,
                now: start.addingTimeInterval(6)
            ) == .announce("Done")
        )
    }

    @Test("Text that never streamed is not announced, and a new answer starts over")
    func historyAndReset() {
        var announcer = MonarchAiStreamingTextAnnouncer()
        let start = Date(timeIntervalSince1970: 1_000)
        #expect(
            announcer.update(
                spoken: "Old answer.",
                done: 0,
                isStreaming: false,
                announcesCompletion: true,
                complete: "Done",
                interval: 1,
                now: start
            ) == .silent
        )
        _ = announcer.update(
            spoken: "First. More",
            done: 0,
            isStreaming: true,
            announcesCompletion: true,
            complete: "Done",
            interval: 1,
            now: start
        )
        #expect(
            announcer.update(
                spoken: "Second. Again",
                done: 0,
                isStreaming: true,
                announcesCompletion: true,
                complete: "Done",
                interval: 1,
                now: start.addingTimeInterval(2)
            ) == .announce("Second.")
        )
    }

    @Test("The announcement interval is never below one second")
    func interval() {
        #expect(MonarchAiStreamingTextAnnouncer.interval(.milliseconds(200)) == 1)
        #expect(MonarchAiStreamingTextAnnouncer.interval(.milliseconds(2500)) == 2.5)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiStreamingTextStrings.complete == "Response complete")
        #expect(MonarchAiStreamingTextStrings.codeBlock == "Code block")
        #expect(MonarchAiStreamingTextStrings.tableName == "Table")
    }
}
