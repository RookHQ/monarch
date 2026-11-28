/// How strongly a recording indicator is drawn.
public enum MonarchRecordingIndicatorVariant: Sendable, Hashable, CaseIterable {
    /// A tinted capsule in the danger colour, for the app's own surfaces.
    /// The default.
    case subtle
    /// A filled capsule in the inverse colour, for placing over video.
    case solid
}
