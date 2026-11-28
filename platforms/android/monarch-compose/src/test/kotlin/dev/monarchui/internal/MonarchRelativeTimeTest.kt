package dev.monarchui.internal

import dev.monarchui.internal.MonarchRelativeTime.Parts
import dev.monarchui.internal.MonarchRelativeTime.Unit
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The one relative-time rule, the web `formatRelativeTime` rule. The cases
 * mirror `packages/react/src/components/data-display/relative-time/relative-time.test.tsx`.
 */
@RunWith(RobolectricTestRunner::class)
class MonarchRelativeTimeTest {
    private val now = 1_800_000_000_000L
    private val second = 1_000L
    private val minute = 60_000L
    private val hour = 3_600_000L
    private val day = 86_400_000L
    private val en = Locale.US

    private fun format(time: Long, numeric: Boolean = false) =
        MonarchRelativeTime.format(time, now, en, numeric = numeric)

    @Test
    fun picksTheLargestUnitThatFitsAfterRoundingAsTheWebDoes() {
        assertEquals(Parts(-30, Unit.Second), MonarchRelativeTime.parts(now - 30 * second, now))
        assertEquals(Parts(-5, Unit.Minute), MonarchRelativeTime.parts(now - 5 * minute, now))
        assertEquals(Parts(-1, Unit.Hour), MonarchRelativeTime.parts(now - (59.6 * minute).toLong(), now))
        assertEquals(Parts(2, Unit.Day), MonarchRelativeTime.parts(now + 2 * day, now))
        assertEquals(Parts(-2, Unit.Week), MonarchRelativeTime.parts(now - 14 * day, now))
        assertEquals(Parts(-1, Unit.Year), MonarchRelativeTime.parts(now - 400 * day, now))
    }

    @Test
    fun theRolloversAreTheWebOnes() {
        // Math.round sends halves up, so the rollover is not symmetric: -59.5 s rounds to -59 (still seconds),
        // -59.6 s to -60 (a minute), and +59.5 s to 60 (a minute).
        assertEquals(Parts(-59, Unit.Second), MonarchRelativeTime.parts(now - 59_400L, now))
        assertEquals(Parts(-59, Unit.Second), MonarchRelativeTime.parts(now - 59_500L, now))
        assertEquals(Parts(-1, Unit.Minute), MonarchRelativeTime.parts(now - 59_600L, now))
        assertEquals(Parts(1, Unit.Minute), MonarchRelativeTime.parts(now + 59_500L, now))
        assertEquals(Parts(-23, Unit.Hour), MonarchRelativeTime.parts(now - 23 * hour, now))
        assertEquals(Parts(-1, Unit.Day), MonarchRelativeTime.parts(now - (23.6 * hour).toLong(), now))
        // 6.6 days rounds to 7, the day rollover, so it is a week.
        assertEquals(Parts(-6, Unit.Day), MonarchRelativeTime.parts(now - 6 * day, now))
        assertEquals(Parts(-1, Unit.Week), MonarchRelativeTime.parts(now - (6.6 * day).toLong(), now))
        // 4 weeks stays in weeks (below 4.345), and so do 30 and 31 days (4 rounded); 32 days rounds to 5 weeks,
        // past the rollover, so it is a month.
        assertEquals(Parts(-4, Unit.Week), MonarchRelativeTime.parts(now - 28 * day, now))
        assertEquals(Parts(-4, Unit.Week), MonarchRelativeTime.parts(now - 30 * day, now))
        assertEquals(Parts(-4, Unit.Week), MonarchRelativeTime.parts(now - 31 * day, now))
        assertEquals(Parts(-1, Unit.Month), MonarchRelativeTime.parts(now - 32 * day, now))
        assertEquals(Parts(-11, Unit.Month), MonarchRelativeTime.parts(now - 335 * day, now))
        // -350 days is -11.5 months, which rounds to -11; -351 days rounds to -12, so it is a year.
        assertEquals(Parts(-11, Unit.Month), MonarchRelativeTime.parts(now - 350 * day, now))
        assertEquals(Parts(-1, Unit.Year), MonarchRelativeTime.parts(now - 351 * day, now))
        assertEquals(Parts(-10, Unit.Year), MonarchRelativeTime.parts(now - 3_653 * day, now))
    }

    @Test
    fun fourToFiveWeeksNeverReadZeroMonths() {
        // The old rule switched to months at 28 days but divided by 30, so 28 and 29 days read "0 months ago".
        assertEquals("4 weeks ago", format(now - 27 * day))
        assertEquals("4 weeks ago", format(now - 28 * day))
        assertEquals("4 weeks ago", format(now - 29 * day))
        assertEquals("4 weeks ago", format(now - 30 * day))
        assertEquals("last month", format(now - 34 * day))
        (1..800).forEach { days ->
            val text = format(now - days * day)
            assertFalse(text.startsWith("0 "), "$days days: $text")
        }
    }

    @Test
    fun halvesRoundTowardsPositiveInfinityAsMathRoundDoes() {
        assertEquals(Parts(-2, Unit.Second), MonarchRelativeTime.parts(now - 2_500L, now))
        assertEquals(Parts(3, Unit.Second), MonarchRelativeTime.parts(now + 2_500L, now))
        assertEquals(Parts(0, Unit.Second), MonarchRelativeTime.parts(now - 400L, now))
        assertEquals(Parts(0, Unit.Second), MonarchRelativeTime.parts(now - 500L, now))
    }

    @Test
    fun formatsWithTheLocaleWordsByDefaultAndNumbersWhenAsked() {
        assertEquals("3 hours ago", format(now - 3 * hour))
        assertEquals("yesterday", format(now - day))
        assertEquals("1 day ago", format(now - day, numeric = true))
        assertEquals("now", format(now))
        assertEquals("in 2 days", format(now + 2 * day))
        assertEquals("30 seconds ago", format(now - 30 * second))
        assertEquals("5 minutes ago", format(now - 5 * minute))
        assertEquals("1 hour ago", format(now - hour))
        assertEquals("tomorrow", format(now + day))
        assertEquals("2 days ago", format(now - 2 * day))
        assertEquals("last week", format(now - 7 * day))
        assertEquals("2 weeks ago", format(now - 14 * day))
        assertEquals("next month", format(now + 35 * day))
        assertEquals("2 months ago", format(now - 60 * day))
        assertEquals("last year", format(now - 400 * day))
        assertEquals("3 years ago", format(now - 1_100 * day))
        // Intl writes a rounded zero as positive: "in 0 seconds".
        assertEquals("in 0 seconds", format(now - 100L, numeric = true))
    }

    @Test
    fun neverSwitchesToADateByItself() {
        assertEquals("10 years ago", format(now - 3_653 * day))
        assertFalse(MonarchRelativeTime.showsAbsolute(now - 3_653 * day, now, absoluteAfter = null))
        assertFalse(MonarchRelativeTime.showsAbsolute(now - 7 * day, now, absoluteAfter = 7 * day))
        assertTrue(MonarchRelativeTime.showsAbsolute(now - 7 * day - 1, now, absoluteAfter = 7 * day))
        assertTrue(MonarchRelativeTime.showsAbsolute(now + 8 * day, now, absoluteAfter = 7 * day))
    }

    @Test
    fun otherLocalesUseTheirOwnWords() {
        assertEquals("vor 3 Stunden", MonarchRelativeTime.format(now - 3 * hour, now, Locale.GERMANY))
        assertEquals("vorgestern", MonarchRelativeTime.format(now - 2 * day, now, Locale.GERMANY))
        assertEquals("vor 2 Tagen", MonarchRelativeTime.format(now - 2 * day, now, Locale.GERMANY, numeric = true))
        assertEquals("il y a 5 minutes", MonarchRelativeTime.format(now - 5 * minute, now, Locale.FRANCE))
    }

    @Test
    fun styleCaseAndTheFutureCanChange() {
        val short = MonarchRelativeTime.Style.Short
        assertEquals("3 min. ago", MonarchRelativeTime.format(now - 3 * minute, now, en, style = short))
        assertNotEquals(
            MonarchRelativeTime.format(now - 3 * minute, now, en),
            MonarchRelativeTime.format(now - 3 * minute, now, en, style = MonarchRelativeTime.Style.Narrow),
        )
        assertEquals("now", MonarchRelativeTime.format(now + 5 * minute, now, en, future = false))
        assertEquals("Now", MonarchRelativeTime.format(now, now, en, case = MonarchRelativeTime.Case.Sentence))
        val sentence = MonarchRelativeTime.Case.Sentence
        assertEquals("Yesterday", MonarchRelativeTime.format(now - day, now, en, case = sentence))
        assertEquals("now", MonarchRelativeTime.format(now, now, en, case = MonarchRelativeTime.Case.MidSentence))
    }

    @Test
    fun theRefreshFollowsTheWebLiveInterval() {
        assertEquals(5_000L, MonarchRelativeTime.refreshMillis(now - 10 * second, now))
        assertEquals(30_000L, MonarchRelativeTime.refreshMillis(now - 5 * minute, now))
        assertEquals(300_000L, MonarchRelativeTime.refreshMillis(now - 5 * hour, now))
        assertEquals(3_600_000L, MonarchRelativeTime.refreshMillis(now - 3 * day, now))
        val times = listOf(null, now - 3 * day, now - 2 * second)
        assertEquals(5_000L, MonarchRelativeTime.refreshMillis(times, now))
        assertEquals(3_600_000L, MonarchRelativeTime.refreshMillis(emptyList(), now))
    }
}
