import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAppearancePanelFixtures: ComponentFixtures {
    static let id = "appearance-panel"
    static let fixtures = [
        Fixture("default rows, device settings") {
            MonarchAppearancePanel(selection: .constant(MonarchAppearancePreferences()))
        },
        Fixture("every row with a title, every choice set") {
            MonarchAppearancePanel(
                "Appearance",
                selection: .constant(
                    MonarchAppearancePreferences(
                        scheme: .dark,
                        contrast: .more,
                        accent: MonarchTokens.Accents.chat,
                        motion: .reduce,
                        density: .compact
                    )
                ),
                rows: MonarchAppearancePanelRow.allCases
            )
        },
        Fixture("two rows, accent missing from the list") {
            MonarchAppearancePanel(
                selection: .constant(MonarchAppearancePreferences(accent: MonarchTokens.Accents.atlas)),
                rows: [.accent, .theme],
                accents: [MonarchTokens.Accents.rook, MonarchTokens.Accents.neutral]
            )
        },
        Fixture("disabled") {
            MonarchAppearancePanel(selection: .constant(MonarchAppearancePreferences())).disabled(true)
        },
        Fixture("translated, long text") {
            MonarchAppearancePanel(
                "Darstellung",
                selection: .constant(MonarchAppearancePreferences(contrast: .standard)),
                labels: MonarchAppearancePanelLabels(
                    theme: "Erscheinungsbild",
                    themeDescription: "Hell, dunkel oder so, wie das Gerät eingestellt ist, auch wenn es sich ändert.",
                    light: "Hell",
                    dark: "Dunkel",
                    system: "Gerät",
                    contrast: "Kontrast",
                    contrastStandard: "Standard",
                    contrastMore: "Mehr",
                    contrastSystem: "Gerät",
                    densityComfortable: "Großzügig",
                    densityCompact: "Kompakt"
                )
            )
        },
        Fixture("no rows") {
            MonarchAppearancePanel("Appearance", selection: .constant(MonarchAppearancePreferences()), rows: [])
        },
    ]
}

@Suite("MonarchAppearancePanel")
struct MonarchAppearancePanelTests {
    typealias Tokens = MonarchAppearancePanelTokens
    private let labels = MonarchAppearancePanelLabels()

    @Test("The defaults follow the device settings and use the default accent")
    func defaults() {
        let preferences = MonarchAppearancePreferences()
        #expect(preferences.scheme == .system)
        #expect(preferences.contrast == .system)
        #expect(preferences.motion == .system)
        #expect(preferences.density == .comfortable)
        #expect(preferences.accent == MonarchTokens.Accents.rook)
        #expect(preferences.appearance == MonarchAppearance())
    }

    @Test("The appearance carries scheme, contrast and accent into the theme")
    func appearance() {
        let preferences = MonarchAppearancePreferences(
            scheme: .dark,
            contrast: .more,
            accent: MonarchTokens.Accents.calendar
        )
        let appearance = preferences.appearance
        #expect(appearance.scheme == .dark)
        #expect(appearance.contrast == .more)
        #expect(appearance.accent.id == "calendar")
        let tokens = appearance.resolve(systemIsDark: false, systemIncreasesContrast: false)
        #expect(tokens.isDark)
        #expect(tokens.isHighContrast)
    }

    @Test("Reduce always reduces motion; system follows the device")
    func motion() {
        var preferences = MonarchAppearancePreferences()
        #expect(preferences.reducesMotion(systemReducesMotion: false) == false)
        #expect(preferences.reducesMotion(systemReducesMotion: true) == true)
        preferences.motion = .reduce
        #expect(preferences.reducesMotion(systemReducesMotion: false) == true)
        #expect(preferences.reducesMotion(systemReducesMotion: true) == true)
    }

    @Test("The standard rows are the web's four, in its order")
    func standardRows() {
        #expect(MonarchAppearancePanelRow.standard == [.theme, .contrast, .motion, .density])
    }

    @Test("Rows keep the requested order and appear once")
    func visibleRows() {
        #expect(Tokens.visibleRows([.density, .theme, .density, .accent, .theme]) == [.density, .theme, .accent])
        #expect(Tokens.visibleRows([]).isEmpty)
    }

    @Test("Choices are in the web's order")
    func order() {
        #expect(Tokens.contrastOrder == [.standard, .more, .system])
        #expect(Tokens.motionOrder == [.system, .reduce])
        #expect(Tokens.densityOrder == [.comfortable, .compact])
    }

    @Test("The built-in strings resolve")
    func builtInStrings() {
        let titles = MonarchAppearancePanelRow.allCases.map { Tokens.title($0, labels: labels) }
        #expect(titles == ["Theme", "Contrast", "Accent", "Motion", "Density"])
        for section in MonarchAppearancePanelRow.allCases {
            let description = Tokens.description(section, labels: labels)
            #expect(!description.isEmpty)
            #expect(!description.hasPrefix("monarch."))
        }
        #expect(Tokens.contrastOrder.map { Tokens.name($0, labels: labels) } == ["Standard", "More", "System"])
        #expect(Tokens.motionOrder.map { Tokens.name($0, labels: labels) } == ["System", "Reduce"])
        #expect(Tokens.densityOrder.map { Tokens.name($0, labels: labels) } == ["Comfortable", "Compact"])
        #expect(Tokens.description(.motion, labels: labels) == "Reduce animation and movement.")
    }

    @Test("Overrides replace only the strings given")
    func overrides() {
        let custom = MonarchAppearancePanelLabels(
            contrast: "Kontrast",
            contrastMore: "Mehr",
            densityDescription: "Enger"
        )
        #expect(Tokens.title(.contrast, labels: custom) == "Kontrast")
        #expect(Tokens.title(.theme, labels: custom) == "Theme")
        #expect(Tokens.name(MonarchContrastPreference.more, labels: custom) == "Mehr")
        #expect(Tokens.name(MonarchContrastPreference.standard, labels: custom) == "Standard")
        #expect(Tokens.description(.density, labels: custom) == "Enger")
    }

    @Test("The theme row names its toggle and passes only the theme names given")
    func themeToggleLabels() {
        let builtIn = Tokens.themeToggleLabels(labels)
        #expect(builtIn.title == "Theme")
        #expect(builtIn.light == nil)
        let custom = Tokens.themeToggleLabels(MonarchAppearancePanelLabels(theme: "Erscheinungsbild", dark: "Dunkel"))
        #expect(custom.title == "Erscheinungsbild")
        #expect(custom.dark == "Dunkel")
        #expect(custom.system == nil)
    }

    @Test("The accent menu always offers the current accent, and each accent once")
    func accentChoices() {
        let rook = MonarchTokens.Accents.rook
        let atlas = MonarchTokens.Accents.atlas
        #expect(Tokens.accentChoices([rook, atlas, rook], current: atlas).map(\.id) == ["rook", "atlas"])
        #expect(Tokens.accentChoices([rook], current: atlas).map(\.id) == ["rook", "atlas"])
        #expect(Tokens.accentChoices([], current: rook).map(\.id) == ["rook"])
        #expect(Tokens.accent(id: "atlas", in: [rook, atlas]) == atlas)
        #expect(Tokens.accent(id: "nope", in: [rook, atlas]) == nil)
    }

    @Test("The default accents are every token accent")
    func defaultAccents() {
        #expect(
            Tokens.accentChoices(MonarchTokens.Accents.all, current: MonarchTokens.Accents.rook).count
                == MonarchTokens.Accents.all.count
        )
    }
}
