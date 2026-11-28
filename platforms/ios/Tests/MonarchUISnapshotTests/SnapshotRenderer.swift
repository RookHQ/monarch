import CoreGraphics
import MonarchUI
import SwiftUI

#if canImport(UIKit)
    import UIKit
#endif

/// Renders the fixtures of one component to pixels, with every input pinned.
@MainActor
enum SnapshotRenderer {
    /// All fixtures of a component, one under the other, in the key's theme and conditions.
    static func view(_ set: FixtureSet, _ key: SnapshotKey, now: Date? = nil) -> some View {
        let theme = ThemeCase.all.first { $0.id == key.theme } ?? ThemeCase.all[0]
        let condition: RenderCondition =
            switch (key.size == "x2", key.direction == "rtl") {
            case (false, false): .standard
            case (true, false): .largestText
            case (false, true): .rightToLeft
            case (true, true): .largestTextRightToLeft
            }
        return VStack(alignment: .leading, spacing: 16) {
            ForEach(Array(set.fixtures.enumerated()), id: \.offset) { _, fixture in
                fixture.make().modifier(PinnedNow(now: SnapshotEnvironment.pinnedNow[set.id]))
            }
        }
        // Full width first, so the canvas colour fills the whole image, not only the content.
        .frame(width: SnapshotEnvironment.width - 32, alignment: .leading)
        // A container with no height of its own (a lazy pager, a pane that fills what it is given) is drawn at a
        // fixed height.
        .frame(height: SnapshotStore.fixedHeights[key.id].map { CGFloat($0) })
        .rendered(in: theme, condition)
        .environment(\.locale, SnapshotEnvironment.locale)
        .environment(\.calendar, SnapshotEnvironment.calendar)
        .environment(\.timeZone, SnapshotEnvironment.timeZone)
        // A clock pinned from outside; a fixture that pins its own with `.monarchNow` keeps it.
        .modifier(PinnedNow(now: now))
        .transaction {
            $0.animation = nil
            $0.disablesAnimations = true
        }
    }

    /// The component as palette-reduced pixels and their PNG at a scale: the largest palette (256 down to 16
    /// colours) whose PNG fits the size limit, or the smallest. Nil when nothing was drawn. A pure function of
    /// the render, so the recorded image and the compared one go through the same steps.
    static func rendered(_ set: FixtureSet, _ key: SnapshotKey, scale: Double, now: Date? = nil) -> (
        pixels: Pixels, png: Data
    )? {
        #if canImport(UIKit)
            UIView.setAnimationsEnabled(false)
        #endif
        let renderer = ImageRenderer(content: view(set, key, now: now))
        renderer.scale = scale
        renderer.isOpaque = true
        renderer.proposedSize = ProposedViewSize(width: SnapshotEnvironment.width, height: nil)
        guard let image = renderer.cgImage, let pixels = Pixels(image) else { return nil }
        return pixels.fitted(maxBytes: SnapshotEnvironment.maxBytes)
    }

    /// The component as quantized pixels at a scale, or nil when nothing was drawn.
    static func pixels(_ set: FixtureSet, _ key: SnapshotKey, scale: Double, now: Date? = nil) -> Pixels? {
        rendered(set, key, scale: scale, now: now)?.pixels
    }

    /// The first scale whose PNG fits the size limit, with that PNG. If none fits, the smallest.
    static func fitted(_ set: FixtureSet, _ key: SnapshotKey) -> (pixels: Pixels, png: Data, scale: Double)? {
        var last: (Pixels, Data, Double)?
        for scale in SnapshotEnvironment.scales {
            guard let result = rendered(set, key, scale: scale) else { return nil }
            last = (result.pixels, result.png, scale)
            if result.png.count <= SnapshotEnvironment.maxBytes { break }
        }
        return last.map { (pixels: $0.0, png: $0.1, scale: $0.2) }
    }
}

/// Applies `.monarchNow` only when a date is given.
private struct PinnedNow: ViewModifier {
    let now: Date?

    func body(content: Content) -> some View {
        if let now { content.monarchNow(now) } else { content }
    }
}
