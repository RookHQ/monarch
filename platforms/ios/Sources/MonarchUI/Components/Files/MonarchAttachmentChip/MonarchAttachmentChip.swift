import SwiftUI

/// One attached file: its type icon, name, size, upload progress or
/// failure, and a remove button.
///
/// ```swift
/// MonarchAttachmentChip(
///     "Q3 report.pdf",
///     size: 2_400_000,
///     status: .uploading,
///     progress: 0.4,
///     onRemove: { attachments.remove(id) }
/// )
/// ```
///
/// Use it in a composer, a form or a reply, alone or in a
/// ``MonarchFlowLayout``. The app does the upload and reports the status
/// and progress. Once uploaded, a `url` turns the name into a link.
///
/// Accessibility: the name is one element whose value is the line under it
/// (the size, the percentage or the failure). The progress bar is a
/// standard `ProgressView` named "Uploading" and the file name. The retry
/// and remove buttons are separate buttons named with the file name, with
/// 44-point touch targets. A failure is announced. The type icon is
/// decorative. Long names are shortened in the middle, so the extension
/// stays visible.
public struct MonarchAttachmentChip: View {
    private let name: String
    private let size: Int64?
    private let contentType: String?
    private let status: MonarchUploadStatus
    private let progress: Double?
    private let errorMessage: String?
    private let url: URL?
    private let labels: MonarchAttachmentChipLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onRemove: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.monarchLowercaseKilobyte) private var lowercaseKilobyte

    /// An attachment chip.
    ///
    /// - Parameters:
    ///   - name: The file name.
    ///   - size: The size in bytes, shown once uploaded.
    ///   - contentType: The MIME type; with the name it picks the type icon.
    ///   - status: Where the upload is.
    ///   - progress: The uploaded part, 0 through 1, while uploading. `nil` is unknown.
    ///   - errorMessage: Why the upload failed, shown in place of the size.
    ///   - url: Opens the file once uploaded. Only http, https and mailto URLs are opened.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Retries a failed upload. Adds a retry button to the failed status.
    ///   - onRemove: Removes the attachment. Adds a remove button.
    public init(
        _ name: String,
        size: Int64? = nil,
        contentType: String? = nil,
        status: MonarchUploadStatus = .done,
        progress: Double? = nil,
        errorMessage: String? = nil,
        url: URL? = nil,
        labels: MonarchAttachmentChipLabels = MonarchAttachmentChipLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onRemove: (@MainActor () -> Void)? = nil
    ) {
        self.name = name
        self.size = size
        self.contentType = contentType
        self.status = status
        self.progress = progress
        self.errorMessage = errorMessage
        self.url = url
        self.labels = labels
        self.onRetry = onRetry
        self.onRemove = onRemove
    }

    private var details: String? {
        let text = MonarchAttachmentChipTokens.details(
            status: status,
            size: size,
            progress: progress,
            errorMessage: errorMessage,
            labels: labels,
            locale: locale
        )
        // "46.9 kB" where the web writes the size with a lowercase k (the AI message), "46.9 KB" elsewhere.
        return lowercaseKilobyte ? text?.replacingOccurrences(of: " KB", with: " kB") : text
    }

    private var hasButtons: Bool {
        onRemove != nil || MonarchAttachmentChipTokens.showsRetry(status, hasHandler: onRetry != nil)
    }

    /// The content of the view.
    public var body: some View {
        HStack(spacing: MonarchAttachmentChipTokens.spacing) {
            MonarchFileTypeIcon(fileName: name, contentType: contentType, size: .sm, isDecorative: true)
            textColumn
            if MonarchAttachmentChipTokens.showsRetry(status, hasHandler: onRetry != nil), let onRetry {
                iconButton(
                    MonarchAttachmentChipTokens.retrySymbol,
                    label: labels.retry?(name) ?? MonarchAttachmentChipStrings.retry(name),
                    action: onRetry
                )
            }
            if let onRemove {
                iconButton(
                    MonarchAttachmentChipTokens.removeSymbol,
                    label: labels.remove?(name) ?? MonarchAttachmentChipStrings.remove(name),
                    action: onRemove
                )
            }
        }
        .padding(.leading, MonarchAttachmentChipTokens.leadingPadding)
        .padding(.trailing, hasButtons ? 0 : MonarchAttachmentChipTokens.leadingPadding)
        .padding(.vertical, MonarchAttachmentChipTokens.verticalPadding)
        .background(MonarchShapes.control.fill(theme.colors[role: MonarchAttachmentChipTokens.fill]))
        .overlay(
            MonarchShapes.control
                .strokeBorder(
                    theme.colors[role: MonarchAttachmentChipTokens.border],
                    lineWidth: MonarchShapes.borderWidth
                )
                .allowsHitTesting(false)
        )
        .accessibilityElement(children: .contain)
        .onChange(of: status) { old, new in
            if MonarchAttachmentChipTokens.announcesFailure(from: old, to: new), let details {
                AccessibilityNotification.Announcement(details).post()
            }
        }
    }

    private var textColumn: some View {
        VStack(alignment: .leading, spacing: MonarchAttachmentChipTokens.lineSpacing) {
            nameAndDetails
            if status == .uploading {
                progressBar
            }
        }
        .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
    }

    @ViewBuilder private var nameAndDetails: some View {
        let link = MonarchAttachmentChipTokens.link(url, status: status)
        let text = VStack(alignment: .leading, spacing: MonarchAttachmentChipTokens.lineSpacing) {
            Text(name)
                .monarchTextStyle(.label)
                .lineLimit(1)
                .truncationMode(.middle)
                .foregroundStyle(
                    link == nil
                        ? theme.colors[role: MonarchAttachmentChipTokens.nameColor(status)] : theme.colors.textLink
                )
                .underline(link != nil)
            if let details {
                Text(details)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAttachmentChipTokens.detailsColor(status)])
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        if let link {
            Link(destination: link) {
                text
                    .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(name)
            .accessibilityValue(details ?? "")
        } else {
            text
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(name)
                .accessibilityValue(details ?? "")
        }
    }

    @ViewBuilder private var progressBar: some View {
        let label = labels.uploading?(name) ?? MonarchAttachmentChipStrings.uploading(name)
        if let fraction = MonarchAttachmentChipTokens.fraction(progress) {
            ProgressView(value: fraction)
                .progressViewStyle(.monarchLinear)
                .accessibilityLabel(label)
        } else {
            ProgressView()
                .progressViewStyle(.monarchLinear)
                .accessibilityLabel(label)
        }
    }

    private func iconButton(_ symbol: String, label: String, action: @escaping @MainActor () -> Void) -> some View {
        Button(action: action) {
            MonarchIcon(systemName: symbol, size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityLabel(label)
    }
}

#if DEBUG
    #Preview("MonarchAttachmentChip") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAttachmentChip("Q3 report.pdf", size: 2_400_000, onRemove: {})
                MonarchAttachmentChip("photo.jpg", status: .uploading, progress: 0.4, onRemove: {})
                MonarchAttachmentChip("notes.txt", status: .queued, onRemove: {})
                MonarchAttachmentChip("big.mov", status: .failed, onRetry: {}, onRemove: {})
                MonarchAttachmentChip("draft.docx", status: .cancelled)
            }
        }
    }
#endif

private struct MonarchLowercaseKilobyteKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Writes file sizes in kilobytes as "kB", the way the web AI message does.
    var monarchLowercaseKilobyte: Bool {
        get { self[MonarchLowercaseKilobyteKey.self] }
        set { self[MonarchLowercaseKilobyteKey.self] = newValue }
    }
}
