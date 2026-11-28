package dev.monarchui.components.identity.memberrow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/** Which swipe actions a row offers now. */
internal class MemberRowSwipe(val remove: Boolean, val resend: Boolean) {
    val any: Boolean get() = remove || resend
}

/**
 * The swipe actions of a row, as TalkBack actions on the member: the same
 * names as the buttons ("Remove Priya Raman", "Resend invite, Priya Raman").
 */
internal fun memberRowSwipeActions(
    swipe: MemberRowSwipe,
    controls: MemberRowControls,
): List<CustomAccessibilityAction> =
    listOfNotNull(
        CustomAccessibilityAction(controls.texts.remove.spoken) {
            controls.askRemove()
            true
        }.takeIf { swipe.remove },
        CustomAccessibilityAction(controls.texts.resendName) {
            controls.resend()
            true
        }.takeIf { swipe.resend },
    )

/**
 * The identity in a Material 3 `SwipeToDismissBox` when the row has swipe
 * actions: toward the start (end to start) asks to remove, toward the end
 * resends the invitation. The row springs back; the app removes it.
 */
@Composable
internal fun MemberRowSwipeBox(swipe: MemberRowSwipe, controls: MemberRowControls, content: @Composable () -> Unit) {
    if (!swipe.any) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val colors = MonarchTheme.colors
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            when (state.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> SwipeBackground(
                    text = controls.texts.remove.confirm,
                    container = colors.dangerSolid,
                    content = colors.dangerOnSolid,
                    alignment = Alignment.CenterEnd,
                )
                SwipeToDismissBoxValue.StartToEnd -> SwipeBackground(
                    text = controls.texts.resendText,
                    container = colors.infoSolid,
                    content = colors.infoOnSolid,
                    alignment = Alignment.CenterStart,
                )
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        enableDismissFromStartToEnd = swipe.resend,
        enableDismissFromEndToStart = swipe.remove,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> controls.askRemove()
                SwipeToDismissBoxValue.StartToEnd -> controls.resend()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch { state.reset() }
        },
    ) {
        Box(Modifier.background(colors.bgSurface)) { content() }
    }
}

@Composable
private fun SwipeBackground(text: String, container: Color, content: Color, alignment: Alignment) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(container)
            .padding(horizontal = MonarchMemberRowTokens.Inset)
            // The member's TalkBack actions and the buttons offer the same actions, so this is not read.
            .clearAndSetSemantics { },
        contentAlignment = alignment,
    ) {
        Text(text = text, style = MonarchTheme.typography.label, color = content)
    }
}
