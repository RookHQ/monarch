import QuickLook
import SwiftUI

/// A "View receipt" button that opens an expense's receipt in Quick Look,
/// fetching a private file only when someone asks to see it.
///
/// ```swift
/// // A file the app already has on disk:
/// MonarchReceiptPreview(url: localReceiptURL)
///
/// // A private file fetched with the app's own authorised request:
/// MonarchReceiptPreview(
///     onClose: { try? FileManager.default.removeItem(at: $0) },
///     load: { try await receipts.download(expense.receiptID) }  // returns a local file URL
/// )
/// ```
///
/// Quick Look shows images (including HEIC), PDFs and most document types,
/// with the system's share and markup controls. Monarch does not fetch
/// anything: `load` is the app's code, run only when the button is pressed
/// and again on Try again. It must return a local file URL. `onClose` gets
/// that URL back when the preview closes, so a temporary copy can be
/// deleted. With neither a file nor a loader, "No receipt" is shown instead
/// of the button.
///
/// Accessibility: the button is a standard ``MonarchButton``; while the file
/// loads it shows a spinner and is read as "Loading receipt". A failed load
/// shows ``MonarchInlineErrorRetry`` under the button and is announced.
/// "No receipt" is plain text.
public struct MonarchReceiptPreview: View {
    private let url: URL?
    private let load: (@Sendable () async throws -> URL)?
    private let systemImage: String?
    private let variant: MonarchButtonVariant
    private let size: MonarchButtonSize
    private let labels: MonarchReceiptPreviewLabels
    private let onClose: (@MainActor (URL) -> Void)?

    @State private var previewURL: URL?
    @State private var request = 0
    @State private var isLoading = false
    @State private var hasFailed = false
    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchStartsPresented) private var startsPresented

    /// A receipt button for a file the app already has.
    ///
    /// - Parameters:
    ///   - url: The local file of the receipt, or `nil` when there is none.
    ///   - systemImage: Replaces the paperclip symbol before the title.
    ///   - variant: The visual weight of the button.
    ///   - size: The height and padding of the button.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClose: Runs with the file's URL after the preview closes.
    public init(
        url: URL?,
        systemImage: String? = nil,
        variant: MonarchButtonVariant = .ghost,
        size: MonarchButtonSize = .sm,
        labels: MonarchReceiptPreviewLabels = MonarchReceiptPreviewLabels(),
        onClose: (@MainActor (URL) -> Void)? = nil
    ) {
        self.url = url
        self.load = nil
        self.systemImage = systemImage
        self.variant = variant
        self.size = size
        self.labels = labels
        self.onClose = onClose
    }

    /// A receipt button that fetches the file when it is pressed.
    ///
    /// - Parameters:
    ///   - systemImage: Replaces the paperclip symbol before the title.
    ///   - variant: The visual weight of the button.
    ///   - size: The height and padding of the button.
    ///   - labels: Replacements for the built-in strings.
    ///   - onClose: Runs with the loaded file's URL after the preview closes,
    ///     for example to delete a temporary copy.
    ///   - load: The app's code that fetches the receipt and returns a local
    ///     file URL. Run on each press and on Try again.
    public init(
        systemImage: String? = nil,
        variant: MonarchButtonVariant = .ghost,
        size: MonarchButtonSize = .sm,
        labels: MonarchReceiptPreviewLabels = MonarchReceiptPreviewLabels(),
        onClose: (@MainActor (URL) -> Void)? = nil,
        load: @escaping @Sendable () async throws -> URL
    ) {
        self.url = nil
        self.load = load
        self.systemImage = systemImage
        self.variant = variant
        self.size = size
        self.labels = labels
        self.onClose = onClose
    }

    /// The content of the view.
    public var body: some View {
        let action = MonarchReceiptPreviewTokens.action(url: url, hasLoader: load != nil)
        if action == .none {
            Text(labels.none ?? MonarchReceiptPreviewStrings.none)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        } else {
            VStack(alignment: .leading, spacing: MonarchReceiptPreviewTokens.errorSpacing) {
                MonarchButton(
                    labels.view ?? MonarchReceiptPreviewStrings.view,
                    systemImage: systemImage ?? MonarchReceiptPreviewTokens.symbol,
                    variant: variant,
                    size: size,
                    isLoading: isLoading,
                    loadingLabel: labels.loading ?? MonarchReceiptPreviewStrings.loading
                ) {
                    open(action)
                }
                if hasFailed {
                    MonarchInlineErrorRetry(
                        labels.loadError ?? MonarchReceiptPreviewStrings.loadError,
                        retryLabel: labels.retry
                    ) {
                        open(action)
                    }
                }
            }
            .quickLookPreview($previewURL)
            .onAppear { if startsPresented { open(action) } }
            .onChange(of: previewURL) { old, new in
                if let closed = MonarchReceiptPreviewTokens.closedFile(old: old, new: new) {
                    onClose?(closed)
                }
            }
            .task(id: request) { await fetch() }
        }
    }

    private func open(_ action: MonarchReceiptPreviewTokens.Action) {
        switch action {
        case .preview(let file):
            previewURL = file
        case .load:
            request += 1
        case .none:
            break
        }
    }

    /// Runs the app's loader for the latest press. A newer press, or the
    /// view leaving the screen, cancels it.
    private func fetch() async {
        guard request > 0, let load else { return }
        isLoading = true
        hasFailed = false
        do {
            let file = try await load()
            guard !Task.isCancelled else { return }
            isLoading = false
            previewURL = file
        } catch {
            guard !Task.isCancelled else { return }
            isLoading = false
            hasFailed = true
            AccessibilityNotification.Announcement(
                labels.loadError ?? MonarchReceiptPreviewStrings.loadError
            ).post()
        }
    }
}

#if DEBUG
    #Preview("MonarchReceiptPreview") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchReceiptPreview(url: URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("r.pdf"))
                MonarchReceiptPreview(variant: .secondary, size: .md) {
                    URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("r.jpg")
                }
                MonarchReceiptPreview(url: nil)
            }
        }
    }
#endif
