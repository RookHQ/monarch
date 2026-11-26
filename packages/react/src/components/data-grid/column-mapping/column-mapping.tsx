'use client';

import { forwardRef, useId, useMemo, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { Badge } from '../../data-display/badge/index.js';
import { Button } from '../../actions/button/index.js';
import { NativeSelect } from '../../forms/form-field/index.js';

/** A column of the source data (a file header), with a few example values. */
export interface ColumnMappingSource {
  /** The header text. Unique among the sources. */
  name: string;
  /** Example values from the first rows, shown next to the column. */
  samples?: readonly string[] | undefined;
}

/** A field the data can be mapped to. The same shape as ImportWizard's `ImportField`. */
export interface ColumnMappingField {
  /** Stable id: the value of the mapping. */
  id: string;
  /** Label in the field picker. */
  label: string;
  /** Must be mapped from some column. */
  required?: boolean | undefined;
  /** Help text, e.g. the expected format. Shown under the picker once chosen. */
  description?: string | undefined;
  /** Other header names that mean this field ("E-mail", "Mail"), for suggestions. */
  aliases?: readonly string[] | undefined;
}

/** Source column name → field id. Missing or `undefined` means "do not import". */
export type ColumnMappingValue = Record<string, string | undefined>;

/** How sure a suggestion is. */
export type ColumnMappingConfidence = 'exact' | 'similar';

export interface ColumnMappingSuggestion {
  fieldId: string;
  /** `exact`: the header equals the field id, label or an alias. `similar`: a close match. */
  confidence: ColumnMappingConfidence;
  /** 0–1 similarity score. */
  score: number;
}

/** Lower-cases and keeps only letters and digits: "E-mail Address" → "emailaddress". */
export function normalizeColumnName(value: string): string {
  return value
    .normalize('NFKD')
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]/gu, '');
}

function words(value: string): string[] {
  return value
    .normalize('NFKD')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .toLowerCase()
    .split(/[^\p{L}\p{N}]+/u)
    .filter(Boolean);
}

/** Similarity of two short names, 0–1: the best of word overlap and edit distance. */
function similarity(a: string, b: string): number {
  const na = normalizeColumnName(a);
  const nb = normalizeColumnName(b);
  if (!na || !nb) return 0;
  if (na === nb) return 1;
  // Word overlap (Dice coefficient): "Phone number" vs "Mobile phone".
  const wa = new Set(words(a));
  const wb = new Set(words(b));
  let shared = 0;
  for (const w of wa) if (wb.has(w)) shared++;
  const dice = wa.size + wb.size ? (2 * shared) / (wa.size + wb.size) : 0;
  // One name contains the other: "email" in "workemail".
  const contains =
    Math.min(na.length, nb.length) >= 3 && (na.includes(nb) || nb.includes(na))
      ? 0.6 + 0.3 * (Math.min(na.length, nb.length) / Math.max(na.length, nb.length))
      : 0;
  // Edit distance for typos: "adress" vs "address".
  const prev = Array.from({ length: nb.length + 1 }, (_, j) => j);
  for (let i = 1; i <= na.length; i++) {
    let diag = prev[0] ?? 0;
    prev[0] = i;
    for (let j = 1; j <= nb.length; j++) {
      const up = prev[j] ?? 0;
      prev[j] = Math.min(up + 1, (prev[j - 1] ?? 0) + 1, diag + (na[i - 1] === nb[j - 1] ? 0 : 1));
      diag = up;
    }
  }
  const edit = 1 - (prev[nb.length] ?? 0) / Math.max(na.length, nb.length);
  return Math.max(dice, contains, edit);
}

const sourceName = (s: string | ColumnMappingSource) => (typeof s === 'string' ? s : s.name);

/**
 * Suggests a field for each source column. Exact matches (the header equals
 * a field's id, label or alias, ignoring case and punctuation) come first;
 * then close matches scoring at least `threshold` (default 0.75). Each field
 * is suggested for one column at most.
 */
export function suggestColumnMapping(
  sources: readonly (string | ColumnMappingSource)[],
  fields: readonly ColumnMappingField[],
  threshold = 0.75,
): Record<string, ColumnMappingSuggestion | undefined> {
  const names = sources.map(sourceName);
  const candidates: { source: string; field: string; score: number }[] = [];
  for (const source of names) {
    for (const f of fields) {
      let best = 0;
      for (const name of [f.id, f.label, ...(f.aliases ?? [])]) {
        best = Math.max(best, similarity(source, name));
        if (best === 1) break;
      }
      if (best >= threshold) candidates.push({ source, field: f.id, score: best });
    }
  }
  // Greedy by score; ties keep the source order.
  candidates.sort((x, y) => y.score - x.score);
  const out: Record<string, ColumnMappingSuggestion | undefined> = {};
  const usedFields = new Set<string>();
  for (const c of candidates) {
    if (out[c.source] || usedFields.has(c.field)) continue;
    out[c.source] = {
      fieldId: c.field,
      confidence: c.score === 1 ? 'exact' : 'similar',
      score: Math.round(c.score * 100) / 100,
    };
    usedFields.add(c.field);
  }
  return out;
}

/** The suggestions as a mapping value, ready for `defaultValue`. */
export function mappingFromSuggestions(
  suggestions: Record<string, ColumnMappingSuggestion | undefined>,
): ColumnMappingValue {
  const out: ColumnMappingValue = {};
  for (const [source, s] of Object.entries(suggestions)) if (s) out[source] = s.fieldId;
  return out;
}

/**
 * Flips a mapping to field id → source column, the shape ImportWizard's
 * `ImportMapping` and `toImportRecords` use. The first column wins when two
 * map to the same field.
 */
export function toFieldMapping(value: ColumnMappingValue): Record<string, string | undefined> {
  const out: Record<string, string | undefined> = {};
  for (const [source, field] of Object.entries(value)) {
    if (field && out[field] === undefined) out[field] = source;
  }
  return out;
}

/** What still blocks a mapping: required fields nobody maps, and fields mapped twice. */
export function columnMappingIssues(
  value: ColumnMappingValue,
  fields: readonly ColumnMappingField[],
): { missing: ColumnMappingField[]; duplicates: Record<string, string[]> } {
  const bySource = new Map<string, string[]>();
  for (const [source, field] of Object.entries(value)) {
    if (!field) continue;
    bySource.set(field, [...(bySource.get(field) ?? []), source]);
  }
  const duplicates: Record<string, string[]> = {};
  for (const [field, list] of bySource) if (list.length > 1) duplicates[field] = list;
  return { missing: fields.filter((f) => f.required && !bySource.has(f.id)), duplicates };
}

export interface ColumnMappingMessages {
  /** Accessible name of the list. */
  label: string;
  /** Column headings (shown above the rows on wide screens). */
  sourceHeading: string;
  sampleHeading: string;
  fieldHeading: string;
  /** Picker name: "Field for E-mail". */
  fieldFor: (source: string) => string;
  /** The "do not import" option. */
  skip: string;
  /** Suffix of required field options. */
  required: string;
  /** Badges. */
  exactMatch: string;
  suggested: string;
  /** Example values line. */
  samples: (values: string) => string;
  /** No example values. */
  noSamples: string;
  /** Summary: "8 of 10 columns mapped". */
  summary: (mapped: number, total: number) => string;
  /** Required fields that no column maps to. */
  missing: (fields: string) => string;
  /** A field chosen for more than one column. */
  duplicate: (field: string, otherSources: string) => string;
  /** Resets every picker to the suggestions. */
  useSuggestions: string;
  /** Sets every picker to "do not import". */
  skipAll: string;
}

export const defaultColumnMappingMessages: ColumnMappingMessages = {
  label: 'Column mapping',
  sourceHeading: 'Column in file',
  sampleHeading: 'Example values',
  fieldHeading: 'Import as',
  fieldFor: (s) => `Field for ${s}`,
  skip: 'Don’t import',
  required: 'required',
  exactMatch: 'Matched',
  suggested: 'Suggested',
  samples: (v) => v,
  noSamples: 'No values',
  summary: (mapped, total) => `${mapped} of ${total} columns mapped`,
  missing: (f) => `Map a column to: ${f}.`,
  duplicate: (f, others) => `${f} is also chosen for ${others}.`,
  useSuggestions: 'Use suggestions',
  skipAll: 'Don’t import any',
};

export interface ColumnMappingProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children'
> {
  /** The source columns (file headers), as names or `{ name, samples }`. */
  sources: readonly (string | ColumnMappingSource)[];
  /** The fields they can be mapped to. */
  fields: readonly ColumnMappingField[];
  /** The mapping (controlled): source column → field id. */
  value?: ColumnMappingValue | undefined;
  /** The initial mapping (uncontrolled). Default: the suggestions. */
  defaultValue?: ColumnMappingValue | undefined;
  /** Called with the whole new mapping. */
  onValueChange?: ((value: ColumnMappingValue) => void) | undefined;
  /** Suggestions to show. Default: `suggestColumnMapping(sources, fields)`. */
  suggestions?: Record<string, ColumnMappingSuggestion | undefined> | undefined;
  /** Show the required and duplicate problems now (e.g. after a Next press). Default true. */
  showIssues?: boolean | undefined;
  /** Shows "Use suggestions" and "Don't import any" buttons. Default true. */
  bulkActions?: boolean | undefined;
  /** Picker size. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /** Disables every picker. */
  disabled?: boolean | undefined;
  messages?: Partial<ColumnMappingMessages> | undefined;
}

/**
 * Maps the columns of imported data (a CSV header, a spreadsheet) to the
 * product's fields: one picker per column with example values, suggestions
 * from names and aliases, and a summary of required fields still unmapped and
 * fields chosen twice. The mapping step of an import, on its own.
 */
export const ColumnMapping = forwardRef<HTMLDivElement, ColumnMappingProps>(function ColumnMapping(
  {
    sources,
    fields,
    value: valueProp,
    defaultValue,
    onValueChange,
    suggestions: suggestionsProp,
    showIssues = true,
    bulkActions = true,
    size = 'md',
    disabled = false,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultColumnMappingMessages, ...messages };
  const uid = useId();
  const computed = useMemo(
    () => suggestionsProp ?? suggestColumnMapping(sources, fields),
    [suggestionsProp, sources, fields],
  );
  const [inner, setInner] = useState<ColumnMappingValue>(
    () => defaultValue ?? mappingFromSuggestions(computed),
  );
  const value = valueProp ?? inner;
  const set = (next: ColumnMappingValue) => {
    if (valueProp === undefined) setInner(next);
    onValueChange?.(next);
  };

  const list = sources.map((s) => (typeof s === 'string' ? { name: s } : s));
  const fieldById = new Map(fields.map((f) => [f.id, f]));
  const { missing, duplicates } = columnMappingIssues(value, fields);
  const mapped = list.filter((s) => value[s.name] && fieldById.has(value[s.name] ?? '')).length;
  const suggestedValue = mappingFromSuggestions(computed);
  const sameAsSuggested = list.every(
    (s) => (value[s.name] ?? undefined) === (suggestedValue[s.name] ?? undefined),
  );
  const anyMapped = list.some((s) => value[s.name]);
  const hasSuggestions = Object.values(computed).some(Boolean);

  const options = [
    { value: '', label: m.skip },
    ...fields.map((f) => ({
      value: f.id,
      label: f.required ? `${f.label} (${m.required})` : f.label,
    })),
  ];

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-column-mapping', className)}
      data-size={size}
      data-invalid={
        (showIssues && (missing.length > 0 || Object.keys(duplicates).length > 0)) || undefined
      }
    >
      <div className="mn-column-mapping__summary">
        <p className="mn-column-mapping__count" role="status">
          {m.summary(mapped, list.length)}
        </p>
        {bulkActions && (hasSuggestions || anyMapped) ? (
          <div className="mn-column-mapping__bulk">
            {hasSuggestions && !sameAsSuggested ? (
              <Button
                size="sm"
                variant="ghost"
                disabled={disabled}
                onClick={() => set({ ...suggestedValue })}
              >
                {m.useSuggestions}
              </Button>
            ) : null}
            {anyMapped ? (
              <Button size="sm" variant="ghost" disabled={disabled} onClick={() => set({})}>
                {m.skipAll}
              </Button>
            ) : null}
          </div>
        ) : null}
      </div>
      {showIssues && missing.length ? (
        <p className="mn-column-mapping__missing" role="alert">
          {m.missing(missing.map((f) => f.label).join(', '))}
        </p>
      ) : null}
      <div className="mn-column-mapping__head" aria-hidden>
        <span>{m.sourceHeading}</span>
        <span>{m.sampleHeading}</span>
        <span>{m.fieldHeading}</span>
      </div>
      <ul className="mn-column-mapping__list" aria-label={m.label}>
        {list.map((s, i) => {
          const current = value[s.name] ?? '';
          const field = fieldById.get(current);
          const suggestion = computed[s.name];
          const matchesSuggestion = !!suggestion && suggestion.fieldId === current;
          const others = field ? (duplicates[field.id] ?? []).filter((n) => n !== s.name) : [];
          const dup = showIssues && others.length > 0;
          const base = `${uid}-${i}`;
          const describedBy = [
            `${base}-samples`,
            matchesSuggestion ? `${base}-badge` : null,
            dup ? `${base}-error` : null,
            field?.description ? `${base}-help` : null,
          ]
            .filter(Boolean)
            .join(' ');
          const samples = (s.samples ?? []).filter((v) => v.trim() !== '').slice(0, 3);
          return (
            <li
              // Files repeat headers (two blank trailing columns, say): the position keeps keys unique.
              key={`${i}:${s.name}`}
              className="mn-column-mapping__row"
              data-mapped={field ? '' : undefined}
              data-invalid={dup || undefined}
            >
              <span className="mn-column-mapping__source" title={s.name}>
                {s.name || `#${i + 1}`}
              </span>
              <span
                id={`${base}-samples`}
                className="mn-column-mapping__samples"
                data-empty={samples.length ? undefined : ''}
              >
                {samples.length ? m.samples(samples.join(', ')) : m.noSamples}
              </span>
              <span className="mn-column-mapping__field">
                <span className="mn-column-mapping__picker">
                  <NativeSelect
                    size={size}
                    aria-label={m.fieldFor(s.name || `#${i + 1}`)}
                    aria-describedby={describedBy || undefined}
                    invalid={dup}
                    disabled={disabled}
                    value={current}
                    options={options}
                    onChange={(e) => set({ ...value, [s.name]: e.target.value || undefined })}
                  />
                  {matchesSuggestion ? (
                    <Badge
                      id={`${base}-badge`}
                      size="sm"
                      tone={suggestion.confidence === 'exact' ? 'success' : 'info'}
                      className="mn-column-mapping__badge"
                    >
                      {suggestion.confidence === 'exact' ? m.exactMatch : m.suggested}
                    </Badge>
                  ) : null}
                </span>
                {field?.description ? (
                  <span id={`${base}-help`} className="mn-column-mapping__help">
                    {field.description}
                  </span>
                ) : null}
                {dup && field ? (
                  <span id={`${base}-error`} className="mn-column-mapping__error">
                    {m.duplicate(field.label, others.join(', '))}
                  </span>
                ) : null}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
});
