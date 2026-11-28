import Foundation
import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct ExportError: Error {}

struct MonarchConsentHistorySection: CataloguePage {
    static let title = "Consent history"

    @State private var exportFails = true

    private let categories = [
        MonarchConsentCategory(id: "necessary", label: "Necessary"),
        MonarchConsentCategory(id: "analytics", label: "Analytics"),
        MonarchConsentCategory(id: "marketing", label: "Marketing"),
    ]

    private let records = [
        MonarchConsentRecord(
            id: "1",
            date: .now.addingTimeInterval(-86_400 * 3),
            method: .custom,
            policyVersion: "2026-03",
            regime: .optIn,
            categories: ["necessary": true, "analytics": true, "marketing": false],
            isGlobalPrivacyControl: true
        ),
        MonarchConsentRecord(
            id: "2",
            date: .now.addingTimeInterval(-86_400 * 90),
            method: .acceptAll,
            policyVersion: "2025-11",
            regime: .optIn,
            categories: ["necessary": true, "analytics": true, "marketing": true]
        ),
    ]

    var body: some View {
        CatalogueCaption("The first download fails, the next one succeeds")
        MonarchConsentHistory(
            records: records,
            categories: categories,
            description: "Choices about cookies and similar storage on this account.",
            showsRelativeTime: true,
            exportTitle: "Download as CSV"
        ) {
            try await Task.sleep(for: .milliseconds(800))
            if exportFails {
                exportFails = false
                throw ExportError()
            }
        }

        CatalogueCaption("Empty")
        MonarchConsentHistory(records: [])
    }
}
