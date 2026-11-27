package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCode
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCodes
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SimulatedRegenerateMillis = 800L

private val FirstSet = listOf("4f7k-2m9q", "8d3r-6t1w", "7n4h-3j8k", "2p6s-5l0z", "6v1c-9x3b", "3h8n-0q5e")
private val SecondSet = listOf("9a1e-4u7y", "3g8b-1q6r", "6m2t-7c4x", "5k0w-8h3d", "1r7p-2z9f", "0j4s-6w2a")

/** The catalogue page of RecoveryCodes. */
object MonarchRecoveryCodesSection : CataloguePage {
    override val title = "RecoveryCodes"

    @Composable
    override fun Content() {
        var second by rememberSaveable { mutableStateOf(false) }
        var continued by rememberSaveable { mutableIntStateOf(0) }
        val codes = (if (second) SecondSet else FirstSet).mapIndexed { index, code ->
            MonarchRecoveryCode(code, used = !second && index == 1)
        }
        CatalogueCaption("Setup: copy, save, generate new codes and acknowledge")
        MonarchRecoveryCodes(
            codes = codes,
            heading = "Example account recovery codes",
            onRegenerate = {
                delay(SimulatedRegenerateMillis)
                second = !second
            },
            onAcknowledge = { continued++ },
        )
        if (continued > 0) MonarchText(text = "Continued $continued times")
        CatalogueCaption("Review: hidden until shown")
        MonarchRecoveryCodes(codes = codes, initiallyRevealed = false)
    }
}
