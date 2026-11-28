import PhotosUI
import SwiftUI

/// Picks a profile or workspace picture with the system photo picker and
/// crops it to a circle or a square before handing it over: drag to move,
/// pinch or use the slider to zoom.
///
/// ```swift
/// MonarchAvatarUpload(
///     name: "Ada Lovelace",
///     url: profile.pictureURL,
///     onRemove: { profile.removePicture() },
///     onSave: { result in try await api.uploadAvatar(result.data) }
/// )
/// ```
///
/// The picker is `PhotosPicker`, which needs no photo library permission.
/// The crop editor opens in a sheet. `onSave` receives the square picture
/// (512 pixels, PNG by default), the crop and the original file; while it
/// runs the Save button shows progress, and when it throws the editor stays
/// open with "The photo could not be saved. Try again." Disable the whole
/// control with `.disabled(true)`.
///
/// Accessibility: the avatar is read by name; the buttons are standard
/// buttons. In the editor the crop area is one element named "Photo
/// position" whose value is the zoom ("150%"): swipe up or down to zoom,
/// and use the Move left, right, up, down and Reset position actions. A
/// hardware keyboard moves the picture with the arrow keys and zooms with
/// + and −. Errors are announced and drawn with an icon.
public struct MonarchAvatarUpload: View {
    private enum Source {
        case image(Image?)
        case url(URL?)
    }

    /// A picked file decoded and waiting in the crop editor.
    private struct Picked: Identifiable {
        let id: Int
        let image: CGImage
        let data: Data
    }

    private let name: String
    private let source: Source
    private let shape: MonarchAvatarShape
    private let maxFileSize: Int?
    private let outputSize: Int
    private let outputFormat: MonarchAvatarUploadFormat
    private let labels: MonarchAvatarUploadLabels
    private let onRemove: (@MainActor () -> Void)?
    private let onSave: @MainActor (MonarchAvatarUploadResult) async throws -> Void

    @State private var pickerItem: PhotosPickerItem?
    @State private var picked: Picked?
    @State private var pickCount = 0
    @State private var errorText: String?
    @Environment(\.monarchTheme) private var theme

    /// An avatar upload showing a picture the app has loaded.
    ///
    /// - Parameters:
    ///   - name: The person's or workspace's name, for the initials and what VoiceOver reads.
    ///   - image: The current picture, or `nil` for none.
    ///   - shape: `circle` for people, `square` for workspaces; also the crop mask.
    ///   - maxFileSize: The largest file accepted, in bytes. `nil` accepts any size.
    ///   - outputSize: The width and height of the saved picture, in pixels.
    ///   - outputFormat: The format of the saved picture.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRemove: Removes the picture. Without it there is no remove button.
    ///   - onSave: Receives the saved crop. Throw to keep the editor open with an error.
    public init(
        name: String,
        image: Image?,
        shape: MonarchAvatarShape = .circle,
        maxFileSize: Int? = nil,
        outputSize: Int = 512,
        outputFormat: MonarchAvatarUploadFormat = .png,
        labels: MonarchAvatarUploadLabels = MonarchAvatarUploadLabels(),
        onRemove: (@MainActor () -> Void)? = nil,
        onSave: @escaping @MainActor (MonarchAvatarUploadResult) async throws -> Void
    ) {
        self.name = name
        self.source = .image(image)
        self.shape = shape
        self.maxFileSize = maxFileSize
        self.outputSize = outputSize
        self.outputFormat = outputFormat
        self.labels = labels
        self.onRemove = onRemove
        self.onSave = onSave
    }

    /// An avatar upload showing the picture at a URL.
    ///
    /// - Parameters:
    ///   - name: The person's or workspace's name, for the initials and what VoiceOver reads.
    ///   - url: The current picture's address, or `nil` for none.
    ///   - shape: `circle` for people, `square` for workspaces; also the crop mask.
    ///   - maxFileSize: The largest file accepted, in bytes. `nil` accepts any size.
    ///   - outputSize: The width and height of the saved picture, in pixels.
    ///   - outputFormat: The format of the saved picture.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRemove: Removes the picture. Without it there is no remove button.
    ///   - onSave: Receives the saved crop. Throw to keep the editor open with an error.
    public init(
        name: String,
        url: URL?,
        shape: MonarchAvatarShape = .circle,
        maxFileSize: Int? = nil,
        outputSize: Int = 512,
        outputFormat: MonarchAvatarUploadFormat = .png,
        labels: MonarchAvatarUploadLabels = MonarchAvatarUploadLabels(),
        onRemove: (@MainActor () -> Void)? = nil,
        onSave: @escaping @MainActor (MonarchAvatarUploadResult) async throws -> Void
    ) {
        self.name = name
        self.source = .url(url)
        self.shape = shape
        self.maxFileSize = maxFileSize
        self.outputSize = outputSize
        self.outputFormat = outputFormat
        self.labels = labels
        self.onRemove = onRemove
        self.onSave = onSave
    }

    private var hasPicture: Bool {
        switch source {
        case .image(let image): image != nil
        case .url(let url): url != nil
        }
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAvatarUploadTokens.buttonSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchAvatarUploadTokens.spacing) {
                    avatar
                    buttons
                }
                VStack(alignment: .leading, spacing: MonarchAvatarUploadTokens.spacing) {
                    avatar
                    buttons
                }
            }
            if let errorText {
                MonarchAvatarUploadError(text: errorText)
            }
        }
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            pickerItem = nil
            Task { @MainActor in await load(item) }
        }
        .sheet(item: $picked) { picked in
            MonarchAvatarUploadEditor(
                image: picked.image,
                original: picked.data,
                shape: shape,
                outputSize: outputSize,
                outputFormat: outputFormat,
                labels: labels,
                onSave: onSave,
                onClose: { self.picked = nil }
            )
            .padding(MonarchAvatarUploadTokens.spacing)
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
            .environment(\.monarchTheme, theme)
        }
    }

    @ViewBuilder private var avatar: some View {
        switch source {
        case .image(let image):
            MonarchAvatar(image: image, name: name, shape: shape, size: MonarchAvatarUploadTokens.avatarSize)
        case .url(let url):
            MonarchAvatar(url: url, name: name, shape: shape, size: MonarchAvatarUploadTokens.avatarSize)
        }
    }

    private var buttons: some View {
        // The picker's label is built off the main actor, so its text is worked out here.
        let title = MonarchAvatarUploadTokens.pickTitle(hasPicture: hasPicture, labels: labels)
        let symbol = MonarchAvatarUploadTokens.pickSymbol
        return HStack(spacing: MonarchAvatarUploadTokens.buttonSpacing) {
            PhotosPicker(selection: $pickerItem, matching: .images, photoLibrary: .shared()) {
                Label(title, systemImage: symbol)
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
            if hasPicture, let onRemove {
                MonarchButton(labels.remove ?? MonarchAvatarUploadStrings.remove, variant: .ghost, size: .sm) {
                    onRemove()
                }
            }
        }
    }

    private func load(_ item: PhotosPickerItem) async {
        let data: Data?
        do {
            data = try await item.loadTransferable(type: Data.self)
        } catch {
            data = nil
        }
        let problem: MonarchAvatarUploadProblem?
        var image: CGImage?
        if let data, MonarchAvatarUploadTokens.isTooLarge(byteCount: data.count, maxFileSize: maxFileSize) {
            problem = .tooLarge
        } else if let data,
            let decoded = MonarchAvatarUploadImage.decode(data, maxSide: MonarchAvatarUploadTokens.maximumDecodedSide)
        {
            image = decoded
            problem = nil
        } else {
            problem = .unreadable
        }
        if let problem {
            let message = MonarchAvatarUploadTokens.message(for: problem, maxFileSize: maxFileSize, labels: labels)
            errorText = message
            AccessibilityNotification.Announcement(message).post()
            return
        }
        guard let image, let data else { return }
        errorText = nil
        pickCount += 1
        picked = Picked(id: pickCount, image: image, data: data)
    }
}

#if DEBUG
    #Preview("MonarchAvatarUpload") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAvatarUpload(name: "Ada Lovelace", image: nil) { _ in }
                MonarchAvatarUpload(
                    name: "Rook Labs",
                    image: Image(systemName: "building.2"),
                    shape: .square,
                    onRemove: {},
                    onSave: { _ in }
                )
            }
        }
    }
#endif
