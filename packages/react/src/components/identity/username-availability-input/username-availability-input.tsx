'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type InputHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, InfoGlyph, WarningGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../../forms/form-field/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

/** Where an asynchronous check stands. `idle` means there is nothing to check. */
export type AsyncValidationStatus = 'idle' | 'checking' | 'valid' | 'invalid' | 'error';

export interface AsyncValidationResult {
  /** True when the value can be used (for example, the handle is free). */
  valid: boolean;
  /** Shown under the field and announced. Falls back to the component's default message. */
  message?: string;
}

/**
 * Checks a value. Return `null` when there is nothing to check (empty, or the
 * user's current value). Throwing or rejecting sets the `error` status. The
 * signal aborts when the value changes again, so pass it to `fetch`.
 */
export type AsyncValidator = (
  value: string,
  signal: AbortSignal,
) => AsyncValidationResult | null | Promise<AsyncValidationResult | null>;

export interface AsyncValidationState {
  status: AsyncValidationStatus;
  /** The validator's message for the latest result, if it gave one. */
  message: string | undefined;
  /** Runs the check again for the current value, without waiting (e.g. after an error). */
  revalidate: () => void;
}

/**
 * Runs `validate` for `value` after `debounce` ms without changes. Earlier
 * checks are aborted and their late results ignored, so the state always
 * describes the latest value. Empty values are `idle`.
 */
export function useAsyncValidation(
  value: string,
  validate: AsyncValidator,
  { debounce = 400 }: { debounce?: number } = {},
): AsyncValidationState {
  const [state, setState] = useState<{ status: AsyncValidationStatus; message?: string }>({
    status: 'idle',
  });
  const [nonce, setNonce] = useState(0);
  const immediate = useRef(false);
  const validateRef = useRef(validate);
  validateRef.current = validate;

  useEffect(() => {
    if (value === '') {
      setState({ status: 'idle' });
      return;
    }
    const controller = new AbortController();
    const run = async () => {
      try {
        const result = await validateRef.current(value, controller.signal);
        if (controller.signal.aborted) return;
        if (result === null) setState({ status: 'idle' });
        else
          setState(
            result.message === undefined
              ? { status: result.valid ? 'valid' : 'invalid' }
              : { status: result.valid ? 'valid' : 'invalid', message: result.message },
          );
      } catch {
        if (!controller.signal.aborted) setState({ status: 'error' });
      }
    };
    setState({ status: 'checking' });
    const wait = immediate.current ? 0 : debounce;
    immediate.current = false;
    const timer = setTimeout(() => void run(), wait);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [value, debounce, nonce]);

  const revalidate = useCallback(() => {
    immediate.current = true;
    setNonce((n) => n + 1);
  }, []);

  return { status: state.status, message: state.message, revalidate };
}

export interface UsernameAvailabilityInputMessages {
  checking: string;
  valid: string;
  invalid: string;
  error: string;
  /** Label of the button shown after a failed check. */
  retry: string;
}

const defaultMessages: UsernameAvailabilityInputMessages = {
  checking: 'Checking availability…',
  valid: 'Available',
  invalid: 'Not available',
  error: 'Couldn’t check availability.',
  retry: 'Try again',
};

export interface UsernameAvailabilityInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'prefix' | 'type'
> {
  /** The text (controlled). */
  value?: string;
  /** Initial text when uncontrolled. */
  defaultValue?: string;
  /** Called with the new text (after `transform`) on every change. */
  onValueChange?: (value: string) => void;
  /** The product's check: local rules and the availability lookup. Required. */
  validate: AsyncValidator;
  /** Milliseconds without typing before `validate` runs. Default 400. */
  debounce?: number;
  /** Rewrites typed text before it is stored, e.g. lower-casing or slugifying. */
  transform?: (raw: string) => string;
  /** Fixed text before the value, such as a domain ("example.com/"). Read as part of the description. */
  prefix?: ReactNode;
  /** Called whenever the status changes, so a form can block submit until `valid`. */
  onStatusChange?: (status: AsyncValidationStatus, message: string | undefined) => void;
  /** Override the default status texts. */
  messages?: Partial<UsernameAvailabilityInputMessages>;
  size?: 'sm' | 'md' | 'lg';
  /** Marks the value invalid regardless of the check. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean;
  /** Class for the outer wrapper (the input itself is `.mn-username-availability-input__input`). */
  className?: string;
}

const statusIcon: Record<AsyncValidationStatus, ReactNode> = {
  idle: null,
  checking: <Spinner size="sm" />,
  valid: <CheckGlyph />,
  invalid: <WarningGlyph />,
  error: <InfoGlyph />,
};

/**
 * A text input whose value is checked asynchronously while the user types:
 * handles, workspace addresses, slugs. The product supplies the check; the
 * component debounces it, aborts stale requests, shows a status icon with a
 * text message, and announces the result to screen readers.
 */
export const UsernameAvailabilityInput = forwardRef<
  HTMLInputElement,
  UsernameAvailabilityInputProps
>(function UsernameAvailabilityInput(
  {
    value: valueProp,
    defaultValue = '',
    onValueChange,
    validate,
    debounce = 400,
    transform,
    prefix,
    onStatusChange,
    messages: messagesProp,
    size = 'md',
    invalid,
    className,
    ...rest
  },
  ref,
) {
  const id = useId();
  const prefixId = `mn-uai-${id}-prefix`;
  const messageId = `mn-uai-${id}-message`;
  const messages = { ...defaultMessages, ...messagesProp };
  const [internal, setInternal] = useState(defaultValue);
  const value = valueProp !== undefined ? valueProp : internal;
  const { status, message, revalidate } = useAsyncValidation(value, validate, { debounce });

  const text =
    status === 'idle'
      ? ''
      : status === 'checking'
        ? messages.checking
        : (message ?? messages[status]);
  // Announce settled results only: "checking" on every pause would be noise.
  const announcement = status === 'valid' || status === 'invalid' || status === 'error' ? text : '';

  const statusRef = useRef(onStatusChange);
  statusRef.current = onStatusChange;
  useEffect(() => {
    statusRef.current?.(status, message);
  }, [status, message]);

  const fieldProps = useFieldControl(
    {
      ...rest,
      'aria-describedby': cx(
        prefix ? prefixId : undefined,
        rest['aria-describedby'],
        text ? messageId : undefined,
      ),
    },
    { invalid: invalid || (status === 'invalid' ? true : undefined) },
  );

  return (
    <div
      className={cx('mn-username-availability-input', className)}
      data-status={status}
      data-size={size}
    >
      <div
        className="mn-input mn-username-availability-input__control"
        data-size={size}
        data-disabled={fieldProps.disabled || undefined}
        data-invalid={fieldProps['aria-invalid'] ? true : undefined}
      >
        {prefix ? (
          <span id={prefixId} className="mn-username-availability-input__prefix">
            {prefix}
          </span>
        ) : null}
        <input
          ref={ref}
          autoComplete="off"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          {...fieldProps}
          type="text"
          className="mn-username-availability-input__input"
          value={value}
          onChange={(e) => {
            const next = transform ? transform(e.target.value) : e.target.value;
            if (valueProp === undefined) setInternal(next);
            onValueChange?.(next);
          }}
        />
        {statusIcon[status] ? (
          <span className="mn-username-availability-input__icon" aria-hidden>
            {statusIcon[status]}
          </span>
        ) : null}
      </div>
      {text ? (
        <p id={messageId} className="mn-username-availability-input__message">
          <span>{text}</span>
          {status === 'error' ? (
            <button
              type="button"
              className="mn-username-availability-input__retry"
              onClick={revalidate}
            >
              {messages.retry}
            </button>
          ) : null}
        </p>
      ) : null}
      <span className="mn-visually-hidden" role="status">
        {announcement}
      </span>
    </div>
  );
});
