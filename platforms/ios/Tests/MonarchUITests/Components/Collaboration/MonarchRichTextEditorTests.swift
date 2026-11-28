import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let longDocument = AttributedString(
    String(repeating: "A paragraph of notes that is long enough to wrap onto several lines. ", count: 4)
)

/// The SwiftUI font of the first run of a range, typed so the attribute is not ambiguous.
@available(iOS 26.0, macOS 26.0, *)
private func runFont(_ text: AttributedString, _ range: Range<AttributedString.Index>) -> Font? {
    text[range].runs.first?.font
}

/// Whether the first run of a range has a SwiftUI strikethrough.
@available(iOS 26.0, macOS 26.0, *)
private func hasStrikethrough(_ text: AttributedString, _ range: Range<AttributedString.Index>) -> Bool {
    let style: Text.LineStyle? = text[range].runs.first?.strikethroughStyle
    return style != nil
}

enum MonarchRichTextEditorFixtures: ComponentFixtures {
    static let id = "rich-text-editor"
    static let fixtures = [
        Fixture("empty with a prompt and the default toolbar") {
            if #available(iOS 26.0, macOS 26.0, *) {
                MonarchRichTextEditor("Notes", text: .constant(AttributedString()), prompt: "Write something")
            }
        },
        Fixture("formatted text, inline toolbar, count") {
            if #available(iOS 26.0, macOS 26.0, *) {
                MonarchRichTextEditor(
                    "Reply",
                    text: .constant(longDocument),
                    tools: MonarchEditorTool.inlineGroups,
                    showsCharacterCount: true
                )
            }
        },
        Fixture("over the limit, invalid") {
            if #available(iOS 26.0, macOS 26.0, *) {
                MonarchRichTextEditor("Reply", text: .constant(longDocument), maxLength: 40, isInvalid: true)
            }
        },
        Fixture("no toolbar, disabled") {
            if #available(iOS 26.0, macOS 26.0, *) {
                MonarchRichTextEditor("Notes", text: .constant(AttributedString("Locked")), tools: [])
                    .disabled(true)
            }
        },
        Fixture("read only") {
            if #available(iOS 26.0, macOS 26.0, *) {
                MonarchRichTextEditor("Summary", text: .constant(longDocument), isReadOnly: true)
            }
        },
        Fixture("with mentions") {
            if #available(iOS 26.0, macOS 26.0, *) {
                MonarchRichTextEditor(
                    "Comment",
                    text: .constant(AttributedString("Thanks @an")),
                    mentionItems: [MonarchMentionItem(id: "u1", label: "Ana Lopez", detail: "Design")]
                )
            }
        },
    ]
}

@Suite("MonarchRichTextEditor")
@MainActor
struct MonarchRichTextEditorTests {
    typealias Tokens = MonarchRichTextEditorTokens
    let labels = MonarchRichTextEditorLabels()
    let context = EnvironmentValues().fontResolutionContext
    let fonts = MonarchDocumentFonts(typography: .system)

    @Test("Bold, italic and code toggle on a selection and combine")
    func inlineTraits() throws {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("Hello world")
        let hello = try #require(text.range(of: "Hello"))
        var selection = AttributedTextSelection(range: hello)
        Engine.apply(.bold, to: &text, selection: &selection, fonts: fonts, context: context)
        let afterBold = try #require(text.range(of: "Hello"))
        #expect(Engine.traits(of: runFont(text, afterBold), fonts: fonts, context: context) == .bold)
        #expect(Engine.active(in: text, selection: selection, fonts: fonts, context: context).contains(.bold))
        let world = try #require(text.range(of: "world"))
        #expect(runFont(text, world) == nil)

        Engine.apply(.italic, to: &text, selection: &selection, fonts: fonts, context: context)
        let both = Engine.active(in: text, selection: selection, fonts: fonts, context: context)
        #expect(both.isSuperset(of: [.bold, .italic]))
        Engine.apply(.bold, to: &text, selection: &selection, fonts: fonts, context: context)
        let italicOnly = try #require(text.range(of: "Hello"))
        #expect(Engine.traits(of: runFont(text, italicOnly), fonts: fonts, context: context) == .italic)
        Engine.apply(.italic, to: &text, selection: &selection, fonts: fonts, context: context)
        let plain = try #require(text.range(of: "Hello"))
        #expect(runFont(text, plain) == nil)

        Engine.apply(.code, to: &text, selection: &selection, fonts: fonts, context: context)
        #expect(Engine.active(in: text, selection: selection, fonts: fonts, context: context).contains(.code))
    }

    @Test("A format is on only when every selected run has it")
    func mixedSelection() throws {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("Hello world")
        var selection = AttributedTextSelection(range: try #require(text.range(of: "Hello")))
        Engine.apply(.bold, to: &text, selection: &selection, fonts: fonts, context: context)
        var all = AttributedTextSelection(range: text.startIndex..<text.endIndex)
        #expect(!Engine.active(in: text, selection: all, fonts: fonts, context: context).contains(.bold))
        Engine.apply(.bold, to: &text, selection: &all, fonts: fonts, context: context)
        #expect(Engine.active(in: text, selection: all, fonts: fonts, context: context).contains(.bold))
    }

    @Test("Strikethrough and links are attributes")
    func strikethroughAndLink() throws {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("See the plan")
        var selection = AttributedTextSelection(range: try #require(text.range(of: "plan")))
        Engine.apply(.strikethrough, to: &text, selection: &selection, fonts: fonts, context: context)
        let plan = try #require(text.range(of: "plan"))
        #expect(hasStrikethrough(text, plan))
        let url = try #require(URL(string: "https://example.com/plan"))
        Engine.setLink(url, in: &text, selection: &selection)
        #expect(text[try #require(text.range(of: "plan"))].runs.first?.link == url)
        #expect(Engine.active(in: text, selection: selection, fonts: fonts, context: context).contains(.link))
        Engine.setLink(nil, in: &text, selection: &selection)
        #expect(text[try #require(text.range(of: "plan"))].runs.first?.link == nil)
    }

    @Test("A link at the insertion point is inserted as linked text")
    func insertLink() throws {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("Read ")
        var selection = AttributedTextSelection(insertionPoint: text.endIndex)
        let url = try #require(URL(string: "https://example.com"))
        Engine.setLink(url, in: &text, selection: &selection)
        #expect(String(text.characters) == "Read https://example.com")
        #expect(text[try #require(text.range(of: "https://example.com"))].runs.first?.link == url)
        #expect(Engine.bounds(selection, in: text) == text.endIndex..<text.endIndex)
    }

    @Test("Headings apply to the whole paragraph and toggle back to body text")
    func headings() throws {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("Intro\nPlan")
        let plan = try #require(text.range(of: "Plan"))
        var selection = AttributedTextSelection(insertionPoint: text.characters.index(after: plan.lowerBound))
        Engine.apply(.heading(2), to: &text, selection: &selection, fonts: fonts, context: context)
        let heading = try #require(text.range(of: "Plan"))
        #expect(runFont(text, heading) == fonts.base(2))
        #expect(runFont(text, try #require(text.range(of: "Intro"))) == nil)
        let active = Engine.active(in: text, selection: selection, fonts: fonts, context: context)
        #expect(active.contains(.heading(2)))
        #expect(!active.contains(.paragraph))
        Engine.apply(.heading(2), to: &text, selection: &selection, fonts: fonts, context: context)
        #expect(runFont(text, try #require(text.range(of: "Plan"))) == nil)
        #expect(Engine.active(in: text, selection: selection, fonts: fonts, context: context).contains(.paragraph))
    }

    @Test("Each heading level has its own font, recognised by size")
    func levels() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        #expect(Engine.level(of: nil, fonts: fonts, context: context) == 0)
        #expect(Engine.level(of: fonts.body, fonts: fonts, context: context) == 0)
        for level in MonarchDocumentFonts.levels {
            #expect(Engine.level(of: fonts.base(level), fonts: fonts, context: context) == level)
            #expect(Engine.level(of: fonts.base(level).italic(), fonts: fonts, context: context) == level)
        }
        #expect(Engine.traits(of: fonts.base(1), fonts: fonts, context: context).isEmpty)
        #expect(Engine.font(level: 0, traits: [], fonts: fonts) == nil)
        #expect(Engine.font(level: 2, traits: [], fonts: fonts) == fonts.base(2))
    }

    @Test("Lists are prefixes on every selected paragraph, numbered from 1")
    func lists() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("Milk\nEggs\nBread")
        var selection = AttributedTextSelection(range: text.startIndex..<text.endIndex)
        Engine.apply(.bulletList, to: &text, selection: &selection, fonts: fonts, context: context)
        #expect(String(text.characters) == "• Milk\n• Eggs\n• Bread")
        selection = AttributedTextSelection(range: text.startIndex..<text.endIndex)
        #expect(Engine.active(in: text, selection: selection, fonts: fonts, context: context).contains(.bulletList))
        Engine.apply(.orderedList, to: &text, selection: &selection, fonts: fonts, context: context)
        #expect(String(text.characters) == "1. Milk\n2. Eggs\n3. Bread")
        selection = AttributedTextSelection(range: text.startIndex..<text.endIndex)
        Engine.apply(.orderedList, to: &text, selection: &selection, fonts: fonts, context: context)
        #expect(String(text.characters) == "Milk\nEggs\nBread")
    }

    @Test("List prefixes are recognised")
    func prefixes() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        #expect(Engine.listKind("• Milk") == .bullet)
        #expect(Engine.listKind("12. Milk") == .ordered)
        #expect(Engine.orderedNumber("12. Milk") == 12)
        #expect(Engine.listKind("12.Milk") == nil)
        #expect(Engine.listKind("Milk") == nil)
        #expect(Engine.prefixLength("12. Milk") == 4)
        #expect(Engine.prefixLength("• Milk") == 2)
    }

    @Test("Return continues a list, and Return on an empty item ends it")
    func continueList() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("1. Milk\n")
        var selection = AttributedTextSelection(insertionPoint: text.endIndex)
        #expect(Engine.continueList(after: AttributedString("1. Milk"), in: &text, selection: &selection))
        #expect(String(text.characters) == "1. Milk\n2. ")
        #expect(Engine.bounds(selection, in: text) == text.endIndex..<text.endIndex)

        var bullets = AttributedString("• Milk\n• \n")
        var caret = AttributedTextSelection(insertionPoint: bullets.endIndex)
        #expect(Engine.continueList(after: AttributedString("• Milk\n• "), in: &bullets, selection: &caret))
        #expect(String(bullets.characters) == "• Milk\n")
        #expect(Engine.bounds(caret, in: bullets) == bullets.endIndex..<bullets.endIndex)

        var plain = AttributedString("Milk\n")
        var end = AttributedTextSelection(insertionPoint: plain.endIndex)
        #expect(!Engine.continueList(after: AttributedString("Milk"), in: &plain, selection: &end))
        #expect(!Engine.continueList(after: AttributedString("• Milk"), in: &text, selection: &selection))
    }

    @Test("Typing past the limit is refused and a paste is cut to fit")
    func clampsToTheLimit() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        // Typing one character at the limit changes nothing.
        let old = AttributedString("abcde")
        var typed = AttributedString("abcdeX")
        var caret = AttributedTextSelection(insertionPoint: typed.endIndex)
        #expect(Engine.clamp(&typed, after: old, limit: 5, selection: &caret))
        #expect(String(typed.characters) == "abcde")
        #expect(Engine.bounds(caret, in: typed) == typed.endIndex..<typed.endIndex)

        // A paste in the middle keeps what fits and puts the caret after it.
        var pasted = AttributedString("abXXXXXcde")
        var inside = AttributedTextSelection(insertionPoint: pasted.endIndex)
        #expect(Engine.clamp(&pasted, after: old, limit: 7, selection: &inside))
        #expect(String(pasted.characters) == "abXXcde")
        let offset = pasted.characters.distance(
            from: pasted.startIndex,
            to: Engine.bounds(inside, in: pasted).lowerBound
        )
        #expect(offset == 4)

        // Within the limit, deleting and a replacement of equal length are untouched.
        var shorter = AttributedString("abcd")
        var none = AttributedTextSelection(insertionPoint: shorter.endIndex)
        #expect(!Engine.clamp(&shorter, after: old, limit: 5, selection: &none))
        var same = AttributedString("abXde")
        #expect(!Engine.clamp(&same, after: old, limit: 5, selection: &none))

        // A document the app set over the limit may shrink but not grow.
        let long = AttributedString("abcdefgh")
        var grown = AttributedString("abcdefghi")
        #expect(Engine.clamp(&grown, after: long, limit: 5, selection: &none))
        #expect(String(grown.characters) == "abcdefgh")
        var cut = AttributedString("abcdefg")
        #expect(!Engine.clamp(&cut, after: long, limit: 5, selection: &none))
        #expect(MonarchRichTextEditor.isOverLimit(long, maxLength: 5))
    }

    @Test("A mention being typed is found at the caret and replaced by the name")
    func mentions() throws {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        var text = AttributedString("Thanks @an")
        var selection = AttributedTextSelection(insertionPoint: text.endIndex)
        let found = try #require(Engine.mention(in: text, selection: selection, triggers: Tokens.mentionTriggers))
        #expect(found.query.text == "an")
        #expect(String(text.characters[found.range]) == "@an")
        Engine.insertMention(
            MonarchMentionItem(id: "u1", label: "Ana Lopez"),
            trigger: found.query.trigger,
            replacing: found.range,
            in: &text,
            selection: &selection
        )
        #expect(String(text.characters) == "Thanks @Ana Lopez ")
        #expect(Engine.bounds(selection, in: text) == text.endIndex..<text.endIndex)
        #expect(
            Engine.mention(in: AttributedString("mail@x"), selection: AttributedTextSelection(), triggers: ["@"]) == nil
        )
    }

    @Test("Paragraphs covering a range exclude the line breaks")
    func paragraphs() throws {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        typealias Engine = MonarchRichTextEditorEngine
        let text = AttributedString("One\nTwo\nThree")
        let range = try #require(text.range(of: "ne\nTw"))
        let found = Engine.paragraphs(covering: range, in: text).map { Engine.string($0, in: text) }
        #expect(found == ["One", "Two"])
        let all = Engine.paragraphs(covering: text.startIndex..<text.endIndex, in: text).count
        #expect(all == 3)
    }

    @Test("Unsupported tools are left out of the toolbar; checklist, quote, code block and custom tools stay")
    func supportedTools() {
        let custom = MonarchEditorTool.custom(id: "ai", title: "Rewrite", systemImage: "sparkles")
        let groups = Tokens.supportedTools([[.taskList, .blockquote, .codeBlock], [.bold, custom, .heading(5)]])
        #expect(groups == [[.taskList, .blockquote, .codeBlock], [.bold, custom]])
        #expect(Tokens.supportedTools(MonarchEditorTool.defaultGroups).flatMap { $0 }.contains(.undo))
        #expect(Tokens.supportedTools(MonarchEditorTool.defaultGroups).flatMap { $0 }.contains(.taskList))
    }

    @Test("The count shows against the limit and reads what is left or over")
    func count() {
        #expect(Tokens.showsCount(nil, limit: 10))
        #expect(!Tokens.showsCount(nil, limit: nil))
        #expect(Tokens.showsCount(true, limit: nil))
        #expect(Tokens.countText(count: 1, limit: nil, labels: labels) == "1 character")
        #expect(Tokens.countText(count: 12, limit: nil, labels: labels) == "12 characters")
        #expect(Tokens.countText(count: 12, limit: 280, labels: labels) == "12 / 280")
        #expect(Tokens.countAccessibilityText(count: 279, limit: 280, labels: labels) == "1 character left")
        #expect(Tokens.countAccessibilityText(count: 12, limit: 280, labels: labels) == "268 characters left")
        #expect(Tokens.countAccessibilityText(count: 281, limit: 280, labels: labels) == "1 character over the limit")
        #expect(Tokens.countAccessibilityText(count: 283, limit: 280, labels: labels) == "3 characters over the limit")
        #expect(Tokens.isOverLimit(count: 281, limit: 280))
        #expect(!Tokens.isOverLimit(count: 280, limit: 280))
        #expect(!Tokens.isOverLimit(count: 5, limit: nil))
    }

    @Test("Colours follow the state")
    func colors() {
        #expect(Tokens.border(isInvalid: true, isEnabled: true) == \.dangerStrong)
        #expect(Tokens.border(isInvalid: false, isEnabled: true) == \.borderInput)
        #expect(Tokens.border(isInvalid: true, isEnabled: false) == \.borderSubtle)
        #expect(Tokens.background(isEnabled: false) == \.bgSubtle)
        #expect(Tokens.countColor(count: 5, limit: 4) == \.dangerStrong)
        #expect(Tokens.countColor(count: 4, limit: 4) == \.textMuted)
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchRichTextEditorLabels(
            count: { "\($0) Zeichen" },
            countOfLimit: { "\($0) von \($1)" },
            remaining: { "noch \($0)" }
        )
        #expect(Tokens.countText(count: 3, limit: nil, labels: custom) == "3 Zeichen")
        #expect(Tokens.countText(count: 3, limit: 9, labels: custom) == "3 von 9")
        #expect(Tokens.countAccessibilityText(count: 3, limit: 9, labels: custom) == "noch 6")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchRichTextEditorStrings.count(2) == "2 characters")
        #expect(MonarchRichTextEditorStrings.remaining(-2) == "2 characters over the limit")
        #expect(MonarchRichTextEditorStrings.countOfLimit(1, limit: 2) == "1 / 2")
    }

    @Test("Documents over the limit are reported")
    func overLimit() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        #expect(MonarchRichTextEditor.isOverLimit(AttributedString("abc"), maxLength: 2))
        #expect(!MonarchRichTextEditor.isOverLimit(AttributedString("abc"), maxLength: nil))
    }
}
