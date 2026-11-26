import SwiftUI
import UniformTypeIdentifiers

/// One-time recovery codes in a monospaced grid, with copy, save or share,
/// an optional show and hide button, generate new codes behind a
/// confirmation, and an "I have saved my recovery codes" acknowledgement for
/// setup flows. Used codes are struck through.
///
/// ```swift
/// MonarchRecoveryCodes(
///     codes,
///     heading: "Rook account recovery codes",
///     onRegenerate: { codes = try? await api.regenerateCodes() ?? codes },
///     onAcknowledge: { path.append(.done) }
/// )
/// ```
///
/// "Save or share" opens the system share sheet with a text file of the
/// unused codes, where the person can save it to Files or a password
/// manager. "Generate new codes" asks for confirmation first, because the
/// old codes stop working; pass the new codes back through `codes`. A new set
/// of codes needs a new acknowledgement.
///
/// Accessibility: the codes are a group named "Recovery codes". Each code is
/// spelled out character by character, and a used code has the value
/// "Used", so its state does not depend on the strike-through. Buttons are
/// standard buttons with 44-point touch targets. New codes are announced.
/// Pressing Continue without the checkbox shows and announces an error with
/// an icon and text.
public struct MonarchRecoveryCodes: View {
    private let codes: [MonarchRecoveryCode]
    private let heading: String?
    private let fileName: String
    private let showsRevealToggle: Bool
    private let labels: MonarchRecoveryCodesLabels
    private let onRegenerate: (@MainActor () async -> Void)?
    private let onAcknowledge: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var columnMinWidth = MonarchRecoveryCodesTokens.columnMinWidth
    @State private var isRevealed: Bool
    @State private var isExporting = false
    @State private var isAcknowledged = false
    @State private var showsMissing = false
    @State private var isConfirmingRegenerate = false
    @State private var isRegenerating = false

    /// Recovery codes.
    ///
    /// - Parameters:
    ///   - codes: The codes, as ``MonarchRecoveryCode`` values or string literals.
    ///   - heading: A first line for the copied and saved text, such as the product and account.
    ///   - fileName: The name of the saved text file.
    ///   - startsRevealed: Whether the codes are visible at first.
    ///   - showsRevealToggle: Whether the show and hide button is shown. By default when the codes start hidden.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRegenerate: Replaces the codes after a confirmation. Shows "Generate new codes". The button
    ///     shows progress until the function returns.
    ///   - onAcknowledge: Runs when the person ticks "I have saved my recovery codes" and presses
    ///     Continue. Neither is shown without it.
    public init(
        _ codes: [MonarchRecoveryCode],
        heading: String? = nil,
        fileName: String = "recovery-codes.txt",
        startsRevealed: Bool = true,
        showsRevealToggle: Bool? = nil,
        labels: MonarchRecoveryCodesLabels = MonarchRecoveryCodesLabels(),
        onRegenerate: (@MainActor () async -> Void)? = nil,
        onAcknowledge: (@MainActor () -> Void)? = nil
    ) {
        self.codes = codes
        self.heading = heading
        self.fileName = fileName
        self.showsRevealToggle = MonarchRecoveryCodesTokens.showsRevealToggle(
            startsRevealed: startsRevealed,
            override: showsRevealToggle
        )
        self.labels = labels
        self.onRegenerate = onRegenerate
        self.onAcknowledge = onAcknowledge
        _isRevealed = State(initialValue: startsRevealed)
    }

    /// The content of the view.
    public var body: some View {
        let unused = MonarchRecoveryCodesTokens.unused(codes)
        VStack(alignment: .leading, spacing: MonarchRecoveryCodesTokens.sectionSpacing) {
            if MonarchRecoveryCodesTokens.showsCount(codes) {
                Text(
                    labels.remaining?(unused.count, codes.count)
                        ?? MonarchRecoveryCodesStrings.remaining(unused.count, of: codes.count)
                )
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
            }
            panel
            actions(hasUnused: !unused.isEmpty)
            if onAcknowledge != nil {
                MonarchDivider()
                acknowledgement
            }
        }
        .fileExporter(
            isPresented: $isExporting,
            document: MonarchRecoveryCodesDocument(text: MonarchRecoveryCodesTokens.text(codes, heading: heading)),
            contentType: .plainText,
            defaultFilename: fileName,
            onCompletion: exported
        )
        .onChange(of: MonarchRecoveryCodesTokens.signature(codes)) {
            isAcknowledged = false
            showsMissing = false
        }
        .confirmationDialog(
            labels.regenerateTitle ?? MonarchRecoveryCodesStrings.regenerateTitle,
            isPresented: $isConfirmingRegenerate,
            titleVisibility: .visible
        ) {
            Button(labels.regenerateConfirm ?? MonarchRecoveryCodesStrings.regenerateConfirm, role: .destructive) {
                regenerate()
            }
            Button(labels.cancel ?? MonarchRecoveryCodesStrings.cancel, role: .cancel) {}
        } message: {
            Text(labels.regenerateDescription ?? MonarchRecoveryCodesStrings.regenerateDescription)
        }
    }

    @ViewBuilder private var panel: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchRecoveryCodesTokens.panelRadius, style: .continuous)
        Group {
            if isRevealed {
                LazyVGrid(
                    columns: [
                        GridItem(.adaptive(minimum: columnMinWidth), spacing: MonarchRecoveryCodesTokens.gridSpacing)
                    ],
                    alignment: .leading,
                    spacing: MonarchRecoveryCodesTokens.gridSpacing
                ) {
                    ForEach(Array(codes.enumerated()), id: \.offset) { _, code in
                        codeView(code)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.listLabel ?? MonarchRecoveryCodesStrings.listLabel)
            } else {
                HStack(alignment: .firstTextBaseline, spacing: MonarchRecoveryCodesTokens.actionSpacing) {
                    MonarchIcon(systemName: MonarchRecoveryCodesTokens.concealedSymbol, size: .sm)
                    Text(labels.concealedHint ?? MonarchRecoveryCodesStrings.concealedHint)
                        .monarchTextStyle(.body)
                }
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityElement(children: .combine)
            }
        }
        .padding(MonarchRecoveryCodesTokens.panelPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors.bgSubtle))
        .overlay(shape.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchRecoveryCodesTokens.borderWidth))
    }

    private func codeView(_ code: MonarchRecoveryCode) -> some View {
        Text(code.code)
            .monarchTextStyle(.code)
            .strikethrough(code.isUsed)
            .foregroundStyle(theme.colors[role: MonarchRecoveryCodesTokens.codeRole(isUsed: code.isUsed)])
            .textSelection(.enabled)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityLabel(Text(code.code).speechSpellsOutCharacters())
            .accessibilityValue(
                MonarchRecoveryCodesTokens.codeValue(
                    isUsed: code.isUsed,
                    usedLabel: labels.used ?? MonarchRecoveryCodesStrings.used
                )
            )
    }

    private func actions(hasUnused: Bool) -> some View {
        MonarchFlowLayout(spacing: MonarchRecoveryCodesTokens.actionSpacing) {
            if showsRevealToggle {
                MonarchButton(
                    isRevealed
                        ? (labels.conceal ?? MonarchRecoveryCodesStrings.conceal)
                        : (labels.reveal ?? MonarchRecoveryCodesStrings.reveal),
                    systemImage: isRevealed
                        ? MonarchRecoveryCodesTokens.concealSymbol : MonarchRecoveryCodesTokens.revealSymbol,
                    variant: .ghost,
                    size: .sm
                ) {
                    isRevealed.toggle()
                }
            }
            MonarchCopyButton(
                labels.copy ?? MonarchRecoveryCodesStrings.copy,
                value: MonarchRecoveryCodesTokens.text(codes, heading: heading),
                size: .sm,
                copiedTitle: labels.copied ?? MonarchRecoveryCodesStrings.copied
            )
            .disabled(!hasUnused)
            ShareLink(
                item: MonarchRecoveryCodesFile(
                    text: MonarchRecoveryCodesTokens.text(codes, heading: heading),
                    fileName: fileName
                ),
                preview: SharePreview(fileName)
            ) {
                Text(labels.save ?? MonarchRecoveryCodesStrings.save)
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
            .disabled(!hasUnused)
            if onRegenerate != nil {
                MonarchButton(
                    labels.regenerate ?? MonarchRecoveryCodesStrings.regenerate,
                    systemImage: MonarchRecoveryCodesTokens.regenerateSymbol,
                    variant: .danger,
                    size: .sm,
                    isLoading: isRegenerating
                ) {
                    isConfirmingRegenerate = true
                }
            }
        }
    }

    private func exported(_ result: Result<URL, any Error>) {
        let message: String
        switch result {
        case .success: message = labels.saved ?? MonarchRecoveryCodesStrings.saved
        case .failure(let error):
            // Dismissing the file exporter is not a failure.
            if (error as? CocoaError)?.code == .userCancelled { return }
            message = labels.saveFailed ?? MonarchRecoveryCodesStrings.saveFailed
        }
        AccessibilityNotification.Announcement(message).post()
    }

    private var acknowledgement: some View {
        VStack(alignment: .leading, spacing: MonarchRecoveryCodesTokens.actionSpacing) {
            Toggle(labels.acknowledge ?? MonarchRecoveryCodesStrings.acknowledge, isOn: $isAcknowledged)
                .toggleStyle(.monarchCheckbox)
                .onChange(of: isAcknowledged) { showsMissing = false }
            if showsMissing {
                HStack(alignment: .firstTextBaseline, spacing: MonarchRecoveryCodesTokens.iconSpacing) {
                    MonarchIcon(systemName: MonarchRecoveryCodesTokens.errorSymbol, size: .sm)
                    Text(labels.acknowledgeMissing ?? MonarchRecoveryCodesStrings.acknowledgeMissing)
                        .monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityElement(children: .combine)
            }
            MonarchButton(labels.continueTitle ?? MonarchRecoveryCodesStrings.continueTitle, variant: .primary) {
                pressContinue()
            }
        }
    }

    private func pressContinue() {
        switch MonarchRecoveryCodesTokens.continueResult(isAcknowledged: isAcknowledged) {
        case .acknowledge:
            onAcknowledge?()
        case .showError:
            showsMissing = true
            AccessibilityNotification.Announcement(
                labels.acknowledgeMissing ?? MonarchRecoveryCodesStrings.acknowledgeMissing
            ).post()
        }
    }

    private func regenerate() {
        guard let onRegenerate, !isRegenerating else { return }
        isRegenerating = true
        Task {
            await onRegenerate()
            isRegenerating = false
            isRevealed = true
            AccessibilityNotification.Announcement(
                labels.regenerated ?? MonarchRecoveryCodesStrings.regenerated
            ).post()
        }
    }
}

/// The codes as a plain text document for the file exporter.
struct MonarchRecoveryCodesDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.plainText] }

    let text: String

    init(text: String) {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        text = configuration.file.regularFileContents.flatMap { String(data: $0, encoding: .utf8) } ?? ""
    }

    /// The bytes of the saved file.
    var data: Data { Data(text.utf8) }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

/// The codes as a plain text file for the share sheet, so the person can save them to Files.
private struct MonarchRecoveryCodesFile: Transferable, Sendable {
    let text: String
    let fileName: String

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .plainText) { file in
            Data(file.text.utf8)
        }
        .suggestedFileName { $0.fileName }
    }
}

#if DEBUG
    #Preview("MonarchRecoveryCodes") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchRecoveryCodes(
                    [
                        "4f7k-2m9q", "8d3r-6t1w", MonarchRecoveryCode("1x5c-9v2b", isUsed: true), "7n4h-3j8k",
                        "2p6s-5l0z", "9a1e-4u7y",
                    ],
                    onRegenerate: {},
                    onAcknowledge: {}
                )
                MonarchRecoveryCodes(["4f7k-2m9q", "8d3r-6t1w"], startsRevealed: false)
            }
        }
    }
#endif
