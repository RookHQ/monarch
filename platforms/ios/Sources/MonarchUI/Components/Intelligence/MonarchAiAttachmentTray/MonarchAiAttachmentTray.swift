import MonarchTokens
import SwiftUI

/// The files attached to a message for an assistant, in a row that wraps onto
/// new rows: each with its type, name and size, upload progress, failure and
/// retry, and a remove button.
///
/// ```swift
/// MonarchAiAttachmentTray(
///     attachments,
///     onRetry: { id in uploader.retry(id) },
///     onRemove: { id in attachments.removeAll { $0.id == id } }
/// )
/// ```
///
/// Each file is a ``MonarchAttachmentChip``. The app uploads the files and
/// reports progress and failures in `files`; the tray reports which file to
/// retry or remove. Put it above the prompt field of a composer. With no
/// files it draws nothing.
///
/// Accessibility: the tray is a container named "Attachments". Each file's
/// name is read with its size, percentage or failure; retry and remove are
/// buttons named with the file name, with 44-point touch targets. A
/// finished upload and a file removed with its button are announced; a
/// failure is announced by the file's chip.
public struct MonarchAiAttachmentTray: View {
    private let files: [MonarchAiAttachmentTrayFile]
    private let labels: MonarchAiAttachmentTrayLabels
    private let onRetry: (@MainActor (String) -> Void)?
    private let onRemove: @MainActor (String) -> Void

    @State private var previous: [String: MonarchAiAttachmentTrayTokens.Snapshot]?
    @State private var removing: Set<String> = []
    @AccessibilityFocusState private var focused: String?
    @State private var pendingRemoval: (id: String, index: Int)?
    @ScaledMetric(relativeTo: .body) private var maxTileWidth = MonarchAiAttachmentTrayTokens.maxTileWidth

    /// An attachment tray.
    ///
    /// - Parameters:
    ///   - files: The attached files, in the order they were added.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries a failed upload, by file id. Without it failed files have no retry button.
    ///   - onRemove: Removes a file, by file id. Take it out of `files`.
    public init(
        _ files: [MonarchAiAttachmentTrayFile],
        labels: MonarchAiAttachmentTrayLabels = MonarchAiAttachmentTrayLabels(),
        onRetry: (@MainActor (String) -> Void)? = nil,
        onRemove: @escaping @MainActor (String) -> Void
    ) {
        self.files = files
        self.labels = labels
        self.onRetry = onRetry
        self.onRemove = onRemove
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if !files.isEmpty {
                MonarchFlowLayout(
                    spacing: MonarchAiAttachmentTrayTokens.spacing,
                    alignment: .top
                ) {
                    ForEach(files) { file in
                        chip(file)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .contain)
                .accessibilityLabel(MonarchAiAttachmentTrayTokens.label(labels))
            }
        }
        .onAppear { previous = MonarchAiAttachmentTrayTokens.snapshot(files) }
        .onChange(of: files) { _, newFiles in
            announce(newFiles)
        }
    }

    private func chip(_ file: MonarchAiAttachmentTrayFile) -> some View {
        let id = file.id
        var retry: (@MainActor () -> Void)?
        if let onRetry {
            retry = { onRetry(id) }
        }
        return MonarchAiAttachmentTile(
            file: file,
            labels: labels,
            onRetry: retry,
            onRemove: {
                removing.insert(id)
                pendingRemoval = (id, files.firstIndex { $0.id == id } ?? 0)
                onRemove(id)
            }
        )
        .frame(maxWidth: maxTileWidth)
        .accessibilityFocused($focused, equals: id)
    }

    private func announce(_ newFiles: [MonarchAiAttachmentTrayFile]) {
        let lines = MonarchAiAttachmentTrayTokens.announcements(
            previous: previous,
            files: newFiles,
            removing: removing,
            labels: labels
        )
        previous = MonarchAiAttachmentTrayTokens.snapshot(newFiles)
        // As on the web: after a file is removed, focus stays in the tray, on the next file.
        if let pending = pendingRemoval, !newFiles.contains(where: { $0.id == pending.id }) {
            pendingRemoval = nil
            focused = MonarchFocusAfterRemoval.target(removedIndex: pending.index, remaining: newFiles.map(\.id))
        }
        removing = removing.intersection(Set(newFiles.map(\.id)))
        guard !lines.isEmpty else { return }
        AccessibilityNotification.Announcement(lines.joined(separator: ". ")).post()
    }
}

#if DEBUG
    #Preview("MonarchAiAttachmentTray") {
        MonarchPreviewMatrix {
            MonarchAiAttachmentTray(
                [
                    MonarchAiAttachmentTrayFile(
                        id: "a",
                        name: "Q3 report.pdf",
                        size: 2_400_000,
                        contentType: "application/pdf"
                    ),
                    MonarchAiAttachmentTrayFile(
                        id: "b",
                        name: "photo.png",
                        size: 840_000,
                        contentType: "image/png",
                        progress: 0.45
                    ),
                    MonarchAiAttachmentTrayFile(id: "c", name: "data.csv", errorMessage: "File too large"),
                ],
                onRetry: { _ in },
                onRemove: { _ in }
            )
        }
    }
#endif

/// One file of the tray as the web draws it: the extension in a small tile, the name, the size (with "Uploading"
/// and the percentage while it uploads) and, for a failed upload, a red tile with the reason, Retry and Remove.
private struct MonarchAiAttachmentTile: View {
    let file: MonarchAiAttachmentTrayFile
    let labels: MonarchAiAttachmentTrayLabels
    let onRetry: (@MainActor () -> Void)?
    let onRemove: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let status = MonarchAiAttachmentTrayTokens.status(file)
        let failed = status == .failed
        let shape = MonarchShapes.control
        HStack(alignment: .center, spacing: MonarchTokens.Space.space2) {
            Text(MonarchAiAttachmentTrayTokens.extensionBadge(file.name))
                .monarchTextStyle(.caption, weight: .semibold)
                .foregroundStyle(theme.colors.textMuted)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
                .frame(width: MonarchTokens.Space.space10, height: MonarchTokens.Space.space10)
                .background(
                    RoundedRectangle(cornerRadius: MonarchTokens.Space.space2, style: .continuous)
                        .fill(theme.colors.bgSubtle)
                )
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchTokens.Space.space0_5) {
                Text(file.name)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .lineLimit(1)
                    .truncationMode(.middle)
                if failed {
                    HStack(alignment: .top, spacing: MonarchTokens.Space.space1) {
                        MonarchIcon(systemName: "xmark.circle", size: .sm).imageScale(.small)
                        Text(file.errorMessage ?? MonarchAttachmentChipStrings.failed)
                            .monarchTextStyle(.caption)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .foregroundStyle(theme.colors.dangerStrong)
                } else if let meta = MonarchAiAttachmentTrayTokens.meta(file, locale: locale, labels: labels) {
                    Text(meta)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if status == .uploading {
                    if let fraction = MonarchAiAttachmentTrayTokens.progress(file) {
                        ProgressView(value: fraction).progressViewStyle(.monarchLinear)
                    } else {
                        ProgressView().progressViewStyle(.monarchLinear)
                    }
                }
                if failed, let onRetry {
                    Button(action: onRetry) {
                        HStack(spacing: MonarchTokens.Space.space1) {
                            MonarchIcon(systemName: "arrow.clockwise", size: .sm)
                            Text(MonarchAiAttachmentTrayStrings.retry).monarchTextStyle(.label).lineLimit(1).fixedSize()
                        }
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    .accessibilityLabel(
                        labels.chip.retry?(file.name) ?? MonarchAttachmentChipStrings.retry(file.name)
                    )
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(file.name)
            .accessibilityValue(
                MonarchAiAttachmentTrayTokens.meta(file, locale: locale, labels: labels)
                    ?? (failed ? (file.errorMessage ?? MonarchAttachmentChipStrings.failed) : "")
            )
            Button(action: onRemove) {
                MonarchIcon(systemName: "xmark", size: .sm)
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .accessibilityLabel(
                labels.chip.remove?(file.name) ?? MonarchAttachmentChipStrings.remove(file.name)
            )
        }
        .padding(.leading, MonarchTokens.Space.space2)
        .padding(.vertical, MonarchTokens.Space.space2)
        .background(shape.fill(theme.colors[role: failed ? \.dangerSubtle : \.bgSubtle]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: failed ? \.dangerBorder : \.borderSubtle],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
    }
}
