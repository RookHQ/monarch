'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
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

/** What deleting the workspace removes. Leave a count out (or 0) to hide its line. */
export interface DeleteWorkspaceImpact {
  /** Apps in the workspace. */
  apps?: number;
  /** Members who lose access. */
  members?: number;
  /** Records (items, rows, documents) in the workspace. */
  records?: number;
  /** Active paid subscriptions that are cancelled. */
  subscriptions?: number;
  /** Custom domains that are disconnected. */
  domains?: number;
}

/** Every visible string of the DeleteWorkspaceFlow, for translation. */
export interface DeleteWorkspaceFlowMessages {
  /** Dialog title and accessible name. Default "Delete workspace". */
  title: string;
  /** Impact step heading. Default "What will be deleted". */
  impactTitle: string;
  /** Impact step description for immediate deletion. */
  impactDescription: (workspaceName: string) => ReactNode;
  /** Impact step description when `gracePeriodDays` is set. `date` is already formatted. */
  scheduledImpactDescription: (workspaceName: string, days: number, date: string) => ReactNode;
  /** Shown while the impact is loading and Continue is disabled. */
  impactLoading: string;
  /** Text after the count of apps. */
  apps: (count: number) => string;
  /** Text after the count of members. */
  members: (count: number) => string;
  /** Text after the count of records. */
  records: (count: number) => string;
  /** Text after the count of active subscriptions. */
  subscriptions: (count: number) => string;
  /** Text after the count of custom domains. */
  domains: (count: number) => string;
  /** Export step heading. Default "Keep a copy". */
  exportTitle: string;
  exportDescription: string;
  /** Export button. Default "Export data". */
  exportButton: string;
  /** Announced after `onExport` resolves. */
  exportStarted: string;
  /** Shown when `onExport` rejects. */
  exportFailed: string;
  /** Transfer step heading. Default "Transfer it instead?". */
  transferTitle: string;
  transferDescription: string;
  /** Button that closes the flow and calls `onTransferInstead`. */
  transferButton: string;
  /** Confirm step heading. Default "Confirm deletion". */
  confirmTitle: string;
  /** Confirm step description for immediate deletion. */
  confirmDescription: string;
  /** Confirm step description when `gracePeriodDays` is set. */
  scheduledConfirmDescription: (days: number, date: string) => ReactNode;
  /** Label of the name field. `name` is the workspace name, already styled. */
  typeToConfirm: (name: ReactNode) => ReactNode;
  /** Shown beside the disabled button until the name matches. */
  confirmBlocked: string;
  /** Destructive button for immediate deletion. Default "Delete workspace". */
  deleteAction: string;
  /** Destructive button when `gracePeriodDays` is set. Default "Schedule deletion". */
  scheduleAction: string;
  /** Description inside the step-up dialog. */
  stepUpDescription: string;
  /** Done heading after immediate deletion. */
  doneTitle: string;
  doneDescription: (workspaceName: string) => ReactNode;
  /** Done heading after scheduled deletion. */
  scheduledDoneTitle: string;
  scheduledDoneDescription: (workspaceName: string, date: string) => ReactNode;
}

const plural = (count: number, one: string, other: string) => (count === 1 ? one : other);

export const DELETE_WORKSPACE_FLOW_MESSAGES: DeleteWorkspaceFlowMessages = {
  title: 'Delete workspace',
  impactTitle: 'What will be deleted',
  impactDescription: (name) =>
    `Deleting ${name} removes it and everything in it for every member. This cannot be undone.`,
  scheduledImpactDescription: (name, days, date) =>
    `${name} will be scheduled for deletion in ${days} ${plural(days, 'day', 'days')}. You can cancel until ${date}; after that, this cannot be undone.`,
  impactLoading: 'Wait for the summary to load.',
  apps: (n) => plural(n, 'app', 'apps'),
  members: (n) => plural(n, 'member loses access', 'members lose access'),
  records: (n) => plural(n, 'record', 'records'),
  subscriptions: (n) =>
    plural(n, 'active subscription is cancelled', 'active subscriptions are cancelled'),
  domains: (n) => plural(n, 'custom domain is disconnected', 'custom domains are disconnected'),
  exportTitle: 'Keep a copy',
  exportDescription:
    'Download the workspace data before it is deleted. This is optional; you can continue without it.',
  exportButton: 'Export data',
  exportStarted: 'Export started.',
  exportFailed: 'The export could not be started. Try again.',
  transferTitle: 'Transfer it instead?',
  transferDescription:
    'If someone else should keep the workspace, make them the owner instead of deleting it.',
  transferButton: 'Transfer ownership instead',
  confirmTitle: 'Confirm deletion',
  confirmDescription: 'This cannot be undone.',
  scheduledConfirmDescription: (days, date) =>
    `The workspace is deleted in ${days} ${plural(days, 'day', 'days')}. You can cancel until ${date}.`,
  typeToConfirm: (name) => <>Type {name} to confirm</>,
  confirmBlocked: 'Type the workspace name to continue.',
  deleteAction: 'Delete workspace',
  scheduleAction: 'Schedule deletion',
  stepUpDescription: 'Deleting a workspace needs you to confirm your identity.',
  doneTitle: 'Workspace deleted',
  doneDescription: (name) => `${name} and everything in it has been deleted.`,
  scheduledDoneTitle: 'Deletion scheduled',
  scheduledDoneDescription: (name, date) =>
    `${name} will be deleted on ${date}. Until then, an owner can cancel the deletion.`,
};

export interface DeleteWorkspaceFlowProps extends Omit<
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
  /** The workspace's name. Shown in the copy and typed by the person to confirm. */
  workspaceName: string;
  /** Counts for the impact summary. Each count above 0 becomes one line. */
  impact?: DeleteWorkspaceImpact;
  /** Extra impact lines after the counts, e.g. something product-specific. */
  extraConsequences?: ConsequenceItem[];
  /** The impact counts are still loading: skeleton rows, and Continue waits. */
  impactLoading?: boolean;
  /** The impact could not be loaded, e.g. an `InlineErrorRetry`. Shown instead of the list. */
  impactError?: ReactNode;
  /**
   * Starts a data export. Adds an optional "Keep a copy" step; leave it out to
   * skip the step. Return a promise to show progress on the button.
   */
  onExport?: () => unknown;
  /**
   * Adds a "Transfer it instead?" step. Its button closes this flow and calls
   * this, so the app can open its transfer-ownership flow.
   */
  onTransferInstead?: () => void;
  /**
   * Re-authentication before the delete runs (`methods`, `onVerify`, …), the shape
   * every account flow shares. When given,
   * the destructive button opens the step-up dialog first, and the delete
   * only runs once it succeeds.
   */
  stepUp?: StepUpRequirement;
  /**
   * Deletes (or schedules the deletion of) the workspace. Return a promise:
   * the flow stays pending until it settles, shows a rejection's message on
   * the confirm step, and moves to the done screen on success.
   */
  onDelete: () => unknown;
  /**
   * Called when the done screen is closed, after a successful delete, e.g. to
   * navigate away from the deleted workspace.
   */
  onDone?: () => void;
  /**
   * The backend deletes after a grace period. The copy then says "scheduled
   * for deletion in N days, you can cancel until <date>", and so does the
   * done screen.
   */
  gracePeriodDays?: number;
  /** The exact scheduled date, when the backend knows it. Default: today plus `gracePeriodDays`. */
  deletionDate?: Date;
  /** Locale for counts and the date. Default: the runtime's locale. */
  locale?: string;
  /** Visible strings, for translation. */
  messages?: Partial<DeleteWorkspaceFlowMessages>;
  /** Strings of the underlying WizardDialog (Back, Cancel, Continue, Done, progress). */
  wizardMessages?: WizardDialogProps['messages'];
}

const DAY = 24 * 60 * 60 * 1000;

/**
 * The full "delete this workspace" flow: what will be deleted, an optional
 * export, an optional "transfer ownership instead" detour, type the workspace
 * name, optional re-authentication, then the delete and a closing screen.
 * Supports deletion that is scheduled after a grace period. The product loads
 * the counts and runs the request; the flow only presents the steps.
 */
export const DeleteWorkspaceFlow = forwardRef<
  ElementRef<typeof WizardDialog>,
  DeleteWorkspaceFlowProps
>(function DeleteWorkspaceFlow(
  {
    workspaceName,
    impact,
    extraConsequences,
    impactLoading = false,
    impactError,
    onExport,
    onTransferInstead,
    stepUp,
    onDelete,
    onDone,
    gracePeriodDays,
    deletionDate,
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
  const m = { ...DELETE_WORKSPACE_FLOW_MESSAGES, ...messagesProp };
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const [typed, setTyped] = useState('');
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
      setExportState('idle');
    }
  }, [open]);

  const scheduled = gracePeriodDays !== undefined && gracePeriodDays > 0;
  // "Today" is taken when the flow opens, so the date does not drift while it is open.
  const [openedAt, setOpenedAt] = useState(() => Date.now());
  useEffect(() => {
    if (open) setOpenedAt(Date.now());
  }, [open]);
  const dateText = useMemo(() => {
    if (!scheduled) return '';
    const date = deletionDate ?? new Date(openedAt + (gracePeriodDays ?? 0) * DAY);
    return new Intl.DateTimeFormat(locale, { dateStyle: 'long' }).format(date);
  }, [scheduled, deletionDate, gracePeriodDays, locale, openedAt]);

  const items: ConsequenceItem[] = [];
  const add = (id: keyof DeleteWorkspaceImpact, kind: ConsequenceItem['kind']) => {
    const count = impact?.[id];
    if (count !== undefined && count > 0) items.push({ id, kind, count, label: m[id](count) });
  };
  add('apps', 'removed');
  add('records', 'removed');
  add('members', 'changed');
  add('subscriptions', 'changed');
  add('domains', 'changed');
  if (extraConsequences) items.push(...extraConsequences);

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

  const matches = phraseMatches(typed, workspaceName);

  const steps: WizardStep[] = [
    {
      id: 'impact',
      title: m.impactTitle,
      description: scheduled
        ? m.scheduledImpactDescription(workspaceName, gracePeriodDays, dateText)
        : m.impactDescription(workspaceName),
      content: (
        <ConsequenceList
          items={items}
          groupBy="kind"
          headingLevel="h4"
          loading={impactLoading}
          error={impactError}
          {...(locale !== undefined ? { locale } : {})}
        />
      ),
      canContinue: !impactLoading,
      blockedReason: m.impactLoading,
    },
  ];
  if (onExport) {
    steps.push({
      id: 'export',
      title: m.exportTitle,
      description: m.exportDescription,
      content: (
        <div className="mn-delete-workspace-flow__action">
          <Button loading={exportState === 'pending'} onClick={runExport}>
            {m.exportButton}
          </Button>
          <span className="mn-delete-workspace-flow__status" role="status">
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
  if (onTransferInstead) {
    steps.push({
      id: 'transfer',
      title: m.transferTitle,
      description: m.transferDescription,
      content: (
        <div className="mn-delete-workspace-flow__action">
          <Button
            onClick={() => {
              setOpen(false);
              onTransferInstead();
            }}
          >
            {m.transferButton}
          </Button>
        </div>
      ),
    });
  }
  steps.push(
    {
      id: 'confirm',
      title: m.confirmTitle,
      description: scheduled
        ? m.scheduledConfirmDescription(gracePeriodDays, dateText)
        : m.confirmDescription,
      content: (
        <TypeToConfirmInput
          phrase={workspaceName}
          value={typed}
          onValueChange={setTyped}
          label={m.typeToConfirm(
            <span className="mn-type-to-confirm-input__phrase">{workspaceName}</span>,
          )}
        />
      ),
      canContinue: matches,
      blockedReason: m.confirmBlocked,
      tone: 'danger',
      continueLabel: scheduled ? m.scheduleAction : m.deleteAction,
      onContinue: async () => {
        if (stepUp) {
          const verified = await requireStepUp({ description: m.stepUpDescription, ...stepUp });
          if (!verified) return false;
        }
        await onDelete();
        deleted.current = true;
        return true;
      },
    },
    {
      id: 'done',
      kind: 'done',
      title: scheduled ? m.scheduledDoneTitle : m.doneTitle,
      content: (
        <p className="mn-delete-workspace-flow__done">
          {scheduled
            ? m.scheduledDoneDescription(workspaceName, dateText)
            : m.doneDescription(workspaceName)}
        </p>
      ),
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
        data-scheduled={scheduled || undefined}
        className={cx('mn-delete-workspace-flow', className)}
      />
      {stepUpElement}
    </>
  );
});
