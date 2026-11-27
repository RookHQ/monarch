import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCopyButtonFixtures: ComponentFixtures {
    static let id = "copy-button"
    static let fixtures = [
        Fixture("variants and sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                        MonarchCopyButton(value: "sk_test", variant: variant, size: size)
                    }
                }
            }
        },
        Fixture("icon only, custom title and symbol, disabled") {
            VStack(alignment: .leading) {
                MonarchCopyButton(value: "sk_test", target: "API key", variant: .ghost, isIconOnly: true)
                MonarchCopyButton(
                    "Copy link",
                    value: "https://example.com",
                    systemImage: "link",
                    copiedTitle: "Link copied"
                )
                MonarchCopyButton(value: "sk_test").disabled(true)
                MonarchCopyButton(
                    "Copy the whole configuration file to the clipboard",
                    value: "config",
                    resetAfter: .zero
                )
            }
        },
    ]
}

@Suite("MonarchCopyButton")
struct MonarchCopyButtonTests {
    @Test("The icon is a checkmark after a copy, and a custom icon replaces only the idle one")
    func symbol() {
        #expect(MonarchCopyButtonTokens.symbol(isCopied: false, custom: nil) == "doc.on.doc")
        #expect(MonarchCopyButtonTokens.symbol(isCopied: false, custom: "link") == "link")
        #expect(MonarchCopyButtonTokens.symbol(isCopied: true, custom: nil) == "checkmark")
        #expect(MonarchCopyButtonTokens.symbol(isCopied: true, custom: "link") == "checkmark")
    }

    @Test("VoiceOver reads the title, then what is copied")
    func accessibilityLabel() {
        #expect(MonarchCopyButtonTokens.accessibilityLabel(title: "Copy", target: nil) == "Copy")
        #expect(MonarchCopyButtonTokens.accessibilityLabel(title: "Copy", target: "") == "Copy")
        #expect(MonarchCopyButtonTokens.accessibilityLabel(title: "Copy", target: "webhook URL") == "Copy webhook URL")
        #expect(
            MonarchCopyButtonTokens.accessibilityLabel(title: "Copied", target: "webhook URL") == "Copied webhook URL"
        )
    }

    @Test("The copied state ends by itself only after a copy and with a duration above zero")
    func resets() {
        #expect(!MonarchCopyButtonTokens.resets(copyCount: 0, after: .seconds(2)))
        #expect(MonarchCopyButtonTokens.resets(copyCount: 1, after: .seconds(2)))
        #expect(MonarchCopyButtonTokens.resets(copyCount: 3, after: .milliseconds(1)))
        #expect(!MonarchCopyButtonTokens.resets(copyCount: 1, after: .zero))
    }

    @Test("The titles and the announcement come from the component's strings table")
    func strings() {
        #expect(MonarchCopyButtonStrings.copy == "Copy")
        #expect(MonarchCopyButtonStrings.copied == "Copied")
        #expect(MonarchCopyButtonStrings.announcement == "Copied to clipboard")
    }

    #if os(iOS)
        @Test("An icon-only copy button keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let button = MonarchCopyButton(value: "x", size: .sm, isIconOnly: true).fixedSize()
            let size = Host.layout(button)
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

@Suite("MonarchCopyButton sensitive")
@MainActor
struct MonarchCopyButtonSensitiveTests {
    @Test("Sensitive text expires after two minutes")
    func lifetime() {
        #expect(MonarchCopyButtonPasteboard.sensitiveLifetime == 120)
    }

    #if canImport(UIKit)
        @Test("Sensitive text stays on this device and expires; other text has no options")
        func options() {
            let now = Date(timeIntervalSinceReferenceDate: 0)
            let options = MonarchCopyButtonPasteboard.options(isSensitive: true, now: now)
            #expect(options[.localOnly] as? Bool == true)
            #expect(options[.expirationDate] as? Date == now.addingTimeInterval(120))
            #expect(MonarchCopyButtonPasteboard.options(isSensitive: false, now: now).isEmpty)
        }
    #endif

    @Test("A sensitive copy button lays out like any other")
    func layout() {
        #expect(Host.layout(MonarchCopyButton(value: "sk_live", target: "secret key", isSensitive: true)).height > 0)
    }
}
