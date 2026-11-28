package com.rookhq.monarch.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.switchsettingslist.MonarchSwitchSettingsList
import com.rookhq.monarch.components.forms.switchsettingslist.MonarchSwitchSettingsRow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of SwitchSettingsList. */
object MonarchSwitchSettingsListSection : CataloguePage {
    override val title = "SwitchSettingsList"

    private const val SAVE_MILLIS = 800L

    @Composable
    override fun Content() {
        var weekly by rememberSaveable { mutableStateOf(true) }
        var news by rememberSaveable { mutableStateOf(false) }
        var offline by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Each row saves on its own; the last one always fails")
        MonarchSwitchSettingsList(
            title = "Email notifications",
            description = "Each setting saves as soon as you change it.",
        ) {
            MonarchSwitchSettingsRow(
                checked = weekly,
                onCheckedChange = { weekly = it },
                onSave = { delay(SAVE_MILLIS) },
                label = "Weekly summary",
                description = "Sent on Mondays.",
            )
            MonarchSwitchSettingsRow(
                checked = news,
                onCheckedChange = { news = it },
                onSave = { delay(SAVE_MILLIS) },
                label = "Product news",
            )
            MonarchSwitchSettingsRow(
                checked = offline,
                onCheckedChange = { offline = it },
                onSave = {
                    delay(SAVE_MILLIS)
                    error("The server could not be reached.")
                },
                label = "Security alerts",
            )
        }
    }
}
