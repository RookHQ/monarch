import Foundation
import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct RemoveError: Error {}

struct MonarchDeviceListSection: CataloguePage {
    static let title = "Device list"

    @State private var devices = [
        MonarchDevice(
            id: "mac",
            name: "Work laptop",
            os: "macOS 15",
            browser: "Safari",
            location: "Berlin, Germany",
            lastUsed: .now.addingTimeInterval(-60),
            firstSeen: .now.addingTimeInterval(-86_400 * 90),
            isCurrent: true
        ),
        MonarchDevice(
            id: "pixel",
            name: "Pixel 8",
            kind: .mobile,
            os: "Android 15",
            browser: "Chrome",
            location: "Lisbon, Portugal",
            lastUsed: .now.addingTimeInterval(-86_400 * 3),
            isBlocked: true
        ),
        MonarchDevice(
            id: "ipad",
            name: "Kitchen iPad",
            kind: .tablet,
            os: "iPadOS 18",
            lastUsed: .now.addingTimeInterval(-86_400 * 40)
        ),
    ]
    @State private var removalFails = true
    @State private var state = MonarchDeviceListState.ready

    var body: some View {
        CatalogueCaption("The first removal fails, the next one succeeds")
        MonarchDeviceList(
            devices,
            state: state,
            onRetry: { state = .ready },
            onRemove: { id in
                try await Task.sleep(for: .milliseconds(800))
                if removalFails {
                    removalFails = false
                    throw RemoveError()
                }
                devices.removeAll { $0.id == id }
            }
        )

        CatalogueCaption("State")
        Picker("State", selection: $state) {
            Text("Ready").tag(MonarchDeviceListState.ready)
            Text("Loading").tag(MonarchDeviceListState.loading)
            Text("Failed").tag(MonarchDeviceListState.failed)
        }
        .pickerStyle(.segmented)

        CatalogueCaption("Empty")
        MonarchDeviceList([])
    }
}
