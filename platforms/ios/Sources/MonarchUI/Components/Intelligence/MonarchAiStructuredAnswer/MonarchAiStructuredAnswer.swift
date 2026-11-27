import SwiftUI

/// A model's structured output shown as labelled fields (text, lists,
/// numbers, badges or Markdown) instead of a wall of prose: an interview
/// brief, a lead summary, a record extracted from a document. Fields fill
/// in as they stream, with placeholders for the ones still to come.
///
/// ```swift
/// MonarchAiStructuredAnswer(
///     [
///         MonarchAiStructuredField("candidate", label: "Candidate"),
///         MonarchAiStructuredField("stage", label: "Stage", type: .badge, tone: .info),
///         MonarchAiStructuredField("summary", label: "Summary", type: .markdown),
///     ],
///     data: brief,
///     title: "Interview brief",
///     isStreaming: message.isStreaming
/// )
/// ```
///
/// The schema decides the layout and the model only fills values, so
/// answers look the same every time. Values are `[String: MonarchAiJSON]`,
/// the JSON type of the conversation model, and are treated as untrusted:
/// text is shown as text, Markdown through ``MonarchAiMarkdown`` without
/// HTML. Keys missing from the schema are ignored. A missing value shows a
/// placeholder while streaming and a dash once the answer is complete.
///
/// Accessibility: the provenance label ("AI answer") and the title are read
/// together as a heading. Each field is one element read as its label and
/// then its value, "Still generating" or "Not provided"; list and badge
/// items are read separated by commas. A Markdown field keeps its own
/// elements so its links stay reachable. "Response complete." is announced
/// once when streaming ends, never per field. Placeholders are static
/// shapes, so they need no Reduce Motion form.
public struct MonarchAiStructuredAnswer<Actions: View>: View {
    private let schema: [MonarchAiStructuredField]
    private let data: [String: MonarchAiJSON]
    private let title: String?
    private let layout: MonarchAiStructuredAnswerLayout
    private let isStreaming: Bool
    private let labels: MonarchAiStructuredAnswerLabels
    private let onOpenLink: (@MainActor (URL) -> Void)?
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private typealias Tokens = MonarchAiStructuredAnswerTokens

    /// A structured answer with actions after the fields.
    ///
    /// - Parameters:
    ///   - schema: The fields to show, in order.
    ///   - data: The values by key, usually parsed from the model's structured output.
    ///   - title: The title under the provenance label, already localised.
    ///   - layout: One field per row, or field cards in two columns.
    ///   - isStreaming: Whether the answer is still arriving: missing fields show placeholders.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpenLink: Opens links of Markdown fields in the app instead of the environment's `openURL` action.
    ///   - actions: Views after the fields, such as a copy button or feedback buttons.
    public init(
        _ schema: [MonarchAiStructuredField],
        data: [String: MonarchAiJSON],
        title: String? = nil,
        layout: MonarchAiStructuredAnswerLayout = .list,
        isStreaming: Bool = false,
        labels: MonarchAiStructuredAnswerLabels = MonarchAiStructuredAnswerLabels(),
        onOpenLink: (@MainActor (URL) -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.schema = schema
        self.data = data
        self.title = title
        self.layout = layout
        self.isStreaming = isStreaming
        self.labels = labels
        self.onOpenLink = onOpenLink
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            header
            fields
            if Actions.self != EmptyView.self {
                MonarchFlowLayout(spacing: Tokens.actionSpacing) {
                    actions
                }
            }
        }
        .padding(Tokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: Tokens.Role.surface]))
        .overlay(shape.strokeBorder(theme.colors[role: Tokens.Role.border], lineWidth: Tokens.borderWidth))
        .accessibilityElement(children: .contain)
        .onChange(of: isStreaming) { wasStreaming, streaming in
            if wasStreaming && !streaming {
                AccessibilityNotification.Announcement(labels.complete ?? MonarchAiStructuredAnswerStrings.complete)
                    .post()
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: Tokens.headerSpacing) {
            MonarchAiGeneratedBadge(size: .sm, label: labels.aiAnswer ?? MonarchAiStructuredAnswerStrings.aiAnswer)
            if let title, !title.isEmpty {
                Text(title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors[role: Tokens.Role.title])
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private var fields: some View {
        switch layout {
        case .list:
            VStack(alignment: .leading, spacing: Tokens.fieldSpacing) {
                ForEach(schema) { field in
                    fieldView(field, isCard: false)
                }
            }
        case .grid:
            let columns = dynamicTypeSize.isAccessibilitySize ? 1 : Tokens.gridColumns
            Grid(alignment: .topLeading, horizontalSpacing: Tokens.fieldSpacing, verticalSpacing: Tokens.fieldSpacing) {
                ForEach(Tokens.gridRows(schema, columns: columns), id: \.self) { row in
                    GridRow {
                        ForEach(row, id: \.self) { index in
                            fieldView(schema[index], isCard: true)
                                .gridCellColumns(row.count == 1 && Tokens.isWide(schema[index]) ? columns : 1)
                        }
                    }
                }
            }
        }
    }

    private func fieldView(_ field: MonarchAiStructuredField, isCard: Bool) -> some View {
        MonarchAiStructuredAnswerField(
            field: field,
            value: data[field.key],
            isStreaming: isStreaming,
            isCard: isCard,
            pendingLabel: labels.pending ?? MonarchAiStructuredAnswerStrings.pending,
            emptyLabel: labels.empty ?? MonarchAiStructuredAnswerStrings.empty,
            onOpenLink: onOpenLink
        )
    }
}

extension MonarchAiStructuredAnswer where Actions == EmptyView {
    /// A structured answer without actions.
    ///
    /// - Parameters:
    ///   - schema: The fields to show, in order.
    ///   - data: The values by key, usually parsed from the model's structured output.
    ///   - title: The title under the provenance label, already localised.
    ///   - layout: One field per row, or field cards in two columns.
    ///   - isStreaming: Whether the answer is still arriving: missing fields show placeholders.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpenLink: Opens links of Markdown fields in the app instead of the environment's `openURL` action.
    public init(
        _ schema: [MonarchAiStructuredField],
        data: [String: MonarchAiJSON],
        title: String? = nil,
        layout: MonarchAiStructuredAnswerLayout = .list,
        isStreaming: Bool = false,
        labels: MonarchAiStructuredAnswerLabels = MonarchAiStructuredAnswerLabels(),
        onOpenLink: (@MainActor (URL) -> Void)? = nil
    ) {
        self.init(
            schema,
            data: data,
            title: title,
            layout: layout,
            isStreaming: isStreaming,
            labels: labels,
            onOpenLink: onOpenLink
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchAiStructuredAnswer") {
        MonarchPreviewMatrix {
            MonarchAiStructuredAnswer(
                [
                    MonarchAiStructuredField("candidate", label: "Candidate"),
                    MonarchAiStructuredField("stage", label: "Stage", type: .badge, tone: .info),
                    MonarchAiStructuredField("score", label: "Overall score", type: .number),
                    MonarchAiStructuredField("strengths", label: "Strengths", type: .list),
                    MonarchAiStructuredField("summary", label: "Summary", type: .markdown),
                ],
                data: [
                    "candidate": "Alex Morgan",
                    "stage": "Final interview",
                    "score": 4.2,
                    "strengths": ["Clear system design reasoning", "Strong written communication"],
                ],
                title: "Interview brief",
                isStreaming: true
            )
            MonarchAiStructuredAnswer(
                [
                    MonarchAiStructuredField("health", label: "Health", type: .badge, tone: .warning),
                    MonarchAiStructuredField("usage", label: "Seat usage", type: .number, format: .percent),
                    MonarchAiStructuredField("renewal", label: "Renewal"),
                ],
                data: ["health": "At risk", "usage": 0.62],
                title: "Account health",
                layout: .grid
            )
        }
    }
#endif
