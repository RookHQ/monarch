'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { WarningGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../popover/index.js';

const DEFAULT_ERROR = 'That did not work. Try again.';

function defaultErrorMessage(error: unknown): ReactNode {
  return error instanceof Error && error.message ? error.message : DEFAULT_ERROR;
}

export interface PopconfirmProps extends Omit<
  ComponentPropsWithoutRef<typeof PopoverContent>,
  'title' | 'children' | 'onOpenAutoFocus' | 'role'
> {
  /** The button that asks for confirmation (rendered with `asChild`). */
  trigger: ReactElement;
  /** The question, e.g. "Remove this filter?". Becomes the accessible name. */
  title: ReactNode;
  /** One short line on what will happen. Becomes the accessible description. */
  description?: ReactNode;
  /**
   * `danger` for removing or discarding something small and recoverable: a
   * warning icon and a red confirm button. Default `default`.
   */
  tone?: 'default' | 'danger' | undefined;
  /** Label of the confirming button. Name the action ("Remove"), not "OK". Default "Confirm". */
  confirmLabel?: ReactNode;
  /** Default "Cancel". */
  cancelLabel?: ReactNode;
  /**
   * Runs the action. Return a promise to keep the popover open in a pending
   * state until it settles: on success it closes, on rejection the error is
   * shown inside it and the person can retry or cancel.
   */
  onConfirm: () => unknown;
  /** Called when the popover is dismissed without confirming (Cancel, Escape, click outside). */
  onCancel?: (() => void) | undefined;
  /** Whether the popover is open (controlled). */
  open?: boolean | undefined;
  /** Initial open state when uncontrolled. */
  defaultOpen?: boolean | undefined;
  /** Called when the popover opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Turns a rejected `onConfirm` into a message. Default: the Error's message, or a generic line. */
  errorMessage?: ((error: unknown) => ReactNode) | undefined;
  /**
   * Which button has focus when it opens. `cancel` (default) is the safe
   * choice; `confirm` suits harmless, frequent confirmations.
   */
  initialFocus?: 'cancel' | 'confirm' | undefined;
}

/**
 * A small confirmation anchored to the button that triggers it, for
 * low-stakes actions that are easy to redo: remove a filter, discard a
 * draft reply, reset a setting. Focus moves to Cancel; Escape, Cancel or a
 * click outside cancels and focus returns to the trigger. For anything that
 * loses real work use ConfirmDialog or `useConfirm()`, or act and offer undo.
 */
export const Popconfirm = forwardRef<ElementRef<typeof PopoverContent>, PopconfirmProps>(
  function Popconfirm(
    {
      trigger,
      title,
      description,
      tone = 'default',
      confirmLabel = 'Confirm',
      cancelLabel = 'Cancel',
      onConfirm,
      onCancel,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      errorMessage = defaultErrorMessage,
      initialFocus = 'cancel',
      side = 'top',
      align = 'center',
      className,
      ...rest
    },
    ref,
  ) {
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [pending, setPending] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const cancelRef = useRef<HTMLButtonElement>(null);
    const confirmRef = useRef<HTMLButtonElement>(null);
    const mounted = useRef(true);
    const id = useId();
    const titleId = `mn-popconfirm-${id}-title`;
    const descriptionId = description ? `mn-popconfirm-${id}-description` : undefined;
    const errorId = `mn-popconfirm-${id}-error`;

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Every opening starts without an old error.
    useEffect(() => {
      if (open) setFailure(null);
    }, [open]);

    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    const handleOpenChange = (next: boolean) => {
      if (!next && pending) return;
      if (!next) onCancel?.();
      setOpen(next);
    };

    const confirm = async () => {
      if (pending) return;
      setFailure(null);
      try {
        const result = onConfirm();
        if (isThenable(result)) {
          setPending(true);
          await result;
        }
      } catch (error) {
        if (mounted.current) {
          setPending(false);
          setFailure(errorMessage(error));
        }
        return;
      }
      if (!mounted.current) return;
      setPending(false);
      setOpen(false);
    };

    return (
      <Popover open={open} onOpenChange={handleOpenChange} modal>
        <PopoverTrigger asChild>{trigger}</PopoverTrigger>
        <PopoverContent
          ref={ref}
          side={side}
          align={align}
          {...rest}
          role="alertdialog"
          aria-labelledby={titleId}
          aria-describedby={descriptionId}
          aria-busy={pending || undefined}
          data-tone={tone}
          className={cx('mn-popconfirm', className)}
          onOpenAutoFocus={(event) => {
            event.preventDefault();
            (initialFocus === 'confirm' ? confirmRef : cancelRef).current?.focus();
          }}
        >
          <div className="mn-popconfirm__body">
            {tone === 'danger' ? (
              <span className="mn-popconfirm__icon" aria-hidden>
                <WarningGlyph />
              </span>
            ) : null}
            <div className="mn-popconfirm__text">
              <p id={titleId} className="mn-popconfirm__title">
                {title}
              </p>
              {description ? (
                <p id={descriptionId} className="mn-popconfirm__description">
                  {description}
                </p>
              ) : null}
            </div>
          </div>
          {failure ? (
            <p id={errorId} className="mn-popconfirm__error" role="alert">
              {failure}
            </p>
          ) : null}
          <div className="mn-popconfirm__actions">
            <Button
              ref={cancelRef}
              size="sm"
              variant="secondary"
              disabled={pending}
              onClick={() => handleOpenChange(false)}
            >
              {cancelLabel}
            </Button>
            <Button
              ref={confirmRef}
              size="sm"
              variant={tone === 'danger' ? 'danger' : 'primary'}
              loading={pending}
              aria-describedby={failure ? errorId : undefined}
              onClick={() => void confirm()}
            >
              {confirmLabel}
            </Button>
          </div>
        </PopoverContent>
      </Popover>
    );
  },
);
