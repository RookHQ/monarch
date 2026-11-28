import AVFoundation
import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDevicePicker``.
enum MonarchDevicePickerTokens {
    /// The gap between the notice and the fields, and between fields.
    static func spacing(_ size: MonarchDevicePickerSize) -> CGFloat {
        size == .sm ? MonarchTokens.Space.space2 : MonarchTokens.Space.space4
    }

    /// The system control size of the menus.
    static func controlSize(_ size: MonarchDevicePickerSize) -> ControlSize {
        size == .sm ? .small : .regular
    }

    /// The type role of the speaker row's labels.
    static func labelStyle(_ size: MonarchDevicePickerSize) -> MonarchTextStyle {
        size == .sm ? .caption : .label
    }

    /// The gap between the speaker label and its row.
    static let labelSpacing: CGFloat = MonarchTokens.Space.space1
    /// The colour of the speaker label.
    static var labelRole: MonarchColorRole { \.textDefault }
    /// The colour of the current output's name.
    static var outputRole: MonarchColorRole { \.textMuted }
    /// The colour of the route button, and while a route other than the device is active.
    static var routeRole: MonarchColorRole { \.textDefault }
    static var routeActiveRole: MonarchColorRole { \.accentSolid }

    /// What a field chooses.
    enum Kind: Hashable {
        case microphone
        case camera
    }

    /// One choice of a menu: `nil` for the system default.
    struct Option: Hashable {
        let id: String?
        let title: String
    }

    /// The name of a device without one.
    static func unnamed(_ kind: Kind, position: Int, labels: MonarchDevicePickerLabels) -> String {
        switch kind {
        case .microphone:
            labels.unnamedMicrophone?(position) ?? MonarchDevicePickerStrings.unnamedMicrophone(position.formatted())
        case .camera:
            labels.unnamedCamera?(position) ?? MonarchDevicePickerStrings.unnamedCamera(position.formatted())
        }
    }

    /// The choices of a menu: the system default first; a selected device
    /// that is no longer listed stays as an unnamed choice, so the choice is
    /// never silently changed; then each device with an id, unnamed ones
    /// numbered by position.
    static func options(
        _ devices: [MonarchDevicePickerDevice],
        selection: String?,
        kind: Kind,
        labels: MonarchDevicePickerLabels
    ) -> [Option] {
        let listed = devices.filter { !$0.id.isEmpty }
        var options = [Option(id: nil, title: labels.systemDefault ?? MonarchDevicePickerStrings.systemDefault)]
        if let selection, !selection.isEmpty, !listed.contains(where: { $0.id == selection }) {
            options.append(Option(id: selection, title: unnamed(kind, position: 1, labels: labels)))
        }
        for (index, device) in listed.enumerated() {
            options.append(
                Option(
                    id: device.id,
                    title: device.name.isEmpty ? unnamed(kind, position: index + 1, labels: labels) : device.name
                )
            )
        }
        return options
    }

    /// The selection a menu shows: an empty id is the system default.
    static func normalized(_ selection: String?) -> String? {
        guard let selection, !selection.isEmpty else { return nil }
        return selection
    }

    /// The access of the shown kinds together: refused if any is refused,
    /// not asked if any is not asked, else allowed.
    static func combined(_ statuses: [MonarchDevicePickerAccess]) -> MonarchDevicePickerAccess {
        if statuses.contains(.denied) { return .denied }
        if statuses.contains(.notDetermined) { return .notDetermined }
        if statuses.contains(.unavailable) { return .unavailable }
        return .granted
    }

    /// The picker's access for a system authorization status.
    static func access(_ status: AVAuthorizationStatus) -> MonarchDevicePickerAccess {
        switch status {
        case .authorized: .granted
        case .notDetermined: .notDetermined
        case .denied, .restricted: .denied
        @unknown default: .denied
        }
    }

    /// The notice to show: the access state, or unavailable when access is
    /// allowed but no shown field has a device. `nil` for no notice.
    static func notice(access: MonarchDevicePickerAccess, hasDevices: Bool) -> MonarchDevicePickerAccess? {
        switch access {
        case .granted: hasDevices ? nil : .unavailable
        default: access
        }
    }

    /// The tone of a notice.
    static func tone(_ notice: MonarchDevicePickerAccess) -> MonarchTone {
        switch notice {
        case .granted, .unavailable: .neutral
        case .notDetermined: .info
        case .denied: .warning
        }
    }

    /// The message of a notice.
    static func message(_ notice: MonarchDevicePickerAccess, labels: MonarchDevicePickerLabels) -> String {
        switch notice {
        case .granted, .unavailable: labels.unavailable ?? MonarchDevicePickerStrings.unavailable
        case .notDetermined: labels.notDetermined ?? MonarchDevicePickerStrings.notDetermined
        case .denied: labels.denied ?? MonarchDevicePickerStrings.denied
        }
    }

    /// The action of a notice: Allow access before asking, Open Settings
    /// after a refusal when the platform can open them. `nil` for none.
    static func actionTitle(
        _ notice: MonarchDevicePickerAccess,
        canOpenSettings: Bool,
        labels: MonarchDevicePickerLabels
    ) -> String? {
        switch notice {
        case .notDetermined: labels.allowAccess ?? MonarchDevicePickerStrings.allowAccess
        case .denied: canOpenSettings ? labels.openSettings ?? MonarchDevicePickerStrings.openSettings : nil
        case .granted, .unavailable: nil
        }
    }

    /// The name of the current audio output: the route's outputs joined, or
    /// the system default text when the route is unknown.
    static func outputName(_ portNames: [String], fallback: String) -> String {
        let names = portNames.filter { !$0.isEmpty }
        return names.isEmpty ? fallback : names.joined(separator: ", ")
    }
}
