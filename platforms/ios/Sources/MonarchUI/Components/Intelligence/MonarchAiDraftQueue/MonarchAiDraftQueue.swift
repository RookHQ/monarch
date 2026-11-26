import SwiftUI

/// A review queue for AI-written drafts (replies, messages, notes): each
/// draft with its context and Approve, Edit, Regenerate, Skip and Send.
/// Nothing is sent without a person approving it.
///
/// ```swift
/// ScrollView {
///     MonarchAiDraftQueue(
///         drafts,
///         title: "Replies to review",
///         onEdit: { id, body in store.update(id, body: body) },
///         onRegenerate: { id in store.regenerate(id) },
///         onSkip: { id in store.set(id, .skipped) },
///         onSend: { id in store.send(id) },
///         onApprove: { id in store.set(id, .approved) }
///     )
///     .padding()
/// }
/// ```
///
/// The queue is controlled: each button asks the app to change a draft's
/// status or body, and the queue shows the drafts it is given. While a
/// draft's status is `regenerating` its body is replaced by "AI is still
/// writing…" and it offers no actions. Edit replaces the body with a
/// multi-line field and Save and Cancel. The body is plain text, never
/// rendered as markup. Place the queue in a `ScrollView`.
///
/// Accessibility: the queue is a container named by its title, or "Drafts
/// to review". Each draft's title is a header; its status is a badge with
/// an icon and a name, so it does not depend on colour; its body is read
/// after "AI draft". After Approve, Send or Skip, VoiceOver moves to the
/// next draft that needs review (wrapping to the top), and every status
/// change is announced, for example "Approved: Reply to Alex Morgan".
/// Buttons have 44-point touch targets and wrap onto new rows.
public struct MonarchAiDraftQueue: View {
    private let drafts: [MonarchAiDraft]
    private let title: String?
    private let advancesOnAction: Bool
    private let labels: MonarchAiDraftQueueLabels
    private let onEdit: @MainActor (_ id: String, _ body: String) -> Void
    private let onRegenerate: @MainActor (_ id: String) -> Void
    private let onSkip: @MainActor (_ id: String) -> Void
    private let onSend: (@MainActor (_ id: String) -> Void)?
    private let onApprove: @MainActor (_ id: String) -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var editingID: String?
    @State private var editText = ""
    @AccessibilityFocusState private var focusedID: String?

    /// A draft queue.
    ///
    /// - Parameters:
    ///   - drafts: The drafts, in review order.
    ///   - title: A heading above the queue, already localised. It also names the queue.
    ///   - advancesOnAction: Whether VoiceOver moves to the next draft that needs review after Approve, Send or Skip.
    ///   - labels: Replaces the built-in strings.
    ///   - onEdit: Receives the id and the trimmed edited text when the person saves.
    ///   - onRegenerate: Asks for a new version; set the status to `regenerating` while it is written.
    ///   - onSkip: Receives the id of a skipped draft.
    ///   - onSend: Adds Send to approved drafts. Receives the id.
    ///   - onApprove: Receives the id of an approved draft.
    public init(
        _ drafts: [MonarchAiDraft],
        title: String? = nil,
        advancesOnAction: Bool = true,
        labels: MonarchAiDraftQueueLabels = MonarchAiDraftQueueLabels(),
        onEdit: @escaping @MainActor (_ id: String, _ body: String) -> Void,
        onRegenerate: @escaping @MainActor (_ id: String) -> Void,
        onSkip: @escaping @MainActor (_ id: String) -> Void,
        onSend: (@MainActor (_ id: String) -> Void)? = nil,
        onApprove: @escaping @MainActor (_ id: String) -> Void
    ) {
        self.drafts = drafts
        self.title = title
        self.advancesOnAction = advancesOnAction
        self.labels = labels
        self.onEdit = onEdit
        self.onRegenerate = onRegenerate
        self.onSkip = onSkip
        self.onSend = onSend
        self.onApprove = onApprove
    }

    private typealias Tokens = MonarchAiDraftQueueTokens

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            header
            if drafts.isEmpty {
                MonarchEmptyState(labels.empty, kind: .success, size: .sm)
            } else {
                ForEach(drafts) { card($0) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title ?? labels.label)
        .onChange(of: Tokens.statuses(drafts)) { previous, _ in
            for text in Tokens.announcements(previous: previous, drafts: drafts, labels: labels) {
                AccessibilityNotification.Announcement(text).post()
            }
        }
    }

    @ViewBuilder private var header: some View {
        VStack(alignment: .leading, spacing: Tokens.textSpacing) {
            if let title {
                Text(title)
                    .monarchTextStyle(.title)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
            }
            if !drafts.isEmpty {
                ProgressView(value: Tokens.fraction(drafts)) {
                    Text(labels.progress(Tokens.reviewedCount(drafts), drafts.count))
                }
                .progressViewStyle(.monarchLinear)
                .accessibilityValue(labels.progress(Tokens.reviewedCount(drafts), drafts.count))
            }
        }
    }

    private func card(_ draft: MonarchAiDraft) -> some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: Tokens.cardSpacing) {
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: Tokens.spacing) {
                        heading(draft)
                        Spacer(minLength: 0)
                        badge(draft.status)
                    }
                    VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                        heading(draft)
                        badge(draft.status)
                    }
                }
                if let context = draft.context, !context.isEmpty {
                    contextBlock(context)
                }
                content(draft)
                actions(draft)
            }
        }
        .monarchLeadingEdge(draft.status == .draft || draft.status == .regenerating ? \.aiBorder : nil)
    }

    private func heading(_ draft: MonarchAiDraft) -> some View {
        Text(draft.title)
            .monarchTextStyle(.heading)
            .foregroundStyle(theme.colors.textDefault)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
            .accessibilityFocused($focusedID, equals: draft.id)
    }

    private func badge(_ status: MonarchAiDraftStatus) -> some View {
        MonarchBadge(labels.status(status), tone: Tokens.tone(status), systemImage: Tokens.symbol(status))
    }

    private func contextBlock(_ context: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.textSpacing) {
            Text(labels.context)
                .monarchTextStyle(.caption, weight: .semibold)
                .foregroundStyle(theme.colors.textMuted)
            Text(context)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(Tokens.contextPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: Tokens.contextRadius, style: .continuous).fill(theme.colors.bgSubtle)
        )
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private func content(_ draft: MonarchAiDraft) -> some View {
        if draft.status == .regenerating {
            MonarchAiThinkingIndicator(label: labels.writing, isAnnounced: false)
        } else if editingID == draft.id {
            MonarchAiDraftQueueEditor(
                label: labels.editField(draft.title),
                text: $editText,
                labels: labels,
                onSave: { save(draft) },
                onCancel: { stopEditing(draft) }
            )
        } else {
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                Text(draft.body)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.bodyRole(draft.status)])
                    .textSelection(.enabled)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityLabel(Tokens.bodyLabel(draft.body, labels: labels))
            }
        }
    }

    @ViewBuilder private func actions(_ draft: MonarchAiDraft) -> some View {
        let actions = Tokens.actions(draft.status, canSend: onSend != nil)
        if !actions.isEmpty, editingID != draft.id {
            MonarchFlowLayout(spacing: Tokens.spacing, rowSpacing: Tokens.spacing) {
                ForEach(actions, id: \.self) { action in
                    MonarchButton(
                        Tokens.title(action, labels: labels),
                        systemImage: Tokens.actionSymbol(action),
                        variant: Tokens.variant(action),
                        size: .sm
                    ) {
                        perform(action, on: draft)
                    }
                }
            }
        }
    }

    private func perform(_ action: Tokens.Action, on draft: MonarchAiDraft) {
        switch action {
        case .approve: onApprove(draft.id)
        case .send: onSend?(draft.id)
        case .skip: onSkip(draft.id)
        case .regenerate: onRegenerate(draft.id)
        case .edit:
            editText = draft.body
            editingID = draft.id
        }
        if advancesOnAction, Tokens.advances(action) {
            focusedID = Tokens.next(after: draft.id, in: drafts) ?? draft.id
        }
    }

    private func save(_ draft: MonarchAiDraft) {
        if let text = Tokens.edited(editText, original: draft.body) {
            onEdit(draft.id, text)
        }
        stopEditing(draft)
    }

    private func stopEditing(_ draft: MonarchAiDraft) {
        editingID = nil
        focusedID = draft.id
    }
}

#if DEBUG
    #Preview("MonarchAiDraftQueue") {
        MonarchPreviewMatrix {
            MonarchAiDraftQueue(
                [
                    MonarchAiDraft(
                        id: "1",
                        title: "Reply to Alex Morgan",
                        body: "Thanks for flagging this, Alex. The refund was issued today.",
                        context: "Alex: I was charged twice for my order."
                    ),
                    MonarchAiDraft(id: "2", title: "Follow-up to Priya", body: "", status: .regenerating),
                    MonarchAiDraft(
                        id: "3",
                        title: "Note for Sam",
                        body: "Moved the call to Friday.",
                        status: .approved
                    ),
                    MonarchAiDraft(id: "4", title: "Reply to Lee", body: "Not needed.", status: .skipped),
                ],
                title: "Replies to review",
                onEdit: { _, _ in },
                onRegenerate: { _ in },
                onSkip: { _ in },
                onSend: { _ in },
                onApprove: { _ in }
            )
        }
    }
#endif
