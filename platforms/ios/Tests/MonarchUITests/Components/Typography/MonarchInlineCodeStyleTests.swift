import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

#if canImport(AppKit)
    import AppKit
#elseif canImport(UIKit)
    import UIKit
#endif

enum MonarchInlineCodeStyleFixtures: ComponentFixtures {
    static let id = "inline-code"
    static let fixtures = [
        Fixture("both variants in running text") {
            InlineCodeFixture(markdown: "Run `swift build` in `platforms/ios` before you hand over.", role: .body)
        },
        Fixture("in a caption and in a heading") {
            VStack(alignment: .leading) {
                InlineCodeFixture(markdown: "Set `isLoading` to show progress.", role: .caption)
                InlineCodeFixture(markdown: "The `monarchTheme` modifier", role: .heading)
            }
        },
        Fixture("a long token") {
            InlineCodeFixture(
                markdown: "Checksum `9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08` matches.",
                role: .body
            )
        },
        Fixture("with a custom code font family") {
            InlineCodeFixture(markdown: "Call `refresh()` again.", role: .body, codeFontFamily: "Menlo")
        },
        Fixture("appended to other text") {
            InlineCodeAppendedFixture()
        },
    ]
}

private struct InlineCodeFixture: View {
    let markdown: String
    let role: MonarchTextStyle
    var codeFontFamily: String?

    @Environment(\.monarchTheme) private var environmentTheme

    private var theme: MonarchTheme {
        guard let codeFontFamily else { return environmentTheme }
        return MonarchTheme(
            tokens: environmentTheme.tokens,
            typography: MonarchTypography(codeFontFamily: codeFontFamily)
        )
    }

    var body: some View {
        VStack(alignment: .leading) {
            ForEach(MonarchInlineCodeVariant.allCases, id: \.self) { variant in
                Text(MonarchInlineCodeStyle(variant: variant).markdown(markdown, in: theme, textStyle: role))
                    .monarchTextStyle(role)
            }
        }
    }
}

private struct InlineCodeAppendedFixture: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text(AttributedString("Exit code ") + MonarchInlineCodeStyle.subtle.attributedString("127", in: theme))
            .monarchTextStyle(.body)
    }
}

@Suite("MonarchInlineCodeStyle")
struct MonarchInlineCodeStyleTests {
    private let theme = MonarchTheme.light

    @Test("Subtle has a muted background, plain has none")
    func backgroundRole() {
        #expect(MonarchInlineCodeTokens.background(variant: .subtle) == \MonarchColorTokens.bgMuted)
        #expect(MonarchInlineCodeTokens.background(variant: .plain) == nil)
        #expect(MonarchInlineCodeTokens.foreground == \MonarchColorTokens.textDefault)
    }

    @Test("The default style is subtle")
    func defaultVariant() {
        #expect(MonarchInlineCodeStyle() == .subtle)
        #expect(MonarchInlineCodeStyle.subtle != .plain)
    }

    @Test(
        "A code run has the code intent and the colours of the theme",
        arguments: [MonarchTheme.light, .dark, .highContrastLight, .highContrastDark]
    )
    func attributes(theme: MonarchTheme) {
        let subtle = MonarchInlineCodeStyle.subtle.attributes(in: theme)
        #expect(subtle.inlinePresentationIntent == .code)
        #expect(subtle.swiftUI.foregroundColor == theme.colors.textDefault)
        #expect(subtle.swiftUI.backgroundColor == theme.colors.bgMuted)
        #expect(subtle.swiftUI.font == nil)

        let plain = MonarchInlineCodeStyle.plain.attributes(in: theme)
        #expect(plain.inlinePresentationIntent == .code)
        #expect(plain.swiftUI.foregroundColor == theme.colors.textDefault)
        #expect(plain.swiftUI.backgroundColor == nil)
    }

    @Test("A theme with its own code font family sets that font at the size of the role")
    func customFamily() {
        let custom = MonarchTheme(tokens: theme.tokens, typography: MonarchTypography(codeFontFamily: "Menlo"))
        let body = MonarchInlineCodeStyle.subtle.attributes(in: custom, textStyle: .body)
        let caption = MonarchInlineCodeStyle.subtle.attributes(in: custom, textStyle: .caption)
        #expect(body.swiftUI.font == Font.custom("Menlo", size: MonarchTextStyle.body.spec.size, relativeTo: .body))
        #expect(
            caption.swiftUI.font
                == Font.custom("Menlo", size: MonarchTextStyle.caption.spec.size, relativeTo: .caption)
        )
        #expect(body.swiftUI.font != caption.swiftUI.font)
    }

    @Test("A code fragment keeps its characters and is one styled run")
    func fragment() {
        let code = MonarchInlineCodeStyle.subtle.attributedString("a < b && c", in: theme)
        #expect(String(code.characters) == "a < b && c")
        #expect(code.runs.count == 1)
        #expect(code.runs.first?.inlinePresentationIntent == .code)
    }

    @Test("Markdown: only the code spans are styled, and the backticks are removed")
    func markdown() {
        let text = MonarchInlineCodeStyle.subtle.markdown("Run `swift build` in **your** copy of `ios`.", in: theme)
        #expect(String(text.characters) == "Run swift build in your copy of ios.")

        var styled: [String] = []
        for run in text.runs {
            let fragment = String(text[run.range].characters)
            if run.swiftUI.backgroundColor == theme.colors.bgMuted {
                styled.append(fragment)
            } else {
                #expect(run.swiftUI.backgroundColor == nil)
                #expect(run.swiftUI.foregroundColor == nil)
            }
        }
        #expect(styled == ["swift build", "ios"])
    }

    @Test("Markdown keeps white space and shows unmatched backticks as they are")
    func markdownEdges() {
        let spaced = MonarchInlineCodeStyle.plain.markdown("a  b\nc", in: theme)
        #expect(String(spaced.characters) == "a  b\nc")

        let unmatched = MonarchInlineCodeStyle.subtle.markdown("5` tall", in: theme)
        #expect(String(unmatched.characters) == "5` tall")
        #expect(unmatched.runs.allSatisfy { $0.swiftUI.backgroundColor == nil })

        #expect(String(MonarchInlineCodeStyle.subtle.markdown("", in: theme).characters).isEmpty)
    }

    @Test("Applying the style leaves text without code unchanged and keeps other attributes of code runs")
    func applied() {
        let plainText = AttributedString("No code here")
        #expect(MonarchInlineCodeStyle.subtle.applied(to: plainText, in: theme) == plainText)

        var code = AttributedString("x")
        code.inlinePresentationIntent = [.code, .stronglyEmphasized]
        let result = MonarchInlineCodeStyle.subtle.applied(to: AttributedString("Set ") + code, in: theme)
        let run = result.runs.first { $0.swiftUI.backgroundColor != nil }
        #expect(run.map { String(result[$0.range].characters) } == "x")
        #expect(result.runs.count == 2)
    }

    @MainActor
    @Test("SwiftUI draws a code run in a monospaced face: narrow letters take more room")
    func codeRunIsMonospaced() {
        let letters = String(repeating: "i", count: 24)
        let plain = InlineCodeMeasure.idealWidth(Text(letters).monarchTextStyle(.body))
        let code = InlineCodeMeasure.idealWidth(
            Text(MonarchInlineCodeStyle.plain.attributedString(letters, in: theme)).monarchTextStyle(.body)
        )
        #expect(plain > 0)
        #expect(code > plain * 1.5)
    }
}

/// Measures the width a view asks for when nothing constrains it.
@MainActor
private enum InlineCodeMeasure {
    static func idealWidth(_ view: some View) -> CGFloat {
        let content = view.fixedSize()
        #if canImport(AppKit)
            return NSHostingView(rootView: content).fittingSize.width
        #elseif canImport(UIKit)
            let unbounded = CGSize(width: CGFloat.greatestFiniteMagnitude, height: .greatestFiniteMagnitude)
            return UIHostingController(rootView: content).sizeThatFits(in: unbounded).width
        #endif
    }
}
