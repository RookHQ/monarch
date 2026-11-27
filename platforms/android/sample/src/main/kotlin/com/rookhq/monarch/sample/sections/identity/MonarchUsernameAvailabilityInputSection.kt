package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityInput
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityResult
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityStatus
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SimulatedLookupMillis = 600L
private const val ShortestHandle = 3

/** The catalogue page of UsernameAvailabilityInput. */
object MonarchUsernameAvailabilityInputSection : CataloguePage {
    override val title = "UsernameAvailabilityInput"

    @Composable
    override fun Content() {
        var handle by rememberSaveable { mutableStateOf("") }
        var canCreate by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("A handle with a prefix. \"admin\" and \"rook\" are taken; \"offline\" fails.")
        MonarchUsernameAvailabilityInput(
            value = handle,
            label = "Handle",
            onValueChange = { handle = it },
            validate = { candidate ->
                delay(SimulatedLookupMillis)
                when {
                    candidate == "offline" -> error("The lookup failed")
                    candidate.length < ShortestHandle ->
                        MonarchUsernameAvailabilityResult(valid = false, message = "Use at least 3 characters.")
                    else -> MonarchUsernameAvailabilityResult(valid = candidate !in setOf("admin", "rook"))
                }
            },
            prefix = "rook.com/",
            helperText = "Letters, numbers and dashes.",
            transform = { it.lowercase() },
            onStatusChange = { status, _ -> canCreate = status == MonarchUsernameAvailabilityStatus.Valid },
        )
        MonarchButton(text = "Create", onClick = {}, variant = MonarchButtonVariant.Primary, enabled = canCreate)
    }
}
