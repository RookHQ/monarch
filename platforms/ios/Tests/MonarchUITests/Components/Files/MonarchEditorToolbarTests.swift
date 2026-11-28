import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchEditorToolbarFixtures: ComponentFixtures {
    static let id = "editor-toolbar"

    static let fixtures = [
        Fixture("every tool, some on, undo disabled") {
            MonarchEditorToolbar(
                active: [.bold, .link, .heading(2), .bulletList],
                disabled: [.undo]
            ) { _ in }
        },
        Fixture("nothing on") {
            MonarchEditorToolbar { _ in }
        },
        Fixture("inline tools with link form") {
            MonarchEditorToolbar(
                groups: MonarchEditorTool.inlineGroups,
                active: [.italic],
                onLink: { _ in },
                action: { _ in }
            )
        },
        Fixture("custom tools and labels") {
            MonarchEditorToolbar(
                groups: [
                    [.bold, .italic],
                    [],
                    [.custom(id: "comment", title: "Comment", systemImage: "text.bubble")],
                ],
                labels: MonarchEditorToolbarLabels(toolbar: "Text tools", tools: [.bold: "Strong"])
            ) { _ in }
        },
        Fixture("disabled") {
            MonarchEditorToolbar(active: [.bold]) { _ in }.disabled(true)
        },
    ]
}

@Suite("MonarchEditorToolbar")
struct MonarchEditorToolbarTests {
    @Test("Undo, redo and product tools are buttons; styles are toggles; text styles are a menu")
    func kinds() {
        #expect(MonarchEditorToolbarTokens.kind(.undo) == .button)
        #expect(MonarchEditorToolbarTokens.kind(.custom(id: "c", title: "C", systemImage: "star")) == .button)
        #expect(MonarchEditorToolbarTokens.kind(.bold) == .toggle)
        #expect(MonarchEditorToolbarTokens.kind(.link) == .toggle)
        #expect(MonarchEditorToolbarTokens.kind(.codeBlock) == .toggle)
        #expect(MonarchEditorToolbarTokens.kind(.paragraph) == .textStyle)
        #expect(MonarchEditorToolbarTokens.kind(.heading(3)) == .textStyle)
    }

    @Test("Neighbouring text style tools become one menu")
    func segments() {
        #expect(
            MonarchEditorToolbarTokens.segments([.paragraph, .heading(1), .heading(2)])
                == [.textStyle([.paragraph, .heading(1), .heading(2)])]
        )
        #expect(
            MonarchEditorToolbarTokens.segments([.heading(1), .bold, .heading(2)])
                == [.textStyle([.heading(1)]), .tool(.bold), .textStyle([.heading(2)])]
        )
        #expect(MonarchEditorToolbarTokens.segments([.undo, .redo]) == [.tool(.undo), .tool(.redo)])
        #expect(MonarchEditorToolbarTokens.nonEmpty([[.bold], [], [.italic]]) == [[.bold], [.italic]])
    }

    @Test("The current text style is the active one, else body text")
    func currentStyle() {
        let tools: [MonarchEditorTool] = [.paragraph, .heading(1), .heading(2)]
        #expect(MonarchEditorToolbarTokens.currentTextStyle(tools, active: [.heading(2), .bold]) == .heading(2))
        #expect(MonarchEditorToolbarTokens.currentTextStyle(tools, active: []) == .paragraph)
        #expect(MonarchEditorToolbarTokens.currentTextStyle([.heading(1)], active: []) == nil)
    }

    @Test("Links: safe schemes, email, relative paths and bare domains")
    func links() {
        #expect(
            MonarchEditorToolbarTokens.linkURL(from: " https://example.com/a ")?.absoluteString
                == "https://example.com/a"
        )
        #expect(
            MonarchEditorToolbarTokens.linkURL(from: "example.com/docs")?.absoluteString == "https://example.com/docs"
        )
        #expect(
            MonarchEditorToolbarTokens.linkURL(from: "example.com:8080")?.absoluteString == "https://example.com:8080"
        )
        #expect(MonarchEditorToolbarTokens.linkURL(from: "sam@example.com")?.absoluteString == "mailto:sam@example.com")
        #expect(
            MonarchEditorToolbarTokens.linkURL(from: "mailto:sam@example.com")?.absoluteString
                == "mailto:sam@example.com"
        )
        #expect(MonarchEditorToolbarTokens.linkURL(from: "/help#links")?.absoluteString == "/help#links")
        #expect(MonarchEditorToolbarTokens.linkURL(from: "javascript:alert(1)") == nil)
        #expect(MonarchEditorToolbarTokens.linkURL(from: "data:text/html,hi") == nil)
        #expect(MonarchEditorToolbarTokens.linkURL(from: "not a link") == nil)
        #expect(MonarchEditorToolbarTokens.linkURL(from: "word") == nil)
        #expect(MonarchEditorToolbarTokens.linkURL(from: "") == nil)
    }

    @Test("Titles and symbols, with overrides; undo and redo follow the layout direction")
    func titles() {
        let labels = MonarchEditorToolbarLabels(tools: [.bold: "Strong"])
        #expect(MonarchEditorToolbarTokens.title(.bold, labels: labels) == "Strong")
        #expect(MonarchEditorToolbarTokens.title(.italic, labels: labels) == "Italic")
        #expect(MonarchEditorToolbarTokens.title(.heading(2), labels: labels) == "Heading 2")
        #expect(
            MonarchEditorToolbarTokens.title(.custom(id: "c", title: "Comment", systemImage: "x"), labels: labels)
                == "Comment"
        )
        #expect(MonarchEditorToolbarTokens.symbol(.undo).hasSuffix("backward"))
        #expect(MonarchEditorToolbarTokens.symbol(.redo).hasSuffix("forward"))
        #expect(MonarchEditorToolbarTokens.symbol(.heading(9)) == "6.square")
    }

    @Test("The default groups hold every built-in tool once")
    func groups() {
        let all = MonarchEditorTool.defaultGroups.flatMap { $0 }
        #expect(Set(all).count == all.count)
        #expect(all.count == 16)
        #expect(
            MonarchEditorTool.inlineGroups.flatMap { $0 }.allSatisfy { MonarchEditorToolbarTokens.kind($0) == .toggle }
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        let tools: [MonarchEditorTool] = [
            .undo, .redo, .paragraph, .heading(1), .bold, .italic, .strikethrough, .code, .link, .bulletList,
            .orderedList, .taskList, .blockquote, .codeBlock,
        ]
        let all =
            tools.map(MonarchEditorToolbarStrings.title) + [
                MonarchEditorToolbarStrings.toolbar, MonarchEditorToolbarStrings.textStyle,
                MonarchEditorToolbarStrings.linkTitle, MonarchEditorToolbarStrings.linkAddress,
                MonarchEditorToolbarStrings.linkApply, MonarchEditorToolbarStrings.linkRemove,
                MonarchEditorToolbarStrings.linkCancel, MonarchEditorToolbarStrings.linkHelp,
            ]
        #expect(all.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(MonarchEditorToolbarStrings.toolbar == "Formatting")
    }
}
