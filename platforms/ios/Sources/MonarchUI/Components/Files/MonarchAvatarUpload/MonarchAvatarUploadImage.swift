import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

/// Decoding, cropping and encoding of ``MonarchAvatarUpload`` with Core
/// Graphics and Image I/O, which are on every Apple platform.
enum MonarchAvatarUploadImage {
    /// The picture in a file, turned upright and no larger than `maxSide`
    /// pixels on its long side, or `nil` when the file is not a picture.
    static func decode(_ data: Data, maxSide: Int) -> CGImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil), CGImageSourceGetCount(source) > 0 else {
            return nil
        }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxSide,
        ]
        return CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
    }

    /// The crop square of `image` drawn into a square of `side` pixels.
    static func render(_ image: CGImage, crop: MonarchAvatarCrop, side: Int) -> CGImage? {
        guard side > 0, crop.size > 0, let space = CGColorSpace(name: CGColorSpace.sRGB),
            let context = CGContext(
                data: nil,
                width: side,
                height: side,
                bitsPerComponent: 8,
                bytesPerRow: 0,
                space: space,
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            )
        else { return nil }
        context.interpolationQuality = .high
        let scale = CGFloat(side) / crop.size
        // Core Graphics measures from the bottom; the crop from the top.
        let bottom = CGFloat(image.height) - crop.y - crop.size
        context.draw(
            image,
            in: CGRect(
                x: -crop.x * scale,
                y: -bottom * scale,
                width: CGFloat(image.width) * scale,
                height: CGFloat(image.height) * scale
            )
        )
        return context.makeImage()
    }

    /// The picture encoded as PNG or JPEG.
    static func encode(_ image: CGImage, format: MonarchAvatarUploadFormat) -> Data? {
        let data = NSMutableData()
        let type: UTType
        var properties: [CFString: Any] = [:]
        switch format {
        case .png:
            type = .png
        case .jpeg(let quality):
            type = .jpeg
            properties[kCGImageDestinationLossyCompressionQuality] = min(1, max(0, quality))
        }
        guard let destination = CGImageDestinationCreateWithData(data, type.identifier as CFString, 1, nil) else {
            return nil
        }
        CGImageDestinationAddImage(destination, image, properties as CFDictionary)
        guard CGImageDestinationFinalize(destination) else { return nil }
        return data as Data
    }

    /// The saved crop: the picture cut, scaled to `side` and encoded.
    static func result(
        image: CGImage,
        original: Data,
        viewport: MonarchAvatarUploadViewport,
        side: Int,
        format: MonarchAvatarUploadFormat
    ) -> MonarchAvatarUploadResult? {
        let natural = CGSize(width: image.width, height: image.height)
        let crop = MonarchAvatarUploadTokens.crop(natural: natural, viewport: viewport)
        guard let rendered = render(image, crop: crop, side: side), let data = encode(rendered, format: format) else {
            return nil
        }
        return MonarchAvatarUploadResult(data: data, format: format, pixelSize: side, crop: crop, original: original)
    }
}
