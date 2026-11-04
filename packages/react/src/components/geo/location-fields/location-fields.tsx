'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FieldsetHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Combobox, type ComboboxOption } from '../../forms/combobox/index.js';
import { Field, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';

/** One choice from the data source: a stable code and the text people read. */
export interface LocationOption {
  /** Stable code to store, such as an ISO 3166 code or the source's place id. */
  value: string;
  label: string;
  /** Secondary text in the list, e.g. the region a city is in. */
  description?: string;
}

export interface LocationCityOption extends LocationOption {
  /** The region this city belongs to; filled in when the user picked a city before a region. */
  region?: { value: string; label: string };
}

export interface LocationRegions {
  options: LocationOption[];
  /** The country's own word for its regions ("State", "Province", "Emirate"). */
  label?: string;
}

export interface LocationCityQuery {
  country: string;
  /** The chosen region's code, or null to search the whole country. */
  region: string | null;
  /** What the user typed, trimmed. Empty when listing a region's cities. */
  query: string;
}

/**
 * Where the lists come from. Monarch ships no geographic data and makes no
 * network calls: the product implements this against its own API. Every
 * method receives an AbortSignal that fires when the request is superseded.
 */
export interface LocationAdapter {
  countries: (signal: AbortSignal) => Promise<LocationOption[]>;
  /** Omit, or resolve to no options, for a free-text region field. */
  regions?: (country: string, signal: AbortSignal) => Promise<LocationRegions>;
  /** Omit for a free-text city field. Called as the user types (debounced). */
  cities?: (query: LocationCityQuery, signal: AbortSignal) => Promise<LocationCityOption[]>;
}

export interface LocationValue {
  /** Country code, or null. */
  country: string | null;
  /** Region code from the list, or null when typed freely or not chosen. */
  region: string | null;
  /** Region text: the chosen option's label or what the user typed. */
  regionName: string;
  /** City text: the chosen option's label or what the user typed. */
  city: string;
  /** City code from the list, or null when typed freely. */
  cityId: string | null;
  postalCode: string;
}

export type LocationField = 'country' | 'region' | 'city' | 'postalCode';

export interface LocationFieldsMessages {
  loadFailed: (what: string) => string;
  retry: string;
  chooseCountryFirst: string;
  /** Shown in the city list until enough letters are typed. */
  typeMore: (min: number) => string;
  noMatch: string;
  /** Description of the option that keeps the typed city as is. */
  useTyped: string;
  /** Placeholder of a region typed freely (the country has no list). */
  optional: string;
}

const defaultMessages: LocationFieldsMessages = {
  loadFailed: (what) => `Couldn’t load ${what}.`,
  retry: 'Try again',
  chooseCountryFirst: 'Choose a country first',
  typeMore: (min) => `Type at least ${min} letters to search.`,
  noMatch: 'No matches',
  useTyped: 'Not in the list: use as typed',
  optional: 'Optional',
};

const defaultLabels: Record<LocationField, string> = {
  country: 'Country',
  region: 'State / Region',
  city: 'City',
  postalCode: 'Postal code',
};

const EMPTY: LocationValue = {
  country: null,
  region: null,
  regionName: '',
  city: '',
  cityId: null,
  postalCode: '',
};

const TYPED = '\u0000typed:';
const ALL_FIELDS: LocationField[] = ['country', 'region', 'city', 'postalCode'];

export interface LocationFieldsProps extends Omit<
  FieldsetHTMLAttributes<HTMLFieldSetElement>,
  'defaultValue' | 'onChange'
> {
  /** The data source. Required. */
  adapter: LocationAdapter;
  /** The address (controlled). */
  value?: LocationValue;
  /** Initial address when uncontrolled. */
  defaultValue?: Partial<LocationValue>;
  /** Called with the whole new value and the field that changed. Changing the country clears the rest. */
  onValueChange?: (value: LocationValue, changed: LocationField) => void;
  /** Which fields to show, in order. Default all four. */
  fields?: LocationField[];
  /** Override field labels. The region label is replaced by the adapter's word for the country, when given. */
  labels?: Partial<Record<LocationField, string>>;
  /** Fields that must be filled; they get `required` and the label asterisk. */
  required?: LocationField[];
  /** Error messages per field; a field with a message is marked invalid. */
  errors?: Partial<Record<LocationField, ReactNode>>;
  disabled?: boolean;
  /** Visible group label. Without it, give the fieldset an `aria-label` if the address needs a name. */
  legend?: ReactNode;
  /** Letters needed before cities are searched when no region is chosen. Default 2. */
  cityMinQuery?: number;
  /** Milliseconds without typing before cities are searched. Default 250. */
  cityDebounce?: number;
  /** Offer the typed text as a city when it is not in the list. Default true. */
  allowCustomCity?: boolean;
  messages?: Partial<LocationFieldsMessages>;
  /** Prefix for the field ids, so labels stay stable and two address forms on a page never clash. */
  idPrefix?: string;
}

type Load<T> =
  { state: 'idle' } | { state: 'loading' } | { state: 'failed' } | { state: 'ready'; data: T };

function LoadFailed({
  text,
  retry,
  onRetry,
}: {
  text: string;
  retry: string;
  onRetry: () => void;
}) {
  return (
    <div role="alert" className="mn-location-fields__failed">
      <span>{text}</span>
      <Button size="xs" variant="secondary" onClick={onRetry}>
        {retry}
      </Button>
    </div>
  );
}

/**
 * Country, region, city and postal code as one composite. Lists come from an
 * injected adapter, so the product owns the data and the requests. Picking a
 * country loads its regions under the country's own word; cities are searched
 * as the user types, can be typed freely, and fill in their region.
 */
export const LocationFields = forwardRef<HTMLFieldSetElement, LocationFieldsProps>(
  function LocationFields(
    {
      adapter,
      value: valueProp,
      defaultValue,
      onValueChange,
      fields = ALL_FIELDS,
      labels: labelsProp,
      required = [],
      errors = {},
      disabled = false,
      legend,
      cityMinQuery = 2,
      cityDebounce = 250,
      allowCustomCity = true,
      messages: messagesProp,
      idPrefix,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...defaultMessages, ...messagesProp };
    const labels = { ...defaultLabels, ...labelsProp };
    const [internal, setInternal] = useState<LocationValue>({ ...EMPTY, ...defaultValue });
    const value = valueProp ?? internal;
    const adapterRef = useRef(adapter);
    adapterRef.current = adapter;

    const update = (patch: Partial<LocationValue>, changed: LocationField) => {
      const next = { ...value, ...patch };
      if (valueProp === undefined) setInternal(next);
      onValueChange?.(next, changed);
    };

    // Countries: once on mount, and again on Retry.
    const [countries, setCountries] = useState<Load<LocationOption[]>>({ state: 'loading' });
    const [countriesTry, setCountriesTry] = useState(0);
    useEffect(() => {
      const ctl = new AbortController();
      setCountries({ state: 'loading' });
      adapterRef.current
        .countries(ctl.signal)
        .then((data) => !ctl.signal.aborted && setCountries({ state: 'ready', data }))
        .catch(() => !ctl.signal.aborted && setCountries({ state: 'failed' }));
      return () => ctl.abort();
    }, [countriesTry]);

    // Regions: whenever the country changes.
    const [regions, setRegions] = useState<Load<LocationRegions>>({ state: 'idle' });
    const [regionsTry, setRegionsTry] = useState(0);
    const hasRegionSource = !!adapter.regions;
    useEffect(() => {
      const load = adapterRef.current.regions;
      if (!value.country || !load) {
        setRegions({ state: 'idle' });
        return;
      }
      const ctl = new AbortController();
      setRegions({ state: 'loading' });
      load(value.country, ctl.signal)
        .then((data) => !ctl.signal.aborted && setRegions({ state: 'ready', data }))
        .catch(() => !ctl.signal.aborted && setRegions({ state: 'failed' }));
      return () => ctl.abort();
    }, [value.country, regionsTry, hasRegionSource]);

    // Cities: searched as the user types; a chosen region is listed right away.
    const [cityText, setCityText] = useState(value.city);
    const [cities, setCities] = useState<LocationCityOption[]>([]);
    const [citiesState, setCitiesState] = useState<'idle' | 'loading' | 'failed' | 'short'>('idle');
    const cityAbort = useRef<AbortController | null>(null);
    const cityTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
    const skipQuery = useRef<string | null>(null);

    useEffect(() => setCityText(value.city), [value.city]);

    const searchCities = useCallback(
      (text: string, wait: number) => {
        if (cityTimer.current !== null) clearTimeout(cityTimer.current);
        cityAbort.current?.abort();
        const load = adapterRef.current.cities;
        const query = text.trim();
        if (!load || !value.country) {
          setCities([]);
          setCitiesState('idle');
          return;
        }
        if (!value.region && query.length < cityMinQuery) {
          setCities([]);
          setCitiesState('short');
          return;
        }
        const country = value.country;
        const region = value.region;
        cityTimer.current = setTimeout(() => {
          const ctl = new AbortController();
          cityAbort.current = ctl;
          setCitiesState('loading');
          load({ country, region, query }, ctl.signal)
            .then((data) => {
              if (ctl.signal.aborted) return;
              setCities(data);
              setCitiesState('idle');
            })
            .catch(() => {
              if (ctl.signal.aborted) return;
              setCities([]);
              setCitiesState('failed');
            });
        }, wait);
      },
      [value.country, value.region, cityMinQuery],
    );

    // A new country or region invalidates the city list; list a chosen region's cities at once.
    useEffect(() => {
      searchCities('', 0);
      return () => {
        if (cityTimer.current !== null) clearTimeout(cityTimer.current);
        cityAbort.current?.abort();
      };
    }, [searchCities]);

    const countryOptions = useMemo<ComboboxOption[]>(() => {
      if (countries.state === 'ready') return countries.data;
      return value.country ? [{ value: value.country, label: value.country }] : [];
    }, [countries, value.country]);

    const regionList = useMemo(
      () => (regions.state === 'ready' ? regions.data.options : []),
      [regions],
    );
    const regionLabel =
      (regions.state === 'ready' ? regions.data.label : undefined) ?? labels.region;
    const regionIsList =
      hasRegionSource && !!value.country && !(regions.state === 'ready' && regionList.length === 0);
    const regionOptions = useMemo<ComboboxOption[]>(() => {
      const opts: ComboboxOption[] = [...regionList];
      if (value.region && !opts.some((o) => o.value === value.region))
        opts.push({ value: value.region, label: value.regionName || value.region });
      return opts;
    }, [regionList, value.region, value.regionName]);

    const cityValue = value.cityId ?? (value.city ? TYPED + value.city : null);
    const cityOptions = useMemo<ComboboxOption[]>(() => {
      const opts: ComboboxOption[] = cities.map((c) =>
        c.description === undefined
          ? { value: c.value, label: c.label }
          : { value: c.value, label: c.label, description: c.description },
      );
      if (cityValue && !opts.some((o) => o.value === cityValue))
        opts.push({ value: cityValue, label: value.city });
      const typed = cityText.trim();
      if (
        allowCustomCity &&
        citiesState !== 'short' &&
        typed !== '' &&
        typed !== value.city &&
        !opts.some((o) => o.label.toLowerCase() === typed.toLowerCase())
      )
        opts.push({ value: TYPED + typed, label: typed, description: messages.useTyped });
      return opts;
    }, [cities, cityValue, value.city, cityText, citiesState, allowCustomCity, messages.useTyped]);

    const cityEmpty =
      citiesState === 'failed'
        ? messages.loadFailed('cities')
        : citiesState === 'short'
          ? messages.typeMore(cityMinQuery)
          : messages.noMatch;

    const base = idPrefix ?? undefined;
    const fieldId = (f: LocationField) => (base ? `${base}-${f}` : undefined);
    const isRequired = (f: LocationField) => required.includes(f);

    const renderField = (f: LocationField) => {
      const error = errors[f];
      const label = f === 'region' ? regionLabel : labels[f];
      const noCountry = !value.country;
      let control: ReactNode;
      switch (f) {
        case 'country':
          control =
            countries.state === 'failed' ? (
              <LoadFailed
                text={messages.loadFailed('countries')}
                retry={messages.retry}
                onRetry={() => setCountriesTry((n) => n + 1)}
              />
            ) : (
              <Combobox
                aria-label={label}
                options={countryOptions}
                value={value.country}
                loading={countries.state === 'loading'}
                onValueChange={(code) => {
                  if (code === value.country) return;
                  setCityText('');
                  update({ ...EMPTY, country: code }, 'country');
                }}
              />
            );
          break;
        case 'region':
          control =
            regions.state === 'failed' ? (
              <LoadFailed
                text={messages.loadFailed(regionLabel.toLowerCase())}
                retry={messages.retry}
                onRetry={() => setRegionsTry((n) => n + 1)}
              />
            ) : regionIsList ? (
              <Combobox
                aria-label={label}
                options={regionOptions}
                value={value.region}
                loading={regions.state === 'loading'}
                onValueChange={(code) => {
                  const opt = regionOptions.find((o) => o.value === code);
                  setCityText('');
                  update(
                    { region: code, regionName: opt?.label ?? '', city: '', cityId: null },
                    'region',
                  );
                }}
              />
            ) : (
              <Input
                value={value.regionName}
                disabled={noCountry && fields.includes('country') ? true : undefined}
                placeholder={
                  noCountry && fields.includes('country')
                    ? messages.chooseCountryFirst
                    : isRequired('region')
                      ? undefined
                      : messages.optional
                }
                autoComplete="address-level1"
                onChange={(e) => update({ regionName: e.target.value, region: null }, 'region')}
              />
            );
          break;
        case 'city':
          control = adapter.cities ? (
            <Combobox
              aria-label={label}
              options={cityOptions}
              value={cityValue}
              inputValue={cityText}
              filter={() => true}
              loading={citiesState === 'loading'}
              emptyMessage={cityEmpty}
              disabled={noCountry ? true : undefined}
              placeholder={noCountry ? messages.chooseCountryFirst : undefined}
              onInputValueChange={(text) => {
                setCityText(text);
                if (skipQuery.current === text) {
                  skipQuery.current = null;
                  return;
                }
                if (text === value.city) return;
                searchCities(text, cityDebounce);
              }}
              onValueChange={(next) => {
                if (next === null) {
                  skipQuery.current = '';
                  update({ city: '', cityId: null }, 'city');
                  return;
                }
                if (next.startsWith(TYPED)) {
                  const typed = next.slice(TYPED.length);
                  skipQuery.current = typed;
                  update({ city: typed, cityId: null }, 'city');
                  return;
                }
                const city = cities.find((c) => c.value === next);
                if (!city) return;
                skipQuery.current = city.label;
                update(
                  {
                    city: city.label,
                    cityId: city.value,
                    ...(city.region && !value.region
                      ? { region: city.region.value, regionName: city.region.label }
                      : {}),
                  },
                  'city',
                );
              }}
            />
          ) : (
            <Input
              value={value.city}
              autoComplete="address-level2"
              onChange={(e) => update({ city: e.target.value, cityId: null }, 'city')}
            />
          );
          break;
        case 'postalCode':
          control = (
            <Input
              value={value.postalCode}
              autoComplete="postal-code"
              autoCapitalize="characters"
              spellCheck={false}
              onChange={(e) => update({ postalCode: e.target.value }, 'postalCode')}
            />
          );
          break;
      }
      const id = fieldId(f);
      return (
        <Field
          key={f}
          {...(id ? { id } : {})}
          invalid={!!error}
          required={isRequired(f)}
          disabled={disabled}
          className="mn-location-fields__field"
          data-field={f}
        >
          <FieldLabel>{label}</FieldLabel>
          {control}
          <FieldError>{error}</FieldError>
        </Field>
      );
    };

    return (
      <fieldset
        ref={ref}
        {...rest}
        disabled={disabled || undefined}
        className={cx('mn-location-fields', className)}
      >
        {legend ? <legend className="mn-location-fields__legend">{legend}</legend> : null}
        <div className="mn-location-fields__grid">{fields.map(renderField)}</div>
      </fieldset>
    );
  },
);
