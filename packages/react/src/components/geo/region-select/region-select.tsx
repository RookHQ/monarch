'use client';

import { forwardRef, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  Combobox,
  matchComboboxOption,
  type ComboboxMessages,
  type ComboboxOption,
  type ComboboxProps,
} from '../../forms/combobox/index.js';
import { Input } from '../../forms/text-field/index.js';

/** One first-level subdivision of a country: a state, province, territory or nation. */
export interface RegionOption {
  /** Stable code to store. The built-in lists use ISO 3166-2 codes such as `US-CA`. */
  code: string;
  /** The name people read. */
  name: string;
}

/** A country's subdivisions and the country's own word for them. */
export interface RegionList {
  /** The country's word for its regions ("State", "Province or territory"), for a FieldLabel. English. */
  label: string;
  regions: RegionOption[];
}

const list = (prefix: string, label: string, entries: string): RegionList => ({
  label,
  regions: entries.split(';').map((entry) => {
    const [code = '', name = ''] = entry.split(':');
    return { code: `${prefix}-${code}`, name };
  }),
});

/**
 * The compact built-in subdivision lists, keyed by ISO 3166-1 alpha-2 code:
 * US (states and DC), CA, AU, GB (nations) and IN, with English names and
 * ISO 3166-2 codes. Every other country needs `regions` or `loadRegions`.
 */
export const BUILT_IN_REGIONS: Readonly<Record<string, RegionList>> = {
  US: list(
    'US',
    'State',
    'AL:Alabama;AK:Alaska;AZ:Arizona;AR:Arkansas;CA:California;CO:Colorado;CT:Connecticut;' +
      'DE:Delaware;DC:District of Columbia;FL:Florida;GA:Georgia;HI:Hawaii;ID:Idaho;IL:Illinois;' +
      'IN:Indiana;IA:Iowa;KS:Kansas;KY:Kentucky;LA:Louisiana;ME:Maine;MD:Maryland;' +
      'MA:Massachusetts;MI:Michigan;MN:Minnesota;MS:Mississippi;MO:Missouri;MT:Montana;' +
      'NE:Nebraska;NV:Nevada;NH:New Hampshire;NJ:New Jersey;NM:New Mexico;NY:New York;' +
      'NC:North Carolina;ND:North Dakota;OH:Ohio;OK:Oklahoma;OR:Oregon;PA:Pennsylvania;' +
      'RI:Rhode Island;SC:South Carolina;SD:South Dakota;TN:Tennessee;TX:Texas;UT:Utah;' +
      'VT:Vermont;VA:Virginia;WA:Washington;WV:West Virginia;WI:Wisconsin;WY:Wyoming',
  ),
  CA: list(
    'CA',
    'Province or territory',
    'AB:Alberta;BC:British Columbia;MB:Manitoba;NB:New Brunswick;NL:Newfoundland and Labrador;' +
      'NS:Nova Scotia;NT:Northwest Territories;NU:Nunavut;ON:Ontario;PE:Prince Edward Island;' +
      'QC:Quebec;SK:Saskatchewan;YT:Yukon',
  ),
  AU: list(
    'AU',
    'State or territory',
    'ACT:Australian Capital Territory;NSW:New South Wales;NT:Northern Territory;QLD:Queensland;' +
      'SA:South Australia;TAS:Tasmania;VIC:Victoria;WA:Western Australia',
  ),
  GB: list('GB', 'Nation', 'ENG:England;NIR:Northern Ireland;SCT:Scotland;WLS:Wales'),
  IN: list(
    'IN',
    'State or union territory',
    'AN:Andaman and Nicobar Islands;AP:Andhra Pradesh;AR:Arunachal Pradesh;AS:Assam;BR:Bihar;' +
      'CH:Chandigarh;CG:Chhattisgarh;DH:Dadra and Nagar Haveli and Daman and Diu;DL:Delhi;GA:Goa;' +
      'GJ:Gujarat;HR:Haryana;HP:Himachal Pradesh;JK:Jammu and Kashmir;JH:Jharkhand;KA:Karnataka;' +
      'KL:Kerala;LA:Ladakh;LD:Lakshadweep;MP:Madhya Pradesh;MH:Maharashtra;MN:Manipur;' +
      'ML:Meghalaya;MZ:Mizoram;NL:Nagaland;OD:Odisha;PY:Puducherry;PB:Punjab;RJ:Rajasthan;' +
      'SK:Sikkim;TN:Tamil Nadu;TS:Telangana;TR:Tripura;UP:Uttar Pradesh;UK:Uttarakhand;' +
      'WB:West Bengal',
  ),
};

/** The built-in subdivision list for a country code (any case), or `null` when Monarch ships none. */
export function getBuiltInRegions(country: string): RegionList | null {
  return BUILT_IN_REGIONS[country.toUpperCase()] ?? null;
}

/** Interface text, for translation. Every message has an English default. */
export interface RegionSelectMessages {
  /** Placeholder while no country is chosen. Default "Choose a country first". */
  chooseCountryFirst: string;
  /** Placeholder of the text field used when the country has no list. Default "Type the region". */
  textEntry: string;
  /** Shown when nothing matches. Default "No regions match". */
  empty: string;
}

const DEFAULT_MESSAGES: RegionSelectMessages = {
  chooseCountryFirst: 'Choose a country first',
  textEntry: 'Type the region',
  empty: 'No regions match',
};

export interface RegionSelectProps extends Omit<
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
  | 'inline'
  | 'inputValue'
  | 'onInputValueChange'
  | 'messages'
> {
  /** ISO 3166-1 alpha-2 code of the country whose regions are listed. `null` disables the field. */
  country: string | null | undefined;
  /**
   * Selected region (controlled): a code from the list, or the typed text when
   * the country has no list.
   */
  value?: string | null | undefined;
  /** Initially selected region (uncontrolled). */
  defaultValue?: string | null | undefined;
  /** Called with the chosen region code (or typed text), or `null` when cleared. */
  onValueChange?: ((region: string | null) => void) | undefined;
  /** The country's regions, from the product's own data. Wins over `loadRegions` and the built-in lists. */
  regions?: readonly RegionOption[] | undefined;
  /**
   * Loads a country's regions from the product's API. Called once per country
   * (results are cached for the field's life); the signal aborts when the
   * field unmounts. Resolve to `[]` for a text-entry field. Wins over the
   * built-in lists, so call `getBuiltInRegions` inside it to keep them.
   */
  loadRegions?:
    ((country: string, signal: AbortSignal) => Promise<readonly RegionOption[]>) | undefined;
  /** When the country has no list, show a text field instead of an empty select. Default true. */
  allowTextEntry?: boolean | undefined;
  /** Show the region code at the end of each option. Default false. */
  showCode?: boolean | undefined;
  /** Interface text, for translation. Combobox messages can be passed here too. */
  messages?: (Partial<RegionSelectMessages> & Partial<ComboboxMessages>) | undefined;
}

type Loaded = { country: string; regions: readonly RegionOption[] };

/**
 * Picks a first-level subdivision (state, province, territory) of a country.
 * Regions come from the `regions` prop, an async `loadRegions(country)`
 * loader, or the compact built-in lists (US, CA, AU, GB, IN); a country with
 * no list gets a plain text field. Built on Combobox; forwards its ref to the
 * input.
 */
export const RegionSelect = forwardRef<HTMLInputElement, RegionSelectProps>(function RegionSelect(
  {
    country: countryProp,
    value: valueProp,
    defaultValue = null,
    onValueChange,
    regions,
    loadRegions,
    allowTextEntry = true,
    showCode = false,
    messages: messagesProp,
    emptyMessage,
    placeholder,
    className,
    // Combobox-only props, kept off the text-entry input.
    sortByMatch,
    searchDebounce,
    minSearchLength,
    clearOption,
    renderOption,
    limit,
    loading,
    clearable,
    closeOnSelect,
    open,
    defaultOpen,
    onOpenChange,
    listLabel,
    ...inputProps
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const country = countryProp ? countryProp.toUpperCase() : null;
  const [internalValue, setInternalValue] = useState<string | null>(defaultValue);
  const value = valueProp !== undefined ? valueProp : internalValue;
  const commit = (next: string | null) => {
    if (valueProp === undefined) setInternalValue(next);
    onValueChange?.(next);
  };

  // ----- Async regions: one request per country, cached, aborted on unmount -----
  const loaderRef = useRef(loadRegions);
  loaderRef.current = loadRegions;
  const cache = useRef(new Map<string, Promise<readonly RegionOption[]>>());
  const controllers = useRef<AbortController[]>([]);
  useEffect(() => {
    const list = controllers.current;
    return () => list.forEach((c) => c.abort());
  }, []);
  const load = useCallback((code: string) => {
    let promise = cache.current.get(code);
    if (!promise) {
      const loader = loaderRef.current;
      const ctl = new AbortController();
      controllers.current.push(ctl);
      promise = loader ? Promise.resolve(loader(code, ctl.signal)) : Promise.resolve([]);
      // A failed load is forgotten, so "Try again" asks again.
      promise.catch(() => cache.current.delete(code));
      cache.current.set(code, promise);
    }
    return promise;
  }, []);

  const usesLoader = !regions && !!loadRegions;
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  useEffect(() => {
    if (!usesLoader || !country) return;
    let live = true;
    load(country).then(
      (list) => live && setLoaded({ country, regions: list }),
      () => undefined,
    );
    return () => {
      live = false;
    };
  }, [usesLoader, country, load]);

  const staticRegions = useMemo((): readonly RegionOption[] | null => {
    if (regions) return regions;
    if (usesLoader) return loaded && loaded.country === country ? loaded.regions : null;
    return country ? (getBuiltInRegions(country)?.regions ?? []) : [];
  }, [regions, usesLoader, loaded, country]);

  const toOption = useMemo(
    () =>
      (r: RegionOption): ComboboxOption => ({
        value: r.code,
        label: r.name,
        keywords: r.code,
        ...(showCode ? { hint: r.code.replace(/^[A-Z]{2}-/, '') } : {}),
      }),
    [showCode],
  );
  const options = useMemo(() => (staticRegions ?? []).map(toOption), [staticRegions, toOption]);

  const comboProps = {
    sortByMatch: sortByMatch ?? true,
    searchDebounce,
    minSearchLength,
    clearOption,
    renderOption,
    limit,
    loading,
    clearable,
    closeOnSelect,
    open,
    defaultOpen,
    onOpenChange,
    listLabel,
  };

  if (!country) {
    return (
      <Combobox
        ref={ref}
        {...inputProps}
        {...comboProps}
        className={cx('mn-region-select', className)}
        data-source="none"
        options={[]}
        value={null}
        disabled
        placeholder={messages.chooseCountryFirst}
        messages={messages}
      />
    );
  }

  if (staticRegions && staticRegions.length === 0 && allowTextEntry) {
    const { size = 'md', invalid, ...textProps } = inputProps;
    return (
      <Input
        ref={ref}
        {...textProps}
        size={size}
        {...(invalid !== undefined ? { invalid } : {})}
        className={cx('mn-region-select', className)}
        data-source="text-entry"
        value={value ?? ''}
        placeholder={placeholder ?? messages.textEntry}
        autoComplete={inputProps.autoComplete ?? 'address-level1'}
        onChange={(e) => commit(e.target.value === '' ? null : e.target.value)}
      />
    );
  }

  const waiting = staticRegions === null;
  const selectedOption = value ? options.find((o) => o.value === value) : undefined;
  return (
    <Combobox
      // A new country starts a fresh list (and a fresh search state).
      key={country}
      ref={ref}
      autoComplete="address-level1"
      {...inputProps}
      {...comboProps}
      className={cx('mn-region-select', className)}
      data-source={waiting ? 'loading' : 'list'}
      options={options}
      value={value}
      valueOptions={selectedOption ? [selectedOption] : undefined}
      placeholder={placeholder}
      emptyMessage={emptyMessage ?? messages.empty}
      messages={messages}
      onValueChange={commit}
      {...(waiting
        ? {
            // Until the first load lands, the list asks the cached loader (with
            // Combobox's loading, error and "Try again" states).
            searchDebounce: 0,
            onSearch: (query: string) =>
              load(country).then((list) => {
                // A retry after a failed first load lands here: keep the list, so the
                // field leaves its loading mode and the chosen region shows its name.
                setLoaded({ country, regions: list });
                return list.map(toOption).filter((o) => matchComboboxOption(o, query));
              }),
          }
        : {})}
    />
  );
});
