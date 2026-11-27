import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the pure state logic of ``MonarchDownloadReportButton``.
enum MonarchDownloadReportButtonTokens {
    typealias Format = MonarchDownloadReportFormat

    /// Where a download is.
    enum Phase: Equatable {
        case idle
        case pending(Format)
        case exporting(Format, URL)
        case done(Format)
        case queued(Format, String)
        case failed(Format, String)

        /// The format of the download, if one has started.
        var format: Format? {
            switch self {
            case .idle: nil
            case .pending(let format), .exporting(let format, _), .done(let format): format
            case .queued(let format, _), .failed(let format, _): format
            }
        }

        /// Whether the button is busy and must not start another download.
        var isBusy: Bool {
            switch self {
            case .pending, .exporting: true
            default: false
            }
        }
    }

    /// The SF Symbol before the title.
    static let symbol = "arrow.down.to.line"
    /// The SF Symbol after the title when a menu opens.
    static let menuSymbol = "chevron.down"
    /// The gap between the button, the progress bar and the notes.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The colour of the queued note.
    static var noteColor: MonarchColorRole { \.textMuted }

    /// The button title: "Download CSV" with one format, "Download" with several.
    static func title(formats: [Format], labels: MonarchDownloadReportButtonLabels) -> String {
        if formats.count == 1, let format = formats.first {
            return labels.single?(format.label) ?? MonarchDownloadReportButtonStrings.single(format.label)
        }
        return labels.label ?? MonarchDownloadReportButtonStrings.label
    }

    /// The phase after the app's download returned.
    static func phase(
        after result: MonarchDownloadReportResult,
        format: Format,
        labels: MonarchDownloadReportButtonLabels
    )
        -> Phase
    {
        switch result {
        case .downloaded: .done(format)
        case .queued(let message):
            .queued(format, message ?? labels.queued ?? MonarchDownloadReportButtonStrings.queued)
        case .file(let url): .exporting(format, url)
        }
    }

    /// The phase after the app's download threw.
    static func phase(
        afterError error: any Error,
        format: Format,
        message: String?,
        labels: MonarchDownloadReportButtonLabels
    ) -> Phase {
        .failed(
            format,
            message ?? labels.error?(format.label) ?? MonarchDownloadReportButtonStrings.error(format.label)
        )
    }

    /// The phase after the system save dialog finished. A cancelled save
    /// returns to idle; any other failure is shown.
    static func phase(
        afterExport result: Result<URL, any Error>,
        format: Format,
        labels: MonarchDownloadReportButtonLabels
    ) -> Phase {
        switch result {
        case .success: return .done(format)
        case .failure(let error):
            if (error as? CocoaError)?.code == .userCancelled { return .idle }
            return phase(afterError: error, format: format, message: nil, labels: labels)
        }
    }

    /// What VoiceOver reads as the button's value while it is busy.
    static func value(for phase: Phase, labels: MonarchDownloadReportButtonLabels) -> String {
        guard phase.isBusy, let format = phase.format else { return "" }
        return labels.preparing?(format.label) ?? MonarchDownloadReportButtonStrings.preparing(format.label)
    }

    /// What is announced when the phase changes, or `nil`.
    static func announcement(for phase: Phase, labels: MonarchDownloadReportButtonLabels) -> String? {
        switch phase {
        case .idle, .exporting: nil
        case .pending(let format):
            labels.preparing?(format.label) ?? MonarchDownloadReportButtonStrings.preparing(format.label)
        case .done(let format):
            labels.downloaded?(format.label) ?? MonarchDownloadReportButtonStrings.downloaded(format.label)
        case .queued(_, let message), .failed(_, let message): message
        }
    }

    /// The progress to draw, clamped to 0...1, or `nil` when it is not shown.
    static func progress(_ progress: Double?, phase: Phase) -> Double? {
        guard phase.isBusy, let progress, progress.isFinite else { return nil }
        return min(max(progress, 0), 1)
    }

    /// The default file name for a URL, without its folder.
    static func filename(_ url: URL) -> String { url.lastPathComponent }
}
