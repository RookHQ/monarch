package com.rookhq.monarch.components.technical.embeddedappframe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.spinner.MonarchSpinner
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A framed area that shows a screen owned by another app or module, such as another Rook app's settings page, with
 * its loading and error states.
 *
 * A bordered, rounded container built from Compose layouts: the app puts the content in [content] (a native screen,
 * or a `WebView` through `AndroidView`) and reports what it is doing through [state]. While loading it shows a
 * spinner; after an error a message with "Try again" and "Open the page". The frame is a pane named by [title], and
 * the error is announced politely.
 *
 * @param title the name of what the frame shows, such as "Example Desk: Email notifications". Read by TalkBack.
 * @param state loading, ready or error.
 * @param modifier the modifier of the frame.
 * @param height a fixed height; `null` lets the frame wrap its content.
 * @param onRetry called by "Try again"; `null` hides the button.
 * @param onOpen called by "Open the page", to open the screen outside the frame; `null` hides the button.
 * @param labels replacements for the built-in strings.
 * @param content the embedded screen, shown when [state] is ready.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchEmbeddedAppFrame(
    title: String,
    state: MonarchEmbeddedAppFrameState,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    onRetry: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    labels: MonarchEmbeddedAppFrameLabels = MonarchEmbeddedAppFrameLabels(),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (height != null) {
                    Modifier.height(height)
                } else if (state != MonarchEmbeddedAppFrameState.Ready) {
                    Modifier.heightIn(min = MonarchEmbeddedAppFrameTokens.MinHeight)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(MonarchEmbeddedAppFrameTokens.fill(colors), shape)
            .border(MonarchShapeDefaults.BorderWidth, MonarchEmbeddedAppFrameTokens.border(colors), shape)
            .semantics {
                isTraversalGroup = true
                paneTitle = title
            },
    ) {
        when (state) {
            MonarchEmbeddedAppFrameState.Ready -> content()
            MonarchEmbeddedAppFrameState.Loading -> {
                val text = monarchString(labels.loading, R.string.monarch_embeddedappframe_loading)
                Row(
                    modifier = Modifier.align(Alignment.Center).padding(MonarchEmbeddedAppFrameTokens.Padding),
                    horizontalArrangement = Arrangement.spacedBy(MonarchEmbeddedAppFrameTokens.Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchSpinner(label = text)
                    MonarchText(
                        text = text,
                        modifier = Modifier.clearAndSetSemantics { },
                        color = MonarchEmbeddedAppFrameTokens.muted(colors),
                    )
                }
            }
            MonarchEmbeddedAppFrameState.Error -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(MonarchEmbeddedAppFrameTokens.Padding),
                verticalArrangement = Arrangement.spacedBy(MonarchEmbeddedAppFrameTokens.Gap),
            ) {
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(MonarchEmbeddedAppFrameTokens.TextGap),
                ) {
                    MonarchText(
                        text = monarchString(labels.errorTitle, R.string.monarch_embeddedappframe_error_title),
                        style = MonarchTextStyle.BodyStrong,
                        heading = true,
                    )
                    MonarchText(
                        text = monarchString(
                            labels.errorDescription,
                            R.string.monarch_embeddedappframe_error_description,
                        ),
                        style = MonarchTextStyle.Label,
                        color = MonarchEmbeddedAppFrameTokens.muted(colors),
                    )
                }
                if (onRetry != null || onOpen != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MonarchEmbeddedAppFrameTokens.ActionGap),
                        verticalArrangement = Arrangement.spacedBy(MonarchEmbeddedAppFrameTokens.ActionGap),
                    ) {
                        if (onRetry != null) {
                            MonarchButton(
                                text = monarchString(labels.retry, R.string.monarch_embeddedappframe_retry),
                                onClick = onRetry,
                                variant = MonarchButtonVariant.Secondary,
                            )
                        }
                        if (onOpen != null) {
                            MonarchButton(
                                text = monarchString(labels.open, R.string.monarch_embeddedappframe_open),
                                onClick = onOpen,
                                variant = MonarchButtonVariant.Ghost,
                            )
                        }
                    }
                }
            }
        }
    }
}
