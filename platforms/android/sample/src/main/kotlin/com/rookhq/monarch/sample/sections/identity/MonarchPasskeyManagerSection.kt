package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskey
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskeyManager
import com.rookhq.monarch.components.identity.passkeymanager.MonarchPasskeyManagerState
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val DayMillis = 86_400_000L
private const val MonthDays = 30L
private const val SimulatedRequestMillis = 800L

/** The catalogue page of PasskeyManager. */
object MonarchPasskeyManagerSection : CataloguePage {
    override val title = "PasskeyManager"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var names by rememberSaveable {
            mutableStateOf(mapOf("google" to "Google Password Manager", "key" to "Security key"))
        }
        var adds by rememberSaveable { mutableIntStateOf(0) }
        val passkeys = names.map { (id, name) ->
            MonarchPasskey(
                id = id,
                name = name,
                createdMillis = now - MonthDays * DayMillis,
                lastUsedMillis = if (id == "google") now - DayMillis else null,
                synced = id == "google",
            )
        }
        CatalogueCaption("Every second add is cancelled, as if the person closed the system sheet")
        MonarchPasskeyManager(
            passkeys = passkeys,
            now = now,
            lastSignInMethod = true,
            onAdd = {
                delay(SimulatedRequestMillis)
                adds++
                if (adds % 2 == 0) {
                    false
                } else {
                    names = names + ("new-$adds" to "Passkey $adds")
                    true
                }
            },
            onRename = { id, name ->
                delay(SimulatedRequestMillis)
                names = names + (id to name)
            },
            onRemove = { id ->
                delay(SimulatedRequestMillis)
                names = names - id
            },
        )
        CatalogueCaption("A device that cannot create passkeys, and loading")
        MonarchPasskeyManager(passkeys = emptyList(), supported = false, onAdd = { true })
        MonarchPasskeyManager(passkeys = emptyList(), state = MonarchPasskeyManagerState.Loading)
    }
}
