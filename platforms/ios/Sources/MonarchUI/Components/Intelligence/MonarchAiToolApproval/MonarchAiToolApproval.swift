import SwiftUI

/// The approval card for a tool call the assistant wants to run, such as
/// "Send email to 3 people" or "Delete 12 records": what it will do, its
/// arguments, the risk, and Approve and Deny.
///
/// ```swift
/// MonarchAiToolApproval(
///     part,
///     summary: "Send email to 3 people",
///     onDeny: { reason in conversation.denyTool(part.id, reason: reason) },
///     onApprove: { input in conversation.approveTool(part.id, input: input) }
/// )
/// ```
///
/// The card is a ``MonarchCard``. The arguments are shown as readable
/// key and value pairs. People can edit them as JSON before approving
/// (`onApprove` receives the new input, or `nil` when nothing changed), give
/// a reason when denying, and, when `onAlwaysAllow` is set, tick "Always
/// allow this tool". A destructive call uses the danger tone, states that it
/// cannot be undone, never offers Always allow and its Approve button is the
/// danger variant. Once decided, or when the call is no longer waiting, the
/// buttons give way to the outcome: Approved, Denied or Cancelled. A new
/// call (another `part.id`) starts over.
///
/// Accessibility: the card is a container read as "Approval needed", the
/// title and the risk. It is announced when it appears ("Approval needed:
/// Send email"). With `focusesOnAppear`, VoiceOver focus moves to Deny for
/// high and destructive calls and to Approve otherwise. Invalid JSON is an
/// error with an icon and text under the editor. The risk and outcome are
/// words with icons, not colour alone. Buttons are at least 44 points.
@_spi(Experimental) public struct MonarchAiToolApproval: View {
    private let part: MonarchAiToolCallPart
    private let summary: String?
    private let consequence: String?
    private let riskOverride: MonarchAiToolCallRisk?
    private let toolLabels: [String: String]
    private let argumentLabels: [String: String]
    private let argumentOrder: [String]
    private let isEditable: Bool
    private let focusesOnAppear: Bool
    private let labels: MonarchAiToolApprovalLabels
    private let onAlwaysAllow: (@MainActor (String) -> Void)?
    private let onDeny: @MainActor (String?) -> Void
    private let onApprove: @MainActor (MonarchAiJSON?) -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var decision: MonarchAiToolApprovalTokens.Outcome?
    @State private var isEditing = false
    @State private var json = ""
    @State private var hasJSONError = false
    @State private var isReasonOpen = false
    @State private var reason = ""
    @State private var alwaysAllows = false
    @AccessibilityFocusState private var focusedButton: MonarchAiToolApprovalTokens.Outcome?

    /// A tool approval card.
    ///
    /// - Parameters:
    ///   - part: The tool call waiting for a decision (`state` `.pendingApproval`).
    ///   - summary: The title in plain words, such as "Delete 12 records". The tool's label by default.
    ///   - consequence: What happens if this is approved. Destructive calls say they cannot be undone by default.
    ///   - risk: How much harm approving can do. The call's own `risk`, else medium.
    ///   - toolLabels: Display names by tool name. Unlisted tools get a humanized name.
    ///   - argumentLabels: Labels for argument keys, such as `["to": "Recipients"]`. Unlisted keys are humanized.
    ///   - argumentOrder: The argument keys to show first, in this order. The other keys follow, sorted.
    ///   - isEditable: Offers Edit arguments.
    ///   - focusesOnAppear: Moves VoiceOver focus into the card when it appears.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAlwaysAllow: Shows "Always allow this tool". When ticked, called
    ///     with the tool name just before `onApprove`. Never offered for destructive calls.
    ///   - onDeny: Called when the person denies, with the reason they typed, if any.
    ///   - onApprove: Called when the person approves, with the edited input,
    ///     or `nil` when the arguments were not changed.
    public init(
        _ part: MonarchAiToolCallPart,
        summary: String? = nil,
        consequence: String? = nil,
        risk: MonarchAiToolCallRisk? = nil,
        toolLabels: [String: String] = [:],
        argumentLabels: [String: String] = [:],
        argumentOrder: [String] = [],
        isEditable: Bool = true,
        focusesOnAppear: Bool = false,
        labels: MonarchAiToolApprovalLabels = MonarchAiToolApprovalLabels(),
        onAlwaysAllow: (@MainActor (String) -> Void)? = nil,
        onDeny: @escaping @MainActor (String?) -> Void,
        onApprove: @escaping @MainActor (MonarchAiJSON?) -> Void
    ) {
        self.part = part
        self.summary = summary
        self.consequence = consequence
        self.riskOverride = risk
        self.toolLabels = toolLabels
        self.argumentLabels = argumentLabels
        self.argumentOrder = argumentOrder
        self.isEditable = isEditable
        self.focusesOnAppear = focusesOnAppear
        self.labels = labels
        self.onAlwaysAllow = onAlwaysAllow
        self.onDeny = onDeny
        self.onApprove = onApprove
    }

    private typealias Tokens = MonarchAiToolApprovalTokens

    private var risk: MonarchAiToolCallRisk { Tokens.risk(riskOverride, part: part) }
    private var title: String { Tokens.title(summary: summary, part: part, toolLabels: toolLabels) }
    private var isResolved: Bool { Tokens.isResolved(part.state, decision: decision) }
    private var offersAlwaysAllow: Bool {
        Tokens.offersAlwaysAllow(hasCallback: onAlwaysAllow != nil, risk: risk)
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                header
                if let line = Tokens.consequence(consequence, risk: risk, labels: labels) {
                    consequenceLine(line)
                }
                argumentsView
                if isResolved {
                    outcomeLine(Tokens.outcome(part.state, decision: decision))
                } else {
                    decisionControls
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .contain)
        .task(id: part.id) { appear() }
    }

    private var header: some View {
        let eyebrow = labels.eyebrow ?? MonarchAiToolApprovalStrings.eyebrow
        let riskName = Tokens.riskName(risk, labels: labels)
        return HStack(alignment: .firstTextBaseline, spacing: Tokens.buttonSpacing) {
            MonarchIcon(systemName: Tokens.warningSymbol)
                .foregroundStyle(theme.colors[role: Tokens.accent(risk)])
            VStack(alignment: .leading, spacing: Tokens.tightSpacing) {
                Text(eyebrow)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.secondaryText])
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                MonarchBadge(riskName, tone: Tokens.tone(risk))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.headerLabel(eyebrow: eyebrow, title: title, risk: riskName))
        .accessibilityAddTraits(.isHeader)
    }

    private func consequenceLine(_ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.tightSpacing) {
            MonarchIcon(systemName: Tokens.warningSymbol, size: .sm)
            Text(text)
                .monarchTextStyle(.body)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .foregroundStyle(theme.colors[role: Tokens.accent(risk)])
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var argumentsView: some View {
        VStack(alignment: .leading, spacing: Tokens.tightSpacing) {
            Text(labels.arguments ?? MonarchAiToolApprovalStrings.arguments)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            if Tokens.hasNoArguments(part.input) {
                Text(labels.noArguments ?? MonarchAiToolApprovalStrings.noArguments)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.secondaryText])
            } else if let arguments = Tokens.arguments(part.input, argumentLabels: argumentLabels, order: argumentOrder)
            {
                ForEach(arguments, id: \.key) { argument in
                    VStack(alignment: .leading, spacing: 0) {
                        Text(argument.label)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: Tokens.secondaryText])
                        Text(argument.value)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textDefault)
                            .textSelection(.enabled)
                    }
                    .accessibilityElement(children: .combine)
                }
            } else {
                Text(Tokens.readable(part.input))
                    .monarchTextStyle(.code)
                    .foregroundStyle(theme.colors.textDefault)
                    .textSelection(.enabled)
            }
        }
    }

    private func outcomeLine(_ outcome: MonarchAiToolApprovalTokens.Outcome) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.tightSpacing) {
            MonarchIcon(systemName: Tokens.outcomeSymbol(outcome), size: .sm)
            Text(Tokens.outcomeText(outcome, labels: labels)).monarchTextStyle(.label)
        }
        .foregroundStyle(theme.colors[role: Tokens.outcomeColor(outcome)])
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var decisionControls: some View {
        if isEditing {
            MonarchFormField(
                labels.argumentsJSON ?? MonarchAiToolApprovalStrings.argumentsJSON,
                error: hasJSONError ? labels.invalidJSON ?? MonarchAiToolApprovalStrings.invalidJSON : nil
            ) {
                MonarchActionEditorMultilineField(
                    label: labels.argumentsJSON ?? MonarchAiToolApprovalStrings.argumentsJSON,
                    text: $json,
                    prompt: nil
                )
                .autocorrectionDisabled()
                .onChange(of: json) { hasJSONError = false }
            }
        }
        if isReasonOpen {
            MonarchFormField(labels.reason ?? MonarchAiToolApprovalStrings.reason) {
                MonarchActionEditorMultilineField(
                    label: labels.reason ?? MonarchAiToolApprovalStrings.reason,
                    text: $reason,
                    prompt: nil
                )
            }
        }
        if offersAlwaysAllow {
            Toggle(labels.alwaysAllow ?? MonarchAiToolApprovalStrings.alwaysAllow, isOn: $alwaysAllows)
                .toggleStyle(.monarchCheckbox)
        }
        MonarchFlowLayout(spacing: Tokens.buttonSpacing) {
            if isEditable {
                Button(
                    isEditing
                        ? labels.discardEdits ?? MonarchAiToolApprovalStrings.discardEdits
                        : labels.editArguments ?? MonarchAiToolApprovalStrings.editArguments
                ) {
                    toggleEditing()
                }
                .buttonStyle(.monarch(.ghost, size: Tokens.buttonSize))
            }
            if !isReasonOpen {
                Button(labels.addReason ?? MonarchAiToolApprovalStrings.addReason) { isReasonOpen = true }
                    .buttonStyle(.monarch(.ghost, size: Tokens.buttonSize))
            }
            Button(labels.deny ?? MonarchAiToolApprovalStrings.deny) { deny() }
                .buttonStyle(.monarch(.secondary, size: Tokens.buttonSize))
                .accessibilityFocused($focusedButton, equals: .denied)
            Button(labels.approve ?? MonarchAiToolApprovalStrings.approve) { approve() }
                .buttonStyle(.monarch(Tokens.approveVariant(risk), size: Tokens.buttonSize))
                .accessibilityFocused($focusedButton, equals: .approved)
        }
    }

    private func appear() {
        decision = nil
        isEditing = false
        json = Tokens.editorText(part.input)
        hasJSONError = false
        isReasonOpen = false
        reason = ""
        alwaysAllows = false
        guard part.state == .pendingApproval else { return }
        AccessibilityNotification.Announcement(Tokens.announcement(title, labels: labels)).post()
        if focusesOnAppear {
            focusedButton = Tokens.focusesDeny(risk) ? .denied : .approved
        }
    }

    private func toggleEditing() {
        if isEditing {
            json = Tokens.editorText(part.input)
            hasJSONError = false
        }
        isEditing.toggle()
    }

    private func approve() {
        guard !isResolved else { return }
        var input: MonarchAiJSON?
        if isEditing {
            switch Tokens.edited(json, original: Tokens.editorText(part.input)) {
            case .unchanged: input = nil
            case .changed(let value): input = value
            case .invalid:
                hasJSONError = true
                return
            }
        }
        if alwaysAllows && offersAlwaysAllow { onAlwaysAllow?(part.name) }
        decision = .approved
        onApprove(input)
    }

    private func deny() {
        guard !isResolved else { return }
        let why = reason.trimmingCharacters(in: .whitespacesAndNewlines)
        decision = .denied
        onDeny(why.isEmpty ? nil : why)
    }
}

#if DEBUG
    #Preview("MonarchAiToolApproval") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiToolApproval(
                    MonarchAiToolCallPart(
                        id: "1",
                        name: "send_email",
                        input: ["to": ["ada@example.com", "sam@example.com"], "subject": "Q3 plan"],
                        state: .pendingApproval
                    ),
                    summary: "Send email to 2 people",
                    onAlwaysAllow: { _ in },
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
                MonarchAiToolApproval(
                    MonarchAiToolCallPart(
                        id: "2",
                        name: "delete_records",
                        input: ["count": 12],
                        state: .pendingApproval
                    ),
                    risk: .destructive,
                    onDeny: { _ in },
                    onApprove: { _ in }
                )
            }
        }
    }
#endif
