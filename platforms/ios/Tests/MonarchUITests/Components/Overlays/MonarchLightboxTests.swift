import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// Items that need no network: SF Symbols stand in for photographs.
@MainActor
private enum Sample {
    static let first = MonarchLightboxItem(
        image: Image(systemName: "photo"),
        alt: "Whiteboard sketch of the checkout flow",
        caption: "Sketch from the planning day"
    )
    static let second = MonarchLightboxItem(image: Image(systemName: "photo.fill"), alt: "Team photo")
    static let third = MonarchLightboxItem(
        image: Image(systemName: "chart.bar"),
        alt: "Bar chart of weekly sign-ups",
        shareURL: URL(string: "https://example.com/chart.png")
    )
    static let missing = MonarchLightboxItem(image: nil, alt: "A picture that was deleted")
    static let video = MonarchLightboxItem(
        url: URL(fileURLWithPath: "/monarch-fixture-does-not-exist.mp4"),
        alt: "Screen recording of the import",
        caption: "A video is played in the system player",
        kind: .video
    )
    static let videoWithoutSource = MonarchLightboxItem(url: nil, alt: "A recording that is missing", kind: .video)
    static let all = [first, second, third]
    static let height: CGFloat = MonarchTokens.Breakpoint.sm

    static func viewer(
        _ items: [MonarchLightboxItem],
        selection: Int = 0,
        isLooping: Bool = false,
        labels: MonarchLightboxLabels = MonarchLightboxLabels()
    ) -> some View {
        MonarchLightbox(items, selection: .constant(selection), isLooping: isLooping, labels: labels, onClose: {})
            .frame(height: height)
    }
}

enum MonarchLightboxFixtures: ComponentFixtures {
    static let id = "lightbox"
    static let fixtures = [
        Fixture("first of three, with caption") {
            Sample.viewer(Sample.all)
        },
        Fixture("middle of three") {
            Sample.viewer(Sample.all, selection: 1)
        },
        Fixture("last of three, with share") {
            Sample.viewer(Sample.all, selection: 2)
        },
        Fixture("looping") {
            Sample.viewer(Sample.all, selection: 2, isLooping: true)
        },
        Fixture("single item") {
            Sample.viewer([Sample.first])
        },
        Fixture("no items") {
            Sample.viewer([])
        },
        Fixture("missing image") {
            Sample.viewer([Sample.missing, Sample.first])
        },
        Fixture("video") {
            Sample.viewer([Sample.video, Sample.first])
        },
        Fixture("video without source") {
            Sample.viewer([Sample.videoWithoutSource])
        },
        Fixture("selection out of range") {
            Sample.viewer(Sample.all, selection: 12)
        },
        Fixture("long caption and custom labels") {
            Sample.viewer(
                [
                    MonarchLightboxItem(
                        image: Image(systemName: "photo"),
                        alt: "Photo",
                        caption: String(repeating: "A caption that goes on for a long time. ", count: 12)
                    ),
                    Sample.second,
                ],
                labels: MonarchLightboxLabels(
                    close: "Schliessen",
                    position: { position, count in "Bild \(position) von \(count)" }
                )
            )
        },
    ]
}

@Suite("MonarchLightbox")
@MainActor
struct MonarchLightboxTests {
    private let labels = MonarchLightboxLabels()

    // MARK: Zoom

    @Test("The largest zoom is never below fit")
    func limit() {
        #expect(MonarchLightboxTokens.limit(4) == 4)
        #expect(MonarchLightboxTokens.limit(0.5) == 1)
        #expect(MonarchLightboxTokens.limit(-3) == 1)
        #expect(MonarchLightboxTokens.limit(.nan) == 1)
    }

    @Test("Zoom is limited to fit through the largest zoom")
    func clamped() {
        #expect(MonarchLightboxTokens.clamped(0.2, maximum: 4) == 1)
        #expect(MonarchLightboxTokens.clamped(2.3, maximum: 4) == 2.3)
        #expect(MonarchLightboxTokens.clamped(9, maximum: 4) == 4)
        #expect(MonarchLightboxTokens.clamped(.nan, maximum: 4) == 1)
        #expect(MonarchLightboxTokens.clamped(3, maximum: 0) == 1)
    }

    @Test("The zoom buttons step by a half and stop at the ends")
    func steps() {
        #expect(MonarchLightboxTokens.zoomedIn(from: 1, maximum: 4) == 1.5)
        #expect(MonarchLightboxTokens.zoomedIn(from: 3.5, maximum: 4) == 4)
        #expect(MonarchLightboxTokens.zoomedIn(from: 3.8, maximum: 4) == 4)
        #expect(MonarchLightboxTokens.zoomedIn(from: 4, maximum: 4) == nil)
        #expect(MonarchLightboxTokens.zoomedIn(from: 1, maximum: 1) == nil)

        #expect(MonarchLightboxTokens.zoomedOut(from: 2, maximum: 4) == 1.5)
        #expect(MonarchLightboxTokens.zoomedOut(from: 1.2, maximum: 4) == 1)
        #expect(MonarchLightboxTokens.zoomedOut(from: 1, maximum: 4) == nil)
    }

    @Test("Stepping in from fit reaches the largest zoom and then stops")
    func stepsReachTheLimit() {
        var zoom = MonarchLightboxTokens.fitZoom
        var presses = 0
        while let next = MonarchLightboxTokens.zoomedIn(from: zoom, maximum: 4), presses < 20 {
            zoom = next
            presses += 1
        }
        #expect(zoom == 4)
        #expect(presses == 6)
    }

    @Test("A double tap toggles between fit and twice the size, within the limit")
    func doubleTap() {
        #expect(MonarchLightboxTokens.toggled(from: 1, maximum: 4) == 2)
        #expect(MonarchLightboxTokens.toggled(from: 2, maximum: 4) == 1)
        #expect(MonarchLightboxTokens.toggled(from: 3.7, maximum: 4) == 1)
        #expect(MonarchLightboxTokens.toggled(from: 1, maximum: 1.5) == 1.5)
        #expect(MonarchLightboxTokens.toggled(from: 1, maximum: 1) == 1)
    }

    @Test("The zoom level is a whole percentage")
    func percent() {
        let locale = Locale(identifier: "en_US")
        #expect(MonarchLightboxTokens.percentText(1, locale: locale) == "100%")
        #expect(MonarchLightboxTokens.percentText(1.5, locale: locale) == "150%")
        #expect(MonarchLightboxTokens.percentText(4, locale: locale) == "400%")
        #expect(MonarchLightboxTokens.percentText(0.3, locale: locale) == "100%")
    }

    @Test("Only an image with a source can be zoomed")
    func zoomable() {
        #expect(MonarchLightboxTokens.isZoomable(Sample.first))
        #expect(
            MonarchLightboxTokens.isZoomable(
                MonarchLightboxItem(url: URL(string: "https://example.com/a.png"), alt: "")
            )
        )
        #expect(!MonarchLightboxTokens.isZoomable(Sample.missing))
        #expect(!MonarchLightboxTokens.isZoomable(MonarchLightboxItem(url: nil, alt: "")))
        #expect(!MonarchLightboxTokens.isZoomable(Sample.video))
        #expect(!MonarchLightboxTokens.isZoomable(nil))
    }

    // MARK: Position

    @Test("An index outside the items shows the nearest item")
    func clampedIndex() {
        #expect(MonarchLightboxTokens.clampedIndex(-4, count: 3) == 0)
        #expect(MonarchLightboxTokens.clampedIndex(1, count: 3) == 1)
        #expect(MonarchLightboxTokens.clampedIndex(12, count: 3) == 2)
        #expect(MonarchLightboxTokens.clampedIndex(5, count: 0) == 0)
    }

    @Test("Without looping, stepping stops at the first and the last item")
    func stepping() {
        #expect(MonarchLightboxTokens.index(from: 0, offset: 1, count: 3, isLooping: false) == 1)
        #expect(MonarchLightboxTokens.index(from: 1, offset: -1, count: 3, isLooping: false) == 0)
        #expect(MonarchLightboxTokens.index(from: 0, offset: -1, count: 3, isLooping: false) == nil)
        #expect(MonarchLightboxTokens.index(from: 2, offset: 1, count: 3, isLooping: false) == nil)
    }

    @Test("With looping, stepping wraps around")
    func looping() {
        #expect(MonarchLightboxTokens.index(from: 2, offset: 1, count: 3, isLooping: true) == 0)
        #expect(MonarchLightboxTokens.index(from: 0, offset: -1, count: 3, isLooping: true) == 2)
        #expect(MonarchLightboxTokens.index(from: 1, offset: 1, count: 3, isLooping: true) == 2)
    }

    @Test("With fewer than two items there is nowhere to step")
    func steppingNeedsTwoItems() {
        for isLooping in [false, true] {
            #expect(MonarchLightboxTokens.index(from: 0, offset: 1, count: 1, isLooping: isLooping) == nil)
            #expect(MonarchLightboxTokens.index(from: 0, offset: -1, count: 0, isLooping: isLooping) == nil)
        }
    }

    @Test("The position counts from 1 and is hidden with fewer than two items")
    func position() {
        #expect(MonarchLightboxTokens.positionText(index: 0, count: 5, labels: labels) == "1 of 5")
        #expect(MonarchLightboxTokens.positionText(index: 4, count: 5, labels: labels) == "5 of 5")
        #expect(MonarchLightboxTokens.positionText(index: 40, count: 5, labels: labels) == "5 of 5")
        #expect(MonarchLightboxTokens.positionText(index: 0, count: 1, labels: labels) == nil)
        #expect(MonarchLightboxTokens.positionText(index: 0, count: 0, labels: labels) == nil)
    }

    @Test("The app's position text replaces the built-in one")
    func positionOverride() {
        let german = MonarchLightboxLabels(position: { position, count in "Bild \(position) von \(count)" })
        #expect(MonarchLightboxTokens.positionText(index: 1, count: 5, labels: german) == "Bild 2 von 5")
    }

    @Test("Moving to another item announces the position and what the item shows")
    func announcement() {
        #expect(
            MonarchLightboxTokens.announcement(index: 1, items: Sample.all, labels: labels) == "2 of 3: Team photo"
        )
        #expect(
            MonarchLightboxTokens.announcement(index: 0, items: [Sample.second], labels: labels) == "Team photo"
        )
        let unnamed = [MonarchLightboxItem(image: nil, alt: ""), Sample.second]
        #expect(MonarchLightboxTokens.announcement(index: 0, items: unnamed, labels: labels) == "1 of 2")
        #expect(
            MonarchLightboxTokens.announcement(index: 0, items: [], labels: labels)
                == MonarchLightboxStrings.unavailable
        )
    }

    // MARK: Items and strings

    @Test("An item made from an image is an image; a URL item keeps its kind")
    func itemKinds() {
        #expect(Sample.first.kind == .image)
        #expect(MonarchLightboxItem(url: nil, alt: "").kind == .image)
        #expect(Sample.video.kind == .video)
        #expect(Sample.first.caption == "Sketch from the planning day")
        #expect(Sample.second.caption == nil)
        #expect(Sample.third.shareURL?.absoluteString == "https://example.com/chart.png")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchLightboxStrings.title == "Media viewer")
        #expect(MonarchLightboxStrings.previous == "Previous")
        #expect(MonarchLightboxStrings.next == "Next")
        #expect(MonarchLightboxStrings.zoomIn == "Zoom in")
        #expect(MonarchLightboxStrings.zoomOut == "Zoom out")
        #expect(MonarchLightboxStrings.resetZoom == "Reset zoom")
        #expect(MonarchLightboxStrings.close == "Close")
        #expect(MonarchLightboxStrings.share == "Share")
        #expect(MonarchLightboxStrings.unavailable == "This file can’t be shown.")
        #expect(MonarchLightboxStrings.loading == "Loading")
        #expect(MonarchLightboxStrings.position(2, 5) == "2 of 5")
    }

    @Test("Every label is nil until the app replaces it")
    func labelsDefaultToBuiltIn() {
        #expect(labels.title == nil && labels.previous == nil && labels.next == nil)
        #expect(labels.zoomIn == nil && labels.zoomOut == nil && labels.resetZoom == nil)
        #expect(labels.close == nil && labels.share == nil && labels.position == nil)
        #expect(labels.unavailable == nil && labels.loading == nil)
        #expect(MonarchLightboxLabels(close: "Fermer").close == "Fermer")
    }

    #if os(iOS)
        @Test("The viewer lays out at the largest text size without losing the stage")
        func largestText() {
            let size = Host.layout(
                Sample.viewer(Sample.all).environment(\.dynamicTypeSize, .accessibility5)
            )
            #expect(size.height >= Sample.height)
        }
    #endif
}
