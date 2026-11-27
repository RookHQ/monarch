import SwiftUI

/// Shows a newly created secret exactly once: the value with a copy button,
/// a "store it now" warning, and an acknowledgement the person must tick
/// before the sheet can close. Present it in a sheet.
///
/// ```swift
/// .sheet(item: $createdKey) { key in
///     MonarchKeyCreatedDialog(key.secret, name: key.name, onDone: { createdKey = nil })
/// }
/// ```
///
/// Until the acknowledgement is ticked, the sheet cannot be swiped away
/// (`interactiveDismissDisabled`). Done stays enabled: pressing it early
/// shows and announces why the sheet stays open and moves VoiceOver to the
/// checkbox. A `public` value (a publishable key) gets an information notice
/// and nothing to confirm. A new secret starts unacknowledged again. The
/// value is shown plainly in a ``MonarchCopyField`` or, with `isMasked`, in a
/// read-only ``MonarchSecretField`` that copies without revealing.
///
/// Accessibility: the title is a header. The notice speaks its tone. The
/// value is read as what it is ("API key") with the value, and its copy
/// button as "Copy API key". The acknowledgement is a real toggle with a
/// visible label. Every control has a 44-point touch target.
public struct MonarchKeyCreatedDialog: View {
    private let secret: String
    private let label: String?
    private let name: String?
    private let title: String?
    private let description: String?
    private let kind: MonarchKeyCreatedDialogKind
    private let requiresAcknowledgement: Bool?
    private let isMasked: Bool
    private let isMultiline: Bool
    private let note: String?
    private let labels: MonarchKeyCreatedDialogLabels
    private let onCopy: (@MainActor (String) -> Void)?
    private let onDone: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @State private var isAcknowledged = false
    @State private var isNudged = false
    @AccessibilityFocusState private var isAcknowledgementFocused: Bool

    /// A key created dialog, the content of a sheet.
    ///
    /// - Parameters:
    ///   - secret: The value to show once: an API key, token, signing secret or client secret.
    ///   - label: What the value is, such as "Signing secret". Names the copy
    ///     button and goes into the acknowledgement. "API key" when `nil`.
    ///   - name: The name the person gave it, such as "Production server".
    ///     Shown above the value instead of `label`.
    ///   - title: Replaces the built-in title "Copy your new key".
    ///   - description: Text under the title.
    ///   - kind: `secret` (shown once, with a warning) or `public` (viewable again).
    ///   - requiresAcknowledgement: Whether the checkbox must be ticked before
    ///     closing. By default for a secret and not for a public value.
    ///   - isMasked: Whether the value is masked behind a reveal toggle. Copy still works.
    ///   - isMultiline: Whether the whole value wraps over several lines, for
    ///     certificates and private keys. Ignored when masked.
    ///   - note: Extra text under the value, such as when an old key stops working.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied text after each copy.
    ///   - onDone: Runs when Done closes the sheet after the value was acknowledged.
    public init(
        _ secret: String,
        label: String? = nil,
        name: String? = nil,
        title: String? = nil,
        description: String? = nil,
        kind: MonarchKeyCreatedDialogKind = .secret,
        requiresAcknowledgement: Bool? = nil,
        isMasked: Bool = false,
        isMultiline: Bool = false,
        note: String? = nil,
        labels: MonarchKeyCreatedDialogLabels = MonarchKeyCreatedDialogLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil,
        onDone: (@MainActor () -> Void)? = nil
    ) {
        self.secret = secret
        self.label = label
        self.name = name
        self.title = title
        self.description = description
        self.kind = kind
        self.requiresAcknowledgement = requiresAcknowledgement
        self.isMasked = isMasked
        self.isMultiline = isMultiline
        self.note = note
        self.labels = labels
        self.onCopy = onCopy
        self.onDone = onDone
    }

    private var resolvedLabel: String { label ?? labels.resolvedLabel }

    private var needsAcknowledgement: Bool {
        MonarchKeyCreatedDialogTokens.needsAcknowledgement(kind: kind, requires: requiresAcknowledgement)
    }

    private var isConfirmed: Bool {
        MonarchKeyCreatedDialogTokens.isConfirmed(
            needsAcknowledgement: needsAcknowledgement,
            isAcknowledged: isAcknowledged
        )
    }

    /// The content of the view.
    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonarchKeyCreatedDialogTokens.sectionSpacing) {
                header
                let notice = MonarchKeyCreatedDialogTokens.notice(kind: kind, labels: labels)
                MonarchBanner(notice.title, message: notice.message, tone: notice.tone)
                field
                if let note, !note.isEmpty {
                    Text(note)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if needsAcknowledgement {
                    acknowledgement
                }
                // Right-aligned, as the web dialog's footer.
                HStack {
                    Spacer(minLength: 0)
                    MonarchButton(labels.resolvedDone, variant: .primary) {
                        done()
                    }
                }
            }
            .padding(MonarchKeyCreatedDialogTokens.padding)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(theme.colors.bgSurface)
        .interactiveDismissDisabled(!isConfirmed)
        .presentationDragIndicator(isConfirmed ? .visible : .hidden)
        .onChange(of: secret) {
            isAcknowledged = false
            isNudged = false
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchKeyCreatedDialogTokens.textSpacing) {
            Text(title ?? labels.resolvedTitle)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    @ViewBuilder private var field: some View {
        let fieldLabel = MonarchKeyCreatedDialogTokens.fieldLabel(name: name, label: resolvedLabel)
        let copyName = labels.resolvedCopy + " " + resolvedLabel
        if isMasked {
            MonarchSecretField(
                fieldLabel,
                text: .constant(secret),
                isCopyable: true,
                isReadOnly: true,
                labels: MonarchSecretFieldLabels(copy: { _ in copyName }, copied: labels.resolvedCopied),
                onCopy: onCopy
            )
        } else {
            VStack(alignment: .leading, spacing: MonarchKeyCreatedDialogTokens.textSpacing) {
                Text(fieldLabel)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityHidden(true)
                MonarchCopyField(
                    resolvedLabel,
                    value: secret,
                    buttonVariant: .primary,
                    isMultiline: isMultiline,
                    isLabelHidden: true,
                    copyTitle: labels.resolvedCopy,
                    copiedTitle: labels.resolvedCopied,
                    onCopy: onCopy
                )
            }
        }
    }

    private var acknowledgement: some View {
        VStack(alignment: .leading, spacing: MonarchKeyCreatedDialogTokens.textSpacing) {
            Toggle(isOn: $isAcknowledged) {
                Text(labels.acknowledgeText(resolvedLabel))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .toggleStyle(.monarchCheckbox)
            .padding(MonarchKeyCreatedDialogTokens.textSpacing * 2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .overlay(
                MonarchShapes.control.strokeBorder(theme.colors.borderDefault, lineWidth: MonarchShapes.borderWidth)
            )
            .accessibilityFocused($isAcknowledgementFocused)
            .accessibilityHint(
                MonarchKeyCreatedDialogTokens.showsNudge(isNudged: isNudged, isAcknowledged: isAcknowledged)
                    ? labels.resolvedAcknowledgeRequired : ""
            )
            if MonarchKeyCreatedDialogTokens.showsNudge(isNudged: isNudged, isAcknowledged: isAcknowledged) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchKeyCreatedDialogTokens.messageIconSpacing) {
                    MonarchIcon(systemName: MonarchKeyCreatedDialogTokens.errorSymbol, size: .sm)
                    Text(labels.resolvedAcknowledgeRequired)
                        .monarchTextStyle(.caption)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .foregroundStyle(theme.colors[role: MonarchKeyCreatedDialogTokens.errorRole])
                .accessibilityHidden(true)
            }
        }
    }

    private func done() {
        switch MonarchKeyCreatedDialogTokens.doneOutcome(isConfirmed: isConfirmed) {
        case .close:
            onDone?()
            dismiss()
        case .nudge:
            isNudged = true
            AccessibilityNotification.Announcement(labels.resolvedAcknowledgeRequired).post()
            isAcknowledgementFocused = true
        }
    }
}

#if DEBUG
    #Preview("MonarchKeyCreatedDialog") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchKeyCreatedDialog("sk_live_51HxQ2JkL0mNpQr", name: "Production server")
                MonarchKeyCreatedDialog("pk_live_9d2f7a61", label: "Publishable key", kind: .public)
                MonarchKeyCreatedDialog("whsec_9d2f7a61c0b84e3f", label: "Signing secret", isMasked: true)
            }
        }
    }
#endif
