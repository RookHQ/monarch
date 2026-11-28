import Foundation
import Testing

/// The ids whose fixtures the clock test draws: the ones the snapshot matrix covers.
enum SnapshotClockIDs {
    static let all: [String] = FixtureRegistry.all
        .filter { ComponentFilter.includes($0.id) && !$0.fixtures.isEmpty && SnapshotStore.excluded[$0.id] == nil }
        .map(\.id)
}

#if os(iOS)
    /// A snapshot must not change with the real date. Each component's fixtures are drawn twice, with the
    /// clock pinned (`monarchNow`) to two dates far apart; the two images must be identical. A fixture that
    /// shows "today" or a relative time pins its own clock (`.monarchNow(...)`, or the component's `today` or
    /// `now` argument), which the outer clock does not override.
    @Suite("Snapshots do not depend on the real date", .serialized)
    @MainActor
    struct SnapshotClockTests {
        private static let early = Date(timeIntervalSince1970: 978_307_200)  // 1 Jan 2001
        private static let late = Date(timeIntervalSince1970: 1_907_366_400)  // 15 Jun 2030

        @Test("Fixtures draw the same on any day", arguments: SnapshotClockIDs.all)
        func sameOnAnyDay(_ id: String) {
            guard let set = FixtureRegistry.all.first(where: { $0.id == id }) else { return }
            let key = SnapshotKey(id: id, theme: "light", size: "default", direction: "ltr")
            let first = SnapshotRenderer.pixels(set, key, scale: 1, now: Self.early)
            let second = SnapshotRenderer.pixels(set, key, scale: 1, now: Self.late)
            // A fixture that animates or draws differently on each render cannot say anything about the date.
            let again = SnapshotRenderer.pixels(set, key, scale: 1, now: Self.early)
            guard first?.bytes == again?.bytes else { return }
            #expect(
                first?.bytes == second?.bytes,
                "\(id): the fixtures change with the date. Pin the clock in the fixture with .monarchNow(date), or pass the component's today or now."
            )
        }
    }
#endif
