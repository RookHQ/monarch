import SwiftUI

/// The AI identity mark: a robot head. Use it for custom AI controls.
///
/// Monarch never uses a sparkle for AI. SF Symbols has no robot, so the
/// glyph is drawn, with the same outline as on the web and on Android.
///
/// ```swift
/// MonarchAiBotGlyph()                          // decorative
/// MonarchAiBotGlyph(label: "Assistant")        // read by VoiceOver
/// ```
///
/// Like ``MonarchIcon``, it takes the foreground style of its container,
/// scales with Dynamic Type, and is hidden from VoiceOver unless it has a
/// label. The glyph is symmetric, so it is the same in right-to-left layouts.
public struct MonarchAiBotGlyph: View {
    private let label: String?
    @ScaledMetric private var pointSize: CGFloat

    /// A decorative glyph, hidden from assistive technology.
    public init(size: MonarchIconSize = .md) {
        self.label = nil
        _pointSize = ScaledMetric(wrappedValue: MonarchIconTokens.pointSize(size), relativeTo: .body)
    }

    /// A glyph that carries meaning. `label` is read by VoiceOver.
    public init(label: String, size: MonarchIconSize = .md) {
        self.label = label
        _pointSize = ScaledMetric(wrappedValue: MonarchIconTokens.pointSize(size), relativeTo: .body)
    }

    /// A decorative glyph at a component's own size, in points before Dynamic Type.
    init(pointSize: CGFloat) {
        self.label = nil
        _pointSize = ScaledMetric(wrappedValue: pointSize, relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        let glyph = MonarchAiBotGlyphShape()
            .stroke(
                style: StrokeStyle(
                    lineWidth: pointSize * MonarchAiBotGlyphTokens.strokeRatio,
                    lineCap: .round,
                    lineJoin: .round
                )
            )
            .frame(width: pointSize, height: pointSize)
        if let label {
            glyph
                .accessibilityElement()
                .accessibilityLabel(label)
                .accessibilityAddTraits(.isImage)
        } else {
            glyph.accessibilityHidden(true)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiBotGlyph") {
        MonarchPreviewMatrix {
            HStack {
                MonarchAiBotGlyph(size: .sm)
                MonarchAiBotGlyph(size: .md)
                MonarchAiBotGlyph(label: "Assistant", size: .lg)
            }
        }
    }
#endif
