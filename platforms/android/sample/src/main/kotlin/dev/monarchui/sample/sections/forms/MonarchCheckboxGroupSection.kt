package dev.monarchui.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroup
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroupOrientation
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of CheckboxGroup. */
object MonarchCheckboxGroupSection : CataloguePage {
    override val title = "CheckboxGroup"

    private val channels = listOf(
        MonarchCheckboxGroupOption("email", "Email", description = "A summary every morning."),
        MonarchCheckboxGroupOption("push", "Push notifications"),
        MonarchCheckboxGroupOption(
            "sms",
            "Text message",
            description = "Not available in your region.",
            enabled = false,
        ),
    )

    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri").map { MonarchCheckboxGroupOption(it, it) }

    @Composable
    override fun Content() {
        var picked by rememberSaveable { mutableStateOf(listOf("email")) }
        var workdays by rememberSaveable { mutableStateOf(listOf<String>()) }
        MonarchCheckboxGroup(
            options = channels,
            value = picked,
            onValueChange = { picked = it },
            required = true,
            selectAll = true,
            label = "Notify me by",
            errorText = if (picked.isEmpty()) "Choose at least one channel." else null,
        )
        CatalogueCaption("Horizontal")
        MonarchCheckboxGroup(
            options = days,
            value = workdays,
            onValueChange = { workdays = it },
            orientation = MonarchCheckboxGroupOrientation.Horizontal,
            label = "Working days",
            helperText = "Reminders are sent on these days only.",
        )
        CatalogueCaption("Disabled")
        MonarchCheckboxGroup(options = channels, value = listOf("push"), onValueChange = {}, enabled = false)
    }
}
