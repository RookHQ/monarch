package com.rookhq.monarch.components.intelligence.aistructuredanswer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionItem
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionList
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionListLayout
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdown
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * A model's structured output shown as labelled fields (text, lists,
 * numbers, badges or Markdown) instead of a wall of prose: an interview
 * brief, a lead summary, a record extracted from a document. Fields fill in
 * as the answer streams, with a placeholder for each one still to come.
 *
 * A Monarch pattern built on `MonarchDescriptionList`, with `MonarchBadge`
 * for badges and `MonarchAiMarkdown` for Markdown, in a bordered card. The
 * schema decides the layout and the model only fills values, so answers look
 * the same every time; values are untrusted data, shown as text or as
 * Markdown without HTML. The card is labelled "AI answer" in words. TalkBack
 * reads the label and the title as one heading, each field as its label and
 * value, a field still to come as "Still generating" and an empty one as
 * "Not provided"; "Response complete." is announced once, through a polite
 * live region, when [streaming] turns `false`.
 *
 * @param schema the fields to show, in order. Values of [data] without a field are ignored.
 * @param data the values by key, usually parsed from the model's structured output.
 * @param modifier the modifier of the card.
 * @param layout one field per row, or two per row from 640dp wide.
 * @param streaming the answer is still arriving: missing fields show a placeholder instead of a dash.
 * @param title an optional title under the AI answer label, such as "Interview brief".
 * @param locale the locale of `Number` fields. `null` uses the configuration's locale.
 * @param icon an AI glyph before the AI answer label, as a `MonarchIcon` with a `null` content description.
 * Monarch bundles no icons.
 * @param onOpenLink opens the links of Markdown fields instead of the system browser.
 * @param labels replacements for the built-in strings.
 * @param actions controls under the fields, such as a copy button or feedback buttons.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchAiStructuredAnswer(
    schema: List<MonarchAiStructuredField>,
    data: Map<String, MonarchAiJSON>,
    modifier: Modifier = Modifier,
    layout: MonarchAiStructuredAnswerLayout = MonarchAiStructuredAnswerLayout.List,
    streaming: Boolean = false,
    title: String? = null,
    locale: Locale? = null,
    icon: (@Composable () -> Unit)? = null,
    onOpenLink: ((url: String) -> Unit)? = null,
    labels: MonarchAiStructuredAnswerLabels = MonarchAiStructuredAnswerLabels(),
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchAiStructuredAnswerTokens
    val colors = MonarchTheme.colors
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    val aiAnswer = monarchString(labels.aiAnswer, R.string.monarch_aistructuredanswer_ai_answer)
    val pending = monarchString(labels.pending, R.string.monarch_aistructuredanswer_pending)
    val empty = monarchString(labels.empty, R.string.monarch_aistructuredanswer_empty)
    val complete = monarchString(labels.complete, R.string.monarch_aistructuredanswer_complete)
    var announcement by remember { mutableStateOf("") }
    var wasStreaming by remember { mutableStateOf(streaming) }
    LaunchedEffect(streaming) {
        announcement = if (wasStreaming && !streaming) complete else ""
        wasStreaming = streaming
    }
    val grid = layout == MonarchAiStructuredAnswerLayout.Grid
    Column(
        modifier = modifier
            .background(tokens.containerColor(colors), MonarchShapeDefaults.Container)
            .border(MonarchShapeDefaults.BorderWidth, tokens.borderColor(colors), MonarchShapeDefaults.Container)
            .padding(tokens.Padding)
            .semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
    ) {
        Header(aiAnswer, title, icon, if (streaming) pending else null)
        MonarchDescriptionList(
            items = schema.map { field ->
                val value = data[field.key]
                val present = value?.takeUnless { tokens.isMissing(it) }
                val content: (@Composable () -> Unit)? = when {
                    present != null -> ({ FieldValue(field, present, shownLocale, onOpenLink) })
                    streaming -> ({ Placeholder(pending) })
                    else -> null
                }
                MonarchDescriptionItem(
                    term = field.label,
                    value = present?.let { tokens.asText(it) },
                    wide = tokens.isWide(field),
                    content = content,
                )
            },
            layout = if (grid) MonarchDescriptionListLayout.Stacked else MonarchDescriptionListLayout.Inline,
            columns = if (grid) tokens.GRID_COLUMNS else 1,
            emptyLabel = empty,
        )
        if (actions != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
                verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            ) { actions() }
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The AI answer label and the title, read as one heading. */
@Composable
private fun Header(aiAnswer: String, title: String?, icon: (@Composable () -> Unit)?, busy: String?) {
    val tokens = MonarchAiStructuredAnswerTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            heading()
            if (busy != null) stateDescription = busy
        },
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                CompositionLocalProvider(LocalContentColor provides tokens.eyebrowColor(colors), content = icon)
            }
            Text(text = aiAnswer, style = MonarchTheme.typography.caption, color = tokens.eyebrowColor(colors))
        }
        if (!title.isNullOrBlank()) {
            Text(text = title, style = MonarchTheme.typography.subheading, color = colors.textDefault)
        }
    }
}

/** The bar of a field still to come, read as "Still generating" after the field's label. */
@Composable
private fun Placeholder(pending: String) {
    val tokens = MonarchAiStructuredAnswerTokens
    Box(
        modifier = Modifier
            .fillMaxWidth(tokens.PLACEHOLDER_FRACTION)
            .height(tokens.PlaceholderHeight)
            .background(tokens.placeholderColor(MonarchTheme.colors), MonarchShapeDefaults.Row)
            .clearAndSetSemantics { text = AnnotatedString(pending) },
    )
}

/** The value of one field, as its type says. */
@Composable
private fun FieldValue(
    field: MonarchAiStructuredField,
    value: MonarchAiJSON,
    locale: Locale,
    onOpenLink: ((String) -> Unit)?,
) {
    val tokens = MonarchAiStructuredAnswerTokens
    val colors = MonarchTheme.colors
    when (field.type) {
        MonarchAiStructuredFieldType.Text -> BodyText(tokens.asText(value))
        MonarchAiStructuredFieldType.Number -> BodyText(tokens.numberText(value, field.format, locale))
        MonarchAiStructuredFieldType.List -> Column(verticalArrangement = Arrangement.spacedBy(tokens.SmallGap)) {
            tokens.asList(value).forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.BulletGap)) {
                    Text(
                        text = tokens.BULLET,
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MonarchTheme.typography.body,
                        color = colors.textMuted,
                    )
                    BodyText(item)
                }
            }
        }
        MonarchAiStructuredFieldType.Badge -> FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        ) {
            tokens.asList(value).forEach { item ->
                MonarchBadge(text = item, tone = field.tone?.invoke(item) ?: MonarchTone.Neutral)
            }
        }
        MonarchAiStructuredFieldType.Markdown -> MonarchAiMarkdown(text = tokens.asText(value), onOpenLink = onOpenLink)
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textDefault)
}
