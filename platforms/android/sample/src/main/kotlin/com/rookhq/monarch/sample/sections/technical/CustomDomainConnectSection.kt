package com.rookhq.monarch.sample.sections.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecord
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecordPurpose
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecordTable
import com.rookhq.monarch.components.technical.domainprogress.MonarchDomainProgress
import com.rookhq.monarch.components.technical.domainprogress.MonarchDomainStage
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SimulatedServerMillis = 700L

/** The catalogue page of CustomDomainConnect: enter a domain, add the records, verify, remove. */
object CustomDomainConnectSection : CataloguePage {
    override val title = "CustomDomainConnect"

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var domain by rememberSaveable { mutableStateOf<String?>(null) }
        var typed by rememberSaveable { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var connecting by remember { mutableStateOf(false) }
        var verifying by remember { mutableStateOf(false) }
        var found by rememberSaveable { mutableStateOf(false) }
        var removing by remember { mutableStateOf(false) }
        CatalogueCaption("Connect a help center domain")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            val current = domain
            if (current == null) {
                MonarchTextField(
                    value = typed,
                    onValueChange = {
                        typed = it
                        error = null
                    },
                    label = "Domain",
                    placeholder = "help.example.com",
                    helperText = "A subdomain you own.",
                    errorText = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                MonarchButton(
                    text = "Connect domain",
                    onClick = {
                        val host = typed.trim().lowercase().removeSuffix(".")
                        if (host.count { it == '.' } < 2) {
                            error = "Enter a subdomain, such as help.example.com."
                        } else {
                            connecting = true
                            scope.launch {
                                delay(SimulatedServerMillis)
                                domain = host
                                typed = ""
                                connecting = false
                            }
                        }
                    },
                    variant = MonarchButtonVariant.Primary,
                    loading = connecting,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(text = current, modifier = Modifier.weight(1f), style = MonarchTextStyle.Heading)
                    MonarchButton(
                        text = "Remove domain",
                        onClick = { removing = true },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
                MonarchDomainProgress(
                    host = current,
                    stage = if (found) MonarchDomainStage.Certificate else MonarchDomainStage.Dns,
                    rechecking = !verifying,
                )
                MonarchText(text = "Add these records where the DNS for your domain is managed.")
                MonarchDnsRecordTable(
                    records = listOf(
                        MonarchDnsRecord(
                            type = "CNAME",
                            name = current,
                            value = "domains.example.net",
                            purpose = MonarchDnsRecordPurpose.Routing,
                            found = found,
                        ),
                        MonarchDnsRecord(
                            type = "TXT",
                            name = "_verify.$current",
                            value = "verify=4f7k2m9q",
                            purpose = MonarchDnsRecordPurpose.Verification,
                            found = found,
                        ),
                    ),
                )
                MonarchButton(
                    text = if (found) "Check again" else "Verify DNS",
                    onClick = {
                        verifying = true
                        scope.launch {
                            delay(SimulatedServerMillis)
                            found = true
                            verifying = false
                        }
                    },
                    loading = verifying,
                )
            }
        }
        val current = domain
        if (removing && current != null) {
            MonarchDialog(
                onDismissRequest = { removing = false },
                title = "Remove $current?",
                description = "The domain stops working here. You can connect it again later.",
                actions = {
                    MonarchButton(text = "Cancel", onClick = { removing = false }, variant = MonarchButtonVariant.Ghost)
                    MonarchButton(
                        text = "Remove domain",
                        onClick = {
                            domain = null
                            found = false
                            removing = false
                        },
                        variant = MonarchButtonVariant.Danger,
                    )
                },
            )
        }
    }
}
