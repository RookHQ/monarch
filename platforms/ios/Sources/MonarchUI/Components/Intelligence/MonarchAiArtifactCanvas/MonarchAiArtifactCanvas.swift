import SwiftUI

/// The working surface for an artifact an AI reply made: the title, the
/// version stepper, Copy, Share and Save, and Preview and Code views.
///
/// ```swift
/// .sheet(item: $openArtifact) { artifact in
///     MonarchAiArtifactCanvas(
///         artifact,
///         versions: versions,
///         saveTitle: "Save to library",
///         onVersionChange: { version in load(version) },
///         onSave: { artifact in await library.save(artifact) },
///         onClose: { openArtifact = nil }
///     )
/// }
/// ```
///
/// On iPhone present it in a `.sheet` (full height) from the reply's
/// ``MonarchAiArtifactCard``; in a regular-width layout it can sit beside the
/// conversation. Documents and tables preview through ``MonarchAiMarkdown``,
/// images through `AsyncImage` (http and https only), and HTML only in a
/// sandboxed web view with JavaScript off and every network request and
/// navigation blocked. Code and charts have no built-in preview, so only the
/// code shows unless the app supplies one with ``preview(for:_:)``. The Code
/// view is a ``MonarchAiCodeBlock`` with line numbers. Share opens the system
/// share sheet, which also saves to Files. Save shows only with `onSave` and
/// shows progress until it returns.
///
/// Accessibility: the canvas is a container named "Artifact: Q3 summary";
/// the header is read as "Q3 summary, Document, version 3" with the header
/// trait. Preview and Code are a system segmented picker named "View". The
/// close button is named "Close artifact". Copy announces that it copied.
/// The web preview is named "Preview of Q3 summary". Every button has a
/// 44-point touch target, and the kind icon is decorative.
@_spi(Experimental) public struct MonarchAiArtifactCanvas: View {
    private let artifact: MonarchAiCanvasArtifact
    private let versions: [MonarchAiArtifactVersion]
    private let tabBinding: Binding<MonarchAiArtifactCanvasTab>?
    private let isShareable: Bool
    private let saveTitle: String?
    private let labels: MonarchAiArtifactCanvasLabels
    private let onVersionChange: (@MainActor (Int) -> Void)?
    private let onCopy: (@MainActor (MonarchAiCanvasArtifact) -> Void)?
    private let onSave: (@MainActor (MonarchAiCanvasArtifact) async -> Void)?
    private let onClose: (@MainActor () -> Void)?
    private var customPreview: CustomPreview?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var tileSize = MonarchAiArtifactCanvasTokens.iconTileSize
    @State private var tabState = MonarchAiArtifactCanvasTab.preview
    @State private var isSaving = false

    private typealias Tokens = MonarchAiArtifactCanvasTokens

    /// A preview the app supplies for some kinds.
    private struct CustomPreview {
        let kinds: Set<MonarchAiArtifactKind>
        let make: @MainActor (MonarchAiCanvasArtifact) -> AnyView
    }

    /// An artifact canvas.
    ///
    /// - Parameters:
    ///   - artifact: The artifact version to show.
    ///   - versions: Every version, for the stepper. It shows with two or more and `onVersionChange`.
    ///   - tab: The view on show. Without it the canvas keeps it, starting on Preview.
    ///   - isShareable: Whether Share shows.
    ///   - saveTitle: The save button's title, naming the destination, such as "Save to library".
    ///   - labels: Replacements for the built-in strings.
    ///   - onVersionChange: Receives the version chosen in the stepper; load it and pass it as `artifact`.
    ///   - onCopy: Runs after Copy copied the content.
    ///   - onSave: Shows Save; saves the artifact. The button shows progress until it returns.
    ///   - onClose: Shows the close button.
    public init(
        _ artifact: MonarchAiCanvasArtifact,
        versions: [MonarchAiArtifactVersion] = [],
        tab: Binding<MonarchAiArtifactCanvasTab>? = nil,
        isShareable: Bool = true,
        saveTitle: String? = nil,
        labels: MonarchAiArtifactCanvasLabels = MonarchAiArtifactCanvasLabels(),
        onVersionChange: (@MainActor (Int) -> Void)? = nil,
        onCopy: (@MainActor (MonarchAiCanvasArtifact) -> Void)? = nil,
        onSave: (@MainActor (MonarchAiCanvasArtifact) async -> Void)? = nil,
        onClose: (@MainActor () -> Void)? = nil
    ) {
        self.artifact = artifact
        self.versions = versions
        self.tabBinding = tab
        self.isShareable = isShareable
        self.saveTitle = saveTitle
        self.labels = labels
        self.onVersionChange = onVersionChange
        self.onCopy = onCopy
        self.onSave = onSave
        self.onClose = onClose
    }

    /// Replaces the preview of some kinds, for example a chart drawn with
    /// Swift Charts or a table as a grid. Whatever it draws is the app's
    /// responsibility: never render the content as HTML outside the sandbox.
    ///
    /// - Parameters:
    ///   - kinds: The kinds that use this preview.
    ///   - content: Draws the preview of an artifact.
    /// - Returns: The canvas with the preview.
    public func preview<Content: View>(
        for kinds: Set<MonarchAiArtifactKind>,
        @ViewBuilder _ content: @escaping @MainActor (MonarchAiCanvasArtifact) -> Content
    ) -> MonarchAiArtifactCanvas {
        var copy = self
        copy.customPreview = CustomPreview(kinds: kinds) { AnyView(content($0)) }
        return copy
    }

    private var tab: Binding<MonarchAiArtifactCanvasTab> {
        tabBinding ?? $tabState
    }

    /// The content of the view.
    public var body: some View {
        let hasPreview = Tokens.hasPreview(artifact.kind, customKinds: customPreview?.kinds ?? [])
        let shown = Tokens.tab(tab.wrappedValue, hasPreview: hasPreview)
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                header
                if Tokens.showsVersions(count: versions.count, hasHandler: onVersionChange != nil) {
                    MonarchAiArtifactVersions(
                        selection: Binding(get: { artifact.version }, set: { onVersionChange?($0) }),
                        versions: versions,
                        showsCompare: false
                    )
                }
                toolbar
                if hasPreview {
                    Picker(labels.views ?? MonarchAiArtifactCanvasStrings.views, selection: tab) {
                        Text(labels.preview ?? MonarchAiArtifactCanvasStrings.preview)
                            .tag(MonarchAiArtifactCanvasTab.preview)
                        Text(labels.code ?? MonarchAiArtifactCanvasStrings.code)
                            .tag(MonarchAiArtifactCanvasTab.code)
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                    .accessibilityLabel(labels.views ?? MonarchAiArtifactCanvasStrings.views)
                }
            }
            .padding(Tokens.padding)
            Divider()
            panel(shown)
                .frame(maxWidth: .infinity, minHeight: Tokens.panelMinHeight, maxHeight: .infinity, alignment: .top)
        }
        .background(theme.colors[role: Tokens.backgroundRole])
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Tokens.label(artifact, labels: labels))
    }

    // MARK: - Header and toolbar

    private var header: some View {
        HStack(alignment: .top, spacing: Tokens.spacing) {
            HStack(alignment: .top, spacing: Tokens.spacing) {
                MonarchIcon(systemName: MonarchAiArtifactCardTokens.symbol(artifact.kind), size: .sm)
                    .foregroundStyle(theme.colors[role: Tokens.iconRole])
                    .frame(width: tileSize, height: tileSize)
                    .background(
                        RoundedRectangle(cornerRadius: Tokens.iconTileRadius, style: .continuous)
                            .fill(theme.colors[role: Tokens.iconTileRole])
                    )
                VStack(alignment: .leading, spacing: Tokens.titleSpacing) {
                    Text(artifact.title)
                        .monarchTextStyle(.subheading)
                        .foregroundStyle(theme.colors.textDefault)
                    Text(Tokens.meta(artifact, labels: labels))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.secondaryRole])
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.headerLabel(artifact, labels: labels))
            .accessibilityAddTraits(.isHeader)
            if let onClose {
                Button(action: onClose) {
                    MonarchIcon(
                        systemName: Tokens.closeSymbol,
                        label: labels.close ?? MonarchAiArtifactCanvasStrings.close,
                        size: .sm
                    )
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
    }

    private var toolbar: some View {
        MonarchFlowLayout(spacing: Tokens.buttonSpacing) {
            MonarchCopyButton(
                labels.copy ?? MonarchAiArtifactCanvasStrings.copy,
                value: artifact.content,
                target: artifact.title,
                variant: .ghost,
                size: .sm,
                copiedTitle: labels.copied ?? MonarchAiArtifactCanvasStrings.copied,
                onCopy: { _ in onCopy?(artifact) }
            )
            if isShareable {
                ShareLink(item: artifact.content, subject: Text(artifact.title)) {
                    // Text only, as the web Download button.
                    Text(labels.share ?? MonarchAiArtifactCanvasStrings.share)
                        .monarchTextStyle(.label)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
            if let onSave {
                MonarchButton(
                    saveTitle ?? labels.save ?? MonarchAiArtifactCanvasStrings.save,
                    variant: .primary,
                    size: .sm,
                    isLoading: isSaving
                ) {
                    guard !isSaving else { return }
                    isSaving = true
                    Task { @MainActor in
                        await onSave(artifact)
                        isSaving = false
                    }
                }
            }
        }
    }

    // MARK: - Panel

    @ViewBuilder
    private func panel(_ shown: MonarchAiArtifactCanvasTab) -> some View {
        switch shown {
        case .code:
            ScrollView {
                MonarchAiCodeBlock(
                    artifact.content,
                    language: Tokens.language(artifact),
                    showsLineNumbers: true,
                    isCopyable: false
                )
                .padding(Tokens.padding)
            }
        case .preview:
            if let customPreview, customPreview.kinds.contains(artifact.kind) {
                ScrollView {
                    customPreview.make(artifact)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Tokens.padding)
                }
            } else if artifact.kind == .html {
                VStack(alignment: .leading, spacing: 0) {
                    MonarchAiArtifactCanvasWebView(html: artifact.content)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .accessibilityLabel(
                            labels.frameTitle?(artifact.title)
                                ?? MonarchAiArtifactCanvasStrings.frameTitle(artifact.title)
                        )
                    note(labels.sandboxNote ?? MonarchAiArtifactCanvasStrings.sandboxNote)
                        .padding(Tokens.padding)
                }
            } else {
                ScrollView {
                    builtInPreview
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(Tokens.padding)
                }
            }
        }
    }

    @ViewBuilder
    private var builtInPreview: some View {
        switch artifact.kind {
        case .document, .table:
            MonarchAiMarkdown(artifact.content)
        case .image:
            if let url = Tokens.imageURL(artifact.content) {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFit()
                    } else {
                        ProgressView()
                    }
                }
                .accessibilityLabel(artifact.title)
                .accessibilityAddTraits(.isImage)
            } else {
                MonarchCalloutBanner(
                    message: labels.unsafeImage ?? MonarchAiArtifactCanvasStrings.unsafeImage,
                    tone: .warning
                )
            }
        case .code, .chart, .html:
            note(labels.noPreview ?? MonarchAiArtifactCanvasStrings.noPreview)
        }
    }

    private func note(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: Tokens.secondaryRole])
    }
}

#if DEBUG
    #Preview("MonarchAiArtifactCanvas") {
        MonarchPreviewMatrix {
            MonarchAiArtifactCanvas(
                MonarchAiCanvasArtifact(
                    id: "q3",
                    kind: .document,
                    title: "Q3 summary",
                    version: 2,
                    content: "# Q3 summary\n\nRevenue grew **12%**.\n\n- New customers: 140"
                ),
                saveTitle: "Save to library",
                onSave: { _ in },
                onClose: {}
            )
        }
    }
#endif
