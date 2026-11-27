import Foundation
import SwiftUI
import Testing
import UniformTypeIdentifiers

@testable import MonarchUI

private let formats = [
    MonarchDownloadReportFormat("CSV", id: "csv", description: "Opens in any spreadsheet"),
    MonarchDownloadReportFormat("Excel", id: "xlsx"),
    MonarchDownloadReportFormat("PDF", id: "pdf", description: "For printing and sharing"),
]

enum MonarchDownloadReportButtonFixtures: ComponentFixtures {
    static let id = "download-report-button"
    static let fixtures = [
        Fixture("one format, every variant") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    MonarchDownloadReportButton(variant: variant) { _ in .downloaded }
                }
            }
        },
        Fixture("a menu of formats, every size") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    MonarchDownloadReportButton(formats: formats, size: size) { _ in .downloaded }
                }
            }
        },
        Fixture("disabled") {
            MonarchDownloadReportButton(formats: formats) { _ in .downloaded }.disabled(true)
        },
        Fixture("custom labels, long text") {
            MonarchDownloadReportButton(
                labels: MonarchDownloadReportButtonLabels(single: { "Download the full \($0) export of every invoice" })
            ) { _ in .downloaded }
        },
    ]
}

@Suite("MonarchDownloadReportButton")
struct MonarchDownloadReportButtonTests {
    private typealias Tokens = MonarchDownloadReportButtonTokens
    private let labels = MonarchDownloadReportButtonLabels()
    private let csv = formats[0]

    private struct Failure: Error {}

    @Test("The title names the format when there is one")
    func title() {
        #expect(Tokens.title(formats: [csv], labels: labels) == "Download CSV")
        #expect(Tokens.title(formats: formats, labels: labels) == "Download")
        #expect(Tokens.title(formats: [.csv], labels: labels) == "Download CSV")
        let custom = MonarchDownloadReportButtonLabels(label: "Export", single: { "Get \($0)" })
        #expect(Tokens.title(formats: [csv], labels: custom) == "Get CSV")
        #expect(Tokens.title(formats: formats, labels: custom) == "Export")
    }

    @Test("A result moves to saved, queued or the save dialog")
    func results() {
        #expect(Tokens.phase(after: .downloaded, format: csv, labels: labels) == .done(csv))
        let queued = Tokens.phase(after: .queued(message: nil), format: csv, labels: labels)
        #expect(queued == .queued(csv, MonarchDownloadReportButtonStrings.queued))
        #expect(
            Tokens.phase(after: .queued(message: "Emailed soon"), format: csv, labels: labels)
                == .queued(csv, "Emailed soon")
        )
        let url = URL(fileURLWithPath: "/tmp/report.csv")
        let exporting = Tokens.phase(after: .file(url), format: csv, labels: labels)
        #expect(exporting == .exporting(csv, url))
        #expect(exporting.isBusy)
        #expect(Tokens.filename(url) == "report.csv")
    }

    @Test("A failure shows the app's message or the built-in one")
    func failures() {
        #expect(
            Tokens.phase(afterError: Failure(), format: csv, message: nil, labels: labels)
                == .failed(csv, "The CSV download failed.")
        )
        #expect(
            Tokens.phase(afterError: Failure(), format: csv, message: "No rows", labels: labels)
                == .failed(csv, "No rows")
        )
    }

    @Test("The save dialog: saved, cancelled returns to idle, other errors are shown")
    func export() {
        let url = URL(fileURLWithPath: "/tmp/report.csv")
        #expect(Tokens.phase(afterExport: .success(url), format: csv, labels: labels) == .done(csv))
        #expect(Tokens.phase(afterExport: .failure(CocoaError(.userCancelled)), format: csv, labels: labels) == .idle)
        #expect(
            Tokens.phase(afterExport: .failure(CocoaError(.fileWriteNoPermission)), format: csv, labels: labels)
                == .failed(csv, "The CSV download failed.")
        )
    }

    @Test("Busy only while preparing or saving; the value and announcements follow the phase")
    func spoken() {
        #expect(!Tokens.Phase.idle.isBusy)
        #expect(Tokens.Phase.pending(csv).isBusy)
        #expect(!Tokens.Phase.done(csv).isBusy)
        #expect(Tokens.value(for: .pending(csv), labels: labels) == "Preparing CSV…")
        #expect(Tokens.value(for: .idle, labels: labels).isEmpty)
        #expect(Tokens.announcement(for: .pending(csv), labels: labels) == "Preparing CSV…")
        #expect(Tokens.announcement(for: .done(csv), labels: labels) == "CSV downloaded")
        #expect(Tokens.announcement(for: .failed(csv, "No rows"), labels: labels) == "No rows")
        #expect(Tokens.announcement(for: .idle, labels: labels) == nil)
        #expect(Tokens.Phase.failed(csv, "x").format == csv)
    }

    @Test("Progress is clamped and shown only while busy")
    func progress() {
        #expect(Tokens.progress(0.4, phase: .pending(csv)) == 0.4)
        #expect(Tokens.progress(1.4, phase: .pending(csv)) == 1)
        #expect(Tokens.progress(-1, phase: .pending(csv)) == 0)
        #expect(Tokens.progress(.nan, phase: .pending(csv)) == nil)
        #expect(Tokens.progress(0.4, phase: .idle) == nil)
        #expect(Tokens.progress(nil, phase: .pending(csv)) == nil)
    }

    @Test("A saved file takes its content type from its extension")
    func contentType() {
        #expect(MonarchDownloadReportFile(url: URL(fileURLWithPath: "/tmp/a.csv")).contentType == .commaSeparatedText)
        #expect(MonarchDownloadReportFile(url: URL(fileURLWithPath: "/tmp/a.pdf")).contentType == .pdf)
        #expect(MonarchDownloadReportFile(url: URL(fileURLWithPath: "/tmp/a")).contentType == .data)
    }

    @Test("The built-in strings resolve")
    func strings() {
        let strings = MonarchDownloadReportButtonStrings.self
        #expect(strings.label == "Download")
        #expect(strings.progress == "Download progress")
        #expect(strings.csv == "CSV")
        #expect(strings.error("PDF") == "The PDF download failed.")
        #expect(!strings.queued.hasPrefix("monarch."))
    }

    #if os(iOS)
        @Test("The button keeps a 44-point touch target at the small size")
        @MainActor
        func touchTarget() {
            let size = Host.layout(MonarchDownloadReportButton(size: .sm) { _ in .downloaded })
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
