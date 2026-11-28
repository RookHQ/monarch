import MonarchTokens
import SwiftUI

/// A highlighted block inside a document: a tone, an icon and any content.
///
/// ```swift
/// MonarchCalloutBlock(tone: .warning) {
///     Text("Back up the workspace before you migrate.")
///     Text("The migration cannot be undone.")
/// }
/// ```
///
/// This is the display form of the callout block of a document. For a
/// status message with a title and an action, use ``MonarchBanner``.
///
/// Accessibility: the icon is read as the name of the tone before the
/// content, so the tone does not depend on colour; the neutral tone has no
/// name and its icon is hidden. The content keeps its own elements, so links
/// and buttons inside it stay reachable. Text in the content takes the body
/// type role and scales with Dynamic Type.
public struct MonarchCalloutBlock<Content: View>: View {
    private let systemImage: String?
    private let tone: MonarchTone
    private let toneLabel: String?
    private let content: Content

    @Environment(\.monarchTheme) private var theme

    /// A callout block.
    ///
    /// - Parameters:
    ///   - systemImage: Replaces the SF Symbol of the tone.
    ///   - tone: The meaning, which selects the colours and the icon.
    ///   - toneLabel: Replaces the built-in spoken name of the tone.
    ///   - content: The content, laid out in a leading-aligned vertical stack.
    public init(
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        toneLabel: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.systemImage = systemImage
        self.tone = tone
        self.toneLabel = toneLabel
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchCalloutBlockTokens.appearance(tone: tone)
        HStack(alignment: .firstTextBaseline, spacing: MonarchCalloutBlockTokens.iconSpacing) {
            icon.foregroundStyle(theme.colors[role: appearance.icon])
            VStack(alignment: .leading, spacing: MonarchCalloutBlockTokens.contentSpacing) {
                content
            }
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors[role: appearance.text])
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, MonarchCalloutBlockTokens.horizontalPadding)
        .padding(.vertical, MonarchCalloutBlockTokens.verticalPadding)
        .background(MonarchShapes.container.fill(theme.colors[role: appearance.background]))
        .overlay(
            MonarchShapes.container
                .strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchCalloutBlockTokens.borderWidth)
                .allowsHitTesting(false)
        )
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder private var icon: some View {
        let symbol = MonarchCalloutBlockTokens.symbol(tone: tone, override: systemImage)
        let size = MonarchCalloutBlockTokens.iconSize(tone: tone, override: systemImage)
        if symbol == MonarchCalloutBlockTokens.neutralSymbol {
            // The neutral marker is a small dot, as on the web, not a symbol-sized disc.
            Circle()
                .frame(width: MonarchTokens.Space.space2, height: MonarchTokens.Space.space2)
                .padding(MonarchTokens.Space.space1)
                .accessibilityHidden(true)
        } else if let spoken = toneLabel ?? MonarchToneStrings.name(tone) {
            MonarchIcon(systemName: symbol, label: spoken, size: size)
        } else {
            MonarchIcon(systemName: symbol, size: size)
        }
    }
}

extension MonarchCalloutBlock where Content == Text {
    /// A callout block with one paragraph of text.
    ///
    /// - Parameters:
    ///   - text: The text, already localised.
    ///   - systemImage: Replaces the SF Symbol of the tone.
    ///   - tone: The meaning, which selects the colours and the icon.
    ///   - toneLabel: Replaces the built-in spoken name of the tone.
    public init(
        _ text: String,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        toneLabel: String? = nil
    ) {
        self.init(systemImage: systemImage, tone: tone, toneLabel: toneLabel) {
            Text(text)
        }
    }
}

#if DEBUG
    #Preview("MonarchCalloutBlock") {
        MonarchPreviewMatrix {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchCalloutBlock("A paragraph inside a callout block.", tone: tone)
                }
                MonarchCalloutBlock(systemImage: "lightbulb", tone: .info) {
                    Text("Tip")
                        .monarchTextStyle(.bodyStrong)
                    Text("Callouts can hold more than one paragraph.")
                }
            }
        }
    }
#endif
