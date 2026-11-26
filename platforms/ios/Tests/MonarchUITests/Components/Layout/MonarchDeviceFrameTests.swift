import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchDeviceFrameFixtures: ComponentFixtures {
    static let id = "device-frame"
    static let fixtures = [
        Fixture("phone with a status bar") {
            MonarchDeviceFrame("Preview of your link page on a phone", statusBarTime: "9:41") {
                VStack(alignment: .leading) {
                    Text("Ada Lovelace").monarchTextStyle(.heading)
                    Text("Links, writing and talks").monarchTextStyle(.body)
                }
                .padding()
            }
        },
        Fixture("tablet") {
            MonarchDeviceFrame("Preview of the email on a tablet", device: .tablet) {
                Text("Welcome to the team").monarchTextStyle(.title).padding()
            }
        },
        Fixture("flat, interactive, custom size") {
            MonarchDeviceFrame(
                "Live preview",
                screenSize: CGSize(width: 240, height: 320),
                statusBarTime: "12:00",
                isInteractive: true,
                isFlat: true
            ) {
                VStack {
                    ForEach(0..<12, id: \.self) { index in
                        Text("Row \(index)").frame(maxWidth: .infinity, alignment: .leading).padding()
                    }
                }
            }
        },
        Fixture("empty screen, side by side") {
            HStack(alignment: .top) {
                MonarchDeviceFrame("Empty phone") { EmptyView() }
                MonarchDeviceFrame("Empty tablet", device: .tablet, isFlat: true) { EmptyView() }
            }
        },
    ]
}

@Suite("MonarchDeviceFrame")
struct MonarchDeviceFrameTests {
    typealias Tokens = MonarchDeviceFrameTokens

    @Test("Each device has its own screen size, bezel and corner radius")
    func devices() {
        #expect(Tokens.screenSize(.phone) == CGSize(width: 390, height: 844))
        #expect(Tokens.screenSize(.tablet) == CGSize(width: 820, height: 1180))
        #expect(Tokens.bezel(.phone) == 10)
        #expect(Tokens.bezel(.tablet) == 14)
        #expect(Tokens.cornerRadius(.phone) == 44)
        #expect(Tokens.cornerRadius(.tablet) == 28)
    }

    @Test("The screen corner is concentric with the frame", arguments: MonarchDeviceFrameDevice.allCases)
    func screenCorner(device: MonarchDeviceFrameDevice) {
        #expect(Tokens.screenCornerRadius(device) == Tokens.cornerRadius(device) - Tokens.bezel(device))
        #expect(Tokens.screenCornerRadius(device) > 0)
    }

    @Test("A size that cannot be drawn falls back to the device's own")
    func resolvedScreenSize() {
        let custom = CGSize(width: 320, height: 568)
        #expect(Tokens.resolvedScreenSize(custom, device: .phone) == custom)
        #expect(Tokens.resolvedScreenSize(nil, device: .tablet) == Tokens.screenSize(.tablet))
        #expect(Tokens.resolvedScreenSize(.zero, device: .phone) == Tokens.screenSize(.phone))
        #expect(Tokens.resolvedScreenSize(CGSize(width: -1, height: 10), device: .phone) == Tokens.screenSize(.phone))
        #expect(
            Tokens.resolvedScreenSize(CGSize(width: 100, height: CGFloat.infinity), device: .phone)
                == Tokens.screenSize(.phone)
        )
        #expect(
            Tokens.resolvedScreenSize(CGSize(width: CGFloat.nan, height: 10), device: .phone)
                == Tokens.screenSize(.phone)
        )
    }

    @Test("Only the phone has an island")
    func island() {
        #expect(Tokens.showsIsland(.phone))
        #expect(!Tokens.showsIsland(.tablet))
        #expect(Tokens.islandMaxWidth == 120)
        #expect(Tokens.islandHeight == 22)
    }

    @Test("A phone has three leading buttons and one trailing; a tablet one trailing; a flat frame none")
    func sideButtons() {
        let phone = Tokens.sideButtons(device: .phone, isFlat: false)
        #expect(phone.filter(\.isLeading).count == 3)
        #expect(phone.filter { !$0.isLeading }.count == 1)
        let tablet = Tokens.sideButtons(device: .tablet, isFlat: false)
        #expect(tablet.count == 1 && tablet.allSatisfy { !$0.isLeading })
        #expect(Tokens.sideButtons(device: .phone, isFlat: true).isEmpty)
        #expect(Tokens.sideButtons(device: .tablet, isFlat: true).isEmpty)
        for button in phone + tablet {
            #expect(button.top > 0 && button.top + button.height < 1)
        }
        // Buttons on the same edge do not overlap.
        let leading = phone.filter(\.isLeading).sorted { $0.top < $1.top }
        for (above, below) in zip(leading, leading.dropFirst()) {
            #expect(above.top + above.height <= below.top)
        }
    }

    @Test("A flat frame has no shadow")
    func elevation() {
        #expect(Tokens.elevation(isFlat: false) == .xl)
        #expect(Tokens.elevation(isFlat: true) == MonarchElevationLevel.none)
        #expect(Tokens.bezelColor == \MonarchColorTokens.bgInverse)
        #expect(Tokens.screenColor == \MonarchColorTokens.bgSurface)
    }

    @Test("The frame keeps the device's aspect ratio at the width it is given")
    @MainActor
    func aspectRatio() {
        let size = Host.layout(MonarchDeviceFrame("Phone") { EmptyView() })
        #expect(abs(size.width - Host.width) < 0.5)
        #expect(abs(size.height - Host.width * 844 / 390) < 1)

        let tablet = Host.layout(MonarchDeviceFrame("Tablet", device: .tablet) { EmptyView() })
        #expect(abs(tablet.height - Host.width * 1180 / 820) < 1)
    }

    @Test("The frame is never wider than the screen size, whatever the content")
    @MainActor
    func maxWidth() {
        let small = CGSize(width: 200, height: 300)
        let size = Host.layout(
            MonarchDeviceFrame("Small", screenSize: small) {
                Text(String(repeating: "Wide content. ", count: 40))
            }
            .fixedSize(horizontal: false, vertical: true)
        )
        #expect(abs(size.height - small.height) < 1)
    }
}
