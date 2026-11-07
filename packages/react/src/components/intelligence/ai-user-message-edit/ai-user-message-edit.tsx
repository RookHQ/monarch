'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type FormHTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { InfoGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';

// useLayoutEffect warns during server rendering; sizing only runs in the browser.
const useIsomorphicLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

export interface AiUserMessageEditMessages {
  /** Accessible name of the text field. */
  label: string;
  /** Visible note that resending keeps the original. */
  branchHint: string;
  /** Keyboard hint, read as part of the field's description. */
  keyboardHint: string;
  submit: string;
  cancel: string;
}

export const AI_USER_MESSAGE_EDIT_MESSAGES: AiUserMessageEditMessages = {
  label: 'Edit message',
  branchHint: 'Editing will create a new branch. The original stays available.',
  keyboardHint: 'Enter to send, Shift+Enter for a new line, Escape to cancel.',
  submit: 'Send',
  cancel: 'Cancel',
};

export interface AiUserMessageEditProps extends Omit<
  FormHTMLAttributes<HTMLFormElement>,
  'onSubmit' | 'defaultValue'
> {
  /** The prompt as it was sent. */
  defaultValue: string;
  /** Resends the edited prompt (trimmed). Not called when it is empty or unchanged. */
  onSubmit: (text: string) => void;
  /** Leaves edit mode without resending (Escape or Cancel). */
  onCancel: () => void;
  /** Rows the field grows to before it scrolls. Default 10. */
  maxRows?: number | undefined;
  /** Focus the field, caret at the end, when it mounts. Default true. */
  focusOnMount?: boolean | undefined;
  /** Disables the field and buttons, e.g. while a reply streams. */
  disabled?: boolean | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiUserMessageEditMessages> | undefined;
}

/**
 * Inline editor for a prompt the person already sent. Resending it creates a
 * new branch of the conversation, so the hint says so and the original stays
 * available. The field grows with its text; Enter sends, Shift+Enter adds a
 * line and Escape cancels.
 */
export const AiUserMessageEdit = forwardRef<HTMLFormElement, AiUserMessageEditProps>(
  function AiUserMessageEdit(
    {
      defaultValue,
      onSubmit,
      onCancel,
      maxRows = 10,
      focusOnMount = true,
      disabled = false,
      messages: messagesProp,
      className,
      style,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_USER_MESSAGE_EDIT_MESSAGES, ...messagesProp };
    const [value, setValue] = useState(defaultValue);
    const fieldRef = useRef<HTMLTextAreaElement>(null);
    const baseId = useId();
    const branchId = `${baseId}-branch`;
    const keysId = `${baseId}-keys`;
    const next = value.trim();
    const canSubmit = !disabled && next.length > 0 && next !== defaultValue.trim();

    useEffect(() => {
      const el = fieldRef.current;
      if (!focusOnMount || !el) return;
      el.focus();
      el.setSelectionRange(el.value.length, el.value.length);
      // eslint-disable-next-line react-hooks/exhaustive-deps -- focus once, on mount
    }, []);

    // Auto-grow: fit the content; CSS caps the height at `maxRows` lines.
    useIsomorphicLayoutEffect(() => {
      const el = fieldRef.current;
      if (!el) return;
      el.style.blockSize = 'auto';
      if (el.scrollHeight > 0) el.style.blockSize = `${el.scrollHeight + 2}px`;
    }, [value]);

    const submit = () => {
      if (canSubmit) onSubmit(next);
    };

    const onKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
      if (event.nativeEvent.isComposing) return;
      if (event.key === 'Escape') {
        event.preventDefault();
        // Keep Escape from also stopping a reply or closing a surrounding panel.
        event.stopPropagation();
        onCancel();
      } else if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        submit();
      }
    };

    return (
      <form
        ref={ref}
        aria-label={m.label}
        {...rest}
        style={{ ...style, ['--mn-ai-user-message-edit-rows' as string]: maxRows }}
        className={cx('mn-ai-user-message-edit', className)}
        data-disabled={disabled || undefined}
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
      >
        <textarea
          ref={fieldRef}
          className="mn-ai-user-message-edit__field"
          aria-label={m.label}
          aria-describedby={`${branchId} ${keysId}`}
          value={value}
          rows={1}
          disabled={disabled}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={onKeyDown}
        />
        <div className="mn-ai-user-message-edit__footer">
          <p id={branchId} className="mn-ai-user-message-edit__hint">
            <InfoGlyph aria-hidden className="mn-ai-user-message-edit__hint-icon" />
            {m.branchHint}
          </p>
          <span id={keysId} className="mn-visually-hidden">
            {m.keyboardHint}
          </span>
          <div className="mn-ai-user-message-edit__actions">
            <Button
              type="button"
              size="sm"
              variant="ghost"
              className="mn-ai-user-message-edit__button"
              onClick={onCancel}
            >
              {m.cancel}
            </Button>
            <Button
              type="submit"
              size="sm"
              variant="primary"
              className="mn-ai-user-message-edit__button"
              disabled={!canSubmit}
            >
              {m.submit}
            </Button>
          </div>
        </div>
      </form>
    );
  },
);
