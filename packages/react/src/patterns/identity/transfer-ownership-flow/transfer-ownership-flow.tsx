'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type ElementRef,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import {
  ConsequenceList,
  type ConsequenceItem,
} from '../../../components/feedback/consequence-list/index.js';
import {
  Field,
  FieldLabel,
  NativeSelect,
  type NativeSelectOption,
} from '../../../components/forms/form-field/index.js';
import {
  AssigneePicker,
  type AssigneePerson,
} from '../../../components/identity/assignee-picker/index.js';
import { MemberRow } from '../../../components/identity/member-row/index.js';
import {
  useStepUpAuth,
  type StepUpRequirement,
} from '../../../components/identity/step-up-auth-dialog/index.js';
import {
  WizardDialog,
  type WizardDialogMessages,
  type WizardDialogProps,
  type WizardStep,
} from '../../../components/overlays/wizard-dialog/index.js';

/** A member who could become the owner. The product maps its own member records to this shape. */
export interface TransferOwnershipCandidate extends AssigneePerson {
  /** The member's current role id, compared with `eligibleRoles`. */
  role?: string;
  /** Why this member cannot become the owner. Shown in the list; the member cannot be picked. */
  ineligibleReason?: string;
}

/** What `onTransfer` receives. */
export interface TransferOwnershipRequest {
  /** The id of the member who becomes the owner. */
  memberId: string;
  /** Your role after the transfer, one of `yourRoleOptions`' values. Undefined without `yourRoleOptions`. */
  yourRole?: string;
}

/** What changes for each side, shown on the second step. */
export interface TransferOwnershipConsequences {
  /** What changes for you. */
  you: ConsequenceItem[];
  /** What changes for the new owner. */
  them: ConsequenceItem[];
}

/** Every visible string of the flow, for translation. */
export interface TransferOwnershipFlowMessages extends Partial<WizardDialogMessages> {
  /** Dialog title. Default "Transfer ownership". */
  title: string;
  /** First step heading. Default "Choose the new owner". */
  pickTitle: string;
  /** First step description. */
  pickDescription: string;
  /** Label of the member picker. Default "New owner". */
  pickLabel: string;
  /** Trigger text before anyone is picked. Default "Choose a member". */
  pickPlaceholder: string;
  /** Search box placeholder. Default "Search members". */
  pickSearch: string;
  /** Shown next to the disabled Continue on the first step. */
  pickRequired: string;
  /** Shown when there is nobody to pick. */
  noCandidates: string;
  /** Reason shown for members whose role is not in `eligibleRoles`. */
  roleNotEligible: string;
  /** Second step heading. Default "What changes". */
  reviewTitle: string;
  /** Heading of your consequences. Default "For you". */
  forYou: string;
  /** Heading of the new owner's consequences. Default "For <name>". */
  forThem: (name: string) => string;
  /** Label of the role select. Default "Your role after the transfer". */
  yourRoleLabel: string;
  /** Default consequence: your new role. */
  youBecome: (roleLabel: string | undefined) => string;
  /** Default consequence: you can no longer delete the workspace or transfer it again. */
  youLose: string;
  /** Default consequence: they become the owner. */
  theyBecome: (name: string) => string;
  /** Default consequence: they can delete the workspace. */
  theyCanDelete: (name: string) => string;
  /** Default consequence: they can change your role or remove you. */
  theyCanRemoveYou: (name: string) => string;
  /** Third step heading. Default "Confirm the transfer". */
  confirmTitle: string;
  /** Third step description. */
  confirmDescription: (name: string) => string;
  /** The confirm button. Default "Transfer ownership". */
  confirmLabel: string;
  /** Done heading when the new owner must accept. Default "Waiting for <name> to accept". */
  pendingTitle: (name: string) => string;
  /** Done description when the new owner must accept. */
  pendingDescription: (name: string) => string;
  /** Done heading when the transfer is immediate. Default "<name> is now the owner". */
  doneTitle: (name: string) => string;
  /** Done description when the transfer is immediate. */
  doneDescription: string;
  /** The cancel-transfer button on the waiting screen. Default "Cancel transfer". */
  cancelTransfer: string;
  /** Shown after the pending transfer is cancelled. */
  transferCancelled: string;
}

export const TRANSFER_OWNERSHIP_FLOW_MESSAGES: TransferOwnershipFlowMessages = {
  title: 'Transfer ownership',
  pickTitle: 'Choose the new owner',
  pickDescription: 'The new owner gets full control of the workspace, including billing.',
  pickLabel: 'New owner',
  pickPlaceholder: 'Choose a member',
  pickSearch: 'Search members',
  pickRequired: 'Choose a member to continue.',
  noCandidates: 'There is no other member who can become the owner. Invite someone first.',
  roleNotEligible: 'Only admins can become the owner',
  reviewTitle: 'What changes',
  forYou: 'For you',
  forThem: (name) => `For ${name}`,
  yourRoleLabel: 'Your role after the transfer',
  youBecome: (role) => (role ? `Your role changes to ${role}` : 'You become an admin'),
  youLose: 'You can no longer delete the workspace or transfer ownership',
  theyBecome: (name) => `${name} becomes the owner`,
  theyCanDelete: (name) => `${name} can delete the workspace`,
  theyCanRemoveYou: (name) => `${name} can change your role or remove you`,
  confirmTitle: 'Confirm the transfer',
  confirmDescription: (name) =>
    `Ownership moves to ${name}. Only the new owner can transfer it back.`,
  confirmLabel: 'Transfer ownership',
  pendingTitle: (name) => `Waiting for ${name} to accept`,
  pendingDescription: (name) =>
    `${name} has been asked to accept. You stay the owner until they do.`,
  doneTitle: (name) => `${name} is now the owner`,
  doneDescription: 'Your new role applies straight away.',
  cancelTransfer: 'Cancel transfer',
  transferCancelled: 'The transfer was cancelled. You are still the owner.',
};

export interface TransferOwnershipFlowProps extends Omit<
  WizardDialogProps,
  'title' | 'steps' | 'step' | 'defaultStep' | 'onStepChange' | 'onComplete' | 'messages'
> {
  /** Members who could become the owner, such as everyone in the workspace. */
  candidates: TransferOwnershipCandidate[];
  /** The current owner's id. Left out of the list. */
  currentOwnerId?: string;
  /**
   * Only members with one of these roles can be picked; the rest are listed,
   * disabled, with `messages.roleNotEligible` as the reason.
   */
  eligibleRoles?: readonly string[];
  /** The roles you can keep after the transfer. With them, a select appears on the second step. */
  yourRoleOptions?: readonly NativeSelectOption[];
  /** Your preselected role after the transfer. Default: the first of `yourRoleOptions`. */
  defaultYourRole?: string;
  /** Replaces the default consequences for the picked member and your chosen role. */
  consequences?: (
    member: TransferOwnershipCandidate,
    yourRole: NativeSelectOption | undefined,
  ) => TransferOwnershipConsequences;
  /** Re-authentication before the transfer runs. Cancelling it keeps the person on the confirm step. */
  stepUp?: StepUpRequirement;
  /** Runs the transfer. Return a promise; a rejection shows its message on the confirm step. */
  onTransfer: (request: TransferOwnershipRequest) => unknown;
  /**
   * The new owner must accept before the transfer takes effect. The closing
   * screen then says "Waiting for <name> to accept".
   */
  requiresAcceptance?: boolean;
  /** Withdraws a transfer that is waiting for acceptance. Shows a Cancel transfer button on the closing screen. */
  onCancelTransfer?: () => unknown;
  /** Called after a successful transfer, before the closing screen. */
  onTransferred?: (request: TransferOwnershipRequest) => void;
  /** Visible strings, for translation (WizardDialog strings included). */
  messages?: Partial<TransferOwnershipFlowMessages>;
}

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

/**
 * Hands a workspace to another member in three steps: pick the new owner,
 * review what changes for you and for them (and choose your own role), then
 * confirm, with optional re-authentication. The closing screen shows whether
 * the transfer took effect or waits for the new owner to accept. Built on
 * WizardDialog, AssigneePicker and ConsequenceList; the product runs the request.
 */
export const TransferOwnershipFlow = forwardRef<
  ElementRef<typeof WizardDialog>,
  TransferOwnershipFlowProps
>(function TransferOwnershipFlow(
  {
    candidates,
    currentOwnerId,
    eligibleRoles,
    yourRoleOptions,
    defaultYourRole,
    consequences,
    stepUp,
    onTransfer,
    requiresAcceptance = false,
    onCancelTransfer,
    onTransferred,
    messages: messagesProp,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...TRANSFER_OWNERSHIP_FLOW_MESSAGES, ...messagesProp };
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const [memberId, setMemberId] = useState<string | null>(null);
  const firstRole = defaultYourRole ?? yourRoleOptions?.[0]?.value;
  const [yourRole, setYourRole] = useState<string | undefined>(firstRole);
  const [cancelState, setCancelState] = useState<'idle' | 'pending' | 'done'>('idle');
  const [cancelError, setCancelError] = useState<string | null>(null);
  const mounted = useRef(true);
  const id = useId();
  const { requireStepUp, element: stepUpElement } = useStepUpAuth();

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // Every opening starts clean.
  useEffect(() => {
    if (open) {
      setMemberId(null);
      setYourRole(firstRole);
      setCancelState('idle');
      setCancelError(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange],
  );

  const people: TransferOwnershipCandidate[] = candidates
    .filter((c) => c.id !== currentOwnerId)
    .map((c) => {
      const reason =
        c.ineligibleReason ??
        (eligibleRoles && !(c.role !== undefined && eligibleRoles.includes(c.role))
          ? m.roleNotEligible
          : undefined);
      return reason ? { ...c, disabled: true, hint: reason } : c;
    });
  const member = people.find((p) => p.id === memberId && !p.disabled);
  const name = member?.name ?? '';
  const roleOption = yourRoleOptions?.find((r) => r.value === yourRole);
  const selectable = people.some((p) => !p.disabled);

  const changes: TransferOwnershipConsequences = member
    ? (consequences?.(member, roleOption) ?? {
        you: [
          { id: 'role', kind: 'changed', label: m.youBecome(roleOption?.label) },
          { id: 'lose', kind: 'warning', label: m.youLose },
        ],
        them: [
          { id: 'owner', kind: 'changed', label: m.theyBecome(name) },
          { id: 'delete', kind: 'warning', label: m.theyCanDelete(name) },
          { id: 'remove', kind: 'warning', label: m.theyCanRemoveYou(name) },
        ],
      })
    : { you: [], them: [] };

  const transfer = async () => {
    if (!member) return false;
    if (stepUp) {
      const verified = await requireStepUp(stepUp);
      if (!verified) return false;
    }
    const request: TransferOwnershipRequest =
      yourRole !== undefined ? { memberId: member.id, yourRole } : { memberId: member.id };
    await onTransfer(request);
    onTransferred?.(request);
    return true;
  };

  const cancelTransfer = async () => {
    if (!onCancelTransfer || cancelState !== 'idle') return;
    setCancelError(null);
    setCancelState('pending');
    try {
      await onCancelTransfer();
      if (mounted.current) setCancelState('done');
    } catch (error) {
      if (!mounted.current) return;
      setCancelState('idle');
      setCancelError(errorText(error, m.error ?? 'That did not work. Try again.'));
    }
  };

  const steps: WizardStep[] = [
    {
      id: 'pick',
      title: m.pickTitle,
      description: m.pickDescription,
      canContinue: member !== undefined,
      blockedReason: selectable ? m.pickRequired : undefined,
      content: selectable ? (
        <Field className="mn-transfer-ownership-flow__pick">
          <FieldLabel>{m.pickLabel}</FieldLabel>
          <AssigneePicker
            people={people}
            value={memberId}
            onValueChange={setMemberId}
            allowUnassigned={false}
            showAssignToMe={false}
            placeholder={m.pickPlaceholder}
            searchPlaceholder={m.pickSearch}
          />
        </Field>
      ) : (
        <Alert tone="warning">{m.noCandidates}</Alert>
      ),
    },
    {
      id: 'review',
      title: m.reviewTitle,
      content: member ? (
        <div className="mn-transfer-ownership-flow__review">
          {yourRoleOptions && yourRoleOptions.length > 0 ? (
            <Field>
              <FieldLabel>{m.yourRoleLabel}</FieldLabel>
              <NativeSelect
                options={yourRoleOptions}
                value={yourRole ?? ''}
                onChange={(event) => setYourRole(event.target.value)}
              />
            </Field>
          ) : null}
          <ConsequenceList heading={m.forYou} headingLevel="h4" items={changes.you} />
          <ConsequenceList heading={m.forThem(name)} headingLevel="h4" items={changes.them} />
        </div>
      ) : null,
    },
    {
      id: 'confirm',
      title: m.confirmTitle,
      description: member ? m.confirmDescription(name) : undefined,
      continueLabel: m.confirmLabel,
      tone: 'default',
      content: member ? (
        <MemberRow
          as="div"
          className="mn-transfer-ownership-flow__member"
          name={member.name}
          email={member.email}
          avatarUrl={member.avatarUrl ?? null}
        />
      ) : null,
      onContinue: transfer,
    },
    {
      id: 'done',
      kind: 'done',
      title:
        cancelState === 'done'
          ? m.transferCancelled
          : requiresAcceptance
            ? m.pendingTitle(name)
            : m.doneTitle(name),
      description:
        cancelState === 'done'
          ? undefined
          : requiresAcceptance
            ? m.pendingDescription(name)
            : m.doneDescription,
      content:
        requiresAcceptance && onCancelTransfer && cancelState !== 'done' ? (
          <div className="mn-transfer-ownership-flow__cancel">
            <Button
              loading={cancelState === 'pending'}
              onClick={() => void cancelTransfer()}
              aria-describedby={cancelError ? `${id}-cancel-error` : undefined}
            >
              {m.cancelTransfer}
            </Button>
            {cancelError ? (
              <Alert tone="danger" live="assertive" id={`${id}-cancel-error`}>
                {cancelError}
              </Alert>
            ) : null}
          </div>
        ) : null,
    },
  ];

  return (
    <>
      <WizardDialog
        ref={ref}
        {...rest}
        open={open}
        onOpenChange={setOpen}
        title={m.title}
        steps={steps}
        messages={m}
        data-acceptance={requiresAcceptance ? 'required' : undefined}
        className={cx('mn-transfer-ownership-flow', className)}
      />
      {stepUpElement}
    </>
  );
});
