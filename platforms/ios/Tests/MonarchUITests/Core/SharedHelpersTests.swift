import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// The shared helpers in `Internal/` added by the consolidation pass (C-001,
/// C-015, C-016, C-018). Components move to them; these tests pin their
/// behaviour once, so the copies can be deleted.
@Suite("Shared helpers")
@MainActor
struct SharedHelpersTests {
    // MARK: Tones

    private static let themes: [(String, MonarchTheme)] = [
        ("light", .light), ("dark", .dark), ("hc-light", .highContrastLight), ("hc-dark", .highContrastDark),
    ]

    private static func luminance(_ color: MonarchRGBA) -> Double {
        func channel(_ value: Double) -> Double {
            value <= 0.04045 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private static func contrast(_ a: MonarchRGBA, _ b: MonarchRGBA) -> Double {
        let (la, lb) = (luminance(a), luminance(b))
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @Test("The accent and AI tones read in every theme", arguments: [MonarchTone.accent, .ai])
    func accentAndAiContrast(tone: MonarchTone) {
        let palette = MonarchTonePalette.palette(for: tone)
        for (id, theme) in Self.themes {
            let colors = theme.colors.tokens
            let onSolid = Self.contrast(colors[keyPath: palette.onSolid], colors[keyPath: palette.solid])
            let strong = Self.contrast(colors[keyPath: palette.strong], colors[keyPath: palette.subtle])
            #expect(onSolid >= 4.5, "\(tone) on solid in \(id): \(onSolid)")
            #expect(strong >= 4.5, "\(tone) strong on subtle in \(id): \(strong)")
        }
    }

    @Test("The status tones have distinct solid colours in every theme")
    func tonesPerTheme() {
        // The accent and AI colours are brand choices and may match (the
        // default Rook accent is close to the AI violet), so only the status
        // tones are required to differ.
        let statuses: [MonarchTone] = [.neutral, .info, .success, .warning, .danger]
        for (id, theme) in Self.themes {
            let solids = statuses.map { theme.colors.tokens[keyPath: MonarchTonePalette.palette(for: $0).solid] }
            #expect(Set(solids).count == statuses.count, "solids collide in \(id)")
        }
    }

    // MARK: Relative time

    private static let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
    private static let english = Locale(identifier: "en_US")

    @Test("Relative times are written in full words, and the future reads as now unless allowed")
    func relativeTime() {
        let five = Self.now.addingTimeInterval(-300)
        #expect(MonarchRelativeTime.string(five, now: Self.now, locale: Self.english) == "5 minutes ago")
        let ahead = Self.now.addingTimeInterval(7200)
        #expect(MonarchRelativeTime.string(ahead, now: Self.now, locale: Self.english) == "now")
        #expect(
            MonarchRelativeTime.string(ahead, now: Self.now, locale: Self.english, allowsFuture: true) == "in 2 hours"
        )
        let short = MonarchRelativeTime.string(five, now: Self.now, locale: Self.english, unitsStyle: .abbreviated)
        #expect(short != "5 minutes ago", "the short style is shorter than the full one")
    }

    @Test("A frozen relative-time clock shows the given time")
    func relativeTimeClock() {
        let view = MonarchRelativeTimeClock(now: Self.now) { date in
            Text(MonarchRelativeTime.string(date.addingTimeInterval(-60), now: date, locale: Self.english))
        }
        #expect(Host.layout(view).height > 0)
    }

    // MARK: Links

    @Test("Only http, https with a host, and mailto with an address are safe")
    func safeURLs() {
        #expect(MonarchSafeURL.url("https://example.com/a")?.host == "example.com")
        #expect(MonarchSafeURL.url("  http://example.com  ") != nil)
        #expect(MonarchSafeURL.url("mailto:ana@example.com") != nil)
        #expect(MonarchSafeURL.url("mailto:") == nil)
        #expect(MonarchSafeURL.url("javascript:alert(1)") == nil)
        #expect(MonarchSafeURL.url("java\nscript:alert(1)") == nil)
        #expect(MonarchSafeURL.url("data:text/html,hi") == nil)
        #expect(MonarchSafeURL.url("file:///etc/hosts") == nil)
        #expect(MonarchSafeURL.url("https://") == nil)
        #expect(MonarchSafeURL.url(nil) == nil)
        #expect(MonarchSafeURL.displayHost("https://www.example.com/x") == "example.com")
        #expect(MonarchAiSourceFormat.faviconURL("mailto:ana@example.com") == nil)
        #expect(MonarchAiSourceFormat.faviconURL("https://example.com") != nil)
    }

    @Test("Typed link addresses are normalised as on the web")
    func linkAddresses() {
        #expect(MonarchLinkAddress.normalized("example.com/x") == "https://example.com/x")
        #expect(MonarchLinkAddress.normalized(" ana@example.com ") == "mailto:ana@example.com")
        #expect(MonarchLinkAddress.normalized("https://example.com") == "https://example.com")
        #expect(MonarchLinkAddress.normalized("/docs") == "/docs")
        #expect(MonarchLinkAddress.normalized("#top") == "#top")
        #expect(MonarchLinkAddress.normalized("javascript:alert(1)") == nil)
        #expect(MonarchLinkAddress.normalized("java\tscript:alert(1)") == nil)
        #expect(MonarchLinkAddress.normalized("hello") == nil)
        #expect(MonarchLinkAddress.normalized("   ") == nil)
    }

    // MARK: Errors and formats

    private struct Described: LocalizedError {
        var errorDescription: String? { "The server is down." }
    }

    private struct Plain: Error {}

    @Test("A thrown error shows its own description, else the override, else the fallback")
    func failureMessages() {
        #expect(MonarchFailureMessage.message(for: Described(), override: "X", fallback: "Y") == "The server is down.")
        #expect(
            MonarchFailureMessage.message(for: Plain(), override: "Couldn't send.", fallback: "Y") == "Couldn't send."
        )
        #expect(
            MonarchFailureMessage.message(for: Plain(), override: "", fallback: "Couldn't save.") == "Couldn't save."
        )
        #expect(MonarchFailureMessage.spoken("Too short") == "Error: Too short")
    }

    @Test("Durations are written as the web writes them")
    func durations() {
        #expect(MonarchFormats.duration(minutes: 45) == "45m")
        #expect(MonarchFormats.duration(minutes: 125) == "2h 05m")
        #expect(MonarchFormats.duration(minutes: 60) == "1h 00m")
        #expect(MonarchFormats.duration(minutes: 1440 * 3) == "3d")
        #expect(MonarchFormats.duration(minutes: 1440 * 3 + 240) == "3d 4h")
        #expect(MonarchFormats.duration(minutes: -5) == "0m")
    }

    @Test("Times of day ignore daylight saving and follow the clock asked for")
    func timesOfDay() {
        #expect(MonarchFormats.timeOfDay(360, locale: Self.english, uses24HourClock: true) == "06:00")
        #expect(MonarchFormats.timeOfDay(1440 + 90, locale: Self.english, uses24HourClock: true) == "01:30")
        #expect(MonarchFormats.timeOfDay(-60, locale: Self.english, uses24HourClock: true) == "23:00")
        let twelve = MonarchFormats.timeOfDay(810, locale: Self.english, uses24HourClock: false)
        #expect(twelve.replacingOccurrences(of: "\u{202F}", with: " ") == "1:30 PM")
        let shifts = MonarchShiftRosterFormats(
            locale: Self.english,
            calendar: Calendar(identifier: .gregorian),
            uses24HourClock: true
        )
        #expect(shifts.time(360) == MonarchFormats.timeOfDay(360, locale: Self.english, uses24HourClock: true))
    }

    @Test("The medium date is the locale's abbreviated date")
    func mediumDate() {
        let utc = TimeZone(identifier: "UTC") ?? .gmt
        let date = Date(timeIntervalSince1970: 1_793_750_400)  // 2026-11-04 00:00 UTC
        #expect(MonarchFormats.mediumDate(date, locale: Self.english, timeZone: utc) == "Nov 4, 2026")
    }

    // MARK: State, tasks, motion

    @Test("The load state reports loading and its failure message")
    func loadState() {
        #expect(MonarchLoadState.loading.isLoading)
        #expect(MonarchLoadState.failed(message: "Offline").failureMessage == "Offline")
        #expect(MonarchLoadState.failed(message: "").failureMessage == nil)
        #expect(MonarchLoadState.failed(message: nil).isFailed)
        #expect(!MonarchLoadState.ready.isFailed)
    }

    @Test("A task holder cancels the task it replaces and the task it holds when it goes away")
    func taskHolder() async {
        var holder: MonarchTaskHolder? = MonarchTaskHolder()
        holder?.replace { try? await Task.sleep(for: .seconds(60)) }
        let first = holder?.task
        holder?.replace { try? await Task.sleep(for: .seconds(60)) }
        #expect(first?.isCancelled == true)
        let second = holder?.task
        #expect(holder?.isRunning == true)
        holder = nil
        #expect(second?.isCancelled == true)
    }

    @Test("The pulse runs from 0 to 1 and back over one period, and holds still without motion")
    func pulse() {
        let period = MonarchPulse.period
        #expect(MonarchPulse.value(at: nil) == 0)
        #expect(abs(MonarchPulse.value(at: 0)) < 1e-9)
        #expect(abs(MonarchPulse.value(at: period / 2) - 1) < 1e-9)
        #expect(abs(MonarchPulse.value(at: period) - 0) < 1e-9)
        #expect(abs(MonarchPulse.value(at: 0, offset: 0.5) - 1) < 1e-9)
    }

    // MARK: Accessibility

    @Test("Focus after a removal goes to the next item, else the previous one")
    func focusAfterRemoval() {
        #expect(MonarchFocusAfterRemoval.target(removing: "b", from: ["a", "b", "c"]) == "c")
        #expect(MonarchFocusAfterRemoval.target(removing: "c", from: ["a", "b", "c"]) == "b")
        #expect(MonarchFocusAfterRemoval.target(removing: "a", from: ["a"]) == nil)
        #expect(MonarchFocusAfterRemoval.target(removing: "z", from: ["a"]) == nil)
        #expect(MonarchFocusAfterRemoval.target(removedIndex: 5, remaining: ["a", "b"]) == "b")
    }

    @Test("Announcements carry the politeness as speech priority")
    func announcer() {
        let polite = MonarchAnnouncer.announcement("Saved")
        let assertive = MonarchAnnouncer.announcement("Failed", politeness: .assertive)
        #expect(polite.accessibilitySpeechAnnouncementPriority == .default)
        #expect(assertive.accessibilitySpeechAnnouncementPriority == .high)
    }

    @Test("Spoken labels leave out empty parts")
    func spokenLabels() {
        #expect(MonarchSpokenLabel.join("Invoice 1042", nil, " ", "Paid") == "Invoice 1042, Paid")
        #expect(MonarchSpokenLabel.join([nil, ""]) == "")
    }

    @Test("Sentences end at the web's marks, measured in UTF-16")
    func sentences() {
        let text = "Hello there. “Quoted!” Next? Unfinished"
        #expect(MonarchSentences.complete(in: text) == ["Hello there.", "“Quoted!”", "Next?"])
        let boundaries = MonarchSentences.boundaries(in: text)
        #expect(boundaries.first == 12)
        #expect(MonarchSentences.boundaries(in: "A. B. ", from: 3) == [5])
        #expect(MonarchSentences.boundaries(in: "No end") == [])
    }

    @Test("A row that does not fit stacks, and the accessibility sizes always stack")
    func rowOrStack() {
        #expect(MonarchRowOrStack<EmptyView>.alwaysStacks(isAccessibilitySize: true))
        #expect(!MonarchRowOrStack<EmptyView>.alwaysStacks(isAccessibilitySize: false))
        let narrow = MonarchRowOrStack(spacing: 8) {
            Text(String(repeating: "Long title ", count: 20))
            Text("Action")
        }
        .frame(width: 200)
        let wide = MonarchRowOrStack(spacing: 8) {
            Text("Title")
            Text("Action")
        }
        #expect(Host.layout(narrow).height > Host.layout(wide).height)
    }

    @Test("The ideal width of text grows with the text")
    func idealWidth() {
        #expect(Host.idealWidth(Text("Longer text")) > Host.idealWidth(Text("Short")))
    }

    // MARK: Plurals

    @Test("Plural strings follow the plural rules")
    func plurals() {
        #expect(MonarchStrings.plural("monarch.loadMore.count", 1, locale: Self.english) == "Showing 1 item")
        #expect(MonarchStrings.plural("monarch.loadMore.count", 3, locale: Self.english) == "Showing 3 items")
        #expect(
            MonarchStrings.plural("monarch.loadMore.countOfTotal", 1, 1, locale: Self.english) == "Showing 1 of 1 item"
        )
        #expect(
            MonarchStrings.plural("monarch.loadMore.countOfTotal", 20, 120, locale: Self.english)
                == "Showing 20 of 120 items"
        )
        #expect(MonarchStrings.plural("monarch.loadMore.loaded", 1, locale: Self.english) == "1 more item loaded.")
        #expect(
            MonarchStrings.plural("monarch.uploadQueue.summaryUploaded", 2, 5, locale: Self.english)
                == "2 of 5 uploaded"
        )
        #expect(MonarchStrings.plural("monarch.avatar.groupSummary", 1, locale: Self.english) == "1 person")
        #expect(MonarchStrings.plural("monarch.avatar.groupSummary", 4, locale: Self.english) == "4 people")
    }

    // MARK: Moved definitions (C-016)

    @Test("The overflow count shows every item rather than a +1 tile")
    func overflow() {
        #expect(MonarchOverflow.visibleCount(total: 5, maxVisible: 3) == 3)
        #expect(MonarchOverflow.visibleCount(total: 4, maxVisible: 3) == 4)
        #expect(MonarchOverflow.visibleCount(total: 2, maxVisible: nil) == 2)
        #expect(MonarchOverflow.visibleCount(total: 2, maxVisible: -1) == 0)
        #expect(MonarchAvatarTokens.visibleCount(total: 4, maxVisible: 3) == 4)
    }

    @Test("Mention suggestions close after a spaced query found nothing")
    func mentionSuggestions() {
        let source = "@an a"
        let spaced = MonarchMentionQuery(trigger: "@", text: "an a", range: source.startIndex..<source.endIndex)
        #expect(!MonarchMentionSuggestions.isOpen(query: nil, status: .ready, itemCount: 3))
        #expect(!MonarchMentionSuggestions.isOpen(query: spaced, status: .ready, itemCount: 0))
        #expect(MonarchMentionSuggestions.isOpen(query: spaced, status: .loading, itemCount: 0))
    }

    @Test("Token counts are short and the token string follows the count")
    func aiContextFormat() {
        #expect(MonarchAiContextFormat.formatTokens(820) == "820")
        #expect(MonarchAiContextFormat.formatTokens(1200) == "1.2K")
        #expect(MonarchAiContextFormat.symbol(.file) == "doc")
        #expect(MonarchAiContextFormat.tokens("1", count: 1) != MonarchAiContextFormat.tokens("2", count: 2))
    }

    @Test("Tone chrome keeps neutral plain and marks other tones")
    func toneChrome() {
        #expect(MonarchToneChrome.border(.neutral) == \MonarchColorTokens.borderSubtle)
        #expect(MonarchToneChrome.border(.warning) == \MonarchColorTokens.warningBorder)
        #expect(MonarchToneChrome.titleColor(.neutral) == \MonarchColorTokens.textDefault)
        #expect(MonarchToneChrome.symbol(.neutral, override: nil) == nil)
        #expect(MonarchToneChrome.symbol(.danger, override: nil) == MonarchTonePalette.palette(for: .danger).symbol)
        #expect(MonarchToneChrome.symbol(.accent, override: nil) == MonarchTonePalette.palette(for: .accent).symbol)
        #expect(MonarchToneChrome.symbol(.neutral, override: "star") == "star")
    }

    @Test("The keyed cache stores and returns answers")
    func keyedCache() {
        let cache = MonarchKeyedCache<String, Int>()
        #expect(cache["PT"] == nil)
        cache.store(3, for: "PT")
        #expect(cache["PT"] == 3)
        #expect(cache.answers.count == 1)
    }

    @Test("The text diff finds the shared lines")
    func textDiff() {
        let script = MonarchTextDiff.sequences(["a", "b", "c"], ["a", "c", "d"])
        #expect(script == [.equal("a"), .delete("b"), .equal("c"), .insert("d")])
    }

    @Test("The sandboxed web view blocks the network for untrusted content")
    func sandbox() throws {
        let inert = MonarchSandboxedWebView.configuration(.inert)
        #expect(!inert.defaultWebpagePreferences.allowsContentJavaScript)
        #expect(!inert.websiteDataStore.isPersistent)
        #expect(!inert.preferences.javaScriptCanOpenWindowsAutomatically)
        let embed = MonarchSandboxedWebView.configuration(.embed)
        #expect(embed.defaultWebpagePreferences.allowsContentJavaScript)
        let rules = try #require(MonarchSandboxedWebView.blockingRules.data(using: .utf8))
        let parsed = try #require(try JSONSerialization.jsonObject(with: rules) as? [[String: Any]])
        let filters = parsed.compactMap { ($0["trigger"] as? [String: Any])?["url-filter"] as? String }
        #expect(filters == ["^https?://", "^file:"])
        #expect(parsed.allSatisfy { ($0["action"] as? [String: Any])?["type"] as? String == "block" })
    }

    // MARK: Round two

    @Test("Elapsed times and costs are written as the AI run views write them")
    func elapsedAndCost() {
        #expect(MonarchFormats.elapsed(0.45, locale: Self.english) == "450 ms")
        #expect(MonarchFormats.elapsed(2.5, locale: Self.english) == "2.5 s")
        #expect(MonarchFormats.elapsed(42, locale: Self.english) == "42 s")
        #expect(MonarchFormats.elapsed(120, locale: Self.english) == "2 min")
        #expect(MonarchFormats.elapsed(65, locale: Self.english) == "1 min 5 s")
        #expect(MonarchFormats.cost(micros: 2_500_000, currency: "USD", locale: Self.english) == "$2.50")
        #expect(MonarchFormats.cost(micros: 1_234, currency: "USD", locale: Self.english) == "$0.0012")
    }

    @Test("Geo search: flags, cached names and word search")
    func geoSearch() {
        #expect(MonarchGeoSearch.flag("pt") == "🇵🇹")
        #expect(MonarchGeoSearch.flag("P1") == "")
        #expect(MonarchGeoSearch.countryCodes.count == 249)
        let first = MonarchGeoSearch.name("PT", locale: Self.english)
        #expect(first == "Portugal")
        #expect(MonarchGeoSearch.name("PT", locale: Self.english) == first, "the second lookup comes from the cache")
        #expect(MonarchGeoSearch.name("PT", locale: Self.english, override: { _ in "Custom" }) == "Custom")
        #expect(MonarchCountrySelect.flag(for: "PT") == MonarchGeoSearch.flag("PT"))
        let option = MonarchComboboxOption(id: "PT", label: "Portugal", hint: "PT")
        #expect(MonarchGeoSearch.matchesEveryWord(option, query: "portu"))
        #expect(!MonarchGeoSearch.matchesEveryWord(option, query: "spain"))
    }

    @Test("The disclosure chevron turns a quarter when open, mirrored right to left")
    func chevron() {
        #expect(MonarchDisclosureChevron.rotation(isExpanded: false, layoutDirection: .leftToRight) == .zero)
        #expect(MonarchDisclosureChevron.rotation(isExpanded: true, layoutDirection: .leftToRight) == .degrees(90))
        #expect(MonarchDisclosureChevron.rotation(isExpanded: true, layoutDirection: .rightToLeft) == .degrees(-90))
    }

    @Test("Layout widths come from the generated layout and width tokens")
    func layoutWidths() {
        #expect(MonarchLayout.readingMax == CGFloat(MonarchTokens.Layout.readingMax))
        #expect(MonarchLayout.narrowWidth == CGFloat(MonarchTokens.Width.narrow))
        #expect(MonarchLayout.contentWidth == CGFloat(MonarchTokens.Width.content))
        #expect(MonarchLayout.wideWidth == CGFloat(MonarchTokens.Width.wide))
        #expect(MonarchLayout.readingMax == 672)
        #expect(MonarchLayout.selectedBorderWidth == 2 * MonarchLayout.borderWidth)
    }

    @Test("The small code role is monospaced at the caption size")
    func codeSmall() {
        let spec = MonarchTextStyle.codeSmall.spec
        #expect(spec.isMonospaced)
        #expect(spec.size == MonarchTextStyle.caption.spec.size)
        #expect(spec.size < MonarchTextStyle.code.spec.size)
    }
}
