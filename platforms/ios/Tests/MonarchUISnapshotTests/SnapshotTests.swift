import Foundation
import Testing

/// One image of one component.
struct SnapshotCase: Sendable, CustomTestStringConvertible {
    let key: SnapshotKey
    var testDescription: String { key.relativePath }

    static let core = Set(SnapshotStore.coreIDs())

    /// Every image of every component with fixtures that is part of this run
    /// (`MONARCH_COMPONENT` limits it, as for the matrix test).
    static let all: [SnapshotCase] = FixtureRegistry.all
        .filter {
            ComponentFilter.includes($0.id) && !$0.fixtures.isEmpty && SnapshotStore.excluded[$0.id] == nil
        }
        .flatMap { SnapshotStore.keys(for: $0.id, core: core) }
        .map(SnapshotCase.init)
}

#if os(iOS)
    /// Renders each component and compares it with its baseline in `Tests/Snapshots`, or records
    /// it with `MONARCH_SNAPSHOT_RECORD=1`. The iOS Simulator is the only reference environment: the host
    /// has other fonts, so the test is not built for the host.
    ///
    /// Serialized, like the matrix: each case needs the main actor, and a case's time must be its own.
    @Suite("Snapshots", .serialized)
    @MainActor
    struct SnapshotTests {
        @Test("Component matches its baseline", arguments: SnapshotCase.all)
        func matchesBaseline(_ item: SnapshotCase) {
            let key = item.key
            guard let set = FixtureRegistry.all.first(where: { $0.id == key.id }) else {
                Issue.record("\(key.id) has no fixtures")
                return
            }
            let url = SnapshotStore.baselineURL(key)

            if SnapshotStore.isRecording {
                guard let fitted = SnapshotRenderer.fitted(set, key) else {
                    Issue.record("\(key.relativePath): nothing was drawn")
                    return
                }
                if fitted.pixels.isBlank {
                    // Not written: a blank baseline would pass for ever. The component needs a fixture that
                    // ImageRenderer can draw, or an entry in SnapshotStore.excluded.
                    Issue.record("\(key.relativePath): blank render (one colour); not recorded")
                    return
                }
                do {
                    try FileManager.default.createDirectory(
                        at: url.deletingLastPathComponent(),
                        withIntermediateDirectories: true
                    )
                    try fitted.png.write(to: url)
                } catch {
                    Issue.record("\(key.relativePath): cannot write the baseline: \(error)")
                }
                if fitted.png.count > SnapshotEnvironment.maxBytes {
                    Issue.record(
                        "\(key.relativePath): \(fitted.png.count) bytes at scale \(fitted.scale), over the \(SnapshotEnvironment.maxBytes) limit"
                    )
                }
                return
            }

            guard let data = try? Data(contentsOf: url), let baseline = Pixels.decode(png: data) else {
                Issue.record(
                    "\(key.relativePath): no baseline. Record with MONARCH_SNAPSHOT_RECORD=1 Scripts/verify.sh --snapshots"
                )
                return
            }
            guard let scale = SnapshotStore.scale(ofBaselineWidth: baseline.width),
                let actual = SnapshotRenderer.pixels(set, key, scale: scale)
            else {
                Issue.record("\(key.relativePath): the baseline is \(baseline.width) px wide, not a recorded scale")
                return
            }
            if actual.isBlank { Issue.record("\(key.relativePath): blank render (one colour)") }
            let difference = SnapshotCompare.compare(actual: actual, baseline: baseline, tolerance: .standard)
            if difference.exactlyDifferingPixels > 0 {
                // Not a failure: a record of how far the render is from the baseline, for the determinism check.
                print("snapshot-drift: \(key.relativePath) \(difference.exactlyDifferingPixels) pixels differ at all")
            }
            if !difference.passes(.standard) {
                let paths = SnapshotStore.writeFailure(key, actual: actual, diff: difference.diff)
                Issue.record(
                    "\(key.relativePath): \(difference.summary). Baseline: \(url.path)\n  Actual and diff: \(paths.joined(separator: "\n  "))"
                )
            }
        }
    }
#endif
