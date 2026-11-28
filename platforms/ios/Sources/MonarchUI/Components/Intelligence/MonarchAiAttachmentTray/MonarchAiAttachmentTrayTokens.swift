import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiAttachmentTray``.
enum MonarchAiAttachmentTrayTokens {
    /// The gap between files.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The widest a file's chip grows before Dynamic Type, so a long name does not fill the row.
    static let maxTileWidth: CGFloat = MonarchTokens.Space.space24 * 2.5

    /// Where a file is: failed (an error or the flag), uploading (progress below 1), else done.
    static func status(_ file: MonarchAiAttachmentTrayFile) -> MonarchUploadStatus {
        if file.isFailed || file.errorMessage != nil { return .failed }
        if let progress = file.progress, progress < 1 { return .uploading }
        return .done
    }

    /// The progress the chip shows: clamped to 0 through 1 while uploading, else none.
    static func progress(_ file: MonarchAiAttachmentTrayFile) -> Double? {
        guard status(file) == .uploading, let progress = file.progress, progress.isFinite else { return nil }
        return min(1, max(0, progress))
    }

    /// The file type in a few capitals, "PDF": the name's extension.
    static func extensionBadge(_ name: String) -> String {
        let ext = (name as NSString).pathExtension
        return ext.isEmpty ? "FILE" : String(ext.uppercased().prefix(4))
    }

    /// The line under the name: the size, with "Uploading 10%" after it while the file uploads.
    static func meta(_ file: MonarchAiAttachmentTrayFile, locale: Locale, labels: MonarchAiAttachmentTrayLabels)
        -> String?
    {
        let size = file.size.map { MonarchFormats.fileSize($0, locale: locale) }
        switch status(file) {
        case .uploading:
            let percent = progress(file).map {
                $0.formatted(.percent.precision(.fractionLength(0)).locale(locale))
            }
            let uploading = MonarchAiAttachmentTrayStrings.uploading
            return [size, [uploading, percent].compactMap { $0 }.joined(separator: " ")]
                .compactMap { $0 }.joined(separator: " ")
        case .done:
            return size
        default:
            return nil
        }
    }

    /// The spoken name of the tray.
    static func label(_ labels: MonarchAiAttachmentTrayLabels) -> String {
        labels.label ?? MonarchAiAttachmentTrayStrings.label
    }

    /// What one file was the last time the tray was drawn.
    struct Snapshot: Equatable {
        /// The file name.
        let name: String
        /// The status.
        let status: MonarchUploadStatus
    }

    /// The status and name of every file by id, to compare with the next update.
    static func snapshot(_ files: [MonarchAiAttachmentTrayFile]) -> [String: Snapshot] {
        Dictionary(
            files.map { ($0.id, Snapshot(name: $0.name, status: status($0))) },
            uniquingKeysWith: { _, last in last }
        )
    }

    /// The announcements for a change from `previous` to `files`: an upload that finished,
    /// and a file removed with its button (`removing` holds those ids). Failures are announced
    /// by the file's chip. Nothing is announced for the first state.
    static func announcements(
        previous: [String: Snapshot]?,
        files: [MonarchAiAttachmentTrayFile],
        removing: Set<String>,
        labels: MonarchAiAttachmentTrayLabels
    ) -> [String] {
        guard let previous else { return [] }
        var lines: [String] = []
        for file in files where previous[file.id]?.status == .uploading && status(file) == .done {
            lines.append(labels.ready?(file.name) ?? MonarchAiAttachmentTrayStrings.ready(file.name))
        }
        let present = Set(files.map(\.id))
        for id in removing.sorted() where !present.contains(id) {
            guard let name = previous[id]?.name else { continue }
            lines.append(labels.removed?(name) ?? MonarchAiAttachmentTrayStrings.removed(name))
        }
        return lines
    }
}
