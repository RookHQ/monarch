import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiAutofillFieldFixtures: ComponentFixtures {
    static let id = "ai-autofill-field"
    static let date = Date(timeIntervalSinceReferenceDate: 800_000_000)

    @MainActor static func field(
        _ state: MonarchAiAutofillState,
        confidence: Double? = 0.92,
        source: String? = "invoice.pdf"
    ) -> some View {
        MonarchAiAutofillField(
            state: state,
            suggestion: "Rook Labs",
            fieldName: "Company",
            source: source,
            confidence: confidence,
            model: "Rook 2",
            date: date,
            onAccept: {},
            onClear: {},
            control: { MonarchTextField("Company", text: .constant(state == .cleared ? "" : "Rook Labs")) }
        )
    }

    static let fixtures = [
        Fixture("suggested, high confidence") { field(.suggested) },
        Fixture("suggested, low confidence") { field(.suggested, confidence: 0.3) },
        Fixture("suggested, no confidence or source") { field(.suggested, confidence: nil, source: nil) },
        Fixture("accepted") { field(.accepted) },
        Fixture("edited") { field(.edited) },
        Fixture("cleared") { field(.cleared) },
        Fixture("long source, form field control") {
            MonarchAiAutofillField(
                state: .suggested,
                suggestion: "12 Market Street, Lisbon",
                source: "Signed purchase agreement for the warehouse lease, version 3 (final).pdf",
                confidence: 0.7,
                onAccept: {},
                onClear: {},
                control: {
                    MonarchFormField("Address", helper: "Where the invoice is sent") {
                        TextField("Address", text: .constant("12 Market Street, Lisbon"))
                    }
                }
            )
        },
    ]
}

@Suite("MonarchAiAutofillField")
struct MonarchAiAutofillFieldTests {
    private let labels = MonarchAiAutofillFieldLabels()

    @Test("Each state shows its own parts")
    func parts() {
        #expect(MonarchAiAutofillFieldTokens.showsStatus(.suggested))
        #expect(MonarchAiAutofillFieldTokens.showsStatus(.accepted))
        #expect(MonarchAiAutofillFieldTokens.showsStatus(.edited))
        #expect(!MonarchAiAutofillFieldTokens.showsStatus(.cleared))
        let withActions = MonarchAiAutofillState.allCases.filter(MonarchAiAutofillFieldTokens.showsActions)
        #expect(withActions == [.suggested])
        #expect(MonarchAiAutofillFieldTokens.showsConfidence(.suggested, confidence: 0.5))
        #expect(!MonarchAiAutofillFieldTokens.showsConfidence(.suggested, confidence: nil))
        #expect(!MonarchAiAutofillFieldTokens.showsConfidence(.accepted, confidence: 0.5))
        #expect(MonarchAiAutofillFieldTokens.showsSource(.suggested, source: "a.pdf"))
        #expect(MonarchAiAutofillFieldTokens.showsSource(.accepted, source: "a.pdf"))
        #expect(!MonarchAiAutofillFieldTokens.showsSource(.edited, source: "a.pdf"))
        #expect(!MonarchAiAutofillFieldTokens.showsSource(.suggested, source: ""))
        let withOriginal = MonarchAiAutofillState.allCases.filter(MonarchAiAutofillFieldTokens.showsOriginal)
        #expect(withOriginal == [.edited])
    }

    @Test("The badge says who filled the field")
    func badge() {
        #expect(MonarchAiAutofillFieldTokens.badgeKind(.suggested) == .generated)
        #expect(MonarchAiAutofillFieldTokens.badgeKind(.accepted) == .generated)
        #expect(MonarchAiAutofillFieldTokens.badgeKind(.edited) == .edited)
        #expect(MonarchAiAutofillFieldTokens.badgeText(.suggested, labels: labels) == "AI suggested")
        #expect(MonarchAiAutofillFieldTokens.badgeText(.accepted, labels: labels) == "AI filled")
        #expect(MonarchAiAutofillFieldTokens.badgeText(.edited, labels: labels) == "Edited by you")
    }

    @Test("Only a suggested field is marked")
    func marker() {
        #expect(MonarchAiAutofillFieldTokens.markerRole(.suggested) == \.borderStrong)
        #expect(MonarchAiAutofillFieldTokens.markerRole(.accepted) == nil)
        #expect(MonarchAiAutofillFieldTokens.markerRole(.edited) == nil)
        #expect(MonarchAiAutofillFieldTokens.markerRole(.cleared) == nil)
    }

    @Test("Actions are named with the field")
    func actionLabels() {
        #expect(
            MonarchAiAutofillFieldTokens.acceptLabel(fieldName: "Company", labels: labels) == "Accept suggested Company"
        )
        #expect(
            MonarchAiAutofillFieldTokens.clearLabel(fieldName: "Company", labels: labels) == "Clear suggested Company"
        )
        #expect(MonarchAiAutofillFieldTokens.acceptLabel(fieldName: nil, labels: labels) == "Accept")
        #expect(MonarchAiAutofillFieldTokens.clearLabel(fieldName: "", labels: labels) == "Clear")
        let custom = MonarchAiAutofillFieldLabels(acceptField: { "Accepter \($0)" })
        #expect(MonarchAiAutofillFieldTokens.acceptLabel(fieldName: "Société", labels: custom) == "Accepter Société")
    }

    @Test("Source, original and announcements read in words")
    func texts() {
        #expect(MonarchAiAutofillFieldTokens.sourceText("invoice.pdf", labels: labels) == "From invoice.pdf")
        #expect(MonarchAiAutofillFieldTokens.originalText("Rook Labs", labels: labels) == "AI suggested: Rook Labs")
        #expect(MonarchAiAutofillFieldTokens.acceptedAnnouncement(labels) == "Suggestion accepted")
        #expect(MonarchAiAutofillFieldTokens.clearedAnnouncement(labels) == "Suggestion cleared")
        let custom = MonarchAiAutofillFieldLabels(source: { "Source : \($0)" }, cleared: "Effacé")
        #expect(MonarchAiAutofillFieldTokens.sourceText("a.pdf", labels: custom) == "Source : a.pdf")
        #expect(MonarchAiAutofillFieldTokens.clearedAnnouncement(custom) == "Effacé")
    }
}
