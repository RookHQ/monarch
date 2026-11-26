'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { WarningGlyph } from '../../../internal/icons.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../actions/button/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';

/** Every visible or announced string of FormActionsBar, for translation. */
export interface FormActionsBarMessages {
  /** Accessible name of the bar. Default "Form actions". */
  label: string;
  /** Shown while there are unsaved changes. Default "You have unsaved changes". */
  unsaved: ReactNode;
  /** The save button. Default "Save changes". */
  save: ReactNode;
  /** The discard button. Default "Discard". */
  discard: ReactNode;
  /** Title of the discard confirmation. Default "Discard unsaved changes?". */
  discardTitle: ReactNode;
  /** Description of the discard confirmation. Default "Your changes will be lost. This cannot be undone.". */
  discardDescription: ReactNode;
  /** Confirming button of the discard confirmation. Default "Discard changes". */
  discardConfirm: ReactNode;
  /** Cancelling button of the discard confirmation. Default "Keep editing". */
  keepEditing: ReactNode;
  /** Shown when `onSave` rejects without a readable message. Default "Your changes could not be saved. Try again.". */
  saveFailed: ReactNode;
}

const DEFAULT_MESSAGES: FormActionsBarMessages = {
  label: 'Form actions',
  unsaved: 'You have unsaved changes',
  save: 'Save changes',
  discard: 'Discard',
  discardTitle: 'Discard unsaved changes?',
  discardDescription: 'Your changes will be lost. This cannot be undone.',
  discardConfirm: 'Discard changes',
  keepEditing: 'Keep editing',
  saveFailed: 'Your changes could not be saved. Try again.',
};

export interface FormActionsBarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'role'> {
  /** Whether the form has unsaved changes. Save and Discard are disabled while it is `false`. */
  dirty: boolean;
  /**
   * Saves the form. Return a promise to show the pending state until it
   * settles; a rejection shows the error in the bar. Without `onSave`, Save
   * is a submit button, so the form's `onSubmit` handles it.
   */
  onSave?: (() => unknown) | undefined;
  /** Reverts the form. When given, a Discard button is shown. */
  onDiscard?: (() => void) | undefined;
  /** Ask before discarding, with a danger confirmation. Default `true`. */
  confirmDiscard?: boolean;
  /** Controlled pending state, for a save the product tracks itself (e.g. a submitting form). */
  pending?: boolean;
  /** Controlled error message, shown in the bar. */
  error?: ReactNode;
  /** Turns a rejected `onSave` into a message. Default: the Error's message, or `messages.saveFailed`. */
  errorMessage?: ((error: unknown) => ReactNode) | undefined;
  /** The `id` of the form to submit, when the bar is rendered outside the `<form>`. */
  form?: string;
  /** Stick to the bottom of the scrolling container. Default `true`. */
  sticky?: boolean;
  /** Hide the bar while there are no unsaved changes, no error and nothing pending. Default `false`. */
  hideWhenClean?: boolean;
  /** Replaces the "unsaved changes" text, e.g. a SaveStatusIndicator. */
  status?: ReactNode;
  /** Extra actions placed before Discard, e.g. "Preview". */
  children?: ReactNode;
  /** Replaces any of the default strings. */
  messages?: Partial<FormActionsBarMessages>;
}

function defaultError(error: unknown, fallback: ReactNode): ReactNode {
  return error instanceof Error && error.message ? error.message : fallback;
}

/**
 * The Save and Discard bar for a form, sticky at the bottom so the actions
 * stay in reach on long forms and phones. Save and Discard are enabled only
 * with unsaved changes; Save shows a pending state while `onSave` runs and
 * keeps the error in the bar if it fails; Discard asks for confirmation
 * first. Pair it with UnsavedChangesGuard (`when={dirty}`) so leaving the
 * page asks too.
 */
export const FormActionsBar = forwardRef<HTMLDivElement, FormActionsBarProps>(
  function FormActionsBar(
    {
      dirty,
      onSave,
      onDiscard,
      confirmDiscard = true,
      pending: pendingProp,
      error: errorProp,
      errorMessage,
      form,
      sticky = true,
      hideWhenClean = false,
      status,
      children,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const { confirm, element: confirmElement } = useConfirm();
    const [running, setRunning] = useState(false);
    const [failure, setFailure] = useState<ReactNode>(null);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    const pending = pendingProp ?? running;
    const error = errorProp ?? failure;
    const hidden = hideWhenClean && !dirty && !pending && !error;

    const save = () => {
      if (!onSave || pending) return;
      setFailure(null);
      let result: unknown;
      try {
        result = onSave();
      } catch (raw) {
        setFailure(errorMessage ? errorMessage(raw) : defaultError(raw, messages.saveFailed));
        return;
      }
      if (!isThenable(result)) return;
      setRunning(true);
      Promise.resolve(result).then(
        () => {
          if (mounted.current) setRunning(false);
        },
        (raw: unknown) => {
          if (!mounted.current) return;
          setRunning(false);
          setFailure(errorMessage ? errorMessage(raw) : defaultError(raw, messages.saveFailed));
        },
      );
    };

    const discard = async () => {
      if (!onDiscard) return;
      if (confirmDiscard) {
        const ok = await confirm({
          title: messages.discardTitle,
          description: messages.discardDescription,
          tone: 'danger',
          confirmLabel: messages.discardConfirm,
          cancelLabel: messages.keepEditing,
        });
        if (!ok) return;
      }
      setFailure(null);
      onDiscard();
    };

    return (
      <div
        ref={ref}
        role="region"
        aria-label={messages.label}
        {...rest}
        hidden={hidden || rest.hidden}
        data-dirty={dirty || undefined}
        data-pending={pending || undefined}
        data-error={error ? '' : undefined}
        data-sticky={sticky || undefined}
        className={cx('mn-form-actions-bar', className)}
      >
        <div className="mn-form-actions-bar__status">
          {error ? (
            <p className="mn-form-actions-bar__error" role="alert">
              <span className="mn-form-actions-bar__error-icon" aria-hidden>
                <WarningGlyph />
              </span>
              <span>{error}</span>
            </p>
          ) : null}
          <span className="mn-form-actions-bar__message" role="status" aria-live="polite">
            {status ?? (dirty && !error ? messages.unsaved : null)}
          </span>
        </div>
        <div className="mn-form-actions-bar__actions">
          {children}
          {onDiscard ? (
            <Button
              variant="ghost"
              disabled={!dirty || pending}
              onClick={() => void discard()}
              className="mn-form-actions-bar__discard"
            >
              {messages.discard}
            </Button>
          ) : null}
          <Button
            variant="primary"
            type={onSave ? 'button' : 'submit'}
            form={form}
            disabled={!dirty && !pending}
            loading={pending}
            onClick={onSave ? save : undefined}
            className="mn-form-actions-bar__save"
          >
            {messages.save}
          </Button>
        </div>
        {confirmElement}
      </div>
    );
  },
);
