@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingOption
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingPicker
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdown
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerOrientation
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The catalogue page of AiEvalReview: a recipe of existing components on the
 * list-detail pattern. The queue with its progress, and for the chosen output
 * its prompt, output, expected answer, rubric, notes and verdict.
 *
 * Apps use the Material NavigableListDetailPaneScaffold, as the inventory
 * example shows. The catalogue app does not depend on the adaptive artifacts,
 * so, like SplitLayoutSection, this page shows the same behaviour with a Row
 * and BackHandler: both panes from 600dp, one pane at a time below it.
 */
object AiEvalReviewSection : CataloguePage {
    override val title = "AiEvalReview"

    private const val HEIGHT_STEPS = 6
    private const val MAX_SCALE = 5
    private const val LIST_SHARE = 0.4f
    private val MediumWidth = 600.dp

    private class EvalItem(
        val id: String,
        val title: String,
        val prompt: String,
        val output: String,
        val reference: String? = null,
    )

    private class Criterion(
        val id: String,
        val label: String,
        val description: String? = null,
        val scale: Int = MAX_SCALE,
    )

    private val items = listOf(
        EvalItem(
            id = "q1",
            title = "Summarize a meeting",
            prompt = "Summarize the attached meeting notes in three bullet points.",
            output = "- The launch moves to **next Tuesday**.\n- Design owns the final copy review.\n" +
                "- Support needs the FAQ by Friday.",
            reference = "Launch moved to Tuesday; design reviews copy; support needs the FAQ by Friday.",
        ),
        EvalItem(
            id = "q2",
            title = "Answer a policy question",
            prompt = "Can I expense a train ticket for a client visit?",
            output = "Yes. Travel for client visits is reimbursable with a receipt, up to the standard fare.",
        ),
        EvalItem(
            id = "q3",
            title = "Draft a reply",
            prompt = "Draft a short, polite reply declining the invitation.",
            output = "Thank you for the invitation. Unfortunately I can’t join this time, but I hope it goes well.",
        ),
    )

    private val rubric = listOf(
        Criterion("accuracy", "Accuracy", "Facts match the source or reference."),
        Criterion("completeness", "Completeness", "Covers everything asked."),
        Criterion("tone", "Tone", scale = 3),
    )

    /** The reviews, held by the page; an app persists every change. */
    private class Reviews(
        val scores: Map<String, Map<String, Int>>,
        val notes: Map<String, String>,
        val verdicts: Map<String, String>,
    )

    @Composable
    override fun Content() {
        // An app keeps these in its view model and saves every change.
        var scores by remember { mutableStateOf(mapOf<String, Map<String, Int>>()) }
        var notes by remember { mutableStateOf(mapOf<String, String>()) }
        var verdicts by remember { mutableStateOf(mapOf<String, String>()) }
        var current by rememberSaveable { mutableStateOf(items.first().id) }
        var open by rememberSaveable { mutableStateOf(false) }
        val reviews = Reviews(scores, notes, verdicts)
        CatalogueCaption("Two panes from 600dp, one pane below")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS)) {
            val width = maxWidth
            val twoPanes = width >= MediumWidth
            BackHandler(enabled = !twoPanes && open) { open = false }
            val queue = @Composable { modifier: Modifier ->
                Queue(reviews, current, modifier) {
                    current = it
                    open = true
                }
            }
            val detail = @Composable {
                Detail(
                    reviews = reviews,
                    current = current,
                    onCurrentChange = { current = it },
                    onScore = { id, next -> scores = scores + (id to next) },
                    onNotes = { id, text -> notes = notes + (id to text) },
                    onVerdict = { id, verdict -> verdicts = verdicts + (id to verdict) },
                )
            }
            when {
                twoPanes -> Row {
                    queue(Modifier.width(width * LIST_SHARE))
                    MonarchDivider(Modifier.fillMaxHeight(), orientation = MonarchDividerOrientation.Vertical)
                    detail()
                }
                open -> detail()
                else -> queue(Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun Queue(reviews: Reviews, current: String, modifier: Modifier, onSelect: (String) -> Unit) {
        val reviewed = items.count { it.id in reviews.verdicts }
        LazyColumn(modifier = modifier) {
            item(key = "progress") {
                MonarchProgress(
                    progress = reviewed.toFloat() / items.size,
                    label = "$reviewed of ${items.size} reviewed",
                )
            }
            items(items = items, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = {
                        Text(
                            when (reviews.verdicts[item.id]) {
                                "pass" -> "Pass"
                                "fail" -> "Fail"
                                else -> "Not reviewed"
                            },
                        )
                    },
                    modifier = Modifier.selectable(selected = item.id == current) { onSelect(item.id) },
                )
            }
        }
    }

    @Suppress("LongParameterList")
    @Composable
    private fun Detail(
        reviews: Reviews,
        current: String,
        onCurrentChange: (String) -> Unit,
        onScore: (String, Map<String, Int>) -> Unit,
        onNotes: (String, String) -> Unit,
        onVerdict: (String, String) -> Unit,
    ) {
        val index = items.indexOfFirst { it.id == current }.coerceAtLeast(0)
        val item = items[index]
        val itemScores = reviews.scores[item.id].orEmpty()
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(MonarchTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
        ) {
            MonarchText(text = "${index + 1} of ${items.size}", style = MonarchTextStyle.Caption)
            MonarchText(text = item.title, style = MonarchTextStyle.Heading)
            MonarchText(text = "Prompt", style = MonarchTextStyle.Subheading)
            MonarchText(text = item.prompt)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(text = "Output", style = MonarchTextStyle.Subheading)
                MonarchAiGeneratedBadge(size = MonarchAiGeneratedBadgeSize.Sm)
            }
            MonarchAiMarkdown(text = item.output)
            item.reference?.let { reference ->
                MonarchText(text = "Expected", style = MonarchTextStyle.Subheading)
                MonarchText(text = reference)
            }
            MonarchText(text = "Rubric", style = MonarchTextStyle.Subheading)
            MonarchText(
                text = "Score ${itemScores.values.sum()} / ${rubric.sumOf { it.scale }}",
                style = MonarchTextStyle.BodyStrong,
            )
            rubric.forEach { criterion ->
                criterion.description?.let { MonarchText(text = it, style = MonarchTextStyle.Caption) }
                MonarchRatingPicker(
                    value = itemScores[criterion.id],
                    onValueChange = { score ->
                        val next = if (score == null) {
                            itemScores - criterion.id
                        } else {
                            itemScores + (criterion.id to score)
                        }
                        onScore(item.id, next)
                    },
                    label = criterion.label,
                    options = MonarchRatingOption.range(1, criterion.scale),
                    clearable = true,
                )
            }
            MonarchTextField(
                value = reviews.notes[item.id].orEmpty(),
                onValueChange = { onNotes(item.id, it) },
                label = "Notes",
                singleLine = false,
            )
            MonarchText(text = "Verdict", style = MonarchTextStyle.Subheading)
            MonarchSegmentedControl(modifier = Modifier.fillMaxWidth()) {
                listOf("pass" to "Pass", "fail" to "Fail").forEachIndexed { position, (value, label) ->
                    Segment(
                        selected = reviews.verdicts[item.id] == value,
                        onClick = { onVerdict(item.id, value) },
                    ) { Text(label) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchButton(
                    text = "Previous",
                    onClick = { onCurrentChange(items[index - 1].id) },
                    size = MonarchButtonSize.Sm,
                    enabled = index > 0,
                )
                MonarchButton(
                    text = "Next",
                    onClick = { onCurrentChange(items[index + 1].id) },
                    size = MonarchButtonSize.Sm,
                    enabled = index < items.lastIndex,
                )
            }
            MonarchButton(
                text = "Submit reviews",
                onClick = { /* The app sends the reviews to its evaluation service. */ },
                variant = MonarchButtonVariant.Primary,
                enabled = reviews.verdicts.size == items.size,
            )
        }
    }
}
