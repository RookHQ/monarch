import MonarchUI
import SwiftUI

/// Native adoption: the standard `ScrollView` with a bottom anchor.
struct AiScrollAnchorSection: CataloguePage {
    static let title = "AI scroll anchor (standard ScrollView)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var messages = Array(1...12)
    @State private var position: Int?

    var body: some View {
        CatalogueCaption("defaultScrollAnchor(.bottom): opens at the newest message")
        ScrollView {
            LazyVStack(alignment: .leading, spacing: theme.spacing.space2) {
                ForEach(messages, id: \.self) { message in
                    Text("Message \(message)")
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .scrollTargetLayout()
        }
        .defaultScrollAnchor(.bottom)
        .scrollPosition(id: $position, anchor: .bottom)
        .frame(height: theme.spacing.space24 * 2)

        HStack(spacing: theme.spacing.space2) {
            Button("Add a message") {
                messages.append((messages.last ?? 0) + 1)
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
            Button("Jump to latest") {
                withAnimation(theme.motion.animation(reduceMotion: reduceMotion)) {
                    position = messages.last
                }
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
        }
    }
}
