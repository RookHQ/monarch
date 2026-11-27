import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let reference = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let categories = [
    MonarchConsentCategory(id: "necessary", label: "Necessary"),
    MonarchConsentCategory(id: "analytics", label: "Analytics"),
    MonarchConsentCategory(id: "marketing", label: "Marketing"),
]

private let records = [
    MonarchConsentRecord(
        id: "old",
        date: reference.addingTimeInterval(-86_400 * 40),
        method: .acceptAll,
        policyVersion: "2026-01",
        regime: .optIn,
        categories: ["necessary": true, "analytics": true, "marketing": true]
    ),
    MonarchConsentRecord(
        id: "new",
        date: reference,
        method: .custom,
        policyVersion: "2026-03",
        regime: .optOut,
        categories: ["necessary": true, "analytics": false, "session_replay": false],
        isGlobalPrivacyControl: true
    ),
]

enum MonarchConsentHistoryFixtures: ComponentFixtures {
    static let id = "consent-history"
    static let fixtures = [
        Fixture("records with export") {
            MonarchConsentHistory(
                records: records,
                categories: categories,
                description: "Choices about cookies and similar storage on this account.",
                showsRelativeTime: true,
                exportTitle: "Download as CSV"
            ) {}
        },
        Fixture("every method") {
            MonarchConsentHistory(
                "Every method",
                records: MonarchConsentMethod.allCases.enumerated().map { index, method in
                    MonarchConsentRecord(
                        id: "\(index)",
                        date: reference.addingTimeInterval(Double(-index) * 3_600),
                        method: method,
                        policyVersion: "v\(index)",
                        regime: MonarchConsentRegime.allCases[index % MonarchConsentRegime.allCases.count],
                        categories: [:]
                    )
                }
            )
        },
        Fixture("empty") {
            MonarchConsentHistory(records: [])
        },
    ]
}

@Suite("MonarchConsentHistory")
struct MonarchConsentHistoryTests {
    private let labels = MonarchConsentHistoryLabels()

    @Test("Records are shown newest first")
    func sorted() {
        #expect(MonarchConsentHistoryTokens.sorted(records).map(\.id) == ["new", "old"])
        let same = [
            records[0],
            MonarchConsentRecord(
                id: "twin",
                date: records[0].date,
                method: .rejectAll,
                policyVersion: "1",
                regime: .notice,
                categories: [:]
            ),
        ]
        #expect(MonarchConsentHistoryTokens.sorted(same).map(\.id) == ["old", "twin"])
    }

    @Test("Known categories come first in their order, then other ids by id")
    func categoryRows() {
        let rows = MonarchConsentHistoryTokens.categoryRows(records[1], categories: categories)
        #expect(rows.map(\.id) == ["necessary", "analytics", "session_replay"])
        #expect(rows.map(\.label) == ["Necessary", "Analytics", "session_replay"])
        #expect(rows.map(\.isAllowed) == [true, false, false])
        #expect(
            MonarchConsentHistoryTokens.categoryRows(records[1], categories: []).map(\.id) == [
                "analytics", "necessary", "session_replay",
            ]
        )
    }

    @Test("Methods, regimes and decisions read as text")
    func texts() {
        #expect(MonarchConsentHistoryTokens.methodText(.acceptAll, labels: labels) == "Accepted all")
        #expect(
            MonarchConsentHistoryTokens.methodText(.globalPrivacyControl, labels: labels) == "Browser privacy signal"
        )
        #expect(MonarchConsentHistoryTokens.methodText(.withdrawn, labels: labels) == "Withdrew consent")
        #expect(MonarchConsentHistoryTokens.regimeText(.optIn, labels: labels) == "Asked before use (opt-in)")
        #expect(MonarchConsentHistoryTokens.regimeText(.notice, labels: labels) == "Notice only")
        for method in MonarchConsentMethod.allCases {
            #expect(!MonarchConsentHistoryStrings.method(method).hasPrefix("monarch."))
        }
        for regime in MonarchConsentRegime.allCases {
            #expect(!MonarchConsentHistoryStrings.regime(regime).hasPrefix("monarch."))
        }
        let row = MonarchConsentHistoryTokens.CategoryRow(id: "a", label: "Analytics", isAllowed: false)
        #expect(MonarchConsentHistoryTokens.categoryLabel(row, labels: labels) == "Analytics, Not allowed")
    }

    @Test("The signal line shows only when the method does not already say it")
    func signal() {
        #expect(MonarchConsentHistoryTokens.signalNote(records[1], labels: labels) == "Browser privacy signal present")
        #expect(MonarchConsentHistoryTokens.signalNote(records[0], labels: labels) == nil)
        let gpc = MonarchConsentRecord(
            id: "g",
            date: reference,
            method: .globalPrivacyControl,
            policyVersion: "1",
            regime: .optOut,
            categories: [:],
            isGlobalPrivacyControl: true
        )
        #expect(MonarchConsentHistoryTokens.signalNote(gpc, labels: labels) == nil)
    }

    @Test("Overrides replace the built-in text")
    func overrides() {
        let custom = MonarchConsentHistoryLabels(
            method: { $0 == .custom ? "Choix" : nil },
            allowed: "Autorisé"
        )
        #expect(MonarchConsentHistoryTokens.methodText(.custom, labels: custom) == "Choix")
        #expect(MonarchConsentHistoryTokens.methodText(.acceptAll, labels: custom) == "Accepted all")
        #expect(MonarchConsentHistoryTokens.decisionText(isAllowed: true, labels: custom) == "Autorisé")
        #expect(MonarchConsentHistoryTokens.decisionText(isAllowed: false, labels: custom) == "Not allowed")
    }

    @Test("Decision colours back up the icon and the text")
    func colors() {
        #expect(MonarchConsentHistoryTokens.decisionColor(isAllowed: true) == \MonarchColorTokens.successStrong)
        #expect(MonarchConsentHistoryTokens.decisionColor(isAllowed: false) == \MonarchColorTokens.textMuted)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchConsentHistoryStrings.title == "Consent history")
        #expect(MonarchConsentHistoryStrings.export == "Download history")
        #expect(MonarchConsentHistoryStrings.emptyTitle == "No consent choices recorded")
        #expect(MonarchConsentHistoryStrings.exportFailed == "The download did not start. Try again.")
    }
}
