'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { safeCollator } from '../../../internal/collation.js';
import { SearchGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import {
  LocationFields,
  type LocationAdapter,
  type LocationField,
  type LocationValue,
} from '../location-fields/index.js';
import { PHONE_COUNTRIES } from '../phone-country-select/index.js';

/** A structured address: two street lines plus the LocationFields value. */
export interface AddressValue extends LocationValue {
  /** Street and number. */
  line1: string;
  /** Flat, suite, floor or building. */
  line2: string;
}

/** One suggestion from the provider. */
export interface AddressSuggestion {
  /** Stable id from the provider, such as a place id. */
  id: string;
  /** The main line people read, such as "10 Market Street". */
  label: string;
  /** Secondary text, such as the city and country. */
  description?: string | undefined;
  /**
   * The structured address, when the search already returns it. Without it,
   * `resolve` is called to fetch the details after the pick.
   */
  address?: Partial<AddressValue> | undefined;
}

/** Passed to `search` and `resolve`. The signal aborts when a newer request starts or the field unmounts. */
export interface AddressRequestContext {
  signal: AbortSignal;
}

/** Where the address field is in its flow. */
export type AddressEntryMode = 'search' | 'manual';

/** Interface text, for translation. Every message has an English default. */
export interface AddressAutocompleteMessages {
  /** Label of the search field. Default "Address". */
  label: string;
  /** Placeholder of the search field. Default "Start typing an address". */
  placeholder: string;
  /** Accessible name of the suggestion list. Default "Address suggestions". */
  listLabel: string;
  /** Shown while suggestions load. Default "Searching…". */
  searching: string;
  /** Shown while the picked suggestion's details load. Default "Getting the address details…". */
  resolving: string;
  /** Shown when the search finds nothing. Default "No addresses found". */
  empty: string;
  /** Shown when `search` rejects. Default "Couldn’t search addresses.". */
  searchFailed: string;
  /** Shown when `resolve` rejects. Default "Couldn’t get that address. Enter it manually or try another.". */
  resolveFailed: string;
  /** Retry button. Default "Try again". */
  retry: string;
  /** Shown until `minQueryLength` characters are typed. */
  typeMore: (min: number) => string;
  /** The option and button that open manual entry. Default "Enter the address manually". */
  enterManually: string;
  /** The button that returns to search. Default "Search for an address". */
  backToSearch: string;
  /** Announced after a suggestion fills the fields. Default "Address filled in. Check the details.". */
  filled: string;
  /** Announced with the number of suggestions. */
  results: (count: number) => string;
  /** Label of the first street line. Default "Street address". */
  line1: string;
  /** Label of the second street line. Default "Apartment, suite, etc.". */
  line2: string;
  /** Text after optional labels. Default "Optional". */
  optional: string;
}

const DEFAULT_MESSAGES: AddressAutocompleteMessages = {
  label: 'Address',
  placeholder: 'Start typing an address',
  listLabel: 'Address suggestions',
  searching: 'Searching…',
  resolving: 'Getting the address details…',
  empty: 'No addresses found',
  searchFailed: 'Couldn’t search addresses.',
  resolveFailed: 'Couldn’t get that address. Enter it manually or try another.',
  retry: 'Try again',
  typeMore: (min) => `Type at least ${min} characters to search.`,
  enterManually: 'Enter the address manually',
  backToSearch: 'Search for an address',
  filled: 'Address filled in. Check the details.',
  results: (count) => (count === 1 ? '1 address found' : `${count} addresses found`),
  line1: 'Street address',
  line2: 'Apartment, suite, etc.',
  optional: 'Optional',
};

const EMPTY_ADDRESS: AddressValue = {
  line1: '',
  line2: '',
  country: null,
  region: null,
  regionName: '',
  city: '',
  cityId: null,
  postalCode: '',
};

/** Fills the missing parts of a partial address with empty values. */
export function toAddressValue(partial: Partial<AddressValue> = {}): AddressValue {
  return { ...EMPTY_ADDRESS, ...partial };
}

/** Countries from `Intl.DisplayNames`, without regions or cities (both typed freely). */
function defaultAdapter(locale: string | undefined): LocationAdapter {
  return {
    countries: () => {
      // Never throws on an unknown locale, and one collator for the whole sort.
      const collator = safeCollator(locale);
      let names: Intl.DisplayNames | null = null;
      try {
        names = new Intl.DisplayNames(locale ? [locale] : undefined, { type: 'region' });
      } catch {
        names = null;
      }
      const list = PHONE_COUNTRIES.map((c) => {
        let label = c.code;
        try {
          label = names?.of(c.code) ?? c.code;
        } catch {
          // Unknown to this runtime: keep the code.
        }
        return { value: c.code, label };
      }).sort((a, b) => collator.compare(a.label, b.label));
      return Promise.resolve(list);
    },
  };
}

type SearchState =
  | { status: 'idle' }
  | { status: 'short' }
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'ready'; items: AddressSuggestion[] };

type Item = { kind: 'suggestion'; suggestion: AddressSuggestion } | { kind: 'manual' };

export interface AddressAutocompleteProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange'
> {
  /**
   * The suggestion provider: your own endpoint or a geocoding service behind
   * it. Called with the trimmed query after `debounce` ms; a newer query
   * aborts the previous call through `signal`, and late answers are ignored.
   */
  search: (query: string, context: AddressRequestContext) => Promise<AddressSuggestion[]>;
  /**
   * Turns a picked suggestion into a structured address, for providers whose
   * search returns only labels. Omit when suggestions carry `address`.
   */
  resolve?:
    | ((
        suggestion: AddressSuggestion,
        context: AddressRequestContext,
      ) => Promise<Partial<AddressValue>>)
    | undefined;
  /** The address (controlled). */
  value?: AddressValue | undefined;
  /** Initial address when uncontrolled. */
  defaultValue?: Partial<AddressValue> | undefined;
  /** Called with the whole new address whenever a suggestion fills it or a field changes. */
  onValueChange?: ((value: AddressValue) => void) | undefined;
  /** Called after a suggestion is picked and resolved, with the suggestion and the address it gave. */
  onSuggestionSelect?: ((suggestion: AddressSuggestion, value: AddressValue) => void) | undefined;
  /**
   * `search` shows only the search field; `manual` shows the street lines and
   * LocationFields (filled in after a pick, so people can check them). Controlled.
   */
  mode?: AddressEntryMode | undefined;
  /** Initial mode when uncontrolled. Default `search`, or `manual` when `defaultValue` has a street line. */
  defaultMode?: AddressEntryMode | undefined;
  onModeChange?: ((mode: AddressEntryMode) => void) | undefined;
  /** Offer manual entry from the list and below the field. Default true. */
  allowManualEntry?: boolean | undefined;
  /**
   * Data for the manual country, region and city fields. Default: every
   * country named by `Intl.DisplayNames`, with region and city typed freely.
   */
  locationAdapter?: LocationAdapter | undefined;
  /** Language for the default country names, such as "de". */
  locale?: string | undefined;
  /** Location fields shown in manual mode, in order. Default all four. */
  locationFields?: LocationField[] | undefined;
  /** Characters needed before `search` runs. Default 3. */
  minQueryLength?: number | undefined;
  /** Milliseconds without typing before `search` runs. Default 300. */
  debounce?: number | undefined;
  /** Help text under the search field. */
  description?: ReactNode | undefined;
  /** Error under the search field; marks it invalid. */
  error?: ReactNode | undefined;
  /** Errors for the manual fields. */
  fieldErrors?: Partial<Record<'line1' | 'line2' | LocationField, ReactNode>> | undefined;
  /** Marks the street line and the country required. */
  required?: boolean | undefined;
  disabled?: boolean | undefined;
  /** Prefix for field ids, so two address forms on one page never clash. */
  idPrefix?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<AddressAutocompleteMessages> | undefined;
}

/**
 * An address field with suggestions from your own provider: people type, pick
 * a suggestion, and the street, city, region, postal code and country fill in
 * for review. Monarch ships no vendor SDK and makes no requests: `search` and
 * `resolve` are callbacks. Manual entry (street lines plus LocationFields) is
 * always one step away, for addresses the provider does not know. The search
 * field follows the WAI-ARIA combobox pattern with a listbox popup.
 */
export const AddressAutocomplete = forwardRef<HTMLDivElement, AddressAutocompleteProps>(
  function AddressAutocomplete(
    {
      search,
      resolve,
      value: valueProp,
      defaultValue,
      onValueChange,
      onSuggestionSelect,
      mode: modeProp,
      defaultMode,
      onModeChange,
      allowManualEntry = true,
      locationAdapter,
      locale,
      locationFields,
      minQueryLength = 3,
      debounce = 300,
      description,
      error,
      fieldErrors = {},
      required = false,
      disabled = false,
      idPrefix,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const auto = useId();
    const base = idPrefix ?? `mn-address-${auto.replace(/:/g, '')}`;
    const listId = `${base}-list`;

    // ----- Value -----
    const [internal, setInternal] = useState<AddressValue>(() => toAddressValue(defaultValue));
    const value = valueProp ?? internal;
    const setValue = (next: AddressValue) => {
      if (valueProp === undefined) setInternal(next);
      onValueChange?.(next);
    };

    // ----- Mode -----
    const [modeState, setModeState] = useState<AddressEntryMode>(
      defaultMode ?? (defaultValue?.line1 ? 'manual' : 'search'),
    );
    const mode = modeProp ?? modeState;
    const focusLine1 = useRef(false);
    const focusSearch = useRef(false);
    const setMode = (next: AddressEntryMode, focus = true) => {
      focusLine1.current = focus && next === 'manual';
      focusSearch.current = focus && next === 'search';
      if (modeProp === undefined) setModeState(next);
      onModeChange?.(next);
    };

    // ----- Search -----
    const [query, setQuery] = useState('');
    const [open, setOpen] = useState(false);
    const [state, setState] = useState<SearchState>({ status: 'idle' });
    const [attempt, setAttempt] = useState(0);
    const [active, setActive] = useState(-1);
    const [resolving, setResolving] = useState(false);
    const [resolveFailed, setResolveFailed] = useState(false);
    const [announcement, setAnnouncement] = useState('');
    const searchRef = useRef(search);
    searchRef.current = search;
    const inputRef = useRef<HTMLInputElement | null>(null);
    const line1Ref = useRef<HTMLInputElement | null>(null);
    const resolveCtl = useRef<AbortController | null>(null);

    const trimmed = query.trim();
    useEffect(() => {
      if (!open || mode !== 'search') return;
      if (trimmed.length < minQueryLength) {
        setState(trimmed === '' ? { status: 'idle' } : { status: 'short' });
        return;
      }
      const controller = new AbortController();
      setState({ status: 'loading' });
      const timer = setTimeout(() => {
        Promise.resolve()
          .then(() => searchRef.current(trimmed, { signal: controller.signal }))
          .then(
            (items) => {
              if (controller.signal.aborted) return;
              setState({ status: 'ready', items });
              setAnnouncement(items.length === 0 ? messages.empty : messages.results(items.length));
            },
            () => {
              if (!controller.signal.aborted) setState({ status: 'error' });
            },
          );
      }, debounce);
      return () => {
        clearTimeout(timer);
        controller.abort();
      };
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [trimmed, open, mode, attempt, minQueryLength, debounce]);

    useEffect(() => () => resolveCtl.current?.abort(), []);

    useEffect(() => {
      if (focusLine1.current && mode === 'manual') line1Ref.current?.focus();
      if (focusSearch.current && mode === 'search') inputRef.current?.focus();
      focusLine1.current = false;
      focusSearch.current = false;
    }, [mode]);

    const items = useMemo<Item[]>(() => {
      const out: Item[] =
        state.status === 'ready'
          ? state.items.map((suggestion) => ({ kind: 'suggestion', suggestion }))
          : [];
      if (allowManualEntry && state.status !== 'idle' && state.status !== 'short')
        out.push({ kind: 'manual' });
      return out;
    }, [state, allowManualEntry]);

    useEffect(() => setActive(-1), [items]);

    const expanded = open && mode === 'search' && state.status !== 'idle';

    const finish = (suggestion: AddressSuggestion, address: Partial<AddressValue>) => {
      const next = toAddressValue({ line1: suggestion.label, ...address });
      setValue(next);
      onSuggestionSelect?.(suggestion, next);
      setAnnouncement(messages.filled);
      setQuery('');
      setMode('manual');
    };

    const pick = (item: Item | undefined) => {
      if (!item) return;
      setOpen(false);
      if (item.kind === 'manual') {
        setValue({ ...value, line1: value.line1 || trimmed });
        setQuery('');
        setMode('manual');
        return;
      }
      const { suggestion } = item;
      setQuery(suggestion.label);
      setResolveFailed(false);
      if (suggestion.address || !resolve) {
        finish(suggestion, suggestion.address ?? {});
        return;
      }
      resolveCtl.current?.abort();
      const controller = new AbortController();
      resolveCtl.current = controller;
      setResolving(true);
      Promise.resolve()
        .then(() => resolve(suggestion, { signal: controller.signal }))
        .then(
          (address) => {
            if (controller.signal.aborted) return;
            setResolving(false);
            finish(suggestion, address);
          },
          () => {
            if (controller.signal.aborted) return;
            setResolving(false);
            setResolveFailed(true);
          },
        );
    };

    const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
      // Enter, Escape and the arrows belong to the input method while it composes
      // (Japanese, Chinese, Korean): Enter there confirms the text, not a suggestion.
      if (e.nativeEvent.isComposing || e.keyCode === 229) return;
      const last = items.length - 1;
      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          if (!open) setOpen(true);
          else if (!e.altKey && last >= 0) setActive((a) => (a >= last ? 0 : a + 1));
          break;
        case 'ArrowUp':
          e.preventDefault();
          if (e.altKey) setOpen(false);
          else if (!open) setOpen(true);
          else if (last >= 0) setActive((a) => (a <= 0 ? last : a - 1));
          break;
        case 'Enter':
          if (expanded && active >= 0) {
            e.preventDefault();
            pick(items[active]);
          } else if (expanded && state.status === 'error') {
            e.preventDefault();
            setAttempt((n) => n + 1);
          }
          break;
        case 'Escape':
          if (expanded) {
            e.preventDefault();
            setOpen(false);
          } else if (query !== '') {
            e.preventDefault();
            setQuery('');
          }
          break;
        case 'Tab':
          setOpen(false);
          break;
      }
    };

    const activeId = expanded && active >= 0 ? `${listId}-${active}` : undefined;

    const statusText =
      state.status === 'short'
        ? messages.typeMore(minQueryLength)
        : state.status === 'loading'
          ? messages.searching
          : state.status === 'ready' && state.items.length === 0
            ? messages.empty
            : null;

    const adapter = useMemo(
      () => locationAdapter ?? defaultAdapter(locale),
      [locationAdapter, locale],
    );

    const location: LocationValue = {
      country: value.country,
      region: value.region,
      regionName: value.regionName,
      city: value.city,
      cityId: value.cityId,
      postalCode: value.postalCode,
    };
    const busy = state.status === 'loading' || resolving;

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-address-autocomplete', className)}
        data-mode={mode}
        data-state={resolving ? 'resolving' : expanded ? state.status : undefined}
        data-disabled={disabled || undefined}
      >
        {mode === 'search' ? (
          <div className="mn-address-autocomplete__search">
            <Field
              id={`${base}-search`}
              invalid={!!error}
              required={required}
              disabled={disabled}
              className="mn-address-autocomplete__field"
            >
              <FieldLabel>{messages.label}</FieldLabel>
              <div className="mn-address-autocomplete__control">
                <SearchGlyph className="mn-address-autocomplete__icon" aria-hidden />
                <Input
                  ref={inputRef}
                  className="mn-address-autocomplete__input"
                  type="text"
                  role="combobox"
                  autoComplete="off"
                  spellCheck={false}
                  aria-autocomplete="list"
                  aria-expanded={expanded}
                  aria-controls={expanded && items.length > 0 ? listId : undefined}
                  aria-activedescendant={activeId}
                  aria-busy={busy || undefined}
                  placeholder={messages.placeholder}
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    setResolveFailed(false);
                    // Typing again withdraws the pick whose details are still loading;
                    // its late answer must not fill the fields and leave the search.
                    if (resolving) {
                      resolveCtl.current?.abort();
                      setResolving(false);
                    }
                    setOpen(true);
                  }}
                  onFocus={() => {
                    if (trimmed !== '') setOpen(true);
                  }}
                  onBlur={(e) => {
                    const next = e.relatedTarget as Node | null;
                    if (
                      !next ||
                      !e.currentTarget.closest('.mn-address-autocomplete__search')?.contains(next)
                    )
                      setOpen(false);
                  }}
                  onKeyDown={onKeyDown}
                />
                {busy ? <Spinner size="sm" className="mn-address-autocomplete__spinner" /> : null}
              </div>
              {description ? <FieldDescription>{description}</FieldDescription> : null}
              <FieldError>{error}</FieldError>
            </Field>

            {expanded ? (
              <div className="mn-address-autocomplete__popup">
                {statusText ? (
                  <p className="mn-address-autocomplete__status">{statusText}</p>
                ) : null}
                {state.status === 'error' ? (
                  <div className="mn-address-autocomplete__failed" role="alert">
                    <span>{messages.searchFailed}</span>
                    <Button
                      size="xs"
                      variant="secondary"
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => {
                        setAttempt((n) => n + 1);
                        inputRef.current?.focus();
                      }}
                    >
                      {messages.retry}
                    </Button>
                  </div>
                ) : null}
                {items.length > 0 ? (
                  <div
                    id={listId}
                    role="listbox"
                    aria-label={messages.listLabel}
                    className="mn-address-autocomplete__list"
                  >
                    {items.map((item, i) => (
                      // Keyboard is handled on the combobox input (aria-activedescendant).
                      // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
                      <div
                        key={item.kind === 'manual' ? 'manual' : item.suggestion.id}
                        id={`${listId}-${i}`}
                        role="option"
                        aria-selected={i === active}
                        data-highlighted={i === active || undefined}
                        data-kind={item.kind}
                        className="mn-address-autocomplete__option"
                        onMouseDown={(e) => e.preventDefault()}
                        onMouseMove={() => setActive(i)}
                        onClick={() => pick(item)}
                      >
                        {item.kind === 'manual' ? (
                          <span className="mn-address-autocomplete__option-label">
                            {messages.enterManually}
                          </span>
                        ) : (
                          <>
                            <span className="mn-address-autocomplete__option-label">
                              {item.suggestion.label}
                            </span>
                            {item.suggestion.description ? (
                              <span className="mn-address-autocomplete__option-description">
                                {item.suggestion.description}
                              </span>
                            ) : null}
                          </>
                        )}
                      </div>
                    ))}
                  </div>
                ) : null}
              </div>
            ) : null}

            {resolving ? (
              <p className="mn-address-autocomplete__status">{messages.resolving}</p>
            ) : null}
            {resolveFailed ? (
              <p className="mn-address-autocomplete__failed" role="alert">
                {messages.resolveFailed}
              </p>
            ) : null}
            {allowManualEntry ? (
              <Button
                variant="ghost"
                size="sm"
                className="mn-address-autocomplete__switch"
                disabled={disabled}
                onClick={() => pick({ kind: 'manual' })}
              >
                {messages.enterManually}
              </Button>
            ) : null}
          </div>
        ) : (
          <div className="mn-address-autocomplete__manual">
            <div className="mn-address-autocomplete__lines">
              <Field
                id={`${base}-line1`}
                required={required}
                disabled={disabled}
                invalid={!!fieldErrors.line1}
                className="mn-address-autocomplete__line"
              >
                <FieldLabel>{messages.line1}</FieldLabel>
                <Input
                  ref={line1Ref}
                  autoComplete="address-line1"
                  value={value.line1}
                  onChange={(e) => setValue({ ...value, line1: e.target.value })}
                />
                <FieldError>{fieldErrors.line1}</FieldError>
              </Field>
              <Field
                id={`${base}-line2`}
                disabled={disabled}
                invalid={!!fieldErrors.line2}
                className="mn-address-autocomplete__line"
              >
                <FieldLabel optionalText={messages.optional}>{messages.line2}</FieldLabel>
                <Input
                  autoComplete="address-line2"
                  value={value.line2}
                  onChange={(e) => setValue({ ...value, line2: e.target.value })}
                />
                <FieldError>{fieldErrors.line2}</FieldError>
              </Field>
            </div>
            <LocationFields
              adapter={adapter}
              value={location}
              onValueChange={(next) =>
                setValue({ ...next, line1: value.line1, line2: value.line2 })
              }
              idPrefix={`${base}-location`}
              disabled={disabled}
              required={required ? ['country'] : []}
              errors={{
                ...(fieldErrors.country ? { country: fieldErrors.country } : {}),
                ...(fieldErrors.region ? { region: fieldErrors.region } : {}),
                ...(fieldErrors.city ? { city: fieldErrors.city } : {}),
                ...(fieldErrors.postalCode ? { postalCode: fieldErrors.postalCode } : {}),
              }}
              {...(locationFields ? { fields: locationFields } : {})}
            />
            <Button
              variant="ghost"
              size="sm"
              className="mn-address-autocomplete__switch"
              disabled={disabled}
              onClick={() => setMode('search')}
            >
              {messages.backToSearch}
            </Button>
          </div>
        )}
        <span className="mn-visually-hidden" role="status">
          {announcement}
        </span>
      </div>
    );
  },
);
