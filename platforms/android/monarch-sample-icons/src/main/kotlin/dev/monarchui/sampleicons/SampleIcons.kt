package dev.monarchui.sampleicons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ScreenShare
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.ThumbDownOffAlt
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Real icons for examples, previews, tests and the sample app: the Material icons, Outlined style, 24dp,
 * drawn in `LocalContentColor`. One place to swap the icon source.
 *
 * Outline glyphs only. A few icons of the Material Outlined set are solid shapes (`Star`, `Error`, `Warning`,
 * `Mic`); their hollow variants (`StarOutline`, `ErrorOutline`, `WarningAmber`, `MicNone`) are used instead, so
 * a filled glyph never sits next to an outlined one (PREMIUM.md rule 9). `SampleIconsOutlineTest` checks it.
 *
 * The names follow the role, not the Material name: `SampleIcons.Search`, `SampleIcons.Close`. The mapping
 * from the web (lucide) glyphs and from `android.R.drawable.*` is in the visual spec, `icons.md`. There is no
 * sparkle: AI content uses [Bot].
 *
 * This is a sample and test module. Monarch for Android bundles no icons; an app passes its own.
 */
public object SampleIcons {
    // Marks used inside components.
    public val Check: ImageVector get() = Icons.Outlined.Check
    public val Dash: ImageVector get() = Icons.Outlined.Remove
    public val ChevronDown: ImageVector get() = Icons.Outlined.KeyboardArrowDown
    public val ChevronUp: ImageVector get() = Icons.Outlined.KeyboardArrowUp
    public val ChevronRight: ImageVector get() = Icons.AutoMirrored.Outlined.KeyboardArrowRight
    public val ChevronLeft: ImageVector get() = Icons.AutoMirrored.Outlined.KeyboardArrowLeft
    public val Close: ImageVector get() = Icons.Outlined.Close
    public val Dot: ImageVector get() = Icons.Outlined.FiberManualRecord
    public val Info: ImageVector get() = Icons.Outlined.Info
    public val Warning: ImageVector get() = Icons.Outlined.WarningAmber
    public val Success: ImageVector get() = Icons.Outlined.CheckCircle
    public val Error: ImageVector get() = Icons.Outlined.ErrorOutline
    public val Partial: ImageVector get() = Icons.Outlined.Contrast
    public val Help: ImageVector get() = Icons.AutoMirrored.Outlined.HelpOutline

    /** A lifebuoy drawn in 2px strokes, the web's Help and support glyph in the Rook rail. */
    public val Lifebuoy: ImageVector by lazy { strokeLifebuoy() }

    // Actions and objects.
    public val Copy: ImageVector get() = Icons.Outlined.ContentCopy
    public val Pause: ImageVector get() = Icons.Outlined.Pause
    public val PauseCircle: ImageVector get() = Icons.Outlined.PauseCircle
    public val Briefcase: ImageVector get() = Icons.Outlined.BusinessCenter
    public val Table: ImageVector get() = Icons.Outlined.TableChart
    /** A cylinder drawn in 2px strokes, the web's database glyph (the Material Storage icon is a server stack). */
    public val Database: ImageVector by lazy { strokeDatabase() }
    public val Eye: ImageVector get() = Icons.Outlined.Visibility
    public val EyeOff: ImageVector get() = Icons.Outlined.VisibilityOff
    public val Wrench: ImageVector get() = Icons.Outlined.Build
    public val Lock: ImageVector get() = Icons.Outlined.Lock
    public val Offline: ImageVector get() = Icons.Outlined.WifiOff
    public val Search: ImageVector get() = Icons.Outlined.Search
    public val Retry: ImageVector get() = Icons.Outlined.Refresh
    /**
     * A pencil drawn in 2px strokes on the 24px grid. The outlined Material set's Edit is a solid glyph, which looks
     * filled next to the outlined copy and share icons (PREMIUM rule 9).
     */
    public val Pencil: ImageVector by lazy { strokePencil() }
    public val Menu: ImageVector get() = Icons.Outlined.Menu
    public val Calendar: ImageVector get() = Icons.Outlined.CalendarToday
    public val CalendarMonth: ImageVector get() = Icons.Outlined.CalendarMonth
    public val Clock: ImageVector get() = Icons.Outlined.Schedule
    public val More: ImageVector get() = Icons.Outlined.MoreHoriz
    public val MoreVertical: ImageVector get() = Icons.Outlined.MoreVert
    public val Columns: ImageVector get() = Icons.Outlined.ViewColumn
    public val Filter: ImageVector get() = Icons.Outlined.FilterList
    public val Plus: ImageVector get() = Icons.Outlined.Add
    public val Star: ImageVector get() = Icons.Outlined.StarOutline
    public val Bell: ImageVector get() = Icons.Outlined.Notifications
    public val BellOff: ImageVector get() = Icons.Outlined.NotificationsOff
    public val ArrowDown: ImageVector get() = Icons.Outlined.ArrowDownward
    public val ArrowUp: ImageVector get() = Icons.Outlined.ArrowUpward
    public val ArrowBack: ImageVector get() = Icons.AutoMirrored.Outlined.ArrowBack
    public val ArrowForward: ImageVector get() = Icons.AutoMirrored.Outlined.ArrowForward
    public val Logout: ImageVector get() = Icons.AutoMirrored.Outlined.Logout
    public val Key: ImageVector get() = Icons.Outlined.Key
    public val Upload: ImageVector get() = Icons.Outlined.FileUpload
    public val Layers: ImageVector get() = Icons.Outlined.Layers

    /** The AI glyph. A robot, never a sparkle. */
    public val Bot: ImageVector get() = Icons.Outlined.SmartToy

    // Views and navigation.
    public val List: ImageVector get() = Icons.AutoMirrored.Outlined.ViewList
    public val ListSimple: ImageVector get() = Icons.AutoMirrored.Outlined.List
    public val Board: ImageVector get() = Icons.Outlined.ViewKanban
    public val Grid: ImageVector get() = Icons.Outlined.GridView
    public val Window: ImageVector get() = Icons.Outlined.WebAsset
    public val Agenda: ImageVector get() = Icons.Outlined.ViewAgenda
    public val Dashboard: ImageVector get() = Icons.Outlined.Dashboard
    public val Home: ImageVector get() = Icons.Outlined.Home
    public val Inbox: ImageVector get() = Icons.Outlined.Inbox
    public val Users: ImageVector get() = Icons.Outlined.Group
    public val Settings: ImageVector get() = Icons.Outlined.Settings

    // Domain.
    public val Trash: ImageVector get() = Icons.Outlined.Delete
    public val Card: ImageVector get() = Icons.Outlined.CreditCard
    public val Call: ImageVector get() = Icons.Outlined.Call
    public val Priority: ImageVector get() = Icons.Outlined.Flag
    public val Share: ImageVector get() = Icons.Outlined.Share
    public val Mic: ImageVector get() = Icons.Outlined.MicNone
    public val Image: ImageVector get() = Icons.Outlined.Image
    public val Play: ImageVector get() = Icons.Outlined.PlayArrow
    public val Undo: ImageVector get() = Icons.AutoMirrored.Outlined.Undo
    public val Sort: ImageVector get() = Icons.AutoMirrored.Outlined.Sort
    public val Send: ImageVector get() = Icons.AutoMirrored.Outlined.Send
    public val Mail: ImageVector get() = Icons.Outlined.Mail
    public val MicOff: ImageVector get() = Icons.Outlined.MicOff
    public val Video: ImageVector get() = Icons.Outlined.Videocam
    public val VideoOff: ImageVector get() = Icons.Outlined.VideocamOff
    public val CallEnd: ImageVector get() = Icons.Outlined.PhoneDisabled
    public val CallIncoming: ImageVector get() = Icons.AutoMirrored.Outlined.CallReceived
    public val ScreenShare: ImageVector get() = Icons.AutoMirrored.Outlined.ScreenShare
    public val RaiseHand: ImageVector get() = Icons.Outlined.PanTool
    public val React: ImageVector get() = Icons.Outlined.AddReaction
    public val Download: ImageVector get() = Icons.Outlined.Download
    public val Map: ImageVector get() = Icons.Outlined.Map
    public val Pin: ImageVector get() = Icons.Outlined.PushPin
    public val Shrink: ImageVector get() = Icons.Outlined.CloseFullscreen
    public val Bold: ImageVector get() = Icons.Outlined.FormatBold
    public val BulletList: ImageVector get() = Icons.AutoMirrored.Outlined.FormatListBulleted
    public val Devices: ImageVector get() = Icons.Outlined.Devices
    public val Building: ImageVector get() = Icons.Outlined.Business
    public val LightMode: ImageVector get() = Icons.Outlined.LightMode
    public val DarkMode: ImageVector get() = Icons.Outlined.DarkMode
    public val Palette: ImageVector get() = Icons.Outlined.Palette
    public val ThumbUp: ImageVector get() = Icons.Outlined.ThumbUpOffAlt
    public val ThumbDown: ImageVector get() = Icons.Outlined.ThumbDownOffAlt
    public val ReadAloud: ImageVector get() = Icons.AutoMirrored.Outlined.VolumeUp
    public val Italic: ImageVector get() = Icons.Outlined.FormatItalic
    public val Strikethrough: ImageVector get() = Icons.Outlined.StrikethroughS
    public val Code: ImageVector get() = Icons.Outlined.Code
    public val Link: ImageVector get() = Icons.Outlined.Link
    public val Globe: ImageVector get() = Icons.Outlined.Language
    public val Document: ImageVector get() = Icons.Outlined.Description
    public val Attach: ImageVector get() = Icons.Outlined.AttachFile
    public val Person: ImageVector get() = Icons.Outlined.Person
    public val History: ImageVector get() = Icons.Outlined.History
    public val Archive: ImageVector get() = Icons.Outlined.Archive
    public val MarkUnread: ImageVector get() = Icons.Outlined.MarkEmailUnread
    public val Rss: ImageVector get() = Icons.Outlined.RssFeed
    public val Cancel: ImageVector get() = Icons.Outlined.Cancel
    public val PanelLeft: ImageVector get() = Icons.AutoMirrored.Outlined.ViewSidebar
    public val Checklist: ImageVector get() = Icons.Outlined.Checklist
    public val Expand: ImageVector get() = Icons.Outlined.Fullscreen
    public val Grip: ImageVector get() = Icons.Outlined.DragIndicator

    /** A stand-in for "an icon" when the example does not care which: the star. Used by `TestIcon`. */
    public val Placeholder: ImageVector get() = Icons.Outlined.StarOutline

    /**
     * The replacement for each `android.R.drawable` name, as listed in the visual spec (icons.md). The key is
     * the drawable name without the package, for example `ic_menu_search`.
     */
    public val ForLegacyDrawable: Map<String, ImageVector>
        get() = mapOf(
            "ic_media_previous" to ChevronLeft,
            "ic_media_next" to ChevronRight,
            "ic_menu_close_clear_cancel" to Close,
            "ic_menu_view" to Eye,
            "ic_menu_search" to Search,
            "ic_media_play" to Play,
            "ic_menu_more" to More,
            "ic_menu_gallery" to Image,
            "arrow_down_float" to ChevronDown,
            "arrow_up_float" to ChevronUp,
            "ic_dialog_info" to Info,
            "ic_dialog_alert" to Warning,
            "ic_btn_speak_now" to Mic,
            "ic_popup_reminder" to Bell,
            "ic_menu_agenda" to Agenda,
            "ic_menu_revert" to Undo,
            "ic_menu_edit" to Pencil,
            "ic_menu_share" to Share,
            "ic_lock_silent_mode" to BellOff,
            "ic_menu_sort_by_size" to Sort,
            "ic_menu_add" to Plus,
            "ic_menu_delete" to Trash,
            "ic_menu_call" to Call,
            "ic_menu_send" to Send,
            "ic_menu_help" to Help,
            "ic_lock_lock" to Lock,
            "ic_menu_month" to CalendarMonth,
            "star_big_on" to Star,
            "star_on" to Star,
            "btn_star" to Star,
            "ic_input_add" to Plus,
            "ic_delete" to Trash,
            "ic_menu_info_details" to Info,
            "ic_menu_camera" to Video,
            "ic_menu_manage" to Settings,
            "ic_menu_mapmode" to Map,
            "ic_menu_mylocation" to Pin,
            "ic_menu_myplaces" to Building,
            "ic_menu_save" to Copy,
            "sym_call_incoming" to CallIncoming,
            "stat_sys_download" to Download,
        )
}

/** The pencil of [SampleIcons.Pencil]: the body from the tip to the cap, and the line of the ferrule. */
private fun strokePencil(): ImageVector =
    ImageVector.Builder(
        name = "SampleIcons.Pencil",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString("M4 20 L4 16.5 L15.5 5 L19 8.5 L7.5 20 Z M13 7.5 L16.5 11").toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()


/** The cylinder of [SampleIcons.Database]: the top ellipse, the two sides with the base, and the middle band. */
private fun strokeDatabase(): ImageVector = strokeGlyph(
    "SampleIcons.Database",
    "M21 5 C21 6.66 16.97 8 12 8 C7.03 8 3 6.66 3 5 C3 3.34 7.03 2 12 2 C16.97 2 21 3.34 21 5 Z " +
        "M3 5 L3 19 C3 20.66 7.03 22 12 22 C16.97 22 21 20.66 21 19 L21 5 " +
        "M3 12 C3 13.66 7.03 15 12 15 C16.97 15 21 13.66 21 12",
)

/** The lifebuoy of [SampleIcons.Lifebuoy]: the ring, the inner circle and the four struts between them. */
private fun strokeLifebuoy(): ImageVector = strokeGlyph(
    "SampleIcons.Lifebuoy",
    "M22 12 A10 10 0 1 1 2 12 A10 10 0 1 1 22 12 Z M16 12 A4 4 0 1 1 8 12 A4 4 0 1 1 16 12 Z " +
        "M4.93 4.93 L9.17 9.17 M14.83 9.17 L19.07 4.93 M14.83 14.83 L19.07 19.07 M9.17 14.83 L4.93 19.07",
)

private fun strokeGlyph(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
