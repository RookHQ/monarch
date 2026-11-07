/**
 * `@rookcom/monarch-react/scheduling`: date and time pickers, calendar views
 * and availability editors. Kept out of the root entry so apps without
 * scheduling UI never load them. Components are added here by wave 3
 * (registry/roadmap-waves.json).
 */
// Pickers (wave 3 group A): timezone-select, duration-input.
export {
  TimezoneSelect,
  getTimezoneInfo,
  listTimezones,
  formatUtcOffset,
  canonicalTimezone,
  type TimezoneSelectProps,
  type TimezoneInfo,
  type TimezoneSelectMessages,
} from '../components/scheduling/timezone-select/index.js';
export {
  DurationInput,
  parseDuration,
  formatDuration,
  type DurationInputProps,
  type DurationInputMessages,
  type DurationFormatOptions,
} from '../components/forms/duration-input/index.js';
// Date pickers (wave 3, group B).
export {
  MiniMonth,
  type MiniMonthProps,
  type MiniMonthDayInfo,
  type MiniMonthDotTone,
  type MiniMonthLabels,
  type MiniMonthRange,
  type IsoDate,
  type Weekday,
} from '../components/scheduling/mini-month/index.js';
export {
  DatePicker,
  type DatePickerProps,
  type DatePickerPreset,
  type DatePickerMessages,
  type DateValidationError,
} from '../components/scheduling/date-picker/index.js';
export {
  DateTimePicker,
  type DateTimePickerProps,
  type DateTimePickerMessages,
  type DateTimeValidationError,
  type IsoDateTime,
  type HourCycle,
} from '../components/scheduling/date-time-picker/index.js';
export {
  DateRangePicker,
  type DateRangePickerProps,
  type DateRange,
  type DateRangePreset,
  type DateRangePickerMessages,
  type DateRangeValidationError,
} from '../components/search/date-range-picker/index.js';

// Calendar views (wave 3, group C).
export {
  EventChip,
  type CalendarEvent,
  type EventChipProps,
  type EventChipStatus,
  type EventChipVariant,
} from '../components/scheduling/event-chip/index.js';
export {
  AgendaList,
  type AgendaListLabels,
  type AgendaListProps,
} from '../components/scheduling/agenda-list/index.js';
export {
  CalendarMonthView,
  type CalendarMonthViewDayState,
  type CalendarMonthViewLabels,
  type CalendarMonthViewProps,
} from '../components/scheduling/calendar-month-view/index.js';
export {
  TimeGrid,
  type TimeGridEventMove,
  type TimeGridLabels,
  type TimeGridProps,
  type TimeGridSlot,
  type TimeGridUnavailable,
} from '../components/scheduling/time-grid/index.js';
export {
  WeeklyHoursEditor,
  validateWeeklyHours,
  type WeeklyHoursEditorLabels,
  type WeeklyHoursEditorProps,
  type WeeklyHoursError,
  type WeeklyHoursInterval,
  type WeeklyHoursValue,
} from '../components/scheduling/weekly-hours-editor/index.js';
export {
  SlotPicker,
  type SlotPickerSlot,
  type SlotPickerMessages,
  type SlotPickerProps,
} from '../components/scheduling/slot-picker/index.js';
export {
  useBookingFlow,
  BookingFlow,
  type BookingFlowStep,
  type BookingFlowMessages,
  type BookingFlowProps,
} from '../patterns/scheduling/booking-flow/index.js';
export {
  RescheduleDialog,
  type RescheduleDetails,
  type RescheduleDialogMessages,
  type RescheduleSlotPickerProps,
  type RescheduleDialogProps,
} from '../components/scheduling/reschedule-dialog/index.js';
export {
  TimeOffPanel,
  type TimeOffEntry,
  type TimeOffPerson,
  type TimeOffDraft,
  type TimeOffPanelMessages,
  type TimeOffPanelProps,
} from '../components/scheduling/time-off-panel/index.js';
export {
  CalendarToolbar,
  type CalendarToolbarView,
  type CalendarToolbarMessages,
  type CalendarToolbarProps,
} from '../components/scheduling/calendar-toolbar/index.js';
export {
  EventPopover,
  type EventPopoverDetail,
  type EventPopoverMessages,
  type EventPopoverProps,
} from '../components/scheduling/event-popover/index.js';
export {
  ShiftRoster,
  type ShiftRosterShift,
  type ShiftRosterMember,
  type ShiftRosterValue,
  type ShiftRosterChange,
  type ShiftRosterMessages,
  type ShiftRosterProps,
} from '../components/scheduling/shift-roster/index.js';
export {
  validateBusinessHours,
  BusinessHoursEditor,
  type BusinessHoursException,
  type BusinessHoursValue,
  type BusinessHoursExceptionError,
  type BusinessHoursErrors,
  type ValidateBusinessHoursOptions,
  type BusinessHoursEditorMessages,
  type BusinessHoursEditorProps,
} from '../components/scheduling/business-hours-editor/index.js';
export {
  TeamCalendar,
  type TeamCalendarMember,
  type TeamCalendarPortion,
  type TeamCalendarEntry,
  type TeamCalendarHoliday,
  type TeamCalendarMessages,
  type TeamCalendarProps,
} from '../components/scheduling/team-calendar/index.js';
