import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchButtonGroupFixtures: ComponentFixtures {
    static let id = "button-group"
    static let fixtures = [
        Fixture("attached: every variant and size") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                        MonarchButtonGroup("Pages", variant: variant, size: size) {
                            Button("Previous") {}
                            Button("Next") {}
                        }
                    }
                }
            }
        },
        Fixture("attached: disabled button, icons and one button") {
            VStack(alignment: .leading) {
                MonarchButtonGroup("Pages") {
                    Button("Previous") {}.disabled(true)
                    Button("Next") {}
                    Button("Last") {}
                }
                MonarchButtonGroup("Zoom", variant: .ghost) {
                    Button("Zoom out", systemImage: "minus") {}
                    Button("Zoom in", systemImage: "plus") {}
                }
                .labelStyle(.iconOnly)
                MonarchButtonGroup { Button("Alone") {} }
                MonarchButtonGroup("Disabled", variant: .primary) {
                    Button("One") {}
                    Button("Two") {}
                }
                .disabled(true)
            }
        },
        Fixture("vertical") {
            HStack(alignment: .top) {
                MonarchButtonGroup("Zoom", axis: .vertical) {
                    Button("Zoom in", systemImage: "plus") {}
                    Button("Zoom out", systemImage: "minus") {}
                }
                .labelStyle(.iconOnly)
                MonarchButtonGroup("Move", axis: .vertical, variant: .primary, fullWidth: true) {
                    Button("Move up") {}
                    Button("Move down") {}
                }
            }
        },
        Fixture("full width, not attached and long titles") {
            VStack(alignment: .leading) {
                MonarchButtonGroup("Pages", fullWidth: true) {
                    Button("Previous") {}
                    Button("Next") {}
                }
                MonarchButtonGroup("Pages", isAttached: false) {
                    Button("Previous") {}
                    Button("Next") {}
                }
                MonarchButtonGroup("Pages", axis: .vertical, variant: .danger, isAttached: false, fullWidth: true) {
                    Button("Delete this page") {}
                    Button("Delete every page in the document") {}
                }
                MonarchButtonGroup("Long", variant: .primary) {
                    Button("Go back to the previous page of results") {}
                    Button("Next") {}
                }
            }
        },
    ]
}

@Suite("MonarchButtonGroup")
struct MonarchButtonGroupTests {
    @Test("Joined buttons in a row make one shape with a hairline between neighbours")
    func horizontalGeometry() throws {
        let frames = [CGRect(x: 0, y: 4, width: 50, height: 36), CGRect(x: 50, y: 0, width: 60, height: 44)]
        let geometry = try #require(MonarchButtonGroupTokens.geometry(frames: frames, axis: .horizontal))
        #expect(geometry.outline == CGRect(x: 0, y: 0, width: 110, height: 44))
        // The shorter button is filled to the height of the group.
        #expect(
            geometry.fills == [CGRect(x: 0, y: 0, width: 50, height: 44), CGRect(x: 50, y: 0, width: 60, height: 44)]
        )
        #expect(geometry.dividers == [CGRect(x: 49.5, y: 0, width: 1, height: 44)])
    }

    @Test("Joined buttons in a column are as wide as the group")
    func verticalGeometry() throws {
        let frames = [CGRect(x: 0, y: 0, width: 44, height: 44), CGRect(x: 0, y: 44, width: 44, height: 44)]
        let geometry = try #require(MonarchButtonGroupTokens.geometry(frames: frames, axis: .vertical))
        #expect(geometry.outline == CGRect(x: 0, y: 0, width: 44, height: 88))
        #expect(geometry.fills == frames)
        #expect(geometry.dividers == [CGRect(x: 0, y: 43.5, width: 44, height: 1)])
    }

    @Test("The lines sit between neighbours whatever order the frames arrive in")
    func geometryOrder() throws {
        // Right to left, the first button is the rightmost one.
        let frames = [
            CGRect(x: 100, y: 0, width: 50, height: 36),
            CGRect(x: 50, y: 0, width: 50, height: 36),
            CGRect(x: 0, y: 0, width: 50, height: 36),
        ]
        let geometry = try #require(MonarchButtonGroupTokens.geometry(frames: frames, axis: .horizontal))
        #expect(geometry.dividers.map(\.midX) == [50, 100])
        #expect(geometry.fills.map(\.minX) == [100, 50, 0])
    }

    @Test("One button has no line and no buttons have no shape")
    func geometryEdges() throws {
        let single = try #require(
            MonarchButtonGroupTokens.geometry(frames: [CGRect(x: 0, y: 0, width: 80, height: 36)], axis: .horizontal)
        )
        #expect(single.dividers.isEmpty)
        #expect(MonarchButtonGroupTokens.geometry(frames: [], axis: .horizontal) == nil)
    }

    @Test("Only the secondary variant has a border, and every variant has a line")
    func roles() {
        #expect(MonarchButtonGroupTokens.border(variant: .secondary) == \MonarchColorTokens.borderDefault)
        #expect(MonarchButtonGroupTokens.border(variant: .primary) == nil)
        #expect(MonarchButtonGroupTokens.border(variant: .danger) == nil)
        #expect(MonarchButtonGroupTokens.border(variant: .ghost) == nil)
        #expect(MonarchButtonGroupTokens.divider(variant: .secondary) == \MonarchColorTokens.borderDefault)
        // A line in the surface colour keeps two filled buttons apart.
        #expect(MonarchButtonGroupTokens.divider(variant: .primary) == \MonarchColorTokens.bgSurface)
        #expect(MonarchButtonGroupTokens.divider(variant: .danger) == \MonarchColorTokens.bgSurface)
        #expect(MonarchButtonGroupTokens.divider(variant: .ghost) == \MonarchColorTokens.borderSubtle)
    }

    @Test("Joined buttons are full touch targets", arguments: MonarchButtonSize.allCases)
    func touchTarget(size: MonarchButtonSize) {
        // In a row the visible height is the button's; the touch target is added around it.
        #expect(
            MonarchButtonGroupTokens.minSegmentHeight(size, axis: .horizontal) == MonarchButtonTokens.minHeight(size)
        )
        // In a column the buttons touch, so the visible height is the touch target.
        #expect(
            MonarchButtonGroupTokens.minSegmentHeight(size, axis: .vertical) >= MonarchAccessibility.minimumHitTarget
        )
        #expect(MonarchButtonGroupTokens.minSegmentWidth >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("A MonarchButton inside the group takes the group's style, as a standard Button does")
    @MainActor
    func monarchButtonsDefer() {
        let standard = Host.layout(
            MonarchButtonGroup("Pages", size: .lg) {
                Button("Previous") {}
                Button("Next") {}
            }
            .fixedSize()
        )
        let monarch = Host.layout(
            MonarchButtonGroup("Pages", size: .lg) {
                MonarchButton("Previous") {}
                MonarchButton("Next") {}
            }
            .fixedSize()
        )
        #expect(monarch == standard)
    }

    @Test("The joined fill is drawn behind the buttons, on the leading side in both directions")
    @MainActor
    func fillFollowsTheButtons() throws {
        let light = try #require(ThemeCase.all.first { $0.id == "light" })
        let group = MonarchButtonGroup("Pages", variant: .primary) {
            Button("One") {}
            Button("Two") {}
        }
        .frame(maxWidth: .infinity, alignment: .leading)

        let accent = MonarchTokens.light.colors.accentSolid
        let leftToRight = try #require(horizontalExtent(of: accent, in: group.rendered(in: light, .standard)))
        let rightToLeft = try #require(horizontalExtent(of: accent, in: group.rendered(in: light, .rightToLeft)))
        #expect(leftToRight.upperBound < 0.5, "left to right, the group is in the left half")
        #expect(rightToLeft.lowerBound > 0.5, "right to left, the group is in the right half")
        // The same shape, mirrored.
        #expect(
            abs((leftToRight.upperBound - leftToRight.lowerBound) - (rightToLeft.upperBound - rightToLeft.lowerBound))
                < 0.02
        )
        #expect(abs(leftToRight.lowerBound - (1 - rightToLeft.upperBound)) < 0.02)
    }

    /// The horizontal range, as fractions of the image width, of the pixels
    /// that have a colour. `nil` when the view does not draw or has none.
    @MainActor
    private func horizontalExtent(of color: MonarchRGBA, in view: some View) -> ClosedRange<Double>? {
        guard let image = Host.image(view), let space = CGColorSpace(name: CGColorSpace.sRGB) else { return nil }
        let width = image.width
        let height = image.height
        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        let drawn = pixels.withUnsafeMutableBytes { buffer -> Bool in
            guard
                let context = CGContext(
                    data: buffer.baseAddress,
                    width: width,
                    height: height,
                    bitsPerComponent: 8,
                    bytesPerRow: width * 4,
                    space: space,
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
                )
            else { return false }
            context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
            return true
        }
        guard drawn else { return nil }
        let wanted = [color.red, color.green, color.blue].map { Int(($0 * 255).rounded()) }
        var columns: [Int] = []
        for column in 0..<width {
            for row in 0..<height {
                let offset = (row * width + column) * 4
                let matches = (0..<3).allSatisfy { abs(Int(pixels[offset + $0]) - wanted[$0]) <= 12 }
                if matches {
                    columns.append(column)
                    break
                }
            }
        }
        guard let first = columns.first, let last = columns.last else { return nil }
        return (Double(first) / Double(width))...(Double(last) / Double(width))
    }
}
