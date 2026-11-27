import SwiftUI

/// The row of controls under an assistant prompt: web search, templates,
/// mode, model, tone. Put standard `Button`s, `Toggle`s and pickers in it.
///
/// ```swift
/// MonarchAiPromptToolbar {
///     Toggle("Search the web", systemImage: "globe", isOn: $searchesWeb)
///     Button("Templates", systemImage: "list.bullet") { showTemplates() }
///     MonarchAiModeSwitcher(selection: $mode, showsDescription: false)
/// }
/// ```
///
/// The toolbar gives every `Button` inside it the small ghost Monarch button
/// style and every `Toggle` a compact toggle-button style that takes the
/// assistant fill and border when on. Apply your own style to a control to
/// replace it. For an icon-only control apply `.labelStyle(.iconOnly)`; the
/// label stays its spoken name. The row wraps onto more lines when the
/// controls do not fit.
///
/// Accessibility: a container named by `label` ("Prompt options"), in
/// reading order. A toggle is read as a switch with its on or off value,
/// and its label never changes with its state. Every control is at least
/// 44 points in both directions. When the person asks for differentiation
/// without colour, a toggle that is on also shows a check mark.
public struct MonarchAiPromptToolbar<Content: View>: View {
    private let label: String?
    private let content: Content

    /// A prompt toolbar.
    ///
    /// - Parameters:
    ///   - label: Replaces the built-in name of the group, "Prompt options".
    ///   - content: The buttons, toggles and pickers, in order.
    public init(label: String? = nil, @ViewBuilder content: () -> Content) {
        self.label = label
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        MonarchFlowLayout(spacing: MonarchAiPromptToolbarTokens.spacing) {
            content
        }
        .buttonStyle(
            .monarch(MonarchAiPromptToolbarTokens.buttonVariant, size: MonarchAiPromptToolbarTokens.controlSize)
        )
        .toggleStyle(MonarchAiPromptToolbarToggleStyle())
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label ?? MonarchAiPromptToolbarStrings.label)
    }
}

#if DEBUG
    #Preview("MonarchAiPromptToolbar") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiPromptToolbar {
                    Toggle("Search the web", systemImage: "globe", isOn: .constant(true))
                    Toggle("Deep research", systemImage: "books.vertical", isOn: .constant(false))
                    Button("Templates", systemImage: "list.bullet") {}
                    Button("Attach", systemImage: "paperclip") {}
                        .labelStyle(.iconOnly)
                }
                MonarchAiPromptToolbar {
                    Toggle("Search the web", isOn: .constant(true))
                    Button("Templates") {}
                }
                .disabled(true)
            }
        }
    }
#endif
