package com.rookhq.monarch.components.scheduling.agendalist

/**
 * One calendar event: the neutral name of [MonarchAgendaEvent], for the
 * calendar month view, the time grid and other calendars that take the same
 * events as the agenda list. One type, so an app builds its events once and
 * shows them in every calendar.
 */
public typealias MonarchCalendarEvent = MonarchAgendaEvent
