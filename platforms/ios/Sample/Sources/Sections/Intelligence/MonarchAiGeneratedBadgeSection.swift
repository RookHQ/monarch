import MonarchUI
import SwiftUI

struct MonarchAiGeneratedBadgeSection: CataloguePage {
    static let title = "AI generated badge"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ForEach(MonarchAiGeneratedBadgeKind.allCases, id: \.self) { kind in
            CatalogueCaption(String(describing: kind))
            HStack(spacing: theme.spacing.space2) {
                MonarchAiGeneratedBadge(kind)
                MonarchAiGeneratedBadge(kind, size: .sm)
                MonarchAiGeneratedBadge(kind, isIconOnly: true)
                MonarchAiGeneratedBadge(kind, size: .sm, isIconOnly: true)
            }
        }
        CatalogueCaption("With the model and time (read by VoiceOver, shown to a pointer)")
        MonarchAiGeneratedBadge(model: "gemini-2.5-pro", date: Date())

        CatalogueCaption("The AI glyph")
        HStack(spacing: theme.spacing.space3) {
            ForEach(MonarchIconSize.allCases, id: \.self) { size in
                MonarchAiBotGlyph(size: size)
            }
        }
        .foregroundStyle(theme.colors.aiStrong)
    }
}
