import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private enum SourcePreviewSamples {
    static let notes = MonarchAiSourcePart(
        id: "s1",
        title: "Quarterly planning notes",
        url: "https://www.example.com/notes/q3",
        kind: .doc,
        product: "Docs"
    )
    static let record = MonarchAiSourcePart(id: "s2", title: "Acme Ltd", url: "javascript:alert(1)", kind: .record)
    static let content = """
        Planning for the third quarter starts with the numbers.

        Revenue grew 12% against the same
        quarter last year, led by the enterprise plan.

        Hiring stays flat until the new year.
        """
}

enum MonarchAiSourcePreviewFixtures: ComponentFixtures {
    static let id = "ai-source-preview"
    static let fixtures = [
        Fixture("content with the quote highlighted") {
            MonarchAiSourcePreview(
                SourcePreviewSamples.notes,
                quote: "revenue grew 12% against the same quarter",
                content: SourcePreviewSamples.content
            )
        },
        Fixture("quote not in the content") {
            MonarchAiSourcePreview(
                SourcePreviewSamples.notes,
                quote: "Margins improved.",
                content: SourcePreviewSamples.content
            )
        },
        Fixture("quote only, unsafe address, custom labels") {
            MonarchAiSourcePreview(
                SourcePreviewSamples.record,
                quote: "Renewal due in March.",
                labels: MonarchAiSourcePreviewLabels(quoted: "Cited text", close: "Done")
            )
        },
        Fixture("nothing to preview") {
            MonarchAiSourcePreview(MonarchAiSourcePart(id: "s3", title: "Untitled", kind: .file))
        },
    ]
}

@Suite("MonarchAiSourcePreview")
struct MonarchAiSourcePreviewTests {
    @Test("A quote is found ignoring case and differences in whitespace")
    func quoteRange() {
        let text = "Revenue grew 12% against the same\n   quarter last year."
        let range = MonarchAiSourcePreviewTokens.quoteRange(
            in: text,
            quote: "revenue  grew 12% against the same quarter"
        )
        #expect(range.map { String(text[$0]) } == "Revenue grew 12% against the same\n   quarter")
        #expect(MonarchAiSourcePreviewTokens.quoteRange(in: text, quote: "costs fell") == nil)
        #expect(MonarchAiSourcePreviewTokens.quoteRange(in: text, quote: "   ") == nil)
        // Characters with a meaning in patterns are matched literally.
        #expect(MonarchAiSourcePreviewTokens.quoteRange(in: "a (b) c.*", quote: "(b) c.*") != nil)
        #expect(MonarchAiSourcePreviewTokens.quoteRange(in: "abc", quote: "a.c") == nil)
    }

    @Test("Content splits into paragraphs on blank lines, and the quote is marked")
    func paragraphs() {
        let paragraphs = MonarchAiSourcePreviewTokens.paragraphs(
            SourcePreviewSamples.content,
            quote: "revenue grew 12%"
        )
        #expect(paragraphs.count == 3)
        #expect(paragraphs.map(\.index) == [0, 1, 2])
        #expect(!paragraphs[0].isMarked)
        #expect(paragraphs[0].before == "Planning for the third quarter starts with the numbers.")
        #expect(paragraphs[1].before == "")
        #expect(paragraphs[1].mark == "Revenue grew 12%")
        #expect(paragraphs[1].after.hasPrefix(" against the same\nquarter"))
        #expect(!paragraphs[2].isMarked)
    }

    @Test("A quote that spans a paragraph break is marked in each paragraph it covers")
    func spanningQuote() {
        let paragraphs = MonarchAiSourcePreviewTokens.paragraphs("One two.\n\nThree four.", quote: "two. three")
        #expect(paragraphs.count == 2)
        #expect(paragraphs[0].mark == "two.")
        #expect(paragraphs[1].mark == "Three")
        #expect(paragraphs[1].after == " four.")
    }

    @Test("Blank paragraphs are dropped and no quote marks nothing")
    func blanks() {
        let paragraphs = MonarchAiSourcePreviewTokens.paragraphs("\n\nFirst.\n\n   \n\nSecond.\n\n", quote: nil)
        #expect(paragraphs.map(\.before) == ["First.", "Second."])
        #expect(paragraphs.allSatisfy { !$0.isMarked })
        #expect(MonarchAiSourcePreviewTokens.paragraphs("", quote: "x").isEmpty)
    }

    @Test("The quote has its own block when it is not in the content")
    func quoteBlock() {
        #expect(MonarchAiSourcePreviewTokens.showsQuoteBlock(quote: "Margins", content: nil))
        #expect(MonarchAiSourcePreviewTokens.showsQuoteBlock(quote: "Margins", content: "Revenue grew."))
        #expect(!MonarchAiSourcePreviewTokens.showsQuoteBlock(quote: "revenue", content: "Revenue grew."))
        #expect(!MonarchAiSourcePreviewTokens.showsQuoteBlock(quote: nil, content: "Revenue grew."))
        #expect(!MonarchAiSourcePreviewTokens.showsQuoteBlock(quote: " ", content: nil))
    }

    @Test("There is nothing to preview only without content and without a quote")
    func empty() {
        #expect(MonarchAiSourcePreviewTokens.isEmpty(quote: nil, content: nil))
        #expect(MonarchAiSourcePreviewTokens.isEmpty(quote: "  ", content: ""))
        #expect(!MonarchAiSourcePreviewTokens.isEmpty(quote: "x", content: nil))
        #expect(!MonarchAiSourcePreviewTokens.isEmpty(quote: nil, content: "x"))
    }

    @Test("The highlight is a fill and an underline in the AI roles")
    func colours() {
        #expect(MonarchAiSourcePreviewTokens.markBackground == \.aiSubtle)
        #expect(MonarchAiSourcePreviewTokens.markUnderline == \.aiCitation)
        #expect(MonarchAiSourcePreviewTokens.markBackground != MonarchAiSourcePreviewTokens.markUnderline)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiSourcePreviewStrings.quoted == "Quoted passage")
        #expect(MonarchAiSourcePreviewStrings.openOriginal == "Open original")
        #expect(MonarchAiSourcePreviewStrings.noPreview == "No preview is available for this source.")
        #expect(MonarchAiSourcePreviewStrings.close == "Close")
    }
}
