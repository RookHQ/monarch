package dev.monarchui.components.forms.taginput

/** Why a tag was not added. */
internal sealed interface MonarchTagRejection {
    /** The tag is already there (compared without case). */
    data class Duplicate(val tag: String) : MonarchTagRejection

    /** No more tags fit. */
    data class Maximum(val maximum: Int) : MonarchTagRejection

    /** The app's validation refused it, with its message. */
    data class Invalid(val message: String) : MonarchTagRejection
}

/**
 * The outcome of adding tags.
 *
 * @property tags the tags after the accepted ones were added.
 * @property added the accepted tags, in order.
 * @property rejected the refused texts, which stay in the field.
 * @property rejection why the first refused text was refused.
 */
internal data class MonarchTagCommit(
    val tags: List<String>,
    val added: List<String>,
    val rejected: List<String>,
    val rejection: MonarchTagRejection?,
)

/**
 * What the field does with an edit.
 *
 * @property parts texts to add now.
 * @property remainder the text left in the field.
 */
internal data class MonarchTagInputEdit(val parts: List<String>, val remainder: String)

/**
 * What a tag must meet to be added.
 *
 * @property normalize cleans typed text; trims white space when `null`.
 * @property maxCount the most tags, or `null` for no limit.
 * @property allowDuplicates whether the same tag (without case) may appear twice.
 * @property validate the app's check, which returns a message to refuse a tag.
 */
internal data class MonarchTagRules(
    val normalize: ((String) -> String)? = null,
    val maxCount: Int? = null,
    val allowDuplicates: Boolean = false,
    val validate: ((String, List<String>) -> String?)? = null,
)

/** The text logic of MonarchTagInput: splitting, checking and suggesting. */
internal object MonarchTagInputLogic {
    /** The most suggestions shown at once. */
    const val MAX_SUGGESTIONS: Int = 8

    private val alwaysSplitOn = setOf('\n', '\t')

    /** The separator joining refused texts left in the field. */
    const val REJECTED_JOIN: String = ", "

    /** Whether [text] contains a separator, a new line or a tab. */
    fun hasSeparator(text: String, separators: Set<Char>): Boolean =
        text.any { it in separators || it in alwaysSplitOn }

    /** Splits [text] on the separators, new lines and tabs. */
    fun split(text: String, separators: Set<Char>): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        text.forEach { char ->
            if (char in separators || char in alwaysSplitOn) {
                parts += current.toString()
                current.clear()
            } else {
                current.append(char)
            }
        }
        parts += current.toString()
        return parts
    }

    /**
     * Reads an edit from [old] to [new]. Typing a separator adds what comes
     * before it; inserting several characters at once that contain a
     * separator (a paste) adds every part.
     */
    fun input(old: String, new: String, separators: Set<Char>): MonarchTagInputEdit {
        if (!hasSeparator(new, separators)) return MonarchTagInputEdit(emptyList(), new)
        val parts = split(new, separators)
        val pasted = new.length - old.length > 1
        return if (pasted) {
            MonarchTagInputEdit(parts, "")
        } else {
            MonarchTagInputEdit(parts.dropLast(1), parts.last())
        }
    }

    /**
     * Adds [parts] to [tags] one by one. Each part is cleaned with the
     * normalize rule (trimmed by default); empty parts are ignored; then the
     * duplicate check, the maximum and the app's validation can refuse it.
     */
    fun commit(parts: List<String>, tags: List<String>, rules: MonarchTagRules): MonarchTagCommit {
        val result = tags.toMutableList()
        val added = mutableListOf<String>()
        val rejected = mutableListOf<String>()
        var rejection: MonarchTagRejection? = null
        parts.forEach { part ->
            val tag = rules.normalize?.invoke(part) ?: part.trim()
            if (tag.isEmpty()) return@forEach
            val refusal = check(tag, result, rules)
            if (refusal == null) {
                result += tag
                added += tag
            } else {
                rejected += part.trim()
                if (rejection == null) rejection = refusal
            }
        }
        return MonarchTagCommit(result, added, rejected, rejection)
    }

    /** Why [tag] cannot be added to [tags], or `null` when it can. */
    fun check(tag: String, tags: List<String>, rules: MonarchTagRules): MonarchTagRejection? {
        val max = rules.maxCount
        return when {
            !rules.allowDuplicates && tags.any { it.equals(tag, ignoreCase = true) } ->
                MonarchTagRejection.Duplicate(tag)
            max != null && tags.size >= max -> MonarchTagRejection.Maximum(max)
            else -> rules.validate?.invoke(tag, tags)?.let { MonarchTagRejection.Invalid(it) }
        }
    }

    /**
     * The suggestions to offer for the typed text: those that contain it
     * (without case) and are not added yet, at most [MAX_SUGGESTIONS]. None
     * while nothing is typed.
     */
    fun suggestions(suggestions: List<String>, typed: String, tags: List<String>): List<String> {
        val query = typed.trim()
        if (query.isEmpty()) return emptyList()
        return suggestions
            .filter { it.contains(query, ignoreCase = true) && tags.none { tag -> tag.equals(it, ignoreCase = true) } }
            .distinct()
            .take(MAX_SUGGESTIONS)
    }
}
