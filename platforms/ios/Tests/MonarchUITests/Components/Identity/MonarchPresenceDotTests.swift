import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPresenceDotFixtures: ComponentFixtures {
    static let id = "presence-dot"
    static let fixtures = [
        Fixture("every status at every size") {
            VStack(alignment: .leading) {
                ForEach(MonarchPresenceDotSize.allCases, id: \.self) { size in
                    HStack {
                        ForEach(MonarchPresenceStatus.allCases, id: \.self) { status in
                            MonarchPresenceDot(status, size: size)
                        }
                    }
                }
            }
        },
        Fixture("overlay ring") {
            HStack {
                ForEach(MonarchPresenceStatus.allCases, id: \.self) { status in
                    MonarchPresenceDot(status, size: .lg, isOverlay: true)
                }
            }
        },
        Fixture("custom status, custom label and decorative") {
            HStack {
                MonarchPresenceDot(.away, customStatus: "In a meeting")
                MonarchPresenceDot(.busy, statusLabel: "On a call")
                MonarchPresenceDot(.online, isDecorative: true)
                Text("Online")
            }
        },
    ]
}

@Suite("MonarchPresenceDot")
struct MonarchPresenceDotTests {
    typealias Tokens = MonarchPresenceDotTokens

    @Test("Every presence has a name, and the names differ")
    func names() {
        #expect(MonarchPresenceDot.label(for: .online) == "Online")
        #expect(MonarchPresenceDot.label(for: .away) == "Away")
        #expect(MonarchPresenceDot.label(for: .busy) == "Busy")
        #expect(MonarchPresenceDot.label(for: .doNotDisturb) == "Do not disturb")
        #expect(MonarchPresenceDot.label(for: .offline) == "Offline")
    }

    @Test("The person's status message is joined to the presence")
    func customStatus() {
        #expect(MonarchPresenceDot.label(for: .away, customStatus: "In a meeting") == "Away · In a meeting")
        #expect(MonarchPresenceDot.label(for: .away, customStatus: "  ") == "Away")
        #expect(MonarchPresenceDot.label(for: .away, customStatus: nil) == "Away")
        #expect(Tokens.label(.busy, customStatus: "Back at 3", statusLabel: "On a call") == "On a call · Back at 3")
        #expect(Tokens.label(.busy, customStatus: nil, statusLabel: "On a call") == "On a call")
    }

    @Test("Do not disturb and offline have their own form without any setting")
    func defaultForms() {
        #expect(Tokens.form(.online, differentiateWithoutColor: false) == .filled)
        #expect(Tokens.form(.away, differentiateWithoutColor: false) == .filled)
        #expect(Tokens.form(.busy, differentiateWithoutColor: false) == .filled)
        #expect(Tokens.form(.doNotDisturb, differentiateWithoutColor: false) == .barred)
        #expect(Tokens.form(.offline, differentiateWithoutColor: false) == .ring)
    }

    @Test("With Differentiate Without Colour every presence has its own form")
    func differentiatedForms() {
        let forms = MonarchPresenceStatus.allCases.map { Tokens.form($0, differentiateWithoutColor: true) }
        #expect(forms == [.filled, .half, .square, .barred, .ring])
        for (index, form) in forms.enumerated() {
            #expect(!forms[(index + 1)...].contains(form))
        }
    }

    @Test("Colours: busy and do not disturb share the danger colour")
    func colours() {
        #expect(Tokens.fill(.online) == \MonarchColorTokens.successSolid)
        #expect(Tokens.fill(.away) == \MonarchColorTokens.warningSolid)
        #expect(Tokens.fill(.busy) == \MonarchColorTokens.dangerSolid)
        #expect(Tokens.fill(.doNotDisturb) == \MonarchColorTokens.dangerSolid)
        #expect(Tokens.fill(.offline) == \MonarchColorTokens.textSubtle)
        #expect(Tokens.bar == \MonarchColorTokens.dangerOnSolid)
        #expect(Tokens.surface == \MonarchColorTokens.bgSurface)
    }

    @Test("Sizes are 8, 10 and 12 points")
    func sizes() {
        #expect(Tokens.diameter(.sm) == 8)
        #expect(Tokens.diameter(.md) == 10)
        #expect(Tokens.diameter(.lg) == 12)
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        let names = MonarchPresenceStatus.allCases.map(MonarchPresenceDotStrings.name)
        #expect(Set(names).count == MonarchPresenceStatus.allCases.count)
        #expect(names.allSatisfy { !$0.hasPrefix("monarch.") })
        #expect(MonarchPresenceDotStrings.withStatus("Away", "Lunch") == "Away · Lunch")
    }

    #if os(iOS)
        @Test("The overlay ring adds 2 points on every side")
        @MainActor
        func overlaySize() {
            let plain = Host.layout(MonarchPresenceDot(.online).fixedSize())
            let ringed = Host.layout(MonarchPresenceDot(.online, isOverlay: true).fixedSize())
            #expect(ringed.height == plain.height + 2 * Tokens.ringWidth)
        }
    #endif
}
