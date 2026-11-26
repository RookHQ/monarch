'use client';

import { forwardRef, useMemo, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import { safeCollator } from '../../../internal/collation.js';
import { CheckGlyph } from '../../../internal/icons.js';
import {
  Combobox,
  type ComboboxMessages,
  type ComboboxOption,
  type ComboboxProps,
} from '../../forms/combobox/index.js';

/**
 * About 80 widely used BCP 47 language tags: the default list. Products that
 * support fewer languages pass `languages`.
 */
export const LANGUAGE_TAGS: readonly string[] = (
  'af am ar az be bg bn bs ca cs cy da de el en en-GB en-US es es-419 et eu fa fi fil fr fr-CA ' +
  'ga gl gu ha he hi hr hu hy id ig is it ja jv ka kk km kn ko ky lo lt lv mk ml mn mr ms mt my ' +
  'nb ne nl pa pl ps pt pt-BR pt-PT ro ru si sk sl so sq sr sv sw ta te th tr uk ur uz vi xh yo ' +
  'zh-Hans zh-Hant zu'
).split(' ');

const namesCache = new Map<string, Intl.DisplayNames | null>();
function languageNames(locale: string): Intl.DisplayNames | null {
  if (!namesCache.has(locale)) {
    try {
      namesCache.set(locale, new Intl.DisplayNames([locale], { type: 'language' }));
    } catch {
      namesCache.set(locale, null);
    }
  }
  return namesCache.get(locale) ?? null;
}

/** The canonical form of a BCP 47 tag (`'EN-us'` → `'en-US'`), or `null` when it is not a valid tag. */
export function canonicalLanguageTag(tag: string): string | null {
  try {
    return Intl.getCanonicalLocales(tag)[0] ?? null;
  } catch {
    return null;
  }
}

function capitalize(text: string, locale: string): string {
  try {
    return text.charAt(0).toLocaleUpperCase(locale) + text.slice(1);
  } catch {
    return text.charAt(0).toUpperCase() + text.slice(1);
  }
}

/**
 * The language's name in `locale` (`getLanguageName('de', 'en')` → "German").
 * With no `locale`, the endonym: the name in the language itself
 * (`getLanguageName('de')` → "Deutsch"). The first letter is capitalised for the
 * locale. Returns the tag when the runtime has no name or no `Intl.DisplayNames`.
 */
export function getLanguageName(tag: string, locale?: string): string {
  const inLocale = locale ?? tag;
  try {
    const name = languageNames(inLocale)?.of(tag);
    return name ? capitalize(name, inLocale) : tag;
  } catch {
    return tag;
  }
}

/** One language, as listed by `listLanguages`. */
export interface LanguageInfo {
  /** Canonical BCP 47 tag. */
  tag: string;
  /** Name in the language itself ("Deutsch", "日本語"). */
  endonym: string;
  /** Name in the requested locale ("German"). */
  name: string;
}

/**
 * Languages with endonyms and names in `locale`, sorted by name in `locale`.
 * Invalid tags are dropped. Default: every tag in `LANGUAGE_TAGS`.
 */
export function listLanguages(
  options: { locale?: string | undefined; tags?: readonly string[] | undefined } = {},
): LanguageInfo[] {
  const locale = options.locale ?? 'en';
  // Never throws on an unknown locale.
  const collator = safeCollator(locale);
  const tags = [
    ...new Set(
      (options.tags ?? LANGUAGE_TAGS)
        .map(canonicalLanguageTag)
        .filter((t): t is string => t !== null),
    ),
  ];
  return tags
    .map((tag) => ({ tag, endonym: getLanguageName(tag), name: getLanguageName(tag, locale) }))
    .sort((a, b) => collator.compare(a.name, b.name));
}

/** Interface text, for translation. Every message has an English default. */
export interface LanguageSelectMessages {
  /** Heading over `priority` languages. Default "Suggested". */
  suggested: string;
  /** Heading over the rest when `priority` is set. Default "All languages". */
  all: string;
  /** Shown when nothing matches. Default "No languages match". */
  empty: string;
}

const DEFAULT_MESSAGES: LanguageSelectMessages = {
  suggested: 'Suggested',
  all: 'All languages',
  empty: 'No languages match',
};

export interface LanguageSelectProps extends Omit<
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
  | 'renderOption'
  | 'messages'
> {
  /** Selected BCP 47 tag (controlled), such as `'pt-BR'`. `onValueChange` reports the canonical form. */
  value?: string | null | undefined;
  /** Initially selected tag (uncontrolled). */
  defaultValue?: string | null | undefined;
  /** Called with the chosen canonical tag, or `null` when cleared. */
  onValueChange?: ((tag: string | null) => void) | undefined;
  /** The interface's locale: names under each endonym and the sort order use it. Default `en`. */
  locale?: string | undefined;
  /** Limit the list to these tags, such as the languages the product is translated into. Default: `LANGUAGE_TAGS`. */
  languages?: readonly string[] | undefined;
  /** Tags listed first under "Suggested" while nothing is typed, such as the browser's languages. */
  priority?: readonly string[] | undefined;
  /**
   * What each option's main label is. `endonym` → "Deutsch" with "German"
   * below; `localized` → "German" with "Deutsch" below. Default `endonym`, so
   * people find their own language whatever the interface language is.
   */
  labelStyle?: 'endonym' | 'localized' | undefined;
  /** Show the tag at the end of each option. Default false. */
  showTag?: boolean | undefined;
  /** Interface text, for translation. Combobox messages can be passed here too. */
  messages?: (Partial<LanguageSelectMessages> & Partial<ComboboxMessages>) | undefined;
}

function matches(option: ComboboxOption, query: string): boolean {
  const haystack =
    `${option.label} ${option.description ?? ''} ${option.value} ${option.keywords ?? ''}`.toLowerCase();
  return query
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .every((term) => haystack.includes(term));
}

/**
 * Searchable language picker. The value is a BCP 47 tag; each option shows
 * the language's own name (endonym, marked with its `lang` so screen readers
 * pronounce it) and its name in the interface locale, both from
 * `Intl.DisplayNames`. Search matches either name, the tag and the English
 * name. Built on Combobox; forwards its ref to the input.
 */
export const LanguageSelect = forwardRef<HTMLInputElement, LanguageSelectProps>(
  function LanguageSelect(
    {
      value: valueProp,
      defaultValue = null,
      onValueChange,
      locale = 'en',
      languages,
      priority,
      labelStyle = 'endonym',
      showTag = false,
      messages: messagesProp,
      emptyMessage,
      onInputValueChange,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const [internalValue, setInternalValue] = useState<string | null>(defaultValue);
    const raw = valueProp !== undefined ? valueProp : internalValue;
    const value = raw ? (canonicalLanguageTag(raw) ?? raw) : null;
    const [text, setText] = useState('');

    const toOption = useMemo(() => {
      return (tag: string, group?: string): ComboboxOption => {
        const endonym = getLanguageName(tag);
        const name = getLanguageName(tag, locale);
        const english = getLanguageName(tag, 'en');
        const label = labelStyle === 'endonym' ? endonym : name;
        const other = labelStyle === 'endonym' ? name : endonym;
        return {
          value: tag,
          label,
          ...(other !== label ? { description: other } : {}),
          ...(showTag ? { hint: tag } : {}),
          keywords: `${tag} ${english}`,
          ...(group ? { group } : {}),
        };
      };
    }, [locale, labelStyle, showTag]);

    const all = useMemo(
      () => listLanguages({ locale, tags: languages }).map((l) => l.tag),
      [locale, languages],
    );

    const selectedOption = value ? toOption(value) : null;
    const searching = text.trim() !== '' && text !== (selectedOption?.label ?? '');

    const options = useMemo(() => {
      const extra = value && !all.includes(value) ? [value] : [];
      const top = searching
        ? []
        : [
            ...new Set(
              (priority ?? []).map((t) => canonicalLanguageTag(t)).filter((t) => t !== null),
            ),
          ].filter((t) => all.includes(t));
      if (top.length === 0) return [...extra, ...all].map((t) => toOption(t));
      return [
        ...top.map((t) => toOption(t, messages.suggested)),
        ...[...extra, ...all].map((t) => toOption(t, messages.all)),
      ];
    }, [all, value, searching, priority, toOption, messages.suggested, messages.all]);

    // The endonym is in its own language; the localized name is in the interface's.
    const endonymFirst = labelStyle === 'endonym';
    const renderOption = (o: ComboboxOption, state: { selected: boolean }) => (
      <>
        <span className="mn-menu__text">
          <span lang={endonymFirst ? o.value : locale}>{o.label}</span>
          {o.description ? (
            <span className="mn-combobox__description" lang={endonymFirst ? locale : o.value}>
              {o.description}
            </span>
          ) : null}
        </span>
        {o.hint ? <span className="mn-combobox__hint">{o.hint}</span> : null}
        {state.selected ? (
          <span className="mn-menu__indicator">
            <CheckGlyph />
          </span>
        ) : null}
      </>
    );

    return (
      <Combobox
        ref={ref}
        sortByMatch
        {...rest}
        className={cx('mn-language-select', className)}
        options={options}
        value={value}
        valueOptions={selectedOption ? [selectedOption] : undefined}
        filter={matches}
        renderOption={renderOption}
        emptyMessage={emptyMessage ?? messages.empty}
        messages={messages}
        onValueChange={(next) => {
          if (valueProp === undefined) setInternalValue(next);
          onValueChange?.(next);
        }}
        onInputValueChange={(t) => {
          setText(t);
          onInputValueChange?.(t);
        }}
      />
    );
  },
);
