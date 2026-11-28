package dev.monarchui.components.layout.marketingsection

import androidx.compose.runtime.Immutable

/**
 * One question of a [MonarchFaq].
 *
 * @property question the question, the row the user taps to show the answer.
 * @property answer the answer, shown below the question when it is expanded.
 */
@Immutable
public class MonarchFaqItem(
    public val question: String,
    public val answer: String,
)
