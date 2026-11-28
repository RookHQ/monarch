package dev.monarchui.components.identity.recoverycodes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * One-time recovery codes in a monospaced grid, with copy, save or share, an
 * optional show and hide button, generate new codes behind a confirmation,
 * and an acknowledgement for setup flows.
 *
 * Built by Monarch from `MonarchCopyButton`, `MonarchButton`,
 * `MonarchCheckboxRow` and a Material 3 `AlertDialog` for the confirmation.
 * Each code is spelled out character by character by TalkBack and a used code
 * reads "Used", so its state does not depend on the strike-through. Copy marks
 * the clip as sensitive. Download saves the codes as a text file through the
 * Storage Access Framework (`CreateDocument`, the system file picker), as the
 * web downloads `recovery-codes.txt`. Save or share opens the system share
 * sheet with the codes as text, for a password manager or another app; pass
 * [onSave] to handle it yourself. New codes are announced after [onRegenerate]
 * returns, and a missing acknowledgement is shown and announced.
 *
 * @param codes the codes. Used codes are struck through and not copied or saved.
 * @param modifier the modifier of the component.
 * @param heading a first line for the copied and saved text, such as the product and account, followed by a
 * blank line.
 * @param initiallyRevealed whether the codes are visible at first.
 * @param showRevealToggle whether the show and hide button is shown. `null` shows it when the codes start hidden.
 * @param onSave saves the text of the codes. `null` opens the system share sheet.
 * @param fileName the suggested name of the downloaded file.
 * @param showSave whether the save or share button is shown beside Download. The web has no such button; `false`
 * leaves Copy and Download.
 * @param onRegenerate replaces the codes after a destructive confirmation; pass the new codes back through
 * [codes]. The button is busy until it returns. New codes clear the acknowledgement. When it throws, nothing
 * is announced; report the failure in the app.
 * @param onAcknowledge runs when the person ticks "I have saved my recovery codes" and presses Continue.
 * Neither is shown without it.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchRecoveryCodes(
    codes: List<MonarchRecoveryCode>,
    modifier: Modifier = Modifier,
    heading: String? = null,
    initiallyRevealed: Boolean = true,
    showRevealToggle: Boolean? = null,
    onSave: ((String) -> Unit)? = null,
    onRegenerate: (suspend () -> Unit)? = null,
    onAcknowledge: (() -> Unit)? = null,
    labels: MonarchRecoveryCodesLabels = MonarchRecoveryCodesLabels(),
    fileName: String = MonarchRecoveryCodesTokens.DEFAULT_FILE_NAME,
    showSave: Boolean = true,
) {
    val tokens = MonarchRecoveryCodesTokens
    var revealed by rememberSaveable { mutableStateOf(initiallyRevealed) }
    var announcement by remember { mutableStateOf("") }
    val regeneratedText = monarchString(labels.regenerated, R.string.monarch_recoverycodes_regenerated)
    val left = codes.count { !it.used }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
        if (left < codes.size) {
            Text(
                text = labels.remaining?.invoke(left, codes.size)
                    ?: pluralStringResource(R.plurals.monarch_recoverycodes_remaining, left, left, codes.size),
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        if (revealed) {
            CodeGrid(codes, labels)
        } else {
            Text(
                text = monarchString(labels.concealedHint, R.string.monarch_recoverycodes_concealed_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .monarchSurface(tokens.panelColors(MonarchTheme.colors), MonarchShapeDefaults.Control)
                    .padding(tokens.PanelPadding),
                style = MonarchTheme.typography.body,
                color = MonarchTheme.colors.textMuted,
            )
        }
        Actions(
            codes = codes,
            heading = heading,
            revealed = revealed,
            showToggle = tokens.showsRevealToggle(showRevealToggle, initiallyRevealed),
            onToggle = { revealed = !revealed },
            onSave = onSave,
            showSave = showSave,
            onRegenerate = onRegenerate,
            onRegenerated = { announcement = regeneratedText },
            onAnnounce = { announcement = it },
            fileName = fileName,
            labels = labels,
        )
        if (onAcknowledge != null) {
            Acknowledgement(codes, onAcknowledge, labels) { announcement = it }
        }
        // Always present, so new codes and a missing acknowledgement are announced as changes of this region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun CodeGrid(codes: List<MonarchRecoveryCode>, labels: MonarchRecoveryCodesLabels) {
    val tokens = MonarchRecoveryCodesTokens
    val colors = MonarchTheme.colors
    val listLabel = monarchString(labels.listLabel, R.string.monarch_recoverycodes_list_label)
    val usedText = monarchString(labels.used, R.string.monarch_recoverycodes_used)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(tokens.panelColors(colors), MonarchShapeDefaults.Control)
            .padding(tokens.PanelPadding)
            .semantics {
                contentDescription = listLabel
                isTraversalGroup = true
            },
    ) {
        val columns = tokens.columns(maxWidth, fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            codes.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                    row.forEach { code ->
                        Text(
                            // TalkBack spells a verbatim span character by character.
                            text = buildAnnotatedString {
                                withAnnotation(VerbatimTtsAnnotation(code.code)) { append(code.code) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { if (code.used) stateDescription = usedText },
                            style = MonarchTheme.typography.code,
                            color = tokens.codeColor(code.used, colors),
                            textDecoration = if (code.used) TextDecoration.LineThrough else null,
                        )
                    }
                    repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Actions(
    codes: List<MonarchRecoveryCode>,
    heading: String?,
    revealed: Boolean,
    showToggle: Boolean,
    onToggle: () -> Unit,
    onSave: ((String) -> Unit)?,
    showSave: Boolean,
    onRegenerate: (suspend () -> Unit)?,
    onRegenerated: () -> Unit,
    onAnnounce: (String) -> Unit,
    fileName: String,
    labels: MonarchRecoveryCodesLabels,
) {
    val tokens = MonarchRecoveryCodesTokens
    val text = tokens.text(codes, heading)
    val usable = tokens.anyUnused(codes)
    val context = LocalContext.current
    val save = monarchString(labels.save, R.string.monarch_recoverycodes_save)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (showToggle) {
            MonarchButton(
                text = if (revealed) {
                    monarchString(labels.conceal, R.string.monarch_recoverycodes_conceal)
                } else {
                    monarchString(labels.reveal, R.string.monarch_recoverycodes_reveal)
                },
                onClick = onToggle,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        MonarchCopyButton(
            value = text,
            size = MonarchButtonSize.Sm,
            enabled = usable,
            sensitive = true,
            label = monarchString(labels.copy, R.string.monarch_recoverycodes_copy),
            copiedLabel = monarchString(labels.copied, R.string.monarch_recoverycodes_copied),
        )
        RecoveryCodesDownload(text, fileName, usable, labels, onAnnounce)
        if (showSave) {
            MonarchButton(
                text = save,
                onClick = { if (onSave != null) onSave(text) else share(context, text, save) },
                size = MonarchButtonSize.Sm,
                enabled = usable,
            )
        }
        if (onRegenerate != null) Regenerate(onRegenerate, onRegenerated, labels)
    }
}

@Composable
private fun Regenerate(
    onRegenerate: suspend () -> Unit,
    onRegenerated: () -> Unit,
    labels: MonarchRecoveryCodesLabels,
) {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(onRegenerate)
    var confirming by rememberSaveable { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val title = monarchString(labels.regenerate, R.string.monarch_recoverycodes_regenerate)
    MonarchButton(
        text = title,
        onClick = { confirming = true },
        variant = MonarchButtonVariant.Danger,
        size = MonarchButtonSize.Sm,
        loading = running,
    )
    if (confirming) {
        MonarchDialog(
            onDismissRequest = { confirming = false },
            title = monarchString(labels.regenerateTitle, R.string.monarch_recoverycodes_regenerate_title),
            description = monarchString(
                labels.regenerateDescription,
                R.string.monarch_recoverycodes_regenerate_description,
            ),
            actions = {
                MonarchButton(
                    text = monarchString(labels.cancel, R.string.monarch_recoverycodes_cancel),
                    onClick = { confirming = false },
                    variant = MonarchButtonVariant.Ghost,
                )
                MonarchButton(
                    text = monarchString(labels.regenerateConfirm, R.string.monarch_recoverycodes_regenerate_confirm),
                    onClick = {
                        confirming = false
                        running = true
                        scope.launch {
                            val outcome = runCatching { current() }
                            running = false
                            (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                            // A failure is the app's to report; only new codes are announced.
                            if (outcome.isSuccess) onRegenerated()
                        }
                    },
                    variant = MonarchButtonVariant.Danger,
                )
            },
        )
    }
}

@Composable
private fun Acknowledgement(
    codes: List<MonarchRecoveryCode>,
    onAcknowledge: () -> Unit,
    labels: MonarchRecoveryCodesLabels,
    announce: (String) -> Unit,
) {
    // A new set of codes clears the acknowledgement; marking a code used does not.
    val key = codes.map { it.code }
    var checked by rememberSaveable(key) { mutableStateOf(false) }
    var missing by rememberSaveable(key) { mutableStateOf(false) }
    val missingText = monarchString(labels.acknowledgeMissing, R.string.monarch_recoverycodes_acknowledge_missing)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchRecoveryCodesTokens.Gap)) {
        MonarchCheckboxRow(
            checked = checked,
            onCheckedChange = {
                checked = it
                if (it) missing = false
            },
            label = monarchString(labels.acknowledge, R.string.monarch_recoverycodes_acknowledge),
        )
        if (missing) {
            Text(text = missingText, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.dangerStrong)
        }
        MonarchButton(
            text = monarchString(labels.continueTitle, R.string.monarch_recoverycodes_continue),
            onClick = {
                if (checked) {
                    onAcknowledge()
                } else {
                    missing = true
                    announce(missingText)
                }
            },
            variant = MonarchButtonVariant.Primary,
        )
    }
}

/** Opens the system share sheet with [text], from which the person can save it. */
private fun share(context: Context, text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
        .putExtra(Intent.EXTRA_TITLE, title)
    context.startActivity(Intent.createChooser(send, title))
}
