import PhotosUI
import SwiftUI
import UniformTypeIdentifiers

/// An area for adding files: the system document picker, the system photo
/// picker and drag and drop, with checks of type, size and count that
/// explain every refusal.
///
/// ```swift
/// MonarchFileUpload(
///     allowedContentTypes: [.pdf, .image],
///     maxSize: 10_000_000,
///     maxFiles: 5,
///     currentCount: attachments.count,
///     allowsPhotos: true
/// ) { accepted, rejected in
///     upload(accepted)
/// }
/// ```
///
/// The pickers are the system's: "Browse files" opens `.fileImporter`, and
/// "Choose photos" opens `PhotosPicker`. On iPad and Mac, files can also be
/// dropped on the area. Monarch does not upload: `onFiles` receives the
/// accepted files and the refused ones, and the app uploads the accepted
/// files, for example showing progress with ``MonarchUploadQueue``. A file
/// from the document picker is security-scoped; a photo or a dropped file is
/// a copy in the temporary directory.
///
/// Accessibility: the buttons are standard buttons with 44-point touch
/// targets and are the way in for VoiceOver and Switch Control; dropping is
/// an extra. The title and hint are read as one element. After a pick, the
/// number of files added and each refusal are announced, and refusals are
/// shown as a danger notice with an icon and text, not colour alone.
public struct MonarchFileUpload: View {
    private let allowedContentTypes: [UTType]
    private let maxSize: Int64?
    private let maxFiles: Int?
    private let currentCount: Int
    private let hint: String?
    private let systemImage: String?
    private let variant: MonarchFileUploadVariant
    private let allowsMultipleSelection: Bool?
    private let allowsPhotos: Bool
    private let showsErrors: Bool
    private let isInvalid: Bool
    private let labels: MonarchFileUploadLabels
    private let validate: (@Sendable (MonarchFileUploadFile) -> String?)?
    private let onFiles: @MainActor ([MonarchFileUploadFile], [MonarchFileUploadRejection]) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var isImporting = false
    @State private var isTargeted = false
    @State private var isLoadingPhotos = false
    @State private var photoItems: [PhotosPickerItem] = []
    @State private var rejections: [MonarchFileUploadRejection] = []

    /// A file upload area.
    ///
    /// - Parameters:
    ///   - allowedContentTypes: The types that are accepted. Files of other types are refused.
    ///   - maxSize: The largest size of one file, in bytes.
    ///   - maxFiles: The most files in total, counting `currentCount`.
    ///   - currentCount: Files already attached, which count towards `maxFiles`.
    ///   - hint: Text under the title. By default it is made from `maxSize` and `maxFiles`.
    ///   - systemImage: A decorative SF Symbol at the start.
    ///   - variant: A bordered zone or one compact row.
    ///   - allowsMultipleSelection: Whether several files can be chosen at once. By default, unless `maxFiles` is 1.
    ///   - allowsPhotos: Adds a button that opens the photo picker for the images and videos the types allow.
    ///   - showsErrors: Shows the refusals under the area. Turn off to show them yourself from `onFiles`.
    ///   - isInvalid: Draws the danger border, for example when a required file is missing.
    ///   - labels: Replacements for the built-in strings.
    ///   - validate: An extra check. Return a message to refuse a file, or `nil` to accept it.
    ///   - onFiles: Receives the accepted and the refused files of every pick or drop.
    public init(
        allowedContentTypes: [UTType] = [.item],
        maxSize: Int64? = nil,
        maxFiles: Int? = nil,
        currentCount: Int = 0,
        hint: String? = nil,
        systemImage: String? = nil,
        variant: MonarchFileUploadVariant = .zone,
        allowsMultipleSelection: Bool? = nil,
        allowsPhotos: Bool = false,
        showsErrors: Bool = true,
        isInvalid: Bool = false,
        labels: MonarchFileUploadLabels = MonarchFileUploadLabels(),
        validate: (@Sendable (MonarchFileUploadFile) -> String?)? = nil,
        onFiles: @escaping @MainActor ([MonarchFileUploadFile], [MonarchFileUploadRejection]) -> Void
    ) {
        self.allowedContentTypes = allowedContentTypes
        self.maxSize = maxSize
        self.maxFiles = maxFiles
        self.currentCount = currentCount
        self.hint = hint
        self.systemImage = systemImage
        self.variant = variant
        self.allowsMultipleSelection = allowsMultipleSelection
        self.allowsPhotos = allowsPhotos
        self.showsErrors = showsErrors
        self.isInvalid = isInvalid
        self.labels = labels
        self.validate = validate
        self.onFiles = onFiles
    }

    /// Splits files into accepted and refused, with the same checks and
    /// messages as the area. Use it for files from other sources, such as
    /// the pasteboard.
    ///
    /// - Parameters:
    ///   - files: The files to check, in order.
    ///   - allowedContentTypes: The types that are accepted.
    ///   - maxSize: The largest size of one file, in bytes.
    ///   - maxFiles: The most files in total, counting `currentCount`.
    ///   - currentCount: Files already attached.
    ///   - labels: Replacements for the built-in messages.
    ///   - locale: The locale the size limit is written in.
    ///   - validate: An extra check. Return a message to refuse a file.
    /// - Returns: The accepted files and the refusals, each in the order given.
    nonisolated public static func validate(
        _ files: [MonarchFileUploadFile],
        allowedContentTypes: [UTType] = [.item],
        maxSize: Int64? = nil,
        maxFiles: Int? = nil,
        currentCount: Int = 0,
        labels: MonarchFileUploadLabels = MonarchFileUploadLabels(),
        locale: Locale = .autoupdatingCurrent,
        validate: (@Sendable (MonarchFileUploadFile) -> String?)? = nil
    ) -> (accepted: [MonarchFileUploadFile], rejected: [MonarchFileUploadRejection]) {
        MonarchFileUploadTokens.validate(
            files,
            allowedContentTypes: allowedContentTypes,
            maxSize: maxSize,
            maxFiles: maxFiles,
            currentCount: currentCount,
            labels: labels,
            locale: locale,
            validate: validate
        )
    }

    private var isFull: Bool { MonarchFileUploadTokens.isFull(maxFiles: maxFiles, currentCount: currentCount) }
    private var isBlocked: Bool { !isEnabled || isFull }
    private var allowsMultiple: Bool {
        MonarchFileUploadTokens.allowsMultiple(allowsMultipleSelection, maxFiles: maxFiles)
    }
    private var appearance: MonarchFileUploadTokens.Appearance {
        MonarchFileUploadTokens.appearance(
            isTargeted: isTargeted && !isBlocked,
            isInvalid: isInvalid || !rejections.isEmpty,
            isEnabled: !isBlocked
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchFileUploadTokens.messageSpacing) {
            area
            if showsErrors, !rejections.isEmpty {
                MonarchInlineNotice(rejections.map(\.message).joined(separator: "\n"), tone: .danger)
            }
        }
        .fileImporter(
            isPresented: $isImporting,
            allowedContentTypes: allowedContentTypes,
            allowsMultipleSelection: allowsMultiple
        ) { result in
            guard case .success(let urls) = result else { return }
            take(urls.map(Self.describe))
        }
        .onChange(of: photoItems) { _, items in
            guard !items.isEmpty else { return }
            loadPhotos(items)
        }
    }

    @ViewBuilder private var area: some View {
        let shape = MonarchShapes.container
        Group {
            if variant == .compact && !dynamicTypeSize.isAccessibilitySize {
                // As the web: the buttons and the hint in one row, with no box.
                HStack(spacing: MonarchFileUploadTokens.spacing) {
                    buttons
                    compactHint.frame(maxWidth: .infinity, alignment: .leading)
                }
            } else if variant == .compact {
                VStack(alignment: .leading, spacing: MonarchFileUploadTokens.spacing) {
                    buttons
                    compactHint
                }
            } else {
                VStack(spacing: MonarchFileUploadTokens.spacing) {
                    icon
                    text(alignment: .center)
                    buttons
                }
                .padding(MonarchFileUploadTokens.zonePadding)
            }
        }
        .frame(maxWidth: .infinity, alignment: variant == .zone ? .center : .leading)
        .background(
            shape.fill(theme.colors[role: variant == .zone ? appearance.fill : \.bgCanvas]).opacity(
                variant == .zone ? 1 : 0
            )
        )
        .overlay(
            shape.strokeBorder(
                theme.colors[role: appearance.border],
                style: StrokeStyle(
                    lineWidth: MonarchFileUploadTokens.borderWidth,
                    dash: [MonarchFileUploadTokens.dash, MonarchFileUploadTokens.dash]
                )
            )
            .opacity(variant == .zone ? 1 : 0)
        )
        .dropDestination(
            for: MonarchReceivedFile.self,
            action: { received, _ in
                guard !isBlocked else { return false }
                take(received.map(\.file))
                return true
            },
            isTargeted: { targeted in isTargeted = targeted }
        )
    }

    private var icon: some View {
        // The symbol sits in a round surface with the smallest shadow, as on the web.
        MonarchIcon(systemName: systemImage ?? MonarchFileUploadTokens.symbol, size: .md)
            .foregroundStyle(theme.colors[role: appearance.icon])
            .frame(width: MonarchFileUploadTokens.iconCircle, height: MonarchFileUploadTokens.iconCircle)
            .monarchElevation(.xs, in: Circle())
    }

    @ViewBuilder private var compactHint: some View {
        let shownHint = MonarchFileUploadTokens.hint(
            hint,
            maxSize: maxSize,
            maxFiles: maxFiles,
            currentCount: currentCount,
            labels: labels,
            locale: locale
        )
        if let shownHint {
            Text(shownHint)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: MonarchFileUploadTokens.hintColor(isEnabled: !isBlocked)])
        }
    }

    private func text(alignment: TextAlignment) -> some View {
        let shownHint = MonarchFileUploadTokens.hint(
            hint,
            maxSize: maxSize,
            maxFiles: maxFiles,
            currentCount: currentCount,
            labels: labels,
            locale: locale
        )
        return VStack(
            alignment: alignment == .center ? .center : .leading,
            spacing: MonarchFileUploadTokens.textSpacing
        ) {
            Text(
                isTargeted && !isBlocked
                    ? labels.dropHere ?? MonarchFileUploadStrings.dropHere
                    : labels.title ?? MonarchFileUploadStrings.title
            )
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors[role: appearance.title])
            if let shownHint {
                Text(shownHint)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchFileUploadTokens.hintColor(isEnabled: !isBlocked)])
            }
        }
        .multilineTextAlignment(alignment)
        .accessibilityElement(children: .combine)
    }

    private var buttons: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchFileUploadTokens.buttonSpacing) { buttonItems }
            VStack(spacing: MonarchFileUploadTokens.buttonSpacing) { buttonItems }
        }
        .disabled(isBlocked)
    }

    @ViewBuilder private var buttonItems: some View {
        Button(labels.browse ?? MonarchFileUploadStrings.browse) { isImporting = true }
            .buttonStyle(.monarch(.secondary, size: .sm))
        if allowsPhotos {
            photosButton
        }
    }

    @ViewBuilder private var photosButton: some View {
        let kinds = MonarchFileUploadTokens.photoKinds(allowedContentTypes: allowedContentTypes)
        let filters = [kinds.images ? PHPickerFilter.images : nil, kinds.videos ? PHPickerFilter.videos : nil]
            .compactMap { $0 }
        if !filters.isEmpty {
            HStack(spacing: MonarchFileUploadTokens.buttonSpacing) {
                PhotosPicker(
                    labels.photos ?? MonarchFileUploadStrings.photos,
                    selection: $photoItems,
                    maxSelectionCount: allowsMultiple
                        ? MonarchFileUploadTokens.room(maxFiles: maxFiles, currentCount: currentCount) : 1,
                    matching: .any(of: filters)
                )
                .buttonStyle(.monarch(.secondary, size: .sm))
                .disabled(isLoadingPhotos)
                if isLoadingPhotos {
                    ProgressView().controlSize(.small)
                }
            }
        }
    }

    private func loadPhotos(_ items: [PhotosPickerItem]) {
        isLoadingPhotos = true
        Task {
            var files: [MonarchFileUploadFile] = []
            for item in items {
                if let received = try? await item.loadTransferable(type: MonarchReceivedFile.self) {
                    files.append(received.file)
                }
            }
            photoItems = []
            isLoadingPhotos = false
            take(files)
        }
    }

    private func take(_ files: [MonarchFileUploadFile]) {
        guard !isBlocked, !files.isEmpty else { return }
        let picked = allowsMultiple ? files : Array(files.prefix(1))
        let extra =
            allowsMultiple
            ? [] : files.dropFirst().map { MonarchFileUploadTokens.tooMany($0, limit: 1, labels: labels) }
        let result = Self.validate(
            picked,
            allowedContentTypes: allowedContentTypes,
            maxSize: maxSize,
            maxFiles: maxFiles,
            currentCount: currentCount,
            labels: labels,
            locale: locale,
            validate: validate
        )
        let rejected = result.rejected + extra
        rejections = rejected
        if let message = MonarchFileUploadTokens.announcement(
            accepted: result.accepted.count,
            rejected: rejected,
            labels: labels
        ) {
            AccessibilityNotification.Announcement(message).post()
        }
        onFiles(result.accepted, rejected)
    }

    /// Reads a picked file while its security scope is open.
    nonisolated private static func describe(_ url: URL) -> MonarchFileUploadFile {
        let isScoped = url.startAccessingSecurityScopedResource()
        defer { if isScoped { url.stopAccessingSecurityScopedResource() } }
        return MonarchFileUploadFile(url: url)
    }
}

#if DEBUG
    #Preview("MonarchFileUpload") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchFileUpload(maxSize: 10_000_000, maxFiles: 5, allowsPhotos: true) { _, _ in }
                MonarchFileUpload(allowedContentTypes: [.pdf], variant: .compact) { _, _ in }
                MonarchFileUpload(maxFiles: 1, currentCount: 1) { _, _ in }
                MonarchFileUpload(isInvalid: true) { _, _ in }.disabled(true)
            }
        }
    }
#endif
