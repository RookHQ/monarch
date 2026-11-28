import SwiftUI

/// Adds "Ask AI" to the context menu of any content, such as a paragraph
/// card or a note row, to open a ``MonarchAiInlinePrompt``.
///
/// ```swift
/// MonarchAiInlinePromptArea(onAskAi: { isAsking = true }, content: {
///     Text(paragraph).monarchTextStyle(.body)
/// })
/// .sheet(isPresented: $isAsking) {
///     MonarchAiInlinePrompt(context: paragraph) { prompt in rewrite(paragraph, with: prompt) }
/// }
/// ```
///
/// SwiftUI gives no access to the text selection of `Text` with
/// `.textSelection(.enabled)` and no way to add items to its menu, so this
/// wrapper uses the system context menu (a long press) for whole content.
/// For an item in the text selection menu, with the selected text, use
/// ``MonarchAiInlinePromptTextView``. Do not combine this with
/// `.textSelection(.enabled)` on the same text: both use the long press.
///
/// Accessibility: the context menu is reached with the VoiceOver actions
/// rotor, and "Ask AI" is also a named accessibility action of the content.
public struct MonarchAiInlinePromptArea<Content: View>: View {
    private let isEnabled: Bool
    private let label: String?
    private let onAskAi: @MainActor () -> Void
    private let content: Content

    /// Content with "Ask AI" in its context menu.
    ///
    /// - Parameters:
    ///   - isEnabled: Off, the menu item and the action are not added.
    ///   - label: The menu item's title. Built in: "Ask AI".
    ///   - onAskAi: Runs when "Ask AI" is chosen. Present the ``MonarchAiInlinePrompt`` sheet.
    ///   - content: The content the prompt is about.
    public init(
        isEnabled: Bool = true,
        label: String? = nil,
        onAskAi: @escaping @MainActor () -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.isEnabled = isEnabled
        self.label = label
        self.onAskAi = onAskAi
        self.content = content()
    }

    private var title: String { label ?? MonarchAiInlinePromptStrings.label }

    /// The content of the view.
    public var body: some View {
        if isEnabled {
            content
                .contextMenu {
                    Button(title, systemImage: MonarchAiInlinePromptTokens.askSymbol) { onAskAi() }
                }
                .accessibilityAction(named: title) { onAskAi() }
        } else {
            content
        }
    }
}

#if DEBUG
    #Preview("MonarchAiInlinePromptArea") {
        MonarchPreviewMatrix {
            MonarchAiInlinePromptArea(
                onAskAi: {},
                content: {
                    Text("Early numbers suggest more people finish setup on their first visit.")
                        .monarchTextStyle(.body)
                }
            )
            MonarchAiInlinePromptArea(
                isEnabled: false,
                onAskAi: {},
                content: {
                    Text("Read only").monarchTextStyle(.body)
                }
            )
        }
    }
#endif
