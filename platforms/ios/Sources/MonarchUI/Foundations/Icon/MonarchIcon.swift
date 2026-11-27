import SwiftUI

/// An SF Symbol at a Monarch size.
///
/// An icon is decorative unless it is given a label. A decorative icon is
/// hidden from VoiceOver; a labelled icon is read with its label. The icon
/// takes the foreground style of its container and scales with Dynamic Type.
///
/// ```swift
/// MonarchIcon(systemName: "tray")                        // decorative
/// MonarchIcon(systemName: "tray", label: "Inbox")        // read by VoiceOver
/// ```
public struct MonarchIcon: View {
    private let systemName: String
    private let label: String?

    @ScaledMetric private var pointSize: CGFloat

    /// A decorative icon, hidden from assistive technology.
    public init(systemName: String, size: MonarchIconSize = .md) {
        self.systemName = systemName
        self.label = nil
        _pointSize = ScaledMetric(wrappedValue: MonarchIconTokens.pointSize(size), relativeTo: .body)
    }

    /// An icon that carries meaning. `label` is read by VoiceOver.
    public init(systemName: String, label: String, size: MonarchIconSize = .md) {
        self.systemName = systemName
        self.label = label
        _pointSize = ScaledMetric(wrappedValue: MonarchIconTokens.pointSize(size), relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        if systemName == "cpu" {
            // The AI mark is the drawn robot head, as on the web; SF Symbols has no robot.
            let bot = MonarchAiBotGlyph(pointSize: pointSize)
            if let label {
                bot.accessibilityElement().accessibilityLabel(label)
            } else {
                bot
            }
        } else {
            let image = Image(systemName: systemName).font(.system(size: pointSize))
            if let label {
                image.accessibilityLabel(label)
            } else {
                image.accessibilityHidden(true)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchIcon") {
        MonarchPreviewMatrix {
            HStack {
                MonarchIcon(systemName: "tray", size: .sm)
                MonarchIcon(systemName: "tray", size: .md)
                MonarchIcon(systemName: "tray", label: "Inbox", size: .lg)
            }
        }
    }
#endif
