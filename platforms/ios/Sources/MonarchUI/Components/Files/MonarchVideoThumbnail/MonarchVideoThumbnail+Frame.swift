// AVFoundation reads the frame: SwiftUI has no API that decodes video.
import AVFoundation
import SwiftUI

extension MonarchVideoThumbnail {
    /// Captures one frame of a video as an image, for a video that has no poster.
    ///
    /// The frame is read with `AVAssetImageGenerator`, off the main actor.
    /// Keep the result in state and pass it to ``init(_:image:duration:aspectRatio:contentMode:showsPlayIcon:playLabel:onPlay:)``:
    ///
    /// ```swift
    /// .task { poster = try? await MonarchVideoThumbnail.frame(of: videoURL) }
    /// ```
    ///
    /// - Parameters:
    ///   - videoURL: A local file or a remote video the system can open.
    ///   - time: The second to capture. By default one second in, or a tenth
    ///     of the length for a video shorter than ten seconds.
    /// - Returns: The frame, upright.
    /// - Throws: The AVFoundation error when the video cannot be opened or decoded.
    public nonisolated static func frame(of videoURL: URL, at time: TimeInterval? = nil) async throws -> Image {
        Image(decorative: try await MonarchVideoThumbnailFrame.capture(of: videoURL, at: time), scale: 1)
    }
}

/// Constants of the frame capture.
enum MonarchVideoThumbnailFrame {
    /// Time units per second: fine enough for every common frame rate.
    static let timescale: CMTimeScale = 600

    /// Reads one frame of a video, upright, at the second chosen by
    /// `MonarchVideoThumbnailTokens.captureTime`.
    static func capture(of videoURL: URL, at time: TimeInterval?) async throws -> CGImage {
        let asset = AVURLAsset(url: videoURL)
        let duration = try await asset.load(.duration).seconds
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        let second = MonarchVideoThumbnailTokens.captureTime(requested: time, duration: duration)
        return try await generator.image(at: CMTime(seconds: second, preferredTimescale: timescale)).image
    }
}
