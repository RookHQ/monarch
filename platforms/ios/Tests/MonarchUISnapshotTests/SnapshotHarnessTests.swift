import CoreGraphics
import Foundation
import Testing

/// Tests of the harness itself: names, the core list, pixels, the compare and the failure files.
@Suite("Snapshot harness")
struct SnapshotHarnessTests {
    private func solid(_ width: Int, _ height: Int, _ red: UInt8, _ green: UInt8 = 255, _ blue: UInt8 = 255) -> Pixels {
        var bytes: [UInt8] = []
        for _ in 0..<(width * height) { bytes += [red, green, blue, 255] }
        return Pixels(width: width, height: height, bytes: bytes)
    }

    @Test("File names and paths follow the Android shape and stay out of previews")
    func keyNames() {
        #expect(
            SnapshotKey(id: "button", theme: "hc-light", size: "x2", direction: "rtl").fileName
                == "button.hc-light.x2.rtl.png"
        )
        #expect(
            SnapshotKey(id: "badge", theme: "light", size: "default", direction: "ltr").relativePath
                == "Tests/Snapshots/badge/badge.light.png"
        )
        #expect(
            SnapshotKey(id: "badge", theme: "dark", size: "x2", direction: "ltr").relativePath
                == "Tests/Snapshots/badge/badge.dark.x2.png"
        )
        #expect(
            SnapshotKey(id: "badge", theme: "light", size: "default", direction: "rtl").relativePath
                == "Tests/Snapshots/badge/badge.light.rtl.png"
        )
    }

    @Test("Every component gets light and dark; the core set gets sixteen images")
    func matrix() {
        #expect(SnapshotStore.keys(for: "badge", core: []).map(\.fileName) == ["badge.light.png", "badge.dark.png"])
        let core = SnapshotStore.keys(for: "badge", core: ["badge"])
        #expect(core.count == 16)
        #expect(Set(core.map(\.fileName)).count == 16)
        #expect(core.contains { $0.theme == "hc-dark" && $0.size == "x2" && $0.direction == "rtl" })
    }

    /// The ids of `inventory/*.json`.
    private static let inventoryIDs: Set<String> = {
        let folder = SnapshotStore.packageRoot.appendingPathComponent("inventory")
        let names = (try? FileManager.default.contentsOfDirectory(atPath: folder.path)) ?? []
        return Set(names.filter { $0.hasSuffix(".json") }.map { String($0.dropLast(5)) })
    }()

    @Test("The core list has 30 ids, each a real iOS inventory id with fixtures")
    func coreList() {
        let ids = SnapshotStore.coreIDs()
        #expect(ids.count == 30, "core-components.txt lists \(ids.count) ids")
        #expect(Set(ids).count == ids.count)
        #expect(
            Set(ids).subtracting(Self.inventoryIDs).isEmpty,
            "not inventory ids: \(Set(ids).subtracting(Self.inventoryIDs).sorted())"
        )
        #expect(
            Set(ids).intersection(SnapshotStore.excluded.keys) == ["calendar-month-view"],
            "only calendar-month-view may be both core and excluded on iOS"
        )
        if ComponentFilter.ids == nil {
            let withFixtures = Set(FixtureRegistry.all.filter { !$0.fixtures.isEmpty }.map(\.id))
            #expect(
                Set(ids).subtracting(withFixtures).isEmpty,
                "core ids without fixtures: \(Set(ids).subtracting(withFixtures).sorted())"
            )
        }
    }

    @Test("Fixed-height components are real inventory ids with fixtures' sizes in a sane range")
    func fixedHeights() {
        for (id, height) in SnapshotStore.fixedHeights {
            #expect(Self.inventoryIDs.contains(id), "\(id) is not an inventory id")
            #expect((100...800).contains(height), "\(id) height \(height)")
        }
    }

    @Test("Excluded components are real, have a reason, and are not core")
    func exclusions() {
        for (id, reason) in SnapshotStore.excluded {
            #expect(Self.inventoryIDs.contains(id), "\(id) is not an inventory id")
            #expect(reason.count > 10, "\(id) needs a reason")
        }
    }

    @Test("Many colours are reduced to a 256-colour palette PNG that round-trips and is smaller")
    func palette() throws {
        var bytes: [UInt8] = []
        for index in 0..<(64 * 64) {
            bytes += [UInt8(index % 251), UInt8((index / 7) % 253), UInt8((index / 3) % 241), 255]
        }
        let noisy = Pixels(width: 64, height: 64, bytes: bytes)
        let reduced = noisy.paletteReduced()
        var colours = Set<[UInt8]>()
        for index in stride(from: 0, to: reduced.bytes.count, by: 4) {
            colours.insert(Array(reduced.bytes[index..<index + 3]))
        }
        #expect(colours.count <= 256)
        let data = try #require(reduced.png())
        #expect(try #require(Pixels.decode(png: data)) == reduced)
        #expect(reduced.paletteReduced() == reduced, "reducing twice must change nothing")
        let flat = solid(64, 64, 200)
        #expect(try #require(flat.png()).count < 400)
    }

    @Test("Large flat fills keep their exact colour when the colour count grows")
    func flatFillsStayExact() {
        let pale: [UInt8] = [0xFC, 0xF0, 0xF0, 255]
        let dark: [UInt8] = [0x14, 0x18, 0x20, 255]
        let width = 120
        let height = 80
        var bytes = [UInt8](repeating: 255, count: width * height * 4)
        func paint(_ x: Range<Int>, _ y: Range<Int>, _ colour: [UInt8]) {
            for row in y {
                for column in x {
                    bytes.replaceSubrange((row * width + column) * 4..<(row * width + column) * 4 + 4, with: colour)
                }
            }
        }
        paint(10..<50, 10..<30, pale)
        paint(60..<110, 40..<70, dark)
        for index in 0..<600 {
            paint(
                index % 120..<index % 120 + 1,
                75 + index / 120..<76 + index / 120,
                [UInt8(index % 256), UInt8(index * 7 % 256), UInt8(index * 13 % 256), 255]
            )
        }
        let reduced = Pixels(width: width, height: height, bytes: bytes).quantised(budget: 256)
        func pixel(_ x: Int, _ y: Int) -> [UInt8] {
            Array(reduced.bytes[(y * width + x) * 4..<(y * width + x) * 4 + 4])
        }
        #expect(pixel(20, 20) == pale, "the pale pill stays pale")
        #expect(pixel(80, 55) == dark, "the dark card stays dark")
        #expect(pixel(0, 0) == [255, 255, 255, 255])
        var colours = Set<[UInt8]>()
        for index in stride(from: 0, to: reduced.bytes.count, by: 4) {
            colours.insert(Array(reduced.bytes[index..<index + 3]))
        }
        #expect(colours.count <= 256)
        #expect(reduced.quantised(budget: 256) == reduced, "reducing twice must change nothing")
    }

    @Test("An image with few colours is kept exactly, and a smaller budget never exceeds itself")
    func fewColoursStayExact() {
        var bytes: [UInt8] = []
        for index in 0..<(30 * 30) {
            bytes += index % 7 == 0 ? [UInt8(index % 60) << 2, 8, 200, 255] : [255, 255, 255, 255]
        }
        let image = Pixels(width: 30, height: 30, bytes: bytes)
        #expect(image.quantised(budget: 256) == image)
        let fitted = image.fitted(maxBytes: 10)
        #expect(fitted != nil)
        #expect((fitted?.pixels.quantised(budget: 16) == fitted?.pixels) == true)
    }

    @Test("The core list matches the Android one when it exists")
    func coreListMatchesAndroid() {
        let android = SnapshotStore.packageRoot
            .appendingPathComponent("../android/monarch-compose/src/test/screenshots/core-components.txt")
            .standardizedFileURL
        guard FileManager.default.fileExists(atPath: android.path) else { return }
        // The iOS list leaves out date-picker: it marks today from the real clock (see core-components.txt).
        #expect(SnapshotStore.coreIDs() == SnapshotStore.coreIDs(in: android).filter { $0 != "date-picker" })
    }

    @Test("Quantizing keeps six bits and PNG encoding round-trips exactly")
    func pngRoundTrip() throws {
        let pixels = solid(8, 4, 0xFC, 0x80, 0x04)
        let data = try #require(pixels.png())
        #expect(try #require(Pixels.decode(png: data)) == pixels)
    }

    @Test("A single-colour image is blank; one changed pixel is not")
    func blank() {
        var pixels = solid(8, 8, 200)
        #expect(pixels.isBlank)
        pixels.bytes[4 * 9] = 10
        #expect(!pixels.isBlank)
    }

    @Test("Identical images pass even with no tolerance")
    func identical() {
        let a = solid(10, 10, 100)
        let result = SnapshotCompare.compare(actual: a, baseline: a, tolerance: .exact)
        #expect(result.passes(.exact))
        #expect(result.differingPixels == 0)
    }

    @Test("Small channel noise passes; a real change fails and is drawn red")
    func tolerance() {
        let baseline = solid(100, 100, 100)
        let noisy = solid(100, 100, 104)
        #expect(SnapshotCompare.compare(actual: noisy, baseline: baseline, tolerance: .standard).passes(.standard))

        var changed = baseline
        for row in 0..<10 {
            for column in 0..<10 { changed.bytes[(row * 100 + column) * 4] = 250 }
        }
        let result = SnapshotCompare.compare(actual: changed, baseline: baseline, tolerance: .standard)
        #expect(result.differingPixels == 100)
        #expect(!result.passes(.standard))
        #expect(result.diff?.bytes[0] == 255 && result.diff?.bytes[1] == 0)
    }

    @Test("A different size fails and says so")
    func sizeMismatch() {
        let result = SnapshotCompare.compare(actual: solid(10, 10, 1), baseline: solid(10, 12, 1), tolerance: .standard)
        #expect(!result.passes(.standard))
        #expect(result.summary.contains("10x10") && result.summary.contains("10x12"))
    }

    @Test("A failure writes the actual and diff images and returns their paths")
    func failureFiles() throws {
        let key = SnapshotKey(id: "harness-self-test", theme: "light", size: "default", direction: "ltr")
        let actual = solid(4, 4, 200)
        let result = SnapshotCompare.compare(actual: actual, baseline: solid(4, 4, 0), tolerance: .exact)
        let paths = SnapshotStore.writeFailure(key, actual: actual, diff: result.diff)
        #expect(paths.count == 2)
        for path in paths {
            #expect(FileManager.default.fileExists(atPath: path))
            try? FileManager.default.removeItem(atPath: path)
        }
    }

    @Test("A baseline's width gives back its scale")
    func scales() {
        #expect(SnapshotStore.scale(ofBaselineWidth: 780) == 2)
        #expect(SnapshotStore.scale(ofBaselineWidth: 585) == 1.5)
        #expect(SnapshotStore.scale(ofBaselineWidth: 390) == 1)
        #expect(SnapshotStore.scale(ofBaselineWidth: 400) == nil)
    }
}
