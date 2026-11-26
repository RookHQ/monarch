import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct SaveFailure: LocalizedError {
    var errorDescription: String? { "The server is offline." }
}

private func rowState(_ build: (inout MonarchSwitchSettingsRowState) -> Void) -> MonarchSwitchSettingsRowState {
    var state = MonarchSwitchSettingsRowState()
    build(&state)
    return state
}

enum MonarchSwitchSettingsListFixtures: ComponentFixtures {
    static let id = "switch-settings-list"
    static let fixtures = [
        Fixture("titled list, idle rows") {
            MonarchSwitchSettingsList("Email notifications", description: "Choose what we email you about.") {
                MonarchSwitchSettingsRow("Weekly summary", isOn: .constant(true), description: "Sent on Mondays.") {
                    _ in
                }
                MonarchSwitchSettingsRow("Product news", isOn: .constant(false)) { _ in }
            }
        },
        Fixture("saving, saved and failed rows") {
            MonarchSwitchSettingsList("Privacy") {
                MonarchSwitchSettingsRow(
                    "Share usage data",
                    isOn: .constant(false),
                    state: rowState { _ = $0.begin(true, isEnabled: true) }
                ) { _ in }
                MonarchSwitchSettingsRow(
                    "Personalised tips",
                    isOn: .constant(true),
                    state: rowState {
                        _ = $0.begin(true, isEnabled: true)
                        $0.succeed()
                    }
                ) { _ in }
                MonarchSwitchSettingsRow(
                    "Show my profile in search results of other workspaces",
                    isOn: .constant(false),
                    description: "People outside your workspace can find you by name.",
                    state: rowState {
                        _ = $0.begin(true, isEnabled: true)
                        $0.fail(true, message: "The server is offline.")
                    }
                ) { _ in }
            }
        },
        Fixture("disabled row, no title") {
            MonarchSwitchSettingsList {
                MonarchSwitchSettingsRow("Security alerts", isOn: .constant(true), description: "Always on.") { _ in }
                    .disabled(true)
            }
        },
        Fixture("rows in a plain stack") {
            VStack {
                MonarchSwitchSettingsRow("Outside a list", isOn: .constant(true)) { _ in }
            }
        },
    ]
}

@Suite("MonarchSwitchSettingsList")
struct MonarchSwitchSettingsListTests {
    private let labels = MonarchSwitchSettingsListLabels()

    @Test("A toggle shows the new value at once and keeps it after a save")
    func optimisticSave() {
        var state = MonarchSwitchSettingsRowState()
        #expect(state.shown(saved: false) == false)
        let started1 = state.begin(true, isEnabled: true)

        #expect(started1)
        #expect(state.status == .saving)
        #expect(state.shown(saved: false) == true)
        state.succeed()
        #expect(state.status == .saved)
        #expect(state.shown(saved: true) == true)
        state.clearSaved(generation: state.generation)
        #expect(state.status == .idle)
    }

    @Test("Toggles during a save and on a disabled row are ignored")
    func ignoredToggles() {
        var state = MonarchSwitchSettingsRowState()
        let started2 = state.begin(true, isEnabled: true)

        #expect(started2)
        let started3 = state.begin(false, isEnabled: true)

        #expect(!started3)
        #expect(state.shown(saved: false) == true)
        var disabled = MonarchSwitchSettingsRowState()
        let started4 = disabled.begin(true, isEnabled: false)

        #expect(!started4)
        #expect(disabled.status == .idle)
    }

    @Test("A failed save rolls back and keeps the value to retry")
    func failure() {
        var state = MonarchSwitchSettingsRowState()
        _ = state.begin(true, isEnabled: true)
        state.fail(true, message: "Offline")
        #expect(state.status == .error)
        #expect(state.shown(saved: false) == false)
        #expect(state.retryValue == true)
        #expect(state.message == "Offline")
        let started5 = state.begin(true, isEnabled: true)

        #expect(started5)
        #expect(state.retryValue == nil)
        #expect(state.message == nil)
    }

    @Test("A late timer does not clear a newer Saved")
    func staleTimer() {
        var state = MonarchSwitchSettingsRowState()
        _ = state.begin(true, isEnabled: true)
        state.succeed()
        let first = state.generation
        _ = state.begin(false, isEnabled: true)
        state.succeed()
        state.clearSaved(generation: first)
        #expect(state.status == .saved)
        state.clearSaved(generation: state.generation)
        #expect(state.status == .idle)
    }

    @Test("Status text, announcements and the switch hint")
    func statusText() {
        #expect(MonarchSwitchSettingsListTokens.statusText(.idle, message: nil, labels: labels) == nil)
        #expect(MonarchSwitchSettingsListTokens.statusText(.saving, message: nil, labels: labels) == "Saving")
        #expect(MonarchSwitchSettingsListTokens.statusText(.saved, message: nil, labels: labels) == "Saved")
        #expect(MonarchSwitchSettingsListTokens.statusText(.error, message: nil, labels: labels) == "Couldn’t save.")
        #expect(
            MonarchSwitchSettingsListTokens.statusText(.error, message: "Offline.", labels: labels)
                == "Couldn’t save. Offline."
        )
        #expect(MonarchSwitchSettingsListTokens.announcement(.saving, message: nil, labels: labels) == nil)
        #expect(MonarchSwitchSettingsListTokens.announcement(.saved, message: nil, labels: labels) == "Saved")
        #expect(MonarchSwitchSettingsListTokens.switchHint(.saved, message: nil, labels: labels).isEmpty)
        #expect(
            MonarchSwitchSettingsListTokens.switchHint(.error, message: "Offline.", labels: labels)
                == "Couldn’t save. Offline."
        )
    }

    @Test("The retry button names the row")
    func retry() {
        #expect(MonarchSwitchSettingsListTokens.retryTitle(labels: labels) == "Retry")
        #expect(
            MonarchSwitchSettingsListTokens.retryLabel(rowLabel: "Weekly summary", labels: labels)
                == "Retry Weekly summary"
        )
    }

    @Test("Row labels win over list labels, which win over the built-in strings")
    func mergedLabels() {
        let list = MonarchSwitchSettingsListLabels(saving: "Enregistrement", saved: "Enregistré")
        let row = MonarchSwitchSettingsListLabels(saved: "OK")
        let merged = row.merged(over: list)
        #expect(merged.saving == "Enregistrement")
        #expect(merged.saved == "OK")
        #expect(MonarchSwitchSettingsListTokens.statusText(.saved, message: nil, labels: merged) == "OK")
        #expect(MonarchSwitchSettingsListTokens.statusText(.error, message: nil, labels: merged) == "Couldn’t save.")
    }

    @Test("The error message comes from LocalizedError or the override")
    func errorMessage() {
        #expect(MonarchSwitchSettingsListTokens.errorMessage(SaveFailure(), labels: labels) == "The server is offline.")
        #expect(MonarchSwitchSettingsListTokens.errorMessage(CancellationError(), labels: labels) == nil)
        let custom = MonarchSwitchSettingsListLabels(errorMessage: { _ in "Try later." })
        #expect(MonarchSwitchSettingsListTokens.errorMessage(SaveFailure(), labels: custom) == "Try later.")
    }

    @Test("Colours for each status and state")
    func colors() {
        #expect(MonarchSwitchSettingsListTokens.statusColor(.saving) == \MonarchColorTokens.textMuted)
        #expect(MonarchSwitchSettingsListTokens.statusColor(.saved) == \MonarchColorTokens.successStrong)
        #expect(MonarchSwitchSettingsListTokens.statusColor(.error) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchSwitchSettingsListTokens.labelColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchSwitchSettingsListTokens.descriptionColor(isEnabled: true) == \MonarchColorTokens.textMuted)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchSwitchSettingsListStrings.saving == "Saving")
        #expect(MonarchSwitchSettingsListStrings.saved == "Saved")
        #expect(MonarchSwitchSettingsListStrings.retry == "Retry")
        #expect(MonarchSwitchSettingsListStrings.retryLabel("X") == "Retry X")
    }
}
