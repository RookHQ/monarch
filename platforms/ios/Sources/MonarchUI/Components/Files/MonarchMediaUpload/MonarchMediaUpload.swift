import SwiftUI
import UniformTypeIdentifiers

/// A field that holds one image or video: pick it from Photos or Files,
/// see it checked and uploaded, then preview it with Replace and Remove.
///
/// ```swift
/// MonarchMediaUpload(
///     "Cover image",
///     url: $coverURL,
///     maxImageSize: 10_000_000
/// ) { file, context in
///     let stored = try await storage.upload(file.url) { context.reportProgress($0) }
///     return MonarchMediaUploadResult(url: stored)
/// }
/// ```
///
/// The empty field is a ``MonarchFileUpload`` with the system document
/// picker, the system photo picker and drag and drop. A picked file is
/// checked for kind and size, and a video for its length (read from its
/// metadata with AVFoundation) before anything is sent. Monarch does no
/// network work: `onUpload` stores the file, reports progress through the
/// context and returns the stored file's address, which becomes the value.
/// Cancel stops the upload by cancelling its task. Once uploaded, an image
/// is previewed with `AsyncImage` and a video as a poster that opens the
/// system player; a compact picker replaces it and Remove sets the value to
/// `nil`. An error thrown by `onUpload` is shown with its
/// `LocalizedError.errorDescription`, or "The upload failed. Try again."
///
/// Accessibility: the label is a header. The pickers and buttons are
/// standard buttons with 44-point touch targets. Progress is a standard
/// progress view with the file name; refusals and errors are announced and
/// shown with an icon and text. "Image uploaded", "Upload cancelled" and
/// "Image removed" are announced. The preview is an image named "Uploaded
/// image", or a play button for a video.
public struct MonarchMediaUpload: View {
    private let label: String
    @Binding private var url: URL?
    private let kind: MonarchMediaUploadKind?
    private let helper: String?
    private let kinds: Set<MonarchMediaUploadKind>
    private let maxImageSize: Int64?
    private let maxVideoSize: Int64?
    private let maxVideoDuration: TimeInterval?
    private let videoUnavailable: String?
    private let previewLabel: String?
    private let labels: MonarchMediaUploadLabels
    private let onUpload:
        @MainActor (MonarchFileUploadFile, MonarchMediaUploadContext) async throws -> MonarchMediaUploadResult

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @State private var model = MonarchMediaUploadModel()
    @State private var task: Task<Void, Never>?
    @State private var uploadedKind: (url: URL, kind: MonarchMediaUploadKind)?

    /// A media field.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - url: The address of the stored file, or `nil` when empty.
    ///   - kind: The kind of the current file. Guessed from the address when `nil`.
    ///   - helper: Help text under the label.
    ///   - kinds: The kinds that are taken.
    ///   - maxImageSize: The largest image, in bytes.
    ///   - maxVideoSize: The largest video, in bytes.
    ///   - maxVideoDuration: The longest video, in seconds.
    ///   - videoUnavailable: Why videos cannot be added, in the product's words, when `kinds` has no video.
    ///   - previewLabel: The spoken name of the image preview. "Uploaded image" by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - onUpload: Stores the file and returns its address. Throw to show an error.
    public init(
        _ label: String,
        url: Binding<URL?>,
        kind: MonarchMediaUploadKind? = nil,
        helper: String? = nil,
        kinds: Set<MonarchMediaUploadKind> = [.image],
        maxImageSize: Int64? = nil,
        maxVideoSize: Int64? = nil,
        maxVideoDuration: TimeInterval? = nil,
        videoUnavailable: String? = nil,
        previewLabel: String? = nil,
        labels: MonarchMediaUploadLabels = MonarchMediaUploadLabels(),
        onUpload:
            @escaping @MainActor (MonarchFileUploadFile, MonarchMediaUploadContext) async throws ->
            MonarchMediaUploadResult
    ) {
        self.label = label
        _url = url
        self.kind = kind
        self.helper = helper
        self.kinds = kinds
        self.maxImageSize = maxImageSize
        self.maxVideoSize = maxVideoSize
        self.maxVideoDuration = maxVideoDuration
        self.videoUnavailable = videoUnavailable
        self.previewLabel = previewLabel
        self.labels = labels
        self.onUpload = onUpload
    }

    private func shownKind(of url: URL) -> MonarchMediaUploadKind {
        if let kind { return kind }
        if let uploadedKind, uploadedKind.url == url { return uploadedKind.kind }
        return MonarchMediaUploadTokens.kind(of: url)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchMediaUploadTokens.spacing) {
            heading
            if model.phase.isBusy {
                progress
            } else if let url {
                filled(url)
            } else {
                picker(title: MonarchMediaUploadTokens.addTitle(kinds: kinds, labels: labels), variant: .zone)
            }
            if let error = model.error {
                MonarchInlineNotice(error, tone: .danger)
            }
            if !model.warnings.isEmpty {
                MonarchInlineNotice(
                    model.warnings.joined(separator: "\n"),
                    title: labels.warnings ?? MonarchMediaUploadStrings.warnings,
                    tone: .warning
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onDisappear { task?.cancel() }
    }

    private var heading: some View {
        VStack(alignment: .leading, spacing: MonarchMediaUploadTokens.labelSpacing) {
            Text(label)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.labelColor(isEnabled: isEnabled)])
                .accessibilityAddTraits(.isHeader)
            if let helper, !helper.isEmpty {
                Text(helper)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchFormFieldTokens.helperColor(isEnabled: isEnabled)])
            }
        }
    }

    private func picker(title: String, variant: MonarchFileUploadVariant) -> some View {
        let kinds = kinds
        let maxImageSize = maxImageSize
        let maxVideoSize = maxVideoSize
        let labels = labels
        let locale = locale
        var fileLabels = labels.fileUpload
        fileLabels.title = fileLabels.title ?? title
        return MonarchFileUpload(
            allowedContentTypes: MonarchMediaUploadTokens.allowedContentTypes(
                kinds: kinds,
                explainsVideo: videoUnavailable != nil
            ),
            maxFiles: 1,
            hint: MonarchMediaUploadTokens.hint(
                kinds: kinds,
                maxImageSize: maxImageSize,
                maxVideoSize: maxVideoSize,
                maxVideoDuration: maxVideoDuration,
                videoUnavailable: videoUnavailable,
                labels: labels,
                locale: locale
            ),
            systemImage: MonarchMediaUploadTokens.symbol,
            variant: variant,
            allowsPhotos: true,
            labels: fileLabels,
            validate: { file in
                MonarchMediaUploadTokens.refusal(
                    file,
                    kinds: kinds,
                    maxImageSize: maxImageSize,
                    maxVideoSize: maxVideoSize,
                    labels: labels,
                    locale: locale
                )
            },
            onFiles: { accepted, rejected in
                if let file = accepted.first {
                    start(file)
                } else if !rejected.isEmpty {
                    model.clearMessages()
                }
            }
        )
    }

    private var progress: some View {
        let text = MonarchMediaUploadTokens.progressText(model.phase, labels: labels, locale: locale)
        let fraction: Double? =
            if case .uploading(_, let fraction) = model.phase { fraction } else { nil }
        return VStack(alignment: .leading, spacing: MonarchMediaUploadTokens.buttonSpacing) {
            ProgressView(value: fraction) {
                Text(text)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchMediaUploadTokens.secondary])
            }
            .progressViewStyle(.monarchLinear)
            MonarchButton(labels.cancel ?? MonarchMediaUploadStrings.cancel, variant: .ghost, size: .sm) {
                cancel()
            }
        }
    }

    private func filled(_ url: URL) -> some View {
        let kind = shownKind(of: url)
        return VStack(alignment: .leading, spacing: MonarchMediaUploadTokens.spacing) {
            MonarchMediaUploadPreview(
                url: url,
                kind: kind,
                label: MonarchMediaUploadTokens.previewLabel(kind, custom: previewLabel, labels: labels),
                labels: labels
            )
            picker(title: MonarchMediaUploadTokens.replaceTitle(kind, labels: labels), variant: .compact)
            MonarchButton(
                MonarchMediaUploadTokens.removeTitle(kind, labels: labels),
                systemImage: MonarchMediaUploadTokens.removeSymbol,
                variant: .danger,
                size: .sm
            ) {
                remove(kind)
            }
        }
    }

    private func start(_ file: MonarchFileUploadFile) {
        task?.cancel()
        let kind = MonarchMediaUploadTokens.kind(of: file)
        let labels = labels
        let locale = locale
        let maxVideoDuration = maxVideoDuration
        let onUpload = onUpload
        task = Task { @MainActor in
            let result = await model.upload(
                file,
                kind: kind,
                maxVideoDuration: maxVideoDuration,
                labels: labels,
                locale: locale,
                readDuration: { try await MonarchMediaUploadTokens.videoDuration(of: $0) },
                upload: onUpload
            )
            if let result {
                uploadedKind = (result.url, kind)
                url = result.url
                AccessibilityNotification.Announcement(MonarchMediaUploadTokens.uploaded(kind, labels: labels)).post()
            } else if let error = model.error {
                AccessibilityNotification.Announcement(error).post()
            }
        }
    }

    private func cancel() {
        task?.cancel()
        task = nil
        model.cancel()
        AccessibilityNotification.Announcement(labels.cancelled ?? MonarchMediaUploadStrings.cancelled).post()
    }

    private func remove(_ kind: MonarchMediaUploadKind) {
        model.clearMessages()
        url = nil
        AccessibilityNotification.Announcement(MonarchMediaUploadTokens.removed(kind, labels: labels)).post()
    }
}

#if DEBUG
    #Preview("MonarchMediaUpload") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchMediaUpload("Cover image", url: .constant(nil), maxImageSize: 10_000_000) { file, _ in
                    MonarchMediaUploadResult(url: file.url)
                }
                MonarchMediaUpload(
                    "Tile media",
                    url: .constant(nil),
                    kinds: [.image, .video],
                    maxVideoDuration: 90
                ) { file, _ in
                    MonarchMediaUploadResult(url: file.url)
                }
            }
        }
    }
#endif
