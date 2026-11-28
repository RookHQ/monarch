package dev.monarchui.components.feedback.errorstate

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.feedback.emptystate.EmptyStateLayout
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A region failed to load: what failed, why in plain words, and a retry
 * button, in the layout of `MonarchEmptyState`.
 *
 * Built by Monarch: Material 3 has no error state. The [kind] sets the colour
 * of the icon tile and whether a retry is offered: a retry cannot change the
 * outcome of `Forbidden` or `NotFound`, so they have no retry button. The
 * request id can be selected and copied with the system text selection.
 *
 * The state does not announce itself when it replaces content; announce the
 * failure if it follows something the user did.
 *
 * @param title what failed, for example "Invoices did not load". Marked as a heading for TalkBack.
 * @param modifier the modifier of the error state.
 * @param kind what went wrong.
 * @param size the spacing and the type sizes, shared with `MonarchEmptyState`.
 * @param retrying shows the retry button as busy while a retry runs.
 * @param message why, in plain words, and what the user can do.
 * @param icon an icon in the tinted tile, for example a `MonarchIcon` with a `null` content description. It
 * is hidden from TalkBack. Monarch bundles no icons.
 * @param requestId the request id of the failed response, shown in code text so the user can quote it to
 * support.
 * @param onRetry adds a primary retry button, except for `Forbidden` and `NotFound`. Not called while
 * [retrying].
 * @param actions further buttons after the retry button.
 * @param retryLabel the label of the retry button. `null` uses `monarch_errorstate_retry`.
 * @param retryingLabel the state of the retry button while [retrying]. `null` uses
 * `monarch_errorstate_retrying`.
 * @param requestIdLabel the label before the request id. `null` uses `monarch_errorstate_request_id`.
 * @param pageTile draws the icon tile of a full error page: 56dp, rounded, in the accent colour for a missing page.
 */
@Composable
public fun MonarchErrorState(
    title: String,
    modifier: Modifier = Modifier,
    kind: MonarchErrorStateKind = MonarchErrorStateKind.Error,
    size: MonarchEmptyStateSize = MonarchEmptyStateSize.Md,
    retrying: Boolean = false,
    message: String? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Warning,
    requestId: String? = null,
    onRetry: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    retryLabel: String? = null,
    retryingLabel: String? = null,
    requestIdLabel: String? = null,
    pageTile: Boolean = false,
) {
    val retry = if (MonarchErrorStateTokens.offersRetry(kind)) onRetry else null
    val retryText = monarchString(retryLabel, R.string.monarch_errorstate_retry)
    val retryingText = monarchString(retryingLabel, R.string.monarch_errorstate_retrying)
    val idLabel = monarchString(requestIdLabel, R.string.monarch_errorstate_request_id)
    val allActions: (@Composable () -> Unit)? = if (retry != null || actions != null) {
        {
            if (retry != null) {
                MonarchButton(
                    text = retryText,
                    onClick = retry,
                    variant = MonarchButtonVariant.Primary,
                    loading = retrying,
                    loadingLabel = retryingText,
                )
            }
            actions?.invoke()
        }
    } else {
        null
    }
    EmptyStateLayout(
        title = title,
        modifier = modifier,
        size = size,
        tileColors = if (pageTile && kind == MonarchErrorStateKind.NotFound) {
            MonarchSurfaceColors(MonarchTheme.colors.accentSubtle, MonarchTheme.colors.accentStrong, null)
        } else {
            MonarchErrorStateTokens.tileColors(kind, MonarchTheme.colors)
        },
        message = message,
        // The web draws a lock for a page the person may not open.
        icon = if (icon === MonarchDefaultIcons.Warning && kind == MonarchErrorStateKind.Forbidden) {
            MonarchDefaultIcons.Lock
        } else {
            icon
        },
        tip = null,
        actions = allActions,
        extra = requestId?.let { id -> { RequestId(idLabel, id) } },
        pageTile = pageTile,
    )
}

/** "Request ID: abc123" in one selectable text, read by TalkBack as one element, with a button that copies the id. */
@Composable
private fun RequestId(label: String, id: String) {
    val colors = MonarchTheme.colors
    val code = MonarchTheme.typography.code
    Row(verticalAlignment = Alignment.CenterVertically) {
        SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = buildAnnotatedString {
                    append("$label: ")
                    withStyle(SpanStyle(fontFamily = code.fontFamily, color = colors.textDefault)) { append(id) }
                },
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
        MonarchCopyButton(value = id, icon = MonarchDefaultIcons.Copy, target = label, size = MonarchButtonSize.Sm)
    }
}
