import SwiftUI

/// One tool call of an assistant turn as a card that opens: what the
/// assistant did in plain words, the state as an icon and a word, how long
/// it took and, when open, the exact input and output.
///
/// ```swift
/// MonarchAiToolCall(part, toolLabels: ["search_documents": "Search documents"]) { part in
///     part.input["query"]?.stringValue.map { "Searched documents for “\($0)”" }
/// }
/// ```
///
/// It is a standard `DisclosureGroup` in ``MonarchDisclosureGroupStyle``,
/// closed by default so a reply with many calls stays short. The title is
/// `summary`, else what `describeTool` returns, else the tool's label from
/// `toolLabels`, else the humanized tool name ("search_documents" reads
/// "Search documents"). The input and output are pretty-printed JSON in
/// ``MonarchAiCodeBlock``, with Copy; a long value is cut at
/// `maxValueLength` characters with a Show all button. A failed call shows
/// its error under the card without opening it.
///
/// Accessibility: the summary row is one button read as the title, the
/// state and the duration, with the value "Expanded" or "Collapsed". Icons
/// are decorative; the state is a word. When the state changes after the
/// card appears, "<tool>: <state>" is announced; the first state is never
/// announced. The error line is read as "Error: message".
public struct MonarchAiToolCall: View {
    private let part: MonarchAiToolCallPart
    private let summary: String?
    private let systemImage: String?
    private let toolLabels: [String: String]
    private let isExpandedBinding: Binding<Bool>?
    private let announcesChanges: Bool
    private let maxValueLength: Int
    private let labels: MonarchAiToolCallLabels
    private let describeTool: (@MainActor (MonarchAiToolCallPart) -> String?)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var ownIsExpanded = false

    /// A tool call card.
    ///
    /// - Parameters:
    ///   - part: The tool call, straight from the conversation model.
    ///   - summary: The one-line summary in words, already localised. Wins over `describeTool`.
    ///   - systemImage: Replaces the wrench symbol before the title. Decorative.
    ///   - toolLabels: Display names by tool name. Unlisted tools get a humanized name.
    ///   - isExpanded: Whether the details are open. Kept inside, closed at first, when `nil`.
    ///   - announcesChanges: Whether a change of state is announced to VoiceOver.
    ///     Turn it off when the thread already announces it.
    ///   - maxValueLength: The longest input or output shown before Show all, in
    ///     characters. 0 shows everything.
    ///   - labels: Replacements for the built-in strings.
    ///   - describeTool: Builds the summary from the call, for example from its
    ///     input. Return `nil` to fall back to the tool's label.
    public init(
        _ part: MonarchAiToolCallPart,
        summary: String? = nil,
        systemImage: String? = nil,
        toolLabels: [String: String] = [:],
        isExpanded: Binding<Bool>? = nil,
        announcesChanges: Bool = true,
        maxValueLength: Int = 4000,
        labels: MonarchAiToolCallLabels = MonarchAiToolCallLabels(),
        describeTool: (@MainActor (MonarchAiToolCallPart) -> String?)? = nil
    ) {
        self.part = part
        self.summary = summary
        self.systemImage = systemImage
        self.toolLabels = toolLabels
        self.isExpandedBinding = isExpanded
        self.announcesChanges = announcesChanges
        self.maxValueLength = maxValueLength
        self.labels = labels
        self.describeTool = describeTool
    }

    private var isExpanded: Binding<Bool> { isExpandedBinding ?? $ownIsExpanded }

    private var label: String { MonarchAiToolCallTokens.toolLabel(part.name, toolLabels: toolLabels) }

    /// The content of the view.
    public var body: some View {
        let shape = MonarchShapes.container
        VStack(alignment: .leading, spacing: MonarchAiToolCallTokens.spacing) {
            DisclosureGroup(isExpanded: isExpanded) {
                details
            } label: {
                header
            }
            .disclosureGroupStyle(.monarch)
            if part.state == .failed, let error = part.error, !error.isEmpty {
                errorLine(error)
            }
        }
        .padding(MonarchAiToolCallTokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchAiToolCallTokens.fill]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchAiToolCallTokens.border],
                lineWidth: MonarchAiToolCallTokens.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
        .onChange(of: part.state) { _, newState in
            guard announcesChanges else { return }
            AccessibilityNotification.Announcement(
                MonarchAiToolCallTokens.announcement(
                    label: label,
                    state: MonarchAiToolCallTokens.stateName(newState, labels: labels),
                    labels: labels
                )
            )
            .post()
        }
    }

    private var header: some View {
        let title = MonarchAiToolCallTokens.title(summary: summary, described: describeTool?(part), label: label)
        let stateName = MonarchAiToolCallTokens.stateName(part.state, labels: labels)
        let look = MonarchAiToolCallTokens.stateLook(part.state)
        let duration = MonarchAiToolCallTokens.duration(part).map {
            MonarchAiToolCallTokens.durationText($0, locale: locale, labels: labels)
        }
        return HStack(alignment: .firstTextBaseline, spacing: MonarchAiToolCallTokens.headerSpacing) {
            MonarchIcon(systemName: systemImage ?? MonarchAiToolCallTokens.toolSymbol, size: .sm)
                .foregroundStyle(theme.colors[role: MonarchAiToolCallTokens.toolIcon])
            MonarchFlowLayout(spacing: MonarchAiToolCallTokens.headerSpacing) {
                Text(title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                MonarchStatusBadge(
                    MonarchStatusDefinition(stateName, tone: look.tone, systemImage: look.symbol),
                    size: .sm
                )
                if let duration {
                    Text(duration)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchAiToolCallTokens.secondaryText])
                        .monospacedDigit()
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchAiToolCallTokens.rowLabel(title: title, state: stateName, duration: duration))
    }

    @ViewBuilder private var details: some View {
        let input = MonarchAiToolCallTokens.inputValue(part)
        VStack(alignment: .leading, spacing: MonarchAiToolCallTokens.spacing) {
            Text(MonarchAiToolCallStrings.toolLine(labels.tool ?? MonarchAiToolCallStrings.tool, part.name))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchAiToolCallTokens.secondaryText])
            if let input {
                MonarchAiToolCallValue(
                    title: labels.input ?? MonarchAiToolCallStrings.input,
                    value: input,
                    maxLength: maxValueLength,
                    labels: labels
                )
            }
            if let output = part.output {
                MonarchAiToolCallValue(
                    title: labels.output ?? MonarchAiToolCallStrings.output,
                    value: output,
                    maxLength: maxValueLength,
                    labels: labels
                )
            }
            if input == nil, part.output == nil {
                Text(labels.empty ?? MonarchAiToolCallStrings.empty)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiToolCallTokens.secondaryText])
            }
        }
    }

    private func errorLine(_ error: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchAiToolCallTokens.headerSpacing) {
            MonarchIcon(systemName: MonarchAiToolCallTokens.errorSymbol, size: .sm)
            Text(error)
                .monarchTextStyle(.caption)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .foregroundStyle(theme.colors[role: MonarchAiToolCallTokens.errorText])
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchAiToolCallStrings.errorLine(labels.error ?? MonarchAiToolCallStrings.error, error))
    }
}

/// One value of the details: its heading and the code block, cut to the
/// longest length with a Show all button.
private struct MonarchAiToolCallValue: View {
    let title: String
    let value: MonarchAiJSON
    let maxLength: Int
    let labels: MonarchAiToolCallLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var showsAll = false

    var body: some View {
        let formatted = MonarchAiToolCallTokens.formatted(value)
        let shown = MonarchAiToolCallTokens.truncated(formatted.text, max: showsAll ? 0 : maxLength)
        VStack(alignment: .leading, spacing: MonarchAiToolCallTokens.spacing) {
            MonarchAiCodeBlock(
                shown.text,
                language: formatted.isJSON ? "json" : nil,
                title: title,
                showsWrapToggle: false,
                isWrapped: .constant(true)
            )
            if shown.isTruncated {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiToolCallTokens.spacing) {
                    Text(
                        MonarchAiToolCallTokens.truncatedNote(
                            shown: shown.text.count,
                            total: formatted.text.count,
                            locale: locale,
                            labels: labels
                        )
                    )
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiToolCallTokens.secondaryText])
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Button(labels.showMore ?? MonarchAiToolCallStrings.showMore) {
                        showsAll = true
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                }
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAiToolCall") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiToolCall(
                    MonarchAiToolCallPart(
                        id: "1",
                        name: "search_documents",
                        input: ["query": "Q3 plan"],
                        state: .succeeded,
                        output: ["count": 3],
                        startedAt: "2026-01-01T10:00:00Z",
                        endedAt: "2026-01-01T10:00:02Z"
                    ),
                    isExpanded: .constant(true)
                )
                ForEach(MonarchAiToolCallState.allCases, id: \.self) { state in
                    MonarchAiToolCall(
                        MonarchAiToolCallPart(id: state.rawValue, name: "send_email", state: state, error: "Timed out")
                    )
                }
            }
        }
    }
#endif
