/// Whether a text or reasoning part is still being written.
public enum MonarchAiPartState: String, Sendable, Hashable, Codable, CaseIterable {
    /// More text is arriving.
    case streaming
    /// The part is complete.
    case done
}
