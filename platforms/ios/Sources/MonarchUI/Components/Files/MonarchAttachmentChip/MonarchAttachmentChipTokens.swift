import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchAttachmentChip``.
enum MonarchAttachmentChipTokens {
    /// The gap between the icon, the text and the buttons.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the name, the details and the progress bar.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// Padding before the icon.
    static let leadingPadding: CGFloat = MonarchTokens.Space.space2_5
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the remove button.
    static let removeSymbol = "xmark"
    /// The SF Symbol of the retry button.
    static let retrySymbol = "arrow.clockwise"
    /// The URL schemes the name may open.
    static let safeSchemes: Set<String> = ["http", "https", "mailto"]
    /// The fill of the chip.
    static var fill: MonarchColorRole { \.bgSurface }
    /// The border of the chip.
    static var border: MonarchColorRole { \.borderDefault }

    /// A progress between 0 and 1, or `nil` for unknown.
    static func fraction(_ progress: Double?) -> Double? {
        guard let progress, progress.isFinite else { return nil }
        return min(max(progress, 0), 1)
    }

    /// A size in bytes in the locale's units, such as "2.4 MB".
    static func formattedSize(_ bytes: Int64, locale: Locale) -> String {
        MonarchFormats.fileSize(Int64(bytes), locale: locale)
    }

    /// The line under the name: the percentage while uploading, the state
    /// while queued, failed or cancelled, otherwise the size.
    static func details(
        status: MonarchUploadStatus,
        size: Int64?,
        progress: Double?,
        errorMessage: String?,
        labels: MonarchAttachmentChipLabels,
        locale: Locale = .current
    ) -> String? {
        switch status {
        case .uploading:
            return fraction(progress).map {
                $0.formatted(.percent.precision(.fractionLength(0)).locale(locale))
            }
        case .queued:
            return labels.queued ?? MonarchAttachmentChipStrings.queued
        case .failed:
            return errorMessage.flatMap { $0.isEmpty ? nil : $0 }
                ?? labels.failed ?? MonarchAttachmentChipStrings.failed
        case .cancelled:
            return labels.cancelled ?? MonarchAttachmentChipStrings.cancelled
        case .done:
            return size.map { formattedSize($0, locale: locale) }
        }
    }

    /// The colour role of the line under the name.
    static func detailsColor(_ status: MonarchUploadStatus) -> MonarchColorRole {
        status == .failed ? \.dangerStrong : \.textMuted
    }

    /// The colour role of the name: muted once cancelled.
    static func nameColor(_ status: MonarchUploadStatus) -> MonarchColorRole {
        status == .cancelled ? \.textMuted : \.textDefault
    }

    /// The URL the name opens: only once uploaded, and only http, https and mailto.
    static func link(_ url: URL?, status: MonarchUploadStatus) -> URL? {
        guard status == .done, let url, let scheme = url.scheme?.lowercased(), safeSchemes.contains(scheme) else {
            return nil
        }
        return url
    }

    /// Whether the retry button is shown: only for a failed upload the app can retry.
    static func showsRetry(_ status: MonarchUploadStatus, hasHandler: Bool) -> Bool {
        status == .failed && hasHandler
    }

    /// Whether a change of status is announced: only a failure.
    static func announcesFailure(from old: MonarchUploadStatus, to new: MonarchUploadStatus) -> Bool {
        old != .failed && new == .failed
    }
}
