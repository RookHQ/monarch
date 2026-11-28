import AVFoundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchVideoThumbnailFixtures: ComponentFixtures {
    static let id = "video-thumbnail"
    static let fixtures = [
        Fixture("poster, duration, button") {
            MonarchVideoThumbnail("Product tour", image: Image(systemName: "photo"), duration: 65) {}
        },
        Fixture("placeholder without a poster") {
            MonarchVideoThumbnail("Keynote", image: nil, duration: 3725)
        },
        Fixture("no poster URL") {
            MonarchVideoThumbnail("Clip", url: nil)
        },
        Fixture("square, letterboxed, no play mark") {
            MonarchVideoThumbnail(
                "Portrait clip",
                image: Image(systemName: "person.fill"),
                aspectRatio: 1,
                contentMode: .fit,
                showsPlayIcon: false
            )
        },
        Fixture("disabled button, custom spoken name") {
            MonarchVideoThumbnail("Locked", image: Image(systemName: "photo"), playLabel: "Watch the locked video") {}
                .disabled(true)
        },
        Fixture("narrow, empty title") {
            MonarchVideoThumbnail("  ", image: Image(systemName: "photo"), duration: 5).frame(width: 96)
        },
    ]
}

@Suite("MonarchVideoThumbnail")
struct MonarchVideoThumbnailTests {
    private let english = Locale(identifier: "en_US")

    @Test("The badge shows minutes and seconds, and hours from one hour")
    func durationText() {
        #expect(MonarchVideoThumbnailTokens.durationText(5, locale: english) == "0:05")
        #expect(MonarchVideoThumbnailTokens.durationText(65, locale: english) == "1:05")
        #expect(MonarchVideoThumbnailTokens.durationText(64.6, locale: english) == "1:05")
        #expect(MonarchVideoThumbnailTokens.durationText(3599, locale: english) == "59:59")
        #expect(MonarchVideoThumbnailTokens.durationText(3600, locale: english) == "1:00:00")
        #expect(MonarchVideoThumbnailTokens.durationText(3725, locale: english) == "1:02:05")
        #expect(MonarchVideoThumbnailTokens.durationText(-3, locale: english) == "0:00")
        #expect(MonarchVideoThumbnailTokens.durationText(nil, locale: english) == nil)
        #expect(MonarchVideoThumbnailTokens.durationText(.infinity, locale: english) == nil)
        #expect(MonarchVideoThumbnailTokens.durationText(.nan, locale: english) == nil)
    }

    @Test("VoiceOver reads the length in words")
    func spokenDuration() {
        #expect(MonarchVideoThumbnailTokens.spokenDuration(65, locale: english) == "1 minute, 5 seconds")
        #expect(MonarchVideoThumbnailTokens.spokenDuration(3725, locale: english) == "1 hour, 2 minutes, 5 seconds")
        #expect(MonarchVideoThumbnailTokens.spokenDuration(120, locale: english) == "2 minutes")
        #expect(MonarchVideoThumbnailTokens.spokenDuration(nil, locale: english) == nil)
        let german = MonarchVideoThumbnailTokens.spokenDuration(65, locale: Locale(identifier: "de_DE"))
        #expect(german?.contains("Minute") == true)
    }

    @Test("A button is named Play and the title; an image by the title alone")
    func accessibilityLabel() {
        #expect(
            MonarchVideoThumbnailTokens.accessibilityLabel(title: "Product tour", isButton: true, playLabel: nil)
                == "Play Product tour"
        )
        #expect(
            MonarchVideoThumbnailTokens.accessibilityLabel(title: "Product tour", isButton: false, playLabel: nil)
                == "Product tour"
        )
        #expect(
            MonarchVideoThumbnailTokens.accessibilityLabel(title: "Tour", isButton: true, playLabel: "Watch the tour")
                == "Watch the tour"
        )
        #expect(
            MonarchVideoThumbnailTokens.accessibilityLabel(title: "Tour", isButton: false, playLabel: "Watch") == "Tour"
        )
    }

    @Test("An empty title falls back to the built-in name")
    func fallbackTitle() {
        #expect(MonarchVideoThumbnailTokens.name(title: "") == "Video")
        #expect(MonarchVideoThumbnailTokens.name(title: " \n ") == "Video")
        #expect(MonarchVideoThumbnailTokens.name(title: "  Demo ") == "Demo")
        #expect(
            MonarchVideoThumbnailTokens.accessibilityLabel(title: "", isButton: true, playLabel: nil) == "Play Video"
        )
    }

    @Test("The aspect ratio is 16 to 9 unless a positive one is given")
    func aspectRatio() {
        #expect(abs(MonarchVideoThumbnailTokens.defaultAspectRatio - 1.777_777) < 0.000_001)
        #expect(MonarchVideoThumbnailTokens.aspectRatio(nil) == MonarchVideoThumbnailTokens.defaultAspectRatio)
        #expect(MonarchVideoThumbnailTokens.aspectRatio(1) == 1)
        #expect(MonarchVideoThumbnailTokens.aspectRatio(0) == MonarchVideoThumbnailTokens.defaultAspectRatio)
        #expect(MonarchVideoThumbnailTokens.aspectRatio(-2) == MonarchVideoThumbnailTokens.defaultAspectRatio)
        #expect(MonarchVideoThumbnailTokens.aspectRatio(.infinity) == MonarchVideoThumbnailTokens.defaultAspectRatio)
    }

    @Test("A frame is captured one second in, earlier in a short video, and never past the end")
    func captureTime() {
        #expect(MonarchVideoThumbnailTokens.captureTime(requested: nil, duration: 120) == 1)
        #expect(MonarchVideoThumbnailTokens.captureTime(requested: nil, duration: 10) == 1)
        #expect(abs(MonarchVideoThumbnailTokens.captureTime(requested: nil, duration: 4) - 0.4) < 0.000_001)
        #expect(MonarchVideoThumbnailTokens.captureTime(requested: nil, duration: 0) == 0)
        #expect(MonarchVideoThumbnailTokens.captureTime(requested: nil, duration: .nan) == 0)
        #expect(MonarchVideoThumbnailTokens.captureTime(requested: 30, duration: 120) == 30)
        #expect(abs(MonarchVideoThumbnailTokens.captureTime(requested: 500, duration: 120) - 119.95) < 0.000_001)
        #expect(MonarchVideoThumbnailTokens.captureTime(requested: -5, duration: 120) == 0)
    }

    @Test("The poster colours come from the inverse and surface roles")
    func colors() {
        #expect(MonarchVideoThumbnailTokens.background == \MonarchColorTokens.bgInverse)
        #expect(MonarchVideoThumbnailTokens.placeholderGlyph == \MonarchColorTokens.textInverseMuted)
        #expect(MonarchVideoThumbnailTokens.playBackground == \MonarchColorTokens.bgSurface)
        #expect(MonarchVideoThumbnailTokens.playGlyph == \MonarchColorTokens.textDefault)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchVideoThumbnailStrings.fallbackTitle == "Video")
        #expect(MonarchVideoThumbnailStrings.play("Demo") == "Play Demo")
    }

    @Test("A frame is captured from a real video file")
    func capturesFrame() async throws {
        let side = 128
        let url = try await TestVideo.write(side: side, frames: 20, framesPerSecond: 10)
        defer { try? FileManager.default.removeItem(at: url) }
        let frame = try await MonarchVideoThumbnailFrame.capture(of: url, at: nil)
        #expect(frame.width == side)
        #expect(frame.height == side)
        let late = try await MonarchVideoThumbnailFrame.capture(of: url, at: 60)
        #expect(late.width == side)
        _ = try await MonarchVideoThumbnail.frame(of: url)
    }

    @Test("Capturing from a missing file throws")
    func captureFails() async {
        let missing = FileManager.default.temporaryDirectory.appendingPathComponent("monarch-missing-\(UUID()).mp4")
        await #expect(throws: (any Error).self) {
            _ = try await MonarchVideoThumbnail.frame(of: missing)
        }
    }

    #if os(iOS)
        // The touch target applies on the simulator.
        @Test("A narrow thumbnail button still lays out")
        @MainActor
        func layout() {
            let thumbnail = MonarchVideoThumbnail("Clip", image: nil, duration: 5) {}
            let size = Host.layout(thumbnail)
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

/// Writes a short grey H.264 video to a temporary file.
private enum TestVideo {
    struct WriteError: Error {}

    static func write(side: Int, frames: Int, framesPerSecond: Int32) async throws -> URL {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("monarch-\(UUID().uuidString).mp4")
        let writer = try AVAssetWriter(outputURL: url, fileType: .mp4)
        let input = AVAssetWriterInput(
            mediaType: .video,
            outputSettings: [
                AVVideoCodecKey: AVVideoCodecType.h264,
                AVVideoWidthKey: side,
                AVVideoHeightKey: side,
            ]
        )
        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: input,
            sourcePixelBufferAttributes: [
                kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
                kCVPixelBufferWidthKey as String: side,
                kCVPixelBufferHeightKey as String: side,
            ]
        )
        writer.add(input)
        guard writer.startWriting() else { throw writer.error ?? WriteError() }
        writer.startSession(atSourceTime: .zero)
        for frame in 0..<frames {
            while !input.isReadyForMoreMediaData {
                try await Task.sleep(for: .milliseconds(5))
            }
            var created: CVPixelBuffer?
            CVPixelBufferCreate(nil, side, side, kCVPixelFormatType_32ARGB, nil, &created)
            guard let buffer = created else { throw WriteError() }
            CVPixelBufferLockBaseAddress(buffer, [])
            memset(CVPixelBufferGetBaseAddress(buffer), 0x80, CVPixelBufferGetDataSize(buffer))
            CVPixelBufferUnlockBaseAddress(buffer, [])
            let time = CMTime(value: CMTimeValue(frame), timescale: framesPerSecond)
            guard adaptor.append(buffer, withPresentationTime: time) else { throw writer.error ?? WriteError() }
        }
        input.markAsFinished()
        await writer.finishWriting()
        guard writer.status == .completed else { throw writer.error ?? WriteError() }
        return url
    }
}
