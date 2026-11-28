package dev.monarchui.components.technical.installverify

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.technical.codesnippettabs.MonarchCodeSnippet
import dev.monarchui.components.technical.codesnippettabs.MonarchCodeSnippetTabs
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Install a snippet on a website and watch for it to report in: the code
 * per platform with copy, a handoff for a developer, then "Waiting for your
 * site…" until the product sees it and says "Connected on example.com".
 *
 * A Monarch pattern built from `MonarchCodeSnippetTabs` (a Material 3
 * `SecondaryScrollableTabRow` over `MonarchCodeBlock`, with copy),
 * `MonarchCopyButton`, `MonarchButton` and `MonarchProgress`. The developer
 * handoff opens the system share sheet (`Intent.ACTION_SEND` through
 * `Intent.createChooser`) unless [onShareInstructions] handles it. The status
 * is one TalkBack element in a polite live region, so a change from waiting
 * to connected is announced; each site is one element read with when it was
 * last seen. Data comes in through parameters: the app polls its server and
 * passes the new [status].
 *
 * @param snippets the install options, one tab each. A single snippet shows without tabs.
 * @param status whether the installed code has reported in.
 * @param modifier the modifier of the component.
 * @param part both halves, or only one for a step of a wizard. Headings are shown with both.
 * @param selection the id of the snippet shown. The component keeps the person's choice and starts again from this
 * value when it changes.
 * @param onSelectionChange called with the id of the snippet the person picks.
 * @param connectedOn the hosts it was seen on, for the connected title. `null` uses the [targets] that have been seen.
 * @param targets the sites the install is expected on, listed with when each was last seen.
 * @param now the time "last seen" is measured from, in epoch milliseconds. `null` reads the clock once.
 * @param developerInstructions a plain-text handoff for a developer. Shows Copy instructions and Share with your
 * developer.
 * @param testDisabled disables the test page button; say why in [testHint].
 * @param testHint one line under the test page button.
 * @param labels replacements for the built-in strings.
 * @param onShareInstructions shares the handoff instead of the system share sheet.
 * @param onOpenTest shows Open a test page, for example to open the site in a browser.
 * @param onConnected called once, the first time [status] is `Connected` while the component is shown.
 * @param credential content above the snippets: the key the snippet needs and its state.
 */
@Suppress("LongParameterList", "LongMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchInstallVerify(
    snippets: List<MonarchInstallSnippet>,
    status: MonarchInstallVerifyStatus,
    modifier: Modifier = Modifier,
    part: MonarchInstallVerifyPart = MonarchInstallVerifyPart.All,
    selection: String? = null,
    onSelectionChange: ((String) -> Unit)? = null,
    connectedOn: List<String>? = null,
    targets: List<MonarchInstallTarget> = emptyList(),
    now: Long? = null,
    developerInstructions: String? = null,
    testDisabled: Boolean = false,
    testHint: String? = null,
    labels: MonarchInstallVerifyLabels = MonarchInstallVerifyLabels(),
    onShareInstructions: ((String) -> Unit)? = null,
    onOpenTest: (() -> Unit)? = null,
    onConnected: (() -> Unit)? = null,
    credential: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchInstallVerifyTokens
    var announced by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(status) {
        if (status == MonarchInstallVerifyStatus.Connected && !announced) {
            announced = true
            onConnected?.invoke()
        }
    }
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
    ) {
        val both = part == MonarchInstallVerifyPart.All
        if (part != MonarchInstallVerifyPart.Verify) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                if (both) Heading(monarchString(labels.installHeading, R.string.monarch_installverify_install))
                credential?.invoke()
                Snippets(snippets, selection, onSelectionChange, labels)
                if (developerInstructions != null) Handoff(developerInstructions, labels, onShareInstructions)
            }
        }
        if (part != MonarchInstallVerifyPart.Install) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                if (both) Heading(monarchString(labels.verifyHeading, R.string.monarch_installverify_verify))
                Status(status, tokens.hosts(connectedOn, targets), targets.map { it.label }, labels)
                if (targets.isNotEmpty()) {
                    val reference = now ?: remember { System.currentTimeMillis() }
                    Targets(targets, reference, labels)
                }
                if (onOpenTest != null) {
                    // As on the web: the button and its hint on one line, the hint wrapping beside it.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonarchButton(
                            text = monarchString(labels.openTest, R.string.monarch_installverify_open_test),
                            onClick = onOpenTest,
                            variant = MonarchButtonVariant.Secondary,
                            enabled = !testDisabled,
                        )
                        testHint?.let { Box(Modifier.weight(1f)) { Muted(it) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = MonarchTheme.typography.subheading,
        color = MonarchTheme.colors.textDefault,
    )
}

@Composable
private fun Muted(text: String) {
    Text(text = text, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
}

/** The snippet tabs, with the shown snippet's description above its code. */
@Composable
private fun Snippets(
    snippets: List<MonarchInstallSnippet>,
    selection: String?,
    onSelectionChange: ((String) -> Unit)?,
    labels: MonarchInstallVerifyLabels,
) {
    if (snippets.isEmpty()) return
    var chosen by rememberSaveable(selection) { mutableStateOf(selection) }
    val current = MonarchInstallVerifyTokens.current(chosen, snippets)
    snippets.firstOrNull { it.id == current }?.description?.let { Muted(it) }
    MonarchCodeSnippetTabs(
        snippets = snippets.map {
            MonarchCodeSnippet(label = it.label, code = it.code, language = it.language, id = it.id)
        },
        label = monarchString(labels.platforms, R.string.monarch_installverify_platforms),
        selection = current,
        onSelectionChange = { id ->
            chosen = id
            onSelectionChange?.invoke(id)
        },
    )
}

/** Copy the handoff, and share it through the share sheet. */
@Composable
private fun Handoff(instructions: String, labels: MonarchInstallVerifyLabels, onShare: ((String) -> Unit)?) {
    val context = LocalContext.current
    val tokens = MonarchInstallVerifyTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        MonarchCopyButton(
            value = instructions,
            label = monarchString(labels.copyInstructions, R.string.monarch_installverify_copy_instructions),
            copiedLabel = monarchString(labels.instructionsCopied, R.string.monarch_installverify_instructions_copied),
        )
        MonarchButton(
            text = monarchString(labels.shareInstructions, R.string.monarch_installverify_share_instructions),
            onClick = { onShare?.invoke(instructions) ?: installVerifyShare(context, instructions) },
            variant = MonarchButtonVariant.Ghost,
        )
    }
}

/** Opens the system share sheet with [text]. */
internal fun installVerifyShare(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent.createChooser(send, null))
    } catch (_: ActivityNotFoundException) {
        // No app can share text; the copy button is still there.
    }
}

/** The status as one element in a polite live region. */
@Composable
private fun Status(
    status: MonarchInstallVerifyStatus,
    hosts: List<String>,
    targets: List<String>,
    labels: MonarchInstallVerifyLabels,
) {
    val tokens = MonarchInstallVerifyTokens
    val colors = MonarchTheme.colors
    val (title, body) = statusTexts(status, hosts, targets, labels)
    val (fill, border) = tokens.statusColors(status, colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fill, MonarchShapeDefaults.Container)
            .border(MonarchShapeDefaults.BorderWidth, border, MonarchShapeDefaults.Container)
            .padding(tokens.StatusPadding)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(tokens.IndicatorSize).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(tokens.DotSize)
                    .background(tokens.indicatorColor(status, colors), MonarchShapeDefaults.Mark),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            Text(text = body, style = MonarchTheme.typography.body, color = colors.textMuted)
        }
    }
}

@Composable
private fun statusTexts(
    status: MonarchInstallVerifyStatus,
    hosts: List<String>,
    targets: List<String>,
    labels: MonarchInstallVerifyLabels,
): Pair<String, String> = when (status) {
    MonarchInstallVerifyStatus.Connected -> Pair(
        labels.connectedTitle?.invoke(hosts) ?: if (hosts.isEmpty()) {
            stringResource(R.string.monarch_installverify_connected)
        } else {
            stringResource(R.string.monarch_installverify_connected_on, hosts.joinToString(", "))
        },
        monarchString(labels.connectedBody, R.string.monarch_installverify_connected_body),
    )
    MonarchInstallVerifyStatus.Unavailable -> Pair(
        monarchString(labels.unavailableTitle, R.string.monarch_installverify_unavailable),
        monarchString(labels.unavailableBody, R.string.monarch_installverify_unavailable_body),
    )
    MonarchInstallVerifyStatus.Waiting -> Pair(
        monarchString(labels.waitingTitle, R.string.monarch_installverify_waiting),
        labels.waitingBody?.invoke(targets) ?: if (targets.isEmpty()) {
            stringResource(R.string.monarch_installverify_waiting_body_any)
        } else {
            stringResource(R.string.monarch_installverify_waiting_body, targets.joinToString(", "))
        },
    )
}

/** The sites, each one element read with when it was last seen. */
@Composable
private fun Targets(targets: List<MonarchInstallTarget>, now: Long, labels: MonarchInstallVerifyLabels) {
    val tokens = MonarchInstallVerifyTokens
    val colors = MonarchTheme.colors
    val notSeen = monarchString(labels.notSeen, R.string.monarch_installverify_not_seen)
    val listName = monarchString(labels.targetsLabel, R.string.monarch_installverify_targets)
    Column(
        modifier = Modifier.semantics {
            contentDescription = listName
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        targets.forEach { target ->
            val seenAt = target.lastSeenAt
            val relative = seenAt?.let {
                MonarchRelativeTime.format(it, now, LocalConfiguration.current.locales[0])
            }
            val seen = when {
                relative == null -> notSeen
                labels.seen != null -> labels.seen.invoke(relative)
                else -> stringResource(R.string.monarch_installverify_seen, relative)
            }
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(tokens.DotSize)
                        .background(tokens.dotColor(seenAt != null, colors), MonarchShapeDefaults.Mark),
                )
                Text(
                    text = target.label,
                    style = MonarchTheme.typography.codeSmall,
                    color = colors.textDefault,
                )
                Text(text = seen, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
    }
}
