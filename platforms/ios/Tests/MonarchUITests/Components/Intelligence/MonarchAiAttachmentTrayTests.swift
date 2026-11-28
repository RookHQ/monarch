import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiAttachmentTrayFixtures: ComponentFixtures {
    static let id = "ai-attachment-tray"
    static let ready = MonarchAiAttachmentTrayFile(
        id: "a",
        name: "Q3 report.pdf",
        size: 2_400_000,
        contentType: "application/pdf"
    )
    static let uploading = MonarchAiAttachmentTrayFile(
        id: "b",
        name: "photo.png",
        size: 840_000,
        contentType: "image/png",
        progress: 0.45
    )
    static let failed = MonarchAiAttachmentTrayFile(id: "c", name: "data.csv", errorMessage: "File too large")

    static let fixtures = [
        Fixture("ready, uploading and failed with retry") {
            MonarchAiAttachmentTray([ready, uploading, failed], onRetry: { _ in }, onRemove: { _ in })
        },
        Fixture("failed without retry, default message") {
            MonarchAiAttachmentTray(
                [MonarchAiAttachmentTrayFile(id: "d", name: "notes.txt", isFailed: true)],
                onRemove: { _ in }
            )
        },
        Fixture("long name") {
            MonarchAiAttachmentTray(
                [
                    MonarchAiAttachmentTrayFile(
                        id: "e",
                        name: "Signed purchase agreement for the warehouse lease, version 3 (final).pdf",
                        size: 12_000_000
                    )
                ],
                onRemove: { _ in }
            )
        },
        Fixture("from a conversation file part") {
            MonarchAiAttachmentTray(
                [
                    MonarchAiAttachmentTrayFile(
                        MonarchAiFilePart(name: "brief.docx", mediaType: "application/msword", size: 52_000)
                    )
                ],
                onRemove: { _ in }
            )
        },
        Fixture("empty") {
            MonarchAiAttachmentTray([], onRemove: { _ in })
        },
    ]
}

@Suite("MonarchAiAttachmentTray")
struct MonarchAiAttachmentTrayTests {
    private let labels = MonarchAiAttachmentTrayLabels()

    @Test("Status comes from the error, then the progress")
    func status() {
        #expect(MonarchAiAttachmentTrayTokens.status(MonarchAiAttachmentTrayFixtures.ready) == .done)
        #expect(MonarchAiAttachmentTrayTokens.status(MonarchAiAttachmentTrayFixtures.uploading) == .uploading)
        #expect(MonarchAiAttachmentTrayTokens.status(MonarchAiAttachmentTrayFixtures.failed) == .failed)
        let flagged = MonarchAiAttachmentTrayFile(id: "x", name: "x", progress: 0.2, isFailed: true)
        #expect(MonarchAiAttachmentTrayTokens.status(flagged) == .failed)
        let complete = MonarchAiAttachmentTrayFile(id: "x", name: "x", progress: 1)
        #expect(MonarchAiAttachmentTrayTokens.status(complete) == .done)
    }

    @Test("Progress is shown only while uploading, clamped")
    func progress() {
        #expect(MonarchAiAttachmentTrayTokens.progress(MonarchAiAttachmentTrayFixtures.uploading) == 0.45)
        #expect(MonarchAiAttachmentTrayTokens.progress(MonarchAiAttachmentTrayFixtures.ready) == nil)
        let negative = MonarchAiAttachmentTrayFile(id: "x", name: "x", progress: -0.5)
        #expect(MonarchAiAttachmentTrayTokens.progress(negative) == 0)
    }

    @Test("A finished upload and a removed file are announced; the first state is not")
    func announcements() {
        let before = MonarchAiAttachmentTrayTokens.snapshot([
            MonarchAiAttachmentTrayFixtures.ready, MonarchAiAttachmentTrayFixtures.uploading,
        ])
        var done = MonarchAiAttachmentTrayFixtures.uploading
        done.progress = 1
        #expect(
            MonarchAiAttachmentTrayTokens.announcements(
                previous: before,
                files: [done],
                removing: ["a"],
                labels: labels
            )
                == ["photo.png uploaded", "Q3 report.pdf removed"]
        )
        #expect(
            MonarchAiAttachmentTrayTokens.announcements(previous: before, files: [done], removing: [], labels: labels)
                == ["photo.png uploaded"]
        )
        #expect(
            MonarchAiAttachmentTrayTokens.announcements(previous: nil, files: [done], removing: [], labels: labels)
                .isEmpty
        )
        // A failure is left to the chip.
        var failed = MonarchAiAttachmentTrayFixtures.uploading
        failed.errorMessage = "Network lost"
        #expect(
            MonarchAiAttachmentTrayTokens.announcements(previous: before, files: [failed], removing: [], labels: labels)
                .isEmpty
        )
    }

    @Test("Labels replace the built-in strings")
    func overrides() {
        let custom = MonarchAiAttachmentTrayLabels(label: "Pièces jointes", removed: { "\($0) retiré" })
        #expect(MonarchAiAttachmentTrayTokens.label(custom) == "Pièces jointes")
        #expect(MonarchAiAttachmentTrayTokens.label(labels) == "Attachments")
        let before = MonarchAiAttachmentTrayTokens.snapshot([MonarchAiAttachmentTrayFixtures.ready])
        #expect(
            MonarchAiAttachmentTrayTokens.announcements(previous: before, files: [], removing: ["a"], labels: custom)
                == ["Q3 report.pdf retiré"]
        )
    }

    @Test("A conversation file part becomes a ready file")
    func filePart() {
        let part = MonarchAiFilePart(name: "brief.docx", mediaType: "application/msword", size: 52_000, fileId: "f1")
        let file = MonarchAiAttachmentTrayFile(part)
        #expect(file.id == "f1")
        #expect(file.size == 52_000)
        #expect(file.contentType == "application/msword")
        #expect(MonarchAiAttachmentTrayTokens.status(file) == .done)
        #expect(MonarchAiAttachmentTrayFile(part, id: "x", progress: 0.1).id == "x")
    }
}
