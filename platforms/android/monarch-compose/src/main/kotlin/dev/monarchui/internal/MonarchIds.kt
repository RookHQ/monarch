package dev.monarchui.internal

/**
 * The first id "[prefix]-n", counting from 1, that is not in [existing]: the
 * id of a row an editor adds ("level-3", "condition-2"). Deterministic, so
 * tests and screenshots are stable.
 */
internal fun monarchFreshId(prefix: String, existing: Collection<String>): String {
    val taken = existing.toHashSet()
    return generateSequence(1) { it + 1 }.map { "$prefix-$it" }.first { it !in taken }
}
