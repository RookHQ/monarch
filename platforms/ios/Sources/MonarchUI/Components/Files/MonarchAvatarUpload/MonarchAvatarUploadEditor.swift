import SwiftUI

/// The crop editor of ``MonarchAvatarUpload``: the picture in a square
/// area with a circle or square mask, moved by dragging and zoomed by
/// pinching, the zoom slider, and Save and Cancel.
struct MonarchAvatarUploadEditor: View {
    let image: CGImage
    let original: Data
    let shape: MonarchAvatarShape
    let outputSize: Int
    let outputFormat: MonarchAvatarUploadFormat
    let labels: MonarchAvatarUploadLabels
    let onSave: @MainActor (MonarchAvatarUploadResult) async throws -> Void
    let onClose: @MainActor () -> Void

    @State private var viewport: MonarchAvatarUploadViewport
    @State private var dragStart: MonarchAvatarUploadViewport?
    @State private var pinchStart: MonarchAvatarUploadViewport?
    @State private var isSaving: Bool
    @State private var errorText: String?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    init(
        image: CGImage,
        original: Data,
        shape: MonarchAvatarShape,
        outputSize: Int,
        outputFormat: MonarchAvatarUploadFormat,
        labels: MonarchAvatarUploadLabels,
        viewport: MonarchAvatarUploadViewport = MonarchAvatarUploadViewport(),
        isSaving: Bool = false,
        errorText: String? = nil,
        onSave: @escaping @MainActor (MonarchAvatarUploadResult) async throws -> Void,
        onClose: @escaping @MainActor () -> Void
    ) {
        self.image = image
        self.original = original
        self.shape = shape
        self.outputSize = outputSize
        self.outputFormat = outputFormat
        self.labels = labels
        self.onSave = onSave
        self.onClose = onClose
        _viewport = State(initialValue: viewport)
        _isSaving = State(initialValue: isSaving)
        _errorText = State(initialValue: errorText)
    }

    private var natural: CGSize { CGSize(width: image.width, height: image.height) }
    private var isLocked: Bool { isSaving || !isEnabled }

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchAvatarUploadTokens.spacing) {
            Text(labels.editorTitle ?? MonarchAvatarUploadStrings.editorTitle)
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            cropArea
                .frame(maxWidth: MonarchAvatarUploadTokens.maximumCropSide)
                .frame(maxWidth: .infinity)
            Text(labels.cropHint ?? MonarchAvatarUploadStrings.cropHint)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchAvatarUploadTokens.hint])
                .accessibilityHidden(true)
            Slider(value: zoom, in: MonarchAvatarUploadTokens.minimumZoom...MonarchAvatarUploadTokens.maximumZoom) {
                Text(labels.zoom ?? MonarchAvatarUploadStrings.zoom)
            }
            .accessibilityValue(MonarchAvatarUploadTokens.zoomValue(viewport.zoom, labels: labels))
            .disabled(isSaving)
            if let errorText {
                MonarchAvatarUploadError(text: errorText)
            }
            HStack(spacing: MonarchAvatarUploadTokens.buttonSpacing) {
                MonarchButton(
                    labels.save ?? MonarchAvatarUploadStrings.save,
                    variant: .primary,
                    size: .sm,
                    isLoading: isSaving
                ) {
                    save()
                }
                MonarchButton(labels.cancel ?? MonarchAvatarUploadStrings.cancel, variant: .ghost, size: .sm) {
                    onClose()
                }
                .disabled(isSaving)
            }
        }
    }

    private var zoom: Binding<CGFloat> {
        Binding(
            get: { viewport.zoom },
            set: { new in
                var next = viewport
                next.zoom = new
                viewport = MonarchAvatarUploadTokens.clamp(next, natural: natural)
            }
        )
    }

    private var cropArea: some View {
        GeometryReader { proxy in
            let side = proxy.size.width
            let size = MonarchAvatarUploadTokens.imageSize(natural: natural, zoom: viewport.zoom)
            ZStack {
                theme.colors[role: MonarchAvatarUploadTokens.cropBackground]
                Image(decorative: image, scale: 1)
                    .resizable()
                    .frame(width: size.width * side, height: size.height * side)
                    .position(x: side / 2 + viewport.x * side, y: side / 2 + viewport.y * side)
                MonarchAvatarUploadMask(shape: shape)
                    .fill(
                        theme.colors[role: MonarchAvatarUploadTokens.mask].opacity(
                            MonarchAvatarUploadTokens.maskOpacity
                        ),
                        style: FillStyle(eoFill: true)
                    )
                maskOutline
            }
            .frame(width: side, height: side)
            .clipped()
            .contentShape(Rectangle())
            .gesture(drag(side: side))
            .simultaneousGesture(pinch)
        }
        .aspectRatio(1, contentMode: .fit)
        // A photo is not mirrored in right-to-left layouts: positions, drags and
        // the move actions stay in the picture's own left-to-right space.
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.cropArea ?? MonarchAvatarUploadStrings.cropArea)
        .accessibilityValue(MonarchAvatarUploadTokens.zoomValue(viewport.zoom, labels: labels))
        .accessibilityHint(labels.cropHint ?? MonarchAvatarUploadStrings.cropHint)
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: step(.zoomIn)
            case .decrement: step(.zoomOut)
            @unknown default: break
            }
        }
        .accessibilityActions {
            ForEach([MonarchAvatarUploadMove.left, .right, .up, .down, .reset], id: \.self) { move in
                if let name = MonarchAvatarUploadTokens.actionName(move, labels: labels) {
                    Button(name) { step(move) }
                }
            }
        }
        .focusable()
        .onKeyPress(phases: .down) { press in
            guard let move = MonarchAvatarUploadTokens.move(forKey: press.key) else {
                if press.key == .return {
                    save()
                    return .handled
                }
                if press.key == .escape, !isSaving {
                    onClose()
                    return .handled
                }
                return .ignored
            }
            step(move, isLarge: press.modifiers.contains(.shift))
            return .handled
        }
    }

    @ViewBuilder private var maskOutline: some View {
        switch shape {
        case .circle:
            Circle().strokeBorder(
                theme.colors[role: MonarchAvatarUploadTokens.ring],
                lineWidth: MonarchAvatarUploadTokens.ringWidth
            )
        case .square:
            RoundedRectangle(cornerRadius: MonarchAvatarUploadTokens.squareRadius, style: .continuous)
                .strokeBorder(
                    theme.colors[role: MonarchAvatarUploadTokens.ring],
                    lineWidth: MonarchAvatarUploadTokens.ringWidth
                )
        }
    }

    private func drag(side: CGFloat) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                guard !isLocked else { return }
                let start = dragStart ?? viewport
                dragStart = start
                viewport = MonarchAvatarUploadTokens.dragged(start, by: value.translation, side: side, natural: natural)
            }
            .onEnded { _ in dragStart = nil }
    }

    private var pinch: some Gesture {
        MagnifyGesture()
            .onChanged { value in
                guard !isLocked else { return }
                let start = pinchStart ?? viewport
                pinchStart = start
                viewport = MonarchAvatarUploadTokens.magnified(start, by: value.magnification, natural: natural)
            }
            .onEnded { _ in pinchStart = nil }
    }

    private func step(_ move: MonarchAvatarUploadMove, isLarge: Bool = false) {
        guard !isLocked else { return }
        viewport = MonarchAvatarUploadTokens.apply(move, to: viewport, natural: natural, isLarge: isLarge)
    }

    private func save() {
        guard !isLocked else { return }
        isSaving = true
        errorText = nil
        let failed = labels.saveFailed ?? MonarchAvatarUploadStrings.saveFailed
        guard
            let result = MonarchAvatarUploadImage.result(
                image: image,
                original: original,
                viewport: viewport,
                side: outputSize,
                format: outputFormat
            )
        else {
            fail(failed)
            return
        }
        Task { @MainActor in
            do {
                try await onSave(result)
                isSaving = false
                onClose()
            } catch {
                fail(failed)
            }
        }
    }

    private func fail(_ message: String) {
        isSaving = false
        errorText = message
        AccessibilityNotification.Announcement(message).post()
    }
}

/// The area outside the kept circle or square, filled even-odd over the crop area.
struct MonarchAvatarUploadMask: Shape {
    let shape: MonarchAvatarShape

    func path(in rect: CGRect) -> Path {
        var path = Path(rect)
        switch shape {
        case .circle:
            path.addEllipse(in: rect)
        case .square:
            path.addRoundedRect(
                in: rect,
                cornerSize: CGSize(
                    width: MonarchAvatarUploadTokens.squareRadius,
                    height: MonarchAvatarUploadTokens.squareRadius
                ),
                style: .continuous
            )
        }
        return path
    }
}

/// An error under the avatar upload's buttons or in its editor: an icon and text.
struct MonarchAvatarUploadError: View {
    let text: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchAvatarUploadTokens.errorSpacing) {
            MonarchIcon(systemName: MonarchAvatarUploadTokens.errorSymbol, size: .sm)
            Text(text)
                .monarchTextStyle(.caption)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(theme.colors[role: MonarchAvatarUploadTokens.error])
        .accessibilityElement(children: .combine)
    }
}
