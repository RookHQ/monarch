/// One step of ``MonarchAiOnboardingTour``.
public struct MonarchAiOnboardingTourStep: Identifiable, Sendable, Hashable {
    /// A stable id, unique among the steps.
    public let id: String
    /// A short heading, such as "What the assistant can see". Already localised.
    public let title: String
    /// One or two sentences. Already localised.
    public let body: String
    /// An SF Symbol shown above the heading.
    public let systemImage: String?

    /// A tour step.
    ///
    /// - Parameters:
    ///   - title: A short heading, already localised.
    ///   - id: A stable id. The title when `nil`.
    ///   - body: One or two sentences, already localised.
    ///   - systemImage: An SF Symbol shown above the heading.
    public init(_ title: String, id: String? = nil, body: String, systemImage: String? = nil) {
        self.id = id ?? title
        self.title = title
        self.body = body
        self.systemImage = systemImage
    }
}
