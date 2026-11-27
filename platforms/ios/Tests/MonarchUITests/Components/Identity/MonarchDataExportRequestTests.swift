import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchDataExportRequestFixtures: ComponentFixtures {
    static let id = "data-export-request"
    static let requestedAt = Date(timeIntervalSince1970: 1_800_000_000)
    static let expiresAt = Date(timeIntervalSince1970: 1_800_604_800)
    static let scope = [
        MonarchCheckboxGroupOption(id: "profile", label: "Profile and settings"),
        MonarchCheckboxGroupOption(
            id: "messages",
            label: "Messages and comments",
            description: "Every message you sent or received, with attachments."
        ),
    ]
    static let formats = [
        MonarchDataExportFormat(id: "zip", label: "ZIP archive"),
        MonarchDataExportFormat(id: "json", label: "JSON (machine-readable)"),
    ]

    static let fixtures = [
        Fixture("idle with scope, formats and note") {
            MonarchDataExportRequest(
                scope: scope,
                formats: formats,
                defaultFormat: "json",
                preparationNote: "It can take up to 48 hours. We will email you when it is ready.",
                onRequest: { _ in }
            )
        },
        Fixture("idle, read only") {
            MonarchDataExportRequest()
        },
        Fixture("requested, busy") {
            MonarchDataExportRequest(status: .requested, requestedAt: requestedAt, isRequesting: true)
        },
        Fixture("preparing") {
            MonarchDataExportRequest(
                status: .preparing,
                requestedAt: requestedAt,
                preparationNote: "It can take up to 48 hours."
            )
        },
        Fixture("ready with download") {
            MonarchDataExportRequest(
                status: .ready,
                readyAt: requestedAt,
                expiresAt: expiresAt,
                downloadURL: URL(string: "https://example.com/export.zip")
            )
        },
        Fixture("expired, request again") {
            MonarchDataExportRequest(status: .expired, expiresAt: expiresAt, scope: scope, onRequest: { _ in })
        },
        Fixture("failed with reason, custom labels") {
            MonarchDataExportRequest(
                status: .failed,
                failureReason: "The archive was larger than the 50 GB limit for a single export.",
                labels: MonarchDataExportRequestLabels(title: "Export your workspace", statuses: [.failed: "Stopped"]),
                onRequest: { _ in }
            )
        },
    ]
}

@Suite("MonarchDataExportRequest")
struct MonarchDataExportRequestTests {
    private let english = Locale(identifier: "en_US")
    private let utc = TimeZone(identifier: "UTC") ?? .gmt
    private let labels = MonarchDataExportRequestLabels()
    private let requestedAt = MonarchDataExportRequestFixtures.requestedAt
    private let expiresAt = MonarchDataExportRequestFixtures.expiresAt

    private struct Described: LocalizedError {
        var errorDescription: String? { "Too many exports today." }
    }

    private struct Plain: Error {}

    private func line(
        _ status: MonarchDataExportStatus,
        requestedAt: Date? = nil,
        readyAt: Date? = nil,
        expiresAt: Date? = nil,
        labels: MonarchDataExportRequestLabels? = nil
    ) -> String? {
        MonarchDataExportRequestTokens.statusLine(
            status,
            requestedAt: requestedAt,
            readyAt: readyAt,
            expiresAt: expiresAt,
            locale: english,
            timeZone: utc,
            labels: labels ?? self.labels
        )
    }

    @Test("Every status has a distinct tone and symbol, and a name")
    func badges() {
        #expect(MonarchDataExportRequestTokens.tone(.idle) == .neutral)
        #expect(MonarchDataExportRequestTokens.tone(.requested) == .info)
        #expect(MonarchDataExportRequestTokens.tone(.preparing) == .info)
        #expect(MonarchDataExportRequestTokens.tone(.ready) == .success)
        #expect(MonarchDataExportRequestTokens.tone(.expired) == .neutral)
        #expect(MonarchDataExportRequestTokens.tone(.failed) == .danger)
        let symbols = MonarchDataExportStatus.allCases.map(MonarchDataExportRequestTokens.symbol)
        #expect(Set(symbols).count == MonarchDataExportStatus.allCases.count)
        #expect(
            MonarchDataExportStatus.allCases.map { MonarchDataExportRequestTokens.statusName($0, labels: labels) } == [
                "Not requested", "Requested", "Preparing", "Ready", "Expired", "Failed",
            ]
        )
        let custom = MonarchDataExportRequestLabels(statuses: [.ready: "Done"])
        #expect(MonarchDataExportRequestTokens.statusName(.ready, labels: custom) == "Done")
    }

    @Test("The form shows for idle, expired and failed with a handler; the note before and while preparing")
    func visibility() {
        let requestable = MonarchDataExportStatus.allCases.filter {
            MonarchDataExportRequestTokens.canRequest($0, hasHandler: true)
        }
        #expect(requestable == [.idle, .expired, .failed])
        #expect(!MonarchDataExportRequestTokens.canRequest(.idle, hasHandler: false))
        let noted = MonarchDataExportStatus.allCases.filter(MonarchDataExportRequestTokens.showsNote)
        #expect(noted == [.idle, .requested, .preparing])
    }

    @Test("Dates use the locale and time zone given")
    func dates() {
        #expect(MonarchDataExportRequestTokens.formatted(nil, locale: english, timeZone: utc) == nil)
        let text = MonarchDataExportRequestTokens.formatted(requestedAt, locale: english, timeZone: utc)
        #expect(text?.contains("Jan 15, 2027") == true)
        #expect(text?.contains("8:00") == true)
    }

    @Test("Status lines name the dates when known")
    func statusLines() {
        #expect(line(.idle) == nil)
        #expect(line(.requested) == "We received your request.")
        #expect(
            line(.requested, requestedAt: requestedAt)?.hasPrefix("We received your request on Jan 15, 2027") == true
        )
        #expect(line(.preparing) == "Your export is being prepared.")
        #expect(line(.ready) == "Your export is ready.")
        let ready = line(.ready, readyAt: requestedAt, expiresAt: expiresAt) ?? ""
        #expect(ready.hasPrefix("Your export is ready. Prepared on Jan 15, 2027"))
        #expect(ready.contains("You can download it until Jan 22, 2027"))
        #expect(line(.expired) == "The download has expired. Request a new export to get your data.")
        #expect(line(.expired, expiresAt: expiresAt)?.hasPrefix("The download expired on Jan 22, 2027") == true)
        #expect(line(.failed) == "Your export could not be prepared. Request it again.")
        let custom = MonarchDataExportRequestLabels(requested: { "Asked \($0 ?? "-")" }, failed: "Broken")
        #expect(line(.requested, labels: custom) == "Asked -")
        #expect(line(.failed, labels: custom) == "Broken")
    }

    @Test("A request needs at least one kind of data when there is a scope")
    func details() {
        #expect(
            MonarchDataExportRequestTokens.details(scope: ["a", "b"], chosen: [], hasFormats: true, format: "zip")
                == nil
        )
        #expect(
            MonarchDataExportRequestTokens.details(
                scope: ["a", "b", "c"],
                chosen: ["c", "a"],
                hasFormats: true,
                format: "zip"
            )
                == MonarchDataExportRequestDetails(scope: ["a", "c"], format: "zip")
        )
        #expect(
            MonarchDataExportRequestTokens.details(scope: [], chosen: [], hasFormats: false, format: "")
                == MonarchDataExportRequestDetails(scope: [], format: nil)
        )
    }

    @Test("Only http and https links are downloaded; a handler alone shows Download")
    func download() {
        #expect(MonarchDataExportRequestTokens.safeDownloadURL(URL(string: "https://example.com/a.zip")) != nil)
        #expect(MonarchDataExportRequestTokens.safeDownloadURL(URL(string: "HTTP://example.com/a.zip")) != nil)
        #expect(MonarchDataExportRequestTokens.safeDownloadURL(URL(string: "javascript:alert(1)")) == nil)
        #expect(MonarchDataExportRequestTokens.safeDownloadURL(URL(string: "file:///etc/hosts")) == nil)
        #expect(MonarchDataExportRequestTokens.safeDownloadURL(nil) == nil)
        let https = URL(string: "https://example.com/a.zip")
        #expect(MonarchDataExportRequestTokens.showsDownload(.ready, url: https, hasHandler: false))
        #expect(MonarchDataExportRequestTokens.showsDownload(.ready, url: nil, hasHandler: true))
        #expect(!MonarchDataExportRequestTokens.showsDownload(.ready, url: URL(string: "ftp://x/y"), hasHandler: false))
        #expect(!MonarchDataExportRequestTokens.showsDownload(.preparing, url: https, hasHandler: true))
    }

    @Test("A failed request shows the error's description or the built-in message")
    func failure() {
        #expect(MonarchDataExportRequestTokens.failureMessage(Described(), labels: labels) == "Too many exports today.")
        #expect(
            MonarchDataExportRequestTokens.failureMessage(Plain(), labels: labels)
                == "The request did not go through. Try again."
        )
        #expect(
            MonarchDataExportRequestTokens.failureMessage(
                Plain(),
                labels: MonarchDataExportRequestLabels(requestFailed: "No")
            )
                == "No"
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        let plain = [
            MonarchDataExportRequestStrings.title, MonarchDataExportRequestStrings.description,
            MonarchDataExportRequestStrings.request, MonarchDataExportRequestStrings.requestAgain,
            MonarchDataExportRequestStrings.download, MonarchDataExportRequestStrings.scopeLegend,
            MonarchDataExportRequestStrings.scopeRequired, MonarchDataExportRequestStrings.formatLabel,
            MonarchDataExportRequestStrings.preparingProgress, MonarchDataExportRequestStrings.failed,
            MonarchDataExportRequestStrings.requestFailed,
        ]
        #expect(plain.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(
            MonarchDataExportRequestStrings.ready("A", "B")
                == "Your export is ready. Prepared on A. You can download it until B."
        )
        #expect(MonarchDataExportRequestStrings.preparing("A") == "Your export is being prepared. Requested on A.")
    }
}
