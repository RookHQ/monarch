import SwiftUI

/// The "I agree to the Terms of Service and Privacy Policy" checkbox for
/// sign-up, with a link to each document and the versions that were accepted.
///
/// ```swift
/// MonarchTermsAcceptance(
///     isAccepted: $accepted,
///     documents: [
///         MonarchTermsDocument("Terms of Service", url: termsURL, version: "2026-11-01"),
///         MonarchTermsDocument("Privacy Policy", url: privacyURL, version: "2026-11-01"),
///     ],
///     showsRequiredError: triedToContinue
/// ) { accepted, details in
///     proof = accepted ? details : nil
/// }
/// ```
///
/// The box starts as the binding says; leave it `false` for sign-up so the
/// agreement is an active choice. The app decides when to show the error,
/// usually after the person presses Continue with the box unticked; the error
/// clears as soon as the box is ticked. Every change reports the document
/// versions and the time, so the app can keep proof of acceptance.
///
/// Accessibility: the box is a standard `Toggle` with the Monarch checkbox
/// style, labelled with the whole sentence, and has the hint "Required" when
/// required. Each document is a separate standard `Link`, underlined so it
/// does not depend on colour, with a 44-point touch target. The error has an
/// icon and text and is announced when it appears.
public struct MonarchTermsAcceptance: View {
    @Binding private var isAccepted: Bool
    private let documents: [MonarchTermsDocument]
    private let isRequired: Bool
    private let showsRequiredError: Bool
    private let labels: MonarchTermsAcceptanceLabels
    private let onChange: (@MainActor (Bool, MonarchTermsAcceptanceDetails) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale

    /// A terms acceptance checkbox.
    ///
    /// - Parameters:
    ///   - isAccepted: Whether the box is ticked. Start it `false` for sign-up.
    ///   - documents: The documents, linked under the label in the order given.
    ///   - isRequired: Whether the box must be ticked to continue. Adds the hint "Required".
    ///   - showsRequiredError: Shows "Accept the terms to continue" while the box is required and not
    ///     ticked. Set it when the person tries to continue.
    ///   - labels: Replacements for the built-in strings.
    ///   - onChange: Runs when the box changes, with the new state, the document versions and the time.
    public init(
        isAccepted: Binding<Bool>,
        documents: [MonarchTermsDocument],
        isRequired: Bool = true,
        showsRequiredError: Bool = false,
        labels: MonarchTermsAcceptanceLabels = MonarchTermsAcceptanceLabels(),
        onChange: (@MainActor (Bool, MonarchTermsAcceptanceDetails) -> Void)? = nil
    ) {
        _isAccepted = isAccepted
        self.documents = documents
        self.isRequired = isRequired
        self.showsRequiredError = showsRequiredError
        self.labels = labels
        self.onChange = onChange
    }

    private var showsError: Bool {
        MonarchTermsAcceptanceTokens.showsError(
            isRequired: isRequired,
            isAccepted: isAccepted,
            showsRequiredError: showsRequiredError
        )
    }

    private var requiredMessage: String {
        labels.requiredMessage ?? MonarchTermsAcceptanceStrings.requiredMessage
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchTermsAcceptanceTokens.stackSpacing) {
            if let sentence = inlineSentence {
                HStack(spacing: 0) {
                    Toggle(isOn: Binding(get: { isAccepted }, set: { change(to: $0) })) { EmptyView() }
                        .toggleStyle(.monarchCheckbox)
                        .accessibilityLabel(labelText)
                        .accessibilityHint(
                            isRequired ? (labels.requiredHint ?? MonarchTermsAcceptanceStrings.requiredHint) : ""
                        )
                        .modifier(MonarchTermsDocumentActions(documents: documents))
                    Text(sentence)
                        .monarchTextStyle(.label)
                        .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.leading, MonarchTermsAcceptanceTokens.indent - MonarchAccessibility.minimumHitTarget)
                        .contentShape(Rectangle())
                        .onTapGesture { if isEnabled { change(to: !isAccepted) } }
                        .accessibilityHidden(true)
                }
            } else {
                Toggle(labelText, isOn: Binding(get: { isAccepted }, set: { change(to: $0) }))
                    .toggleStyle(.monarchCheckbox)
                    .accessibilityHint(
                        isRequired ? (labels.requiredHint ?? MonarchTermsAcceptanceStrings.requiredHint) : ""
                    )
                if !documents.isEmpty {
                    MonarchFlowLayout(spacing: MonarchTermsAcceptanceTokens.linkSpacing, rowSpacing: 0) {
                        ForEach(documents) { document in
                            Link(destination: document.url) {
                                Text(document.title)
                                    .monarchTextStyle(.label)
                                    .underline()
                                    .foregroundStyle(
                                        theme.colors[role: MonarchTermsAcceptanceTokens.linkRole(isEnabled: isEnabled)]
                                    )
                                    .multilineTextAlignment(.leading)
                                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                                    .contentShape(Rectangle())
                            }
                        }
                    }
                    .padding(.leading, MonarchTermsAcceptanceTokens.indent)
                }
            }
            if showsError {
                HStack(alignment: .firstTextBaseline, spacing: MonarchTermsAcceptanceTokens.iconSpacing) {
                    MonarchIcon(systemName: MonarchTermsAcceptanceTokens.errorSymbol, size: .sm)
                    Text(requiredMessage).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors[role: MonarchTermsAcceptanceTokens.errorRole])
                .padding(.leading, MonarchTermsAcceptanceTokens.indent)
                .accessibilityElement(children: .combine)
            }
        }
        .onChange(of: showsError) { _, shows in
            if shows { AccessibilityNotification.Announcement(requiredMessage).post() }
        }
    }

    private var labelText: String {
        MonarchTermsAcceptanceTokens.label(documents, locale: locale, builder: labels.label)
    }

    /// The sentence with each document title as a link inside it, as on the web; `nil` when a title is not in
    /// the sentence (a custom label), where the links go under the box instead.
    private var inlineSentence: AttributedString? {
        guard !documents.isEmpty else { return nil }
        var sentence = AttributedString(labelText)
        let linkColor = theme.colors[role: MonarchTermsAcceptanceTokens.linkRole(isEnabled: isEnabled)]
        for document in documents {
            guard let range = sentence.range(of: document.title) else { return nil }
            sentence[range].link = document.url
            sentence[range].underlineStyle = .single
            sentence[range].foregroundColor = linkColor
        }
        return sentence
    }

    private func change(to accepted: Bool) {
        isAccepted = accepted
        onChange?(
            accepted,
            MonarchTermsAcceptanceDetails(versions: MonarchTermsDocument.versions(of: documents), date: Date())
        )
    }
}

/// One VoiceOver action per document, because the visible links sit inside the sentence, which is hidden from VoiceOver.
private struct MonarchTermsDocumentActions: ViewModifier {
    let documents: [MonarchTermsDocument]
    @Environment(\.openURL) private var openURL

    func body(content: Content) -> some View {
        documents.reduce(AnyView(content)) { view, document in
            AnyView(
                view.accessibilityAction(named: Text(document.title)) { openURL(document.url) }
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchTermsAcceptance") {
        let documents = [
            MonarchTermsDocument("Terms of Service", url: URL(filePath: "/terms"), version: "2026-11-01"),
            MonarchTermsDocument("Privacy Policy", url: URL(filePath: "/privacy"), version: "2026-11-01"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTermsAcceptance(isAccepted: .constant(false), documents: documents)
                MonarchTermsAcceptance(isAccepted: .constant(false), documents: documents, showsRequiredError: true)
                MonarchTermsAcceptance(isAccepted: .constant(true), documents: documents)
                MonarchTermsAcceptance(isAccepted: .constant(false), documents: documents).disabled(true)
            }
        }
    }
#endif
