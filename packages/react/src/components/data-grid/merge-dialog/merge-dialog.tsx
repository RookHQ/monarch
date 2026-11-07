'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogTrigger,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../overlays/dialog/index.js';

/** One of the records being merged. */
export interface MergeRecord {
  id: string;
  /** Name of the record, e.g. the company name. */
  title: string;
  /** Secondary line, e.g. "Created 3 Mar 2025 · 12 invoices". */
  description?: ReactNode;
  /** Field values by field id. `null`, `undefined` and `''` count as empty. */
  values: Readonly<Record<string, ReactNode>>;
}

/** A field whose surviving value the person picks. */
export interface MergeField {
  id: string;
  label: string;
}

/** What the person chose. */
export interface MergeSelection {
  /** The record that survives. */
  primaryId: string;
  /** The other records, merged into the primary one. */
  mergedIds: string[];
  /** For each field, the id of the record whose value is kept. */
  fields: Record<string, string>;
}

export interface MergeDialogMessages {
  title: string;
  description: string;
  /** Legend of the "record to keep" choice. */
  keep: string;
  /** Tag on the record that is kept. */
  kept: string;
  /** Heading of the field choices. */
  values: string;
  /** Hidden suffix of each value option: "from Acme Ltd". */
  from: (record: string) => string;
  /** Shown instead of options when every record has the same value. */
  same: string;
  empty: string;
  /** Heading of the consequences list. */
  consequences: string;
  confirm: (count: number) => string;
  cancel: string;
  /** Fallback error text when `onMerge` rejects without a message. */
  error: string;
}

export const defaultMergeDialogMessages: MergeDialogMessages = {
  title: 'Merge records',
  description:
    'Choose the record to keep and which value to keep for each field. The other records are merged into it.',
  keep: 'Record to keep',
  kept: 'Kept',
  values: 'Values to keep',
  from: (record) => `from ${record}`,
  same: 'Same in every record',
  empty: 'Empty',
  consequences: 'What happens',
  confirm: (n) => `Merge ${n} records`,
  cancel: 'Cancel',
  error: 'The records could not be merged. Nothing was changed.',
};

const isEmpty = (v: ReactNode) => v === null || v === undefined || v === '' || v === false;
const keyOf = (v: ReactNode): string | null =>
  isEmpty(v) ? '' : typeof v === 'string' || typeof v === 'number' ? String(v).trim() : null;

/**
 * The value to keep by default: the primary record's, or the first other
 * record's when the primary one is empty (blank details are filled in).
 */
export function defaultMergeChoice(
  field: string,
  records: readonly MergeRecord[],
  primaryId: string,
): string {
  const primary = records.find((r) => r.id === primaryId);
  if (primary && !isEmpty(primary.values[field])) return primaryId;
  return records.find((r) => r.id !== primaryId && !isEmpty(r.values[field]))?.id ?? primaryId;
}

export interface MergeDialogProps {
  open?: boolean;
  /** An element that opens the dialog (rendered as the DialogTrigger). */
  trigger?: ReactElement;
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The records to merge, two or more. */
  records: readonly MergeRecord[];
  /** Fields whose values can differ between the records, in display order. */
  fields: readonly MergeField[];
  /** The record kept by default. Default: the first. */
  defaultPrimaryId?: string | undefined;
  /**
   * Performs the merge. Return a promise to keep the dialog open and busy;
   * a rejection shows its message and leaves everything as chosen.
   */
  onMerge: (selection: MergeSelection) => unknown;
  /** What else happens, e.g. "12 invoices move to the kept record", "The other record is archived". */
  consequences?: readonly ReactNode[] | undefined;
  /** Problems that block the merge (shown as an alert; the Merge button is disabled). */
  blockers?: readonly ReactNode[] | undefined;
  /** Extra content under the field choices. */
  children?: ReactNode;
  /** Dialog width. Default `lg`. */
  size?: 'md' | 'lg' | 'xl';
  messages?: Partial<MergeDialogMessages>;
  className?: string;
}

/**
 * Merges two or more duplicate records into one: pick the record to keep,
 * then the value to keep for every field where they differ. Fields that
 * already match are listed once. The merge runs through `onMerge`, with
 * pending and error states.
 */
export const MergeDialog = forwardRef<HTMLDivElement, MergeDialogProps>(function MergeDialog(
  {
    open: openProp,
    trigger,
    defaultOpen = false,
    onOpenChange,
    records,
    fields,
    defaultPrimaryId,
    onMerge,
    consequences,
    blockers,
    children,
    size = 'lg',
    messages,
    className,
  },
  ref,
) {
  const m = { ...defaultMergeDialogMessages, ...messages };
  const id = useId();
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const firstId = records[0]?.id ?? '';
  const [primaryId, setPrimaryId] = useState(defaultPrimaryId ?? firstId);
  const [picked, setPicked] = useState<Record<string, string>>({});
  const [pending, setPending] = useState(false);
  const [failure, setFailure] = useState<ReactNode>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);
  const returnFocus = useRef<HTMLElement | null>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // Every opening starts from the defaults.
  useEffect(() => {
    if (open) {
      setPrimaryId(defaultPrimaryId ?? firstId);
      setPicked({});
      setFailure(null);
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const primary = records.find((r) => r.id === primaryId) ?? records[0];
  const { differing, same } = useMemo(() => {
    const differing: MergeField[] = [];
    const same: MergeField[] = [];
    for (const f of fields) {
      const keys = records.map((r) => keyOf(r.values[f.id]));
      const allEqual = keys.every((k) => k !== null && k === keys[0]);
      (allEqual ? same : differing).push(f);
    }
    return { differing, same };
  }, [fields, records]);

  const choice = (field: string) => picked[field] ?? defaultMergeChoice(field, records, primaryId);
  const setOpen = (next: boolean) => {
    if (!next && pending) return;
    if (openProp === undefined) setOpenState(next);
    onOpenChange?.(next);
  };
  const blocked = Boolean(blockers?.length) || records.length < 2;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (pending || blocked || !primary) return;
    setFailure(null);
    const selection: MergeSelection = {
      primaryId: primary.id,
      mergedIds: records.filter((r) => r.id !== primary.id).map((r) => r.id),
      fields: Object.fromEntries(fields.map((f) => [f.id, choice(f.id)])),
    };
    try {
      const result = onMerge(selection);
      if (result && typeof (result as Promise<unknown>).then === 'function') {
        setPending(true);
        await result;
      }
    } catch (error) {
      if (mounted.current) {
        setPending(false);
        setFailure(error instanceof Error && error.message ? error.message : m.error);
      }
      return;
    }
    if (!mounted.current) return;
    setPending(false);
    if (openProp === undefined) setOpenState(false);
    onOpenChange?.(false);
  };

  const errorId = `${id}-error`;
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
      <DialogContent
        ref={ref}
        role="alertdialog"
        size={size}
        busy={pending}
        closeOnOutsideClick={false}
        fullScreenOnPhone
        className={cx('mn-merge-dialog', className)}
        onOpenAutoFocus={(event) => {
          event.preventDefault();
          // Remember where focus was, so it can go back there without a DialogTrigger.
          const active = typeof document === 'undefined' ? null : document.activeElement;
          if (active instanceof HTMLElement && active !== document.body)
            returnFocus.current = active;
          cancelRef.current?.focus();
        }}
        onCloseAutoFocus={(event) => {
          const target = returnFocus.current;
          returnFocus.current = null;
          if (target?.isConnected) {
            event.preventDefault();
            target.focus();
          }
        }}
      >
        <form className="mn-merge-dialog__form" onSubmit={submit} noValidate>
          <DialogHeader>
            <DialogTitle>{m.title}</DialogTitle>
            <DialogDescription>{m.description}</DialogDescription>
          </DialogHeader>
          <DialogBody className="mn-merge-dialog__body">
            <fieldset className="mn-merge-dialog__group" disabled={pending}>
              <legend className="mn-merge-dialog__legend">{m.keep}</legend>
              <div className="mn-merge-dialog__records">
                {records.map((r, i) => (
                  <label
                    key={r.id}
                    className="mn-merge-dialog__record"
                    data-checked={r.id === primary?.id || undefined}
                  >
                    <input
                      type="radio"
                      name={`${id}-primary`}
                      value={r.id}
                      checked={r.id === primary?.id}
                      onChange={() => setPrimaryId(r.id)}
                      aria-labelledby={`${id}-rec-${i}-title`}
                      aria-describedby={r.description ? `${id}-rec-${i}` : undefined}
                    />
                    <span className="mn-merge-dialog__record-text">
                      <span className="mn-merge-dialog__record-title" id={`${id}-rec-${i}-title`}>
                        {r.title}
                      </span>
                      {r.description ? (
                        <span className="mn-merge-dialog__record-description" id={`${id}-rec-${i}`}>
                          {r.description}
                        </span>
                      ) : null}
                    </span>
                    {r.id === primary?.id ? (
                      <span className="mn-merge-dialog__kept" aria-hidden="true">
                        {m.kept}
                      </span>
                    ) : null}
                  </label>
                ))}
              </div>
            </fieldset>

            {differing.length ? (
              <div className="mn-merge-dialog__fields">
                <h3 className="mn-merge-dialog__heading">{m.values}</h3>
                {differing.map((f) => (
                  <fieldset key={f.id} className="mn-merge-dialog__field" disabled={pending}>
                    <legend className="mn-merge-dialog__field-label">{f.label}</legend>
                    <div className="mn-merge-dialog__options">
                      {records.map((r) => {
                        const value = r.values[f.id];
                        const empty = isEmpty(value);
                        return (
                          <label
                            key={r.id}
                            className="mn-merge-dialog__option"
                            data-checked={choice(f.id) === r.id || undefined}
                            data-empty={empty || undefined}
                          >
                            <input
                              type="radio"
                              name={`${id}-field-${f.id}`}
                              value={r.id}
                              checked={choice(f.id) === r.id}
                              onChange={() => setPicked((p) => ({ ...p, [f.id]: r.id }))}
                            />
                            <span className="mn-merge-dialog__value">
                              {empty ? m.empty : value}
                              <span className="mn-merge-dialog__source">
                                <span className="mn-visually-hidden"> </span>
                                {m.from(r.title)}
                              </span>
                            </span>
                          </label>
                        );
                      })}
                    </div>
                  </fieldset>
                ))}
              </div>
            ) : null}

            {same.length && primary ? (
              <div className="mn-merge-dialog__same">
                <h3 className="mn-merge-dialog__heading">{m.same}</h3>
                <dl className="mn-merge-dialog__same-list">
                  {same.map((f) => (
                    <div key={f.id} className="mn-merge-dialog__same-row">
                      <dt>{f.label}</dt>
                      <dd>{isEmpty(primary.values[f.id]) ? m.empty : primary.values[f.id]}</dd>
                    </div>
                  ))}
                </dl>
              </div>
            ) : null}

            {children}

            {consequences?.length ? (
              <div className="mn-merge-dialog__consequences">
                <h3 className="mn-merge-dialog__heading">{m.consequences}</h3>
                <ul className="mn-merge-dialog__consequence-list">
                  {consequences.map((c, i) => (
                    <li key={i}>{c}</li>
                  ))}
                </ul>
              </div>
            ) : null}

            {blockers?.length ? (
              <Alert tone="warning">
                <ul className="mn-merge-dialog__blockers">
                  {blockers.map((b, i) => (
                    <li key={i}>{b}</li>
                  ))}
                </ul>
              </Alert>
            ) : null}
          </DialogBody>
          {failure ? (
            <div className="mn-merge-dialog__error">
              <Alert tone="danger" live="assertive" id={errorId}>
                {failure}
              </Alert>
            </div>
          ) : null}
          <DialogFooter>
            <DialogClose asChild>
              <Button ref={cancelRef} disabled={pending}>
                {m.cancel}
              </Button>
            </DialogClose>
            <Button
              type="submit"
              variant="danger"
              loading={pending}
              disabled={blocked}
              aria-describedby={failure ? errorId : undefined}
            >
              {m.confirm(records.length)}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
});
