import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchScreenShareViewFixtures: ComponentFixtures {
    static let id = "screen-share-view"
    static let fixtures = [
        Fixture("remote screen with every control") {
            MonarchScreenShareView(
                "Ana Flores",
                onPictureInPicture: {},
                onFullScreen: {},
                video: { Image(systemName: "macwindow").resizable().scaledToFit() }
            )
        },
        Fixture("zoomed, picture in picture active, full screen") {
            MonarchScreenShareView(
                "Ana Flores",
                zoom: .constant(2),
                isPictureInPictureActive: true,
                isFullScreen: true,
                onPictureInPicture: {},
                onFullScreen: {},
                video: { Image(systemName: "macwindow").resizable().scaledToFit() }
            )
        },
        Fixture("no controls") {
            MonarchScreenShareView("Ana Flores", showsControls: false) {
                Image(systemName: "macwindow").resizable().scaledToFit()
            }
        },
        Fixture("waiting") {
            MonarchScreenShareView("Ana Flores", isWaiting: true) { EmptyView() }
        },
        Fixture("local, preview hidden, stop sharing") {
            MonarchScreenShareView(
                "Ana Flores",
                isLocal: true,
                onStopSharing: {},
                video: { Image(systemName: "macwindow").resizable().scaledToFit() }
            )
        },
        Fixture("long name and custom strings") {
            MonarchScreenShareView(
                "Maximilian Alexander von Hohenberg-Lichtenstein",
                labels: MonarchScreenShareViewLabels(fit: "Einpassen")
            ) {
                Image(systemName: "macwindow").resizable().scaledToFit()
            }
        },
    ]
}

@Suite("MonarchScreenShareView")
@MainActor
struct MonarchScreenShareViewTests {
    typealias Tokens = MonarchScreenShareViewTokens

    private let labels = MonarchScreenShareViewLabels()
    private let english = Locale(identifier: "en_US")

    @Test("Zoom is limited to fit through three times")
    func clamped() {
        #expect(Tokens.clamped(0.2) == 1)
        #expect(Tokens.clamped(1.7) == 1.7)
        #expect(Tokens.clamped(9) == 3)
        #expect(Tokens.clamped(.nan) == 1)
        #expect(Tokens.zoomLevels.first == Tokens.fitZoom)
        #expect(Tokens.zoomLevels.last == Tokens.maximumZoom)
        #expect(Tokens.zoomLevels == Tokens.zoomLevels.sorted())
    }

    @Test("The buttons step through the zoom levels, also from a pinched zoom")
    func steps() {
        #expect(Tokens.stepIn(from: 1) == 1.5)
        #expect(Tokens.stepIn(from: 1.5) == 2)
        #expect(Tokens.stepIn(from: 1.7) == 2)
        #expect(Tokens.stepIn(from: 3) == nil)
        #expect(Tokens.stepOut(from: 3) == 2)
        #expect(Tokens.stepOut(from: 1.7) == 1.5)
        #expect(Tokens.stepOut(from: 1.5) == 1)
        #expect(Tokens.stepOut(from: 1) == nil)
    }

    @Test("A double tap zooms in from fit and back to fit from any zoom")
    func doubleTap() {
        #expect(Tokens.toggled(from: 1) == 2)
        #expect(Tokens.toggled(from: 2) == 1)
        #expect(Tokens.toggled(from: 1.3) == 1)
        #expect(!Tokens.isZoomed(1))
        #expect(Tokens.isZoomed(1.01))
    }

    @Test("The zoom level is written as a percentage")
    func percentText() {
        #expect(Tokens.percentText(1, locale: english) == "100%")
        #expect(Tokens.percentText(1.5, locale: english) == "150%")
        #expect(Tokens.percentText(7, locale: english) == "300%")
    }

    @Test("The presenter line names the presenter, or says You are presenting")
    func presenterText() {
        #expect(Tokens.presenterText(name: "Ana", isLocal: false, labels: labels) == "Ana is presenting")
        #expect(Tokens.presenterText(name: "Ana", isLocal: true, labels: labels) == "You are presenting")
        #expect(Tokens.regionText(name: "Ana", labels: labels) == "Screen shared by Ana")
        let custom = MonarchScreenShareViewLabels(presenting: "Ana teilt", region: "Bildschirm von Ana")
        #expect(Tokens.presenterText(name: "Ana", isLocal: false, labels: custom) == "Ana teilt")
        #expect(Tokens.regionText(name: "Ana", labels: custom) == "Bildschirm von Ana")
    }

    @Test("The picture-in-picture and full-screen buttons change name and symbol with their state")
    func toggles() {
        #expect(Tokens.pictureInPictureLabel(isActive: false, labels: labels) == "Picture in picture")
        #expect(Tokens.pictureInPictureLabel(isActive: true, labels: labels) == "Exit picture in picture")
        #expect(Tokens.fullScreenLabel(isFullScreen: false, labels: labels) == "Full screen")
        #expect(Tokens.fullScreenLabel(isFullScreen: true, labels: labels) == "Exit full screen")
        #expect(Tokens.pictureInPictureSymbol(isActive: true) != Tokens.pictureInPictureSymbol(isActive: false))
        #expect(Tokens.fullScreenSymbol(isFullScreen: true) != Tokens.fullScreenSymbol(isFullScreen: false))
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchScreenShareViewStrings.controls == "Shared screen controls")
        #expect(MonarchScreenShareViewStrings.waiting == "Waiting for the shared screen…")
        #expect(MonarchScreenShareViewStrings.localPreviewHidden.hasPrefix("Your screen is being shared."))
        #expect(MonarchScreenShareViewStrings.showPreview == "Show preview")
        #expect(MonarchScreenShareViewStrings.fit == "Fit to view")
        #expect(MonarchScreenShareViewStrings.zoomIn == "Zoom in")
        #expect(MonarchScreenShareViewStrings.zoomOut == "Zoom out")
        #expect(MonarchScreenShareViewStrings.stopSharing == "Stop sharing")
    }

    @Test("The picture area keeps its 16 by 9 shape, with or without a picture")
    func layout() {
        let picture = Host.layout(
            MonarchScreenShareView("Ana", showsControls: false) { Rectangle() }
        )
        let waiting = Host.layout(
            MonarchScreenShareView("Ana", isWaiting: true, showsControls: false) { EmptyView() }
        )
        let pictureHeight = Host.width / Tokens.aspectRatio
        #expect(picture.height > pictureHeight)
        #expect(abs(picture.height - waiting.height) < 1)
    }
}
