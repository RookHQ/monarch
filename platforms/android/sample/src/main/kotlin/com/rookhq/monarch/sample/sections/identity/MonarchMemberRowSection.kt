package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.identity.memberrow.MonarchMemberRow
import com.rookhq.monarch.components.identity.memberrow.MonarchMemberRowStatus
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.components.identity.rolepicker.MonarchRoleOption
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SimulatedRequestMillis = 600L

private val Roles = listOf(
    MonarchRoleOption("admin", "Admin", description = "Manages members and billing"),
    MonarchRoleOption("member", "Member", description = "Works in every space"),
    MonarchRoleOption("viewer", "Viewer", description = "Reads, comments, and cannot edit"),
)

/** The catalogue page of MemberRow, with simulated requests; making Jonas a viewer fails. */
object MonarchMemberRowSection : CataloguePage {
    override val title = "MemberRow"

    @Composable
    override fun Content() {
        var jonasRole by rememberSaveable { mutableStateOf("member") }
        var jonasShown by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Your own row")
        MonarchMemberRow(
            name = "Priya Raman",
            email = "priya@example.com",
            presence = MonarchPresenceStatus.Online,
            role = "admin",
            roles = Roles,
            self = true,
        )
        CatalogueCaption("Change the role (Viewer fails) or remove")
        if (jonasShown) {
            MonarchMemberRow(
                name = "Jonas Berg",
                email = "jonas@example.com",
                meta = "Joined March 2024",
                role = jonasRole,
                roles = Roles,
                badges = listOf("Guest"),
                onRoleChange = { next ->
                    delay(SimulatedRequestMillis)
                    check(next != "viewer") { "Guests cannot be viewers in this workspace." }
                    jonasRole = next
                },
                onRemove = {
                    delay(SimulatedRequestMillis)
                    jonasShown = false
                },
                removeConsequences = listOf("3 assigned tickets go back to the queue"),
            )
        } else {
            MonarchButton(text = "Bring Jonas back", onClick = { jonasShown = true })
        }
        CatalogueCaption("An invitation")
        MonarchMemberRow(
            name = "ana@example.com",
            meta = "Invite expires in 3 days",
            status = MonarchMemberRowStatus.Invited,
            onResend = { delay(SimulatedRequestMillis) },
            onRemove = { delay(SimulatedRequestMillis) },
        )
    }
}
