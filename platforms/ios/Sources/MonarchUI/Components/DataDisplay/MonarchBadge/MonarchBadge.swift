import SwiftUI

/// A short status label, such as "Paid" or "Draft".
///
/// ```swift
/// MonarchBadge("Paid", tone: .success)
/// MonarchBadge("Beta", tone: .info, variant: .outline)
/// ```
///
/// A badge is not interactive. Keep the text to one or two words.
///
/// Accessibility: the badge is read as its text, so the text must carry the
/// meaning without the colour. The icon is decorative. The text scales with
/// Dynamic Type.
public struct MonarchBadge: View {
    private let text: String
    private let tone: MonarchTone
    private let variant: MonarchBadgeVariant
    private let systemImage: String?
    private let size: MonarchBadgeSize
    private let showsDot: Bool

    /// A badge.
    ///
    /// - Parameters:
    ///   - text: The label, already localised.
    ///   - tone: The meaning, which selects the colours.
    ///   - variant: How strongly the badge is drawn.
    ///   - systemImage: An SF Symbol before the text.
    ///   - size: `.md` by default; `.sm` for dense rows and tables.
    ///   - dot: Draws a small status dot before the text, in the badge's text colour.
    public init(
        _ text: String,
        tone: MonarchTone = .neutral,
        variant: MonarchBadgeVariant = .subtle,
        systemImage: String? = nil,
        size: MonarchBadgeSize = .md,
        dot: Bool = false
    ) {
        self.text = text
        self.tone = tone
        self.variant = variant
        self.systemImage = systemImage
        self.size = size
        self.showsDot = dot
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchBadgeTokens.appearance(tone: tone, variant: variant)
        HStack(spacing: MonarchBadgeTokens.iconSpacing) {
            if showsDot {
                Circle()
                    .frame(width: MonarchBadgeTokens.dotSize, height: MonarchBadgeTokens.dotSize)
                    .accessibilityHidden(true)
            }
            if let systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
                    .imageScale(.small)
            }
            Text(text)
                .monarchTextStyle(MonarchBadgeTokens.textStyle, weight: MonarchBadgeTokens.fontWeight)
        }
        .padding(
            .horizontal,
            size == .sm ? MonarchBadgeTokens.smallHorizontalPadding : MonarchBadgeTokens.horizontalPadding
        )
        .padding(.vertical, size == .sm ? 0 : MonarchBadgeTokens.verticalPadding)
        .monarchSurface(appearance, in: Capsule())
        .accessibilityElement(children: .combine)
    }
}

#if DEBUG
    #Preview("MonarchBadge") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchBadgeVariant.allCases, id: \.self) { variant in
                    HStack {
                        ForEach(MonarchTone.allCases, id: \.self) { tone in
                            MonarchBadge("Label", tone: tone, variant: variant)
                        }
                    }
                }
            }
        }
    }
#endif
