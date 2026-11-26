import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchConsentPreferencesFixtures: ComponentFixtures {
    static let id = "consent-preferences"
    static let now = Date(timeIntervalSince1970: 1_800_000_000)
    static let categories = [
        MonarchConsentPreferencesCategory(
            id: "necessary",
            label: "Strictly necessary",
            description: "Sign-in, security and your settings.",
            isRequired: true
        ),
        MonarchConsentPreferencesCategory(id: "analytics", label: "Analytics", description: "How the app is used."),
        MonarchConsentPreferencesCategory(
            id: "ads",
            label: "Advertising",
            description: "Ads based on your activity in other apps and on websites.",
            isSaleOrSharing: true
        ),
    ]
    static let record = MonarchConsentRecord(
        id: "1",
        date: now.addingTimeInterval(-2 * 86_400),
        method: .custom,
        policyVersion: "3",
        regime: .optIn,
        categories: ["necessary": true, "analytics": true, "ads": false]
    )

    static let fixtures = [
        Fixture("first time, nothing chosen") {
            MonarchConsentPreferences(categories: categories) { _, _ in }
        },
        Fixture("saved choice with record and policy") {
            MonarchConsentPreferences(
                categories: categories,
                choices: record.categories,
                record: record,
                policyURL: URL(filePath: "/privacy"),
                now: now
            ) { _, _ in }
        },
        Fixture("Global Privacy Control") {
            MonarchConsentPreferences(
                categories: categories,
                choices: ["analytics": true, "ads": true],
                isGlobalPrivacyControl: true
            ) { _, _ in }
        },
        Fixture("choice mode") {
            MonarchConsentPreferences(
                categories: Array(categories.prefix(2)),
                choices: ["analytics": true],
                mode: .choice,
                record: record,
                now: now
            ) { _, _ in }
        },
        Fixture("custom labels, no withdraw") {
            MonarchConsentPreferences(
                categories: categories,
                record: record,
                allowsWithdraw: false,
                now: now,
                labels: MonarchConsentPreferencesLabels(title: "Cookie settings", save: "Save")
            ) { _, _ in }
        },
    ]
}

@Suite("MonarchConsentPreferences")
struct MonarchConsentPreferencesTests {
    private let categories = MonarchConsentPreferencesFixtures.categories
    private var necessary: MonarchConsentPreferencesCategory { categories[0] }
    private var analytics: MonarchConsentPreferencesCategory { categories[1] }
    private var ads: MonarchConsentPreferencesCategory { categories[2] }

    @Test("Required categories are locked on; GPC locks sale-or-sharing off")
    func locks() {
        #expect(MonarchConsentPreferencesTokens.isLocked(necessary, isGlobalPrivacyControl: false))
        #expect(!MonarchConsentPreferencesTokens.isLocked(ads, isGlobalPrivacyControl: false))
        #expect(MonarchConsentPreferencesTokens.isLocked(ads, isGlobalPrivacyControl: true))
        #expect(!MonarchConsentPreferencesTokens.isLocked(analytics, isGlobalPrivacyControl: true))
        #expect(MonarchConsentPreferencesTokens.isOn(necessary, choices: [:], isGlobalPrivacyControl: false))
        #expect(!MonarchConsentPreferencesTokens.isOn(ads, choices: ["ads": true], isGlobalPrivacyControl: true))
        #expect(MonarchConsentPreferencesTokens.isOn(ads, choices: ["ads": true], isGlobalPrivacyControl: false))
        #expect(!MonarchConsentPreferencesTokens.isOn(analytics, choices: [:], isGlobalPrivacyControl: false))
    }

    @Test("Each decision reports every category, with the locks applied")
    func decisions() {
        let draft = ["analytics": true, "ads": false]
        #expect(
            MonarchConsentPreferencesTokens.choices(
                for: .acceptAll,
                categories: categories,
                draft: [:],
                isGlobalPrivacyControl: false
            ) == ["necessary": true, "analytics": true, "ads": true]
        )
        #expect(
            MonarchConsentPreferencesTokens.choices(
                for: .acceptAll,
                categories: categories,
                draft: [:],
                isGlobalPrivacyControl: true
            ) == ["necessary": true, "analytics": true, "ads": false]
        )
        #expect(
            MonarchConsentPreferencesTokens.choices(
                for: .rejectAll,
                categories: categories,
                draft: draft,
                isGlobalPrivacyControl: false
            ) == ["necessary": true, "analytics": false, "ads": false]
        )
        #expect(
            MonarchConsentPreferencesTokens.choices(
                for: .custom,
                categories: categories,
                draft: draft,
                isGlobalPrivacyControl: false
            ) == ["necessary": true, "analytics": true, "ads": false]
        )
        #expect(
            MonarchConsentPreferencesTokens.choices(
                for: .withdrawn,
                categories: categories,
                draft: draft,
                isGlobalPrivacyControl: false
            ) == ["necessary": true, "analytics": false, "ads": false]
        )
    }

    @Test("Withdraw needs categories mode, permission and a saved, not withdrawn, decision")
    func withdraw() {
        let record = MonarchConsentPreferencesFixtures.record
        #expect(MonarchConsentPreferencesTokens.showsWithdraw(mode: .categories, allowsWithdraw: true, record: record))
        #expect(!MonarchConsentPreferencesTokens.showsWithdraw(mode: .choice, allowsWithdraw: true, record: record))
        #expect(
            !MonarchConsentPreferencesTokens.showsWithdraw(mode: .categories, allowsWithdraw: false, record: record)
        )
        #expect(!MonarchConsentPreferencesTokens.showsWithdraw(mode: .categories, allowsWithdraw: true, record: nil))
        let withdrawn = MonarchConsentRecord(
            id: "2",
            date: record.date,
            method: .withdrawn,
            policyVersion: "3",
            regime: .optIn,
            categories: [:]
        )
        #expect(
            !MonarchConsentPreferencesTokens.showsWithdraw(mode: .categories, allowsWithdraw: true, record: withdrawn)
        )
    }

    @Test("Choice mode status is a word, not only a colour")
    func status() {
        let labels = MonarchConsentPreferencesLabels()
        #expect(
            MonarchConsentPreferencesTokens.status(
                necessary,
                choices: [:],
                isGlobalPrivacyControl: false,
                labels: labels
            ) == "Always on"
        )
        #expect(
            MonarchConsentPreferencesTokens.status(
                analytics,
                choices: ["analytics": true],
                isGlobalPrivacyControl: false,
                labels: labels
            ) == "On"
        )
        #expect(
            MonarchConsentPreferencesTokens.status(
                ads,
                choices: ["ads": true],
                isGlobalPrivacyControl: true,
                labels: labels
            ) == "Off"
        )
        #expect(MonarchConsentPreferencesTokens.statusRole(isOn: true) == \MonarchColorTokens.successStrong)
        #expect(MonarchConsentPreferencesTokens.statusRole(isOn: false) == \MonarchColorTokens.textMuted)
    }

    @Test("The record line is relative within a week, then a date")
    func record() {
        let english = Locale(identifier: "en_US")
        let now = MonarchConsentPreferencesFixtures.now
        let line = MonarchConsentPreferencesTokens.recordLine(
            MonarchConsentPreferencesFixtures.record,
            now: now,
            locale: english,
            labels: MonarchConsentPreferencesLabels()
        )
        #expect(line == "Policy version 3 · Saved 2 days ago")
        let old = MonarchConsentPreferencesTokens.savedTime(
            now.addingTimeInterval(-30 * 86_400),
            now: now,
            locale: english
        )
        #expect(!old.contains("ago"))
        #expect(old.contains("2026") || old.contains("2027"))
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchConsentPreferencesStrings.title, MonarchConsentPreferencesStrings.description,
            MonarchConsentPreferencesStrings.policy, MonarchConsentPreferencesStrings.alwaysOn,
            MonarchConsentPreferencesStrings.globalPrivacyControl, MonarchConsentPreferencesStrings.on,
            MonarchConsentPreferencesStrings.off, MonarchConsentPreferencesStrings.rejectAll,
            MonarchConsentPreferencesStrings.acceptAll, MonarchConsentPreferencesStrings.save,
            MonarchConsentPreferencesStrings.withdraw,
        ]
        #expect(all.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(MonarchConsentPreferencesStrings.policyVersion("3") == "Policy version 3")
        #expect(MonarchConsentPreferencesStrings.saved("today") == "Saved today")
    }
}
