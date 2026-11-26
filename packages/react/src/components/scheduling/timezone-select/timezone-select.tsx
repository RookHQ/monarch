'use client';

import { forwardRef, useEffect, useMemo, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  Combobox,
  type ComboboxMessages,
  type ComboboxOption,
  type ComboboxProps,
} from '../../forms/combobox/index.js';

/** What TimezoneSelect knows about one IANA zone at a given instant. */
export interface TimezoneInfo {
  /** IANA id, e.g. `Asia/Kolkata`. */
  id: string;
  /** Last segment of the id, readable: `Kolkata`, `New York`. */
  city: string;
  /** First segment of the id: `Asia`, `America`. Empty for `UTC`. */
  region: string;
  /** Offset from UTC in minutes at `now` (DST-aware), e.g. 330. */
  offsetMinutes: number;
  /** `UTC+05:30`. */
  offsetLabel: string;
  /** Localised long name, e.g. "India Standard Time". Empty when the runtime has none. */
  longName: string;
}

/** Interface text, for translation. */
export interface TimezoneSelectMessages {
  detected: string;
  pinned: string;
  recent: string;
  all: string;
  empty: string;
}

const DEFAULT_MESSAGES: TimezoneSelectMessages = {
  detected: 'Detected',
  pinned: 'Pinned',
  recent: 'Recent',
  all: 'All time zones',
  empty: 'No time zones match',
};

/** Used when the runtime has no `Intl.supportedValuesOf` (older browsers and Node). */
const FALLBACK_ZONES = [
  'UTC',
  'Pacific/Midway',
  'Pacific/Honolulu',
  'America/Anchorage',
  'America/Los_Angeles',
  'America/Vancouver',
  'America/Tijuana',
  'America/Denver',
  'America/Phoenix',
  'America/Chicago',
  'America/Mexico_City',
  'America/New_York',
  'America/Toronto',
  'America/Bogota',
  'America/Lima',
  'America/Caracas',
  'America/Halifax',
  'America/Santiago',
  'America/Sao_Paulo',
  'America/Argentina/Buenos_Aires',
  'America/St_Johns',
  'Atlantic/Azores',
  'Atlantic/Reykjavik',
  'Europe/London',
  'Europe/Dublin',
  'Europe/Lisbon',
  'Africa/Lagos',
  'Africa/Casablanca',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Madrid',
  'Europe/Rome',
  'Europe/Amsterdam',
  'Europe/Stockholm',
  'Europe/Warsaw',
  'Africa/Cairo',
  'Africa/Johannesburg',
  'Europe/Athens',
  'Europe/Helsinki',
  'Europe/Kyiv',
  'Asia/Jerusalem',
  'Europe/Istanbul',
  'Europe/Moscow',
  'Africa/Nairobi',
  'Asia/Riyadh',
  'Asia/Tehran',
  'Asia/Dubai',
  'Asia/Kabul',
  'Asia/Karachi',
  'Asia/Tashkent',
  'Asia/Kolkata',
  'Asia/Colombo',
  'Asia/Kathmandu',
  'Asia/Dhaka',
  'Asia/Yangon',
  'Asia/Bangkok',
  'Asia/Jakarta',
  'Asia/Ho_Chi_Minh',
  'Asia/Shanghai',
  'Asia/Hong_Kong',
  'Asia/Singapore',
  'Asia/Kuala_Lumpur',
  'Asia/Manila',
  'Asia/Taipei',
  'Australia/Perth',
  'Asia/Seoul',
  'Asia/Tokyo',
  'Australia/Adelaide',
  'Australia/Darwin',
  'Australia/Brisbane',
  'Australia/Sydney',
  'Australia/Melbourne',
  'Pacific/Noumea',
  'Pacific/Auckland',
  'Pacific/Fiji',
  'Pacific/Tongatapu',
  'Pacific/Kiritimati',
];

/** Country names, old names and common abbreviations, so people find zones the way they think of them. */
const ALIASES: Record<string, string> = {
  UTC: 'gmt zulu universal coordinated',
  'Asia/Kolkata': 'india ist calcutta mumbai delhi bengaluru bangalore chennai hyderabad',
  'Asia/Dubai': 'uae emirates gulf abu dhabi',
  'Asia/Riyadh': 'saudi arabia',
  'Asia/Karachi': 'pakistan',
  'Asia/Dhaka': 'bangladesh',
  'Asia/Kathmandu': 'nepal katmandu',
  'Asia/Colombo': 'sri lanka',
  'Asia/Singapore': 'sgt',
  'Asia/Tokyo': 'japan jst',
  'Asia/Seoul': 'korea kst',
  'Asia/Shanghai': 'china beijing cst',
  'Asia/Hong_Kong': 'hkt',
  'Asia/Jakarta': 'indonesia wib',
  'Asia/Manila': 'philippines',
  'Asia/Ho_Chi_Minh': 'vietnam saigon',
  'Asia/Bangkok': 'thailand',
  'Asia/Jerusalem': 'israel tel aviv',
  'Europe/London': 'uk united kingdom britain england gmt bst',
  'Europe/Dublin': 'ireland',
  'Europe/Paris': 'france cet',
  'Europe/Berlin': 'germany cet',
  'Europe/Madrid': 'spain',
  'Europe/Rome': 'italy',
  'Europe/Amsterdam': 'netherlands holland',
  'Europe/Kyiv': 'ukraine kiev',
  'Europe/Istanbul': 'turkey türkiye',
  'Europe/Moscow': 'russia msk',
  'Africa/Lagos': 'nigeria wat',
  'Africa/Nairobi': 'kenya eat',
  'Africa/Johannesburg': 'south africa sast',
  'Africa/Cairo': 'egypt',
  'America/New_York': 'us usa united states eastern est edt',
  'America/Chicago': 'us usa united states central cst cdt',
  'America/Denver': 'us usa united states mountain mst mdt',
  'America/Phoenix': 'us usa arizona mst',
  'America/Los_Angeles': 'us usa united states pacific pst pdt california san francisco',
  'America/Anchorage': 'alaska',
  'Pacific/Honolulu': 'hawaii hst',
  'America/Toronto': 'canada eastern',
  'America/Vancouver': 'canada pacific',
  'America/Mexico_City': 'mexico',
  'America/Sao_Paulo': 'brazil brasil',
  'America/Argentina/Buenos_Aires': 'argentina',
  'America/Bogota': 'colombia',
  'Australia/Sydney': 'australia aest aedt',
  'Australia/Melbourne': 'australia aest aedt',
  'Australia/Perth': 'australia awst',
  'Pacific/Auckland': 'new zealand nzst nzdt',
};

/**
 * Old ids that browsers still report (Chrome and Node list `Asia/Calcutta`,
 * not `Asia/Kolkata`), mapped to the current IANA names.
 */
const LEGACY_ZONES: Record<string, string> = {
  'Africa/Asmera': 'Africa/Asmara',
  'America/Buenos_Aires': 'America/Argentina/Buenos_Aires',
  'America/Catamarca': 'America/Argentina/Catamarca',
  'America/Cordoba': 'America/Argentina/Cordoba',
  'America/Jujuy': 'America/Argentina/Jujuy',
  'America/Mendoza': 'America/Argentina/Mendoza',
  'America/Coral_Harbour': 'America/Atikokan',
  'America/Godthab': 'America/Nuuk',
  'America/Indianapolis': 'America/Indiana/Indianapolis',
  'America/Louisville': 'America/Kentucky/Louisville',
  'Asia/Calcutta': 'Asia/Kolkata',
  'Asia/Katmandu': 'Asia/Kathmandu',
  'Asia/Rangoon': 'Asia/Yangon',
  'Asia/Saigon': 'Asia/Ho_Chi_Minh',
  'Asia/Dacca': 'Asia/Dhaka',
  'Asia/Thimbu': 'Asia/Thimphu',
  'Asia/Ulan_Bator': 'Asia/Ulaanbaatar',
  'Asia/Macao': 'Asia/Macau',
  'Atlantic/Faeroe': 'Atlantic/Faroe',
  'Europe/Kiev': 'Europe/Kyiv',
  'Pacific/Enderbury': 'Pacific/Kanton',
  'Pacific/Ponape': 'Pacific/Pohnpei',
  'Pacific/Truk': 'Pacific/Chuuk',
  'Etc/UTC': 'UTC',
  'Etc/GMT': 'UTC',
  'Etc/Universal': 'UTC',
  'Etc/Zulu': 'UTC',
  GMT: 'UTC',
  UCT: 'UTC',
  Universal: 'UTC',
  Zulu: 'UTC',
};
const CURRENT_TO_LEGACY: Record<string, string> = Object.fromEntries(
  Object.entries(LEGACY_ZONES).map(([legacy, current]) => [current, legacy]),
);

/** The current IANA name of a zone id: `Asia/Calcutta` → `Asia/Kolkata`, `Etc/UTC` → `UTC`. */
export function canonicalTimezone(zone: string): string {
  return LEGACY_ZONES[zone] ?? zone;
}

/**
 * Every IANA zone the runtime knows, by current name, with `UTC` first; or a
 * static list of about 80 common zones when `Intl.supportedValuesOf` is missing.
 */
export function listTimezones(): string[] {
  try {
    const supported = (Intl as { supportedValuesOf?: (key: string) => string[] }).supportedValuesOf;
    if (typeof supported === 'function') {
      const ids = new Set(['UTC']);
      for (const z of supported('timeZone')) ids.add(canonicalTimezone(z));
      return [...ids];
    }
  } catch {
    /* fall through to the static list */
  }
  return FALLBACK_ZONES;
}

/** `UTC+05:30`, `UTC-08:00`, `UTC+00:00`. */
export function formatUtcOffset(minutes: number): string {
  const sign = minutes < 0 ? '-' : '+';
  const abs = Math.abs(minutes);
  const h = String(Math.floor(abs / 60)).padStart(2, '0');
  const m = String(abs % 60).padStart(2, '0');
  return `UTC${sign}${h}:${m}`;
}

const partsFormatters = new Map<string, Intl.DateTimeFormat | null>();
const nameFormatters = new Map<string, Intl.DateTimeFormat | null>();

function cached(
  map: Map<string, Intl.DateTimeFormat | null>,
  key: string,
  make: () => Intl.DateTimeFormat,
): Intl.DateTimeFormat | null {
  if (!map.has(key)) {
    try {
      map.set(key, make());
    } catch {
      map.set(key, null); // unknown zone
    }
  }
  return map.get(key) ?? null;
}

/** Offset in minutes of `zone` at `at`, from the wall-clock time Intl reports there. */
function offsetAt(zone: string, at: number): number | null {
  const own = offsetIn(zone, at);
  // Older ICU builds know only the legacy id (Europe/Kiev, not Europe/Kyiv).
  const legacy = CURRENT_TO_LEGACY[zone];
  return own ?? (legacy ? offsetIn(legacy, at) : null);
}

function offsetIn(zone: string, at: number): number | null {
  const fmt = cached(
    partsFormatters,
    zone,
    () =>
      new Intl.DateTimeFormat('en-US', {
        timeZone: zone,
        hourCycle: 'h23',
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
      }),
  );
  if (!fmt) return null;
  const p: Record<string, number> = {};
  for (const part of fmt.formatToParts(at)) p[part.type] = Number(part.value);
  const wall = Date.UTC(
    p.year ?? 1970,
    (p.month ?? 1) - 1,
    p.day ?? 1,
    (p.hour ?? 0) % 24,
    p.minute ?? 0,
  );
  const utcMinute = Math.floor(at / 60_000) * 60_000;
  return Math.round((wall - utcMinute) / 60_000);
}

/**
 * Offset, readable city and long name of one zone at `now` (default: the
 * current time). Pure apart from `Intl`; safe on the server.
 */
export function getTimezoneInfo(
  zone: string,
  { now, locale = 'en-US' }: { now?: Date | number; locale?: string } = {},
): TimezoneInfo {
  const at = now === undefined ? Date.now() : +now;
  const segments = zone.split('/');
  const city = (segments[segments.length - 1] ?? zone).replace(/_/g, ' ');
  const region = segments.length > 1 ? (segments[0] ?? '') : '';
  const offsetMinutes = offsetAt(zone, at) ?? 0;
  let longName = '';
  const nameFor = (z: string) =>
    cached(
      nameFormatters,
      `${locale}|${z}`,
      () => new Intl.DateTimeFormat(locale, { timeZone: z, timeZoneName: 'long' }),
    );
  const legacy = CURRENT_TO_LEGACY[zone];
  const named = nameFor(zone) ?? (legacy ? nameFor(legacy) : null);
  if (named) longName = named.formatToParts(at).find((x) => x.type === 'timeZoneName')?.value ?? '';
  if (/^GMT[+-]/.test(longName)) longName = '';
  return {
    id: zone,
    city: zone === 'UTC' ? 'UTC' : city,
    region,
    offsetMinutes,
    offsetLabel: formatUtcOffset(offsetMinutes),
    longName,
  };
}

/** The zone this device is set to, or null when the runtime cannot tell. */
function detectZone(): string | null {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || null;
  } catch {
    return null;
  }
}

export interface TimezoneSelectProps extends Omit<
  ComboboxProps,
  | 'options'
  | 'value'
  | 'defaultValue'
  | 'onValueChange'
  | 'multiple'
  | 'values'
  | 'defaultValues'
  | 'onValuesChange'
  | 'valueOptions'
  | 'onSearch'
  | 'onCreate'
  | 'filter'
  | 'sortByMatch'
  | 'inline'
  | 'messages'
> {
  /**
   * Selected IANA zone id (controlled). Legacy ids select their current name
   * (`Asia/Calcutta` → `Asia/Kolkata`); a zone the list does not contain is still shown.
   */
  value?: string | null | undefined;
  defaultValue?: string | null | undefined;
  onValueChange?: ((zone: string | null) => void) | undefined;
  /** The instant offsets are computed for. Default: the time of first render. Pass it to render the same on server and client. */
  now?: Date | number | undefined;
  /** Locale for long zone names ("India Standard Time"). Default `en-US`. */
  locale?: string | undefined;
  /** Limit the list to these zones. Default: every zone the runtime knows, or a static list. */
  zones?: string[] | undefined;
  /**
   * The zone shown under "Detected". Default: the device's zone, read after
   * mount so server and client render the same. `null` hides the section.
   */
  detectedZone?: string | null | undefined;
  /** Zones listed first under "Pinned", such as the workspace's offices. */
  pinned?: string[] | undefined;
  /** Zones listed under "Recent", such as the person's last choices. */
  recent?: string[] | undefined;
  /** Option label. Default `(UTC+05:30) Kolkata`. */
  formatLabel?: ((zone: TimezoneInfo) => string) | undefined;
  /** Interface text, for translation. */
  messages?: (Partial<TimezoneSelectMessages> & Partial<ComboboxMessages>) | undefined;
}

const defaultLabel = (z: TimezoneInfo) => `(${z.offsetLabel}) ${z.city}`;

/** Every word typed must appear in the zone's id, city, offset, long name or aliases. */
function matches(option: ComboboxOption, query: string): boolean {
  const haystack =
    `${option.label} ${option.description ?? ''} ${option.keywords ?? ''}`.toLowerCase();
  return query
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .every((term) => haystack.includes(term));
}

/**
 * Searchable IANA time zone picker, labelled by UTC offset and city and
 * sorted by offset, with "Detected", "Pinned" and "Recent" sections. Search
 * matches city, zone id, offset (`+5:30`, `utc-8`), long name and common
 * country names. Offsets are DST-aware for `now`. Built on Combobox.
 */
export const TimezoneSelect = forwardRef<HTMLInputElement, TimezoneSelectProps>(
  function TimezoneSelect(
    {
      value: valueProp,
      defaultValue = null,
      onValueChange,
      now,
      locale = 'en-US',
      zones,
      detectedZone,
      pinned,
      recent,
      formatLabel = defaultLabel,
      messages: messagesProp,
      onInputValueChange,
      onOpenChange,
      onFocus,
      emptyMessage,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [internalValue, setInternalValue] = useState<string | null>(defaultValue);
    const raw = valueProp !== undefined ? valueProp : internalValue;
    // Legacy ids select their current entry: Asia/Calcutta shows as Kolkata.
    const value = raw ? canonicalTimezone(raw) : raw;
    const [firstRender] = useState(() => Date.now());
    const at = now === undefined ? firstRender : +now;

    const [deviceZone, setDeviceZone] = useState<string | null>(null);
    useEffect(() => {
      if (detectedZone === undefined) setDeviceZone(detectZone());
    }, [detectedZone]);
    const detectedRaw = detectedZone === undefined ? deviceZone : detectedZone;
    const detected = detectedRaw ? canonicalTimezone(detectedRaw) : null;

    // Building ~420 options is deferred until the list is first wanted.
    const [wanted, setWanted] = useState(false);
    const [text, setText] = useState('');

    const toOption = useMemo(() => {
      return (zone: string, group?: string): ComboboxOption => {
        const info = getTimezoneInfo(zone, { now: at, locale });
        const offsetShort = info.offsetLabel.replace(/^UTC/, '').replace(/^([+-])0/, '$1');
        return {
          value: zone,
          label: formatLabel(info),
          description: info.longName ? `${zone} · ${info.longName}` : zone,
          keywords: [
            zone.replace(/_/g, ' '),
            info.offsetLabel,
            `gmt${offsetShort}`,
            `utc${offsetShort}`,
            offsetShort,
            offsetShort.replace(':', ''),
            ALIASES[zone] ?? '',
          ].join(' '),
          ...(group ? { group } : {}),
        };
      };
    }, [at, locale, formatLabel]);

    const all = useMemo(() => {
      if (!wanted) return [];
      const ids = zones ? [...new Set(zones.map(canonicalTimezone))] : listTimezones();
      return ids
        .map((z) => ({ z, off: getTimezoneInfo(z, { now: at }).offsetMinutes }))
        .sort((a, b) => a.off - b.off || a.z.localeCompare(b.z))
        .map((x) => x.z);
    }, [wanted, zones, at]);

    const selectedOption = value ? toOption(value) : null;
    const searching = text.trim() !== '' && text !== (selectedOption?.label ?? '');

    const options = useMemo(() => {
      if (!wanted) return selectedOption ? [selectedOption] : [];
      const extra = value && !all.includes(value) ? [value] : [];
      if (searching) return [...extra, ...all].map((z) => toOption(z));
      const out: ComboboxOption[] = [];
      const seen = new Set<string>();
      const section = (list: (string | null | undefined)[], group: string) => {
        for (const z of list) {
          if (!z || seen.has(z)) continue;
          seen.add(z);
          out.push(toOption(z, group));
        }
      };
      section([detected], messages.detected);
      section(pinned ?? [], messages.pinned);
      section(recent ?? [], messages.recent);
      // The full list keeps every zone, so a zone also appears in its offset order.
      const grouped = out.length > 0;
      for (const z of [...extra, ...all]) out.push(toOption(z, grouped ? messages.all : undefined));
      return out;
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [wanted, all, searching, value, detected, pinned, recent, toOption, messages.detected]);

    return (
      <Combobox
        ref={ref}
        {...rest}
        className={cx('mn-timezone-select', className)}
        options={options}
        value={value}
        valueOptions={selectedOption ? [selectedOption] : undefined}
        filter={matches}
        emptyMessage={emptyMessage ?? messages.empty}
        messages={messages}
        onValueChange={(next) => {
          if (valueProp === undefined) setInternalValue(next);
          onValueChange?.(next);
        }}
        onInputValueChange={(t) => {
          setText(t);
          if (!wanted) setWanted(true);
          onInputValueChange?.(t);
        }}
        onOpenChange={(open) => {
          if (open && !wanted) setWanted(true);
          onOpenChange?.(open);
        }}
        onFocus={(e) => {
          if (!wanted) setWanted(true);
          onFocus?.(e);
        }}
      />
    );
  },
);
