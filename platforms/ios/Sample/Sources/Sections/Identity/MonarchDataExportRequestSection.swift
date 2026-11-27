@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchDataExportRequestSection: CataloguePage {
    static let title = "Data export request"

    private struct RequestFailure: LocalizedError {
        var errorDescription: String? { "You can request one export a day. Try again tomorrow." }
    }

    private static let scope = [
        MonarchCheckboxGroupOption(id: "profile", label: "Profile and settings"),
        MonarchCheckboxGroupOption(id: "messages", label: "Messages and comments", description: "With attachments."),
        MonarchCheckboxGroupOption(id: "files", label: "Files"),
    ]

    private static let formats = [
        MonarchDataExportFormat(id: "zip", label: "ZIP archive"),
        MonarchDataExportFormat(id: "json", label: "JSON (machine-readable)"),
    ]

    @State private var status = MonarchDataExportStatus.idle
    @State private var requestedAt: Date?
    @State private var failsNext = false

    var body: some View {
        CatalogueCaption("Status")
        Picker("Status", selection: $status) {
            ForEach(MonarchDataExportStatus.allCases, id: \.self) { Text($0.rawValue.capitalized).tag($0) }
        }
        .pickerStyle(.menu)
        Toggle("Fail the next request", isOn: $failsNext)

        MonarchDataExportRequest(
            status: status,
            requestedAt: requestedAt,
            readyAt: requestedAt,
            expiresAt: requestedAt?.addingTimeInterval(7 * 86_400),
            scope: Self.scope,
            formats: Self.formats,
            preparationNote: "It can take up to 48 hours. We will email you when it is ready.",
            failureReason: "The archive was larger than the limit for one export.",
            downloadURL: URL(string: "https://example.com/export.zip"),
            onRequest: { _ in
                try await Task.sleep(for: .seconds(1))
                if failsNext {
                    failsNext = false
                    throw RequestFailure()
                }
                requestedAt = Date()
                status = .requested
            }
        )
    }
}
