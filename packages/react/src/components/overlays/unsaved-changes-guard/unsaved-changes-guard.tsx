'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
  type ElementRef,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { focusReturnTarget, isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../actions/button/index.js';
import { Alert } from '../../feedback/alert/index.js';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  type DialogContentProps,
} from '../dialog/index.js';

/** Every visible string of the unsaved-changes dialog, for translation. */
export interface UnsavedChangesMessages {
  /** The question. Default "Discard unsaved changes?". */
  title: ReactNode;
  /** What happens if the person leaves. Default "Your changes have not been saved. If you leave now, they will be lost.". */
  description: ReactNode;
  /** The safe action, focused on open. Default "Keep editing". */
  keepEditing: ReactNode;
  /** The destructive action. Default "Discard changes". */
  discard: ReactNode;
  /** Shown only when there is an `onSave`. Default "Save and leave". */
  save: ReactNode;
  /** Shown when saving fails without a readable message. Default "Your changes could not be saved. Try again.". */
  saveFailed: ReactNode;
}

const DEFAULT_MESSAGES: UnsavedChangesMessages = {
  title: 'Discard unsaved changes?',
  description: 'Your changes have not been saved. If you leave now, they will be lost.',
  keepEditing: 'Keep editing',
  discard: 'Discard changes',
  save: 'Save and leave',
  saveFailed: 'Your changes could not be saved. Try again.',
};

/** How the person chose to leave: without saving, or after a successful save. */
export type UnsavedChangesLeaveReason = 'discard' | 'save';

export interface UnsavedChangesDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'size'
> {
  /** Whether the dialog is shown. The dialog is always controlled. */
  open: boolean;
  /** Called with `false` when the person chooses to stay (Keep editing or Escape). */
  onOpenChange?: (open: boolean) => void;
  /** The person chose to stay: Keep editing, Escape or the close button. */
  onStay?: () => void;
  /**
   * The person chose to leave: `discard` straight away, or `save` once
   * `onSave` has succeeded. Navigate here and close the dialog.
   */
  onLeave: (reason: UnsavedChangesLeaveReason) => void;
  /**
   * Saves the changes. When given, a "Save and leave" button is shown. Return
   * a promise to keep the dialog pending until it settles: on success
   * `onLeave('save')` is called; on rejection the error is shown and the
   * dialog stays open.
   */
  onSave?: () => unknown;
  /** Extra content under the description, e.g. a list of the unsaved fields. */
  children?: ReactNode;
  /** Replaces any of the default strings. */
  messages?: Partial<UnsavedChangesMessages>;
  /** Controlled pending state, for a save the product tracks itself. */
  pending?: boolean;
  /** Controlled error message, shown above the actions. */
  error?: ReactNode;
  /** Turns a rejected `onSave` into a message. Default: the Error's message, or `messages.saveFailed`. */
  errorMessage?: (error: unknown) => ReactNode;
  /** Dialog width. Default `sm`. */
  size?: DialogContentProps['size'];
}

/**
 * The "you have unsaved changes" question, as a controlled alert dialog with
 * three answers: Keep editing (focused, the safe choice), Discard changes
 * (danger) and, when `onSave` is given, Save and leave. Use it directly with
 * a router-level navigation blocker; otherwise use `useUnsavedChangesGuard`
 * or `UnsavedChangesGuard`, which render it for you.
 */
export const UnsavedChangesDialog = forwardRef<
  ElementRef<typeof DialogContent>,
  UnsavedChangesDialogProps
>(function UnsavedChangesDialog(
  {
    open,
    onOpenChange,
    onStay,
    onLeave,
    onSave,
    children,
    messages: messagesProp,
    pending: pendingProp,
    error: errorProp,
    errorMessage,
    size = 'sm',
    closeOnOutsideClick = false,
    closeLabel = null,
    onOpenAutoFocus,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const [running, setRunning] = useState(false);
  const [failure, setFailure] = useState<ReactNode>(null);
  const keepRef = useRef<HTMLButtonElement>(null);
  const mounted = useRef(true);
  const errorId = useId();

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // Every opening starts without an old error.
  useEffect(() => {
    if (open) {
      setFailure(null);
      setRunning(false);
    }
  }, [open]);

  const pending = pendingProp ?? running;
  const shownError = errorProp ?? failure;

  const stay = () => {
    if (pending) return;
    onStay?.();
    onOpenChange?.(false);
  };

  const save = async () => {
    if (pending || !onSave) return;
    setFailure(null);
    try {
      const result = onSave();
      if (isThenable(result)) {
        setRunning(true);
        await result;
      }
    } catch (error) {
      if (mounted.current) {
        setRunning(false);
        setFailure(
          errorMessage
            ? errorMessage(error)
            : error instanceof Error && error.message
              ? error.message
              : messages.saveFailed,
        );
      }
      return;
    }
    if (mounted.current) setRunning(false);
    onLeave('save');
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) stay();
      }}
    >
      <DialogContent
        ref={ref}
        role="alertdialog"
        {...rest}
        size={size}
        busy={pending}
        closeLabel={closeLabel}
        closeOnOutsideClick={closeOnOutsideClick}
        className={cx('mn-unsaved-changes-guard', className)}
        onOpenAutoFocus={(event) => {
          onOpenAutoFocus?.(event);
          if (event.defaultPrevented) return;
          event.preventDefault();
          keepRef.current?.focus();
        }}
      >
        <DialogHeader className="mn-unsaved-changes-guard__header">
          <DialogTitle>{messages.title}</DialogTitle>
          <DialogDescription>{messages.description}</DialogDescription>
        </DialogHeader>
        {children ? <div className="mn-unsaved-changes-guard__body">{children}</div> : null}
        {shownError ? (
          <div className="mn-unsaved-changes-guard__error">
            <Alert tone="danger" live="assertive" id={errorId}>
              {shownError}
            </Alert>
          </div>
        ) : null}
        <DialogFooter className="mn-unsaved-changes-guard__footer">
          <Button
            variant="danger"
            className="mn-unsaved-changes-guard__discard"
            disabled={pending}
            onClick={() => onLeave('discard')}
          >
            {messages.discard}
          </Button>
          <Button ref={keepRef} disabled={pending} onClick={stay}>
            {messages.keepEditing}
          </Button>
          {onSave ? (
            <Button
              variant="primary"
              loading={pending}
              aria-describedby={shownError ? errorId : undefined}
              onClick={() => void save()}
            >
              {messages.save}
            </Button>
          ) : null}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
});

export interface UseUnsavedChangesGuardOptions {
  /** Whether there are unsaved changes. While `false`, `guard` proceeds at once and nothing is registered. */
  when: boolean;
  /**
   * Saves the changes from the dialog. When given, a "Save and leave" button
   * is shown; a returned promise keeps the dialog pending, and a rejection
   * shows the error and keeps the person on the page.
   */
  onSave?: () => unknown;
  /** Replaces any of the dialog's default strings. */
  messages?: Partial<UnsavedChangesMessages>;
  /**
   * Also guard clicks on same-origin links anywhere in the document (not
   * modified clicks, `target="_blank"`, `download` links, same-page `#hash`
   * links, or links marked `data-unsaved-changes="ignore"`). After the person
   * agrees to leave, the page navigates with `window.location.assign`.
   * Default `false`; prefer wrapping your router's navigation with `guard`.
   */
  interceptLinks?: boolean;
  /**
   * Ask the browser to warn before the tab is closed or reloaded while `when`
   * is true. Browsers show their own wording; custom text is ignored. Default `true`.
   */
  beforeUnload?: boolean;
}

export interface UseUnsavedChangesGuardResult {
  /**
   * Runs `proceed` straight away when there are no unsaved changes. Otherwise
   * asks first, and runs it only if the person discards or saves. Resolves
   * `true` when `proceed` ran and `false` when the person stayed.
   * Wrap navigation with it: `guard(() => router.push(href))`.
   */
  guard: (proceed: () => void) => Promise<boolean>;
  /** The dialog. Render it once, anywhere in the component. */
  element: ReactElement;
  /** Whether the dialog is open. */
  open: boolean;
}

/** The link a click should be guarded for, or null when the click is not a plain same-origin navigation. */
function guardedLink(event: MouseEvent): HTMLAnchorElement | null {
  if (event.defaultPrevented || event.button !== 0) return null;
  if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return null;
  const target = event.target instanceof Element ? event.target : null;
  const link = target?.closest('a[href]');
  if (!(link instanceof HTMLAnchorElement)) return null;
  if (link.hasAttribute('download')) return null;
  if (link.getAttribute('data-unsaved-changes') === 'ignore') return null;
  const linkTarget = link.getAttribute('target');
  if (linkTarget && linkTarget !== '_self') return null;
  let url: URL;
  try {
    url = new URL(link.href, window.location.href);
  } catch {
    return null;
  }
  if (url.origin !== window.location.origin) return null;
  const here = window.location;
  // A jump to an anchor on the same page does not leave it.
  if (url.pathname === here.pathname && url.search === here.search && url.hash !== '') return null;
  return link;
}

/**
 * Guards navigation away from a form or editor with unsaved changes, without
 * depending on a router. While `when` is true, `guard(proceed)` asks first
 * (Keep editing, Discard changes, and Save and leave when `onSave` is given),
 * the browser warns before the tab closes or reloads, and, with
 * `interceptLinks`, plain same-origin link clicks are asked about too.
 * A replacement for `window.confirm('You have unsaved changes')`.
 */
export function useUnsavedChangesGuard(
  options: UseUnsavedChangesGuardOptions,
): UseUnsavedChangesGuardResult {
  const { when, onSave, messages, interceptLinks = false, beforeUnload = true } = options;
  const [open, setOpen] = useState(false);
  const whenRef = useRef(when);
  whenRef.current = when;
  const proceedRef = useRef<(() => void) | null>(null);
  const resolver = useRef<((value: boolean) => void) | null>(null);
  const returnFocus = useRef<HTMLElement | null>(null);
  // Set once the person has agreed to leave, so the browser's own
  // before-unload warning does not ask a second time.
  const leaving = useRef(false);

  const settle = useCallback((value: boolean) => {
    resolver.current?.(value);
    resolver.current = null;
    proceedRef.current = null;
  }, []);

  useEffect(() => () => settle(false), [settle]);

  // A new dirty/clean cycle starts fresh.
  useEffect(() => {
    leaving.current = false;
  }, [when]);

  const guard = useCallback(
    (proceed: () => void) => {
      if (!whenRef.current) {
        proceed();
        return Promise.resolve(true);
      }
      settle(false);
      leaving.current = false;
      returnFocus.current = focusReturnTarget();
      proceedRef.current = proceed;
      setOpen(true);
      return new Promise<boolean>((resolve) => {
        resolver.current = resolve;
      });
    },
    [settle],
  );

  useEffect(() => {
    if (!when || !beforeUnload || typeof window === 'undefined') return;
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      if (leaving.current) return;
      event.preventDefault();
      // Older browsers need returnValue set; its text is never shown.
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => window.removeEventListener('beforeunload', onBeforeUnload);
  }, [when, beforeUnload]);

  useEffect(() => {
    if (!when || !interceptLinks || typeof document === 'undefined') return;
    const onClick = (event: MouseEvent) => {
      const link = guardedLink(event);
      if (!link) return;
      event.preventDefault();
      const href = link.href;
      void guard(() => window.location.assign(href));
    };
    // Capture, so this runs before a router's own link handler, which then
    // sees the click as already handled.
    document.addEventListener('click', onClick, true);
    return () => document.removeEventListener('click', onClick, true);
  }, [when, interceptLinks, guard]);

  const leave = () => {
    const proceed = proceedRef.current;
    leaving.current = true;
    returnFocus.current = null;
    setOpen(false);
    try {
      proceed?.();
    } finally {
      settle(true);
    }
  };

  const element = (
    <UnsavedChangesDialog
      open={open}
      {...(messages ? { messages } : {})}
      {...(onSave ? { onSave } : {})}
      onStay={() => {
        setOpen(false);
        settle(false);
      }}
      onLeave={leave}
      onCloseAutoFocus={(event) => {
        const target = returnFocus.current;
        if (!target?.isConnected) return;
        event.preventDefault();
        target.focus();
      }}
    />
  );

  return { guard, element, open };
}

/** What a ref to `UnsavedChangesGuard` exposes. */
export interface UnsavedChangesGuardHandle {
  /** The same as `guard` from `useUnsavedChangesGuard`. */
  guard: (proceed: () => void) => Promise<boolean>;
}

export type UnsavedChangesGuardProps = UseUnsavedChangesGuardOptions;

/**
 * The component form of `useUnsavedChangesGuard`: drop it into a form or
 * editor with `when={dirty}` to warn before the tab closes and (with
 * `interceptLinks`) before a link leaves the page. Its ref exposes `guard`
 * for the app's own navigation. Renders nothing until it has to ask.
 */
export const UnsavedChangesGuard = forwardRef<UnsavedChangesGuardHandle, UnsavedChangesGuardProps>(
  function UnsavedChangesGuard(props, ref) {
    const { guard, element } = useUnsavedChangesGuard(props);
    useImperativeHandle(ref, () => ({ guard }), [guard]);
    return element;
  },
);
