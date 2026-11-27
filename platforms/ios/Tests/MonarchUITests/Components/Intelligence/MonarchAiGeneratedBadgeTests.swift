import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiGeneratedBadgeFixtures: ComponentFixtures {
    static let id = "ai-generated-badge"
    static let fixtures = [
        Fixture("kinds and sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchAiGeneratedBadgeKind.allCases, id: \.self) { kind in
                    ForEach(MonarchAiGeneratedBadgeSize.allCases, id: \.self) { size in
                        MonarchAiGeneratedBadge(kind, size: size)
                    }
                }
            }
        },
        Fixture("icon only") {
            HStack {
                ForEach(MonarchAiGeneratedBadgeKind.allCases, id: \.self) { kind in
                    MonarchAiGeneratedBadge(kind, isIconOnly: true)
                    MonarchAiGeneratedBadge(kind, size: .sm, isIconOnly: true)
                }
            }
        },
        Fixture("with model and date") {
            MonarchAiGeneratedBadge(
                .assisted,
                model: "gemini-2.5-pro",
                date: Date(timeIntervalSince1970: 1_700_000_000)
            )
        },
        Fixture("custom label and detail") {
            MonarchAiGeneratedBadge(
                label: "Drafted by the assistant from your meeting notes",
                detail: "Reviewed by nobody yet"
            )
        },
        Fixture("bot glyph sizes") {
            HStack {
                ForEach(MonarchIconSize.allCases, id: \.self) { size in
                    MonarchAiBotGlyph(size: size)
                }
                MonarchAiBotGlyph(label: "Assistant")
            }
        },
    ]
}

@Suite("MonarchAiGeneratedBadge")
struct MonarchAiGeneratedBadgeTests {
    @Test("The badge uses the AI colours")
    func appearance() {
        let appearance = MonarchAiGeneratedBadgeTokens.appearance
        #expect(appearance.background == \MonarchColorTokens.aiSubtle)
        #expect(appearance.foreground == \MonarchColorTokens.aiStrong)
        #expect(appearance.border == \MonarchColorTokens.aiBorder)
    }

    @Test("Only Edited after AI has a dashed border")
    func dashed() {
        #expect(MonarchAiGeneratedBadgeKind.allCases.filter(MonarchAiGeneratedBadgeTokens.isDashed) == [.edited])
    }

    @Test("The small size is smaller")
    func sizes() {
        #expect(MonarchAiGeneratedBadgeTokens.glyphSize(.sm) < MonarchAiGeneratedBadgeTokens.glyphSize(.md))
        #expect(
            MonarchAiGeneratedBadgeTokens.horizontalPadding(.sm) < MonarchAiGeneratedBadgeTokens.horizontalPadding(.md)
        )
    }

    @Test("The provenance line is made from the model and the time")
    func details() {
        #expect(MonarchAiGeneratedBadgeTokens.details(model: nil, time: nil).isEmpty)
        #expect(MonarchAiGeneratedBadgeTokens.details(model: "", time: "").isEmpty)
        #expect(MonarchAiGeneratedBadgeTokens.details(model: "gemini-2.5-pro", time: nil) == "By gemini-2.5-pro")
        #expect(
            MonarchAiGeneratedBadgeTokens.details(model: nil, time: "Mar 3, 2025 at 9:41 AM")
                == "Mar 3, 2025 at 9:41 AM"
        )
        #expect(
            MonarchAiGeneratedBadgeTokens.details(model: "gemini-2.5-pro", time: "Mar 3, 2025 at 9:41 AM")
                == "By gemini-2.5-pro, Mar 3, 2025 at 9:41 AM"
        )
    }

    @Test("The time is a medium date and a short time in the given locale and zone")
    func formattedTime() {
        var calendar = Calendar(identifier: .gregorian)
        let utc = TimeZone(secondsFromGMT: 0) ?? .gmt
        calendar.timeZone = utc
        let date =
            calendar.date(from: DateComponents(year: 2025, month: 3, day: 3, hour: 9, minute: 41)) ?? .distantPast
        let text = MonarchAiGeneratedBadgeTokens.formattedTime(
            date,
            locale: Locale(identifier: "en_US"),
            calendar: calendar,
            timeZone: utc
        )
        #expect(text.contains("Mar 3, 2025"))
        #expect(text.contains("9:41"))
        let tokyo = MonarchAiGeneratedBadgeTokens.formattedTime(
            date,
            locale: Locale(identifier: "en_US"),
            calendar: calendar,
            timeZone: TimeZone(identifier: "Asia/Tokyo") ?? utc
        )
        #expect(tokyo.contains("6:41"))
    }

    @Test("The tooltip names an icon-only badge")
    func help() {
        #expect(MonarchAiGeneratedBadgeTokens.help(label: "AI generated", details: "", isIconOnly: false).isEmpty)
        #expect(
            MonarchAiGeneratedBadgeTokens.help(label: "AI generated", details: "By m", isIconOnly: false) == "By m"
        )
        #expect(
            MonarchAiGeneratedBadgeTokens.help(label: "AI generated", details: "", isIconOnly: true) == "AI generated"
        )
        #expect(
            MonarchAiGeneratedBadgeTokens.help(label: "AI generated", details: "By m", isIconOnly: true)
                == "AI generated. By m"
        )
    }

    @Test("Every kind has its own label from the strings table")
    func strings() {
        #expect(MonarchAiGeneratedBadgeStrings.label(.generated) == "AI generated")
        #expect(MonarchAiGeneratedBadgeStrings.label(.assisted) == "AI assisted")
        #expect(MonarchAiGeneratedBadgeStrings.label(.edited) == "Edited after AI")
        #expect(MonarchAiGeneratedBadgeStrings.byModel("m") == "By m")
        #expect(MonarchAiGeneratedBadgeStrings.byModel("m", at: "now") == "By m, now")
    }

    @Test("The bot glyph is drawn inside its frame, centred, at any size")
    func glyphShape() {
        for side in [12.0, 16.0, 48.0] {
            let rect = CGRect(x: 10, y: 20, width: side, height: side)
            let bounds = MonarchAiBotGlyphShape().path(in: rect).boundingRect
            #expect(!bounds.isEmpty)
            #expect(rect.contains(bounds))
            #expect(abs(bounds.midX - rect.midX) < 0.001)
        }
        // In a frame that is not square the glyph keeps its proportions and stays centred.
        let wide = CGRect(x: 0, y: 0, width: 64, height: 16)
        let bounds = MonarchAiBotGlyphShape().path(in: wide).boundingRect
        #expect(bounds.width <= 16)
        #expect(abs(bounds.midX - wide.midX) < 0.001)
    }
}
