import SwiftUI

/// The progress text and the health badge beside a roadmap row's name: muted text, then a tone badge with a
/// dot, as on the web.
struct MonarchRoadmapMeta: View {
    let progress: String?
    let health: MonarchRoadmapHealth?
    let text: MonarchRoadmapText

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        HStack(spacing: MonarchRoadmapTokens.metaSpacing) {
            if let progress {
                Text(progress)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let health {
                MonarchBadge(text.health(health), tone: MonarchRoadmapTokens.healthTone(health), size: .sm, dot: true)
            }
        }
        .accessibilityElement(children: .combine)
    }
}
