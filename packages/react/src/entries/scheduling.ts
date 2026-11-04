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
