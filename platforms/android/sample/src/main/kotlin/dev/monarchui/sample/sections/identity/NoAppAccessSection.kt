package dev.monarchui.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.components.feedback.errorstate.MonarchErrorStateKind
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.identity.workspaceswitcher.MonarchWorkspaceOption
import dev.monarchui.components.identity.workspaceswitcher.MonarchWorkspaceSwitcher
import dev.monarchui.components.identity.workspaceswitcher.MonarchWorkspaceSwitcherLabels
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The catalogue page of NoAppAccess: the no-access screen with request, back and switch. */
object NoAppAccessSection : CataloguePage {
    override val title = "NoAppAccess"

    private const val SimulatedRequestMillis = 800L

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var request by rememberSaveable { mutableStateOf("idle") }
        var failNext by rememberSaveable { mutableStateOf(true) }
        var workspace by rememberSaveable { mutableStateOf<String?>("acme") }
        CatalogueCaption("The first request fails, the second goes through")
        MonarchErrorState(
            title = "You don’t have access to Rook Tickets",
            kind = MonarchErrorStateKind.Forbidden,
            size = MonarchEmptyStateSize.Lg,
            message = "Ask an admin of Acme to give you access to Rook Tickets.",
            actions = {
                MonarchButton(
                    text = if (request == "sent") "Access requested" else "Request access",
                    onClick = {
                        request = "pending"
                        scope.launch {
                            delay(SimulatedRequestMillis)
                            request = if (failNext) "error" else "sent"
                            failNext = false
                        }
                    },
                    variant = MonarchButtonVariant.Primary,
                    enabled = request != "sent",
                    loading = request == "pending",
                )
                MonarchButton(text = "Back to workspace", onClick = {})
                MonarchWorkspaceSwitcher(
                    workspaces = listOf(
                        MonarchWorkspaceOption("acme", "Acme", handle = "acme"),
                        MonarchWorkspaceOption("rook", "Rook Labs", handle = "rooklabs"),
                    ),
                    selection = workspace,
                    onSelectionChange = { workspace = it },
                    labels = MonarchWorkspaceSwitcherLabels(label = "Switch workspace"),
                )
            },
        )
        MonarchText(
            text = if (request == "sent") "Request sent. You can open Rook Tickets once an admin approves it." else "",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MonarchTheme.colors.textMuted,
        )
        if (request == "error") {
            MonarchInlineNotice(message = "The request didn’t go through. Try again.", tone = MonarchTone.Danger)
        }
    }
}
