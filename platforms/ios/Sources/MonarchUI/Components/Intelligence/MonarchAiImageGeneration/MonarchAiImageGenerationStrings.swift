import Foundation

/// Built-in strings of the image generation, from the `MonarchAiImageGeneration` strings table.
enum MonarchAiImageGenerationStrings {
    private static let table = "MonarchAiImageGeneration"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiImageGeneration." + key, table: table)
    }

    private static func string(_ key: String, _ argument: any CVarArg) -> String {
        MonarchStrings.string("monarch.aiImageGeneration." + key, table: table, argument)
    }

    /// The spoken name of the whole group.
    static var label: String { string("label") }
    /// The status line while nothing is ready, with an optional percentage.
    static func pending(_ percent: String?) -> String {
        percent.map { string("pendingProgress", $0) } ?? string("pending")
    }
    /// The status line while previews refine, with an optional percentage.
    static func progressive(_ percent: String?) -> String {
        percent.map { string("progressiveProgress", $0) } ?? string("progressive")
    }
    /// The status line when finished.
    static func done(_ count: Int) -> String {
        count == 1 ? string("doneOne") : string("doneOther", count)
    }
    /// The status line when generation failed.
    static var error: String { string("error") }
    /// Announced when generation finishes.
    static func announceDone(_ count: Int) -> String {
        count == 1 ? string("announceDoneOne") : string("announceDoneOther", count)
    }
    /// The retry button.
    static var retry: String { string("retry") }
    /// The spoken name of an image whose alt text is empty.
    static func imageName(_ index: Int) -> String { string("imageName", index) }
    /// Shown in a slot whose image is missing after generation or cannot be loaded.
    static var unavailable: String { string("unavailable") }
    /// The label of an alt text field.
    static func altLabel(_ index: Int) -> String { string("altLabel", index) }
    /// The error under an empty alt text field.
    static var altRequired: String { string("altRequired") }
    /// The title of the button that chooses an image.
    static var select: String { string("select") }
    /// The spoken name of the button that chooses an image.
    static func selectName(_ index: Int) -> String { string("selectName", index) }
    /// The mark on the chosen image.
    static var selected: String { string("selected") }
    /// The title of the button that asks for variations.
    static var variations: String { string("variations") }
    /// The spoken name of the button that asks for variations.
    static func variationsName(_ index: Int) -> String { string("variationsName", index) }
}
