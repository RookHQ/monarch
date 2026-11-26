'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type FocusEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { Spinner } from '../../feedback/spinner/index.js';

// useIsomorphicLayoutEffect warns during server rendering; focus work only runs in the browser.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

/** The kind of editor a cell opens. */
export type EditableCellType = 'text' | 'number' | 'select' | 'date';

/**
 * A cell value. Text and select cells hold strings, number cells numbers,
 * date cells ISO dates (`YYYY-MM-DD`). `null` is an empty cell.
 */
export type EditableCellValue = string | number | null;

/** One choice of a `select` cell. */
export interface EditableCellOption {
  value: string;
  label: string;
  disabled?: boolean | undefined;
}

/** Why editing ended. */
export type EditableCellExitReason = 'commit' | 'cancel' | 'blur';

export interface EditableCellMessages {
  /** Visually hidden hint after the view button's text: "Edit". */
  edit: string;
  /** Accessible name of the pending spinner. */
  saving: string;
  /** The draft of a number cell is not a number. */
  invalidNumber: string;
  /** A `required` cell is empty. */
  required: string;
  /** A date cell holds something that is not a date. */
  invalidDate: string;
  /** The value is below `min`. */
  belowMin: (min: string) => string;
  /** The value is above `max`. */
  aboveMax: (max: string) => string;
  /** Option of a select cell that clears it (only when not `required`). */
  noValue: string;
  /** A rejected `onCommit` without an Error message. */
  saveFailed: string;
}

export const defaultEditableCellMessages: EditableCellMessages = {
  edit: 'Edit',
  saving: 'Saving',
  invalidNumber: 'Enter a number.',
  required: 'Enter a value.',
  invalidDate: 'Enter a valid date.',
  belowMin: (min) => `Enter ${min} or more.`,
  aboveMax: (max) => `Enter ${max} or less.`,
  noValue: 'None',
  saveFailed: 'Couldn’t save. Try again.',
};

export interface EditableCellProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children' | 'onError'
> {
  /** The saved value. */
  value: EditableCellValue;
  /** The editor: `text` (default), `number`, `select` or `date`. */
  type?: EditableCellType | undefined;
  /** What the cell holds, e.g. "Status for Acme": the editor's accessible name. */
  label: string;
  /** Choices of a `select` cell. */
  options?: readonly EditableCellOption[] | undefined;
  /**
   * Saves a new value. Return a promise to show the pending state; reject it
   * to keep the editor open with the error and the draft. Not called when
   * the value is unchanged.
   */
  onCommit: (next: EditableCellValue) => void | Promise<void>;
  /** Called when editing is cancelled with Escape. */
  onCancel?: (() => void) | undefined;
  /** Checks the parsed value before saving. Return a message to block the save. */
  validate?: ((next: EditableCellValue) => ReactNode | null | undefined) | undefined;
  /** An empty value is not allowed. */
  required?: boolean | undefined;
  /** How the value reads in view mode. Default: the option label, the number or the text. */
  format?: ((value: EditableCellValue) => ReactNode) | undefined;
  /** Placeholder of text and number editors, and the view text of an empty cell. */
  placeholder?: string | undefined;
  /** Lowest value of a number cell, or earliest date (`YYYY-MM-DD`) of a date cell. */
  min?: number | string | undefined;
  /** Highest value of a number cell, or latest date of a date cell. */
  max?: number | string | undefined;
  /** Edit mode (controlled). */
  editing?: boolean | undefined;
  /** Initial edit mode (uncontrolled). */
  defaultEditing?: boolean | undefined;
  /** Called when editing starts or ends, with the reason it ended. */
  onEditingChange?: ((editing: boolean, reason?: EditableCellExitReason) => void) | undefined;
  /**
   * Text that replaces the value when the editor opens, for "start typing to
   * edit" in a grid. Applies to text and number cells.
   */
  initialDraft?: string | undefined;
  /**
   * `button` (default): view mode is a button that opens the editor with a
   * click, Enter or F2. `none`: view mode is plain text and a parent (such
   * as DataGrid) opens the editor through `editing`.
   */
  trigger?: 'button' | 'none' | undefined;
  /** Save when focus leaves the editor. Default true. */
  saveOnBlur?: boolean | undefined;
  /** Not editable. */
  disabled?: boolean | undefined;
  /** Right-align and use tabular figures. Default: true for number cells. */
  numeric?: boolean | undefined;
  messages?: Partial<EditableCellMessages> | undefined;
}

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

function toDraft(value: EditableCellValue): string {
  return value === null || value === undefined ? '' : String(value);
}

/** Turns a draft into a value of the cell's type, or an error key. */
export function parseEditableCellDraft(
  type: EditableCellType,
  draft: string,
): { value: EditableCellValue } | { error: 'invalidNumber' | 'invalidDate' } {
  const text = draft.trim();
  if (text === '') return { value: null };
  if (type === 'number') {
    const n = Number(text.replace(/\s/g, ''));
    return Number.isFinite(n) ? { value: n } : { error: 'invalidNumber' };
  }
  if (type === 'date') {
    if (!ISO_DATE.test(text)) return { error: 'invalidDate' };
    const d = new Date(`${text}T00:00:00Z`);
    return Number.isNaN(d.getTime()) || d.toISOString().slice(0, 10) !== text
      ? { error: 'invalidDate' }
      : { value: text };
  }
  return { value: type === 'select' ? draft : text };
}

function present(node: ReactNode): boolean {
  return node !== null && node !== undefined && node !== false && node !== '';
}

/**
 * A table cell that edits in place: text, number, select or date. Enter or
 * F2 opens the editor, Enter saves, Escape cancels, and leaving the cell
 * saves. Validation and a failed save keep the editor open with the message.
 * Works on its own (view mode is a button) or inside DataGrid, which owns
 * focus and opens the editor (`trigger="none"`).
 */
export const EditableCell = forwardRef<HTMLDivElement, EditableCellProps>(function EditableCell(
  {
    value,
    type = 'text',
    label,
    options = [],
    onCommit,
    onCancel,
    validate,
    required = false,
    format,
    placeholder,
    min,
    max,
    editing: editingProp,
    defaultEditing = false,
    onEditingChange,
    initialDraft,
    trigger = 'button',
    saveOnBlur = true,
    disabled = false,
    numeric = type === 'number',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultEditableCellMessages, ...messagesProp };
  const errorId = `${useId()}-error`;
  const [innerEditing, setInnerEditing] = useState(defaultEditing && !disabled);
  const editing = (editingProp ?? innerEditing) && !disabled;
  const [draft, setDraft] = useState(() => initialDraft ?? toDraft(value));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<ReactNode>(null);
  const inputRef = useRef<HTMLInputElement & HTMLSelectElement>(null);
  const viewRef = useRef<HTMLButtonElement>(null);
  const restoreFocus = useRef(false);
  const failedDraft = useRef<string | null>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const setEditing = useCallback(
    (next: boolean, reason?: EditableCellExitReason) => {
      if (editingProp === undefined) setInnerEditing(next);
      onEditingChange?.(next, reason);
    },
    [editingProp, onEditingChange],
  );

  // Opening starts the draft from the value (or the typed character) and focuses the editor.
  const valueRef = useRef(value);
  valueRef.current = value;
  const initialRef = useRef(initialDraft);
  initialRef.current = initialDraft;
  const wasEditing = useRef(false);
  useIsoLayoutEffect(() => {
    if (editing && !wasEditing.current) {
      const typed = type === 'text' || type === 'number' ? initialRef.current : undefined;
      setDraft(typed ?? toDraft(valueRef.current));
      setError(null);
      failedDraft.current = null;
      const el = inputRef.current;
      if (el) {
        el.focus();
        if (typed === undefined && (type === 'text' || type === 'number')) el.select?.();
      }
    }
    if (!editing && wasEditing.current && restoreFocus.current && trigger === 'button') {
      viewRef.current?.focus();
    }
    if (!editing) restoreFocus.current = false;
    wasEditing.current = editing;
  }, [editing]);

  const refocusAfterSave = useRef(false);
  useIsoLayoutEffect(() => {
    if (saving || !refocusAfterSave.current) return;
    refocusAfterSave.current = false;
    inputRef.current?.focus();
  }, [saving]);

  const exit = (reason: EditableCellExitReason, restore: boolean) => {
    restoreFocus.current = restore;
    setError(null);
    failedDraft.current = null;
    setEditing(false, reason);
  };

  const commit = async (reason: 'commit' | 'blur') => {
    if (saving) return;
    const parsed = parseEditableCellDraft(type, draft);
    if ('error' in parsed) {
      setError(m[parsed.error]);
      failedDraft.current = draft;
      return;
    }
    const next = parsed.value;
    if (required && next === null) {
      setError(m.required);
      failedDraft.current = draft;
      return;
    }
    if (next === value || (next === null && (value === null || value === ''))) {
      exit(reason, reason === 'commit');
      return;
    }
    if (next !== null && min !== undefined && next < min) {
      setError(m.belowMin(String(min)));
      failedDraft.current = draft;
      return;
    }
    if (next !== null && max !== undefined && next > max) {
      setError(m.aboveMax(String(max)));
      failedDraft.current = draft;
      return;
    }
    const invalid = validate?.(next);
    if (present(invalid)) {
      setError(invalid);
      failedDraft.current = draft;
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await onCommit(next);
      if (!mounted.current) return;
      setSaving(false);
      exit(reason, reason === 'commit');
    } catch (err) {
      if (!mounted.current) return;
      setSaving(false);
      failedDraft.current = draft;
      setError(err instanceof Error && err.message ? err.message : m.saveFailed);
      // Focus once `saving` has rendered: a select is disabled until then
      // and would refuse the focus.
      refocusAfterSave.current = reason === 'commit';
    }
  };

  const cancel = () => {
    if (saving) return;
    setDraft(toDraft(value));
    exit('cancel', true);
    onCancel?.();
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement | HTMLSelectElement>) => {
    if (event.nativeEvent.isComposing) return;
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      cancel();
    } else if (event.key === 'Enter') {
      event.preventDefault();
      event.stopPropagation();
      void commit('commit');
    }
  };

  const onBlur = (event: FocusEvent<HTMLInputElement | HTMLSelectElement>) => {
    if (!saveOnBlur || saving || !editing) return;
    if (event.relatedTarget && event.currentTarget.parentElement?.contains(event.relatedTarget))
      return;
    // Do not retry a draft that just failed; its message stays visible.
    if (failedDraft.current !== null && failedDraft.current === draft) return;
    void commit('blur');
  };

  const display = (): ReactNode => {
    if (format) return format(value);
    if (value === null || value === '') return null;
    if (type === 'select') return options.find((o) => o.value === value)?.label ?? String(value);
    return String(value);
  };
  const shown = display();
  const empty = !present(shown);

  const hasError = present(error);
  const shared = {
    'aria-label': label,
    'aria-invalid': hasError || undefined,
    'aria-describedby': hasError ? errorId : undefined,
    'aria-busy': saving || undefined,
    'aria-required': required || undefined,
    className: 'mn-editable-cell__input',
    onKeyDown,
    onBlur,
  };

  const view =
    trigger === 'button' ? (
      <button
        ref={viewRef}
        type="button"
        className="mn-editable-cell__view"
        disabled={disabled}
        onClick={() => setEditing(true)}
        onKeyDown={(e) => {
          if (e.key === 'F2') {
            e.preventDefault();
            setEditing(true);
          }
        }}
      >
        <span className="mn-editable-cell__value">{empty ? placeholder : shown}</span>
        {disabled ? null : <span className="mn-visually-hidden">, {`${m.edit} ${label}`}</span>}
      </button>
    ) : (
      <span className="mn-editable-cell__view">
        <span className="mn-editable-cell__value">{empty ? placeholder : shown}</span>
      </span>
    );

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-editable-cell', className)}
      data-state={editing ? 'editing' : 'viewing'}
      data-type={type}
      data-numeric={numeric || undefined}
      data-empty={empty || undefined}
      data-saving={saving || undefined}
      data-invalid={hasError || undefined}
      data-disabled={disabled || undefined}
    >
      {editing ? (
        <div className="mn-editable-cell__editor">
          {type === 'select' ? (
            <select
              ref={inputRef}
              {...shared}
              value={draft}
              disabled={saving}
              onChange={(e) => setDraft(e.target.value)}
            >
              {!required || draft === '' ? <option value="">{m.noValue}</option> : null}
              {options.map((o) => (
                <option key={o.value} value={o.value} disabled={o.disabled}>
                  {o.label}
                </option>
              ))}
            </select>
          ) : (
            <input
              ref={inputRef}
              {...shared}
              type={type === 'date' ? 'date' : 'text'}
              inputMode={type === 'number' ? 'decimal' : undefined}
              min={type === 'date' ? min : undefined}
              max={type === 'date' ? max : undefined}
              placeholder={placeholder}
              readOnly={saving}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
            />
          )}
          {saving ? (
            <Spinner size="sm" label={m.saving} className="mn-editable-cell__spinner" />
          ) : null}
          {hasError ? (
            <p id={errorId} role="alert" className="mn-editable-cell__error">
              {error}
            </p>
          ) : null}
        </div>
      ) : (
        view
      )}
    </div>
  );
});
