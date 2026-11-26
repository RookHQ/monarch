import MonarchUI
import SwiftUI
import Testing

/// One fixture of one component, as a test argument.
struct FixtureCase: Sendable, CustomTestStringConvertible {
    let id: String
    let fixture: Fixture

    var testDescription: String { "\(id)/\(fixture.name)" }

    /// Every fixture in the generated registry that is part of this run.
    static let all: [FixtureCase] = FixtureRegistry.all
        .filter { ComponentFilter.includes($0.id) }
        .flatMap { component in component.fixtures.map { FixtureCase(id: component.id, fixture: $0) } }
}

/// Hosts every fixture of every component in all four themes, at the default
/// and the largest Dynamic Type size, left to right and right to left. A
/// crash, an empty layout or a failed draw fails the test.
///
/// Fixtures are discovered, not listed: see `ComponentFixtures`.
///
/// The suite is serialized. Swift Testing starts every case at once, and each
/// case needs the main actor, so with about 8,000 cases the clock of a case
/// ran for minutes before it began. `xcodebuild -test-timeouts-enabled` (the
/// per-test allowances of `Scripts/verify.sh`) then flagged every case and
/// killed the runner. Serialized, a case's time is its own.
@Suite("Component matrix", .serialized)
@MainActor
struct ComponentMatrixTests {
    @Test("Every fixture lays out and draws", arguments: FixtureCase.all, ThemeCase.all)
    func rendersInEveryCondition(fixtureCase: FixtureCase, theme: ThemeCase) {
        for condition in RenderCondition.allCases {
            let view = fixtureCase.fixture.make().rendered(in: theme, condition)
            let size = Host.layout(view)
            #expect(size.width > 0 && size.height > 0, "\(fixtureCase) \(theme) \(condition) has no size")
            #expect(Host.image(view) != nil, "\(fixtureCase) \(theme) \(condition) did not draw")
        }
    }

    @Test("Fixture names are unique within a component")
    func fixtureNamesAreUnique() {
        for component in FixtureRegistry.all {
            let names = component.fixtures.map(\.name)
            #expect(Set(names).count == names.count, "\(component.id) repeats a fixture name")
            #expect(!names.isEmpty, "\(component.id) has no fixtures")
        }
    }
}
