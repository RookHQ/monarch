'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph, CloseGlyph, PencilGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Textarea } from '../../forms/text-field/index.js';

/** Who or what a memory is about. */
export interface AiMemoryEntity {
  /** The kind of thing, used to group memories. */
  kind: 'person' | 'org' | 'project' | 'other';
  /** Display name, e.g. "Ada Lovelace" or "Northwind". */
  name: string;
}

/** Something the AI remembers between conversations. */
export interface AiMemory {
  /** Stable id, passed back to `onEdit` and `onForget`. */
  id: string;
  /** What is remembered, in plain language, e.g. "Prefers summaries under 200 words". */
  text: string;
  /** Who or what the memory is about. */
  entity?: AiMemoryEntity | undefined;
}

/** Every string AiMemoryChip shows or reads. English defaults. */
export interface AiMemoryChipMessages {
  /** Accessible name of the chip. */
  label: string;
  /** Visible prefix in the `chip` variant. */
  prefix: string;
  /** Accessible name of the edit button. */
  edit: (text: string) => string;
  /** Accessible name of the forget button. */
  forget: (text: string) => string;
  /** Accessible name of the text field while editing. */
  editField: string;
  save: string;
  cancel: string;
  /** Hint under the field while editing. */
  editHint: string;
}

/** Shortens memory text for button names, so each name stays unique but readable. */
export function shortMemoryText(text: string, max = 48): string {
  const clean = text.replace(/\s+/g, ' ').trim();
  return clean.length > max ? `${clean.slice(0, max - 1).trimEnd()}…` : clean;
}

export const AI_MEMORY_CHIP_MESSAGES: AiMemoryChipMessages = {
  label: 'Memory',
  prefix: 'Remembered:',
  edit: (text) => `Edit memory: ${shortMemoryText(text)}`,
  forget: (text) => `Forget memory: ${shortMemoryText(text)}`,
  editField: 'Memory',
  save: 'Save',
  cancel: 'Cancel',
  editHint: 'Enter to save, Escape to cancel',
};

export interface AiMemoryChipProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The memory to show. */
  memory: AiMemory;
  /**
   * Shows an Edit button. Editing happens in place: Enter saves (Shift+Enter
   * adds a line), Escape cancels, and focus returns to the Edit button. Called
   * with the id and the new, trimmed text; never with empty or unchanged text.
   */
  onEdit?: ((id: string, text: string) => void) | undefined;
  /** Shows a Forget button. Called with the id; the chip does not ask for confirmation. */
  onForget?: ((id: string) => void) | undefined;
  /**
   * `chip` (default): an inline pill reading "Remembered: …" with the Bot
   * glyph, for use inside a thread. `row`: a full-width row without the prefix,
   * for lists such as AiMemoryPanel.
   */
  variant?: 'chip' | 'row' | undefined;
  /** Shows the entity name after the text. Default `true` for `chip`, `false` for `row`. */
  showEntity?: boolean | undefined;
  /** Extra content after the text, e.g. when the memory was last updated. */
  meta?: ReactNode | undefined;
  /** Content before the text in the `row` variant, e.g. a selection checkbox. */
  leading?: ReactNode | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiMemoryChipMessages> | undefined;
}

/**
 * One thing the AI remembers, shown as "Remembered: …" with optional Edit
 * (inline, Enter saves and Escape cancels) and Forget buttons. Use it in a
 * thread when the AI saves a memory, or as a row in AiMemoryPanel.
 */
export const AiMemoryChip = forwardRef<HTMLDivElement, AiMemoryChipProps>(function AiMemoryChip(
  {
    memory,
    onEdit,
    onForget,
    variant = 'chip',
    showEntity,
    meta,
    leading,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_MEMORY_CHIP_MESSAGES, ...messagesProp };
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(memory.text);
  const editButton = useRef<HTMLButtonElement>(null);
  const field = useRef<HTMLTextAreaElement>(null);
  const restoreFocus = useRef(false);
  const hintId = useId();
  const withEntity = (showEntity ?? variant === 'chip') && memory.entity;

  useEffect(() => {
    if (editing) {
      const el = field.current;
      el?.focus();
      el?.select();
    } else if (restoreFocus.current) {
      restoreFocus.current = false;
      editButton.current?.focus();
    }
  }, [editing]);

  const startEdit = () => {
    setDraft(memory.text);
    setEditing(true);
  };
  const stopEdit = () => {
    restoreFocus.current = true;
    setEditing(false);
  };
  const trimmed = draft.trim();
  const save = () => {
    if (!trimmed) return;
    if (trimmed !== memory.text) onEdit?.(memory.id, trimmed);
    stopEdit();
  };
  const onFieldKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      save();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      stopEdit();
    }
  };

  return (
    <div
      ref={ref}
      role="group"
      aria-label={m.label}
      {...rest}
      className={cx('mn-ai-memory-chip', className)}
      data-variant={variant}
      data-editing={editing || undefined}
    >
      {variant === 'row' && leading ? (
        <span className="mn-ai-memory-chip__leading">{leading}</span>
      ) : null}
      {variant === 'chip' ? (
        <span className="mn-ai-memory-chip__icon" aria-hidden>
          <BotGlyph />
        </span>
      ) : null}
      {editing ? (
        <div className="mn-ai-memory-chip__editor">
          <Textarea
            ref={field}
            className="mn-ai-memory-chip__field"
            aria-label={m.editField}
            aria-describedby={hintId}
            value={draft}
            rows={2}
            resizable={false}
            invalid={!trimmed}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onFieldKeyDown}
          />
          <div className="mn-ai-memory-chip__edit-actions">
            <span id={hintId} className="mn-ai-memory-chip__hint">
              {m.editHint}
            </span>
            <Button size="xs" variant="ghost" onClick={stopEdit}>
              {m.cancel}
            </Button>
            <Button size="xs" variant="primary" disabled={!trimmed} onClick={save}>
              {m.save}
            </Button>
          </div>
        </div>
      ) : (
        <>
          <span className="mn-ai-memory-chip__body">
            {variant === 'chip' ? (
              <span className="mn-ai-memory-chip__prefix">{m.prefix} </span>
            ) : null}
            <span className="mn-ai-memory-chip__text">{memory.text}</span>
            {withEntity ? (
              <span className="mn-ai-memory-chip__entity"> · {memory.entity?.name}</span>
            ) : null}
            {meta ? <span className="mn-ai-memory-chip__meta">{meta}</span> : null}
          </span>
          {onEdit || onForget ? (
            <span className="mn-ai-memory-chip__actions">
              {onEdit ? (
                <IconButton
                  ref={editButton}
                  size="xs"
                  label={m.edit(memory.text)}
                  icon={<PencilGlyph />}
                  onClick={startEdit}
                />
              ) : null}
              {onForget ? (
                <IconButton
                  size="xs"
                  label={m.forget(memory.text)}
                  icon={<CloseGlyph />}
                  onClick={() => onForget(memory.id)}
                />
              ) : null}
            </span>
          ) : null}
        </>
      )}
    </div>
  );
});
