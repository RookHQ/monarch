import SwiftUI

/// Says, calmly and specifically, that a safety system blocked a request,
/// changed an answer or removed parts of it, with an optional reason, next
/// steps, an "Edit prompt" button and a link to the content guidelines.
///
/// ```swift
/// MonarchAiSafetyNotice(
///     .blocked,
///     reason: "The request asked for someone’s home address.",
///     nextSteps: "Ask about public information instead.",
///     learnMoreURL: guidelinesURL
/// ) {
///     composer.restoreLastPrompt()
/// }
/// ```
///
/// It is drawn like ``MonarchBanner``: `blocked` takes the warning tone
/// because no answer was given; `modified` and `filtered` take the info tone
/// because an answer is still shown. Each kind also has its own symbol (a
/// lock, a pencil, a filter) and its own title, so the meaning never depends
/// on colour. It never blames the person.
///
/// Accessibility: the title, the explanation, the reason (after its visible
/// "Reason:" label) and the next steps are one element read in that order;
/// the symbol is decorative. "Edit prompt" and the guidelines link are
/// separate elements with 44-point touch targets. The notice does not
/// announce itself; place it where the answer would have been.
public struct MonarchAiSafetyNotice: View {
    private let kind: MonarchAiSafetyNoticeKind
    private let title: String?
    private let message: String?
    private let reason: String?
    private let nextSteps: String?
    private let learnMoreURL: URL?
    private let labels: MonarchAiSafetyNoticeLabels
    private let onEditPrompt: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    /// A safety notice.
    ///
    /// - Parameters:
    ///   - kind: What the safety system did. Sets the tone, the symbol and the default wording.
    ///   - title: Replaces the title of the kind for this notice. Already localised.
    ///   - message: Replaces the explanation of the kind for this notice. Already localised.
    ///   - reason: Why, in plain words, when the product knows. Shown after "Reason:".
    ///   - nextSteps: What the person can do next, already localised.
    ///   - learnMoreURL: The content guidelines. Only `http`, `https` and `mailto` links are shown.
    ///   - labels: Replaces the built-in strings.
    ///   - onEditPrompt: Adds an "Edit prompt" button, usually to put the prompt back in the composer.
    public init(
        _ kind: MonarchAiSafetyNoticeKind,
        title: String? = nil,
        message: String? = nil,
        reason: String? = nil,
        nextSteps: String? = nil,
        learnMoreURL: URL? = nil,
        labels: MonarchAiSafetyNoticeLabels = MonarchAiSafetyNoticeLabels(),
        onEditPrompt: (@MainActor () -> Void)? = nil
    ) {
        self.kind = kind
        self.title = title
        self.message = message
        self.reason = reason
        self.nextSteps = nextSteps
        self.learnMoreURL = learnMoreURL
        self.labels = labels
        self.onEditPrompt = onEditPrompt
    }

    private typealias Tokens = MonarchAiSafetyNoticeTokens

    /// The content of the view.
    public var body: some View {
        let appearance = Tokens.appearance(kind)
        let shownTitle = title ?? labels.title(kind)
        let shownMessage = message ?? labels.message(kind)
        let reasonText = Tokens.nonEmpty(reason)
        let nextText = Tokens.nonEmpty(nextSteps)
        let url = Tokens.safeURL(learnMoreURL)
        let shape = RoundedRectangle(cornerRadius: MonarchBannerTokens.cornerRadius, style: .continuous)
        VStack(alignment: .leading, spacing: Tokens.actionSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchBannerTokens.iconSpacing) {
                MonarchIcon(systemName: Tokens.symbol(kind))
                    .foregroundStyle(theme.colors[role: appearance.icon])
                VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                    Text(shownTitle).monarchTextStyle(.bodyStrong)
                    Text(shownMessage).monarchTextStyle(.body)
                    if let reasonText {
                        (Text(labels.reason).bold() + Text(" " + reasonText))
                            .monarchTextStyle(.body)
                    }
                    if let nextText {
                        Text(nextText).monarchTextStyle(.body)
                    }
                }
                .foregroundStyle(theme.colors.textDefault)
                .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                Tokens.accessibilityLabel(
                    title: shownTitle,
                    message: shownMessage,
                    reason: reasonText,
                    nextSteps: nextText,
                    reasonLabel: labels.reason
                )
            )
            if onEditPrompt != nil || url != nil {
                actions(url: url)
            }
        }
        .padding(.horizontal, MonarchBannerTokens.horizontalPadding)
        .padding(.vertical, MonarchBannerTokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors.bgSurface))
        .overlay(
            shape.strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchBannerTokens.borderWidth)
        )
        .accessibilityElement(children: .contain)
    }

    private func actions(url: URL?) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: Tokens.actionGap) { actionViews(url: url) }
            VStack(alignment: .leading, spacing: Tokens.textSpacing) { actionViews(url: url) }
        }
    }

    @ViewBuilder private func actionViews(url: URL?) -> some View {
        if let onEditPrompt {
            MonarchButton(labels.editPrompt, systemImage: Tokens.editSymbol, size: .sm, action: onEditPrompt)
        }
        if let url {
            Link(labels.learnMore, destination: url)
                .buttonStyle(.monarchLink(.standalone, isExternal: true))
                .monarchTextStyle(.label)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiSafetyNotice") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiSafetyNotice(
                    .blocked,
                    reason: "The request asked for someone’s home address.",
                    nextSteps: "Ask about public information instead, such as a company’s office location.",
                    learnMoreURL: URL(string: "https://example.com/guidelines")
                ) {}
                MonarchAiSafetyNotice(.modified, reason: "Medical dosage details were removed.")
                MonarchAiSafetyNotice(.filtered)
            }
        }
    }
#endif
