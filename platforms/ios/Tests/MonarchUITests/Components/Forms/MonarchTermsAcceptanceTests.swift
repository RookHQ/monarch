import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTermsAcceptanceFixtures: ComponentFixtures {
    static let id = "terms-acceptance"
    static let documents = [
        MonarchTermsDocument("Terms of Service", url: URL(filePath: "/terms"), version: "2026-11-01"),
        MonarchTermsDocument("Privacy Policy", url: URL(filePath: "/privacy"), version: "2026-10-15", id: "privacy"),
    ]

    static let fixtures = [
        Fixture("unticked") {
            MonarchTermsAcceptance(isAccepted: .constant(false), documents: documents)
        },
        Fixture("accepted") {
            MonarchTermsAcceptance(isAccepted: .constant(true), documents: documents)
        },
        Fixture("required error") {
            MonarchTermsAcceptance(isAccepted: .constant(false), documents: documents, showsRequiredError: true)
        },
        Fixture("optional, one document") {
            MonarchTermsAcceptance(
                isAccepted: .constant(false),
                documents: [MonarchTermsDocument("Community Guidelines", url: URL(filePath: "/guidelines"))],
                isRequired: false
            )
        },
        Fixture("three documents, custom label") {
            MonarchTermsAcceptance(
                isAccepted: .constant(false),
                documents: documents + [
                    MonarchTermsDocument("Acceptable Use Policy for every workspace", url: URL(filePath: "/aup"))
                ],
                showsRequiredError: true,
                labels: MonarchTermsAcceptanceLabels(
                    label: { "I have read and accept the \($0)" },
                    requiredMessage: "Tick the box to create your account"
                )
            )
        },
        Fixture("disabled") {
            MonarchTermsAcceptance(isAccepted: .constant(false), documents: documents).disabled(true)
        },
    ]
}

@Suite("MonarchTermsAcceptance")
struct MonarchTermsAcceptanceTests {
    private let documents = MonarchTermsAcceptanceFixtures.documents
    private let english = Locale(identifier: "en_US")

    @Test("Titles are joined in the locale's list format")
    func joined() {
        #expect(
            MonarchTermsAcceptanceTokens.joinedTitles(documents, locale: english)
                == "Terms of Service and Privacy Policy"
        )
        let three = documents + [MonarchTermsDocument("Cookie Policy", url: URL(filePath: "/cookies"))]
        #expect(
            MonarchTermsAcceptanceTokens.joinedTitles(three, locale: english)
                == "Terms of Service, Privacy Policy, and Cookie Policy"
        )
        #expect(
            MonarchTermsAcceptanceTokens.joinedTitles(Array(documents.prefix(1)), locale: english) == "Terms of Service"
        )
    }

    @Test("The label is the built-in sentence or the app's")
    func label() {
        #expect(
            MonarchTermsAcceptanceTokens.label(documents, locale: english, builder: nil)
                == "I agree to the Terms of Service and Privacy Policy"
        )
        #expect(
            MonarchTermsAcceptanceTokens.label(documents, locale: english, builder: { "Accept: \($0)" })
                == "Accept: Terms of Service and Privacy Policy"
        )
    }

    @Test("The error shows only when asked, required and unticked")
    func error() {
        #expect(MonarchTermsAcceptanceTokens.showsError(isRequired: true, isAccepted: false, showsRequiredError: true))
        #expect(!MonarchTermsAcceptanceTokens.showsError(isRequired: true, isAccepted: true, showsRequiredError: true))
        #expect(
            !MonarchTermsAcceptanceTokens.showsError(isRequired: false, isAccepted: false, showsRequiredError: true)
        )
        #expect(
            !MonarchTermsAcceptanceTokens.showsError(isRequired: true, isAccepted: false, showsRequiredError: false)
        )
    }

    @Test("Versions are keyed by id, or the URL, and skip documents without one")
    func versions() {
        let all = documents + [MonarchTermsDocument("Cookie Policy", url: URL(filePath: "/cookies"))]
        let versions = MonarchTermsDocument.versions(of: all)
        #expect(versions == ["file:///terms": "2026-11-01", "privacy": "2026-10-15"])
    }

    @Test("Links dim when disabled; the error is danger")
    func colours() {
        #expect(MonarchTermsAcceptanceTokens.linkRole(isEnabled: true) == \MonarchColorTokens.textLink)
        #expect(MonarchTermsAcceptanceTokens.linkRole(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchTermsAcceptanceTokens.errorRole == \MonarchColorTokens.dangerStrong)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchTermsAcceptanceStrings.requiredMessage == "Accept the terms to continue")
        #expect(MonarchTermsAcceptanceStrings.requiredHint == "Required")
        #expect(MonarchTermsAcceptanceStrings.label("X") == "I agree to the X")
    }
}
