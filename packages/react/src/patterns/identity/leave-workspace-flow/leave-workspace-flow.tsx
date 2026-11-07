'use client';

import {
  forwardRef,
  useCallback,
  useState,
  type ElementRef,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { WarningGlyph } from '../../../internal/icons.js';
import { Button } from '../../../components/actions/button/index.js';
import {
  ConsequenceList,
  type ConsequenceItem,
} from '../../../components/feedback/consequence-list/index.js';
import {
  ConfirmDialog,
  type ConfirmDialogProps,
} from '../../../components/overlays/confirm-dialog/index.js';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '../../../components/overlays/dialog/index.js';

/** Every visible string of the flow, for translation. */
export interface LeaveWorkspaceFlowMessages {
  /** Confirmation title. Default "Leave <workspace>?" or "Leave this workspace?". */
  title: (workspaceName: string | undefined) => string;
  /** Confirmation description. */
  description: string;
  /** Default consequence: the apps you lose access to. */
  loseAccessTo: (apps: readonly string[]) => string;
  /** Default consequence without `apps`. */
  loseAccess: string;
  /** Default consequence: what you created stays. */
  contentStays: string;
  /** Default consequence: coming back needs an invite. */
  needInvite: string;
  /** The leave button. Default "Leave workspace". */
  confirm: string;
  /** Default "Cancel". */
  cancel: string;
  /** Title of the blocked dialog. Default "You can’t leave yet". */
  blockedTitle: string;
  /** Reason shown when `isLastOwner` and no `blockedReason` is given. */
  lastOwner: string;
  /** Close button of the blocked dialog. Default "Close". */
  close: string;
}

export const LEAVE_WORKSPACE_FLOW_MESSAGES: LeaveWorkspaceFlowMessages = {
  title: (name) => (name ? `Leave ${name}?` : 'Leave this workspace?'),
  description: 'You are removed from the workspace straight away.',
  loseAccessTo: (apps) => `You lose access to ${new Intl.ListFormat('en').format(apps)}`,
  loseAccess: 'You lose access to everything in the workspace',
  contentStays: 'What you created stays in the workspace for the other members',
  needInvite: 'You need a new invite to come back',
  confirm: 'Leave workspace',
  cancel: 'Cancel',
  blockedTitle: 'You can’t leave yet',
  lastOwner: 'You are the only owner. Transfer ownership to another member before you leave.',
  close: 'Close',
};

export interface LeaveWorkspaceFlowProps extends Omit<
  ConfirmDialogProps,
  | 'title'
  | 'description'
  | 'consequences'
  | 'tone'
  | 'confirmLabel'
  | 'cancelLabel'
  | 'onConfirm'
  | 'children'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement;
  /** Name of the workspace, used in the title. */
  workspaceName?: string;
  /** Names of the apps or areas the person loses, listed in the first consequence. */
  apps?: readonly string[];
  /** Replaces the default consequences (lose access, content stays, invite needed). */
  consequences?: ConsequenceItem[];
  /**
   * The person is the last owner. Leaving is blocked: the dialog explains why
   * (`blockedReason` or `messages.lastOwner`), shows `fixAction` and has no
   * leave button.
   */
  isLastOwner?: boolean;
  /** Any other reason leaving is blocked right now. Blocks like `isLastOwner`. */
  blockedReason?: ReactNode;
  /** The way out of the blocked state, such as a button that opens TransferOwnershipFlow. */
  fixAction?: ReactNode;
  /**
   * Leaves the workspace. Return a promise to keep the dialog pending; a
   * rejection shows its message in the dialog. The dialog closes on success.
   */
  onLeave: () => unknown;
  /** Visible strings, for translation. */
  messages?: Partial<LeaveWorkspaceFlowMessages>;
}

/**
 * Lets a member leave a workspace. A single confirmation (danger tone) lists
 * what they lose and what stays, then runs `onLeave`. When leaving is not
 * allowed (they are the last owner, or the product gives a reason), the same
 * trigger opens a plain dialog that says why and offers the fix instead of a
 * leave button. Built on ConfirmDialog and Dialog.
 */
export const LeaveWorkspaceFlow = forwardRef<
  ElementRef<typeof DialogContent>,
  LeaveWorkspaceFlowProps
>(function LeaveWorkspaceFlow(
  {
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    trigger,
    workspaceName,
    apps,
    consequences,
    isLastOwner = false,
    blockedReason,
    fixAction,
    onLeave,
    onCancel,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...LEAVE_WORKSPACE_FLOW_MESSAGES, ...messagesProp };
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange],
  );

  const hasReason =
    blockedReason !== undefined && blockedReason !== null && blockedReason !== false;
  const blocked = isLastOwner || hasReason;

  if (blocked) {
    const {
      size: _size,
      confirmPhrase: _p,
      confirmPhraseLabel: _pl,
      confirmPhraseMismatch: _pm,
      acknowledgement: _a,
      acknowledgementMissing: _am,
      reason: _r,
      pending: _pending,
      error: _error,
      errorMessage: _em,
      closeOnConfirm: _c,
      initialFocus: _if,
      ...dialogProps
    } = rest;
    return (
      <Dialog
        open={open}
        onOpenChange={(next) => {
          if (!next) onCancel?.();
          setOpen(next);
        }}
      >
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          closeLabel={null}
          {...dialogProps}
          size="sm"
          data-blocked=""
          className={cx('mn-leave-workspace-flow', className)}
        >
          <DialogHeader className="mn-leave-workspace-flow__header">
            <span className="mn-leave-workspace-flow__icon" aria-hidden>
              <WarningGlyph />
            </span>
            <DialogTitle>{m.blockedTitle}</DialogTitle>
            <DialogDescription>{hasReason ? blockedReason : m.lastOwner}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button>{m.close}</Button>
            </DialogClose>
            {fixAction}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  const items: ConsequenceItem[] = consequences ?? [
    {
      id: 'access',
      kind: 'removed',
      label: apps && apps.length > 0 ? m.loseAccessTo(apps) : m.loseAccess,
    },
    { id: 'content', kind: 'kept', label: m.contentStays },
    { id: 'invite', kind: 'warning', label: m.needInvite },
  ];

  return (
    <ConfirmDialog
      ref={ref}
      {...rest}
      open={open}
      onOpenChange={setOpen}
      {...(trigger ? { trigger } : {})}
      {...(onCancel ? { onCancel } : {})}
      tone="danger"
      title={m.title(workspaceName)}
      description={m.description}
      confirmLabel={m.confirm}
      cancelLabel={m.cancel}
      onConfirm={() => onLeave()}
      className={cx('mn-leave-workspace-flow', className)}
    >
      <ConsequenceList items={items} className="mn-leave-workspace-flow__consequences" />
    </ConfirmDialog>
  );
});
