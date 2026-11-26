package com.rookhq.monarch.components.intelligence.aiclaimsources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.intelligence.aicitation.MonarchAiCitationTokens
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Where an assistant's answer came from: its claims, how well the sources
 * back each one in plain words, and, for an open claim, the exact passages
 * with their source, location, date and stance.
 *
 * Built by Monarch from a `Column`: Material 3 has no equivalent. The filter
 * is a `MonarchSegmentedControl`; each claim is a
 * disclosure like `MonarchCollapsible` whose header also shows the support
 * level as a `MonarchBadge` and the number of passages, so they stay visible
 * while the claim is closed. Passages have Open source and Copy quote
 * (`MonarchCopyButton`); open claims can have Check again and Report a
 * problem. Claims without a source, or with sources that disagree, are stated
 * in words and never hidden. A filter change is announced through a polite
 * live region. The claims are not lazy: the view sits under an answer in the
 * app's scrolling conversation or in a sheet.
 *
 * Open claims, the filter and the selected claim are hoisted when the app
 * passes them, and kept by the view when it passes `null`.
 *
 * @param claims the claims of the answer, in the order the answer makes them.
 * @param sources the sources the passages point at.
 * @param modifier the modifier of the view.
 * @param expandedIds the ids of the open claims, when the app keeps them. `null` keeps them in the view, all closed
 * at first.
 * @param onExpandedChange called with the new set of open claim ids when a claim opens or closes.
 * @param filter the filter, when the app keeps it. `null` keeps it in the view, starting with every claim.
 * @param onFilterChange called when the person changes the filter.
 * @param selectedClaimId the selected claim, such as the sentence the person tapped in the answer, when the app
 * keeps it. `null` keeps the last claim the person opened or closed.
 * @param onSelectClaim called with a claim's id when the person opens or closes it, so the app can highlight that
 * sentence in the answer.
 * @param onOpenSource called when the person opens a passage's source; shows Open source on every passage with a
 * known source. Without it, Open source shows only for an http, https or mailto address and opens it outside the
 * app.
 * @param onFlag shows Report a problem in every open claim; called with the claim's id.
 * @param onVerify shows Check again in open, unchecked claims; called with the claim's id.
 * @param onCopyQuote called with the passage after Copy quote.
 * @param supportIcon an icon for a support level in its badge, hidden from TalkBack. Monarch bundles no icons;
 * the level is always words.
 * @param expandIcon an icon at the start of each claim's header, for example a chevron that points down; it points
 * down while the claim is open and turns a quarter turn to point to the end while it is closed.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiClaimSources(
    claims: List<MonarchAiClaim>,
    sources: List<MonarchAiClaimSource>,
    modifier: Modifier = Modifier,
    expandedIds: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    filter: MonarchAiClaimSourcesFilter? = null,
    onFilterChange: ((MonarchAiClaimSourcesFilter) -> Unit)? = null,
    selectedClaimId: String? = null,
    onSelectClaim: ((String) -> Unit)? = null,
    onOpenSource: ((MonarchAiClaimSource, MonarchAiClaimEvidence) -> Unit)? = null,
    onFlag: ((String) -> Unit)? = null,
    onVerify: ((String) -> Unit)? = null,
    onCopyQuote: ((MonarchAiClaimEvidence) -> Unit)? = null,
    supportIcon: (@Composable (MonarchAiClaimSupport) -> Unit)? = MonarchDefaultIcons.claimSupport,
    expandIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchAiClaimSourcesLabels = MonarchAiClaimSourcesLabels(),
) {
    val tokens = MonarchAiClaimSourcesTokens
    val colors = MonarchTheme.colors
    var ownExpanded by remember { mutableStateOf(emptySet<String>()) }
    var ownFilter by rememberSaveable { mutableStateOf(MonarchAiClaimSourcesFilter.All) }
    var ownSelected by rememberSaveable { mutableStateOf<String?>(null) }
    val expanded = expandedIds ?: ownExpanded
    val currentFilter = filter ?: ownFilter
    val selected = selectedClaimId ?: ownSelected
    val counts = MonarchAiClaimCounts.of(claims)
    val shown = tokens.shown(claims, currentFilter)
    val sourceById = remember(sources) { sources.associateBy { it.id } }
    val announcement = rememberFilterAnnouncement(currentFilter, shown.size, claims.size, labels)
    val heading = monarchString(labels.heading, R.string.monarch_aiclaimsources_heading)
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            Text(
                text = heading,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textDefault,
            )
            Text(
                text = labels.summary?.invoke(counts) ?: summaryText(counts),
                style = MonarchTheme.typography.caption,
                color = tokens.muted(colors),
            )
        }
        if (claims.isNotEmpty()) {
            FilterRow(
                filter = currentFilter,
                counts = counts,
                labels = labels,
                onFilterChange = { next ->
                    if (filter == null) ownFilter = next
                    onFilterChange?.invoke(next)
                },
            )
        }
        if (shown.isEmpty()) {
            Text(
                text = if (claims.isEmpty()) {
                    monarchString(labels.empty, R.string.monarch_aiclaimsources_empty)
                } else {
                    monarchString(labels.noneNeedChecking, R.string.monarch_aiclaimsources_none_need_checking)
                },
                style = MonarchTheme.typography.body,
                color = tokens.muted(colors),
            )
        } else {
            Column(
                modifier = Modifier.semantics {
                    contentDescription = heading
                    isTraversalGroup = true
                },
                verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            ) {
                shown.forEach { claim ->
                    ClaimItem(
                        claim = claim,
                        open = claim.id in expanded,
                        selected = claim.id == selected,
                        onToggle = {
                            val next = tokens.toggled(expanded, claim.id)
                            if (expandedIds == null) ownExpanded = next
                            onExpandedChange?.invoke(next)
                            if (selectedClaimId == null) ownSelected = claim.id
                            onSelectClaim?.invoke(claim.id)
                        },
                        sourceById = sourceById,
                        actions = ClaimActions(onOpenSource, onFlag, onVerify, onCopyQuote),
                        supportIcon = supportIcon,
                        expandIcon = expandIcon,
                        labels = labels,
                    )
                }
            }
        }
        // Always present, so a filter change is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The callbacks a claim and its passages offer. */
private class ClaimActions(
    val onOpenSource: ((MonarchAiClaimSource, MonarchAiClaimEvidence) -> Unit)?,
    val onFlag: ((String) -> Unit)?,
    val onVerify: ((String) -> Unit)?,
    val onCopyQuote: ((MonarchAiClaimEvidence) -> Unit)?,
)

@Composable
private fun FilterRow(
    filter: MonarchAiClaimSourcesFilter,
    counts: MonarchAiClaimCounts,
    labels: MonarchAiClaimSourcesLabels,
    onFilterChange: (MonarchAiClaimSourcesFilter) -> Unit,
) {
    val name = monarchString(labels.filterLabel, R.string.monarch_aiclaimsources_filter_label)
    val options = MonarchAiClaimSourcesFilter.entries
    MonarchSegmentedControl(modifier = Modifier.semantics { contentDescription = name }) {
        options.forEach { option ->
            val text = when (option) {
                MonarchAiClaimSourcesFilter.All -> monarchString(labels.all, R.string.monarch_aiclaimsources_all)
                MonarchAiClaimSourcesFilter.NeedsChecking ->
                    monarchString(labels.needsChecking, R.string.monarch_aiclaimsources_needs_checking)
            }
            val count = if (option == MonarchAiClaimSourcesFilter.All) counts.total else counts.needsChecking
            Segment(
                selected = option == filter,
                onClick = { if (option != filter) onFilterChange(option) },
                label = text,
                count = count,
            )
        }
    }
}

@Composable
private fun ClaimItem(
    claim: MonarchAiClaim,
    open: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    sourceById: Map<String, MonarchAiClaimSource>,
    actions: ClaimActions,
    supportIcon: (@Composable (MonarchAiClaimSupport) -> Unit)?,
    expandIcon: (@Composable () -> Unit)?,
    labels: MonarchAiClaimSourcesLabels,
) {
    val tokens = MonarchAiClaimSourcesTokens
    val stripe = claimStripe(claim.support, MonarchTheme.colors)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MonarchShapeDefaults.Container)
            .border(
                MonarchShapeDefaults.BorderWidth,
                MonarchTheme.colors.borderSubtle,
                MonarchShapeDefaults.Container,
            )
            .drawBehind { drawRect(stripe, size = Size(tokens.StripeWidth.toPx(), size.height)) }
            .padding(start = tokens.StripeWidth),
    ) {
        ClaimHeader(claim, open, selected, onToggle, supportIcon, expandIcon, labels)
        ClaimBody(claim, open, sourceById, actions, labels)
    }
}

@Composable
private fun ClaimBody(
    claim: MonarchAiClaim,
    open: Boolean,
    sourceById: Map<String, MonarchAiClaimSource>,
    actions: ClaimActions,
    labels: MonarchAiClaimSourcesLabels,
) {
    val tokens = MonarchAiClaimSourcesTokens
    val motion = MonarchTheme.motion
    Column {
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(motion.spec()) + fadeIn(motion.spec()),
            exit = shrinkVertically(motion.spec()) + fadeOut(motion.spec()),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            ) {
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                Column(
                    modifier = Modifier.padding(horizontal = tokens.PanelIndent, vertical = tokens.ItemGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                ) {
                    ClaimPanel(claim, sourceById, actions, labels)
                }
            }
        }
    }
}

@Composable
private fun ClaimHeader(
    claim: MonarchAiClaim,
    open: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    supportIcon: (@Composable (MonarchAiClaimSupport) -> Unit)?,
    expandIcon: (@Composable () -> Unit)?,
    labels: MonarchAiClaimSourcesLabels,
) {
    val tokens = MonarchAiClaimSourcesTokens
    val colors = MonarchTheme.colors
    val state = if (open) {
        monarchString(labels.expanded, R.string.monarch_aiclaimsources_expanded)
    } else {
        monarchString(labels.collapsed, R.string.monarch_aiclaimsources_collapsed)
    }
    val rotation by animateFloatAsState(
        targetValue = if (open) 0f else tokens.COLLAPSED_ROTATION,
        animationSpec = MonarchTheme.motion.spec(),
        label = "claim expand icon",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MonarchShapeDefaults.Row)
            .background(tokens.headerBackground(selected, colors))
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(interactionSource = null, indication = ripple(), role = Role.Button, onClick = onToggle)
            .semantics {
                stateDescription = state
                this.selected = selected
                if (open) {
                    collapse {
                        onToggle()
                        true
                    }
                } else {
                    expand {
                        onToggle()
                        true
                    }
                }
            }
            .heightIn(min = MonarchTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expandIcon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.muted(colors)) {
                Box(Modifier.padding(start = tokens.HeaderPadding).rotate(rotation).clearAndSetSemantics { }) {
                    expandIcon()
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(tokens.HeaderPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            Text(text = claim.text, style = MonarchTheme.typography.label, color = colors.textDefault)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchBadge(
                    text = supportText(claim.support, labels),
                    tone = tokens.tone(claim.support),
                    leadingIcon = supportIcon?.let { icon -> supportIconSlot(icon, claim.support) },
                )
                Text(
                    text = labels.passages?.invoke(claim.evidence.size) ?: passagesText(claim.evidence.size),
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(colors),
                )
            }
        }
    }
}

/** The badge's icon slot: the app's icon for [support], hidden from TalkBack. */
private fun supportIconSlot(
    icon: @Composable (MonarchAiClaimSupport) -> Unit,
    support: MonarchAiClaimSupport,
): @Composable () -> Unit = { Box(Modifier.clearAndSetSemantics { }) { icon(support) } }

@Composable
private fun ClaimPanel(
    claim: MonarchAiClaim,
    sourceById: Map<String, MonarchAiClaimSource>,
    actions: ClaimActions,
    labels: MonarchAiClaimSourcesLabels,
) {
    val tokens = MonarchAiClaimSourcesTokens
    val colors = MonarchTheme.colors
    val note = if (claim.needsChecking) labels.note?.invoke(claim.support) ?: noteText(claim.support) else null
    note?.let { Text(text = it, style = MonarchTheme.typography.body, color = colors.textDefault) }
    if (claim.evidence.isNotEmpty()) {
        val name = labels.evidenceLabel?.invoke(claim.text)
            ?: stringResource(R.string.monarch_aiclaimsources_evidence_label, claim.text)
        Column(
            modifier = Modifier.semantics {
                contentDescription = name
                isTraversalGroup = true
            },
            verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
        ) {
            claim.evidence.forEach { evidence ->
                EvidenceItem(evidence, sourceById[evidence.sourceId], actions, labels)
            }
        }
    } else if (note == null) {
        Text(
            text = monarchString(labels.noEvidence, R.string.monarch_aiclaimsources_no_evidence),
            style = MonarchTheme.typography.body,
            color = tokens.muted(colors),
        )
    }
    val verify = actions.onVerify?.takeIf { claim.support == MonarchAiClaimSupport.Unchecked }
    if (verify != null || actions.onFlag != null) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap)) {
            verify?.let {
                MonarchButton(
                    text = monarchString(labels.verify, R.string.monarch_aiclaimsources_verify),
                    onClick = { it(claim.id) },
                    size = MonarchButtonSize.Sm,
                )
            }
            actions.onFlag?.let {
                MonarchButton(
                    text = monarchString(labels.flag, R.string.monarch_aiclaimsources_flag),
                    onClick = { it(claim.id) },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

@Composable
private fun EvidenceItem(
    evidence: MonarchAiClaimEvidence,
    source: MonarchAiClaimSource?,
    actions: ClaimActions,
    labels: MonarchAiClaimSourcesLabels,
) {
    val tokens = MonarchAiClaimSourcesTokens
    val colors = MonarchTheme.colors
    val title = source?.title ?: monarchString(labels.unknownSource, R.string.monarch_aiclaimsources_unknown_source)
    val which = labels.fromSource?.invoke(title) ?: stringResource(R.string.monarch_aiclaimsources_from_source, title)
    val locale = LocalConfiguration.current.locales.get(0)
    val details = listOfNotNull(
        source?.kind?.let { kindText(it, labels) },
        evidence.location?.takeIf { it.isNotBlank() },
        source?.publishedMillis?.let { millis ->
            val date = tokens.publishedDate(millis, locale)
            labels.published?.invoke(date) ?: stringResource(R.string.monarch_aiclaimsources_published, date)
        },
    )
    val barColor = tokens.quoteBar(evidence.stance, colors)
    val dashed = evidence.stance == MonarchAiClaimEvidenceStance.Contradicts
    val barWidth = tokens.BarWidth
    Column(
        modifier = Modifier
            .drawBehind {
                val x = barWidth.toPx() / 2f
                val dashes = floatArrayOf(barWidth.toPx(), barWidth.toPx())
                drawLine(
                    color = barColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = barWidth.toPx() / 2f,
                    pathEffect = if (dashed) PathEffect.dashPathEffect(dashes) else null,
                )
            }
            .padding(start = tokens.QuotePadding),
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
    ) {
        Text(text = evidence.quote, style = MonarchTheme.typography.body, color = colors.textDefault)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                source?.kind?.let { kind ->
                    // The web puts the kind's glyph before the source's title.
                    Box(Modifier.clearAndSetSemantics { }) {
                        CompositionLocalProvider(LocalContentColor provides colors.textMuted) {
                            MonarchDefaultIcons.sourceKind(kind)
                        }
                    }
                }
                Text(text = title, style = MonarchTheme.typography.label, color = colors.textDefault)
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(colors),
                )
            }
            evidence.stance?.let { stance ->
                val stanceColor = tokens.stanceColor(stance, colors)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.clearAndSetSemantics { }) {
                        CompositionLocalProvider(LocalContentColor provides stanceColor) {
                            if (stance == MonarchAiClaimEvidenceStance.Supports) {
                                MonarchDefaultIcons.CheckCircle()
                            } else {
                                MonarchDefaultIcons.Error()
                            }
                        }
                    }
                    Text(
                        text = stanceText(stance, labels),
                        style = MonarchTheme.typography.caption,
                        color = stanceColor,
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            OpenSourceButton(evidence, source, which, actions, labels)
            MonarchCopyButton(
                value = evidence.quote,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                target = which,
                onCopy = actions.onCopyQuote?.let { copy -> { _: String -> copy(evidence) } },
                label = monarchString(labels.copyQuote, R.string.monarch_aiclaimsources_copy_quote),
                copiedLabel = monarchString(labels.copied, R.string.monarch_aiclaimsources_copied),
                copiedAnnouncement = monarchString(
                    labels.copiedAnnouncement,
                    R.string.monarch_aiclaimsources_copied_announcement,
                ),
            )
        }
    }
}

@Composable
private fun OpenSourceButton(
    evidence: MonarchAiClaimEvidence,
    source: MonarchAiClaimSource?,
    which: String,
    actions: ClaimActions,
    labels: MonarchAiClaimSourcesLabels,
) {
    source ?: return
    val open = actions.onOpenSource
    val url = MonarchAiCitationTokens.safeUrl(source.url)
    if (open == null && url == null) return
    val uriHandler = LocalUriHandler.current
    val text = monarchString(labels.openSource, R.string.monarch_aiclaimsources_open_source)
    val spoken = stringResource(R.string.monarch_aiclaimsources_action_label, text, which)
    MonarchButton(
        text = text,
        onClick = { if (open != null) open(source, evidence) else url?.let(uriHandler::openUri) },
        modifier = Modifier.semantics { contentDescription = spoken },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
    )
}

/** The announcement of the last filter change; empty until the filter changes. */
@Composable
private fun rememberFilterAnnouncement(
    filter: MonarchAiClaimSourcesFilter,
    shown: Int,
    total: Int,
    labels: MonarchAiClaimSourcesLabels,
): String {
    var previous by remember { mutableStateOf(filter) }
    var announcement by remember { mutableStateOf("") }
    val text = labels.showing?.invoke(shown, total)
        ?: pluralStringResource(R.plurals.monarch_aiclaimsources_showing, total, shown, total)
    LaunchedEffect(filter) {
        if (filter != previous) {
            previous = filter
            announcement = text
        }
    }
    return announcement
}

@Composable
private fun summaryText(counts: MonarchAiClaimCounts): String {
    if (counts.total == 0) return stringResource(R.string.monarch_aiclaimsources_summary_none)
    val parts = listOf(
        counts.supported to R.plurals.monarch_aiclaimsources_summary_supported,
        counts.partial to R.plurals.monarch_aiclaimsources_summary_partial,
        counts.unsupported to R.plurals.monarch_aiclaimsources_summary_unsupported,
        counts.conflicting to R.plurals.monarch_aiclaimsources_summary_conflicting,
        counts.unchecked to R.plurals.monarch_aiclaimsources_summary_unchecked,
    ).filter { (count, _) -> count > 0 }.map { (count, text) -> pluralStringResource(text, count, count) }
    val joined = parts.joinToString(stringResource(R.string.monarch_aiclaimsources_summary_separator))
    return pluralStringResource(R.plurals.monarch_aiclaimsources_summary, counts.total, counts.total, joined)
}

@Composable
private fun passagesText(count: Int): String = if (count == 0) {
    stringResource(R.string.monarch_aiclaimsources_passages_none)
} else {
    pluralStringResource(R.plurals.monarch_aiclaimsources_passages, count, count)
}

@Composable
private fun supportText(support: MonarchAiClaimSupport, labels: MonarchAiClaimSourcesLabels): String =
    labels.support?.invoke(support) ?: stringResource(
        when (support) {
            MonarchAiClaimSupport.Supported -> R.string.monarch_aiclaimsources_support_supported
            MonarchAiClaimSupport.Partial -> R.string.monarch_aiclaimsources_support_partial
            MonarchAiClaimSupport.Unsupported -> R.string.monarch_aiclaimsources_support_unsupported
            MonarchAiClaimSupport.Conflicting -> R.string.monarch_aiclaimsources_support_conflicting
            MonarchAiClaimSupport.Unchecked -> R.string.monarch_aiclaimsources_support_unchecked
        },
    )

@Composable
private fun noteText(support: MonarchAiClaimSupport): String? = when (support) {
    MonarchAiClaimSupport.Supported -> null
    MonarchAiClaimSupport.Partial -> stringResource(R.string.monarch_aiclaimsources_note_partial)
    MonarchAiClaimSupport.Unsupported -> stringResource(R.string.monarch_aiclaimsources_note_unsupported)
    MonarchAiClaimSupport.Conflicting -> stringResource(R.string.monarch_aiclaimsources_note_conflicting)
    MonarchAiClaimSupport.Unchecked -> stringResource(R.string.monarch_aiclaimsources_note_unchecked)
}

@Composable
private fun stanceText(stance: MonarchAiClaimEvidenceStance, labels: MonarchAiClaimSourcesLabels): String =
    labels.stance?.invoke(stance) ?: stringResource(
        when (stance) {
            MonarchAiClaimEvidenceStance.Supports -> R.string.monarch_aiclaimsources_stance_supports
            MonarchAiClaimEvidenceStance.Contradicts -> R.string.monarch_aiclaimsources_stance_contradicts
        },
    )

@Composable
private fun kindText(kind: MonarchAiSourceKind, labels: MonarchAiClaimSourcesLabels): String =
    labels.kind?.invoke(kind) ?: stringResource(
        when (kind) {
            MonarchAiSourceKind.Web -> R.string.monarch_aiclaimsources_kind_web
            MonarchAiSourceKind.Doc -> R.string.monarch_aiclaimsources_kind_doc
            MonarchAiSourceKind.Record -> R.string.monarch_aiclaimsources_kind_record
            MonarchAiSourceKind.File -> R.string.monarch_aiclaimsources_kind_file
        },
    )

/** The stripe at the start of a claim card: the border colour of the support level, quiet when unchecked. */
private fun claimStripe(support: MonarchAiClaimSupport, colors: MonarchColors): Color = when (support) {
    MonarchAiClaimSupport.Supported -> colors.successBorder
    MonarchAiClaimSupport.Partial, MonarchAiClaimSupport.Conflicting -> colors.warningBorder
    MonarchAiClaimSupport.Unsupported -> colors.dangerBorder
    MonarchAiClaimSupport.Unchecked -> colors.borderSubtle
}
