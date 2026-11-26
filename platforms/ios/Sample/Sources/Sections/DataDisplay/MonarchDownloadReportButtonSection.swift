import Foundation
import MonarchUI
import SwiftUI

struct MonarchDownloadReportButtonSection: CataloguePage {
    static let title = "DownloadReportButton"

    private struct ExportFailed: Error {}

    @State private var progress = 0.0
    @State private var failsNext = false

    private let formats = [
        MonarchDownloadReportFormat("CSV", id: "csv", description: "Opens in any spreadsheet"),
        MonarchDownloadReportFormat("PDF", id: "pdf", description: "For printing and sharing"),
        MonarchDownloadReportFormat("Excel (large, prepared later)", id: "xlsx"),
    ]

    var body: some View {
        Toggle("Make the next download fail", isOn: $failsNext)

        CatalogueCaption("One format: saves through the system dialog")
        MonarchDownloadReportButton(progress: progress) { _ in
            try await prepare()
            let url = FileManager.default.temporaryDirectory.appending(path: "invoices.csv")
            try Data("number,total\n1042,120.00\n".utf8).write(to: url)
            return .file(url)
        }

        CatalogueCaption("A menu of formats, one prepared in the background")
        MonarchDownloadReportButton(formats: formats, progress: progress, variant: .primary, size: .md) { format in
            try await prepare()
            return format.id == "xlsx" ? .queued(message: nil) : .downloaded
        }
    }

    private func prepare() async throws {
        progress = 0
        for step in 1...5 {
            try await Task.sleep(for: .milliseconds(300))
            progress = Double(step) / 5
        }
        if failsNext { throw ExportFailed() }
    }
}
