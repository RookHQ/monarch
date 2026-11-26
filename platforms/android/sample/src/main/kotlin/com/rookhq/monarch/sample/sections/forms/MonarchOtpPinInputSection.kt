package com.rookhq.monarch.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.otppininput.MonarchOtpPinInput
import com.rookhq.monarch.components.forms.otppininput.MonarchOtpPinInputMode
import com.rookhq.monarch.components.forms.otppininput.MonarchOtpPinInputSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of OTPPinInput. */
object MonarchOtpPinInputSection : CataloguePage {
    override val title = "OTPPinInput"

    private const val PIN_LENGTH = 4
    private const val GROUP = 3
    private const val EXAMPLE_CODE = "123456"

    @Composable
    override fun Content() {
        var code by rememberSaveable { mutableStateOf("") }
        var checked by rememberSaveable { mutableStateOf<String?>(null) }
        var pin by rememberSaveable { mutableStateOf("") }
        var room by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("A six-digit code; $EXAMPLE_CODE is accepted")
        MonarchOtpPinInput(
            value = code,
            onValueChange = {
                code = it
                checked = null
            },
            label = "Verification code",
            helperText = "Sent to your phone by text message.",
            errorText = if (checked != null && checked != EXAMPLE_CODE) "That code is not right. Try again." else null,
            groupSize = GROUP,
            onComplete = { checked = it },
        )
        CatalogueCaption("A masked PIN")
        MonarchOtpPinInput(
            value = pin,
            onValueChange = { pin = it },
            label = "PIN",
            length = PIN_LENGTH,
            masked = true,
        )
        CatalogueCaption("Letters and digits, large")
        MonarchOtpPinInput(
            value = room,
            onValueChange = { room = it },
            label = "Room code",
            length = PIN_LENGTH,
            mode = MonarchOtpPinInputMode.Alphanumeric,
            size = MonarchOtpPinInputSize.Lg,
        )
    }
}
