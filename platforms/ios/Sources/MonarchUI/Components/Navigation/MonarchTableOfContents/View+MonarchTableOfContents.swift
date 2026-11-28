import SwiftUI

extension View {
    /// Marks a heading as the start of a section listed in ``MonarchTableOfContents``.
    ///
    /// It gives the view `id` for `ScrollViewProxy.scrollTo(_:anchor:)` and, inside a scroll
    /// view with `monarchTableOfContentsSpy(selection:offset:)`, reports where the heading is.
    ///
    /// - Parameter id: The identity of the section, as in its table of contents entry.
    /// - Returns: The heading, marked.
    public func monarchTableOfContentsTarget<ID: Hashable & Sendable>(_ id: ID) -> some View {
        modifier(MonarchTableOfContentsTargetModifier(id: AnyHashable(id))).id(id)
    }

    /// Keeps `selection` on the section being read while this scroll view scrolls.
    ///
    /// The current section is the first heading marked with
    /// `monarchTableOfContentsTarget(_:)` in the top 40% of the visible area,
    /// or else the last heading scrolled past.
    ///
    /// - Parameters:
    ///   - selection: The section being read, shared with ``MonarchTableOfContents``.
    ///   - offset: The height of anything that covers the top of the scroll view. A heading
    ///     becomes current once it passes this line.
    /// - Returns: The scroll view, observed.
    public func monarchTableOfContentsSpy<ID: Hashable>(selection: Binding<ID?>, offset: CGFloat = 0) -> some View {
        modifier(MonarchTableOfContentsSpyModifier(selection: selection, offset: offset))
    }
}

/// Where each marked heading is, shared by the spy with the headings inside its scroll view.
@MainActor
@Observable
final class MonarchTableOfContentsSpyStore {
    /// The top of each heading in the scroll view's visible area.
    var positions: [AnyHashable: CGFloat] = [:]
}

private struct MonarchTableOfContentsTargetModifier: ViewModifier {
    let id: AnyHashable

    @Environment(MonarchTableOfContentsSpyStore.self) private var store: MonarchTableOfContentsSpyStore?

    func body(content: Content) -> some View {
        content
            .onGeometryChange(
                for: CGFloat.self,
                of: { $0.frame(in: .named(MonarchTableOfContentsTokens.coordinateSpace)).minY },
                action: { store?.positions[id] = $0 }
            )
            .onDisappear { store?.positions[id] = nil }
    }
}

private struct MonarchTableOfContentsSpyModifier<ID: Hashable>: ViewModifier {
    @Binding var selection: ID?
    let offset: CGFloat

    @State private var store = MonarchTableOfContentsSpyStore()
    @State private var viewportHeight: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .environment(store)
            .coordinateSpace(.named(MonarchTableOfContentsTokens.coordinateSpace))
            .onGeometryChange(for: CGFloat.self, of: { $0.size.height }, action: { viewportHeight = $0 })
            .onChange(of: store.positions) { update() }
            .onChange(of: viewportHeight) { update() }
    }

    private func update() {
        let current = MonarchTableOfContentsTokens.current(
            positions: store.positions,
            viewportHeight: viewportHeight,
            offset: offset
        )
        if let current = current?.base as? ID, current != selection {
            selection = current
        }
    }
}
