import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private enum InstallSamples {
    static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
    static let snippets = [
        MonarchInstallSnippet(
            "HTML",
            code: "<script src=\"https://cdn.example.com/w.js\" async></script>",
            language: "html",
            description: "Paste before the closing head tag."
        ),
        MonarchInstallSnippet("npm", code: "npm install @example/widget", language: "bash"),
    ]
    static let locale = Locale(identifier: "en_US")
    static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        calendar.locale = locale
        return calendar
    }
}

enum MonarchInstallVerifyFixtures: ComponentFixtures {
    static let id = "install-verify"
    static let fixtures = [
        Fixture("waiting, both halves, share and test") {
            MonarchInstallVerify(
                snippets: InstallSamples.snippets,
                status: .waiting,
                targets: [MonarchInstallTarget("www.example.com"), MonarchInstallTarget("shop.example.com")],
                developerInstructions: "Add the widget tag to every page of www.example.com.",
                testURL: URL(string: "https://www.example.com/?test=1"),
                now: InstallSamples.now
            )
        },
        Fixture("connected, verify only") {
            MonarchInstallVerify(
                snippets: InstallSamples.snippets,
                status: .connected,
                targets: [
                    MonarchInstallTarget("www.example.com", lastSeen: InstallSamples.now.addingTimeInterval(-300)),
                    MonarchInstallTarget("shop.example.com"),
                ],
                part: .verify,
                now: InstallSamples.now
            )
        },
        Fixture("unavailable, disabled test with a hint") {
            MonarchInstallVerify(
                snippets: InstallSamples.snippets,
                status: .unavailable,
                testURL: URL(string: "https://www.example.com"),
                isTestDisabled: true,
                testHint: "Publish your site first.",
                part: .verify
            )
        },
        Fixture("install only, one snippet, a credential") {
            MonarchInstallVerify(
                snippets: [InstallSamples.snippets[1]],
                status: .waiting,
                part: .install
            ) {
                MonarchCopyField("Site key", value: "pk_live_51H8exampleKey")
            }
        },
        Fixture("long site names and custom labels") {
            MonarchInstallVerify(
                snippets: InstallSamples.snippets,
                status: .connected,
                connectedOn: ["a-very-long-subdomain-name.customer-example-domain.co.uk"],
                now: InstallSamples.now,
                labels: MonarchInstallVerifyLabels(installHeading: "Add the tracking code to your website")
            )
        },
    ]
}

@Suite("MonarchInstallVerify")
@MainActor
struct MonarchInstallVerifyTests {
    private let text = MonarchInstallVerifyText(labels: MonarchInstallVerifyLabels())

    @Test("Each status has its own tone and symbol, so colour is not the only signal")
    func appearance() {
        #expect(MonarchInstallVerifyTokens.tone(.waiting) == .neutral)
        #expect(MonarchInstallVerifyTokens.tone(.connected) == .success)
        #expect(MonarchInstallVerifyTokens.tone(.unavailable) == .neutral)
        #expect(MonarchInstallVerifyTokens.symbol(.waiting) == nil)
        #expect(MonarchInstallVerifyTokens.symbol(.connected) != nil)
        #expect(MonarchInstallVerifyTokens.symbol(.unavailable) != MonarchInstallVerifyTokens.symbol(.connected))
        #expect(MonarchInstallVerifyTokens.surface(.connected) != MonarchInstallVerifyTokens.surface(.waiting))
        #expect(MonarchInstallVerifyTokens.targetTone(nil) == .neutral)
        #expect(MonarchInstallVerifyTokens.targetTone(InstallSamples.now) == .success)
    }

    @Test("The connected title names the given sites, else the sites that reported")
    func hosts() {
        let targets = [
            MonarchInstallTarget("a.example.com", lastSeen: InstallSamples.now),
            MonarchInstallTarget("b.example.com"),
            MonarchInstallTarget("c.example.com", lastSeen: InstallSamples.now),
        ]
        #expect(
            MonarchInstallVerifyTokens.hosts(connectedOn: nil, targets: targets) == ["a.example.com", "c.example.com"]
        )
        #expect(MonarchInstallVerifyTokens.hosts(connectedOn: ["x.com"], targets: targets) == ["x.com"])
        #expect(
            text.title(.connected, hosts: ["a.example.com", "c.example.com"])
                == "Connected on a.example.com, c.example.com"
        )
        #expect(text.title(.connected, hosts: []) == "Connected")
    }

    @Test("Titles and texts per status")
    func statusText() {
        #expect(text.title(.waiting, hosts: []) == "Waiting for your site…")
        #expect(text.title(.unavailable, hosts: []) == "Live verification isn’t available")
        #expect(
            text.body(.waiting, targets: ["www.example.com"])
                == "Add the snippet and open any page of www.example.com. This updates by itself."
        )
        #expect(
            text.body(.waiting, targets: [])
                == "Add the snippet and open any page of your site. This updates by itself."
        )
        #expect(
            text.body(.connected, targets: []) == "The snippet loaded there. Try it out to see it working end to end."
        )
        #expect(
            text.body(.unavailable, targets: []) == "Open a page where you added the snippet and check that it appears."
        )
    }

    @Test("A site reads when it was seen, in the locale and calendar given")
    func seen() {
        let fiveMinutesAgo = InstallSamples.now.addingTimeInterval(-300)
        let seen = text.seen(
            fiveMinutesAgo,
            now: InstallSamples.now,
            locale: InstallSamples.locale,
            calendar: InstallSamples.calendar
        )
        #expect(seen == "seen 5 minutes ago")
        #expect(
            text.seen(nil, now: InstallSamples.now, locale: InstallSamples.locale, calendar: InstallSamples.calendar)
                == "not seen yet"
        )
        #expect(
            text.targetValue(
                nil,
                now: InstallSamples.now,
                locale: InstallSamples.locale,
                calendar: InstallSamples.calendar
            )
                == "Not seen"
        )
        #expect(
            text.targetValue(
                fiveMinutesAgo,
                now: InstallSamples.now,
                locale: InstallSamples.locale,
                calendar: InstallSamples.calendar
            )
                == "seen 5 minutes ago"
        )
    }

    @Test("Only http and https test pages are offered")
    func testURL() {
        #expect(MonarchInstallVerifyTokens.safeTestURL(URL(string: "https://www.example.com")) != nil)
        #expect(MonarchInstallVerifyTokens.safeTestURL(URL(string: "HTTP://www.example.com")) != nil)
        #expect(MonarchInstallVerifyTokens.safeTestURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(MonarchInstallVerifyTokens.safeTestURL(URL(string: "file:///etc/hosts")) == nil)
        #expect(MonarchInstallVerifyTokens.safeTestURL(URL(string: "https://")) == nil)
        #expect(MonarchInstallVerifyTokens.safeTestURL(nil) == nil)
    }

    @Test("onConnected runs once, on the first connected status")
    func reportOnce() {
        #expect(MonarchInstallVerifyTokens.shouldReportConnected(.connected, alreadyReported: false))
        #expect(!MonarchInstallVerifyTokens.shouldReportConnected(.connected, alreadyReported: true))
        #expect(!MonarchInstallVerifyTokens.shouldReportConnected(.waiting, alreadyReported: false))
        #expect(!MonarchInstallVerifyTokens.shouldReportConnected(.unavailable, alreadyReported: false))
    }

    @Test("Snippet ids default to the label")
    func snippetID() {
        #expect(MonarchInstallSnippet("HTML", code: "x").id == "HTML")
        #expect(MonarchInstallSnippet("HTML", code: "x", id: "html").id == "html")
        #expect(MonarchInstallTarget("www.example.com").id == "www.example.com")
    }

    @Test("Replacements win over the built-in strings")
    func overrides() {
        let custom = MonarchInstallVerifyText(
            labels: MonarchInstallVerifyLabels(
                waitingBody: { "Open \($0.joined(separator: " or "))." },
                connectedTitle: { "Live on \($0.count) sites" },
                seen: { "last ping \($0)" }
            )
        )
        #expect(custom.body(.waiting, targets: ["a", "b"]) == "Open a or b.")
        #expect(custom.title(.connected, hosts: ["a", "b"]) == "Live on 2 sites")
        #expect(
            custom.seen(
                InstallSamples.now.addingTimeInterval(-60),
                now: InstallSamples.now,
                locale: InstallSamples.locale,
                calendar: InstallSamples.calendar
            ) == "last ping 1 minute ago"
        )
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(text.installHeading == "Install")
        #expect(text.verifyHeading == "Verify")
        #expect(text.platforms == "Platform")
        #expect(text.shareInstructions == "Copy instructions for your developer")
        #expect(text.shareSubject == "Install instructions")
        #expect(text.targets == "Websites")
        #expect(text.openTest == "Open a test page")
        #expect(text.openTestHint == "Opens in Safari.")
    }
}
