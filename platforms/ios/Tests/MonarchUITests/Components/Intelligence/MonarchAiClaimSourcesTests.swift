import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let calendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = .gmt
    return calendar
}()

private let sources = [
    MonarchAiClaimSource(
        id: "s1",
        title: "Q3 report",
        url: "https://example.com/q3",
        kind: .doc,
        published: Date(timeIntervalSince1970: 1_791_590_400)
    ),
    MonarchAiClaimSource(MonarchAiSourcePart(id: "s2", title: "Analyst note", url: "javascript:alert(1)", kind: .web)),
]

private let claims = [
    MonarchAiClaim(
        id: "c1",
        text: "Revenue grew 12% in the third quarter.",
        support: .supported,
        evidence: [
            MonarchAiClaimEvidence(
                id: "e1",
                sourceID: "s1",
                quote: "Third-quarter revenue rose 12% year on year.",
                location: "Page 4",
                stance: .supports
            )
        ]
    ),
    MonarchAiClaim(
        id: "c2",
        text: "Margins improved in every region.",
        support: .conflicting,
        evidence: [
            MonarchAiClaimEvidence(
                id: "e2",
                sourceID: "s1",
                quote: "Margins rose in Europe and Asia.",
                stance: .supports
            ),
            MonarchAiClaimEvidence(
                id: "e3",
                sourceID: "s2",
                quote: "North American margins fell.",
                stance: .contradicts
            ),
        ]
    ),
    MonarchAiClaim(id: "c3", text: "Churn halved.", support: .unsupported),
    MonarchAiClaim(
        id: "c4",
        text: "Headcount stayed flat.",
        support: .partial,
        evidence: [MonarchAiClaimEvidence(id: "e4", sourceID: "missing", quote: "Hiring paused in July.")]
    ),
    MonarchAiClaim(id: "c5", text: "The new plan launches in March.", support: .unchecked),
]

enum MonarchAiClaimSourcesFixtures: ComponentFixtures {
    static let id = "ai-claim-sources"
    static let fixtures = [
        Fixture("every support level, open, with actions") {
            MonarchAiClaimSources(
                claims,
                sources: sources,
                expandedIDs: .constant(Set(claims.map(\.id))),
                calendar: calendar,
                onOpenSource: { _, _ in },
                onFlag: { _ in },
                onVerify: { _ in }
            )
        },
        Fixture("collapsed") {
            MonarchAiClaimSources(claims, sources: sources)
        },
        Fixture("needs checking filter") {
            MonarchAiClaimSources(claims, sources: sources, filter: .constant(.needsChecking))
        },
        Fixture("nothing needs checking") {
            MonarchAiClaimSources([claims[0]], sources: sources, filter: .constant(.needsChecking))
        },
        Fixture("no claims") {
            MonarchAiClaimSources([], sources: [])
        },
    ]
}

@Suite("MonarchAiClaimSources")
struct MonarchAiClaimSourcesTests {
    private let locale = Locale(identifier: "en_US")

    @Test("Claims are counted by support level")
    func counts() {
        let counts = MonarchAiClaimCounts.of(claims)
        #expect(counts.total == 5)
        #expect(counts.needsChecking == 4)
        for support in MonarchAiClaimSupport.allCases {
            #expect(counts.count(support) == 1)
        }
        #expect(!claims[0].needsChecking)
        #expect(claims[4].needsChecking)
    }

    @Test("The filter keeps the claims that need checking, in order")
    func filter() {
        #expect(MonarchAiClaimSourcesTokens.shown(claims, filter: .all).count == 5)
        #expect(
            MonarchAiClaimSourcesTokens.shown(claims, filter: .needsChecking).map(\.id) == ["c2", "c3", "c4", "c5"]
        )
        #expect(!MonarchAiClaimSourcesTokens.showsFilter(MonarchAiClaimCounts.of([])))
    }

    @Test("Each support level has its own icon and a tone")
    func support() {
        let symbols = MonarchAiClaimSupport.allCases.map(MonarchAiClaimSourcesTokens.symbol)
        #expect(Set(symbols).count == MonarchAiClaimSupport.allCases.count)
        #expect(MonarchAiClaimSourcesTokens.tone(MonarchAiClaimSupport.supported) == .success)
        #expect(MonarchAiClaimSourcesTokens.tone(MonarchAiClaimSupport.unsupported) == .danger)
        #expect(MonarchAiClaimSourcesTokens.symbolColor(.unchecked) == \MonarchColorTokens.textMuted)
        #expect(MonarchAiClaimSourcesTokens.symbolColor(.supported) == \MonarchColorTokens.successStrong)
        #expect(MonarchAiClaimSourcesTokens.tone(MonarchAiClaimEvidenceStance.contradicts) == .danger)
    }

    @Test("A passage finds its source, and its detail joins what is known")
    func sourceDetail() {
        let evidence = claims[0].evidence[0]
        let source = MonarchAiClaimSourcesTokens.source(evidence, in: sources)
        #expect(source?.title == "Q3 report")
        let detail = MonarchAiClaimSourcesTokens.sourceDetail(
            source,
            evidence: evidence,
            kind: { MonarchAiSourceListStrings.kind($0) },
            published: { MonarchAiClaimSourcesStrings.published($0) },
            locale: locale,
            calendar: calendar
        )
        #expect(detail.hasPrefix("\(MonarchAiSourceListStrings.kind(.doc)) · Page 4 · Published "))
        #expect(detail.hasSuffix("2026"))
        #expect(MonarchAiClaimSourcesTokens.source(claims[3].evidence[0], in: sources) == nil)
    }

    @Test("Only a handler or a safe address opens a source")
    func open() {
        #expect(MonarchAiClaimSourcesTokens.canOpen(sources[0], hasHandler: false))
        #expect(!MonarchAiClaimSourcesTokens.canOpen(sources[1], hasHandler: false))
        #expect(MonarchAiClaimSourcesTokens.canOpen(sources[1], hasHandler: true))
        #expect(!MonarchAiClaimSourcesTokens.canOpen(nil, hasHandler: true))
    }

    @Test("VoiceOver reads the claim's support and passages, and each passage in full")
    func accessibility() {
        #expect(
            MonarchAiClaimSourcesTokens.claimValue(support: "Supported", passages: "1 passage")
                == "Supported, 1 passage"
        )
        #expect(
            MonarchAiClaimSourcesTokens.passageLabel(
                stance: "Supports the claim",
                quote: "Q",
                source: "Report",
                detail: ""
            )
                == "Supports the claim, Q, Report"
        )
        #expect(
            MonarchAiClaimSourcesTokens.passageLabel(stance: nil, quote: "Q", source: "S", detail: "Page 2")
                == "Q, S, Page 2"
        )
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchAiClaimSourcesStrings.summary(MonarchAiClaimCounts.of([])) == "No claims")
        #expect(
            MonarchAiClaimSourcesStrings.summary(MonarchAiClaimCounts.of(claims))
                == "5 claims: 1 supported, 1 partly, 1 without a source, 1 with sources that disagree, 1 not checked"
        )
        #expect(MonarchAiClaimSourcesStrings.summary(MonarchAiClaimCounts.of([claims[0]])) == "1 claim: 1 supported")
        #expect(MonarchAiClaimSourcesStrings.passages(0) == "No passages")
        #expect(MonarchAiClaimSourcesStrings.passages(1) == "1 passage")
        #expect(MonarchAiClaimSourcesStrings.passages(3) == "3 passages")
        #expect(MonarchAiClaimSourcesStrings.showing(2, 5) == "Showing 2 of 5 claims")
        #expect(MonarchAiClaimSourcesStrings.note(.supported).isEmpty)
        for support in MonarchAiClaimSupport.allCases {
            #expect(!MonarchAiClaimSourcesStrings.support(support).hasPrefix("monarch."))
            if support != .supported {
                #expect(!MonarchAiClaimSourcesStrings.note(support).hasPrefix("monarch."))
            }
        }
        for value in [
            MonarchAiClaimSourcesStrings.heading, MonarchAiClaimSourcesStrings.filterLabel,
            MonarchAiClaimSourcesStrings.all, MonarchAiClaimSourcesStrings.needsChecking,
            MonarchAiClaimSourcesStrings.stance(.supports), MonarchAiClaimSourcesStrings.stance(.contradicts),
            MonarchAiClaimSourcesStrings.unknownSource, MonarchAiClaimSourcesStrings.openSource,
            MonarchAiClaimSourcesStrings.copyQuote, MonarchAiClaimSourcesStrings.copied,
            MonarchAiClaimSourcesStrings.copiedAnnouncement, MonarchAiClaimSourcesStrings.flag,
            MonarchAiClaimSourcesStrings.verify, MonarchAiClaimSourcesStrings.empty,
            MonarchAiClaimSourcesStrings.noneNeedChecking, MonarchAiClaimSourcesStrings.noEvidence,
            MonarchAiClaimSourcesStrings.fromSource("X"), MonarchAiClaimSourcesStrings.evidenceLabel("X"),
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
