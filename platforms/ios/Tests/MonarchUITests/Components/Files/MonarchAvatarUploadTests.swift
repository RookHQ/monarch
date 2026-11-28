import CoreGraphics
import Foundation
import ImageIO
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// A test picture: the given halves in two flat colours (red first, then blue).
private func avatarUploadTestImage(width: Int, height: Int, splitVertically: Bool) -> CGImage? {
    guard let space = CGColorSpace(name: CGColorSpace.sRGB),
        let context = CGContext(
            data: nil,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: 0,
            space: space,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )
    else { return nil }
    context.setFillColor(CGColor(srgbRed: 0, green: 0, blue: 1, alpha: 1))
    context.fill(CGRect(x: 0, y: 0, width: width, height: height))
    context.setFillColor(CGColor(srgbRed: 1, green: 0, blue: 0, alpha: 1))
    if splitVertically {
        // The left half is red.
        context.fill(CGRect(x: 0, y: 0, width: width / 2, height: height))
    } else {
        // The top half is red; Core Graphics measures from the bottom.
        context.fill(CGRect(x: 0, y: height / 2, width: width, height: height / 2))
    }
    return context.makeImage()
}

/// The red, green and blue of one pixel, measured from the top-left.
private func avatarUploadPixel(_ image: CGImage, x: Int, y: Int) -> [UInt8] {
    var bytes = [UInt8](repeating: 0, count: image.width * image.height * 4)
    guard let space = CGColorSpace(name: CGColorSpace.sRGB) else { return [] }
    bytes.withUnsafeMutableBytes { buffer in
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
        else { return }
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
    }
    let offset = (y * image.width + x) * 4
    return Array(bytes[offset..<offset + 3])
}

private let avatarUploadWide = avatarUploadTestImage(width: 200, height: 100, splitVertically: true)
private let avatarUploadTall = avatarUploadTestImage(width: 100, height: 200, splitVertically: false)

enum MonarchAvatarUploadFixtures: ComponentFixtures {
    static let id = "avatar-upload"
    static let fixtures = [
        Fixture("no picture") {
            MonarchAvatarUpload(name: "Ada Lovelace", image: nil) { _ in }
        },
        Fixture("picture with remove, square") {
            MonarchAvatarUpload(
                name: "Rook Labs",
                image: avatarUploadWide.map { Image(decorative: $0, scale: 1) },
                shape: .square,
                onRemove: {},
                onSave: { _ in }
            )
        },
        Fixture("URL, disabled, custom labels") {
            MonarchAvatarUpload(
                name: "Grace Hopper",
                url: nil,
                labels: MonarchAvatarUploadLabels(upload: "Add a profile picture for your account"),
                onSave: { _ in }
            )
            .disabled(true)
        },
        Fixture("editor, circle, zoomed") {
            if let image = avatarUploadWide {
                MonarchAvatarUploadEditor(
                    image: image,
                    original: Data(),
                    shape: .circle,
                    outputSize: 512,
                    outputFormat: .png,
                    labels: MonarchAvatarUploadLabels(),
                    viewport: MonarchAvatarUploadViewport(zoom: 2, x: 0.3, y: -0.2),
                    onSave: { _ in },
                    onClose: {}
                )
            }
        },
        Fixture("editor, square, saving") {
            if let image = avatarUploadTall {
                MonarchAvatarUploadEditor(
                    image: image,
                    original: Data(),
                    shape: .square,
                    outputSize: 256,
                    outputFormat: .jpeg(quality: 0.8),
                    labels: MonarchAvatarUploadLabels(),
                    isSaving: true,
                    onSave: { _ in },
                    onClose: {}
                )
            }
        },
        Fixture("editor with an error") {
            if let image = avatarUploadWide {
                MonarchAvatarUploadEditor(
                    image: image,
                    original: Data(),
                    shape: .circle,
                    outputSize: 512,
                    outputFormat: .png,
                    labels: MonarchAvatarUploadLabels(),
                    errorText: "The photo could not be saved. Try again.",
                    onSave: { _ in },
                    onClose: {}
                )
            }
        },
        Fixture("error under the buttons") {
            MonarchAvatarUploadError(text: "Choose a photo smaller than 5 MB.")
        },
    ]
}

@Suite("MonarchAvatarUpload")
@MainActor
struct MonarchAvatarUploadTests {
    private let labels = MonarchAvatarUploadLabels()
    private let wide = CGSize(width: 200, height: 100)

    @Test("The picture covers the area: zoom is kept in range and offsets are bounded")
    func clamp() {
        let clamped = MonarchAvatarUploadTokens.clamp(MonarchAvatarUploadViewport(zoom: 9, x: 9, y: 9), natural: wide)
        #expect(clamped.zoom == 4)
        // At zoom 4 the picture is 8 by 4 units: it can move 3.5 by 1.5.
        #expect(clamped.x == 3.5)
        #expect(clamped.y == 1.5)
        let low = MonarchAvatarUploadTokens.clamp(MonarchAvatarUploadViewport(zoom: 0.2, x: -9, y: 1), natural: wide)
        #expect(low == MonarchAvatarUploadViewport(zoom: 1, x: -0.5, y: 0))
    }

    @Test("The crop is the square the area shows, in the picture's pixels")
    func crop() {
        #expect(
            MonarchAvatarUploadTokens.crop(natural: wide, viewport: MonarchAvatarUploadViewport())
                == MonarchAvatarCrop(x: 50, y: 0, size: 100)
        )
        #expect(
            MonarchAvatarUploadTokens.crop(natural: wide, viewport: MonarchAvatarUploadViewport(zoom: 2, x: 0, y: 0))
                == MonarchAvatarCrop(x: 75, y: 25, size: 50)
        )
        // Moving the picture right shows more of its left side.
        #expect(
            MonarchAvatarUploadTokens.crop(natural: wide, viewport: MonarchAvatarUploadViewport(zoom: 1, x: 0.5, y: 0))
                == MonarchAvatarCrop(x: 0, y: 0, size: 100)
        )
    }

    @Test("Drags move by the share of the area and pinches multiply the zoom")
    func gestures() {
        let start = MonarchAvatarUploadViewport()
        let dragged = MonarchAvatarUploadTokens.dragged(
            start,
            by: CGSize(width: 50, height: 0),
            side: 200,
            natural: wide
        )
        #expect(dragged.x == 0.25)
        #expect(
            MonarchAvatarUploadTokens.dragged(start, by: CGSize(width: 50, height: 0), side: 0, natural: wide) == start
        )
        let pinched = MonarchAvatarUploadTokens.magnified(start, by: 2.5, natural: wide)
        #expect(pinched.zoom == 2.5)
        #expect(MonarchAvatarUploadTokens.magnified(pinched, by: 10, natural: wide).zoom == 4)
    }

    @Test("Steps pan, zoom and reset; Shift moves four times as far")
    func steps() {
        var viewport = MonarchAvatarUploadViewport(zoom: 2)
        viewport = MonarchAvatarUploadTokens.apply(.right, to: viewport, natural: wide)
        #expect(abs(viewport.x - 0.04) < 0.0001)
        viewport = MonarchAvatarUploadTokens.apply(.up, to: viewport, natural: wide, isLarge: true)
        #expect(abs(viewport.y + 0.16) < 0.0001)
        viewport = MonarchAvatarUploadTokens.apply(.zoomIn, to: viewport, natural: wide)
        #expect(abs(viewport.zoom - 2.1) < 0.0001)
        viewport = MonarchAvatarUploadTokens.apply(.reset, to: viewport, natural: wide)
        #expect(viewport == MonarchAvatarUploadViewport())
        #expect(MonarchAvatarUploadTokens.apply(.zoomOut, to: viewport, natural: wide).zoom == 1)
    }

    @Test("Keys map to steps")
    func keys() {
        #expect(MonarchAvatarUploadTokens.move(forKey: .leftArrow) == .left)
        #expect(MonarchAvatarUploadTokens.move(forKey: .downArrow) == .down)
        #expect(MonarchAvatarUploadTokens.move(forKey: "+") == .zoomIn)
        #expect(MonarchAvatarUploadTokens.move(forKey: "=") == .zoomIn)
        #expect(MonarchAvatarUploadTokens.move(forKey: "-") == .zoomOut)
        #expect(MonarchAvatarUploadTokens.move(forKey: .home) == .reset)
        #expect(MonarchAvatarUploadTokens.move(forKey: "a") == nil)
    }

    @Test("Spoken zoom, actions and button titles")
    func spoken() {
        #expect(MonarchAvatarUploadTokens.zoomValue(1.5, labels: labels) == "150%")
        #expect(MonarchAvatarUploadTokens.zoomValue(1.004, labels: labels) == "100%")
        #expect(MonarchAvatarUploadTokens.actionName(.left, labels: labels) == "Move left")
        #expect(MonarchAvatarUploadTokens.actionName(.reset, labels: labels) == "Reset position")
        #expect(MonarchAvatarUploadTokens.actionName(.zoomIn, labels: labels) == nil)
        #expect(MonarchAvatarUploadTokens.pickTitle(hasPicture: false, labels: labels) == "Upload photo")
        #expect(MonarchAvatarUploadTokens.pickTitle(hasPicture: true, labels: labels) == "Change photo")
        let custom = MonarchAvatarUploadLabels(change: "New picture", zoomValue: { "\($0) percent" })
        #expect(MonarchAvatarUploadTokens.pickTitle(hasPicture: true, labels: custom) == "New picture")
        #expect(MonarchAvatarUploadTokens.zoomValue(2, labels: custom) == "200 percent")
    }

    @Test("Files over the limit and unreadable files are refused with a message")
    func validation() {
        #expect(!MonarchAvatarUploadTokens.isTooLarge(byteCount: 10_000_000, maxFileSize: nil))
        #expect(MonarchAvatarUploadTokens.isTooLarge(byteCount: 5_000_001, maxFileSize: 5_000_000))
        #expect(!MonarchAvatarUploadTokens.isTooLarge(byteCount: 5_000_000, maxFileSize: 5_000_000))
        let limit = Int64(5_000_000).formatted(.byteCount(style: .file))
        #expect(
            MonarchAvatarUploadTokens.message(for: .tooLarge, maxFileSize: 5_000_000, labels: labels)
                == "Choose a photo smaller than \(limit)."
        )
        #expect(
            MonarchAvatarUploadTokens.message(for: .unreadable, maxFileSize: nil, labels: labels)
                == "This file can’t be used as a photo."
        )
        #expect(MonarchAvatarUploadImage.decode(Data("not a picture".utf8), maxSide: 100) == nil)
    }

    @Test("The rendered crop holds the kept square, upright")
    func render() throws {
        let wideImage = try #require(avatarUploadWide)
        let tallImage = try #require(avatarUploadTall)
        // The left square of a picture whose left half is red.
        let left = try #require(
            MonarchAvatarUploadImage.render(wideImage, crop: MonarchAvatarCrop(x: 0, y: 0, size: 100), side: 64)
        )
        #expect(left.width == 64 && left.height == 64)
        #expect(avatarUploadPixel(left, x: 32, y: 32) == [255, 0, 0])
        let right = try #require(
            MonarchAvatarUploadImage.render(
                wideImage,
                crop: MonarchAvatarCrop(x: 100, y: 0, size: 100),
                side: 64
            )
        )
        #expect(avatarUploadPixel(right, x: 32, y: 32) == [0, 0, 255])
        // The top square of a picture whose top half is red.
        let top = try #require(
            MonarchAvatarUploadImage.render(tallImage, crop: MonarchAvatarCrop(x: 0, y: 0, size: 100), side: 64)
        )
        #expect(avatarUploadPixel(top, x: 32, y: 32) == [255, 0, 0])
        #expect(
            MonarchAvatarUploadImage.render(wideImage, crop: MonarchAvatarCrop(x: 0, y: 0, size: 0), side: 64)
                == nil
        )
    }

    @Test("The result is encoded at the output size and decodes again")
    func result() throws {
        let wideImage = try #require(avatarUploadWide)
        let png = try #require(
            MonarchAvatarUploadImage.result(
                image: wideImage,
                original: Data([1, 2, 3]),
                viewport: MonarchAvatarUploadViewport(zoom: 1, x: 0.5, y: 0),
                side: 128,
                format: .png
            )
        )
        #expect(png.pixelSize == 128)
        #expect(png.crop == MonarchAvatarCrop(x: 0, y: 0, size: 100))
        #expect(png.original == Data([1, 2, 3]))
        #expect(png.data.starts(with: [0x89, 0x50, 0x4E, 0x47]))
        let decoded = try #require(MonarchAvatarUploadImage.decode(png.data, maxSide: 1024))
        #expect(decoded.width == 128 && decoded.height == 128)
        #expect(avatarUploadPixel(decoded, x: 64, y: 64) == [255, 0, 0])
        let jpeg = try #require(
            MonarchAvatarUploadImage.result(
                image: wideImage,
                original: Data(),
                viewport: MonarchAvatarUploadViewport(),
                side: 64,
                format: .jpeg(quality: 0.9)
            )
        )
        #expect(jpeg.data.starts(with: [0xFF, 0xD8]))
    }

    @Test("Decoding bounds the long side")
    func decodeBounds() throws {
        let wideImage = try #require(avatarUploadWide)
        let png = try #require(MonarchAvatarUploadImage.encode(wideImage, format: .png))
        let small = try #require(MonarchAvatarUploadImage.decode(png, maxSide: 50))
        #expect(small.width == 50 && small.height == 25)
    }

    @Test("Colours come from theme roles")
    func colours() {
        #expect(MonarchAvatarUploadTokens.error == \MonarchColorTokens.dangerStrong)
        #expect(MonarchAvatarUploadTokens.mask == \MonarchColorTokens.bgInverse)
        #expect(MonarchAvatarUploadTokens.cropBackground == \MonarchColorTokens.bgMuted)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchAvatarUploadStrings.upload, MonarchAvatarUploadStrings.change, MonarchAvatarUploadStrings.remove,
            MonarchAvatarUploadStrings.editorTitle, MonarchAvatarUploadStrings.cropArea,
            MonarchAvatarUploadStrings.cropHint, MonarchAvatarUploadStrings.zoom, MonarchAvatarUploadStrings.moveLeft,
            MonarchAvatarUploadStrings.moveRight, MonarchAvatarUploadStrings.moveUp,
            MonarchAvatarUploadStrings.moveDown, MonarchAvatarUploadStrings.reset, MonarchAvatarUploadStrings.save,
            MonarchAvatarUploadStrings.cancel, MonarchAvatarUploadStrings.saveFailed,
            MonarchAvatarUploadStrings.unreadable,
        ]
        for string in all { #expect(!string.hasPrefix("monarch.")) }
    }
}
