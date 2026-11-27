import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchInlineErrorRetryFixtures: ComponentFixtures {
    static let id = "inline-error-retry"
    static let fixtures = [
        Fixture("inline, danger") {
            MonarchInlineErrorRetry("Your earnings didn't load.") {}
        },
        Fixture("banner, warning, long message") {
            MonarchInlineErrorRetry(
                "Showing data from 10 minutes ago because the latest figures could not be loaded.",
                tone: .warning,
                variant: .banner
            ) {}
        },
        Fixture("retrying with custom strings and icon") {
            MonarchInlineErrorRetry(
                "Comments didn't load.",
                systemImage: "bubble.left.and.exclamationmark.bubble.right",
                variant: .banner,
                isRetrying: true,
                toneLabel: "Problem",
                retryLabel: "Reload",
                retryingLabel: "Reloading"
            ) {}
        },
        Fixture("every tone in both variants") {
            VStack {
                ForEach(MonarchInlineErrorRetryVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchInlineErrorRetry("Didn't load.", tone: tone, variant: variant) {}
                    }
                }
            }
        },
    ]
}

@Suite("MonarchInlineErrorRetry")
struct MonarchInlineErrorRetryTests {
    @Test("The inline variant has no surface and the icon takes the tone")
    func inlineAppearance() {
        let inline = MonarchInlineErrorRetryTokens.appearance(tone: .danger, variant: .inline)
        #expect(inline.background == nil)
        #expect(inline.border == nil)
        #expect(inline.icon == \MonarchColorTokens.dangerStrong)
        #expect(inline.text == \MonarchColorTokens.textDefault)
    }

    @Test("The banner variant is a tinted, bordered strip in the tone")
    func bannerAppearance() {
        let warning = MonarchInlineErrorRetryTokens.appearance(tone: .warning, variant: .banner)
        #expect(warning.background == \MonarchColorTokens.warningSubtle)
        #expect(warning.border == \MonarchColorTokens.warningBorder)
        #expect(warning.icon == \MonarchColorTokens.warningStrong)

        let neutral = MonarchInlineErrorRetryTokens.appearance(tone: .neutral, variant: .banner)
        #expect(neutral.background == \MonarchColorTokens.bgSubtle)
        #expect(neutral.border == \MonarchColorTokens.borderDefault)
    }

    @Test("The icon is the tone's unless the app gives one")
    func symbol() {
        #expect(
            MonarchInlineErrorRetryTokens.symbol(tone: .danger, override: nil)
                == "exclamationmark.triangle"
        )
        #expect(MonarchInlineErrorRetryTokens.symbol(tone: .danger, override: "wifi.slash") == "wifi.slash")
        #expect(
            MonarchInlineErrorRetryTokens.symbol(tone: .warning, override: "")
                == MonarchTonePalette.palette(for: .warning).symbol
        )
    }

    @Test("Only the start of a retry is announced")
    func announcement() {
        #expect(MonarchInlineErrorRetryTokens.announcesRetry(wasRetrying: false, isRetrying: true))
        #expect(!MonarchInlineErrorRetryTokens.announcesRetry(wasRetrying: true, isRetrying: false))
        #expect(!MonarchInlineErrorRetryTokens.announcesRetry(wasRetrying: true, isRetrying: true))
        #expect(!MonarchInlineErrorRetryTokens.announcesRetry(wasRetrying: false, isRetrying: false))
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchInlineErrorRetryStrings.retry == "Try again")
        #expect(MonarchInlineErrorRetryStrings.retrying == "Retrying")
    }

    @Test("A short message shares a line with the button and a long one stacks")
    @MainActor
    func layout() {
        let short = Host.layout(MonarchInlineErrorRetry("Didn't load.") {})
        let long = Host.layout(
            MonarchInlineErrorRetry(String(repeating: "The latest figures could not be loaded. ", count: 4)) {}
        )
        #expect(long.height > short.height)
        #expect(short.width == Host.width)
    }
}
