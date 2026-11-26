import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCodeBlockFixtures: ComponentFixtures {
    static let id = "code-block"

    static let typescript = """
        import { createClient } from "@example/sdk";
        // Create a client once and reuse it.
        export const client = createClient({ retries: 3, timeout: 2.5e3 });
        """

    static let diff = """
        diff --git a/server.ts b/server.ts
        --- a/server.ts
        +++ b/server.ts
        @@ -1,3 +1,3 @@
         import express from "express";
        -const port = 3000;
        +const port = Number(process.env.PORT ?? 8080);
         app.listen(port);
        """

    static let fixtures = [
        Fixture("typescript with line numbers") {
            MonarchCodeBlock(typescript, language: "ts", showsLineNumbers: true)
        },
        Fixture("long lines, wrapped") {
            MonarchCodeBlock(
                "curl -X POST https://api.example.com/v1/workspaces/42/items -H \"Authorization: Bearer $TOKEN\"",
                language: "bash",
                title: "Create an item",
                isWrapped: .constant(true)
            )
        },
        Fixture("long lines, scrolling") {
            MonarchCodeBlock(
                "SELECT id, name, created_at FROM items WHERE workspace_id = 42 ORDER BY created_at DESC LIMIT 50;",
                language: "sql"
            )
        },
        Fixture("diff") {
            MonarchCodeBlock(diff, language: "diff", title: "server.ts", showsLineNumbers: true)
        },
        Fixture("markup, json, css, python, go, rust") {
            VStack {
                MonarchCodeBlock("<a href=\"/docs\" class=\"link\">Docs</a> <!-- note -->", language: "html")
                MonarchCodeBlock("{\"id\": 1, \"tags\": [\"a\"], \"ok\": true}", language: "json")
                MonarchCodeBlock(".card { color: #1a2b3c; padding: 4px !important; }", language: "css")
                MonarchCodeBlock("def greet(name):\n    return f\"Hi {name}\"  # done", language: "python")
                MonarchCodeBlock("func main() { fmt.Println(\"hi\") }", language: "go")
                MonarchCodeBlock("fn main<'a>() { let c = 'x'; }", language: "rust")
            }
        },
        Fixture("plain, no header") {
            MonarchCodeBlock(
                "Plain text\n\n\twith a tab",
                isHighlighted: false,
                showsWrapToggle: false,
                isCopyable: false
            )
        },
        Fixture("empty code") {
            MonarchCodeBlock("", language: "ts")
        },
        Fixture("custom labels") {
            MonarchCodeBlock(
                "print(1)",
                language: "unknownlang",
                labels: MonarchCodeBlockLabels(copy: "Copier", wrap: "Retour à la ligne")
            )
        },
    ]
}

@Suite("MonarchCodeBlock")
struct MonarchCodeBlockTests {
    typealias Tokenizer = MonarchCodeBlockTokenizer
    typealias Tokens = MonarchCodeBlockTokens
    typealias Token = MonarchCodeBlockTokenizer.Token

    private func kinds(_ code: String, _ language: String) -> [Token] {
        Tokenizer.tokenize(code, language: language)
    }

    private func text(of kind: Tokenizer.Kind, in tokens: [Token]) -> [String] {
        tokens.filter { $0.kind == kind }.map(\.text)
    }

    @Test("Languages resolve through aliases; unknown ones are plain")
    func resolve() {
        #expect(Tokenizer.resolve("ts") == "js")
        #expect(Tokenizer.resolve(" TypeScript ") == "js")
        #expect(Tokenizer.resolve("py") == "python")
        #expect(Tokenizer.resolve("zsh") == "bash")
        #expect(Tokenizer.resolve("svg") == "html")
        #expect(Tokenizer.resolve("rs") == "rust")
        #expect(Tokenizer.resolve("cobol") == nil)
        #expect(Tokenizer.resolve(nil) == nil)
        #expect(kinds("x = 1", "cobol") == [Token(kind: .plain, text: "x = 1")])
        #expect(Tokenizer.tokenize("", language: "ts").isEmpty)
    }

    @Test("Tokens put back together give the code exactly")
    func lossless() {
        for (code, language) in [
            (MonarchCodeBlockFixtures.typescript, "ts"), ("<p a=\"1\">x</p>", "html"), ("a { b: c; }", "css"),
            ("SELECT 1 -- x", "sql"), ("echo $HOME # c", "bash"), ("let s = \"a\\\"b\";", "rust"),
        ] {
            #expect(kinds(code, language).map(\.text).joined() == code)
        }
    }

    @Test("JavaScript: keywords, strings, comments, numbers and template literals")
    func javascript() {
        let tokens = kinds("const n = 0x1F + 2.5e3; // c\n/* b */ `t\nx` 'q'", "ts")
        #expect(text(of: .keyword, in: tokens) == ["const"])
        #expect(text(of: .number, in: tokens) == ["0x1F", "2.5e3"])
        #expect(text(of: .comment, in: tokens) == ["// c", "/* b */"])
        #expect(text(of: .string, in: tokens).contains("`t\nx`"))
        #expect(text(of: .string, in: tokens).contains("'q'"))
    }

    @Test("Numbers inside identifiers are not numbers")
    func identifiers() {
        let tokens = kinds("item2 = h1", "js")
        #expect(text(of: .number, in: tokens).isEmpty)
    }

    @Test("JSON keys are properties; values are strings and keywords")
    func json() {
        let tokens = kinds("{\"id\" : \"a\", \"ok\": true, \"n\": -1}", "json")
        #expect(text(of: .property, in: tokens) == ["\"id\"", "\"ok\"", "\"n\""])
        #expect(text(of: .string, in: tokens) == ["\"a\""])
        #expect(text(of: .keyword, in: tokens) == ["true"])
        #expect(text(of: .number, in: tokens) == ["1"])
    }

    @Test("CSS: properties inside braces, hex colours, at-rules and keywords")
    func css() {
        let tokens = kinds("@media print { a { color: #fff; margin: auto; --gap: 2px; } }", "css")
        #expect(text(of: .keyword, in: tokens) == ["@media", "auto"])
        #expect(text(of: .property, in: tokens) == ["color", "margin", "--gap"])
        #expect(text(of: .number, in: tokens) == ["#fff", "2px"])
    }

    @Test("Shell: # starts a comment only after white space")
    func bash() {
        let tokens = kinds("echo a#b # note", "bash")
        #expect(text(of: .comment, in: tokens) == ["# note"])
        #expect(text(of: .keyword, in: tokens) == ["echo"])
    }

    @Test("SQL keywords ignore case")
    func sql() {
        let tokens = kinds("Select * FROM t -- all", "sql")
        #expect(text(of: .keyword, in: tokens) == ["Select", "FROM"])
        #expect(text(of: .comment, in: tokens) == ["-- all"])
    }

    @Test("Python triple quotes span lines")
    func python() {
        let tokens = kinds("x = \"\"\"a\nb\"\"\"\ndef f(): pass", "python")
        #expect(text(of: .string, in: tokens) == ["\"\"\"a\nb\"\"\""])
        #expect(text(of: .keyword, in: tokens) == ["def", "pass"])
    }

    @Test("Rust: short character literals, but lifetimes stay plain")
    func rust() {
        let tokens = kinds("fn f<'a>(c: char) { let x = 'y'; }", "rust")
        #expect(text(of: .string, in: tokens) == ["'y'"])
        #expect(text(of: .keyword, in: tokens) == ["fn", "let"])
    }

    @Test("Markup: tags, attributes, attribute values and comments")
    func markup() {
        let tokens = kinds("<a href=\"/x\" hidden>t</a><br/><!-- c -->", "html")
        // Neighbouring tokens of one kind merge, as on the web.
        #expect(text(of: .tag, in: tokens) == ["<a", ">", "</a><br/>"])
        #expect(text(of: .attr, in: tokens) == ["href", "hidden"])
        #expect(text(of: .string, in: tokens) == ["\"/x\""])
        #expect(text(of: .comment, in: tokens) == ["<!-- c -->"])
    }

    @Test("Lines split at line breaks, including Windows ones; a final break adds no line")
    func lines() {
        let lines = MonarchCodeBlockLine.lines(code: "a\r\nb\n\nc\n", language: "ts", isDiff: false, highlight: true)
        #expect(lines.map(\.text) == ["a", "b", "", "c"])
        #expect(lines.allSatisfy { $0.diff == nil })
        let plain = MonarchCodeBlockLine.lines(code: "let x", language: "ts", isDiff: false, highlight: false)
        #expect(plain.first?.tokens == [Token(kind: .plain, text: "let x")])
    }

    @Test("Diff lines get their kind and lose their marker")
    func diff() {
        let lines = MonarchCodeBlockLine.lines(
            code: MonarchCodeBlockFixtures.diff,
            language: "diff",
            isDiff: true,
            highlight: true
        )
        #expect(lines.map(\.diff) == [.meta, .meta, .meta, .hunk, .context, .remove, .add, .context])
        #expect(lines[4].text == "import express from \"express\";")
        #expect(lines[5].text == "const port = 3000;")
        #expect(lines[1].text == "--- a/server.ts")
        #expect(MonarchCodeBlockLine.diffKinds(["@@ x", "--- y"]) == [.hunk, .remove])
    }

    @Test("The diff mode follows the language unless set")
    func isDiff() {
        #expect(Tokens.isDiff(nil, language: "diff"))
        #expect(Tokens.isDiff(nil, language: "Patch"))
        #expect(!Tokens.isDiff(nil, language: "ts"))
        #expect(Tokens.isDiff(true, language: "ts"))
        #expect(!Tokens.isDiff(false, language: "diff"))
    }

    @Test("The header and the name come from the title, else the language")
    func names() {
        let labels = MonarchCodeBlockLabels()
        #expect(Tokens.languageName("ts") == "TypeScript")
        #expect(Tokens.languageName("Kotlin") == "Kotlin")
        #expect(Tokens.languageName(" ") == nil)
        #expect(Tokens.headerTitle(title: "server.ts", language: "ts") == "server.ts")
        #expect(Tokens.headerTitle(title: nil, language: "py") == "Python")
        #expect(Tokens.headerTitle(title: "", language: nil) == nil)
        #expect(Tokens.name(title: "server.ts", language: "ts", labels: labels) == "server.ts")
        #expect(Tokens.name(title: nil, language: "ts", labels: labels) == "TypeScript code")
        #expect(Tokens.name(title: nil, language: nil, labels: labels) == "Code")
        let custom = MonarchCodeBlockLabels(codeName: { "Code \($0 ?? "brut")" })
        #expect(Tokens.name(title: nil, language: "go", labels: custom) == "Code Go")
        #expect(Tokens.name(title: nil, language: nil, labels: custom) == "Code brut")
    }

    @Test("Each token kind has its colour role; comments are italic")
    func colours() {
        #expect(Tokens.role(for: .plain) == nil)
        #expect(Tokens.role(for: .keyword) == \MonarchColorTokens.infoStrong)
        #expect(Tokens.role(for: .string) == \MonarchColorTokens.successStrong)
        #expect(Tokens.role(for: .number) == \MonarchColorTokens.warningStrong)
        #expect(Tokens.role(for: .comment) == \MonarchColorTokens.textMuted)
        #expect(Tokens.role(for: .tag) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.role(for: .attr) == \MonarchColorTokens.aiStrong)
        #expect(Tokens.role(for: .property) == \MonarchColorTokens.aiStrong)
        #expect(Tokens.isItalic(.comment))
        #expect(!Tokens.isItalic(.keyword))
    }

    @Test("Diff lines have a fill, a visible marker and a marker colour")
    func diffAppearance() {
        #expect(Tokens.lineBackground(.add) == \MonarchColorTokens.aiDiffAdd)
        #expect(Tokens.lineBackground(.remove) == \MonarchColorTokens.aiDiffRemove)
        #expect(Tokens.lineBackground(.context) == nil)
        #expect(Tokens.lineBackground(nil) == nil)
        #expect(Tokens.marker(.add) == "+")
        #expect(Tokens.marker(.remove) == "\u{2212}")
        #expect(Tokens.marker(.hunk) == " ")
        #expect(Tokens.markerRole(.add) == \MonarchColorTokens.aiDiffAddStrong)
        #expect(Tokens.markerRole(.remove) == \MonarchColorTokens.aiDiffRemoveStrong)
        #expect(Tokens.lineForeground(.hunk) == \MonarchColorTokens.infoStrong)
        #expect(Tokens.lineForeground(.meta) == \MonarchColorTokens.textMuted)
        #expect(Tokens.lineForeground(nil) == \MonarchColorTokens.textDefault)
        #expect(Tokens.isLineEmphasized(.meta))
        #expect(!Tokens.isLineEmphasized(.add))
    }

    @Test("Line numbers are padded to one width")
    func lineNumbers() {
        #expect(Tokens.lineNumber(0, of: 5) == "  1")
        #expect(Tokens.lineNumber(9, of: 12) == " 10")
        #expect(Tokens.lineNumber(0, of: 1200) == "   1")
        #expect(Tokens.lineNumber(1199, of: 1200) == "1200")
    }

    @Test("Tabs are drawn as four spaces")
    func tabs() {
        #expect(Tokens.displayText("\tx") == "    x")
    }

    @Test("VoiceOver reads changed diff lines with their change, and skips blank lines")
    func spoken() {
        let labels = MonarchCodeBlockLabels()
        let add = MonarchCodeBlockLine(diff: .add, tokens: [Token(kind: .plain, text: "a = 1")])
        let remove = MonarchCodeBlockLine(diff: .remove, tokens: [])
        let plain = MonarchCodeBlockLine(diff: nil, tokens: [Token(kind: .keyword, text: "let")])
        let blank = MonarchCodeBlockLine(diff: nil, tokens: [Token(kind: .plain, text: "  ")])
        #expect(Tokens.spokenLine(add, labels: labels) == "Added: a = 1")
        #expect(Tokens.spokenLine(remove, labels: labels) == "Removed: ")
        #expect(Tokens.spokenLine(plain, labels: labels) == "let")
        #expect(Tokens.spokenLine(add, labels: MonarchCodeBlockLabels(added: "Ajouté")) == "Ajouté: a = 1")
        #expect(Tokens.isSpoken(add))
        #expect(Tokens.isSpoken(remove))
        #expect(Tokens.isSpoken(plain))
        #expect(!Tokens.isSpoken(blank))
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchCodeBlockStrings.wrap == "Wrap lines")
        #expect(MonarchCodeBlockStrings.added == "Added")
        #expect(MonarchCodeBlockStrings.removed == "Removed")
        #expect(MonarchCodeBlockStrings.code == "Code")
        #expect(MonarchCodeBlockStrings.languageCode("Go") == "Go code")
        #expect(MonarchCodeBlockLabels().resolvedWrap == "Wrap lines")
        #expect(MonarchCodeBlockLabels(wrap: "Umbruch").resolvedWrap == "Umbruch")
    }

    #if os(iOS)
        @Test("The header buttons keep a 44-point touch target")
        @MainActor
        func hitTarget() {
            let block = MonarchCodeBlock("let x = 1", language: "ts")
            #expect(Host.layout(block).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
