package com.rookhq.monarch.components.intelligence.aihandoff

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.internal.MonarchRelativeTime
import java.util.Locale

/** The pure text logic of MonarchAiHandoff. */
internal object MonarchAiHandoffLogic {
    /** "Research agent (AI agent)": an agent's name with its kind, so it is never taken for a person. */
    fun spokenName(name: String, agent: Boolean, agentKind: String): String =
        if (agent) "$name ($agentKind)" else name

    /** The spoken text of the marker: the label, the sentence, the reason and the time, in that order. */
    fun spoken(label: String, sentence: String, reason: String?, reasonPrefix: String, time: String?): String =
        listOfNotNull(
            label,
            sentence,
            reason?.takeIf { it.isNotBlank() }?.let { "$reasonPrefix $it" },
            time,
        ).joinToString(". ")

    /** The sentence with every occurrence of [names] in the strong weight. */
    fun emphasized(sentence: String, names: List<String>): AnnotatedString = buildAnnotatedString {
        append(sentence)
        val style = SpanStyle(fontWeight = FontWeight(MonarchAiHandoffTokens.NAME_WEIGHT))
        names.filter { it.isNotEmpty() }.distinct().forEach { name ->
            var from = sentence.indexOf(name)
            while (from >= 0) {
                addStyle(style, from, from + name.length)
                from = sentence.indexOf(name, from + name.length)
            }
        }
    }

    /** "3 minutes ago", in the default locale, by the shared Monarch rule (the web rule). */
    fun relativeTime(dateMillis: Long, nowMillis: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(dateMillis, nowMillis, locale)
}
