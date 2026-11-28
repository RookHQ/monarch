/// The entry logic of ``MonarchTagInput``: splitting typed and pasted text,
/// checking each tag and matching suggestions. Pure, so it is tested
/// without a view.
enum MonarchTagInputModel {
    /// What decides whether a tag is accepted.
    struct Rules {
        /// Characters that end a tag, besides Return. New lines and tabs
        /// always do.
        var separators: Set<Character> = [","]
        /// The most tags, or `nil` for no limit.
        var maxCount: Int?
        /// Whether the same tag may appear twice (compared without case).
        var allowsDuplicates = false
        /// Cleans a tag before it is checked. Trims white space by default.
        var normalize: ((String) -> String)?
        /// Returns a message to reject a tag, or `nil` to accept it.
        var validate: ((String, [String]) -> String?)?
        /// The texts of the built-in messages.
        var labels = MonarchTagInputLabels()
    }

    /// The result of adding several tags.
    struct Outcome: Equatable {
        /// The list after the additions.
        var tags: [String]
        /// The tags that were added, in order.
        var added: [String] = []
        /// The raw parts that were rejected, to put back in the field.
        var rejected: [String] = []
        /// The message of the first rejection.
        var message: String?
    }

    /// Whether a character ends a tag.
    static func isSeparator(_ character: Character, rules: Rules) -> Bool {
        character == "\n" || character == "\t" || character == "\r\n" || rules.separators.contains(character)
    }

    /// The parts of a text between separators, empty parts included.
    static func split(_ text: String, rules: Rules) -> [String] {
        text.split(omittingEmptySubsequences: false) { isSeparator($0, rules: rules) }.map(String.init)
    }

    /// What a change of the typed text commits. When the new text has no
    /// separator nothing is committed. A paste (more than one character
    /// inserted at once) commits every part; typing commits the parts
    /// before the last separator and keeps the rest as typed text.
    static func process(old: String, new: String, rules: Rules) -> (complete: [String], remainder: String) {
        guard new.contains(where: { isSeparator($0, rules: rules) }) else { return ([], new) }
        let parts = split(new, rules: rules)
        if new.count - old.count > 1 {
            return (parts, "")
        }
        return (Array(parts.dropLast()), parts.last ?? "")
    }

    /// The default clean-up: white space trimmed from both ends.
    static func defaultNormalize(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Adds raw parts to a list, checking each one: empty parts are
    /// skipped, then duplicates, the maximum and the app's check reject.
    static func add(_ parts: [String], to tags: [String], rules: Rules) -> Outcome {
        var outcome = Outcome(tags: tags)
        for raw in parts {
            let tag = rules.normalize?(raw) ?? defaultNormalize(raw)
            guard !tag.isEmpty else { continue }
            if let message = rejection(of: tag, in: outcome.tags, rules: rules) {
                outcome.rejected.append(defaultNormalize(raw))
                if outcome.message == nil { outcome.message = message }
                continue
            }
            outcome.tags.append(tag)
            outcome.added.append(tag)
        }
        return outcome
    }

    /// Why a clean tag cannot be added, or `nil` when it can.
    static func rejection(of tag: String, in tags: [String], rules: Rules) -> String? {
        if !rules.allowsDuplicates, tags.contains(where: { $0.caseInsensitiveCompare(tag) == .orderedSame }) {
            return rules.labels.duplicate?(tag) ?? MonarchTagInputStrings.duplicate(tag)
        }
        if let maxCount = rules.maxCount, tags.count >= maxCount {
            return rules.labels.maximum?(maxCount) ?? MonarchTagInputStrings.maximum(maxCount)
        }
        return rules.validate?(tag, tags)
    }

    /// The text left in the field after a commit: rejected parts joined by
    /// commas, then the part still being typed.
    static func leftover(rejected: [String], remainder: String) -> String {
        guard !rejected.isEmpty else { return remainder }
        let joined = rejected.joined(separator: ", ")
        return remainder.isEmpty ? joined : "\(joined), \(remainder)"
    }

    /// The suggestions that contain the typed text (ignoring case), without
    /// the tags already added, at most `limit`.
    static func matches(_ suggestions: [String], query: String, tags: [String], limit: Int) -> [String] {
        let query = defaultNormalize(query)
        guard !query.isEmpty else { return [] }
        let added = Set(tags.map { $0.lowercased() })
        let found = suggestions.filter { suggestion in
            !added.contains(suggestion.lowercased()) && suggestion.range(of: query, options: .caseInsensitive) != nil
        }
        return Array(found.prefix(limit))
    }

    /// What is announced after tags are added.
    static func addedAnnouncement(_ added: [String], labels: MonarchTagInputLabels) -> String? {
        guard !added.isEmpty else { return nil }
        return added.map { labels.added?($0) ?? MonarchTagInputStrings.added($0) }.joined(separator: " ")
    }
}
