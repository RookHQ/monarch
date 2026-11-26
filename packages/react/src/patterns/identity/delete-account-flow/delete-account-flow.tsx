'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type ElementRef,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../../components/actions/button/index.js';
import {
  ConsequenceList,
  type ConsequenceItem,
} from '../../../components/feedback/consequence-list/index.js';
import { Field, FieldLabel } from '../../../components/forms/form-field/index.js';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from '../../../components/forms/select/index.js';
import { Textarea } from '../../../components/forms/text-field/index.js';
import {
  TypeToConfirmInput,
  phraseMatches,
} from '../../../components/forms/type-to-confirm-input/index.js';
import {
  useStepUpAuth,
  type StepUpRequirement,
} from '../../../components/identity/step-up-auth-dialog/index.js';
import {
  WizardDialog,
  type WizardDialogProps,
  type WizardStep,
} from '../../../components/overlays/wizard-dialog/index.js';

/** A workspace the person owns alone, which blocks deleting the account. */
export interface OwnedWorkspace {
  /** Stable key. */
  id: string;
  /** Workspace name, shown in the list. */
  name: string;
  /** How many members the workspace has. */
  memberCount: number;
}

/** One choice in the "why are you leaving" select. */
export interface DeleteAccountReason {
  value: string;
  label: string;
}

/** What `onDelete` receives. Both fields are absent when left empty. */
export interface DeleteAccountDetails {
  /** The chosen reason's `value`. */
  reason?: string;
  /** The trimmed written comment. */
  details?: string;
}

/** Every visible string of the DeleteAccountFlow, for translation. */
export interface DeleteAccountFlowMessages {
  /** Dialog title and accessible name. Default "Delete account". */
  title: string;
  /** First step heading. Default "Before you delete your account". */
  reviewTitle: string;
  /** First step description. */
  reviewDescription: string;
  /** Introduces the list of workspaces the person owns alone. */
  ownedIntro: string;
  /** Member count line of an owned workspace. `formatted` is the count in `locale`. */
  memberCount: (count: number, formatted: string) => string;
  /** Visible label of the default action button of an owned workspace. */
  resolveWorkspace: string;
  /** Accessible name of that button; must start with `resolveWorkspace`. */
  resolveWorkspaceLabel: (workspaceName: string) => string;
  /** Shown beside the disabled Continue while owned workspaces remain. */
  ownedBlocked: string;
  /** Export step heading. Default "Download your data". */
  exportTitle: string;
  exportDescription: string;
  /** Export button. Default "Export data". */
  exportButton: string;
  /** Announced after `onExport` resolves. */
  exportStarted: string;
  /** Shown when `onExport` rejects. */
  exportFailed: string;
  /** Reason step heading. Default "Why are you leaving?". */
  reasonTitle: string;
  reasonDescription: string;
  /** Label of the reason select. Default "Reason". */
  reasonLabel: string;
  /** Placeholder of the reason select. Default "Choose a reason". */
  reasonPlaceholder: string;
  /** Label of the comment field. Default "Anything else you want to tell us?". */
  detailsLabel: string;
  /** Confirm step heading. Default "Confirm deletion". */
  confirmTitle: string;
  confirmDescription: string;
  /** Label of the confirmation field. `phrase` is already styled. */
  typeToConfirm: (phrase: ReactNode) => ReactNode;
  /** Shown beside the disabled button until the text matches. */
  confirmBlocked: string;
  /** Destructive button. Default "Delete account". */
  deleteAction: string;
  /** Description inside the step-up dialog. */
  stepUpDescription: string;
  /** Farewell heading. Default "Account deleted". */
  doneTitle: string;
  /** Farewell text. */
  doneDescription: string;
}

export const DELETE_ACCOUNT_FLOW_MESSAGES: DeleteAccountFlowMessages = {
  title: 'Delete account',
  reviewTitle: 'Before you delete your account',
  reviewDescription:
    'Your account and your access to every workspace will be removed. This cannot be undone.',
  ownedIntro:
    'You are the only owner of these workspaces. Transfer each one to someone else, or delete it, before you delete your account.',
  memberCount: (count, formatted) => `${formatted} ${count === 1 ? 'member' : 'members'}`,
  resolveWorkspace: 'Transfer or delete',
  resolveWorkspaceLabel: (name) => `Transfer or delete ${name}`,
  ownedBlocked: 'Transfer or delete the workspaces you own first.',
  exportTitle: 'Download your data',
  exportDescription:
    'Get a copy of your data before your account is deleted. This is optional; you can continue without it.',
  exportButton: 'Export data',
  exportStarted: 'Export started.',
  exportFailed: 'The export could not be started. Try again.',
  reasonTitle: 'Why are you leaving?',
  reasonDescription: 'Optional. Your answer helps us improve.',
  reasonLabel: 'Reason',
  reasonPlaceholder: 'Choose a reason',
  detailsLabel: 'Anything else you want to tell us?',
  confirmTitle: 'Confirm deletion',
  confirmDescription: 'Your account is deleted permanently. This cannot be undone.',
  typeToConfirm: (phrase) => <>Type {phrase} to confirm</>,
  confirmBlocked: 'Type the confirmation text to continue.',
  deleteAction: 'Delete account',
  stepUpDescription: 'Deleting your account needs you to confirm your identity.',
  doneTitle: 'Account deleted',
  doneDescription: 'Your account has been removed. Thank you for the time you spent with us.',
};

export interface DeleteAccountFlowProps extends Omit<
  WizardDialogProps,
  | 'title'
  | 'description'
  | 'steps'
  | 'step'
  | 'defaultStep'
  | 'onStepChange'
  | 'onComplete'
  | 'messages'
> {
  /**
   * The text the person types to confirm, usually their account email.
   * Matching is case-sensitive and ignores surrounding spaces.
   */
  confirmPhrase: string;
  /**
   * Workspaces the person owns alone. While the list is not empty the first
   * step is blocked and lists them, each with an action to resolve it. Update
   * the list as they are transferred or deleted and the step unblocks.
   */
  ownedWorkspaces?: OwnedWorkspace[];
  /** Renders the action beside an owned workspace, e.g. a menu with "Transfer" and "Delete". Wins over `onResolveWorkspace`. */
  renderWorkspaceAction?: (workspace: OwnedWorkspace) => ReactNode;
  /** Called by the default "Transfer or delete" button beside an owned workspace. */
  onResolveWorkspace?: (workspace: OwnedWorkspace) => void;
  /** What deleting the account does, shown on the first step. */
  consequences?: ConsequenceItem[];
  /**
   * Starts a data export. Adds an optional "Download your data" step; leave
   * it out to skip the step. Return a promise to show progress on the button.
   */
  onExport?: () => unknown;
  /** Choices for "Why are you leaving?". Adds the optional reason step; leave it out to skip the step. */
  reasons?: DeleteAccountReason[];
  /** Show the comment field on the reason step. Default `true`. */
  showDetails?: boolean;
  /**
   * Re-authentication before the delete runs (`methods`, `onVerify`, …), the shape
   * every account flow shares. When given,
   * the destructive button opens the step-up dialog first, and the delete
   * only runs once it succeeds.
   */
  stepUp?: StepUpRequirement;
  /**
   * Deletes the account, with the optional reason and comment. Return a
   * promise: the flow stays pending until it settles, shows a rejection's
   * message on the confirm step, and moves to the farewell screen on success.
   */
  onDelete: (details: DeleteAccountDetails) => unknown;
  /** Called when the farewell screen is closed, e.g. to sign out and leave the page. */
  onDone?: () => void;
  /** Locale for member counts. Default: the runtime's locale. */
  locale?: string;
  /** Visible strings, for translation. */
  messages?: Partial<DeleteAccountFlowMessages>;
  /** Strings of the underlying WizardDialog (Back, Cancel, Continue, Done, progress). */
  wizardMessages?: WizardDialogProps['messages'];
}

/**
 * The full "delete my account" flow: a check that blocks while the person
 * still owns workspaces alone (with an action to resolve each), an optional
 * data export, an optional reason, typing a confirmation phrase (usually the
 * account email), optional re-authentication, then the delete and a farewell
 * screen. The product loads the data and runs the request.
 */
export const DeleteAccountFlow = forwardRef<
  ElementRef<typeof WizardDialog>,
  DeleteAccountFlowProps
>(function DeleteAccountFlow(
  {
    confirmPhrase,
    ownedWorkspaces = [],
    renderWorkspaceAction,
    onResolveWorkspace,
    consequences,
    onExport,
    reasons,
    showDetails = true,
    stepUp,
    onDelete,
    onDone,
    locale,
    messages: messagesProp,
    wizardMessages,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DELETE_ACCOUNT_FLOW_MESSAGES, ...messagesProp };
  const id = useId();
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const [typed, setTyped] = useState('');
  const [reason, setReason] = useState('');
  const [details, setDetails] = useState('');
  const [exportState, setExportState] = useState<'idle' | 'pending' | 'done' | 'failed'>('idle');
  const { requireStepUp, element: stepUpElement } = useStepUpAuth();
  const deleted = useRef(false);

  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
      if (!next && deleted.current) {
        deleted.current = false;
        onDone?.();
      }
    },
    [openProp, onOpenChange, onDone],
  );

  // Every opening starts clean.
  useEffect(() => {
    if (open) {
      deleted.current = false;
      setTyped('');
      setReason('');
      setDetails('');
      setExportState('idle');
    }
  }, [open]);

  const runExport = async () => {
    if (!onExport || exportState === 'pending') return;
    setExportState('pending');
    try {
      await onExport();
      setExportState('done');
    } catch {
      setExportState('failed');
    }
  };

  const format = new Intl.NumberFormat(locale);
  const owned = ownedWorkspaces.length > 0;

  const steps: WizardStep[] = [
    {
      id: 'review',
      title: m.reviewTitle,
      description: m.reviewDescription,
      content: (
        <>
          {owned ? (
            <div className="mn-delete-account-flow__owned" data-blocked>
              <p id={`${id}-owned`} className="mn-delete-account-flow__owned-intro">
                {m.ownedIntro}
              </p>
              <ul className="mn-delete-account-flow__workspaces" aria-labelledby={`${id}-owned`}>
                {ownedWorkspaces.map((ws) => {
                  const action = renderWorkspaceAction ? (
                    renderWorkspaceAction(ws)
                  ) : onResolveWorkspace ? (
                    <Button
                      size="sm"
                      aria-label={m.resolveWorkspaceLabel(ws.name)}
                      onClick={() => onResolveWorkspace(ws)}
                    >
                      {m.resolveWorkspace}
                    </Button>
                  ) : null;
                  return (
                    <li key={ws.id} className="mn-delete-account-flow__workspace">
                      <span className="mn-delete-account-flow__workspace-text">
                        <span className="mn-delete-account-flow__workspace-name">{ws.name}</span>
                        <span className="mn-delete-account-flow__workspace-meta">
                          {m.memberCount(ws.memberCount, format.format(ws.memberCount))}
                        </span>
                      </span>
                      {action ? (
                        <span className="mn-delete-account-flow__workspace-action">{action}</span>
                      ) : null}
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : null}
          {consequences && consequences.length > 0 ? (
            <ConsequenceList
              items={consequences}
              groupBy="kind"
              headingLevel="h4"
              {...(locale !== undefined ? { locale } : {})}
            />
          ) : null}
        </>
      ),
      canContinue: !owned,
      blockedReason: m.ownedBlocked,
    },
  ];

  if (onExport) {
    steps.push({
      id: 'export',
      title: m.exportTitle,
      description: m.exportDescription,
      content: (
        <div className="mn-delete-account-flow__action">
          <Button loading={exportState === 'pending'} onClick={runExport}>
            {m.exportButton}
          </Button>
          <span className="mn-delete-account-flow__status" role="status">
            {exportState === 'done'
              ? m.exportStarted
              : exportState === 'failed'
                ? m.exportFailed
                : null}
          </span>
        </div>
      ),
    });
  }

  if (reasons && reasons.length > 0) {
    steps.push({
      id: 'reason',
      title: m.reasonTitle,
      description: m.reasonDescription,
      content: (
        <div className="mn-delete-account-flow__fields">
          <Field>
            <FieldLabel>{m.reasonLabel}</FieldLabel>
            <Select value={reason} onValueChange={setReason}>
              <SelectTrigger placeholder={m.reasonPlaceholder} />
              <SelectContent>
                {reasons.map((r) => (
                  <SelectItem key={r.value} value={r.value}>
                    {r.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          {showDetails ? (
            <Field>
              <FieldLabel>{m.detailsLabel}</FieldLabel>
              <Textarea
                value={details}
                onChange={(event) => setDetails(event.target.value)}
                rows={3}
                maxLength={1000}
              />
            </Field>
          ) : null}
        </div>
      ),
    });
  }

  steps.push(
    {
      id: 'confirm',
      title: m.confirmTitle,
      description: m.confirmDescription,
      content: (
        <TypeToConfirmInput
          phrase={confirmPhrase}
          value={typed}
          onValueChange={setTyped}
          label={m.typeToConfirm(
            <span className="mn-type-to-confirm-input__phrase">{confirmPhrase}</span>,
          )}
        />
      ),
      canContinue: phraseMatches(typed, confirmPhrase),
      blockedReason: m.confirmBlocked,
      tone: 'danger',
      continueLabel: m.deleteAction,
      onContinue: async () => {
        if (stepUp) {
          const verified = await requireStepUp({ description: m.stepUpDescription, ...stepUp });
          if (!verified) return false;
        }
        const payload: DeleteAccountDetails = {};
        if (reason !== '') payload.reason = reason;
        const trimmed = details.trim();
        if (showDetails && trimmed !== '') payload.details = trimmed;
        await onDelete(payload);
        deleted.current = true;
        return true;
      },
    },
    {
      id: 'done',
      kind: 'done',
      title: m.doneTitle,
      content: <p className="mn-delete-account-flow__done">{m.doneDescription}</p>,
    },
  );

  return (
    <>
      <WizardDialog
        ref={ref}
        {...rest}
        open={open}
        onOpenChange={setOpen}
        title={m.title}
        steps={steps}
        {...(wizardMessages ? { messages: wizardMessages } : {})}
        data-blocked={owned || undefined}
        className={cx('mn-delete-account-flow', className)}
      />
      {stepUpElement}
    </>
  );
});
