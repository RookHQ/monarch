/// How strongly a toned surface is drawn. Components with `subtle`, `solid`
/// and `outline` variants map their public variant enum to this.
enum MonarchToneEmphasis: CaseIterable {
    /// A tinted background.
    case subtle
    /// A filled background.
    case solid
    /// A border and no fill.
    case outline
}
