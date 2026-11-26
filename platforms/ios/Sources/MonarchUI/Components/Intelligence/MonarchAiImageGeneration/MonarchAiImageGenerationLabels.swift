/// Replacements for the built-in strings of ``MonarchAiImageGeneration``.
/// Every value is optional; leave one out to keep the built-in English text.
/// Positions passed to the closures start at 1.
public struct MonarchAiImageGenerationLabels: Sendable {
    /// The spoken name of the whole group.
    public var label: String
    /// The status line while nothing is ready. Gets the formatted percentage, if known.
    public var pending: @Sendable (_ percent: String?) -> String
    /// The status line while previews refine. Gets the formatted percentage, if known.
    public var progressive: @Sendable (_ percent: String?) -> String
    /// The status line when finished. Gets the number of images.
    public var done: @Sendable (_ count: Int) -> String
    /// The message when generation failed, unless `error` is passed to the view.
    public var error: String
    /// Announced when generation finishes. Gets the number of images.
    public var announceDone: @Sendable (_ count: Int) -> String
    /// The retry button.
    public var retry: String
    /// The spoken name of an image whose alt text is empty.
    public var imageName: @Sendable (_ position: Int) -> String
    /// Shown in a slot whose image is missing or cannot be loaded.
    public var unavailable: String
    /// The label of an alt text field.
    public var altLabel: @Sendable (_ position: Int) -> String
    /// The error under an empty alt text field.
    public var altRequired: String
    /// The title of the button that chooses an image.
    public var select: String
    /// The spoken name of the button that chooses an image.
    public var selectName: @Sendable (_ position: Int) -> String
    /// The mark on the chosen image.
    public var selected: String
    /// The title of the button that asks for variations.
    public var variations: String
    /// The spoken name of the button that asks for variations.
    public var variationsName: @Sendable (_ position: Int) -> String

    /// Labels with optional replacements. Each parameter replaces the
    /// property of the same name.
    public init(
        label: String? = nil,
        pending: (@Sendable (_ percent: String?) -> String)? = nil,
        progressive: (@Sendable (_ percent: String?) -> String)? = nil,
        done: (@Sendable (_ count: Int) -> String)? = nil,
        error: String? = nil,
        announceDone: (@Sendable (_ count: Int) -> String)? = nil,
        retry: String? = nil,
        imageName: (@Sendable (_ position: Int) -> String)? = nil,
        unavailable: String? = nil,
        altLabel: (@Sendable (_ position: Int) -> String)? = nil,
        altRequired: String? = nil,
        select: String? = nil,
        selectName: (@Sendable (_ position: Int) -> String)? = nil,
        selected: String? = nil,
        variations: String? = nil,
        variationsName: (@Sendable (_ position: Int) -> String)? = nil
    ) {
        typealias Strings = MonarchAiImageGenerationStrings
        self.label = label ?? Strings.label
        self.pending = pending ?? { Strings.pending($0) }
        self.progressive = progressive ?? { Strings.progressive($0) }
        self.done = done ?? { Strings.done($0) }
        self.error = error ?? Strings.error
        self.announceDone = announceDone ?? { Strings.announceDone($0) }
        self.retry = retry ?? Strings.retry
        self.imageName = imageName ?? { Strings.imageName($0) }
        self.unavailable = unavailable ?? Strings.unavailable
        self.altLabel = altLabel ?? { Strings.altLabel($0) }
        self.altRequired = altRequired ?? Strings.altRequired
        self.select = select ?? Strings.select
        self.selectName = selectName ?? { Strings.selectName($0) }
        self.selected = selected ?? Strings.selected
        self.variations = variations ?? Strings.variations
        self.variationsName = variationsName ?? { Strings.variationsName($0) }
    }
}
