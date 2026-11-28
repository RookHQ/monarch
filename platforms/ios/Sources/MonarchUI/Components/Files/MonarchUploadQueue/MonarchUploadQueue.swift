import SwiftUI

/// The files being uploaded: progress and status per file, cancel, retry
/// and remove, and a summary with the total progress.
///
/// ```swift
/// MonarchUploadQueue(
///     uploads.items,
///     onCancel: { uploads.cancel($0) },
///     onRetry: { uploads.retry($0) },
///     onRemove: { uploads.remove($0) }
/// )
/// ```
///
/// The app does the transfers and reports status and progress through the
/// items. Transfers that must go on while the app is in the background
/// belong to a background `URLSession` in the app.
///
/// Accessibility: the title is a header. Each row is the file name, read
/// with its size and status; the progress bar is a standard `ProgressView`
/// named "Upload progress of" and the file name. Cancel, retry and remove
/// are separate buttons named with the file name, with 44-point touch
/// targets. Finished and failed uploads are announced, and so is the end
/// of a batch. Status is written in words, so colour is never the only
/// signal.
public struct MonarchUploadQueue: View {
    private let items: [MonarchUploadQueueItem]
    private let showsSummary: Bool
    private let usesBinaryUnits: Bool
    private let labels: MonarchUploadQueueLabels
    private let onCancel: (@MainActor (String) -> Void)?
    private let onRetry: (@MainActor (String) -> Void)?
    private let onRemove: (@MainActor (String) -> Void)?
    private let onClearFinished: (@MainActor () -> Void)?

    @AccessibilityFocusState private var focused: String?
    @State private var pendingRemoval: (id: String, index: Int)?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// An upload queue.
    ///
    /// - Parameters:
    ///   - items: The uploads, in display order.
    ///   - showsSummary: Whether the header with the title, the summary and
    ///     the total progress is shown.
    ///   - usesBinaryUnits: Writes sizes in 1024-based units (the unit the web uses) instead of 1000-based ones.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCancel: Cancels a queued or uploading file, by id. Adds cancel buttons.
    ///   - onRetry: Retries a failed or cancelled file, by id. Adds retry buttons.
    ///   - onRemove: Removes a file that ended from the list, by id. Adds remove buttons.
    ///   - onClearFinished: Removes every file that ended. Adds a header button.
    public init(
        _ items: [MonarchUploadQueueItem],
        showsSummary: Bool = true,
        usesBinaryUnits: Bool = false,
        labels: MonarchUploadQueueLabels = MonarchUploadQueueLabels(),
        onCancel: (@MainActor (String) -> Void)? = nil,
        onRetry: (@MainActor (String) -> Void)? = nil,
        onRemove: (@MainActor (String) -> Void)? = nil,
        onClearFinished: (@MainActor () -> Void)? = nil
    ) {
        self.items = items
        self.showsSummary = showsSummary
        self.usesBinaryUnits = usesBinaryUnits
        self.labels = labels
        self.onCancel = onCancel
        self.onRetry = onRetry
        self.onRemove = onRemove
        self.onClearFinished = onClearFinished
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if showsSummary {
                header
                    .padding(.horizontal, MonarchUploadQueueTokens.cardPadding)
                    .padding(.vertical, MonarchUploadQueueTokens.sectionSpacing)
                if !items.isEmpty { MonarchDivider() }
            }
            VStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    if index > 0 {
                        MonarchDivider()
                    }
                    MonarchUploadQueueRow(
                        item: item,
                        labels: labels,
                        usesBinaryUnits: usesBinaryUnits,
                        onCancel: onCancel,
                        onRetry: onRetry,
                        onRemove: removal(at: index)
                    )
                    .accessibilityFocused($focused, equals: item.id)
                }
            }
        }
        .background(
            RoundedRectangle(cornerRadius: MonarchUploadQueueTokens.cardRadius, style: .continuous)
                .fill(theme.colors.bgSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: MonarchUploadQueueTokens.cardRadius, style: .continuous)
                .strokeBorder(theme.colors.borderSubtle, lineWidth: 1)
        )
        .accessibilityElement(children: .contain)
        .onChange(of: items.map(\.id)) { _, ids in
            // As on the web: after a row is removed, focus moves to the next row, else the heading.
            guard let pending = pendingRemoval, !ids.contains(pending.id) else { return }
            pendingRemoval = nil
            focused =
                MonarchFocusAfterRemoval.target(removedIndex: pending.index, remaining: ids)
                ?? MonarchUploadQueueTokens.headingFocusID
        }
        .onChange(of: items) { old, new in
            let message = MonarchUploadQueueTokens.announcement(
                previous: MonarchUploadQueueTokens.statuses(old),
                items: new,
                labels: labels
            )
            if let message {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    /// Removes a row, remembering where it was so focus can move on after it leaves.
    private func removal(at index: Int) -> (@MainActor (String) -> Void)? {
        guard let onRemove else { return nil }
        return { id in
            pendingRemoval = (id, index)
            onRemove(id)
        }
    }

    private var header: some View {
        let counts = MonarchUploadQueueCounts(items)
        let total = MonarchUploadQueueTokens.totalProgress(items)
        return VStack(alignment: .leading, spacing: MonarchUploadQueueTokens.lineSpacing) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: MonarchUploadQueueTokens.lineSpacing) {
                    Text(labels.title ?? MonarchUploadQueueStrings.title)
                        .monarchTextStyle(.subheading)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityFocused($focused, equals: MonarchUploadQueueTokens.headingFocusID)
                    if !items.isEmpty {
                        Text(labels.summary?(counts) ?? MonarchUploadQueueStrings.summary(counts))
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: MonarchUploadQueueTokens.muted])
                    }
                }
                Spacer(minLength: 0)
                if let onClearFinished, counts.finished > 0 {
                    MonarchButton(
                        labels.clearFinished ?? MonarchUploadQueueStrings.clearFinished,
                        variant: .ghost,
                        size: .sm,
                        action: onClearFinished
                    )
                }
            }
            if counts.active > 0 {
                ProgressView(value: total)
                    .progressViewStyle(.monarchLinear)
                    .accessibilityLabel(labels.totalLabel ?? MonarchUploadQueueStrings.totalLabel)
                    .accessibilityValue(MonarchUploadQueueTokens.percent(total, locale: locale))
            }
        }
    }
}

/// One upload of the queue.
private struct MonarchUploadQueueRow: View {
    let item: MonarchUploadQueueItem
    let labels: MonarchUploadQueueLabels
    let usesBinaryUnits: Bool
    let onCancel: (@MainActor (String) -> Void)?
    let onRetry: (@MainActor (String) -> Void)?
    let onRemove: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    var body: some View {
        let status = MonarchUploadQueueTokens.statusText(item, labels: labels, locale: locale)
        let size = item.size.map { MonarchUploadQueueTokens.formattedSize($0, locale: locale, binary: usesBinaryUnits) }
        HStack(alignment: .top, spacing: MonarchUploadQueueTokens.rowSpacing) {
            MonarchIcon(systemName: MonarchUploadQueueTokens.fileSymbol, size: .sm)
                .foregroundStyle(theme.colors[role: MonarchUploadQueueTokens.muted])
                .frame(width: MonarchUploadQueueTokens.mediaSize, height: MonarchUploadQueueTokens.mediaSize)
                .background(
                    RoundedRectangle(cornerRadius: MonarchUploadQueueTokens.mediaRadius, style: .continuous)
                        .fill(theme.colors.bgMuted)
                )
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchUploadQueueTokens.lineSpacing) {
                text(status: status, size: size)
                if item.status == .uploading {
                    progress
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            actions
        }
        .padding(.horizontal, MonarchUploadQueueTokens.cardPadding)
        .padding(.vertical, MonarchUploadQueueTokens.rowPadding)
    }

    private func text(status: String, size: String?) -> some View {
        VStack(alignment: .leading, spacing: MonarchUploadQueueTokens.lineSpacing) {
            HStack(alignment: .firstTextBaseline) {
                Text(item.name)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .lineLimit(2)
                    .truncationMode(.middle)
                if let size {
                    Text(size)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchUploadQueueTokens.muted])
                }
            }
            HStack(alignment: .firstTextBaseline, spacing: MonarchUploadQueueTokens.lineSpacing) {
                if let symbol = MonarchUploadQueueTokens.statusSymbol(item.status) {
                    MonarchIcon(systemName: symbol, size: .sm)
                }
                Text(status).monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors[role: MonarchUploadQueueTokens.statusColor(item.status)])
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(item.name)
        .accessibilityValue([size, status].compactMap { $0 }.joined(separator: ", "))
    }

    @ViewBuilder private var progress: some View {
        let label = labels.progressLabel?(item.name) ?? MonarchUploadQueueStrings.progressLabel(item.name)
        if let fraction = MonarchUploadQueueTokens.fraction(item.progress) {
            ProgressView(value: fraction)
                .progressViewStyle(.monarchLinear)
                .accessibilityLabel(label)
        } else {
            ProgressView()
                .progressViewStyle(.monarchLinear)
                .accessibilityLabel(label)
        }
    }

    @ViewBuilder private var actions: some View {
        let id = item.id
        HStack(spacing: 0) {
            if MonarchUploadQueueTokens.showsRetry(item, hasHandler: onRetry != nil), let onRetry {
                MonarchButton(
                    labels.retry ?? MonarchUploadQueueStrings.retry,
                    systemImage: MonarchUploadQueueTokens.retrySymbol,
                    variant: .ghost,
                    size: .sm
                ) {
                    onRetry(id)
                }
                .accessibilityLabel(labels.retryLabel?(item.name) ?? MonarchUploadQueueStrings.retryLabel(item.name))
            }
            if MonarchUploadQueueTokens.showsCancel(item, hasHandler: onCancel != nil), let onCancel {
                iconButton(labels.cancel?(item.name) ?? MonarchUploadQueueStrings.cancel(item.name)) {
                    onCancel(id)
                }
            }
            if MonarchUploadQueueTokens.showsRemove(item, hasHandler: onRemove != nil), let onRemove {
                iconButton(labels.remove?(item.name) ?? MonarchUploadQueueStrings.remove(item.name)) {
                    onRemove(id)
                }
            }
        }
    }

    private func iconButton(_ label: String, action: @escaping @MainActor () -> Void) -> some View {
        Button(action: action) {
            MonarchIcon(systemName: MonarchUploadQueueTokens.closeSymbol, size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityLabel(label)
    }
}

#if DEBUG
    #Preview("MonarchUploadQueue") {
        MonarchPreviewMatrix {
            MonarchUploadQueue(
                [
                    MonarchUploadQueueItem(id: "1", name: "Q3 report.pdf", size: 2_400_000, status: .done),
                    MonarchUploadQueueItem(
                        id: "2",
                        name: "photo.jpg",
                        size: 800_000,
                        status: .uploading,
                        progress: 0.4
                    ),
                    MonarchUploadQueueItem(id: "3", name: "notes.txt", status: .queued),
                    MonarchUploadQueueItem(id: "4", name: "big.mov", status: .failed, errorMessage: "Too large"),
                ],
                onCancel: { _ in },
                onRetry: { _ in },
                onRemove: { _ in },
                onClearFinished: {}
            )
        }
    }
#endif
