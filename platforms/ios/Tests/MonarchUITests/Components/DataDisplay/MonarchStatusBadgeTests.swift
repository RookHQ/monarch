import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private enum TicketStatus: String {
    case open
    case waiting
}

enum MonarchStatusBadgeFixtures: ComponentFixtures {
    static let id = "status-badge"
    static let fixtures = [
        Fixture("every variant, both sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchStatusBadgeVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchStatusBadgeSize.allCases, id: \.self) { size in
                        HStack {
                            MonarchStatusBadge("operational", variant: variant, size: size)
                            MonarchStatusBadge("down", variant: variant, size: size)
                        }
                    }
                }
            }
        },
        Fixture("every built-in status") {
            VStack(alignment: .leading) {
                ForEach(MonarchStatusBadgeTokens.builtIn.map(\.key), id: \.self) { key in
                    MonarchStatusBadge(key)
                }
            }
        },
        Fixture("registry, unknown, no icon, dot with icon") {
            VStack(alignment: .leading) {
                MonarchStatusBadge(
                    "waiting",
                    registry: MonarchStatusRegistry([
                        TicketStatus.waiting: MonarchStatusDefinition(
                            "Waiting on customer",
                            tone: .warning,
                            systemImage: "clock"
                        )
                    ])
                )
                MonarchStatusBadge("pending_delete")
                MonarchStatusBadge(nil)
                MonarchStatusBadge("success", showsIcon: false)
                MonarchStatusBadge("info", systemImage: "bolt.fill", variant: .dot)
                MonarchStatusBadge(MonarchStatusDefinition("Archived", tone: .neutral, systemImage: "archivebox"))
            }
        },
        Fixture("installed registry, long label") {
            MonarchStatusBadge("open", label: "Open and waiting for the first reply from the support team")
                .monarchStatusRegistry(
                    MonarchStatusRegistry([TicketStatus.open: MonarchStatusDefinition("Open", tone: .info)])
                )
        },
    ]
}

@Suite("MonarchStatusBadge")
struct MonarchStatusBadgeTests {
    @Test("Built-in statuses have labels, tones and distinct symbols")
    func builtIn() {
        let registry = MonarchStatusRegistry.builtIn
        #expect(registry.definitions.count == MonarchStatusBadgeTokens.builtIn.count)
        let partial = MonarchStatusDefinition("Partial outage", tone: .warning, systemImage: "circle.righthalf.filled")
        #expect(registry.resolve("partial") == partial)
        #expect(registry.resolve("danger").label == "Error")
        #expect(registry.resolve("operational").tone == .success)
        #expect(registry.resolve("maintenance").tone == .info)
        for entry in MonarchStatusBadgeTokens.builtIn {
            let label = MonarchStatusBadgeStrings.label(entry.key)
            #expect(!label.hasPrefix("monarch."), "\(entry.key) has no string")
        }
        let healthSymbols = ["operational", "degraded", "partial", "down", "maintenance", "unknown"]
            .compactMap { registry.resolve($0).systemImage }
        #expect(Set(healthSymbols).count == healthSymbols.count)
    }

    @Test("Lookup order: badge registry, installed registry, built-in, unknown")
    func resolution() {
        let local = MonarchStatusRegistry(["down": MonarchStatusDefinition("Offline", tone: .danger)])
        let installed = MonarchStatusRegistry([
            "down": MonarchStatusDefinition("Unavailable", tone: .danger),
            "open": MonarchStatusDefinition("Open", tone: .info),
        ])
        #expect(MonarchStatusBadgeTokens.resolve("down", in: [local, installed]).label == "Offline")
        #expect(MonarchStatusBadgeTokens.resolve("down", in: [installed]).label == "Unavailable")
        #expect(MonarchStatusBadgeTokens.resolve("open", in: [local, installed]).label == "Open")
        #expect(MonarchStatusBadgeTokens.resolve("down", in: []).label == "Down")
        let unknown = MonarchStatusBadgeTokens.resolve("pending_delete", in: [])
        #expect(unknown.label == "Pending delete")
        #expect(unknown.tone == .neutral)
        #expect(unknown.systemImage == "questionmark.circle")
        #expect(MonarchStatusBadgeTokens.resolve(nil, in: []).label == "Unknown")
    }

    @Test("Registries merge, and typed keys become strings")
    func registries() {
        let typed = MonarchStatusRegistry([TicketStatus.open: MonarchStatusDefinition("Open", tone: .info)])
        #expect(typed.definitions["open"]?.label == "Open")
        let merged = typed.merging(MonarchStatusRegistry(["open": MonarchStatusDefinition("New", tone: .info)]))
        #expect(merged.definitions["open"]?.label == "New")
        let all = MonarchStatusRegistry.builtIn.merging(typed)
        #expect(all.definitions.count == MonarchStatusBadgeTokens.builtIn.count + 1)
    }

    @Test("Unknown keys become readable labels")
    func formatLabel() {
        #expect(MonarchStatusBadgeTokens.formatLabel("pending_delete") == "Pending delete")
        #expect(MonarchStatusBadgeTokens.formatLabel("IN-REVIEW") == "In review")
        #expect(MonarchStatusBadgeTokens.formatLabel("a__b--c") == "A b c")
        #expect(MonarchStatusBadgeTokens.formatLabel("__") == "Unknown")
    }

    @Test("Capsule variants take the tone's surface; the dot has none")
    func appearance() {
        let subtle = MonarchStatusBadgeTokens.appearance(tone: .success, variant: .subtle)
        #expect(subtle?.background == \MonarchColorTokens.successSubtle)
        #expect(subtle?.foreground == \MonarchColorTokens.successStrong)
        let outline = MonarchStatusBadgeTokens.appearance(tone: .warning, variant: .outline)
        #expect(outline?.background == nil)
        #expect(outline?.border == \MonarchColorTokens.warningBorder)
        let solid = MonarchStatusBadgeTokens.appearance(tone: .danger, variant: .solid)
        #expect(solid?.background == \MonarchColorTokens.dangerSolid)
        #expect(MonarchStatusBadgeTokens.appearance(tone: .info, variant: .dot) == nil)
    }

    @Test("The icon follows the status, the override, the dot variant and showsIcon")
    func symbol() {
        let down = MonarchStatusRegistry.builtIn.resolve("down")
        #expect(
            MonarchStatusBadgeTokens.symbol(definition: down, override: nil, showsIcon: true, variant: .subtle)
                == "xmark.circle"
        )
        #expect(MonarchStatusBadgeTokens.symbol(definition: down, override: nil, showsIcon: true, variant: .dot) == nil)
        func symbol(_ override: String?, _ showsIcon: Bool, _ variant: MonarchStatusBadgeVariant) -> String? {
            MonarchStatusBadgeTokens.symbol(
                definition: down,
                override: override,
                showsIcon: showsIcon,
                variant: variant
            )
        }
        #expect(symbol("bolt", true, .dot) == "bolt")
        #expect(symbol("bolt", false, .solid) == nil)
        #expect(MonarchStatusBadgeTokens.textStyle(.sm) == .caption)
        #expect(MonarchStatusBadgeTokens.textStyle(.md) == .label)
    }
}
