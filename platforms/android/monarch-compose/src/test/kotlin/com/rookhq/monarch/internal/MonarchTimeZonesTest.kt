package com.rookhq.monarch.internal

import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The `timeZone: String?` rule of every component: an IANA id, `null` or an unknown id is the device zone. */
@RunWith(RobolectricTestRunner::class)
class MonarchTimeZonesTest {
    private lateinit var saved: TimeZone

    @Before
    fun setUp() {
        saved = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        MonarchTimeZones.resetReports()
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(saved)
    }

    @Test
    fun anIanaIdIsThatZone() {
        assertEquals("Europe/Berlin", MonarchTimeZones.java("Europe/Berlin").id)
        assertEquals("America/New_York", MonarchTimeZones.icu("America/New_York").id)
        assertEquals("UTC", MonarchTimeZones.java("UTC").id)
    }

    @Test
    fun nullIsTheDeviceZoneReadEachTime() {
        assertEquals("Asia/Tokyo", MonarchTimeZones.java(null).id)
        assertEquals("Asia/Tokyo", MonarchTimeZones.icu(null).id)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon"))
        assertEquals("Europe/Lisbon", MonarchTimeZones.java(null).id)
        assertEquals("Europe/Lisbon", MonarchTimeZones.icu(null).id)
    }

    @Test
    fun anInvalidIdIsTheDeviceZoneAndIsReported() {
        val heard = mutableListOf<String>()
        assertEquals("Asia/Tokyo", MonarchTimeZones.java("Mars/Olympus_Mons") { heard += it }.id)
        assertEquals("Asia/Tokyo", MonarchTimeZones.icu("Europe/Berlinn") { heard += it }.id)
        assertEquals("Asia/Tokyo", MonarchTimeZones.java("") { heard += it }.id)
        assertEquals(listOf("Mars/Olympus_Mons", "Europe/Berlinn", ""), heard)
    }

    @Test
    fun gmtAndCustomOffsetsAreValid() {
        assertTrue(MonarchTimeZones.isValid("GMT"))
        assertTrue(MonarchTimeZones.isValid("GMT+05:30"))
        assertTrue(MonarchTimeZones.isValid("Etc/GMT-3"))
        assertFalse(MonarchTimeZones.isValid("Not/AZone"))
        assertFalse(MonarchTimeZones.isValid(" "))
        assertEquals("GMT", MonarchTimeZones.java("GMT").id)
    }

    @Test
    fun theReportIsLoggedOncePerId() {
        MonarchTimeZones.report("Not/AZone")
        MonarchTimeZones.report("Not/AZone")
        MonarchTimeZones.report("Also/NotAZone")
        val logs = ShadowLog.getLogsForTag(MonarchTimeZones.TAG)
        assertEquals(2, logs.size)
        assertTrue(logs[0].msg.contains("Not/AZone"))
        assertTrue(logs[1].msg.contains("Also/NotAZone"))
    }
}
