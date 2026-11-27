/// One step of a ``MonarchOnboardingChecklist``.
///
/// The app decides when a step is done, usually from its own data, and
/// passes a new value.
public struct MonarchOnboardingStep: Identifiable, Sendable, Hashable {
    /// A stable id, unique in the checklist.
    public var id: String
    /// What to do, already localised, such as "Invite your team".
    public var title: String
    /// One line on why or how. Shown while the step is not done.
    public var description: String?
    /// Whether the step is complete.
    public var isDone: Bool

    /// A step.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique in the checklist.
    ///   - title: What to do, already localised.
    ///   - description: One line on why or how, shown while the step is not done.
    ///   - isDone: Whether the step is complete.
    public init(id: String, title: String, description: String? = nil, isDone: Bool = false) {
        self.id = id
        self.title = title
        self.description = description
        self.isDone = isDone
    }
}
