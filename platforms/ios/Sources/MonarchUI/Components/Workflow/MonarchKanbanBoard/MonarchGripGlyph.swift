import SwiftUI

/// The drag grip of the web's boards and lists: two columns of three dots, drawn at the size of an icon.
struct MonarchGripGlyph: View {
    @ScaledMetric(relativeTo: .body) private var side: CGFloat = 16

    var body: some View {
        let dot = side * 0.15
        HStack(spacing: dot) {
            ForEach(0..<2, id: \.self) { _ in
                VStack(spacing: dot) {
                    ForEach(0..<3, id: \.self) { _ in Circle().frame(width: dot, height: dot) }
                }
            }
        }
        .frame(width: side, height: side)
        .accessibilityHidden(true)
    }
}
