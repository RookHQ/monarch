import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAttachmentChipFixtures: ComponentFixtures {
    static let id = "attachment-chip"
    static let fixtures = [
        Fixture("every status") {
            VStack(alignment: .leading) {
                ForEach(MonarchUploadStatus.allCases, id: \.self) { status in
                    MonarchAttachmentChip(
                        "Q3 report.pdf",
                        size: 2_400_000,
                        status: status,
                        progress: 0.4,
                        onRetry: {},
                        onRemove: {}
                    )
                }
            }
        },
        Fixture("indeterminate, error text, no buttons") {
            VStack(alignment: .leading) {
                MonarchAttachmentChip("photo.jpg", contentType: "image/jpeg", status: .uploading)
                MonarchAttachmentChip("big.mov", status: .failed, errorMessage: "Larger than 100 MB")
                MonarchAttachmentChip("notes.txt")
            }
        },
        Fixture("link and long name") {
            MonarchAttachmentChip(
                "Minutes of the quarterly planning meeting with every regional team, final version.docx",
                size: 48_000,
                url: URL(string: "https://example.com/files/1"),
                onRemove: {}
            )
        },
        Fixture("disabled") {
            MonarchAttachmentChip("draft.key", size: 9_000_000, onRemove: {}).disabled(true)
        },
    ]
}

@Suite("MonarchAttachmentChip")
struct MonarchAttachmentChipTests {
    private let english = Locale(identifier: "en_US")
    private let labels = MonarchAttachmentChipLabels()

    private func details(
        _ status: MonarchUploadStatus,
        size: Int64? = 2_400_000,
        progress: Double? = nil,
        error: String? = nil
    ) -> String? {
        MonarchAttachmentChipTokens.details(
            status: status,
            size: size,
            progress: progress,
            errorMessage: error,
            labels: labels,
            locale: english
        )
    }

    @Test("The line under the name follows the status")
    func detailsLine() {
        #expect(details(.done) == MonarchAttachmentChipTokens.formattedSize(2_400_000, locale: english))
        #expect(details(.done, size: nil) == nil)
        #expect(details(.uploading, progress: 0.4) == "40%")
        #expect(details(.uploading, progress: 1.7) == "100%")
        #expect(details(.uploading) == nil)
        #expect(details(.queued) == "Waiting")
        #expect(details(.failed) == "Upload failed")
        #expect(details(.failed, error: "Too large") == "Too large")
        #expect(details(.cancelled) == "Cancelled")
    }

    @Test("Colours: danger details on failure, muted name once cancelled")
    func colours() {
        #expect(MonarchAttachmentChipTokens.detailsColor(.failed) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchAttachmentChipTokens.detailsColor(.done) == \MonarchColorTokens.textMuted)
        #expect(MonarchAttachmentChipTokens.nameColor(.cancelled) == \MonarchColorTokens.textMuted)
        #expect(MonarchAttachmentChipTokens.nameColor(.done) == \MonarchColorTokens.textDefault)
        #expect(MonarchAttachmentChipTokens.fill == \MonarchColorTokens.bgSurface)
        #expect(MonarchAttachmentChipTokens.border == \MonarchColorTokens.borderDefault)
    }

    @Test("The name links only once uploaded, to a safe URL")
    func link() {
        let safe = URL(string: "https://example.com/f")
        #expect(MonarchAttachmentChipTokens.link(safe, status: .done) == safe)
        #expect(MonarchAttachmentChipTokens.link(safe, status: .uploading) == nil)
        #expect(MonarchAttachmentChipTokens.link(URL(string: "javascript:alert(1)"), status: .done) == nil)
        #expect(MonarchAttachmentChipTokens.link(nil, status: .done) == nil)
    }

    @Test("Retry shows only for a failure with a handler; a failure is announced once")
    func retryAndAnnouncement() {
        #expect(MonarchAttachmentChipTokens.showsRetry(.failed, hasHandler: true))
        #expect(!MonarchAttachmentChipTokens.showsRetry(.failed, hasHandler: false))
        #expect(!MonarchAttachmentChipTokens.showsRetry(.cancelled, hasHandler: true))
        #expect(MonarchAttachmentChipTokens.announcesFailure(from: .uploading, to: .failed))
        #expect(!MonarchAttachmentChipTokens.announcesFailure(from: .failed, to: .failed))
        #expect(!MonarchAttachmentChipTokens.announcesFailure(from: .uploading, to: .done))
    }

    @Test("Progress is limited to 0 through 1")
    func fraction() {
        #expect(MonarchAttachmentChipTokens.fraction(0.5) == 0.5)
        #expect(MonarchAttachmentChipTokens.fraction(-1) == 0)
        #expect(MonarchAttachmentChipTokens.fraction(2) == 1)
        #expect(MonarchAttachmentChipTokens.fraction(.nan) == nil)
        #expect(MonarchAttachmentChipTokens.fraction(nil) == nil)
    }

    @Test("The built-in strings resolve and name the file")
    func strings() {
        #expect(MonarchAttachmentChipStrings.remove("a.pdf") == "Remove a.pdf")
        #expect(MonarchAttachmentChipStrings.retry("a.pdf") == "Retry a.pdf")
        #expect(MonarchAttachmentChipStrings.uploading("a.pdf") == "Uploading a.pdf")
        #expect(MonarchAttachmentChipStrings.queued == "Waiting")
        #expect(MonarchAttachmentChipStrings.failed == "Upload failed")
        #expect(MonarchAttachmentChipStrings.cancelled == "Cancelled")
    }

    @Test("The chip is at least 44 points high")
    @MainActor
    func height() {
        let size = Host.layout(MonarchAttachmentChip("a.pdf", size: 10))
        #expect(size.height >= MonarchAccessibility.minimumHitTarget)
    }
}
