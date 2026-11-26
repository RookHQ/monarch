import SwiftUI

/// A preview of what a tool did (a record created, an email sent, an event
/// booked, a ticket filed): the kind of action, the title, a line of detail,
/// key facts, a link to open the result and an optional Undo.
///
/// ```swift
/// MonarchAiToolResultCard(
///     "Acme Ltd",
///     kind: .recordCreated,
///     description: "Added to Accounts",
///     url: URL(string: "https://crm.example.com/accounts/42"),
///     meta: [MonarchKeyValueItem("Owner", value: "Ana Flores")],
///     onUndo: { try await crm.delete(42) }
/// )
/// ```
///
/// Undo runs `onUndo`: the button shows a progress indicator until it
/// returns, then the card says "Undone" and drops Open and Undo. When
/// `onUndo` throws, the card says "Couldn’t undo. Try again." and Undo stays.
///
/// Accessibility: the kind, title and description are one element ("Record
/// created, Acme Ltd, Added to Accounts"). Each fact is read as its key and
/// value. Open is a link and Undo a button, each a separate element with a
/// 44-point touch target. "Undone" and the failure are shown as text and
/// announced. The icon is decorative; the kind is spoken.
public struct MonarchAiToolResultCard: View {
    private let title: String
    private let kind: MonarchAiToolResultKind
    private let description: String?
    private let url: URL?
    private let meta: [MonarchKeyValueItem]
    private let systemImage: String?
    private let labels: MonarchAiToolResultCardLabels
    private let onUndo: (@MainActor () async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var tileSide = MonarchAiToolResultCardTokens.tileSide
    @State private var undoState = MonarchAiToolResultCardTokens.UndoState.idle

    /// A tool result card.
    ///
    /// - Parameters:
    ///   - title: What the tool made or changed, such as the record's name or the email's subject.
    ///   - kind: What the tool did, which picks the icon and the label above the title.
    ///   - description: One line of detail, such as "To 3 people".
    ///   - url: Opens the result. Only `http`, `https` and `mailto` addresses are shown.
    ///   - meta: Short facts shown as key and value pairs.
    ///   - systemImage: Replaces the SF Symbol of the kind.
    ///   - labels: Replacements for the built-in strings.
    ///   - onUndo: Shows Undo. Reverts what the tool did; throw to report a failure.
    public init(
        _ title: String,
        kind: MonarchAiToolResultKind = .generic,
        description: String? = nil,
        url: URL? = nil,
        meta: [MonarchKeyValueItem] = [],
        systemImage: String? = nil,
        labels: MonarchAiToolResultCardLabels = MonarchAiToolResultCardLabels(),
        onUndo: (@MainActor () async throws -> Void)? = nil
    ) {
        self.title = title
        self.kind = kind
        self.description = description
        self.url = MonarchAiToolResultCardTokens.safeURL(url)
        self.meta = meta
        self.systemImage = systemImage
        self.labels = labels
        self.onUndo = onUndo
    }

    private var kindName: String { labels.kind?(kind) ?? MonarchAiToolResultCardStrings.kind(kind) }

    /// The content of the view.
    public var body: some View {
        MonarchCard(elevation: .none) {
            HStack(alignment: .top, spacing: MonarchAiToolResultCardTokens.iconSpacing) {
                tile
                VStack(alignment: .leading, spacing: MonarchAiToolResultCardTokens.sectionSpacing) {
                    text
                    if !meta.isEmpty {
                        MonarchKeyValue(meta, size: .sm, layout: .stack)
                    }
                    actions
                    status
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var tile: some View {
        let colors = MonarchAiToolResultCardTokens.tile(isUndone: undoState == .done)
        let shape = RoundedRectangle(cornerRadius: MonarchAiToolResultCardTokens.tileCornerRadius, style: .continuous)
        return MonarchIcon(systemName: systemImage ?? MonarchAiToolResultCardTokens.symbol(kind), size: .sm)
            .foregroundStyle(theme.colors[role: colors.foreground])
            .frame(width: tileSide, height: tileSide)
            .background(shape.fill(theme.colors[role: colors.background]))
    }

    private var text: some View {
        VStack(alignment: .leading, spacing: MonarchAiToolResultCardTokens.textSpacing) {
            Text(kindName)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            Text(title)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchAiToolResultCardTokens.accessibilityLabel(kind: kindName, title: title, description: description)
        )
    }

    @ViewBuilder private var actions: some View {
        let showsOpen = MonarchAiToolResultCardTokens.showsOpen(url: url, state: undoState)
        let showsUndo = MonarchAiToolResultCardTokens.showsUndo(hasHandler: onUndo != nil, state: undoState)
        if showsOpen || showsUndo {
            HStack(spacing: MonarchAiToolResultCardTokens.actionSpacing) {
                if showsOpen, let url {
                    Link(labels.open ?? MonarchAiToolResultCardStrings.open, destination: url)
                        .buttonStyle(.monarchLink(.standalone))
                        .monarchTextStyle(.label)
                }
                if showsUndo {
                    MonarchButton(
                        labels.undo ?? MonarchAiToolResultCardStrings.undo,
                        systemImage: "arrow.uturn.backward",
                        variant: .ghost,
                        size: .sm,
                        isLoading: undoState == .pending,
                        action: runUndo
                    )
                }
            }
        }
    }

    @ViewBuilder private var status: some View {
        if let message = statusMessage {
            Text(message)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchAiToolResultCardTokens.statusColor(undoState)])
        }
    }

    private var statusMessage: String? {
        switch undoState {
        case .done: labels.undone ?? MonarchAiToolResultCardStrings.undone
        case .failed: labels.undoFailed ?? MonarchAiToolResultCardStrings.undoFailed
        case .idle, .pending: nil
        }
    }

    private func runUndo() {
        guard let onUndo, MonarchAiToolResultCardTokens.canStartUndo(undoState) else { return }
        undoState = .pending
        Task {
            do {
                try await onUndo()
                undoState = .done
            } catch {
                undoState = .failed
            }
            if let statusMessage {
                AccessibilityNotification.Announcement(statusMessage).post()
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAiToolResultCard") {
        MonarchPreviewMatrix {
            VStack {
                MonarchAiToolResultCard(
                    "Acme Ltd",
                    kind: .recordCreated,
                    description: "Added to Accounts",
                    url: URL(string: "https://crm.example.com/accounts/42"),
                    meta: [MonarchKeyValueItem("Owner", value: "Ana Flores")],
                    onUndo: {}
                )
                ForEach([MonarchAiToolResultKind.emailSent, .eventBooked, .ticketFiled, .generic], id: \.self) { kind in
                    MonarchAiToolResultCard("Quarterly review", kind: kind)
                }
            }
        }
    }
#endif
