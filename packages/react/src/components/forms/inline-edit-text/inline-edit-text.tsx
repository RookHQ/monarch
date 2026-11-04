'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type FocusEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { PencilGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

// useLayoutEffect warns during server rendering; focus work only runs in the browser.
const useIsomorphicLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

export interface InlineEditTextProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children' | 'onError'
> {
  /** What the text is, for example "Title". The input's accessible name, and part of the view button's. */
  label: string;
  /** The saved value (controlled). */
  value?: string;
  /** The initial saved value (uncontrolled). */
  defaultValue?: string;
  /**
   * Persists a new value. Return a promise to show the pending state; reject
   * it (throw) to keep the editor open with the error and the user's draft.
   * Not called when the value is unchanged.
   */
  onSave: (next: string) => void | Promise<void>;
  /** Called when the user cancels with Escape or the Cancel button. */
  onCancel?: () => void;
  /**
   * Called when `onSave` rejects, with the error and the draft that failed.
   * Use it for product-specific handling such as an edit conflict (HTTP 409):
   * keep the editor open and pass your own resolution UI through `error`.
   */
  onSaveError?: (error: unknown, draft: string) => void;
  /** Turns a rejected save into a message. Default: the Error's message, or "Couldn't save. Try again." */
  getErrorMessage?: (error: unknown) => ReactNode;
  /** Validates the draft before saving. Return a message to block the save, or nothing to allow it. */
  validate?: (draft: string) => ReactNode | null | undefined;
  /** An error supplied by the product (for example a conflict notice), shown while editing. */
  error?: ReactNode;
  /** Edit mode (controlled). */
  editing?: boolean;
  /** Initial edit mode (uncontrolled). */
  defaultEditing?: boolean;
  onEditingChange?: (editing: boolean) => void;
  /** Shown in view mode when the value is empty, and as the input placeholder. */
  placeholder?: string;
  /** A textarea instead of an input: Enter adds a line, Ctrl/⌘+Enter saves. */
  multiline?: boolean;
  /** Rows for `multiline`. Default 3. */
  rows?: number;
  maxLength?: number;
  /** Trim whitespace before comparing and saving. Default true. */
  trim?: boolean;
  /** Save when focus leaves the editor. Default true. */
  saveOnBlur?: boolean;
  /** Show Save and Cancel buttons under the editor. Default: true for `multiline`. */
  showActions?: boolean;
  /** Not editable: the value is shown as plain text. */
  disabled?: boolean;
  /** Visually hidden hint read after the view button's name. Default "Edit". */
  editHint?: string;
  saveLabel?: string;
  cancelLabel?: string;
  /** Accessible name of the pending spinner. Default "Saving". */
  savingLabel?: string;
}

function defaultMessage(error: unknown): ReactNode {
  if (error instanceof Error && error.message) return error.message;
  return 'Couldn’t save. Try again.';
}

/**
 * Text that turns into an input in place: click it or press Enter to edit,
 * Enter (or leaving the field) saves, Escape cancels. Saving can be async,
 * with a pending state, and a failed save keeps the editor open with the
 * error. The product owns persistence and conflict handling.
 */
export const InlineEditText = forwardRef<HTMLDivElement, InlineEditTextProps>(
  function InlineEditText(
    {
      label,
      value: valueProp,
      defaultValue = '',
      onSave,
      onCancel,
      onSaveError,
      getErrorMessage = defaultMessage,
      validate,
      error: externalError,
      editing: editingProp,
      defaultEditing = false,
      onEditingChange,
      placeholder,
      multiline = false,
      rows = 3,
      maxLength,
      trim = true,
      saveOnBlur = true,
      showActions = multiline,
      disabled = false,
      editHint = 'Edit',
      saveLabel = 'Save',
      cancelLabel = 'Cancel',
      savingLabel = 'Saving',
      className,
      ...rest
    },
    ref,
  ) {
    const auto = useId();
    const errorId = `mn-inline-edit-${auto}-error`;
    const hintId = `mn-inline-edit-${auto}-hint`;
    const [innerValue, setInnerValue] = useState(defaultValue);
    const value = valueProp ?? innerValue;
    const [innerEditing, setInnerEditing] = useState(defaultEditing && !disabled);
    const editing = (editingProp ?? innerEditing) && !disabled;
    const [draft, setDraft] = useState(value);
    const [saving, setSaving] = useState(false);
    const [localError, setLocalError] = useState<ReactNode>(null);
    const viewRef = useRef<HTMLButtonElement>(null);
    const inputRef = useRef<HTMLInputElement & HTMLTextAreaElement>(null);
    const rootRef = useRef<HTMLDivElement | null>(null);
    // Focus returns to the view button after Enter / Escape / the buttons, not
    // after a blur save (the user has already moved elsewhere).
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
      (next: boolean) => {
        if (editingProp === undefined) setInnerEditing(next);
        onEditingChange?.(next);
      },
      [editingProp, onEditingChange],
    );

    // Entering edit mode starts the draft from the saved value and focuses
    // it; leaving after Enter / Escape / the buttons returns focus to the text.
    const valueRef = useRef(value);
    valueRef.current = value;
    const wasEditing = useRef(editing);
    useIsomorphicLayoutEffect(() => {
      if (editing && !wasEditing.current) {
        setDraft(valueRef.current);
        setLocalError(null);
        failedDraft.current = null;
        inputRef.current?.focus();
        inputRef.current?.select();
      }
      if (!editing && wasEditing.current && restoreFocus.current) {
        viewRef.current?.focus();
      }
      if (!editing) restoreFocus.current = false;
      wasEditing.current = editing;
    }, [editing]);
    // Mounted already editing (defaultEditing / editing): focus the input once.
    useEffect(() => {
      if (wasEditing.current) inputRef.current?.focus();
    }, []);

    const exit = (restore: boolean) => {
      restoreFocus.current = restore;
      setLocalError(null);
      failedDraft.current = null;
      setEditing(false);
    };

    const commit = async (restore: boolean) => {
      if (saving) return;
      const next = trim ? draft.trim() : draft;
      if (next === value) {
        exit(restore);
        return;
      }
      const invalid = validate?.(next);
      if (invalid !== null && invalid !== undefined && invalid !== false && invalid !== '') {
        setLocalError(invalid);
        failedDraft.current = draft;
        return;
      }
      setSaving(true);
      setLocalError(null);
      try {
        await onSave(next);
        if (!mounted.current) return;
        if (valueProp === undefined) setInnerValue(next);
        setSaving(false);
        exit(restore);
      } catch (err) {
        if (!mounted.current) return;
        setSaving(false);
        failedDraft.current = draft;
        setLocalError(getErrorMessage(err));
        onSaveError?.(err, next);
        // Keep the user in the editor with their draft.
        if (restore) inputRef.current?.focus();
      }
    };

    const cancel = () => {
      if (saving) return;
      setDraft(value);
      exit(true);
      onCancel?.();
    };

    const onKeyDown = (event: KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      if (event.nativeEvent.isComposing) return;
      if (event.key === 'Escape') {
        event.preventDefault();
        event.stopPropagation();
        cancel();
      } else if (event.key === 'Enter' && (!multiline || event.metaKey || event.ctrlKey)) {
        event.preventDefault();
        void commit(true);
      }
    };

    const onBlur = (event: FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      if (!saveOnBlur || saving) return;
      // Moving to the Save / Cancel buttons is not leaving the editor.
      if (event.relatedTarget && rootRef.current?.contains(event.relatedTarget as Node)) return;
      // Do not retry a draft that has just failed; the error stays visible.
      if (failedDraft.current !== null && failedDraft.current === draft) return;
      void commit(false);
    };

    const shownError = localError ?? (editing ? externalError : null);
    const hasError =
      shownError !== null && shownError !== undefined && shownError !== false && shownError !== '';
    const empty = value === '';

    const setRoot = (el: HTMLDivElement | null) => {
      rootRef.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) ref.current = el;
    };

    const shared = {
      'aria-label': label,
      'aria-invalid': hasError || undefined,
      'aria-describedby': hasError ? errorId : undefined,
      'aria-busy': saving || undefined,
      readOnly: saving,
      value: draft,
      placeholder,
      maxLength,
      onKeyDown,
      onBlur,
      className: 'mn-inline-edit-text__input',
    };

    return (
      <div
        ref={setRoot}
        {...rest}
        className={cx('mn-inline-edit-text', className)}
        data-state={editing ? 'editing' : 'viewing'}
        data-multiline={multiline || undefined}
        data-empty={empty || undefined}
        data-saving={saving || undefined}
        data-invalid={hasError || undefined}
        data-disabled={disabled || undefined}
      >
        {editing ? (
          <>
            <div className="mn-inline-edit-text__editor">
              {multiline ? (
                <textarea
                  ref={inputRef}
                  {...shared}
                  rows={rows}
                  onChange={(e) => setDraft(e.target.value)}
                />
              ) : (
                <input
                  ref={inputRef}
                  {...shared}
                  type="text"
                  onChange={(e) => setDraft(e.target.value)}
                />
              )}
              {saving && !showActions ? (
                <Spinner size="sm" label={savingLabel} className="mn-inline-edit-text__spinner" />
              ) : null}
            </div>
            {hasError ? (
              <p id={errorId} role="alert" className="mn-inline-edit-text__error">
                {shownError}
              </p>
            ) : null}
            {showActions ? (
              <div className="mn-inline-edit-text__actions">
                <Button
                  size="sm"
                  variant="primary"
                  loading={saving}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => void commit(true)}
                >
                  {saving ? savingLabel : saveLabel}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={saving}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={cancel}
                >
                  {cancelLabel}
                </Button>
              </div>
            ) : null}
          </>
        ) : disabled ? (
          <span className="mn-inline-edit-text__view">
            <span className="mn-inline-edit-text__value">{empty ? placeholder : value}</span>
          </span>
        ) : (
          <button
            ref={viewRef}
            type="button"
            className="mn-inline-edit-text__view"
            aria-describedby={hintId}
            onClick={() => setEditing(true)}
          >
            <span className="mn-visually-hidden">{label}: </span>
            <span className="mn-inline-edit-text__value">
              {empty ? (placeholder ?? '\u00a0') : value}
            </span>
            <PencilGlyph className="mn-inline-edit-text__icon" />
          </button>
        )}
        {!editing && !disabled ? (
          <span id={hintId} className="mn-visually-hidden">
            {editHint}
          </span>
        ) : null}
      </div>
    );
  },
);
