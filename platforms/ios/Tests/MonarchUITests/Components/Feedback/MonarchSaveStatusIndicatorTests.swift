import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSaveStatusIndicatorFixtures: ComponentFixtures {
    static let id = "save-status-indicator"
    static let fixtures = [
        Fixture("every status") {
            VStack(alignment: .leading) {
                ForEach(MonarchSaveStatus.allCases, id: \.self) { status in
                    MonarchSaveStatusIndicator(
                        status,
                        savedAt: Date(timeIntervalSince1970: 1_800_000_000).addingTimeInterval(-120)
                    )
                }
            }
        },
        Fixture("error with retry and a reason") {
            MonarchSaveStatusIndicator(.error, errorMessage: "You are offline. Changes are kept on this device.") {}
        },
        Fixture("custom labels") {
            MonarchSaveStatusIndicator(
                .saved,
                labels: MonarchSaveStatusIndicatorLabels(saved: "All changes saved to the shared workspace")
            )
        },
    ]
}

@Suite("MonarchSaveStatusIndicator")
struct MonarchSaveStatusIndicatorTests {
    private let labels = MonarchSaveStatusIndicatorLabels()

    @Test("Each status has its words, and idle has none")
    func messages() {
        func message(_ status: MonarchSaveStatus, _ error: String? = nil) -> String? {
            MonarchSaveStatusIndicatorTokens.message(status, errorMessage: error, labels: labels)
        }
        #expect(message(.idle) == nil)
        #expect(message(.unsaved) == "Unsaved changes")
        #expect(message(.saving) == "Saving…")
        #expect(message(.saved) == "Saved")
        #expect(message(.error) == "Couldn’t save")
        #expect(message(.error, "You are offline") == "You are offline")
        #expect(message(.error, "") == "Couldn’t save")
        let custom = MonarchSaveStatusIndicatorLabels(saved: "Stored")
        #expect(MonarchSaveStatusIndicatorTokens.message(.saved, errorMessage: nil, labels: custom) == "Stored")
    }

    @Test("Icons and colours: a distinct symbol per outcome, danger text on failure")
    func appearance() {
        #expect(MonarchSaveStatusIndicatorTokens.symbol(.saved) == "checkmark")
        #expect(MonarchSaveStatusIndicatorTokens.symbol(.error) == "exclamationmark.triangle")
        #expect(MonarchSaveStatusIndicatorTokens.symbol(.unsaved) == nil)
        #expect(MonarchSaveStatusIndicatorTokens.symbol(.saving) == nil)
        #expect(MonarchSaveStatusIndicatorTokens.icon(.saved) == \MonarchColorTokens.successStrong)
        #expect(MonarchSaveStatusIndicatorTokens.icon(.unsaved) == \MonarchColorTokens.warningStrong)
        #expect(MonarchSaveStatusIndicatorTokens.icon(.error) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchSaveStatusIndicatorTokens.text(.error) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchSaveStatusIndicatorTokens.text(.saved) == \MonarchColorTokens.textMuted)
    }

    @Test("The time shows only when saved, and retry only on a failure with a handler")
    func visibility() {
        let saved = Date(timeIntervalSinceReferenceDate: 800_000_000)
        #expect(MonarchSaveStatusIndicatorTokens.showsTime(.saved, savedAt: saved))
        #expect(!MonarchSaveStatusIndicatorTokens.showsTime(.saved, savedAt: nil))
        #expect(!MonarchSaveStatusIndicatorTokens.showsTime(.saving, savedAt: saved))
        #expect(MonarchSaveStatusIndicatorTokens.showsRetry(.error, hasHandler: true))
        #expect(!MonarchSaveStatusIndicatorTokens.showsRetry(.error, hasHandler: false))
        #expect(!MonarchSaveStatusIndicatorTokens.showsRetry(.saved, hasHandler: true))
    }

    @Test("Outcomes are announced; saving only when asked")
    func announcements() {
        func announce(_ old: MonarchSaveStatus, _ new: MonarchSaveStatus, saving: Bool = false) -> String? {
            MonarchSaveStatusIndicatorTokens.announcement(
                from: old,
                to: new,
                announcesSaving: saving,
                errorMessage: nil,
                labels: labels
            )
        }
        #expect(announce(.saving, .saved) == "Saved")
        #expect(announce(.saving, .error) == "Couldn’t save")
        #expect(announce(.unsaved, .saving) == nil)
        #expect(announce(.unsaved, .saving, saving: true) == "Saving…")
        #expect(announce(.saved, .unsaved) == nil)
        #expect(announce(.saved, .saved) == nil)
    }

    @Test("VoiceOver reads the words and the time")
    func accessibilityLabel() {
        let now = Date(timeIntervalSince1970: 1_800_000_000)
        let time = MonarchSaveStatusIndicatorTokens.relativeText(
            now.addingTimeInterval(-120),
            now: now,
            locale: Locale(identifier: "en_US")
        )
        let saved = MonarchSaveStatusIndicatorTokens.accessibilityLabel(message: "Saved", time: time)
        #expect(saved == "Saved, 2 minutes ago")
        #expect(MonarchSaveStatusIndicatorTokens.accessibilityLabel(message: "Saving…", time: nil) == "Saving…")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchSaveStatusIndicatorStrings.unsaved == "Unsaved changes")
        #expect(MonarchSaveStatusIndicatorStrings.saving == "Saving…")
        #expect(MonarchSaveStatusIndicatorStrings.saved == "Saved")
        #expect(MonarchSaveStatusIndicatorStrings.error == "Couldn’t save")
        #expect(MonarchSaveStatusIndicatorStrings.retry == "Retry")
    }
}
