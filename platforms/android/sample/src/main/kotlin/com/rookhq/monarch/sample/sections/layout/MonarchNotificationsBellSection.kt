package com.rookhq.monarch.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.layout.notificationsbell.MonarchNotificationsBell
import com.rookhq.monarch.components.layout.notificationsbell.MonarchNotificationsBellVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of NotificationsBell. */
object MonarchNotificationsBellSection : CataloguePage {
    override val title = "NotificationsBell"

    private const val MANY = 120
    private const val START = 3

    /** A bell drawn for this page; Monarch bundles no icons. */
    private val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                curveTo(8.7f, 3f, 6f, 5.7f, 6f, 9f)
                verticalLineTo(14f)
                lineTo(4f, 17f)
                verticalLineTo(18f)
                horizontalLineTo(20f)
                verticalLineTo(17f)
                lineTo(18f, 14f)
                verticalLineTo(9f)
                curveTo(18f, 5.7f, 15.3f, 3f, 12f, 3f)
                close()
                moveTo(10f, 19f)
                horizontalLineTo(14f)
                curveTo(14f, 20.1f, 13.1f, 21f, 12f, 21f)
                curveTo(10.9f, 21f, 10f, 20.1f, 10f, 19f)
                close()
            }
        }.build()
    }

    @Composable
    override fun Content() {
        var unread by rememberSaveable { mutableIntStateOf(START) }
        CatalogueCaption("Icon, for a top app bar")
        MonarchNotificationsBell(onClick = { unread = 0 }, unreadCount = unread) {
            MonarchIcon(Bell, contentDescription = null)
        }
        MonarchButton(text = "Add a notification", onClick = { unread++ })

        CatalogueCaption("More than 99 unread")
        MonarchNotificationsBell(onClick = {}, unreadCount = MANY) { MonarchIcon(Bell, contentDescription = null) }

        CatalogueCaption("Row, for a drawer or a menu")
        MonarchNotificationsBell(
            onClick = { unread = 0 },
            unreadCount = unread,
            variant = MonarchNotificationsBellVariant.Row,
        ) {
            MonarchIcon(Bell, contentDescription = null)
        }
    }
}
