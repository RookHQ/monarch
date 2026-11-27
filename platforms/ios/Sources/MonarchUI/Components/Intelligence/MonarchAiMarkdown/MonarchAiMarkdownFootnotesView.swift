import SwiftUI

/// The footnotes at the end of ``MonarchAiMarkdown``: a rule, a "Footnotes"
/// heading and each referenced footnote with its number, in the order of
/// first reference.
struct MonarchAiMarkdownFootnotesView: View {
    let footnotes: MonarchAiMarkdownFootnotes
    let context: MonarchAiMarkdownContext

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiMarkdownTokens

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.tightGap) {
            Rectangle()
                .fill(theme.colors[role: Tokens.Role.rule])
                .frame(maxWidth: .infinity)
                .frame(height: Tokens.borderWidth)
                .padding(.bottom, Tokens.blockGap - Tokens.tightGap)
                // The web has no visible "Footnotes" heading; the rule carries it for VoiceOver.
                .accessibilityLabel(context.labels.footnotes)
                .accessibilityAddTraits(.isHeader)
            ForEach(footnotes.ordered, id: \.number) { footnote in
                HStack(alignment: .firstTextBaseline, spacing: Tokens.markerGap) {
                    Text(Tokens.number(start: footnote.number, index: 0))
                        .monarchTextStyle(.body)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors[role: Tokens.Role.marker])
                        .frame(minWidth: Tokens.markerWidth, alignment: .leading)
                    MonarchAiMarkdownBlocks(
                        blocks: footnote.children,
                        context: context,
                        depth: 0,
                        isTight: true,
                        isMuted: false
                    )
                    // The way back to where the note is cited, as the web draws it after each note.
                    MonarchIcon(systemName: "arrow.turn.up.left", size: .sm)
                        .foregroundStyle(theme.colors.textLink)
                        .accessibilityHidden(true)
                }
            }
        }
        .padding(.top, Tokens.headingGap)
    }
}
