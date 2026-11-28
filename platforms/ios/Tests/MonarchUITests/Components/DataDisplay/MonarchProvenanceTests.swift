import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchProvenanceFixtures: ComponentFixtures {
    static let id = "provenance"
    static let fixtures = [
        Fixture("chip and inline") {
            VStack(alignment: .leading) {
                ForEach(MonarchProvenanceVariant.allCases, id: \.self) { variant in
                    MonarchProvenance(
                        "Registry",
                        checkedAt: Date(timeIntervalSince1970: 1_800_000_000).addingTimeInterval(-7200),
                        variant: variant
                    )
                }
            }
        },
        Fixture("icon, no time") {
            MonarchProvenance("Entered by hand", systemImage: "hand.raised")
        },
        Fixture("link, long source") {
            MonarchProvenance(
                "Companies House register of overseas entities and their beneficial owners",
                checkedAt: Date(timeIntervalSince1970: 1_800_000_000).addingTimeInterval(-300),
                systemImage: "building.columns",
                url: URL(string: "https://example.com/company/123")
            )
        },
        Fixture("empty source draws nothing") {
            MonarchProvenance("", checkedAt: Date(timeIntervalSince1970: 1_800_000_000))
        },
    ]
}

@Suite("MonarchProvenance")
struct MonarchProvenanceTests {
    private let now = Date(timeIntervalSince1970: 1_800_000_000)

    @Test("The source is a link colour only when it opens a URL")
    func colours() {
        #expect(MonarchProvenanceTokens.source(isLink: true) == \MonarchColorTokens.textLink)
        #expect(MonarchProvenanceTokens.source(isLink: false) == \MonarchColorTokens.textMuted)
        #expect(MonarchProvenanceTokens.text == \MonarchColorTokens.textMuted)
        #expect(MonarchProvenanceTokens.chipFill == \MonarchColorTokens.bgSubtle)
        #expect(MonarchProvenanceTokens.chipBorder == \MonarchColorTokens.borderSubtle)
    }

    @Test("Only http, https and mailto URLs are opened")
    func safeURL() {
        #expect(MonarchProvenanceTokens.safeURL(URL(string: "https://example.com")) != nil)
        #expect(MonarchProvenanceTokens.safeURL(URL(string: "HTTP://example.com")) != nil)
        #expect(MonarchProvenanceTokens.safeURL(URL(string: "mailto:help@example.com")) != nil)
        #expect(MonarchProvenanceTokens.safeURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(MonarchProvenanceTokens.safeURL(URL(string: "file:///etc/hosts")) == nil)
        #expect(MonarchProvenanceTokens.safeURL(nil) == nil)
    }

    @Test("Nothing is drawn without a source")
    func visibility() {
        #expect(MonarchProvenanceTokens.isVisible(source: "Registry"))
        #expect(!MonarchProvenanceTokens.isVisible(source: ""))
        #expect(!MonarchProvenanceTokens.isVisible(source: "   "))
    }

    @Test("VoiceOver reads the source and when it was checked")
    func accessibilityLabel() {
        let time = MonarchProvenanceTokens.relativeText(
            now.addingTimeInterval(-7200),
            now: now,
            locale: Locale(identifier: "en_US")
        )
        #expect(time == "2 hours ago")
        let defaults = MonarchProvenanceLabels()
        #expect(
            MonarchProvenanceTokens.accessibilityLabel(source: "Registry", time: time, labels: defaults)
                == "Source: Registry, checked 2 hours ago"
        )
        #expect(
            MonarchProvenanceTokens.accessibilityLabel(source: "Registry", time: nil, labels: defaults)
                == "Source: Registry"
        )
        let labels = MonarchProvenanceLabels(source: { "From \($0)" }, checked: { "seen \($0)" })
        #expect(
            MonarchProvenanceTokens.accessibilityLabel(source: "Registry", time: "now", labels: labels)
                == "From Registry, seen now"
        )
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchProvenanceStrings.source("A") == "Source: A")
        #expect(MonarchProvenanceStrings.checked("now") == "checked now")
    }

    @Test("A link is at least 44 points high")
    @MainActor
    func linkTarget() {
        let size = Host.layout(MonarchProvenance("Registry", url: URL(string: "https://example.com")))
        #expect(size.height >= MonarchAccessibility.minimumHitTarget)
    }
}
