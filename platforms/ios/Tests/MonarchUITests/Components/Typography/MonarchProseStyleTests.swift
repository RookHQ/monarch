import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// An article that uses every kind of block, for the fixtures.
private struct ProseFixtureArticle: View {
    let style: MonarchProseStyle
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchProse(style) {
            Text("Getting started").monarchProseBlock(.heading(2))
            Text(style.markdown("Read the [overview](https://example.com) first, then run `npm install`.", in: theme))
            Text("Create a project").monarchProseBlock(.listItem(marker: "•"))
            Text("Set up notifications").monarchProseBlock(.listItem(marker: "•"))
            Text("Email").monarchProseBlock(.listItem(marker: "◦", depth: 1))
            Text("Small steps every day add up.").monarchProseBlock(.quote)
            Text("const total = items.reduce((sum, i) => sum + i.price, 0);").monarchProseBlock(.code)
            Divider().monarchProseBlock(.rule)
            Text("Options").monarchProseBlock(.heading(3))
            Text("Check the file").monarchProseBlock(.listItem(marker: "1."))
            Text("Import it").monarchProseBlock(.listItem(marker: "2."))
            Text("Figure 1: the import screen.").monarchProseBlock(.caption)
        }
    }
}

enum MonarchProseStyleFixtures: ComponentFixtures {
    static let id = "prose"

    static let fixtures = [
        Fixture("article, small") {
            ProseFixtureArticle(style: MonarchProseStyle(size: .sm))
        },
        Fixture("article, medium") {
            ProseFixtureArticle(style: MonarchProseStyle())
        },
        Fixture("article, large, without the reading measure") {
            ProseFixtureArticle(style: MonarchProseStyle(size: .lg, limitsLineLength: false))
        },
        Fixture("every heading level, and levels out of range") {
            MonarchProse {
                ForEach(0..<8) { level in
                    Text("Heading level \(level)").monarchProseBlock(.heading(level))
                }
            }
        },
        Fixture("long words, deep nesting and a child without a kind") {
            MonarchProse(MonarchProseStyle(size: .sm, limitsLineLength: false)) {
                Text("Pneumonoultramicroscopicsilicovolcanoconiosis-and-a-very-long-identifier-without-spaces")
                Text("Level three").monarchProseBlock(.listItem(marker: "▪", depth: 3))
                Text("A quotation that is long enough to wrap onto several lines at the largest text size.")
                    .monarchProseBlock(.quote)
            }
        },
        Fixture("a block outside prose takes the default style") {
            Text("Options").monarchProseBlock(.heading(4))
        },
    ]
}

@Suite("MonarchProseStyle")
struct MonarchProseStyleTests {
    typealias Tokens = MonarchProseTokens

    private let theme = MonarchTheme.light

    // MARK: Type

    @Test("The base sizes grow from sm to lg and md is the body size")
    func baseSizes() {
        #expect(Tokens.baseSize(.sm) < Tokens.baseSize(.md))
        #expect(Tokens.baseSize(.md) < Tokens.baseSize(.lg))
        #expect(Tokens.baseSize(.md) == CGFloat(MonarchTokens.FontSize.base))
    }

    @Test("Headings get smaller from level 1 to 6, and levels are clamped")
    func headingScales() {
        let scales = (1...6).map { Tokens.scale(of: .heading($0)) }
        #expect(scales == scales.sorted(by: >))
        #expect(scales.first == 1.875)
        #expect(scales.last == 1)
        #expect(Tokens.scale(of: .heading(0)) == Tokens.scale(of: .heading(1)))
        #expect(Tokens.scale(of: .heading(9)) == Tokens.scale(of: .heading(6)))
        #expect(Tokens.clampedLevel(-3) == 1)
    }

    @Test("Code and captions are smaller than body text")
    func smallBlocks() {
        #expect(Tokens.scale(of: .code) < Tokens.scale(of: .paragraph))
        #expect(Tokens.scale(of: .caption) < Tokens.scale(of: .paragraph))
        #expect(Tokens.isMonospaced(.code))
        #expect(!Tokens.isMonospaced(.paragraph))
    }

    @Test("Font sizes scale with the prose size and with Dynamic Type")
    func fontSizes() {
        let reference = Tokens.referenceSize
        #expect(
            Tokens.fontSize(of: .paragraph, size: .md, scaledReference: reference)
                == CGFloat(MonarchTokens.FontSize.base)
        )
        #expect(
            Tokens.fontSize(of: .paragraph, size: .sm, scaledReference: reference) == CGFloat(MonarchTokens.FontSize.sm)
        )
        #expect(Tokens.fontSize(of: .heading(1), size: .md, scaledReference: reference) == 30)
        #expect(Tokens.fontSize(of: .paragraph, size: .md, scaledReference: reference * 2) == 32)
    }

    @Test("Small prose is set tighter; headings are tight or snug")
    func lineHeights() {
        #expect(Tokens.lineHeight(of: .paragraph, size: .sm) == CGFloat(MonarchTokens.LineHeight.normal))
        #expect(Tokens.lineHeight(of: .paragraph, size: .md) == CGFloat(MonarchTokens.LineHeight.relaxed))
        #expect(Tokens.lineHeight(of: .heading(1), size: .md) == CGFloat(MonarchTokens.LineHeight.tight))
        #expect(Tokens.lineHeight(of: .heading(4), size: .md) == CGFloat(MonarchTokens.LineHeight.snug))
        #expect(Tokens.lineSpacing(of: .heading(1), size: .md, fontSize: 30) == 0)
        #expect(Tokens.lineSpacing(of: .paragraph, size: .md, fontSize: 16) > 0)
    }

    @Test("Headings are semibold; level 1 is tracked tightest")
    func weightsAndTracking() {
        #expect(Tokens.weight(of: .heading(3)) == MonarchTokens.FontWeight.semibold)
        #expect(Tokens.weight(of: .paragraph) == MonarchTokens.FontWeight.regular)
        #expect(Tokens.tracking(of: .heading(1)) == CGFloat(MonarchTokens.LetterSpacing.tight))
        #expect(Tokens.tracking(of: .heading(2)) == CGFloat(MonarchTokens.LetterSpacing.snug))
        #expect(Tokens.tracking(of: .paragraph) == CGFloat(MonarchTokens.LetterSpacing.normal))
    }

    @Test("The reading measure is the web's 42 base sizes")
    func measure() {
        #expect(Tokens.measureWidth(size: .md, scaledReference: Tokens.referenceSize) == 672)
        #expect(Tokens.measureWidth(size: .sm, scaledReference: Tokens.referenceSize) == 588)
    }

    // MARK: Colour

    @Test("Quotations, captions and level 6 headings are muted")
    func colours() {
        #expect(Tokens.foreground(of: .paragraph) == \MonarchColorTokens.textDefault)
        #expect(Tokens.foreground(of: .quote) == \MonarchColorTokens.textMuted)
        #expect(Tokens.foreground(of: .caption) == \MonarchColorTokens.textMuted)
        #expect(Tokens.foreground(of: .heading(5)) == \MonarchColorTokens.textDefault)
        #expect(Tokens.foreground(of: .heading(6)) == \MonarchColorTokens.textMuted)
        #expect(Tokens.foreground(of: .rule) == \MonarchColorTokens.borderDefault)
    }

    // MARK: Spacing

    @Test("Blocks are spaced as on the web")
    func spacing() {
        #expect(Tokens.spacing(after: .paragraph, before: .paragraph) == 1.25)
        #expect(Tokens.spacing(after: .paragraph, before: .heading(2)) == 1.75)
        #expect(Tokens.spacing(after: .heading(2), before: .paragraph) == 0.5)
        #expect(Tokens.spacing(after: .heading(2), before: .heading(3)) == 1.75)
        #expect(Tokens.spacing(after: .listItem(marker: "•"), before: .listItem(marker: "•", depth: 1)) == 0.375)
        #expect(Tokens.spacing(after: .listItem(marker: "•"), before: .paragraph) == 1.25)
        #expect(Tokens.spacing(after: .rule, before: .heading(2)) == 2)
        #expect(Tokens.spacing(after: .code, before: .rule) == 2)
    }

    @MainActor
    @Test("The layout puts less space under a heading than between paragraphs")
    func layoutSpacing() {
        let paragraphs = Host.layout(
            MonarchProse {
                Text("A")
                Text("B")
            }
        )
        let heading = Host.layout(
            MonarchProse {
                Text("A").monarchProseBlock(.heading(6))
                Text("B").monarchProseBlock(.paragraph)
            }
        )
        let single = Host.layout(MonarchProse { Text("A") })
        #expect(paragraphs.height > single.height * 2)
        #expect(heading.height < paragraphs.height)
    }

    // MARK: Accessibility

    @Test("Heading levels map to VoiceOver heading levels")
    func headingLevels() {
        #expect(Tokens.accessibilityLevel(1) == .h1)
        #expect(Tokens.accessibilityLevel(3) == .h3)
        #expect(Tokens.accessibilityLevel(6) == .h6)
        #expect(Tokens.accessibilityLevel(12) == .h6)
        #expect(Tokens.accessibilityLevel(0) == .h1)
    }

    @Test("Numbered markers are read; bullets are not")
    func markers() {
        #expect(Tokens.isMarkerSpoken("1."))
        #expect(Tokens.isMarkerSpoken("b)"))
        #expect(!Tokens.isMarkerSpoken("•"))
        #expect(!Tokens.isMarkerSpoken("◦"))
        #expect(!Tokens.isMarkerSpoken("—"))
    }

    // MARK: Inline

    @Test("Links take the link colour and an underline")
    func links() throws {
        let text = MonarchProseStyle().markdown("Read the [overview](https://example.com).", in: theme)
        let link = try #require(text.runs.first { $0.link != nil })
        #expect(link.swiftUI.foregroundColor == theme.colors.textLink)
        #expect(link.swiftUI.underlineStyle != nil)
        let plain = try #require(text.runs.first { $0.link == nil })
        #expect(plain.swiftUI.underlineStyle == nil)
        #expect(plain.swiftUI.foregroundColor == nil)
    }

    @Test("Code spans take the inline code look", arguments: [MonarchTheme.light, .dark, .highContrastDark])
    func code(theme: MonarchTheme) throws {
        let text = MonarchProseStyle().markdown("Run `make` now.", in: theme)
        let code = try #require(text.runs.first { $0.inlinePresentationIntent?.contains(.code) == true })
        #expect(code.swiftUI.backgroundColor == theme.colors.bgMuted)
        #expect(String(text.characters) == "Run make now.")
    }

    @Test("Markdown marks are removed and the size does not change the text")
    func markdown() {
        let small = MonarchProseStyle(size: .sm).markdown("**Bold** and _soft_", in: theme)
        let large = MonarchProseStyle(size: .lg).markdown("**Bold** and _soft_", in: theme)
        #expect(String(small.characters) == "Bold and soft")
        #expect(small == large)
    }

    @Test("The default style is medium with the reading measure")
    func defaults() {
        let style = MonarchProseStyle()
        #expect(style.size == .md)
        #expect(style.limitsLineLength)
        #expect(style != MonarchProseStyle(size: .md, limitsLineLength: false))
    }
}
