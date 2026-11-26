import SwiftUI

/// The details of one span, shown in a sheet: its facts, its error, its
/// attributes and its plain-text input and output.
struct MonarchAiTraceViewDetail: View {
    let span: MonarchAiTraceSpan
    let currency: String
    let labels: MonarchAiTraceViewLabels
    let onOpenPart: (@MainActor (MonarchAiTraceSpan) -> Void)?
    let onClose: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchAiTraceViewTokens

    var body: some View {
        NavigationStack {
            ScrollView {
                content
                    .padding(Tokens.spacing)
            }
            .navigationTitle(span.name)
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.closeDetails ?? MonarchAiTraceViewStrings.closeDetails) { onClose() }
                }
            }
        }
    }

    var content: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            let facts = Tokens.detailFacts(span, currency: currency, locale: locale, labels: labels)
            ForEach(Array(facts.enumerated()), id: \.offset) { _, fact in
                LabeledContent(fact.name, value: fact.value)
            }
            if let error = span.error, !error.isEmpty {
                LabeledContent(labels.error ?? MonarchAiTraceViewStrings.error) {
                    Text(error).foregroundStyle(theme.colors[role: Tokens.errorText])
                }
            }
            let attributes = Tokens.attributes(span)
            if !attributes.isEmpty {
                Text(labels.attributes ?? MonarchAiTraceViewStrings.attributes)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                ForEach(Array(attributes.enumerated()), id: \.offset) { _, attribute in
                    LabeledContent(attribute.name, value: attribute.value)
                }
            }
            if let input = span.input, !input.isEmpty {
                MonarchAiCodeBlock(input, title: labels.input ?? MonarchAiTraceViewStrings.input, isHighlighted: false)
            }
            if let output = span.output, !output.isEmpty {
                MonarchAiCodeBlock(
                    output,
                    title: labels.output ?? MonarchAiTraceViewStrings.output,
                    isHighlighted: false
                )
            }
            if Tokens.canOpenPart(span, hasHandler: onOpenPart != nil), let onOpenPart {
                MonarchButton(labels.openPart ?? MonarchAiTraceViewStrings.openPart, systemImage: Tokens.openPartSymbol)
                {
                    onOpenPart(span)
                }
            }
        }
        .labeledContentStyle(.monarch)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
