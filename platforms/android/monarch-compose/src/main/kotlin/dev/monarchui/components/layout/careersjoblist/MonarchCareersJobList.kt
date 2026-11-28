package dev.monarchui.components.layout.careersjoblist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.datadisplay.tag.MonarchTagSize
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.internal.MonarchSmallCapsLabel
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A list of open roles: a heading with the number of positions, one card per role (department, title, location,
 * type, experience, salary, a two-line summary and the must-have skills), and the details of a role in a bottom
 * sheet with its duties, requirements, skills, benefits and an apply button.
 *
 * Built from `MonarchCard`, `MonarchTag`, `MonarchButton` and `MonarchBottomSheet`. Each card is one button that
 * opens the details; which role is open is hoisted in [openJob]. Cards are stacked on a phone and placed in up to
 * [columns] per row when each can stay 288dp wide.
 *
 * @param jobs the roles, in display order.
 * @param openJob the id of the role whose details are shown, or `null`.
 * @param onOpenJobChange called with the id of the role the user opens, or `null` when the details close.
 * @param onApply called with the role when the user taps the apply button.
 * @param modifier the modifier of the list.
 * @param columns the most cards per row on a wide screen.
 * @param organizationName the hiring organisation, in the line above the apply button.
 * @param contactEmail where applications go by email; a role without an application link is applied to by email.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchCareersJobList(
    jobs: List<MonarchCareerJob>,
    openJob: String?,
    onOpenJobChange: (String?) -> Unit,
    onApply: (MonarchCareerJob) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    organizationName: String? = null,
    contactEmail: String? = null,
    labels: MonarchCareersJobListLabels = MonarchCareersJobListLabels(),
) {
    val colors = MonarchTheme.colors
    val gap = MonarchCareersJobListTokens.Gap
    Column(
        modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonarchText(
                text = monarchString(labels.title, R.string.monarch_careersjoblist_title),
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.Heading,
                heading = true,
            )
            if (jobs.isNotEmpty()) {
                MonarchText(
                    text = pluralStringResource(R.plurals.monarch_careersjoblist_count, jobs.size, jobs.size),
                    style = MonarchTextStyle.Label,
                    color = MonarchCareersJobListTokens.muted(colors),
                )
            }
        }
        if (jobs.isEmpty()) {
            MonarchCard {
                MonarchText(
                    text = monarchString(labels.empty, R.string.monarch_careersjoblist_empty),
                    color = MonarchCareersJobListTokens.muted(colors),
                )
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fit = ((maxWidth + gap) / (MonarchCareersJobListTokens.MinCardWidth + gap)).toInt()
                val perRow = columns.coerceAtMost(fit).coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    jobs.chunked(perRow).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            row.forEach { job ->
                                JobCard(job, Modifier.weight(1f).fillMaxHeight()) { onOpenJobChange(job.id) }
                            }
                            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
    val open = jobs.firstOrNull { it.id == openJob }
    if (open != null) {
        JobDetails(open, organizationName, contactEmail, labels, onDismiss = { onOpenJobChange(null) }, onApply)
    }
}

private fun MonarchCareerJob.meta(): List<String> = listOfNotNull(location, employmentType, experience)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobCard(job: MonarchCareerJob, modifier: Modifier, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    MonarchCard(onClick = onClick, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.LineGap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (job.department != null) MonarchSmallCapsLabel(text = job.department)
                }
                MonarchIcon(
                    imageVector = MonarchGlyphs.KeyboardArrowRight,
                    contentDescription = null,
                    size = MonarchIconSize.Sm,
                    tint = MonarchCareersJobListTokens.chevron(colors),
                )
            }
            MonarchText(text = job.title, style = MonarchTextStyle.Subheading, heading = true)
            Meta(job.meta())
            if (job.salary != null) {
                MonarchText(
                    text = job.salary,
                    style = MonarchTextStyle.Caption,
                    color = MonarchCareersJobListTokens.muted(colors),
                )
            }
            if (job.summary != null) {
                MonarchText(
                    text = job.summary,
                    modifier = Modifier.padding(top = MonarchCareersJobListTokens.LineGap),
                    style = MonarchTextStyle.Label,
                    color = MonarchCareersJobListTokens.muted(colors),
                    maxLines = MonarchCareersJobListTokens.SUMMARY_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Tags(job.mustHaveSkills, Modifier.padding(top = MonarchCareersJobListTokens.LineGap))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Meta(parts: List<String>) {
    if (parts.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.MetaGap)) {
        parts.forEach {
            MonarchText(
                text = it,
                style = MonarchTextStyle.Caption,
                color = MonarchCareersJobListTokens.muted(MonarchTheme.colors),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(skills: List<String>, modifier: Modifier = Modifier) {
    if (skills.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.TagGap),
        verticalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.TagGap),
    ) {
        skills.forEach { MonarchTag(text = it, size = MonarchTagSize.Sm) }
    }
}

@Composable
private fun JobDetails(
    job: MonarchCareerJob,
    organizationName: String?,
    contactEmail: String?,
    labels: MonarchCareersJobListLabels,
    onDismiss: () -> Unit,
    onApply: (MonarchCareerJob) -> Unit,
) {
    val colors = MonarchTheme.colors
    val applyLabel = when {
        job.applyLink != null -> monarchString(labels.apply, R.string.monarch_careersjoblist_apply)
        contactEmail != null -> monarchString(labels.applyByEmail, R.string.monarch_careersjoblist_apply_by_email)
        else -> null
    }
    MonarchBottomSheet(
        onDismissRequest = onDismiss,
        title = job.title,
        description = (listOfNotNull(job.department) + job.meta()).joinToString(" · "),
        closeButton = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = MonarchCareersJobListTokens.DetailsPadding),
            verticalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.DetailsGap),
        ) {
            if (job.salary != null) MonarchText(text = job.salary, style = MonarchTextStyle.BodyStrong)
            if (job.summary != null) MonarchText(text = job.summary, color = MonarchCareersJobListTokens.muted(colors))
            val responsibilities =
                monarchString(labels.responsibilities, R.string.monarch_careersjoblist_responsibilities)
            val qualifications = monarchString(labels.qualifications, R.string.monarch_careersjoblist_qualifications)
            val niceToHave = monarchString(labels.niceToHave, R.string.monarch_careersjoblist_nice_to_have)
            DetailList(responsibilities, job.responsibilities)
            DetailList(qualifications, job.qualifications)
            DetailTags(monarchString(labels.mustHave, R.string.monarch_careersjoblist_must_have), job.mustHaveSkills)
            DetailTags(niceToHave, job.niceToHaveSkills)
            DetailList(monarchString(labels.benefits, R.string.monarch_careersjoblist_benefits), job.benefits)
            if (applyLabel != null) {
                Column(verticalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.LineGap)) {
                    if (organizationName != null) {
                        MonarchText(
                            text = stringResource(R.string.monarch_careersjoblist_applying_to, organizationName),
                            style = MonarchTextStyle.Caption,
                            color = MonarchCareersJobListTokens.muted(colors),
                        )
                    }
                    MonarchButton(
                        text = applyLabel,
                        onClick = { onApply(job) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = MonarchButtonVariant.Primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailList(heading: String, items: List<String>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.LineGap)) {
        MonarchText(text = heading, style = MonarchTextStyle.BodyStrong, heading = true)
        items.forEach { MonarchText(text = "•  $it", color = MonarchCareersJobListTokens.muted(MonarchTheme.colors)) }
    }
}

@Composable
private fun DetailTags(heading: String, skills: List<String>) {
    if (skills.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MonarchCareersJobListTokens.TagGap)) {
        MonarchText(text = heading, style = MonarchTextStyle.BodyStrong, heading = true)
        Tags(skills)
    }
}
