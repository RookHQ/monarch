import SwiftUI

/// An opt-in to marketing messages for sign-up and contact forms.
///
/// With one channel it is one checkbox ("Send me product news by email");
/// with several, a question and one checkbox for each channel. Consent must
/// be an active choice: start `selection` empty, and bind a stored choice
/// only on a settings screen that shows what the person chose earlier.
/// Every change reports the exact wording shown, so the app can keep proof
/// of consent.
///
/// ```swift
/// MonarchMarketingConsentCheckbox(selection: $channels, description: "About twice a month.") { consent in
///     record(consent)
/// }
/// ```
///
/// Accessibility: each checkbox is a standard `Toggle` with the Monarch
/// checkbox style, so the role, the value and the gesture are the system's
/// and the whole row is the touch target. With several channels the question
/// is a header. The notes are text and the policy link is a standard `Link` in the
/// ``MonarchLinkStyle`` (underlined, so it does not depend on colour). Text scales with Dynamic Type.
public struct MonarchMarketingConsentCheckbox: View {
    private let label: String?
    @Binding private var selection: Set<MonarchMarketingChannel>
    private let channels: [MonarchMarketingChannel]
    private let description: String?
    private let policyTitle: String?
    private let policyURL: URL?
    private let showsUnsubscribeNote: Bool
    private let unsubscribeNote: String?
    private let channelLabels: [MonarchMarketingChannel: String]
    private let onChange: (@MainActor (MonarchMarketingConsent) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A marketing consent control.
    ///
    /// - Parameters:
    ///   - label: The question, already localised. Built in: "Send me product news by email" for one
    ///     channel, "Send me product news" for several.
    ///   - selection: The channels the person agreed to. Start it empty.
    ///   - channels: The channels to ask about. One checkbox each when there are several.
    ///   - description: What is sent and how often, for example "Product updates, about twice a month."
    ///   - policyTitle: The title of a link to the privacy policy. Shown only with `policyURL`.
    ///   - policyURL: Where the privacy policy is.
    ///   - showsUnsubscribeNote: Whether the note that consent can be withdrawn is shown.
    ///   - unsubscribeNote: Replaces the built-in "You can unsubscribe at any time."
    ///   - channelLabels: Replaces the built-in labels of the channel checkboxes.
    ///   - onChange: Runs on every change with the channels, the wording shown and the time.
    public init(
        _ label: String? = nil,
        selection: Binding<Set<MonarchMarketingChannel>>,
        channels: [MonarchMarketingChannel] = [.email],
        description: String? = nil,
        policyTitle: String? = nil,
        policyURL: URL? = nil,
        showsUnsubscribeNote: Bool = true,
        unsubscribeNote: String? = nil,
        channelLabels: [MonarchMarketingChannel: String] = [:],
        onChange: (@MainActor (MonarchMarketingConsent) -> Void)? = nil
    ) {
        self.label = label
        _selection = selection
        self.channels = channels
        self.description = description
        self.policyTitle = policyTitle
        self.policyURL = policyURL
        self.showsUnsubscribeNote = showsUnsubscribeNote
        self.unsubscribeNote = unsubscribeNote
        self.channelLabels = channelLabels
        self.onChange = onChange
    }

    private var shown: [MonarchMarketingChannel] {
        MonarchMarketingConsentCheckboxTokens.shownChannels(channels)
    }

    private var question: String {
        MonarchMarketingConsentCheckboxTokens.question(label: label, channels: channels)
    }

    private var note: String? {
        MonarchMarketingConsentCheckboxTokens.unsubscribeNote(shows: showsUnsubscribeNote, override: unsubscribeNote)
    }

    private var shownPolicyTitle: String? {
        guard policyURL != nil, let policyTitle, !policyTitle.isEmpty else { return nil }
        return policyTitle
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchMarketingConsentCheckboxTokens.stackSpacing) {
            if shown.count == 1, let only = shown.first {
                checkbox(only, title: question)
                // The notes sit under the label text, as on the web.
                notes.padding(.leading, MonarchCheckboxTokens.boxSize + MonarchCheckboxTokens.labelSpacing)
            } else {
                Text(question)
                    .monarchTextStyle(.label)
                    .foregroundStyle(
                        theme.colors[role: MonarchMarketingConsentCheckboxTokens.question(isEnabled: isEnabled)]
                    )
                    .accessibilityAddTraits(.isHeader)
                notes
                ForEach(shown, id: \.self) { channel in
                    checkbox(
                        channel,
                        title: MonarchMarketingConsentCheckboxTokens.channelLabel(channel, labels: channelLabels)
                    )
                }
            }
        }
    }

    private func checkbox(_ channel: MonarchMarketingChannel, title: String) -> some View {
        Toggle(
            title,
            isOn: Binding(
                get: { selection.contains(channel) },
                set: { set(channel, to: $0) }
            )
        )
        .toggleStyle(.monarchCheckbox)
    }

    @ViewBuilder private var notes: some View {
        let lines = [description, note].compactMap { $0 }.filter { !$0.isEmpty }
        if !lines.isEmpty || shownPolicyTitle != nil {
            // The policy link ends the sentence, as on the web.
            Text(notesText(lines.joined(separator: " ")))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchMarketingConsentCheckboxTokens.notes(isEnabled: isEnabled)])
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func notesText(_ text: String) -> AttributedString {
        var result = AttributedString(text)
        if let shownPolicyTitle, let policyURL {
            if !text.isEmpty { result.append(AttributedString(" ")) }
            var link = AttributedString(shownPolicyTitle)
            link.link = policyURL
            link.underlineStyle = .single
            let role: MonarchColorRole = isEnabled ? \.textLink : \.textDisabled
            link.swiftUI.foregroundColor = theme.colors[role: role]
            result.append(link)
        }
        return result
    }

    private func set(_ channel: MonarchMarketingChannel, to isOn: Bool) {
        let next = MonarchMarketingConsentCheckboxTokens.selection(selection, setting: channel, to: isOn)
        selection = next
        guard let onChange else { return }
        let labels =
            shown.count > 1
            ? shown.map { MonarchMarketingConsentCheckboxTokens.channelLabel($0, labels: channelLabels) } : []
        let text = MonarchMarketingConsentCheckboxTokens.wording(
            question: question,
            channelLabels: labels,
            description: description,
            unsubscribeNote: note,
            policyTitle: shownPolicyTitle
        )
        onChange(MonarchMarketingConsentCheckboxTokens.consent(shown: shown, selection: next, text: text, date: Date()))
    }
}

#if DEBUG
    #Preview("MonarchMarketingConsentCheckbox") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchMarketingConsentCheckbox(selection: .constant([]), description: "About twice a month.")
                MonarchMarketingConsentCheckbox(
                    selection: .constant([.email]),
                    channels: MonarchMarketingChannel.allCases,
                    policyTitle: "Privacy policy",
                    policyURL: URL(string: "https://example.com/privacy")
                )
                MonarchMarketingConsentCheckbox(selection: .constant([])).disabled(true)
            }
        }
    }
#endif
