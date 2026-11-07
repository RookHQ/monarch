'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import { Progress } from '../../../components/data-display/progress-bar/index.js';
import { FileUpload, formatBytes } from '../../../components/files/file-upload/index.js';
import {
  Field,
  FieldDescription,
  FieldLabel,
  NativeSelect,
} from '../../../components/forms/form-field/index.js';
import { Stepper } from '../../../components/workflow/stepper/index.js';
import {
  PreviewRows,
  countPreviewRows,
  previewRowStatus,
  type PreviewRow,
  type PreviewRowIssue,
  type PreviewRowStatus,
  type PreviewRowsMessages,
} from '../../../components/data-grid/preview-rows/index.js';
import { normalizeHeader, parseCsv, toCsv, type ImportTable } from './import-wizard.csv.js';

export { parseCsv, normalizeHeader, type ImportTable } from './import-wizard.csv.js';

/** A field the product can import into. */
export interface ImportField {
  /** Stable id, used as the key of `ImportRecord.values`. */
  id: string;
  /** Label shown in the mapping step and the preview. */
  label: string;
  /** Rows without a value for this field are errors; the field must be mapped. */
  required?: boolean;
  /** Help under the mapping control, e.g. the expected format. */
  description?: string;
  /** Other header names that should map to this field automatically ("E-mail", "Mail"). */
  aliases?: readonly string[];
}

/** Field id → the file header it reads from. Missing or empty means "do not import". */
export type ImportMapping = Record<string, string | undefined>;

/** One data row, keyed by field id, with its 1-based row number. */
export interface ImportRecord {
  row: number;
  values: Record<string, string>;
}

/** The product's verdict on one record, from `validate`. */
export interface ImportRowCheck {
  /** Overrides the status worked out from the problems, e.g. `duplicate`. */
  status?: PreviewRowStatus | undefined;
  errors?: readonly (PreviewRowIssue | string)[] | undefined;
  warnings?: readonly (PreviewRowIssue | string)[] | undefined;
  /** Values to show in the preview instead of the raw text (cleaned, looked up). */
  values?: Readonly<Record<string, ReactNode>> | undefined;
  /** Extra content in the issues cell, e.g. a link to the matching record. */
  note?: ReactNode;
}

/** A row-level problem reported after the import. */
export interface ImportReportError {
  row?: number | null | undefined;
  field?: string | undefined;
  message: string;
}

/** What the import did. */
export interface ImportReport {
  created: number;
  updated?: number;
  skipped?: number;
  failed?: number;
  errors?: readonly ImportReportError[];
}

/** A choice for what to do with rows that match existing records. */
export interface ImportDuplicateRule {
  value: string;
  label: string;
  description?: string;
}

export interface ImportContext {
  fileName: string;
  mapping: ImportMapping;
}

export type ImportWizardStep = 'upload' | 'map' | 'preview' | 'results';

export interface ImportWizardMessages {
  steps: Record<ImportWizardStep, string>;
  stepsLabel: string;
  uploadTitle: string;
  uploadHint: (limits: { maxSize?: string; maxRows?: number }) => string;
  dropLabel: string;
  browse: string;
  template: string;
  reading: string;
  readFailed: string;
  noRows: string;
  noHeaders: string;
  tooManyRows: (max: number) => string;
  mapTitle: string;
  mapHint: (fileName: string, rows: number) => string;
  notImported: string;
  example: (value: string) => string;
  requiredMissing: (fields: string) => string;
  required: string;
  checking: string;
  checkFailed: string;
  previewTitle: string;
  previewHint: string;
  previewCaption: (fileName: string) => string;
  duplicateRule: string;
  importButton: (rows: number) => string;
  nothingToImport: string;
  importing: (rows: number) => string;
  importFailed: string;
  resultsTitle: string;
  resultsHint: string;
  created: string;
  updated: string;
  skipped: string;
  failed: string;
  problems: string;
  rowProblem: (
    row: number | null | undefined,
    field: string | undefined,
    message: string,
  ) => string;
  downloadIssues: string;
  importAnother: string;
  back: string;
  next: string;
  cancel: string;
  fieldRequired: (field: string) => string;
  /** Preview table messages. */
  preview?: Partial<Omit<PreviewRowsMessages, 'status'>> & {
    status?: Partial<PreviewRowsMessages['status']>;
  };
}

export const defaultImportWizardMessages: ImportWizardMessages = {
  steps: { upload: 'Upload', map: 'Map columns', preview: 'Preview', results: 'Results' },
  stepsLabel: 'Import steps',
  uploadTitle: 'Choose a file',
  uploadHint: ({ maxSize, maxRows }) =>
    [
      'A CSV file whose first row holds the column names.',
      maxSize ? `Up to ${maxSize}` : '',
      maxRows
        ? `${maxSize ? 'and' : 'Up to'} ${maxRows.toLocaleString()} rows.`
        : maxSize
          ? '.'
          : '',
    ]
      .filter(Boolean)
      .join(' ')
      .replace(' .', '.'),
  dropLabel: 'Drag and drop a CSV file here',
  browse: 'Choose file',
  template: 'Download a template',
  reading: 'Reading the file…',
  readFailed: 'This file could not be read. Check that it is a CSV file and try again.',
  noRows: 'This file has no data rows under the header row.',
  noHeaders: 'The first row of this file has no column names.',
  tooManyRows: (max) =>
    `This file has more than ${max.toLocaleString()} rows. Split it and import the parts.`,
  mapTitle: 'Match your columns',
  mapHint: (file, rows) =>
    `${file} · ${rows.toLocaleString()} ${rows === 1 ? 'row' : 'rows'}. Choose which column fills each field.`,
  notImported: 'Do not import',
  example: (v) => `Example: ${v}`,
  requiredMissing: (fields) => `Choose a column for ${fields}.`,
  required: 'Required',
  checking: 'Checking the rows…',
  checkFailed: 'The rows could not be checked. Try again.',
  previewTitle: 'Check the rows',
  previewHint: 'Nothing has been saved yet. Rows with errors are skipped; the rest are imported.',
  previewCaption: (file) => `Preview of ${file}`,
  duplicateRule: 'Rows that match existing records',
  importButton: (n) => `Import ${n.toLocaleString()} ${n === 1 ? 'row' : 'rows'}`,
  nothingToImport: 'No rows can be imported. Fix the file or the column choices and try again.',
  importing: (n) => `Importing ${n.toLocaleString()} ${n === 1 ? 'row' : 'rows'}…`,
  importFailed:
    'The import did not finish. Nothing is lost: you can try again, and rows already saved are not duplicated if your product supports it.',
  resultsTitle: 'Import finished',
  resultsHint: 'Here is what was saved.',
  created: 'Created',
  updated: 'Updated',
  skipped: 'Skipped',
  failed: 'Failed',
  problems: 'Rows that were not imported',
  rowProblem: (row, field, message) =>
    [row ? `Row ${row}` : null, field, message].filter(Boolean).join(' · '),
  downloadIssues: 'Download problems (CSV)',
  importAnother: 'Import another file',
  back: 'Back',
  next: 'Check rows',
  cancel: 'Cancel',
  fieldRequired: (field) => `${field} is required`,
};

/** Maps each field to the first header whose normalised name equals its id, label or an alias. */
export function suggestMapping(
  fields: readonly ImportField[],
  headers: readonly string[],
): ImportMapping {
  const byName = new Map<string, string>();
  for (const h of headers) {
    const key = normalizeHeader(h);
    if (key && !byName.has(key)) byName.set(key, h);
  }
  const used = new Set<string>();
  const mapping: ImportMapping = {};
  for (const f of fields) {
    for (const name of [f.id, f.label, ...(f.aliases ?? [])]) {
      const header = byName.get(normalizeHeader(name));
      if (header && !used.has(header)) {
        mapping[f.id] = header;
        used.add(header);
        break;
      }
    }
  }
  return mapping;
}

/** Turns table rows into records keyed by field id (values trimmed). */
export function toImportRecords(
  table: ImportTable,
  mapping: ImportMapping,
  fields: readonly ImportField[],
): ImportRecord[] {
  const index = new Map(table.headers.map((h, i) => [h, i]));
  return table.rows.map((cells, i) => {
    const values: Record<string, string> = {};
    for (const f of fields) {
      const header = mapping[f.id];
      const col = header ? index.get(header) : undefined;
      if (col !== undefined) values[f.id] = (cells[col] ?? '').trim();
    }
    return { row: i + 1, values };
  });
}

export interface ImportWizardProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The fields the product imports into, in the order to show them. */
  fields: readonly ImportField[];
  /**
   * Checks the records before anything is saved (a dry run). Return one
   * check per record, in order. Required fields are checked for you first.
   * May call the server.
   */
  validate?:
    | ((
        records: ImportRecord[],
        context: ImportContext,
      ) => readonly ImportRowCheck[] | Promise<readonly ImportRowCheck[]>)
    | undefined;
  /**
   * Saves the importable records (every record that is not an error) and
   * returns what happened. A rejection keeps the wizard on the preview step
   * with an error, so the person can try again.
   */
  onImport: (
    records: ImportRecord[],
    context: ImportContext & { duplicateRule?: string | undefined },
  ) => Promise<ImportReport>;
  /** Choices for rows that match existing records, shown on the preview step. */
  duplicateRules?: readonly ImportDuplicateRule[] | undefined;
  defaultDuplicateRule?: string | undefined;
  /** Accepted files, as for `<input accept>`. Default `.csv,text/csv`. */
  accept?: string;
  /** Largest file in bytes. */
  maxSize?: number | undefined;
  /** Most data rows a file may have. */
  maxRows?: number | undefined;
  /** Reads a file into headers and rows. Default: `file.text()` and `parseCsv`. Use it for spreadsheets. */
  parseFile?: ((file: File) => Promise<ImportTable>) | undefined;
  /** Link to a template file with the expected columns. */
  templateHref?: string | undefined;
  /** Field ids to show as preview columns. Default: every mapped field, up to four. */
  previewFields?: readonly string[] | undefined;
  /** Rows per page in the preview. Default 50. */
  previewPageSize?: number;
  /** Called after a successful import, with the report. */
  onComplete?: ((report: ImportReport) => void) | undefined;
  /** Adds a Cancel button to every step before the results. */
  onCancel?: (() => void) | undefined;
  /** Extra actions on the results step, e.g. a link to the imported records. */
  resultActions?: ReactNode;
  /** Heading level of each step's title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4';
  messages?: Partial<Omit<ImportWizardMessages, 'steps'>> & {
    steps?: Partial<ImportWizardMessages['steps']>;
  };
}

const STEPS: ImportWizardStep[] = ['upload', 'map', 'preview', 'results'];

/** `file.text()`, with a FileReader fallback for older browsers. */
function readText(file: File): Promise<string> {
  if (typeof file.text === 'function') return file.text();
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ''));
    reader.onerror = () => reject(reader.error ?? new Error('read failed'));
    reader.readAsText(file);
  });
}

function errorText(err: unknown, fallback: string): string {
  return err instanceof Error && err.message ? err.message : fallback;
}

function download(name: string, text: string) {
  if (typeof document === 'undefined' || typeof URL.createObjectURL !== 'function') return;
  const url = URL.createObjectURL(new Blob([text], { type: 'text/csv' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

/**
 * Imports records from a CSV file in four steps: upload, map columns to
 * fields (matched automatically where the names agree), preview every row
 * with its problems before anything is saved, then import and show a report
 * of what was created, updated, skipped and failed. Parsing is built in;
 * validation and saving are the product's callbacks.
 */
export const ImportWizard = forwardRef<HTMLDivElement, ImportWizardProps>(function ImportWizard(
  {
    fields,
    validate,
    onImport,
    duplicateRules,
    defaultDuplicateRule,
    accept = '.csv,text/csv',
    maxSize,
    maxRows,
    parseFile,
    templateHref,
    previewFields,
    previewPageSize = 50,
    onComplete,
    onCancel,
    resultActions,
    headingLevel = 'h2',
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m: ImportWizardMessages = {
    ...defaultImportWizardMessages,
    ...messages,
    steps: { ...defaultImportWizardMessages.steps, ...messages?.steps },
  };
  const id = useId();
  const [step, setStep] = useState<ImportWizardStep>('upload');
  const [furthest, setFurthest] = useState(0);
  const [fileName, setFileName] = useState('');
  const [table, setTable] = useState<ImportTable | null>(null);
  const [mapping, setMapping] = useState<ImportMapping>({});
  const [checks, setChecks] = useState<{ records: ImportRecord[]; rows: PreviewRow[] } | null>(
    null,
  );
  const [rule, setRule] = useState<string | undefined>(
    defaultDuplicateRule ?? duplicateRules?.[0]?.value,
  );
  const [busy, setBusy] = useState<'reading' | 'checking' | 'importing' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [mapTouched, setMapTouched] = useState(false);
  const [report, setReport] = useState<ImportReport | null>(null);
  const headingRef = useRef<HTMLHeadingElement | null>(null);
  const moved = useRef(false);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // Each new step moves focus to its heading, so keyboard and screen reader users start there.
  useEffect(() => {
    if (!moved.current) return;
    moved.current = false;
    headingRef.current?.focus();
  }, [step]);

  const go = (next: ImportWizardStep) => {
    moved.current = true;
    setError(null);
    setStep(next);
    setFurthest((f) => Math.max(f, STEPS.indexOf(next)));
  };

  const reset = () => {
    setTable(null);
    setFileName('');
    setMapping({});
    setChecks(null);
    setReport(null);
    setMapTouched(false);
    setFurthest(0);
    go('upload');
  };

  const readFile = async (file: File) => {
    setError(null);
    setBusy('reading');
    try {
      const parsed = parseFile ? await parseFile(file) : parseCsv(await readText(file));
      if (!mounted.current) return;
      if (!parsed.headers.some((h) => h.trim())) throw new Error(m.noHeaders);
      if (!parsed.rows.length) throw new Error(m.noRows);
      if (maxRows !== undefined && parsed.rows.length > maxRows)
        throw new Error(m.tooManyRows(maxRows));
      setTable(parsed);
      setFileName(file.name);
      setMapping(suggestMapping(fields, parsed.headers));
      setChecks(null);
      setMapTouched(false);
      setBusy(null);
      go('map');
    } catch (err) {
      if (!mounted.current) return;
      setBusy(null);
      setError(errorText(err, m.readFailed));
    }
  };

  const missing = fields.filter((f) => f.required && !mapping[f.id]);
  const context: ImportContext = { fileName, mapping };

  const runCheck = async () => {
    setMapTouched(true);
    if (!table || missing.length) return;
    setError(null);
    setBusy('checking');
    const records = toImportRecords(table, mapping, fields);
    try {
      const custom = validate ? await validate(records, context) : [];
      if (!mounted.current) return;
      const rows: PreviewRow[] = records.map((r, i) => {
        const c = custom[i];
        const requiredErrors: PreviewRowIssue[] = fields
          .filter((f) => f.required && !r.values[f.id])
          .map((f) => ({ field: f.label, message: m.fieldRequired(f.label) }));
        const errors = [...requiredErrors, ...(c?.errors ?? [])];
        return {
          row: r.row,
          values: { ...r.values, ...c?.values },
          ...(c?.status && !requiredErrors.length ? { status: c.status } : {}),
          ...(errors.length ? { errors } : {}),
          ...(c?.warnings?.length ? { warnings: c.warnings } : {}),
          ...(c?.note ? { note: c.note } : {}),
        };
      });
      setChecks({ records, rows });
      setBusy(null);
      go('preview');
    } catch (err) {
      if (!mounted.current) return;
      setBusy(null);
      setError(errorText(err, m.checkFailed));
    }
  };

  const importable = useMemo(() => {
    if (!checks) return [];
    return checks.records.filter(
      (_, i) => previewRowStatus(checks.rows[i] as PreviewRow) !== 'error',
    );
  }, [checks]);
  const counts = useMemo(() => (checks ? countPreviewRows(checks.rows) : null), [checks]);

  const runImport = async () => {
    if (!checks || !importable.length) return;
    setError(null);
    setBusy('importing');
    try {
      const result = await onImport(importable, { ...context, duplicateRule: rule });
      if (!mounted.current) return;
      setReport(result);
      setBusy(null);
      go('results');
      onComplete?.(result);
    } catch (err) {
      if (!mounted.current) return;
      setBusy(null);
      setError(errorText(err, m.importFailed));
    }
  };

  const previewIssues = (checks?.rows ?? []).filter((r) => r.errors?.length || r.warnings?.length);
  const issuesCsv = (
    list: { row?: number | null | undefined; field?: string | undefined; message: string }[],
  ) =>
    toCsv([
      ['Row', 'Field', 'Problem'],
      ...list.map((e) => [e.row ?? '', e.field ?? '', e.message]),
    ]);
  const flatIssues = previewIssues.flatMap((r) =>
    [...(r.errors ?? []), ...(r.warnings ?? [])].map((i) =>
      typeof i === 'string'
        ? { row: r.row, message: i }
        : { row: r.row, field: i.field, message: i.message },
    ),
  );

  const Heading = headingLevel;
  const SubHeading = headingLevel === 'h2' ? 'h3' : headingLevel === 'h3' ? 'h4' : 'h5';
  const heading = (text: string, hint?: ReactNode) => (
    <div className="mn-import-wizard__head">
      <Heading
        ref={headingRef}
        tabIndex={-1}
        className="mn-import-wizard__title"
        id={`${id}-title`}
      >
        {text}
      </Heading>
      {hint ? <p className="mn-import-wizard__hint">{hint}</p> : null}
    </div>
  );
  const sample = (header: string | undefined) => {
    if (!header || !table) return undefined;
    const col = table.headers.indexOf(header);
    return table.rows.find((r) => (r[col] ?? '').trim())?.[col]?.trim();
  };
  const cancel = onCancel ? (
    <Button variant="ghost" onClick={onCancel} disabled={busy === 'importing'}>
      {m.cancel}
    </Button>
  ) : null;

  const shownFields =
    previewFields ??
    fields
      .filter((f) => mapping[f.id])
      .slice(0, 4)
      .map((f) => f.id);
  const previewColumns = shownFields
    .map((fid) => fields.find((f) => f.id === fid))
    .filter((f): f is ImportField => Boolean(f))
    .map((f) => ({ id: f.id, label: f.label }));

  return (
    <div
      ref={ref}
      {...rest}
      data-step={step}
      aria-busy={busy ? true : undefined}
      className={cx('mn-import-wizard', className)}
    >
      <Stepper
        aria-label={m.stepsLabel}
        className="mn-import-wizard__steps"
        steps={STEPS.map((s) => ({ id: s, label: m.steps[s] }))}
        current={step}
        furthest={STEPS[furthest] as string}
        compact="auto"
      />

      {error ? (
        <Alert tone="danger" live="assertive" className="mn-import-wizard__error">
          {error}
        </Alert>
      ) : null}

      {step === 'upload' ? (
        <section className="mn-import-wizard__step" aria-labelledby={`${id}-title`}>
          {heading(
            m.uploadTitle,
            m.uploadHint({
              ...(maxSize !== undefined ? { maxSize: formatBytes(maxSize) } : {}),
              ...(maxRows !== undefined ? { maxRows } : {}),
            }),
          )}
          <FileUpload
            accept={accept}
            maxFiles={1}
            multiple={false}
            {...(maxSize !== undefined ? { maxSize } : {})}
            disabled={busy === 'reading'}
            messages={{ label: m.dropLabel, browse: m.browse }}
            onFiles={(accepted) => {
              const file = accepted[0];
              if (file) void readFile(file);
            }}
          />
          {busy === 'reading' ? (
            <p className="mn-import-wizard__busy" role="status">
              {m.reading}
            </p>
          ) : null}
          <div className="mn-import-wizard__actions">
            {templateHref ? (
              <a className="mn-import-wizard__link" href={templateHref} download>
                {m.template}
              </a>
            ) : (
              <span />
            )}
            {cancel}
          </div>
        </section>
      ) : null}

      {step === 'map' && table ? (
        <section className="mn-import-wizard__step" aria-labelledby={`${id}-title`}>
          {heading(m.mapTitle, m.mapHint(fileName, table.rows.length))}
          <div className="mn-import-wizard__mapping">
            {fields.map((f) => {
              const example = sample(mapping[f.id]);
              const invalid = mapTouched && f.required && !mapping[f.id];
              return (
                <Field key={f.id} required={f.required === true} invalid={invalid === true}>
                  <FieldLabel>
                    {f.label}
                    {f.required ? (
                      <span className="mn-visually-hidden"> ({m.required})</span>
                    ) : null}
                  </FieldLabel>
                  <NativeSelect
                    value={mapping[f.id] ?? ''}
                    onChange={(e) => {
                      const v = e.target.value;
                      setMapping((prev) => ({ ...prev, [f.id]: v || undefined }));
                    }}
                    options={[
                      { value: '', label: m.notImported },
                      ...table.headers.map((h, i) => ({ value: h, label: h || `#${i + 1}` })),
                    ]}
                  />
                  {f.description || example ? (
                    <FieldDescription>
                      {[f.description, example ? m.example(example) : null]
                        .filter(Boolean)
                        .join(' ')}
                    </FieldDescription>
                  ) : null}
                </Field>
              );
            })}
          </div>
          {mapTouched && missing.length ? (
            <p className="mn-import-wizard__invalid" role="alert">
              {m.requiredMissing(missing.map((f) => f.label).join(', '))}
            </p>
          ) : null}
          {busy === 'checking' ? (
            <p className="mn-import-wizard__busy" role="status">
              {m.checking}
            </p>
          ) : null}
          <div className="mn-import-wizard__actions">
            <Button variant="ghost" onClick={() => go('upload')}>
              {m.back}
            </Button>
            <span className="mn-import-wizard__spacer" />
            {cancel}
            <Button loading={busy === 'checking'} onClick={() => void runCheck()}>
              {m.next}
            </Button>
          </div>
        </section>
      ) : null}

      {step === 'preview' && checks && counts ? (
        <section className="mn-import-wizard__step" aria-labelledby={`${id}-title`}>
          {heading(m.previewTitle, m.previewHint)}
          <PreviewRows
            caption={m.previewCaption(fileName)}
            rows={checks.rows}
            columns={previewColumns}
            pageSize={previewPageSize}
            {...(m.preview ? { messages: m.preview } : {})}
          />
          {flatIssues.length ? (
            <div>
              <Button
                size="sm"
                variant="secondary"
                onClick={() => download('import-problems.csv', issuesCsv(flatIssues))}
              >
                {m.downloadIssues}
              </Button>
            </div>
          ) : null}
          {duplicateRules?.length ? (
            <fieldset className="mn-import-wizard__rules">
              <legend className="mn-import-wizard__legend">{m.duplicateRule}</legend>
              {duplicateRules.map((r, i) => (
                <label
                  key={r.value}
                  className="mn-import-wizard__rule"
                  data-checked={rule === r.value || undefined}
                >
                  <input
                    type="radio"
                    name={`${id}-rule`}
                    value={r.value}
                    checked={rule === r.value}
                    onChange={() => setRule(r.value)}
                    aria-labelledby={`${id}-rule-${i}-label`}
                    aria-describedby={r.description ? `${id}-rule-${i}` : undefined}
                  />
                  <span className="mn-import-wizard__rule-text">
                    <span className="mn-import-wizard__rule-label" id={`${id}-rule-${i}-label`}>
                      {r.label}
                    </span>
                    {r.description ? (
                      <span className="mn-import-wizard__rule-description" id={`${id}-rule-${i}`}>
                        {r.description}
                      </span>
                    ) : null}
                  </span>
                </label>
              ))}
            </fieldset>
          ) : null}
          {!importable.length ? <Alert tone="warning">{m.nothingToImport}</Alert> : null}
          {busy === 'importing' ? (
            <div className="mn-import-wizard__progress">
              <Progress label={m.importing(importable.length)} />
              <p className="mn-import-wizard__busy" role="status">
                {m.importing(importable.length)}
              </p>
            </div>
          ) : null}
          <div className="mn-import-wizard__actions">
            <Button variant="ghost" onClick={() => go('map')} disabled={busy === 'importing'}>
              {m.back}
            </Button>
            <span className="mn-import-wizard__spacer" />
            {cancel}
            <Button
              loading={busy === 'importing'}
              disabled={!importable.length}
              onClick={() => void runImport()}
            >
              {m.importButton(importable.length)}
            </Button>
          </div>
        </section>
      ) : null}

      {step === 'results' && report ? (
        <section className="mn-import-wizard__step" aria-labelledby={`${id}-title`}>
          {heading(m.resultsTitle, m.resultsHint)}
          <dl className="mn-import-wizard__totals">
            {(
              [
                ['created', report.created],
                ['updated', report.updated],
                ['skipped', report.skipped],
                ['failed', report.failed],
              ] as const
            )
              .filter(([key, n]) => key === 'created' || n !== undefined)
              .map(([key, n]) => (
                <div
                  key={key}
                  className="mn-import-wizard__total"
                  data-kind={key}
                  data-nonzero={n ? '' : undefined}
                >
                  <dt>{m[key]}</dt>
                  <dd>{(n ?? 0).toLocaleString()}</dd>
                </div>
              ))}
          </dl>
          {report.errors?.length ? (
            <div className="mn-import-wizard__problems">
              <SubHeading className="mn-import-wizard__subtitle">{m.problems}</SubHeading>
              <ul className="mn-import-wizard__problem-list">
                {report.errors.slice(0, 200).map((e, i) => (
                  <li key={i}>{m.rowProblem(e.row, e.field, e.message)}</li>
                ))}
              </ul>
              <div>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() =>
                    download('import-problems.csv', issuesCsv(report.errors as ImportReportError[]))
                  }
                >
                  {m.downloadIssues}
                </Button>
              </div>
            </div>
          ) : null}
          <div className="mn-import-wizard__actions">
            {resultActions}
            <Button variant="secondary" onClick={reset}>
              {m.importAnother}
            </Button>
          </div>
        </section>
      ) : null}
    </div>
  );
});
