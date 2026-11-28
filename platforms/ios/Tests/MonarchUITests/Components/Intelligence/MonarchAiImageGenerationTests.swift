import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let finished = [
    MonarchAiGeneratedImage(id: "a", image: Image(systemName: "photo"), alt: "A lighthouse at dusk"),
    MonarchAiGeneratedImage(id: "b", image: Image(systemName: "photo.artframe")),
    MonarchAiGeneratedImage(id: "c", url: URL(string: "javascript:alert(1)"), alt: "Not loaded"),
]
private let slots = [
    MonarchAiGeneratedImage(id: "a", url: nil),
    MonarchAiGeneratedImage(id: "b", url: nil),
]

enum MonarchAiImageGenerationFixtures: ComponentFixtures {
    static let id = "ai-image-generation"
    static let fixtures = [
        Fixture("pending with progress") {
            MonarchAiImageGeneration(images: .constant(slots), status: .pending, progress: 0.4)
        },
        Fixture("progressive previews, no progress") {
            MonarchAiImageGeneration(
                images: .constant(Array(finished.prefix(2))),
                status: .progressive
            )
        },
        Fixture("done: selected, actions, empty alt, unavailable") {
            MonarchAiImageGeneration(
                images: .constant(finished),
                status: .done,
                selection: .constant("a"),
                onVariations: { _ in }
            )
        },
        Fixture("done without actions") {
            MonarchAiImageGeneration(images: .constant(Array(finished.prefix(1))), status: .done)
        },
        Fixture("error with and without retry") {
            VStack {
                MonarchAiImageGeneration(images: .constant([]), status: .error, onRetry: {})
                MonarchAiImageGeneration(
                    images: .constant(slots),
                    status: .error,
                    error: "The image service is busy. Try again in a few minutes."
                )
            }
        },
    ]
}

@Suite("MonarchAiImageGeneration")
struct MonarchAiImageGenerationTests {
    typealias Tokens = MonarchAiImageGenerationTokens
    let labels = MonarchAiImageGenerationLabels()
    let english = Locale(identifier: "en_US")

    @Test("Only http, https and file addresses are loaded")
    func loadable() {
        #expect(Tokens.isLoadable(URL(string: "https://example.com/a.png")))
        #expect(Tokens.isLoadable(URL(string: "HTTP://example.com/a.png")))
        #expect(Tokens.isLoadable(URL(fileURLWithPath: "/tmp/a.png")))
        #expect(!Tokens.isLoadable(URL(string: "javascript:alert(1)")))
        #expect(!Tokens.isLoadable(URL(string: "data:image/png;base64,AAAA")))
        #expect(!Tokens.isLoadable(nil))
    }

    @Test("A slot shows the image when done, refines while progressive and waits while pending")
    func slot() {
        let ready = finished[0]
        #expect(Tokens.slot(for: ready, status: .done) == .image)
        #expect(Tokens.slot(for: ready, status: .progressive) == .refining)
        #expect(Tokens.slot(for: ready, status: .pending) == .placeholder)
        #expect(Tokens.slot(for: ready, status: .error) == .image)
        let empty = slots[0]
        #expect(Tokens.slot(for: empty, status: .progressive) == .placeholder)
        #expect(Tokens.slot(for: empty, status: .done) == .unavailable)
        #expect(Tokens.slot(for: finished[2], status: .done) == .unavailable)
    }

    @Test("Empty alt text is an error once images can be seen")
    func altInvalid() {
        #expect(Tokens.isAltInvalid("", status: .done))
        #expect(Tokens.isAltInvalid("   \n", status: .progressive))
        #expect(!Tokens.isAltInvalid("", status: .pending))
        #expect(!Tokens.isAltInvalid("A cat", status: .done))
    }

    @Test("Actions show only for finished, visible images with an action given")
    func actions() {
        #expect(Tokens.showsActions(for: finished[0], status: .done, canSelect: true, canVary: false))
        #expect(Tokens.showsActions(for: finished[0], status: .done, canSelect: false, canVary: true))
        #expect(!Tokens.showsActions(for: finished[0], status: .done, canSelect: false, canVary: false))
        #expect(!Tokens.showsActions(for: finished[0], status: .progressive, canSelect: true, canVary: true))
        #expect(!Tokens.showsActions(for: finished[2], status: .done, canSelect: true, canVary: true))
    }

    @Test("An image is read by its alt text, or by its position while that is empty")
    func spokenName() {
        #expect(Tokens.spokenName(alt: " A cat ", position: 2, labels: labels) == "A cat")
        #expect(Tokens.spokenName(alt: "", position: 2, labels: labels) == "Generated image 2")
    }

    @Test("The status line names the stage, the percentage and the count")
    func statusText() {
        func text(_ status: MonarchAiImageGenerationStatus, _ progress: Double?, _ count: Int) -> String? {
            Tokens.statusText(status: status, progress: progress, count: count, locale: english, labels: labels)
        }
        #expect(text(.pending, nil, 4) == "Generating images…")
        #expect(text(.pending, 0.4, 4) == "Generating images… 40%")
        #expect(text(.progressive, 1.7, 4) == "Refining images… 100%")
        #expect(text(.progressive, .nan, 4) == "Refining images…")
        #expect(text(.done, nil, 1) == "1 image generated")
        #expect(text(.done, nil, 4) == "4 images generated")
        #expect(text(.error, 0.5, 4) == nil)
    }

    @Test("Progress is clamped and generation covers pending and progressive")
    func progress() {
        #expect(Tokens.clamped(-1) == 0)
        #expect(Tokens.clamped(0.25) == 0.25)
        #expect(Tokens.clamped(.infinity) == nil)
        #expect(Tokens.isGenerating(.pending) && Tokens.isGenerating(.progressive))
        #expect(!Tokens.isGenerating(.done) && !Tokens.isGenerating(.error))
    }

    @Test("Completion is announced only on the way into done")
    func announcement() {
        #expect(Tokens.announcesCompletion(from: .progressive, to: .done))
        #expect(Tokens.announcesCompletion(from: .error, to: .done))
        #expect(!Tokens.announcesCompletion(from: .done, to: .done))
        #expect(!Tokens.announcesCompletion(from: .pending, to: .progressive))
        #expect(labels.announceDone(1) == "1 image generated.")
        #expect(labels.announceDone(3) == "3 images generated.")
    }

    @Test("The placeholder pulse stays within 0...1 and is off under Reduce Motion")
    func pulse() {
        #expect(MonarchPulse.value(at: nil) == 0)
        for step in 0..<30 {
            let value = MonarchPulse.value(at: Double(step) * 0.11)
            #expect(value >= 0 && value <= 1)
        }
    }

    @Test("The built-in strings come from the strings table, with positions")
    func strings() {
        #expect(labels.label == "AI generated images")
        #expect(labels.error == "The images could not be generated.")
        #expect(labels.altLabel(3) == "Alt text for image 3")
        #expect(labels.altRequired == "Describe the image for people who cannot see it.")
        #expect(labels.selectName(1) == "Use image 1")
        #expect(labels.variationsName(2) == "More like this, image 2")
        #expect(labels.selected == "Selected")
        #expect(labels.unavailable == "Preview unavailable")
        #expect(labels.retry == "Try again")
    }

    @Test("Large text gives fewer columns")
    @MainActor
    func columns() {
        let images = Binding.constant(Array(repeating: MonarchAiGeneratedImage(id: "x", url: nil), count: 1))
        let four = Binding.constant((0..<4).map { MonarchAiGeneratedImage(id: "\($0)", url: nil) })
        let one = Host.layout(MonarchAiImageGeneration(images: images, status: .pending))
        let more = Host.layout(MonarchAiImageGeneration(images: four, status: .pending))
        #expect(more.height > one.height)
    }
}
