import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchUploadQueue``.
enum MonarchUploadQueueTokens {
    /// The id the title takes in the focus state, where focus goes when the last row is removed.
    static let headingFocusID = "monarch.uploadQueue.heading"

    /// The gap between the header and the rows, and inside the header.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon, the text and the buttons of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space3
    /// The padding at the sides of the header and the rows.
    static let cardPadding: CGFloat = MonarchTokens.Space.space4
    /// The corner radius of the card.
    static let cardRadius: CGFloat = CGFloat(MonarchTokens.Radius.lg)
    /// The size and corner radius of the tile behind a file's icon.
    static let mediaSize: CGFloat = 36
    static let mediaRadius: CGFloat = CGFloat(MonarchTokens.Radius.md)
    /// The SF Symbol of a file.
    static let fileSymbol = "doc"
    /// The SF Symbol of the cancel and remove buttons.
    static let closeSymbol = "xmark"
    /// The SF Symbol of the retry button.
    static let retrySymbol = "arrow.clockwise"
    /// The role of secondary text.
    static var muted: MonarchColorRole { \.textMuted }

    /// A progress between 0 and 1, or `nil` for unknown.
    static func fraction(_ progress: Double?) -> Double? {
        guard let progress, progress.isFinite else { return nil }
        return min(max(progress, 0), 1)
    }

    /// The overall progress, 0 through 1, of the uploads that can still
    /// finish (failed and cancelled ones are left out), weighted by size
    /// when every one of them has a size.
    static func totalProgress(_ items: [MonarchUploadQueueItem]) -> Double {
        let live = items.filter { $0.status != .failed && $0.status != .cancelled }
        guard !live.isEmpty else { return 0 }
        let bySize = live.allSatisfy { ($0.size ?? 0) > 0 }
        var sum = 0.0
        var weight = 0.0
        for item in live {
            let itemWeight = bySize ? Double(item.size ?? 0) : 1
            let done: Double =
                switch item.status {
                case .done: 1
                case .uploading: fraction(item.progress) ?? 0
                case .queued, .failed, .cancelled: 0
                }
            sum += itemWeight * done
            weight += itemWeight
        }
        return weight > 0 ? sum / weight : 0
    }

    /// A fraction as a whole percentage in a locale.
    static func percent(_ fraction: Double, locale: Locale) -> String {
        fraction.formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// A size in bytes in the locale's units: decimal, or binary (1024-based) when asked.
    static func formattedSize(_ bytes: Int64, locale: Locale, binary: Bool = false) -> String {
        bytes.formatted(ByteCountFormatStyle(style: binary ? .memory : .file, locale: locale))
    }

    /// The status line of a row.
    static func statusText(_ item: MonarchUploadQueueItem, labels: MonarchUploadQueueLabels, locale: Locale) -> String {
        if let detail = item.detail, !detail.isEmpty { return detail }
        switch item.status {
        case .queued:
            return labels.queued ?? MonarchUploadQueueStrings.queued
        case .uploading:
            let percent = fraction(item.progress).map { self.percent($0, locale: locale) }
            return labels.uploading?(percent) ?? MonarchUploadQueueStrings.uploading(percent)
        case .done:
            return labels.done ?? MonarchUploadQueueStrings.done
        case .failed:
            return item.errorMessage.flatMap { $0.isEmpty ? nil : $0 }
                ?? labels.failed ?? MonarchUploadQueueStrings.failed
        case .cancelled:
            return labels.cancelled ?? MonarchUploadQueueStrings.cancelled
        }
    }

    /// The SF Symbol before the status line: a check mark when done, an
    /// error mark when failed, none otherwise.
    static func statusSymbol(_ status: MonarchUploadStatus) -> String? {
        switch status {
        case .done: "checkmark.circle.fill"
        case .failed: "xmark.octagon.fill"
        case .queued, .uploading, .cancelled: nil
        }
    }

    /// The colour role of the status line.
    static func statusColor(_ status: MonarchUploadStatus) -> MonarchColorRole {
        switch status {
        case .done: \.successStrong
        case .failed: \.dangerStrong
        case .queued, .uploading, .cancelled: \.textMuted
        }
    }

    /// Whether retry is offered: for a failed or cancelled upload that can be retried.
    static func showsRetry(_ item: MonarchUploadQueueItem, hasHandler: Bool) -> Bool {
        hasHandler && item.isRetryable && (item.status == .failed || item.status == .cancelled)
    }

    /// Whether cancel is offered: for a queued or uploading file.
    static func showsCancel(_ item: MonarchUploadQueueItem, hasHandler: Bool) -> Bool {
        hasHandler && item.status.isActive
    }

    /// Whether remove is offered: for a file that ended.
    static func showsRemove(_ item: MonarchUploadQueueItem, hasHandler: Bool) -> Bool {
        hasHandler && !item.status.isActive
    }

    /// The statuses by id, to compare with the next draw.
    static func statuses(_ items: [MonarchUploadQueueItem]) -> [String: MonarchUploadStatus] {
        Dictionary(items.map { ($0.id, $0.status) }, uniquingKeysWith: { _, last in last })
    }

    /// What is announced when the items change: each upload that finished or
    /// failed, then, when the last active upload of several ended, the result.
    static func announcement(
        previous: [String: MonarchUploadStatus],
        items: [MonarchUploadQueueItem],
        labels: MonarchUploadQueueLabels
    ) -> String? {
        var parts: [String] = []
        var anEndedUpload = false
        for item in items {
            let was = previous[item.id]
            guard was != item.status else { continue }
            if item.status == .done {
                parts.append(labels.announceDone?(item.name) ?? MonarchUploadQueueStrings.announceDone(item.name))
            }
            if item.status == .failed {
                parts.append(
                    labels.announceFailed?(item.name, item.errorMessage)
                        ?? MonarchUploadQueueStrings.announceFailed(item.name, item.errorMessage)
                )
            }
            if let was, was.isActive, !item.status.isActive { anEndedUpload = true }
        }
        let counts = MonarchUploadQueueCounts(items)
        if anEndedUpload && counts.active == 0 && items.count > 1 {
            parts.append(labels.announceFinished?(counts) ?? MonarchUploadQueueStrings.announceFinished(counts))
        }
        return parts.isEmpty ? nil : parts.joined(separator: " ")
    }
}
