import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// Shared sample answers for the Markdown fixtures.
private enum MarkdownSample {
    static let full = """
        # Release plan
        Ship the **beta** on *Monday* and ~~Friday~~, then read the [notes](https://example.com/notes)[^1].

        ## Steps
        1. Freeze the branch
        2. Run `make release`
           - Check the build
           - Tag it

        - [x] Write the changelog
        - [ ] Tell support

        > Small steps every day add up.
        > They really do.

        | Plan | Seats | Price |
        | :--- | :---: | ----: |
        | Team | 10 | $90 |
        | Enterprise with a very long name that wraps inside its column | 500 | Talk to us |

        ```swift title=Release.swift
        let total = items.reduce(0, +)
        ```

        ---

        [^1]: Updated every Friday.
        """

    static let unsafe = """
        Click [paypal.com](https://evil.test/login), [this](javascript:alert(1)) or [that](//cdn.test/x). \
        Raw <b>HTML</b> stays text. Mail <ada@example.com>, visit www.example.com.

        ![A chart of sales](https://example.com/chart.png)
        """
}

enum MonarchAiMarkdownFixtures: ComponentFixtures {
    static let id = "ai-markdown"
    static let fixtures = [
        Fixture("every block: headings, lists, tasks, quote, table, code, rule, footnote") {
            MonarchAiMarkdown(MarkdownSample.full)
        },
        Fixture("streaming: an unclosed code block") {
            MonarchAiMarkdown("Here is the fix:\n\n```python\ndef total(items):\n    return sum(", isStreaming: true)
        },
        Fixture("untrusted links, raw HTML, autolinks and an image as alt text") {
            MonarchAiMarkdown(MarkdownSample.unsafe)
        },
        Fixture("image and code block slots, relative links, own labels") {
            MonarchAiMarkdown(
                "See [the guide](docs/guide).\n\n![A chart of sales](https://example.com/chart.png)\n\n$$\nE = mc^2\n$$",
                showsLinkHosts: false,
                labels: MonarchAiMarkdownLabels(table: "Grid", footnotes: "Notes", task: "To do"),
                onOpenLink: { _ in }
            )
            .image { _, alt in Text(alt) }
            .codeBlock { language, code, _ in Text(language + ": " + code) }
        },
        Fixture("empty text") {
            MonarchAiMarkdown("")
        },
    ]
}

@MainActor
@Suite("MonarchAiMarkdown")
struct MonarchAiMarkdownTests {
    typealias Parser = MonarchAiMarkdownParser
    typealias Inline = MonarchAiMarkdownInlineParser
    typealias Tokens = MonarchAiMarkdownTokens

    // MARK: Blocks

    @Test("ATX and setext headings, with closing hashes removed")
    func headings() {
        let blocks = Parser.parse("# One #\n\nTwo\n---\n\n###### Six")
        #expect(
            blocks == [
                .heading(level: 1, content: [.text("One")]),
                .heading(level: 2, content: [.text("Two")]),
                .heading(level: 6, content: [.text("Six")]),
            ]
        )
    }

    @Test("Paragraph lines join with a soft break; two trailing spaces make a hard break")
    func paragraphs() {
        #expect(Parser.parse("one\ntwo") == [.paragraph([.text("one\ntwo")])])
        #expect(Parser.parse("one  \ntwo") == [.paragraph([.text("one"), .lineBreak, .text("two")])])
        #expect(Parser.parse("") == [])
        #expect(Parser.parse("\n\n  \n") == [])
    }

    @Test("Fenced code keeps its language, file name and whether it is closed")
    func fencedCode() {
        let closed = Parser.parse("```TS title=\"app.ts\"\nlet a = 1\n```")
        #expect(
            closed == [
                .code(
                    MonarchAiMarkdownCode(
                        language: "ts",
                        info: "TS title=\"app.ts\"",
                        filename: "app.ts",
                        code: "let a = 1",
                        isClosed: true
                    )
                )
            ]
        )
        let open = Parser.parse("~~~\nstill coming")
        guard case .code(let code) = open.first else {
            Issue.record("expected a code block")
            return
        }
        #expect(!code.isClosed)
        #expect(code.code == "still coming")
        #expect(Parser.codeInfo("swift:Main.swift") == ("swift", "Main.swift"))
    }

    @Test("Indented code and display maths become code blocks")
    func otherCode() {
        guard case .code(let indented) = Parser.parse("    let a = 1\n    let b = 2").first else {
            Issue.record("expected indented code")
            return
        }
        #expect(indented.code == "let a = 1\nlet b = 2")
        guard case .code(let math) = Parser.parse("$$\nx^2\n$$").first else {
            Issue.record("expected maths")
            return
        }
        #expect(math.language == "math")
        #expect(math.code == "x^2")
        #expect(math.isClosed)
    }

    @Test("Lists: ordered with a start, nested, tight and loose, and task items")
    func lists() {
        let ordered = Parser.parse("3. three\n4. four")
        guard case .list(true, 3, false, let items) = ordered.first else {
            Issue.record("expected an ordered list starting at 3")
            return
        }
        #expect(items.count == 2)

        let nested = Parser.parse("- a\n  - b\n- c")
        guard case .list(false, _, false, let outer) = nested.first, outer.count == 2,
            case .list = outer[0].children.last
        else {
            Issue.record("expected a nested list in the first item")
            return
        }

        guard case .list(_, _, true, _) = Parser.parse("- a\n\n- b").first else {
            Issue.record("expected a loose list")
            return
        }

        guard case .list(_, _, _, let tasks) = Parser.parse("- [x] done\n- [ ] open\n- plain").first else {
            Issue.record("expected a task list")
            return
        }
        #expect(tasks.map(\.checked) == [true, false, nil])
        #expect(tasks[0].children == [.paragraph([.text("done")])])
    }

    @Test("Quotes nest blocks and take lazy continuation lines")
    func quotes() {
        #expect(
            Parser.parse("> one\ntwo\n\nafter") == [
                .quote([.paragraph([.text("one\ntwo")])]),
                .paragraph([.text("after")]),
            ]
        )
    }

    @Test("Tables read alignment, escaped pipes and pipes inside code; missing cells are empty")
    func tables() {
        let blocks = Parser.parse("| a | b | c |\n|:--|:-:|--:|\n| 1 \\| 2 | `x|y` |")
        guard case .table(let table) = blocks.first else {
            Issue.record("expected a table")
            return
        }
        #expect(table.alignments == [.leading, .center, .trailing])
        #expect(table.rows.count == 1)
        #expect(table.rows[0][0] == [.text("1 | 2")])
        #expect(table.rows[0][1] == [.code("x|y")])
        #expect(table.rows[0][2] == [])
        #expect(Parser.splitRow("| a | b |") == ["a", "b"])
        #expect(Parser.parse("a | b\n--- | ---\n") != Parser.parse("a | b"))
    }

    @Test("Rules, and a row of dashes under text is a setext heading, not a rule")
    func rules() {
        #expect(Parser.parse("***") == [.rule])
        #expect(Parser.parse("- - -") == [.rule])
        #expect(Parser.parse("text\n---").first?.isHeading == true)
    }

    @Test("Footnotes are numbered by first reference; unused ones are not listed; unknown ones stay text")
    func footnotes() {
        let blocks = Parser.parse(
            "B[^b] and A[^A] and B again[^b] and [^missing].\n\n[^a]: First.\n[^b]: Second.\n[^c]: Unused."
        )
        let footnotes = MonarchAiMarkdownFootnotes.of(blocks)
        #expect(footnotes.numbers == ["b": 1, "a": 2])
        #expect(footnotes.ordered.map(\.number) == [1, 2])
        #expect(footnotes.ordered[0].children == [.paragraph([.text("Second.")])])
        let renderer = renderer(numbers: footnotes.numbers)
        guard case .paragraph(let content) = blocks.first else {
            Issue.record("expected a paragraph")
            return
        }
        let text = String(renderer.attributed(content).characters)
        #expect(text == "B1 and A2 and B again1 and [^missing].")
    }

    @Test("Incremental parsing keeps the finished blocks and re-parses only the last")
    func incremental() {
        let first = Parser.parseIncremental("# Title\n\nSome", previous: nil)
        #expect(first.blocks.count == 2)
        let second = Parser.parseIncremental("# Title\n\nSome more\n\n```\ncode", previous: first)
        #expect(second.blocks == Parser.parse("# Title\n\nSome more\n\n```\ncode"))
        #expect(second.offsets.first == 0)
        let same = Parser.parseIncremental(second.text, previous: second)
        #expect(same == second)
        let replaced = Parser.parseIncremental("Other", previous: second)
        #expect(replaced.blocks == [.paragraph([.text("Other")])])
    }

    // MARK: Inline

    @Test("Emphasis, strong emphasis and strikethrough follow CommonMark's delimiter rules")
    func emphasis() {
        #expect(
            Inline.parse("*a* **b** ~~c~~") == [
                .styled(.emphasis, [.text("a")]), .text(" "),
                .styled(.strong, [.text("b")]), .text(" "),
                .styled(.strikethrough, [.text("c")]),
            ]
        )
        #expect(Inline.parse("snake_case_name") == [.text("snake_case_name")])
        #expect(Inline.parse("2 * 3 * 4") == [.text("2 * 3 * 4")])
        #expect(Inline.parse("***both***") == [.styled(.emphasis, [.styled(.strong, [.text("both")])])])
    }

    @Test("Code spans, escapes, entities and inline maths")
    func inlineAtoms() {
        #expect(Inline.parse("`` a`b ``") == [.code("a`b")])
        #expect(Inline.parse("\\*not\\*") == [.text("*not*")])
        #expect(Inline.parse("&amp; &#169; &#x41; &bogus;") == [.text("& \u{00A9} A &bogus;")])
        #expect(
            Inline.parse("\\(x^2\\) and $$y$$ cost $5") == [
                .math("x^2"), .text(" and "), .math("y"), .text(" cost $5"),
            ]
        )
    }

    @Test("Links, images, autolinks and bare URLs; links do not nest")
    func links() {
        #expect(
            Inline.parse("[a](https://x.test \"T\")") == [
                .link(href: "https://x.test", title: "T", autolink: false, children: [.text("a")])
            ]
        )
        #expect(
            Inline.parse("![alt *text*](https://x.test/i.png)") == [
                .image(src: "https://x.test/i.png", alt: "alt text", title: nil)
            ]
        )
        #expect(
            Inline.parse("<ada@example.com>") == [
                .link(href: "mailto:ada@example.com", title: nil, autolink: true, children: [.text("ada@example.com")])
            ]
        )
        #expect(
            Inline.parse("see www.example.com/a.") == [
                .text("see "),
                .link(
                    href: "https://www.example.com/a",
                    title: nil,
                    autolink: true,
                    children: [.text("www.example.com/a")]
                ),
                .text("."),
            ]
        )
        #expect(
            Inline.parse("(https://x.test/a_(b))") == [
                .text("("),
                .link(
                    href: "https://x.test/a_(b)",
                    title: nil,
                    autolink: true,
                    children: [.text("https://x.test/a_(b)")]
                ),
                .text(")"),
            ]
        )
        #expect(Inline.parse("<b>bold</b>") == [.text("<b>bold</b>")])
        #expect(Inline.parse("[outer [inner](https://a.test)](https://b.test)").count == 1)
    }

    // MARK: Safety

    @Test("Only http, https and mailto are followed; relative links only when the app opens links")
    func linkSafety() {
        #expect(MonarchAiMarkdownLinks.isSafeLink("https://a.test", allowsRelative: false))
        #expect(MonarchAiMarkdownLinks.isSafeLink("MAILTO:a@b.test", allowsRelative: false))
        #expect(!MonarchAiMarkdownLinks.isSafeLink("javascript:alert(1)", allowsRelative: true))
        #expect(!MonarchAiMarkdownLinks.isSafeLink("java\tscript:alert(1)", allowsRelative: true))
        #expect(!MonarchAiMarkdownLinks.isSafeLink("data:text/html,x", allowsRelative: true))
        #expect(!MonarchAiMarkdownLinks.isSafeLink("//cdn.test/x", allowsRelative: true))
        #expect(!MonarchAiMarkdownLinks.isSafeLink("docs/guide", allowsRelative: false))
        #expect(MonarchAiMarkdownLinks.isSafeLink("docs/guide", allowsRelative: true))
        #expect(!MonarchAiMarkdownLinks.isSafeLink("  ", allowsRelative: true))
        #expect(MonarchAiMarkdownLinks.isSafeImage("https://a.test/i.png"))
        #expect(!MonarchAiMarkdownLinks.isSafeImage("data:image/png;base64,AAAA"))
    }

    @Test("The host follows a link unless its text names it")
    func hosts() {
        #expect(MonarchAiMarkdownLinks.host(of: "https://User@WWW.Example.com:8080/a") == "www.example.com")
        #expect(MonarchAiMarkdownLinks.host(of: "mailto:a@b.test") == nil)
        #expect(MonarchAiMarkdownLinks.host(of: "docs/guide") == nil)
        #expect(
            MonarchAiMarkdownLinks.shownHost(href: "https://evil.test/login", linkText: "paypal.com") == "evil.test"
        )
        #expect(MonarchAiMarkdownLinks.shownHost(href: "https://www.example.com", linkText: "Example.com docs") == nil)
    }

    @Test("Unsafe links keep their text without a link; safe ones carry the URL and the host")
    func renderedLinks() {
        let renderer = renderer()
        let safe = renderer.attributed(Inline.parse("[paypal.com](https://evil.test/login)"))
        #expect(String(safe.characters) == "paypal.com (evil.test)")
        #expect(safe.runs.contains { $0.link == URL(string: "https://evil.test/login") })
        let unsafe = renderer.attributed(Inline.parse("[click](javascript:alert(1))"))
        #expect(String(unsafe.characters) == "click")
        #expect(!unsafe.runs.contains { $0.link != nil })
        let relative = renderer.attributed(Inline.parse("[guide](docs/guide)"))
        #expect(!relative.runs.contains { $0.link != nil })
        let relativeAllowed = self.renderer(allowsRelativeLinks: true).attributed(Inline.parse("[guide](docs/guide)"))
        #expect(relativeAllowed.runs.contains { $0.link != nil })
    }

    @Test("Emphasis and code become presentation intents; struck text is muted and struck")
    func renderedStyles() {
        let text = renderer().attributed(Inline.parse("*a* **b** `c` ~~d~~"))
        let intents = text.runs.compactMap(\.inlinePresentationIntent)
        #expect(intents.contains { $0.contains(.emphasized) })
        #expect(intents.contains { $0.contains(.stronglyEmphasized) })
        #expect(intents.contains { $0.contains(.code) })
        #expect(intents.contains { $0.contains(.strikethrough) })
        #expect(text.runs.contains { $0.strikethroughStyle != nil })
    }

    // MARK: Layout logic

    @Test("Gaps: none first, more above headings, less after them, tight inside tight lists")
    func gaps() {
        let heading = MonarchAiMarkdownBlock.heading(level: 2, content: [])
        let paragraph = MonarchAiMarkdownBlock.paragraph([])
        #expect(Tokens.gap(before: paragraph, after: nil, isTight: false) == 0)
        #expect(Tokens.gap(before: heading, after: paragraph, isTight: true) == Tokens.headingGap)
        #expect(Tokens.gap(before: paragraph, after: heading, isTight: false) == Tokens.afterHeadingGap)
        #expect(Tokens.gap(before: paragraph, after: paragraph, isTight: true) == Tokens.tightGap)
        #expect(Tokens.gap(before: paragraph, after: paragraph, isTight: false) == Tokens.blockGap)
        #expect(Tokens.itemGap(isLoose: true) == Tokens.blockGap)
        #expect(Tokens.itemGap(isLoose: false) == Tokens.tightGap)
    }

    @Test("Markers, heading styles and colour roles")
    func markers() {
        #expect(Tokens.bullet(depth: 0) == "\u{2022}")
        #expect(Tokens.bullet(depth: 1) == "\u{25E6}")
        #expect(Tokens.number(start: 3, index: 1) == "4.")
        #expect(Tokens.headingStyle(level: 1) == .subheading)
        #expect(Tokens.headingStyle(level: 4) == .bodyStrong)
        #expect(Tokens.textRole(isMuted: true) == \.textMuted)
        #expect(Tokens.textRole(isMuted: false) == \.textDefault)
        #expect(Tokens.Role.checkedTask != Tokens.Role.openTask)
        #expect(Tokens.alignment(.trailing) == .topTrailing)
        #expect(Tokens.textAlignment(.center) == .center)
    }

    @Test("Only a paragraph that is a single safe image with alt text goes to the image slot")
    func standaloneImage() {
        let image = Inline.parse("![Chart](https://a.test/c.png)")
        #expect(Tokens.standaloneImage(image)?.alt == "Chart")
        #expect(Tokens.standaloneImage(Inline.parse("![](https://a.test/c.png)")) == nil)
        #expect(Tokens.standaloneImage(Inline.parse("![Chart](data:image/png;base64,AA)")) == nil)
        #expect(Tokens.standaloneImage(Inline.parse("Look: ![Chart](https://a.test/c.png)")) == nil)
    }

    @Test("A code block is complete when closed or when streaming has stopped")
    func completeness() {
        let open = MonarchAiMarkdownCode(language: "", info: "", filename: nil, code: "", isClosed: false)
        #expect(!Tokens.isComplete(open, isStreaming: true))
        #expect(Tokens.isComplete(open, isStreaming: false))
        #expect(Tokens.codeLanguage(open) == nil)
    }

    // MARK: Strings

    @Test("The built-in strings come from the strings table, and labels replace them")
    func strings() {
        let labels = MonarchAiMarkdownLabels()
        #expect(labels.table == "Table")
        #expect(labels.tableSize(3, 2) == "3 rows, 2 columns")
        #expect(labels.footnotes == "Footnotes")
        #expect(labels.task == "Task")
        #expect(labels.linkHost("example.com") == "(example.com)")
        let own = MonarchAiMarkdownLabels(
            table: "T",
            tableSize: { "\($0)x\($1)" },
            footnotes: "F",
            task: "K",
            linkHost: { "<\($0)>" }
        )
        #expect(
            [own.table, own.tableSize(1, 2), own.footnotes, own.task, own.linkHost("h")] == [
                "T", "1x2", "F", "K", "<h>",
            ]
        )
    }

    private func renderer(
        numbers: [String: Int] = [:],
        allowsRelativeLinks: Bool = false
    ) -> MonarchAiMarkdownInlineRenderer {
        MonarchAiMarkdownInlineRenderer(
            theme: .light,
            footnoteNumbers: numbers,
            showsLinkHosts: true,
            allowsRelativeLinks: allowsRelativeLinks,
            linkHost: { "(\($0))" },
            footnoteFont: nil,
            footnoteRaise: 0
        )
    }
}

/// The parsers are pure: they run off the main actor (C-092).
@Suite("MonarchAiMarkdown parsing off the main actor")
struct MonarchAiMarkdownOffMainActorTests {
    @Test("A reply parses in a detached task, with the same result as on the main actor")
    func detachedParse() async {
        let text = "## Plan\n\nShip the **beta** with [notes](https://example.com).\n\n- one\n- two\n"
        let off = await Task.detached { MonarchAiMarkdownParser.parse(text) }.value
        let on = await MainActor.run { MonarchAiMarkdownParser.parse(text) }
        #expect(off == on)
        #expect(!off.isEmpty)
    }
}
