package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorDirection
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceKind
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertSeverity
import com.rookhq.monarch.components.intelligence.aibackgroundtasks.MonarchAiBackgroundTaskStatus
import com.rookhq.monarch.components.intelligence.aiclaimsources.MonarchAiClaimSupport
import com.rookhq.monarch.components.intelligence.aiconfidencemeter.MonarchAiConfidenceLevel
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMessageFeedback
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind
import com.rookhq.monarch.components.intelligence.aidisclaimer.MonarchAiDisclaimerRisk
import com.rookhq.monarch.components.intelligence.aimentioncontext.MonarchAiMentionKind
import com.rookhq.monarch.components.intelligence.aimessageactions.MonarchAiMessageAction
import com.rookhq.monarch.components.intelligence.aitoolresultcard.MonarchAiToolResultKind
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.foundation.icon.dp
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The icon slots Monarch fills by itself, because the web component draws the same glyph by default
 * (platforms/icons.json). A component takes one as the default of its icon parameter, so an app passes an
 * icon only to replace it, or `null` to draw none. Decoration: no content description, hidden from TalkBack
 * by the component that places it.
 */
internal object MonarchDefaultIcons {
    private fun slot(glyph: ImageVector, size: MonarchIconSize = MonarchIconSize.Sm): @Composable () -> Unit =
        { MonarchIcon(glyph, contentDescription = null, size = size) }

    /** Draws [glyph] in the strong colour of [tone], as the web tints a status glyph. */
    @Composable
    fun Toned(tone: MonarchTone, glyph: @Composable () -> Unit) {
        val tint = MonarchTonePalette.of(tone, MonarchTheme.colors).strong
        CompositionLocalProvider(LocalContentColor provides tint, content = glyph)
    }

    /** The assistant mark (web `BotGlyph`). */
    val Bot: @Composable () -> Unit = slot(MonarchGlyphs.SmartToy)
    val CheckCircle: @Composable () -> Unit = slot(MonarchGlyphs.CheckCircle)
    val Dash: @Composable () -> Unit = slot(MonarchGlyphs.Remove)
    val Mail: @Composable () -> Unit = slot(MonarchGlyphs.Mail, MonarchIconSize.Md)
    val Ticket: @Composable () -> Unit = slot(MonarchGlyphs.ConfirmationNumber, MonarchIconSize.Md)
    /** The web's dot: a small filled circle (the glyph is drawn solid, not the outline ring). */
    val Dot: @Composable () -> Unit = slot(MonarchGlyphs.FiberManualRecord)
    val Error: @Composable () -> Unit = slot(MonarchGlyphs.Cancel)
    val Check: @Composable () -> Unit = slot(MonarchGlyphs.Check)
    val Close: @Composable () -> Unit = slot(MonarchGlyphs.Close)
    val Plus: @Composable () -> Unit = slot(MonarchGlyphs.Add)
    val Pencil: @Composable () -> Unit = slot(MonarchGlyphs.Edit)
    val Retry: @Composable () -> Unit = slot(MonarchGlyphs.Refresh)
    val Copy: @Composable () -> Unit = slot(MonarchGlyphs.ContentCopy)
    val Warning: @Composable () -> Unit = slot(MonarchGlyphs.Warning)
    val Play: @Composable () -> Unit = slot(MonarchGlyphs.PlayArrow)
    val Download: @Composable () -> Unit = slot(MonarchGlyphs.Download)
    val Upload: @Composable () -> Unit = slot(MonarchGlyphs.Upload)
    val Lock: @Composable () -> Unit = slot(MonarchGlyphs.Lock)
    val Info: @Composable () -> Unit = slot(MonarchGlyphs.Info)
    val Eye: @Composable () -> Unit = slot(MonarchGlyphs.Visibility)
    val EyeOff: @Composable () -> Unit = slot(MonarchGlyphs.VisibilityOff)
    val Star: @Composable () -> Unit = slot(MonarchGlyphs.Star)
    val Bell: @Composable () -> Unit = slot(MonarchGlyphs.Notifications)
    val More: @Composable () -> Unit = slot(MonarchGlyphs.MoreHoriz)
    val Calendar: @Composable () -> Unit = slot(MonarchGlyphs.CalendarToday)
    val ChevronDown: @Composable () -> Unit = slot(MonarchGlyphs.KeyboardArrowDown)
    val ChevronRight: @Composable () -> Unit = slot(MonarchGlyphs.KeyboardArrowRight)
    val ChevronLeft: @Composable () -> Unit = slot(MonarchGlyphs.KeyboardArrowLeft)
    val ChevronUp: @Composable () -> Unit = slot(MonarchGlyphs.KeyboardArrowUp)
    val ArrowForward: @Composable () -> Unit = slot(MonarchGlyphs.ArrowForward)
    val ThumbUp: @Composable () -> Unit = slot(MonarchGlyphs.ThumbUp)
    val ThumbDown: @Composable () -> Unit = slot(MonarchGlyphs.ThumbDown)
    val DoneAll: @Composable () -> Unit = slot(MonarchGlyphs.DoneAll)
    val Pause: @Composable () -> Unit = slot(MonarchGlyphs.Pause)
    val Comment: @Composable () -> Unit = slot(MonarchGlyphs.ChatBubble)
    val Trash: @Composable () -> Unit = slot(MonarchGlyphs.Delete)
    val Database: @Composable () -> Unit = slot(MonarchGlyphs.Database)
    /** The origin mark of an insight a fixed rule raised (web list glyph). */
    val RuleBased: @Composable () -> Unit = slot(MonarchGlyphs.FormatListBulleted)
    val AddReaction: @Composable () -> Unit = slot(MonarchGlyphs.AddReaction)
    val Share: @Composable () -> Unit = slot(MonarchGlyphs.IosShare)
    val Manage: @Composable () -> Unit = slot(MonarchGlyphs.Settings)
    val ReadAloud: @Composable () -> Unit = slot(MonarchGlyphs.VolumeUp)
    val Search: @Composable () -> Unit = slot(MonarchGlyphs.Search)
    val Partial: @Composable () -> Unit = slot(MonarchGlyphs.Contrast)
    val Spinner: @Composable () -> Unit = slot(MonarchGlyphs.ProgressActivity)
    val Sort: @Composable () -> Unit = slot(MonarchGlyphs.Sort)
    val Globe: @Composable () -> Unit = slot(MonarchGlyphs.Public)
    val Mic: @Composable () -> Unit = slot(MonarchGlyphs.MicNone)
    val CallEnd: @Composable () -> Unit = slot(MonarchGlyphs.CallEnd)
    val Grip: @Composable () -> Unit = slot(MonarchGlyphs.DragIndicator)
    val Expand: @Composable () -> Unit = slot(MonarchGlyphs.OpenInFull)
    val Book: @Composable () -> Unit = slot(MonarchGlyphs.MenuBook)
    val Key: @Composable () -> Unit = slot(MonarchGlyphs.VpnKey)
    val Stop: @Composable () -> Unit = slot(MonarchGlyphs.Stop)
    val Signal: @Composable () -> Unit = slot(MonarchGlyphs.SignalCellularAlt)
    val Flag: @Composable () -> Unit = slot(MonarchGlyphs.Flag)
    val Grid: @Composable () -> Unit = slot(MonarchGlyphs.GridView)
    val External: @Composable () -> Unit = slot(MonarchGlyphs.OpenInNew)
    val Captions: @Composable () -> Unit = slot(MonarchGlyphs.ClosedCaption)
    val Cast: @Composable () -> Unit = slot(MonarchGlyphs.Cast)
    val Fullscreen: @Composable () -> Unit = slot(MonarchGlyphs.Fullscreen)
    val Person: @Composable () -> Unit = slot(MonarchGlyphs.PersonOutline)
    val ArrowUp: @Composable () -> Unit = slot(MonarchGlyphs.ArrowUpward)
    val ArrowDown: @Composable () -> Unit = slot(MonarchGlyphs.ArrowDownward)
    val Undo: @Composable () -> Unit = slot(MonarchGlyphs.Undo)
    val Doc: @Composable () -> Unit = slot(MonarchGlyphs.Description)
    val Link: @Composable () -> Unit = slot(MonarchGlyphs.Link)
    val Table: @Composable () -> Unit = slot(MonarchGlyphs.TableChart)
    val Rss: @Composable () -> Unit = slot(MonarchGlyphs.RssFeed)
    val Clock: @Composable () -> Unit = slot(MonarchGlyphs.Schedule)
    val Help: @Composable () -> Unit = slot(MonarchGlyphs.Help)
    val Offline: @Composable () -> Unit = slot(MonarchGlyphs.WifiOff)
    val Sun: @Composable () -> Unit = slot(MonarchGlyphs.LightMode)
    val Moon: @Composable () -> Unit = slot(MonarchGlyphs.DarkMode)
    val Monitor: @Composable () -> Unit = slot(MonarchGlyphs.Monitor)
    val Pointer: @Composable () -> Unit = slot(MonarchGlyphs.NearMe)
    val Keyboard: @Composable () -> Unit = slot(MonarchGlyphs.Keyboard)
    val Scroll: @Composable () -> Unit = slot(MonarchGlyphs.SwapVert)
    val KeyPress: @Composable () -> Unit = slot(MonarchGlyphs.KeyboardCommandKey)
    val Waveform: @Composable () -> Unit = slot(MonarchGlyphs.GraphicEq)

    /** The glyph of a consequence kind, as the web: removed and kept are the circle marks, a change the swap. */
    val consequence: @Composable (MonarchConsequenceKind) -> Unit = { kind ->
        when (kind) {
            MonarchConsequenceKind.Removed -> slot(MonarchGlyphs.DoNotDisturbOn)()
            MonarchConsequenceKind.Changed -> slot(MonarchGlyphs.ChangeCircle)()
            MonarchConsequenceKind.Warning -> Warning()
            MonarchConsequenceKind.Kept -> CheckCircle()
        }
    }

    /** The icon before a confidence band label, as the web: check for high, half circle for medium, warning for low. */
    val confidence: @Composable (MonarchAiConfidenceLevel) -> Unit = { level ->
        when (level) {
            MonarchAiConfidenceLevel.High -> CheckCircle()
            MonarchAiConfidenceLevel.Medium -> Partial()
            MonarchAiConfidenceLevel.Low -> Warning()
        }
    }

    /** The mark before a usage meter's status line, as the web: the error mark when over, the warning triangle near. */
    val usageStatus: @Composable (Boolean) -> Unit = { over -> if (over) Error() else Warning() }

    /** The glyph before a background task, by status, as the web: clock, spinner, bell, check, error, dash. */
    val backgroundTaskStatus: @Composable (
        MonarchAiBackgroundTaskStatus,
    ) -> Unit = { status ->
        val (tone, glyph) = when (status) {
            MonarchAiBackgroundTaskStatus.Queued -> MonarchTone.Neutral to Clock
            MonarchAiBackgroundTaskStatus.Running -> MonarchTone.Ai to Spinner
            MonarchAiBackgroundTaskStatus.NeedsInput -> MonarchTone.Warning to Bell
            MonarchAiBackgroundTaskStatus.Succeeded -> MonarchTone.Success to CheckCircle
            MonarchAiBackgroundTaskStatus.Failed -> MonarchTone.Danger to Error
            MonarchAiBackgroundTaskStatus.Cancelled -> MonarchTone.Neutral to Dash
        }
        Toned(tone, glyph)
    }

    /** The mark before a claim's support line, as the web: check, half circle, error, warning, help. */
    val claimSupport: @Composable (
        MonarchAiClaimSupport,
    ) -> Unit = { support ->
        when (support) {
            MonarchAiClaimSupport.Supported -> CheckCircle()
            MonarchAiClaimSupport.Partial -> Partial()
            MonarchAiClaimSupport.Unsupported -> Error()
            MonarchAiClaimSupport.Conflicting -> Warning()
            MonarchAiClaimSupport.Unchecked -> Help()
        }
    }

    /** The glyph of a mention kind, as the web: a document, a table for a record, a grid for an app. */
    val mentionKind: @Composable (MonarchAiMentionKind) -> Unit = { kind ->
        when (kind) {
            MonarchAiMentionKind.Doc -> Doc()
            MonarchAiMentionKind.Record -> Table()
            MonarchAiMentionKind.App -> Grid()
            MonarchAiMentionKind.Person -> Person()
        }
    }

    /** The glyph of a source's kind, as the web: a page for a document or file, a globe, a table for a record. */
    val sourceKind: @Composable (MonarchAiSourceKind) -> Unit = { kind ->
        when (kind) {
            MonarchAiSourceKind.Web -> Globe()
            MonarchAiSourceKind.Record -> Table()
            MonarchAiSourceKind.Doc, MonarchAiSourceKind.File -> Doc()
        }
    }

    /** The mark of a plan step, as the web: a check for done, a dash for skipped, a dashed circle for to do. */
    val planStep: @Composable (
        MonarchAiPlanStepState,
    ) -> Unit = { state ->
        when (state) {
            MonarchAiPlanStepState.Done -> CheckCircle()
            MonarchAiPlanStepState.Skipped -> Dash()
            MonarchAiPlanStepState.Todo -> DashedCircle()
            MonarchAiPlanStepState.Failed -> Error()
            else -> Unit
        }
    }

    /** A dashed ring in the content colour, the web's pending marker. */
    val DashedCircle: @Composable () -> Unit = {
        val color = androidx.compose.material3.LocalContentColor.current
        val ring = MonarchIconSize.Sm.dp
        androidx.compose.foundation.Canvas(modifier = androidx.compose.ui.Modifier.size(ring)) {
            val stroke = MonarchTokens.Space.px.dp.toPx() * DASH_STROKE_SCALE
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2))
            drawCircle(
                color = color,
                radius = (size.minDimension - stroke) / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, pathEffect = dash),
            )
        }
    }

    private const val DASH_STROKE_SCALE = 1.5f

    /** The glyph of a tone, as the web alert: danger shows the warning triangle too. */
    fun tone(tone: MonarchTone): @Composable () -> Unit = when (tone) {
        MonarchTone.Success -> CheckCircle
        MonarchTone.Warning, MonarchTone.Danger -> Warning
        else -> Info
    }

    /** The glyph before a security alert, as the web: info, the warning triangle, the error mark when high. */
    val securitySeverity: @Composable (MonarchSecurityAlertSeverity) -> Unit = { severity ->
        when (severity) {
            MonarchSecurityAlertSeverity.Info -> Info()
            MonarchSecurityAlertSeverity.Low, MonarchSecurityAlertSeverity.Medium -> Warning()
            MonarchSecurityAlertSeverity.High, MonarchSecurityAlertSeverity.Critical -> Error()
        }
    }

    /** The glyph in the tile of a tool result card, by kind, as the web. */
    fun toolResult(kind: MonarchAiToolResultKind): @Composable () -> Unit = when (kind) {
        MonarchAiToolResultKind.RecordCreated -> Plus
        MonarchAiToolResultKind.EmailSent -> Mail
        MonarchAiToolResultKind.EventBooked -> Calendar
        MonarchAiToolResultKind.TicketFiled -> Ticket
        MonarchAiToolResultKind.Generic -> Check
    }

    /** The thumbs of the feedback buttons, as the web. */
    val AiFeedback: @Composable (MonarchAiMessageFeedback, Boolean) -> Unit = { value, _ ->
        if (value == MonarchAiMessageFeedback.Up) ThumbUp() else ThumbDown()
    }

    /** The glyph of each action of a message's action row, as the web (a check after a copy). */
    val AiMessageAction: @Composable (MonarchAiMessageAction, Boolean) -> Unit = { action, active ->
        when (action) {
            MonarchAiMessageAction.Copy -> if (active) Check() else Copy()
            MonarchAiMessageAction.Regenerate -> Retry()
            MonarchAiMessageAction.Edit -> Pencil()
            MonarchAiMessageAction.Good -> ThumbUp()
            MonarchAiMessageAction.Bad -> ThumbDown()
            MonarchAiMessageAction.Share -> Share()
            MonarchAiMessageAction.ReadAloud -> ReadAloud()
            MonarchAiMessageAction.More -> More()
        }
    }

    /** The leading glyph of a callout, as the web: a dot for neutral, then the tone glyphs. */
    fun callout(tone: MonarchTone): @Composable () -> Unit = when (tone) {
        MonarchTone.Neutral -> Dot
        MonarchTone.Danger -> Error
        else -> tone(tone)
    }

    /** The glyph of a device kind, as the web. */
    val Device: @Composable (MonarchDeviceKind) -> Unit = { kind ->
        MonarchIcon(
            when (kind) {
                MonarchDeviceKind.Desktop -> MonarchGlyphs.DesktopWindows
                MonarchDeviceKind.Mobile -> MonarchGlyphs.Smartphone
                MonarchDeviceKind.Tablet -> MonarchGlyphs.TabletMac
                else -> MonarchGlyphs.DesktopWindows
            },
            contentDescription = null,
            size = MonarchIconSize.Md,
        )
    }

    /** The trend arrow of a delta, as the web: diagonal up or down, a dash when flat. */
    val Delta: @Composable (MonarchDeltaIndicatorDirection) -> Unit = { direction ->
        when (direction) {
            MonarchDeltaIndicatorDirection.Up ->
                MonarchIcon(MonarchGlyphs.NorthEast, contentDescription = null, size = MonarchIconSize.Sm)
            MonarchDeltaIndicatorDirection.Down ->
                MonarchIcon(MonarchGlyphs.SouthEast, contentDescription = null, size = MonarchIconSize.Sm)
            MonarchDeltaIndicatorDirection.Flat ->
                MonarchIcon(MonarchGlyphs.Remove, contentDescription = null, size = MonarchIconSize.Sm)
        }
    }

    /** The leading glyph of an inline notice, as the web: the error mark, not the triangle, for danger. */
    fun notice(tone: MonarchTone): @Composable () -> Unit = if (tone == MonarchTone.Danger) Error else tone(tone)

    /** The mark of an AI disclaimer, as the web: the assistant for low risk, then info and warning. */
    fun disclaimer(risk: MonarchAiDisclaimerRisk): @Composable () -> Unit = when (risk) {
        MonarchAiDisclaimerRisk.Low -> Bot
        MonarchAiDisclaimerRisk.Medium -> Info
        MonarchAiDisclaimerRisk.High -> Warning
    }
}
