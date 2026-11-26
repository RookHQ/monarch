import SwiftUI

/// The fixtures of one component: the views that the matrix test renders in
/// all four themes, at the default and the largest Dynamic Type size, left to
/// right and right to left.
///
/// Declare one conforming enum in the component's own test file. Do not add
/// it to any list: `Scripts/generate.py` finds every `enum …: ComponentFixtures`
/// under `Tests/MonarchUITests/Components` and writes the registry.
///
/// ```swift
/// enum MonarchBadgeFixtures: ComponentFixtures {
///     static let id = "badge"
///     static let fixtures = [
///         Fixture("tones") { MonarchBadge("Paid", tone: .success) }
///     ]
/// }
/// ```
protocol ComponentFixtures {
    /// The inventory id of the component: the name of its `inventory/<id>.json`.
    static var id: String { get }
    /// The fixtures. Cover every variant, size, tone and state.
    static var fixtures: [Fixture] { get }
}

/// One named view to render.
struct Fixture: Sendable {
    /// A short name, unique within the component, shown in test output.
    let name: String
    /// Builds the view on the main actor.
    let make: @MainActor @Sendable () -> AnyView

    /// A fixture.
    init(_ name: String, @ViewBuilder _ content: @escaping @MainActor @Sendable () -> some View) {
        self.name = name
        self.make = { AnyView(content()) }
    }
}

/// The fixtures of one component as a value, for the generated registry.
struct FixtureSet: Sendable {
    /// The inventory id of the component.
    let id: String
    /// Its fixtures.
    let fixtures: [Fixture]

    /// The fixtures a component's test file declares.
    init<Component: ComponentFixtures>(_ component: Component.Type) {
        self.id = component.id
        self.fixtures = component.fixtures
    }
}
