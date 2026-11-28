import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let connectors = [
    MonarchAiConnector(
        id: "calendar",
        name: "Calendar",
        description: "Read and create events.",
        status: .connected,
        account: "ada@example.com",
        isEnabled: true,
        tools: [
            MonarchAiConnectorTool(id: "list", name: "List events", risk: .read, permission: .allow),
            MonarchAiConnectorTool(
                id: "create",
                name: "Create event",
                description: "Adds an event.",
                risk: .write,
                permission: .ask
            ),
            MonarchAiConnectorTool(id: "delete", name: "Delete event", risk: .destructive, permission: .ask),
        ]
    ),
    MonarchAiConnector(
        id: "drive",
        name: "Drive",
        status: .needsSignIn,
        isEnabled: false,
        tools: [MonarchAiConnectorTool(id: "read", name: "Read files", risk: .read, permission: .ask)]
    ),
    MonarchAiConnector(id: "crm", name: "Café CRM", status: .error, isEnabled: true),
    MonarchAiConnector(id: "mail", name: "Mail", status: .unavailable, isEnabled: false),
    MonarchAiConnector(
        id: "notes",
        name: "Notes",
        status: .connected,
        isEnabled: false,
        tools: [MonarchAiConnectorTool(id: "search", name: "Search notes", risk: .read, permission: .ask)]
    ),
]

enum MonarchAiConnectorPickerFixtures: ComponentFixtures {
    static let id = "ai-connector-picker"
    static let fixtures = [
        Fixture("every status") {
            MonarchAiConnectorPicker(connectors: .constant(connectors), onConnect: { _ in }, onManage: { _ in })
        },
        Fixture("not searchable, destructive allowed") {
            MonarchAiConnectorPicker(
                "Apps",
                connectors: .constant([connectors[0]]),
                allowsDestructiveAutoApprove: true,
                isSearchable: false
            )
        },
        Fixture("empty") {
            MonarchAiConnectorPicker(connectors: .constant([]))
        },
    ]
}

@Suite("MonarchAiConnectorPicker")
struct MonarchAiConnectorPickerTests {
    private let labels = MonarchAiConnectorPickerLabels()
    private let locale = Locale(identifier: "en_US")

    @Test("Only connected and enabled connectors count as on")
    func counts() {
        #expect(MonarchAiConnectorPickerTokens.isActive(connectors[0]))
        #expect(!MonarchAiConnectorPickerTokens.isActive(connectors[2]))
        #expect(!MonarchAiConnectorPickerTokens.isActive(connectors[4]))
        let counts = MonarchAiConnectorPickerTokens.counts(connectors)
        #expect(counts == .init(on: 1, total: 5, asking: 2))
        #expect(
            MonarchAiConnectorPickerTokens.summary(counts, labels: labels) == "1 of 5 connectors on · 2 tools ask first"
        )
        #expect(
            MonarchAiConnectorPickerTokens.summary(.init(on: 1, total: 1, asking: 1), labels: labels)
                == "1 of 1 connector on · 1 tool asks first"
        )
    }

    @Test("Search ignores case, accents and outer spaces")
    func filtered() {
        #expect(
            MonarchAiConnectorPickerTokens.filtered(connectors, query: "  cafe ", locale: locale).map(\.id) == ["crm"]
        )
        #expect(MonarchAiConnectorPickerTokens.filtered(connectors, query: "DR", locale: locale).map(\.id) == ["drive"])
        #expect(
            MonarchAiConnectorPickerTokens.filtered(connectors, query: " ", locale: locale).count == connectors.count
        )
        #expect(MonarchAiConnectorPickerTokens.filtered(connectors, query: "zzz", locale: locale).isEmpty)
    }

    @Test("Status reads in words, with the account and the reason it cannot be on")
    func status() {
        #expect(
            MonarchAiConnectorPickerTokens.statusText(connectors[0], labels: labels) == "Connected as ada@example.com"
        )
        #expect(MonarchAiConnectorPickerTokens.statusText(connectors[1], labels: labels) == "Sign-in needed")
        #expect(MonarchAiConnectorPickerTokens.disabledReason(.connected, labels: labels) == nil)
        #expect(
            MonarchAiConnectorPickerTokens.disabledReason(.needsSignIn, labels: labels)
                == "Sign in before turning this on."
        )
        #expect(
            MonarchAiConnectorPickerTokens.disabledReason(.error, labels: labels) == "Reconnect before turning this on."
        )
        let symbols = MonarchAiConnectorStatus.allCases.map(MonarchAiConnectorPickerTokens.statusSymbol)
        #expect(Set(symbols).count == MonarchAiConnectorStatus.allCases.count)
        #expect(MonarchAiConnectorPickerTokens.statusRole(.connected) == \MonarchColorTokens.successStrong)
        #expect(MonarchAiConnectorPickerTokens.statusRole(.error) == \MonarchColorTokens.dangerStrong)
    }

    @Test("Sign in and Reconnect appear for the statuses that need them")
    func connectTitle() {
        #expect(MonarchAiConnectorPickerTokens.connectTitle(.needsSignIn, labels: labels) == "Sign in")
        #expect(MonarchAiConnectorPickerTokens.connectTitle(.error, labels: labels) == "Reconnect")
        #expect(MonarchAiConnectorPickerTokens.connectTitle(.connected, labels: labels) == nil)
        #expect(MonarchAiConnectorPickerTokens.connectTitle(.unavailable, labels: labels) == nil)
    }

    @Test("Risks have a tone, a symbol and words")
    func risks() {
        #expect(MonarchAiConnectorPickerTokens.tone(.read) == .neutral)
        #expect(MonarchAiConnectorPickerTokens.tone(.write) == .warning)
        #expect(MonarchAiConnectorPickerTokens.tone(.destructive) == .danger)
        #expect(MonarchAiConnectorPickerStrings.risk(.destructive) == "Can delete data")
    }

    @Test("A destructive tool is not offered Allow unless allowed")
    func destructive() {
        let delete = connectors[0].tools[2]
        #expect(MonarchAiConnectorPickerTokens.isLocked(delete, allowsDestructiveAutoApprove: false))
        #expect(!MonarchAiConnectorPickerTokens.isLocked(delete, allowsDestructiveAutoApprove: true))
        #expect(MonarchAiConnectorPickerTokens.options(delete, allowsDestructiveAutoApprove: false) == [.ask, .never])
        #expect(
            MonarchAiConnectorPickerTokens.options(delete, allowsDestructiveAutoApprove: true) == [
                .allow, .ask, .never,
            ]
        )
        var allowed = delete
        allowed.permission = .allow
        #expect(MonarchAiConnectorPickerTokens.options(allowed, allowsDestructiveAutoApprove: false).contains(.allow))
        let refused = MonarchAiConnectorPickerTokens.setting(
            connectors,
            id: "calendar",
            tool: "delete",
            to: .allow,
            allowsDestructiveAutoApprove: false
        )
        #expect(refused[0].tools[2].permission == .ask)
        let never = MonarchAiConnectorPickerTokens.setting(
            connectors,
            id: "calendar",
            tool: "delete",
            to: .never,
            allowsDestructiveAutoApprove: false
        )
        #expect(never[0].tools[2].permission == .never)
        #expect(never[0].tools[0].permission == .allow)
    }

    @Test("Only a connected connector can be turned on; any can be turned off")
    func switching() {
        let on = MonarchAiConnectorPickerTokens.setting(connectors, id: "notes", isEnabled: true)
        #expect(on[4].isEnabled)
        let refused = MonarchAiConnectorPickerTokens.setting(connectors, id: "drive", isEnabled: true)
        #expect(!refused[1].isEnabled)
        let off = MonarchAiConnectorPickerTokens.setting(connectors, id: "crm", isEnabled: false)
        #expect(!off[2].isEnabled)
        #expect(off[0] == connectors[0])
    }

    @Test("Overrides replace the built-in text")
    func overrides() {
        let custom = MonarchAiConnectorPickerLabels(
            summary: { "\($0)/\($1) · \($2)" },
            status: { $0 == .error ? "Broken" : nil },
            account: { "(\($0))" }
        )
        #expect(MonarchAiConnectorPickerTokens.summary(.init(on: 1, total: 2, asking: 3), labels: custom) == "1/2 · 3")
        #expect(MonarchAiConnectorPickerTokens.statusText(connectors[2], labels: custom) == "Broken")
        #expect(
            MonarchAiConnectorPickerTokens.statusText(connectors[0], labels: custom) == "Connected (ada@example.com)"
        )
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchAiConnectorPickerStrings.title == "Connectors")
        #expect(MonarchAiConnectorPickerStrings.results(0) == "No connectors found")
        #expect(MonarchAiConnectorPickerStrings.results(1) == "1 connector found")
        #expect(MonarchAiConnectorPickerStrings.results(3) == "3 connectors found")
        #expect(MonarchAiConnectorPickerStrings.tools(1) == "1 tool")
        #expect(MonarchAiConnectorPickerStrings.tools(4) == "4 tools")
        #expect(MonarchAiConnectorPickerStrings.noResults("x") == "No connectors match “x”.")
        #expect(MonarchAiConnectorPickerStrings.enable("Calendar") == "Use Calendar in this conversation")
        #expect(MonarchAiConnectorPickerStrings.actionLabel("Sign in", "Drive") == "Sign in: Drive")
        #expect(MonarchAiConnectorPickerStrings.permissionLabel("List") == "Permission for List")
        for permission in MonarchAiConnectorToolPermission.allCases {
            #expect(!MonarchAiConnectorPickerStrings.permission(permission).hasPrefix("monarch."))
        }
        for value in [
            MonarchAiConnectorPickerStrings.searchPrompt, MonarchAiConnectorPickerStrings.done,
            MonarchAiConnectorPickerStrings.clearSearch, MonarchAiConnectorPickerStrings.empty,
            MonarchAiConnectorPickerStrings.manage, MonarchAiConnectorPickerStrings.destructiveLocked,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
