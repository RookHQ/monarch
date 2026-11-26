import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let now = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let devices = [
    MonarchDevice(
        id: "mac",
        name: "Work laptop",
        os: "macOS 15",
        browser: "Safari",
        location: "Berlin, Germany",
        lastUsed: now.addingTimeInterval(-60),
        firstSeen: now.addingTimeInterval(-86_400 * 90),
        isCurrent: true
    ),
    MonarchDevice(
        id: "pixel",
        name: "Pixel 8",
        kind: .mobile,
        os: "Android 15",
        browser: "Chrome",
        location: "203.0.113.7",
        lastUsed: now.addingTimeInterval(-86_400 * 3),
        isBlocked: true
    ),
    MonarchDevice(id: "ipad", name: "Kitchen iPad with a very long name that wraps", kind: .tablet),
    MonarchDevice(id: "x", name: "", kind: .unknown, lastUsed: now.addingTimeInterval(-3_600)),
]

enum MonarchDeviceListFixtures: ComponentFixtures {
    static let id = "device-list"
    static let fixtures = [
        Fixture("every kind, removable") {
            MonarchDeviceList(devices, now: now, onRemove: { _ in })
        },
        Fixture("read only") {
            MonarchDeviceList(Array(devices.prefix(2)), now: now)
        },
        Fixture("row actions") {
            MonarchDeviceList(
                [devices[1]],
                now: now,
                onRemove: { _ in },
                rowActions: { _ in
                    MonarchButton("Unblock", variant: .secondary, size: .sm) {}
                }
            )
        },
        Fixture("loading") {
            MonarchDeviceList([], state: .loading)
        },
        Fixture("empty") {
            MonarchDeviceList([])
        },
        Fixture("failed, retrying") {
            MonarchDeviceList([], state: .failed, isRetrying: true, onRetry: {})
        },
        Fixture("custom title and labels") {
            MonarchDeviceList(
                [devices[0]],
                title: "Signed-in devices",
                description: "Remove any device you do not recognise.",
                now: now,
                labels: MonarchDeviceListLabels(current: "Current")
            )
        },
    ]
}

@Suite("MonarchDeviceList")
struct MonarchDeviceListTests {
    private let labels = MonarchDeviceListLabels()
    private let locale = Locale(identifier: "en_US")

    @Test("A loaded empty list is the empty state")
    func phase() {
        #expect(MonarchDeviceListTokens.phase(.ready, devices: devices) == .list)
        #expect(MonarchDeviceListTokens.phase(.ready, devices: []) == .empty)
        #expect(MonarchDeviceListTokens.phase(.loading, devices: devices) == .loading)
        #expect(MonarchDeviceListTokens.phase(.failed, devices: []) == .failed)
    }

    @Test("Each kind has its own symbol")
    func symbols() {
        let symbols = MonarchDeviceKind.allCases.map(MonarchDeviceListTokens.symbol)
        #expect(Set(symbols).count == MonarchDeviceKind.allCases.count)
        #expect(MonarchDeviceListTokens.symbol(.mobile) == "smartphone")
    }

    @Test("No symbol depicts an Apple product (legal check)")
    func noAppleProductSymbols() {
        let names =
            MonarchDeviceKind.allCases.map(MonarchDeviceListTokens.symbol) + [MonarchDeviceListTokens.emptySymbol]
        let restricted = ["iphone", "ipad", "ipod", "macbook", "imac", "applewatch", "airpods"]
        for name in names {
            #expect(!restricted.contains { name.contains($0) }, "\(name)")
        }
    }

    @Test("A blocked device has a danger icon and a Blocked badge")
    func blocked() {
        #expect(MonarchDeviceListTokens.iconRole(isBlocked: true) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchDeviceListTokens.iconRole(isBlocked: false) == \MonarchColorTokens.textMuted)
        let badges = MonarchDeviceListTokens.badges(devices[1], labels: labels)
        #expect(badges == [MonarchDeviceListTokens.Badge(text: "Blocked", tone: .danger)])
        let current = MonarchDeviceListTokens.badges(devices[0], labels: labels)
        #expect(current == [MonarchDeviceListTokens.Badge(text: "This device", tone: .success)])
    }

    @Test("A blank name reads Unknown device")
    func names() {
        #expect(MonarchDeviceListTokens.displayName(devices[3], labels: labels) == "Unknown device")
        #expect(MonarchDeviceListTokens.displayName(devices[0], labels: labels) == "Work laptop")
        let custom = MonarchDeviceListLabels(unnamed: "Unnamed")
        #expect(MonarchDeviceListTokens.displayName(devices[3], labels: custom) == "Unnamed")
    }

    @Test("The detail line joins what is present")
    func detail() {
        #expect(MonarchDeviceListTokens.detail(devices[0]) == "macOS 15 · Safari · Berlin, Germany")
        #expect(MonarchDeviceListTokens.detail(devices[2]) == nil)
        let partial = MonarchDevice(id: "p", name: "P", os: " ", browser: "Firefox")
        #expect(MonarchDeviceListTokens.detail(partial) == "Firefox")
    }

    @Test("Times read as relative phrases with their prefix")
    func times() {
        let lines = MonarchDeviceListTokens.times(devices[0], now: now, locale: locale, labels: labels)
        #expect(lines.count == 2)
        #expect(lines[0].hasPrefix("Last used "))
        #expect(lines[1].hasPrefix("First seen "))
        #expect(lines[0].contains("minute"))
        #expect(MonarchDeviceListTokens.times(devices[2], now: now, locale: locale, labels: labels).isEmpty)
    }

    @Test("VoiceOver reads badges, detail and times after the name")
    func accessibilityValue() {
        let value = MonarchDeviceListTokens.accessibilityValue(
            badges: ["Blocked"],
            detail: "Android 15 · Chrome",
            times: ["Last used 3 days ago"]
        )
        #expect(value == "Blocked, Android 15 · Chrome, Last used 3 days ago")
        #expect(MonarchDeviceListTokens.accessibilityValue(badges: [], detail: nil, times: []).isEmpty)
    }

    @Test("The current device cannot be removed")
    func canRemove() {
        #expect(!MonarchDeviceListTokens.canRemove(devices[0], hasHandler: true))
        #expect(MonarchDeviceListTokens.canRemove(devices[1], hasHandler: true))
        #expect(!MonarchDeviceListTokens.canRemove(devices[1], hasHandler: false))
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchDeviceListStrings.title == "Devices")
        #expect(MonarchDeviceListStrings.removeLabel("Pixel 8") == "Remove Pixel 8")
        #expect(MonarchDeviceListStrings.confirmTitle("Pixel 8") == "Remove Pixel 8?")
        #expect(MonarchDeviceListStrings.removed("Pixel 8") == "Removed Pixel 8")
        #expect(MonarchDeviceListStrings.removeFailed("Pixel 8") == "Couldn’t remove Pixel 8. Try again.")
        for value in [
            MonarchDeviceListStrings.description, MonarchDeviceListStrings.listLabel,
            MonarchDeviceListStrings.confirmMessage, MonarchDeviceListStrings.cancel,
            MonarchDeviceListStrings.loading, MonarchDeviceListStrings.emptyTitle,
            MonarchDeviceListStrings.emptyMessage, MonarchDeviceListStrings.errorTitle,
            MonarchDeviceListStrings.lastUsed, MonarchDeviceListStrings.firstSeen,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
