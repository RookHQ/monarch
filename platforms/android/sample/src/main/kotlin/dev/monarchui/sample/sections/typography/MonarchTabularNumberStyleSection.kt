package dev.monarchui.sample.sections.typography

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumber
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of TabularNumber. */
object MonarchTabularNumberStyleSection : CataloguePage {
    override val title = "TabularNumber"

    @Composable
    override fun Content() {
        CatalogueCaption("A column of figures")
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            amounts.forEach { MonarchTabularNumber(value = it) }
        }
        CatalogueCaption("A counter that does not jitter")
        var count by rememberSaveable { mutableIntStateOf(FIRST_COUNT) }
        MonarchTabularNumber(value = count, style = MonarchTextStyle.Title)
        MonarchButton(text = "Add 1", onClick = { count += 1 })
    }

    private val amounts: List<Double?> = listOf(1111.11, null, 9048.5, 17.0)
    private const val FIRST_COUNT = 1108
}
