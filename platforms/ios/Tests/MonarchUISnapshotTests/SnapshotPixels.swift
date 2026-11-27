import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

/// Pixels, PNG encoding and comparison for the screenshot tests. Pure Core
/// Graphics and Image I/O: no SwiftUI, so the same code is unit-tested on the host.
///
/// Every image goes through one pipeline, for the recorded baseline and for the
/// fresh render alike: draw into 8-bit sRGB RGBA, keep the top six bits of each
/// channel (the low two bits are rendering noise and make PNGs large), encode.
/// The compare therefore never judges encoder differences, only pixels.
struct Pixels: Equatable {
    let width: Int
    let height: Int
    /// RGBA, 4 bytes per pixel, row by row, alpha always 255.
    var bytes: [UInt8]

    /// The channel bits kept. 0xFC keeps six of eight.
    static let channelMask: UInt8 = 0xFC

    init(width: Int, height: Int, bytes: [UInt8]) {
        self.width = width
        self.height = height
        self.bytes = bytes
    }

    /// Draws any image into the canonical format. Nil for an empty image.
    init?(_ image: CGImage, quantize: Bool = true) {
        guard image.width > 0, image.height > 0,
            let space = CGColorSpace(name: CGColorSpace.sRGB)
        else { return nil }
        var data = [UInt8](repeating: 0, count: image.width * image.height * 4)
        let drawn = data.withUnsafeMutableBytes { buffer -> Bool in
            guard
                let context = CGContext(
                    data: buffer.baseAddress,
                    width: image.width,
                    height: image.height,
                    bitsPerComponent: 8,
                    bytesPerRow: image.width * 4,
                    space: space,
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
                )
            else { return false }
            // Opaque white underneath, so transparent pixels are deterministic.
            context.setFillColor(CGColor(srgbRed: 1, green: 1, blue: 1, alpha: 1))
            context.fill(CGRect(x: 0, y: 0, width: image.width, height: image.height))
            context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
            return true
        }
        guard drawn else { return nil }
        if quantize {
            data.withUnsafeMutableBufferPointer { buffer in
                for index in stride(from: 0, to: buffer.count, by: 4) {
                    buffer[index] &= Pixels.channelMask
                    buffer[index + 1] &= Pixels.channelMask
                    buffer[index + 2] &= Pixels.channelMask
                    buffer[index + 3] = 255
                }
            }
        }
        self.init(width: image.width, height: image.height, bytes: data)
    }

    /// True when every pixel is the same colour: nothing was drawn (a view that ImageRenderer cannot render, such
    /// as a UIKit-backed one, or a layout with no size), so the image proves nothing.
    var isBlank: Bool {
        guard bytes.count >= 8 else { return true }
        return bytes.withUnsafeBufferPointer { buffer in
            let first = (buffer[0], buffer[1], buffer[2])
            for index in stride(from: 4, to: buffer.count, by: 4)
            where (buffer[index], buffer[index + 1], buffer[index + 2]) != first {
                return false
            }
            return true
        }
    }

    /// The pixels as an image.
    var cgImage: CGImage? {
        guard let space = CGColorSpace(name: CGColorSpace.sRGB),
            let provider = CGDataProvider(data: Data(bytes) as CFData)
        else { return nil }
        return CGImage(
            width: width,
            height: height,
            bitsPerComponent: 8,
            bitsPerPixel: 32,
            bytesPerRow: width * 4,
            space: space,
            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.noneSkipLast.rawValue),
            provider: provider,
            decode: nil,
            shouldInterpolate: false,
            intent: .defaultIntent
        )
    }

    /// The palette sizes tried, largest first, when an image must shrink to fit the size limit.
    static let budgets = [256, 128, 64, 32, 16]

    /// The pixels with at most 256 distinct colours; see `quantised(budget:)`.
    func paletteReduced() -> Pixels { quantised(budget: 256) }

    /// The pixels with at most `budget` distinct colours. An image that already has few enough is kept exactly.
    /// Otherwise a median-cut quantiser splits the colour histogram (always the box with the most error, the sum
    /// of count times squared distance to its most frequent colour, along its widest channel at the weighted
    /// median) and every colour becomes the most frequent colour of its box, not the mean. A large flat fill (a
    /// pale pill, a dark card, the canvas) therefore keeps its exact colour; only the anti-aliasing around text
    /// and edges is merged. A pure function of the pixels: the recorded image and the fresh render go through the
    /// same step, so it never causes a difference by itself.
    func quantised(budget: Int) -> Pixels {
        var histogram: [UInt32: Int] = [:]
        for index in stride(from: 0, to: bytes.count, by: 4) { histogram[key(at: index), default: 0] += 1 }
        if histogram.count <= budget { return self }
        let colours = histogram.keys.sorted()
        let counts = colours.map { histogram[$0] ?? 0 }
        var boxes = [ColourBox(members: Array(colours.indices), colours: colours, counts: counts)]
        while boxes.count < budget {
            var worst = -1
            for (position, box) in boxes.enumerated() where box.members.count > 1 {
                if worst < 0 || box.error > boxes[worst].error { worst = position }
            }
            if worst < 0 || boxes[worst].error == 0 { break }
            let (low, high) = boxes[worst].split(colours: colours, counts: counts)
            boxes[worst] = low
            boxes.append(high)
        }
        var target = [UInt32](repeating: 0, count: colours.count)
        for box in boxes { for member in box.members { target[member] = colours[box.mode] } }
        var position: [UInt32: Int] = [:]
        for (offset, colour) in colours.enumerated() { position[colour] = offset }
        var out = self
        for index in stride(from: 0, to: bytes.count, by: 4) {
            let colour = target[position[key(at: index)] ?? 0]
            out.bytes[index] = UInt8(colour >> 16 & 255)
            out.bytes[index + 1] = UInt8(colour >> 8 & 255)
            out.bytes[index + 2] = UInt8(colour & 255)
        }
        return out
    }

    /// The first palette size, from 256 down to 16, whose PNG fits `maxBytes` (the smallest if none does), with
    /// that PNG. Nil if encoding fails.
    func fitted(maxBytes: Int) -> (pixels: Pixels, png: Data)? {
        var last: (Pixels, Data)?
        for budget in Pixels.budgets {
            let reduced = quantised(budget: budget)
            guard let png = reduced.png() else { return nil }
            last = (reduced, png)
            if png.count <= maxBytes { break }
        }
        return last.map { (pixels: $0.0, png: $0.1) }
    }

    private func key(at index: Int) -> UInt32 {
        UInt32(bytes[index]) << 16 | UInt32(bytes[index + 1]) << 8 | UInt32(bytes[index + 2])
    }

    /// A set of histogram entries (indices into the sorted colour list) and its most frequent colour.
    private struct ColourBox {
        let members: [Int]
        let mode: Int
        let error: Int

        init(members: [Int], colours: [UInt32], counts: [Int]) {
            self.members = members
            var best = members[0]
            for member in members where counts[member] > counts[best] { best = member }
            mode = best
            var sum = 0
            for member in members { sum += ColourBox.distance(colours[member], colours[best]) * counts[member] }
            error = sum
        }

        func split(colours: [UInt32], counts: [Int]) -> (ColourBox, ColourBox) {
            var shift: UInt32 = 16
            var widest = -1
            for candidate: UInt32 in [16, 8, 0] {
                let values = members.map { Int(colours[$0] >> candidate & 255) }
                let range = (values.max() ?? 0) - (values.min() ?? 0)
                if range > widest {
                    widest = range
                    shift = candidate
                }
            }
            let ordered = members.sorted {
                let a = colours[$0] >> shift & 255
                let b = colours[$1] >> shift & 255
                return a != b ? a < b : $0 < $1
            }
            let total = ordered.reduce(0) { $0 + counts[$1] }
            var running = 0
            var cut = 1
            for (position, member) in ordered.enumerated() {
                running += counts[member]
                if running * 2 >= total {
                    cut = min(max(position + 1, 1), ordered.count - 1)
                    break
                }
            }
            return (
                ColourBox(members: Array(ordered[..<cut]), colours: colours, counts: counts),
                ColourBox(members: Array(ordered[cut...]), colours: colours, counts: counts)
            )
        }

        static func distance(_ a: UInt32, _ b: UInt32) -> Int {
            let dr = Int(a >> 16 & 255) - Int(b >> 16 & 255)
            let dg = Int(a >> 8 & 255) - Int(b >> 8 & 255)
            let db = Int(a & 255) - Int(b & 255)
            return dr * dr + dg * dg + db * db
        }
    }

    /// PNG data: an 8-bit palette image when the pixels have 256 colours or fewer (after `paletteReduced`),
    /// otherwise RGB. Nil if encoding fails.
    func png() -> Data? {
        var lookup = [Int16](repeating: -1, count: 1 << 24)
        var table: [UInt32] = []
        var indices = [UInt8](repeating: 0, count: width * height)
        var overflow = false
        bytes.withUnsafeBufferPointer { source in
            indices.withUnsafeMutableBufferPointer { target in
                lookup.withUnsafeMutableBufferPointer { slots in
                    for pixel in 0..<(width * height) {
                        let at = pixel * 4
                        let colour = Int(source[at]) << 16 | Int(source[at + 1]) << 8 | Int(source[at + 2])
                        let found = slots[colour]
                        if found >= 0 {
                            target[pixel] = UInt8(found)
                        } else if table.count < 256 {
                            slots[colour] = Int16(table.count)
                            target[pixel] = UInt8(table.count)
                            table.append(UInt32(colour))
                        } else {
                            overflow = true
                            return
                        }
                    }
                }
            }
        }
        if overflow { return rgbPNG() }
        var rgb: [UInt8] = []
        for colour in table { rgb += [UInt8(colour >> 16 & 255), UInt8(colour >> 8 & 255), UInt8(colour & 255)] }
        guard let base = CGColorSpace(name: CGColorSpace.sRGB),
            let space = rgb.withUnsafeBufferPointer({ buffer -> CGColorSpace? in
                guard let colours = buffer.baseAddress else { return nil }
                return CGColorSpace(indexedBaseSpace: base, last: table.count - 1, colorTable: colours)
            }),
            let provider = CGDataProvider(data: Data(indices) as CFData),
            let image = CGImage(
                width: width,
                height: height,
                bitsPerComponent: 8,
                bitsPerPixel: 8,
                bytesPerRow: width,
                space: space,
                bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.none.rawValue),
                provider: provider,
                decode: nil,
                shouldInterpolate: false,
                intent: .defaultIntent
            )
        else { return rgbPNG() }
        return Pixels.encode(image)
    }

    private func rgbPNG() -> Data? {
        cgImage.flatMap(Pixels.encode)
    }

    private static func encode(_ image: CGImage) -> Data? {
        let data = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(data, UTType.png.identifier as CFString, 1, nil)
        else { return nil }
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else { return nil }
        return data as Data
    }

    /// Reads a PNG without changing a pixel.
    static func decode(png data: Data) -> Pixels? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
            let image = CGImageSourceCreateImageAtIndex(source, 0, nil)
        else { return nil }
        return Pixels(image, quantize: false)
    }
}

/// How much two images may differ before the compare fails.
struct SnapshotTolerance: Equatable, Sendable {
    /// A pixel differs when any channel is further apart than this (0 to 255).
    var channelDelta: Int
    /// The share of differing pixels that still passes, for example 0.001 is 0.1 percent.
    var pixelShare: Double

    /// The default. Measured on one simulator: identical renders differ by 0
    /// pixels; this absorbs sub-pixel text positioning noise across simulator
    /// builds without hiding a one-point shift of a control.
    static let standard = SnapshotTolerance(channelDelta: 8, pixelShare: 0.0005)
    static let exact = SnapshotTolerance(channelDelta: 0, pixelShare: 0)
}

/// The result of comparing a render with its baseline.
struct SnapshotDifference: Equatable {
    /// Pixels beyond the channel tolerance; nil when the sizes differ.
    var differingPixels: Int?
    /// Pixels that differ at all (any channel), for the determinism report; 0 for identical images.
    var exactlyDifferingPixels = 0
    var totalPixels: Int
    var sizeMismatch: (actual: String, baseline: String)?
    /// Red on a faded copy of the baseline where the images differ; nil when the sizes differ.
    var diff: Pixels?

    static func == (lhs: SnapshotDifference, rhs: SnapshotDifference) -> Bool {
        lhs.differingPixels == rhs.differingPixels && lhs.totalPixels == rhs.totalPixels
            && lhs.sizeMismatch?.actual == rhs.sizeMismatch?.actual
    }

    func passes(_ tolerance: SnapshotTolerance) -> Bool {
        guard sizeMismatch == nil, let count = differingPixels else { return false }
        return Double(count) <= Double(totalPixels) * tolerance.pixelShare
    }

    var summary: String {
        if let mismatch = sizeMismatch { return "size \(mismatch.actual), baseline \(mismatch.baseline)" }
        let count = differingPixels ?? 0
        let share = totalPixels == 0 ? 0 : Double(count) / Double(totalPixels) * 100
        return "\(count) of \(totalPixels) pixels differ (\(String(format: "%.3f", share)) percent)"
    }
}

enum SnapshotCompare {
    static func compare(actual: Pixels, baseline: Pixels, tolerance: SnapshotTolerance) -> SnapshotDifference {
        let total = actual.width * actual.height
        guard actual.width == baseline.width, actual.height == baseline.height else {
            return SnapshotDifference(
                differingPixels: nil,
                totalPixels: total,
                sizeMismatch: ("\(actual.width)x\(actual.height)", "\(baseline.width)x\(baseline.height)"),
                diff: nil
            )
        }
        var diff = baseline.bytes
        var count = 0
        var exact = 0
        let limit = tolerance.channelDelta
        actual.bytes.withUnsafeBufferPointer { a in
            baseline.bytes.withUnsafeBufferPointer { base in
                diff.withUnsafeMutableBufferPointer { out in
                    for index in stride(from: 0, to: a.count, by: 4) {
                        let dr = abs(Int(a[index]) - Int(base[index]))
                        let dg = abs(Int(a[index + 1]) - Int(base[index + 1]))
                        let db = abs(Int(a[index + 2]) - Int(base[index + 2]))
                        if dr | dg | db != 0 { exact += 1 }
                        if dr > limit || dg > limit || db > limit {
                            count += 1
                            out[index] = 255
                            out[index + 1] = 0
                            out[index + 2] = 0
                        } else {
                            // Fade the unchanged pixels so the red stands out.
                            out[index] = UInt8((Int(out[index]) + 255 * 3) / 4)
                            out[index + 1] = UInt8((Int(out[index + 1]) + 255 * 3) / 4)
                            out[index + 2] = UInt8((Int(out[index + 2]) + 255 * 3) / 4)
                        }
                        out[index + 3] = 255
                    }
                }
            }
        }
        return SnapshotDifference(
            differingPixels: count,
            exactlyDifferingPixels: exact,
            totalPixels: total,
            sizeMismatch: nil,
            diff: Pixels(width: actual.width, height: actual.height, bytes: diff)
        )
    }
}
