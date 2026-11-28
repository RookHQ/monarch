import MonarchUI
import SwiftUI

struct MonarchLanguageSelectSection: CataloguePage {
    static let title = "Language select"

    @State private var language: String? = "de"
    @State private var subtitles: String?
    @State private var style = MonarchLanguageSelectLabelStyle.endonym

    var body: some View {
        CatalogueCaption("Every language under its own name, device languages first")
        Picker("Rows show first", selection: $style) {
            Text("Own name").tag(MonarchLanguageSelectLabelStyle.endonym)
            Text("Interface name").tag(MonarchLanguageSelectLabelStyle.localized)
        }
        .pickerStyle(.segmented)
        MonarchLanguageSelect(
            "Language",
            selection: $language,
            helper: "For emails and the app.",
            priority: Array(Locale.preferredLanguages.prefix(2)),
            labelStyle: style
        )
        Text("Value: \(language ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Translated languages only, with tags")
        MonarchLanguageSelect(
            "Subtitles",
            selection: $subtitles,
            prompt: "Choose a language",
            languages: ["en", "fr", "de", "es", "pt-BR", "ja", "ar", "zh-Hans"],
            showsTag: true
        )

        CatalogueCaption("Disabled")
        MonarchLanguageSelect("Language", selection: .constant("ja")).disabled(true)
    }
}
