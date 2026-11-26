import MonarchUI
import SwiftUI

struct MonarchConsentPreferencesSection: CataloguePage {
    static let title = "Consent preferences"

    private static let categories = [
        MonarchConsentPreferencesCategory(
            id: "necessary",
            label: "Strictly necessary",
            description: "Sign-in, security and your settings.",
            isRequired: true
        ),
        MonarchConsentPreferencesCategory(id: "analytics", label: "Analytics", description: "How the app is used."),
        MonarchConsentPreferencesCategory(
            id: "ads",
            label: "Advertising",
            description: "Ads based on your activity in other apps.",
            isSaleOrSharing: true
        ),
    ]

    @State private var showsSheet = false
    @State private var isGlobalPrivacyControl = false
    @State private var choices: [String: Bool] = [:]
    @State private var record: MonarchConsentRecord?

    var body: some View {
        CatalogueCaption("Opened from settings")
        Toggle("Global Privacy Control signal", isOn: $isGlobalPrivacyControl)
        Text(summary).monarchTextStyle(.caption)
        MonarchButton("Privacy choices", systemImage: "hand.raised", variant: .primary) { showsSheet = true }
            .sheet(isPresented: $showsSheet) {
                ScrollView {
                    MonarchConsentPreferences(
                        categories: Self.categories,
                        choices: choices,
                        isGlobalPrivacyControl: isGlobalPrivacyControl,
                        record: record,
                        policyURL: URL(string: "https://example.com/privacy")
                    ) { method, newChoices in
                        save(method, newChoices)
                        showsSheet = false
                    }
                    .padding()
                }
                .presentationDetents([.large])
            }

        CatalogueCaption("Choice mode, one optional category")
        MonarchConsentPreferences(
            categories: Array(Self.categories.prefix(2)),
            choices: choices,
            mode: .choice,
            record: record
        ) { method, newChoices in
            save(method, newChoices)
        }
    }

    private var summary: String {
        guard let record else { return "No choice saved yet." }
        let allowed = record.categories.filter(\.value).keys.sorted().joined(separator: ", ")
        return "Saved (\(record.method.rawValue)): \(allowed)"
    }

    private func save(_ method: MonarchConsentMethod, _ newChoices: [String: Bool]) {
        choices = newChoices
        record = MonarchConsentRecord(
            id: UUID().uuidString,
            date: Date(),
            method: method,
            policyVersion: "3",
            regime: .optIn,
            categories: newChoices,
            isGlobalPrivacyControl: isGlobalPrivacyControl
        )
    }
}
