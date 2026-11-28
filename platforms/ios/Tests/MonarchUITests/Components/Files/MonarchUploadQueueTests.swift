import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchUploadQueueFixtures: ComponentFixtures {
    static let id = "upload-queue"
    static let items = [
        MonarchUploadQueueItem(id: "1", name: "Q3 report.pdf", size: 2_400_000, status: .done),
        MonarchUploadQueueItem(id: "2", name: "photo.jpg", size: 800_000, status: .uploading, progress: 0.4),
        MonarchUploadQueueItem(id: "3", name: "notes.txt", status: .queued),
        MonarchUploadQueueItem(id: "4", name: "big.mov", status: .failed, errorMessage: "Larger than 100 MB"),
        MonarchUploadQueueItem(id: "5", name: "old.zip", status: .cancelled),
    ]
    static let fixtures = [
        Fixture("every status with every action") {
            MonarchUploadQueue(
                items,
                onCancel: { _ in },
                onRetry: { _ in },
                onRemove: { _ in },
                onClearFinished: {}
            )
        },
        Fixture("no summary, no actions, detail line") {
            MonarchUploadQueue(
                [
                    MonarchUploadQueueItem(
                        id: "1",
                        name: "contract.pdf",
                        status: .uploading,
                        detail: "Checking for viruses"
                    ),
                    MonarchUploadQueueItem(id: "2", name: "scan.png", status: .failed, isRetryable: false),
                ],
                showsSummary: false
            )
        },
        Fixture("empty") {
            MonarchUploadQueue([])
        },
        Fixture("long name") {
            MonarchUploadQueue(
                [
                    MonarchUploadQueueItem(
                        id: "1",
                        name: "Recording of the all-hands meeting with questions from every office.mp4",
                        size: 1_800_000_000,
                        status: .uploading,
                        progress: 0.05
                    )
                ],
                onCancel: { _ in }
            )
        },
    ]
}

@Suite("MonarchUploadQueue")
struct MonarchUploadQueueTests {
    private let english = Locale(identifier: "en_US")
    private let labels = MonarchUploadQueueLabels()

    @Test("Counts by status")
    func counts() {
        let counts = MonarchUploadQueueCounts(MonarchUploadQueueFixtures.items)
        #expect(counts.total == 5)
        #expect(counts.done == 1)
        #expect(counts.uploading == 1)
        #expect(counts.queued == 1)
        #expect(counts.failed == 1)
        #expect(counts.cancelled == 1)
        #expect(counts.active == 2)
        #expect(counts.finished == 3)
    }

    @Test("Total progress leaves out failed and cancelled files and weights by size")
    func totalProgress() {
        let bySize = [
            MonarchUploadQueueItem(id: "a", name: "a", size: 300, status: .done),
            MonarchUploadQueueItem(id: "b", name: "b", size: 100, status: .uploading, progress: 0.5),
            MonarchUploadQueueItem(id: "c", name: "c", size: 999, status: .failed),
        ]
        #expect(MonarchUploadQueueTokens.totalProgress(bySize) == 350.0 / 400.0)
        let unsized = [
            MonarchUploadQueueItem(id: "a", name: "a", status: .done),
            MonarchUploadQueueItem(id: "b", name: "b", status: .queued),
        ]
        #expect(MonarchUploadQueueTokens.totalProgress(unsized) == 0.5)
        #expect(MonarchUploadQueueTokens.totalProgress([]) == 0)
    }

    @Test("Status lines, symbols and colours")
    func status() {
        func text(_ item: MonarchUploadQueueItem) -> String {
            MonarchUploadQueueTokens.statusText(item, labels: labels, locale: english)
        }
        #expect(text(MonarchUploadQueueItem(id: "1", name: "a", status: .queued)) == "Waiting")
        #expect(text(MonarchUploadQueueItem(id: "1", name: "a", status: .uploading, progress: 0.4)) == "Uploading… 40%")
        #expect(text(MonarchUploadQueueItem(id: "1", name: "a", status: .uploading)) == "Uploading…")
        #expect(text(MonarchUploadQueueItem(id: "1", name: "a", status: .done)) == "Uploaded")
        #expect(text(MonarchUploadQueueItem(id: "1", name: "a", status: .failed)) == "Upload failed")
        #expect(text(MonarchUploadQueueItem(id: "1", name: "a", status: .failed, errorMessage: "Too big")) == "Too big")
        #expect(text(MonarchUploadQueueItem(id: "1", name: "a", status: .done, detail: "Scanning")) == "Scanning")
        #expect(MonarchUploadQueueTokens.statusSymbol(.done) == "checkmark.circle.fill")
        #expect(MonarchUploadQueueTokens.statusSymbol(.failed) == "xmark.octagon.fill")
        #expect(MonarchUploadQueueTokens.statusSymbol(.queued) == nil)
        #expect(MonarchUploadQueueTokens.statusColor(.done) == \MonarchColorTokens.successStrong)
        #expect(MonarchUploadQueueTokens.statusColor(.failed) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchUploadQueueTokens.statusColor(.uploading) == \MonarchColorTokens.textMuted)
    }

    @Test("Cancel while active, remove once ended, retry when failed or cancelled and retryable")
    func actions() {
        let uploading = MonarchUploadQueueItem(id: "1", name: "a", status: .uploading)
        let failed = MonarchUploadQueueItem(id: "2", name: "b", status: .failed)
        let cancelled = MonarchUploadQueueItem(id: "3", name: "c", status: .cancelled)
        let locked = MonarchUploadQueueItem(id: "4", name: "d", status: .failed, isRetryable: false)
        #expect(MonarchUploadQueueTokens.showsCancel(uploading, hasHandler: true))
        #expect(!MonarchUploadQueueTokens.showsCancel(failed, hasHandler: true))
        #expect(!MonarchUploadQueueTokens.showsCancel(uploading, hasHandler: false))
        #expect(MonarchUploadQueueTokens.showsRemove(failed, hasHandler: true))
        #expect(!MonarchUploadQueueTokens.showsRemove(uploading, hasHandler: true))
        #expect(MonarchUploadQueueTokens.showsRetry(failed, hasHandler: true))
        #expect(MonarchUploadQueueTokens.showsRetry(cancelled, hasHandler: true))
        #expect(!MonarchUploadQueueTokens.showsRetry(locked, hasHandler: true))
        #expect(!MonarchUploadQueueTokens.showsRetry(uploading, hasHandler: true))
    }

    @Test("Finished and failed files are announced, then the end of the batch")
    func announcements() {
        let before = MonarchUploadQueueTokens.statuses([
            MonarchUploadQueueItem(id: "1", name: "a.pdf", status: .uploading),
            MonarchUploadQueueItem(id: "2", name: "b.pdf", status: .uploading),
        ])
        let oneDone = [
            MonarchUploadQueueItem(id: "1", name: "a.pdf", status: .done),
            MonarchUploadQueueItem(id: "2", name: "b.pdf", status: .uploading),
        ]
        #expect(
            MonarchUploadQueueTokens.announcement(previous: before, items: oneDone, labels: labels) == "a.pdf uploaded."
        )
        let allEnded = [
            MonarchUploadQueueItem(id: "1", name: "a.pdf", status: .done),
            MonarchUploadQueueItem(id: "2", name: "b.pdf", status: .failed, errorMessage: "Too large"),
        ]
        #expect(
            MonarchUploadQueueTokens.announcement(
                previous: MonarchUploadQueueTokens.statuses(oneDone),
                items: allEnded,
                labels: labels
            ) == "b.pdf failed: Too large. Uploads finished: 1 uploaded, 1 failed."
        )
        #expect(
            MonarchUploadQueueTokens.announcement(
                previous: MonarchUploadQueueTokens.statuses(allEnded),
                items: allEnded,
                labels: labels
            ) == nil
        )
        let single = [MonarchUploadQueueItem(id: "1", name: "a.pdf", status: .done)]
        #expect(
            MonarchUploadQueueTokens.announcement(previous: ["1": .uploading], items: single, labels: labels)
                == "a.pdf uploaded."
        )
    }

    @Test("The built-in strings resolve")
    func strings() {
        let counts = MonarchUploadQueueCounts(MonarchUploadQueueFixtures.items)
        #expect(MonarchUploadQueueStrings.title == "Uploads")
        #expect(MonarchUploadQueueStrings.summary(counts) == "1 of 4 uploaded · 1 failed · 1 cancelled")
        #expect(MonarchUploadQueueStrings.cancel("a") == "Cancel upload of a")
        #expect(MonarchUploadQueueStrings.retryLabel("a") == "Retry upload of a")
        #expect(MonarchUploadQueueStrings.remove("a") == "Remove a from the list")
        #expect(MonarchUploadQueueStrings.progressLabel("a") == "Upload progress of a")
        #expect(MonarchUploadQueueStrings.totalLabel == "Total upload progress")
        #expect(MonarchUploadQueueStrings.clearFinished == "Clear finished")
        #expect(MonarchUploadQueueStrings.retry == "Retry")
        #expect(MonarchUploadQueueStrings.announceFailed("a", nil) == "a failed.")
        let clean = MonarchUploadQueueCounts([MonarchUploadQueueItem(id: "1", name: "a", status: .done)])
        #expect(MonarchUploadQueueStrings.announceFinished(clean) == "All uploads finished: 1 uploaded.")
    }
}
