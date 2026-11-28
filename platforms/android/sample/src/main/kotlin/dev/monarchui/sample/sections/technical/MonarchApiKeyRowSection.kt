package dev.monarchui.sample.sections.technical

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.technical.apikeyrow.MonarchApiKeyRow
import dev.monarchui.components.technical.apikeyrow.MonarchApiKeyStatus
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SimulatedRequestMillis = 800L
private const val CreatedDaysAgo = 30
private const val ExpiresInDays = 5

/** The catalogue page of ApiKeyRow: a key to rotate and revoke, one that fails, and an expired one. */
object MonarchApiKeyRowSection : CataloguePage {
    override val title = "ApiKeyRow"

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var status by rememberSaveable { mutableStateOf(MonarchApiKeyStatus.Active) }
        CatalogueCaption("Rotate and revoke (type the name)")
        MonarchApiKeyRow(
            name = "Reporting job",
            prefix = "sk_live_4f2a",
            createdAt = now - CreatedDaysAgo * DateUtils.DAY_IN_MILLIS,
            lastUsedAt = now - DateUtils.HOUR_IN_MILLIS,
            scopes = listOf("items:read", "items:write", "reports:read", "users:read"),
            status = status,
            revokeConfirmPhrase = "Reporting job",
            onRotate = { delay(SimulatedRequestMillis) },
            onRevoke = {
                delay(SimulatedRequestMillis)
                status = MonarchApiKeyStatus.Revoked
            },
        )
        CatalogueCaption("Rotation fails")
        MonarchApiKeyRow(
            name = "Mobile app",
            prefix = "pk_live_8d2c",
            createdAt = now - CreatedDaysAgo * DateUtils.DAY_IN_MILLIS,
            expiresAt = now + ExpiresInDays * DateUtils.DAY_IN_MILLIS,
            status = MonarchApiKeyStatus.Expiring,
            onRotate = {
                delay(SimulatedRequestMillis)
                error("The server did not answer.")
            },
        )
        CatalogueCaption("Expired")
        MonarchApiKeyRow(
            name = "Old CI key",
            prefix = "sk_live_9a01",
            createdAt = now - CreatedDaysAgo * 2 * DateUtils.DAY_IN_MILLIS,
            expiresAt = now - ExpiresInDays * DateUtils.DAY_IN_MILLIS,
            status = MonarchApiKeyStatus.Expired,
            onRevoke = {},
        )
    }
}
