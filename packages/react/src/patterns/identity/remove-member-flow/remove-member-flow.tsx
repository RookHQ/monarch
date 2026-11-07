'use client';

import { forwardRef, useCallback, useEffect, useRef, useState, type ElementRef } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ConsequenceList,
  type ConsequenceItem,
} from '../../../components/feedback/consequence-list/index.js';
import {
  useUndoToast,
  type UndoableOptions,
} from '../../../components/feedback/undo-toast/index.js';
import { Checkbox } from '../../../components/forms/checkbox/index.js';
import { Field, FieldLabel } from '../../../components/forms/form-field/index.js';
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

/** The member being removed. */
export interface RemoveMemberTarget {
  id: string;
  name: string;
  /** Shown under the name. */
  email?: string;
  avatarUrl?: string | null;
}

/** A kind of open work the member still owns, such as "12 open issues". */
export interface RemoveMemberOpenWork {
  /** Stable id, returned in `reassignments`. */
  id: string;
  /** What the work is, without the number: "open issues". */
  label: string;
  /** How many items. */
  count: number;
}

/** Where one kind of open work goes. `assigneeId: null` leaves it unassigned. */
export interface RemoveMemberReassignment {
  workId: string;
  assigneeId: string | null;
}

/** What `onRemove` receives. */
export interface RemoveMemberRequest {
  /** One entry per `openWork` item, in the same order. Empty without open work. */
  reassignments: RemoveMemberReassignment[];
}

/** Every visible string of the flow, for translation. */
export interface RemoveMemberFlowMessages extends Partial<WizardDialogMessages> {
  /** Dialog title. Default "Remove <name>". */
  title: (name: string) => string;
  /** Reassign step heading. Default "Hand over their open work". */
  reassignTitle: string;
  /** Reassign step description. */
  reassignDescription: (name: string) => string;
  /** Label of the picker that sets everything at once. Default "Reassign all open work to". */
  reassignAll: string;
  /** The per-item switch. Default "Choose a person for each kind of work". */
  perItem: string;
  /** Label of a per-item picker, e.g. "12 open issues". */
  workLabel: (count: string, label: string) => string;
  /** The empty choice in the pickers. Default "Leave unassigned". */
  unassigned: string;
  /** Search box placeholder. Default "Search members". */
  search: string;
  /** Confirm step heading. Default "Confirm removal". */
  confirmTitle: string;
  /** Confirm step description. */
  confirmDescription: string;
  /** Default consequence: they lose access. */
  losesAccess: (name: string) => string;
  /** Default consequence for reassigned work. Count is already formatted. */
  reassignedTo: (count: string, label: string, assignee: string) => string;
  /** Default consequence for work left unassigned. */
  leftUnassigned: (count: string, label: string) => string;
  /** Default consequence: their past activity stays. */
  historyStays: string;
  /** The remove button. Default "Remove member". */
  confirm: string;
  /** Undo toast title. Default "<name> removed". */
  removed: (name: string) => string;
}

export const REMOVE_MEMBER_FLOW_MESSAGES: RemoveMemberFlowMessages = {
  title: (name) => `Remove ${name}`,
  reassignTitle: 'Hand over their open work',
  reassignDescription: (name) =>
    `Choose who takes over what ${name} still owns, or leave it unassigned.`,
  reassignAll: 'Reassign all open work to',
  perItem: 'Choose a person for each kind of work',
  workLabel: (count, label) => `${count} ${label}`,
  unassigned: 'Leave unassigned',
  search: 'Search members',
  confirmTitle: 'Confirm removal',
  confirmDescription: 'They lose access straight away. You can invite them again later.',
  losesAccess: (name) => `${name} loses access to the workspace`,
  reassignedTo: (count, label, assignee) => `${count} ${label} go to ${assignee}`,
  leftUnassigned: (count, label) => `${count} ${label} are left unassigned`,
  historyStays: 'Their past activity and comments stay',
  confirm: 'Remove member',
  removed: (name) => `${name} removed`,
};

export interface RemoveMemberFlowProps extends Omit<
  WizardDialogProps,
  'title' | 'steps' | 'step' | 'defaultStep' | 'onStepChange' | 'onComplete' | 'messages'
> {
  /** Who is removed. Shown with their avatar on the confirm step. */
  member: RemoveMemberTarget;
  /** What they still own. With none, the reassign step is skipped. */
  openWork?: RemoveMemberOpenWork[];
  /** People who can take over the work. The removed member is left out. */
  assignees?: AssigneePerson[];
  /** The signed-in user, listed first with "Assign to me" in the pickers. */
  currentUserId?: string | null;
  /** Extra consequences listed after the default ones on the confirm step. */
  consequences?: ConsequenceItem[];
  /** Locale for the counts. Default: the runtime's locale. */
  locale?: string;
  /** Re-authentication before the removal runs. Cancelling it keeps the person on the confirm step. */
  stepUp?: StepUpRequirement;
  /** Removes the member. Return a promise; a rejection shows its message on the confirm step. */
  onRemove: (request: RemoveMemberRequest) => unknown;
  /** Called after a successful removal, as the dialog closes. */
  onRemoved?: (request: RemoveMemberRequest) => void;
  /**
   * Restores the member. When given, a "<name> removed. Undo" toast follows a
   * successful removal (`useUndoToast`), so a `ToastProvider` must be mounted
   * above the flow. Without it no toast code runs and no provider is needed.
   */
  onUndo?: (request: RemoveMemberRequest) => unknown;
  /** Extra options for the undo toast, such as `duration` or `onCommit`. */
  undoOptions?: Omit<UndoableOptions, 'title' | 'onUndo'>;
  /** Visible strings, for translation (WizardDialog strings included). */
  messages?: Partial<RemoveMemberFlowMessages>;
}

type Undoable = (options: UndoableOptions) => Promise<unknown>;

/** Mounted only when `onUndo` is given, so the flow works without a ToastProvider otherwise. */
function UndoBridge({ register }: { register: (undoable: Undoable | null) => void }) {
  const { undoable } = useUndoToast();
  useEffect(() => {
    register(undoable);
    return () => register(null);
  }, [undoable, register]);
  return null;
}

/**
 * Removes a member from a workspace in two steps: hand over their open work
 * (all at once or per kind of work, to a member or left unassigned), then
 * confirm with the consequences listed, with optional re-authentication. The
 * reassign step is skipped when they own nothing. With `onUndo`, an Undo
 * toast follows the removal. Built on WizardDialog, AssigneePicker,
 * ConsequenceList, MemberRow and useUndoToast.
 */
export const RemoveMemberFlow = forwardRef<ElementRef<typeof WizardDialog>, RemoveMemberFlowProps>(
  function RemoveMemberFlow(
    {
      member,
      openWork = [],
      assignees = [],
      currentUserId = null,
      consequences,
      locale,
      stepUp,
      onRemove,
      onRemoved,
      onUndo,
      undoOptions,
      messages: messagesProp,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...REMOVE_MEMBER_FLOW_MESSAGES, ...messagesProp };
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [allAssignee, setAllAssignee] = useState<string | null>(null);
    const [perItem, setPerItem] = useState(false);
    const [assignments, setAssignments] = useState<Record<string, string | null>>({});
    const undoRef = useRef<Undoable | null>(null);
    const registerUndo = useCallback((fn: Undoable | null) => {
      undoRef.current = fn;
    }, []);
    const { requireStepUp, element: stepUpElement } = useStepUpAuth();
    const format = new Intl.NumberFormat(locale);

    // Every opening starts clean.
    useEffect(() => {
      if (open) {
        setAllAssignee(null);
        setPerItem(false);
        setAssignments({});
      }
    }, [open]);

    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    const people = assignees.filter((p) => p.id !== member.id);
    const nameOf = (id: string | null) =>
      id === null ? null : (people.find((p) => p.id === id)?.name ?? null);
    const assigneeFor = (workId: string): string | null =>
      perItem ? (assignments[workId] ?? null) : allAssignee;
    const reassignments: RemoveMemberReassignment[] = openWork.map((w) => ({
      workId: w.id,
      assigneeId: assigneeFor(w.id),
    }));

    const pickerMessages = { unassigned: m.unassigned };

    const items: ConsequenceItem[] = [
      { id: 'access', kind: 'removed', label: m.losesAccess(member.name) },
      ...openWork.map((w): ConsequenceItem => {
        const to = nameOf(assigneeFor(w.id));
        const count = format.format(w.count);
        return to
          ? { id: `work-${w.id}`, kind: 'changed', label: m.reassignedTo(count, w.label, to) }
          : { id: `work-${w.id}`, kind: 'warning', label: m.leftUnassigned(count, w.label) };
      }),
      { id: 'history', kind: 'kept', label: m.historyStays },
      ...(consequences ?? []),
    ];

    const remove = async () => {
      if (stepUp) {
        const verified = await requireStepUp(stepUp);
        if (!verified) return false;
      }
      const request: RemoveMemberRequest = { reassignments };
      await onRemove(request);
      onRemoved?.(request);
      if (onUndo && undoRef.current) {
        // The toast outlives the dialog; its outcome is the product's concern.
        undoRef
          .current({
            ...undoOptions,
            title: m.removed(member.name),
            onUndo: () => onUndo(request),
          })
          .catch(() => undefined);
      }
      return true;
    };

    const reassignStep: WizardStep = {
      id: 'reassign',
      title: m.reassignTitle,
      description: m.reassignDescription(member.name),
      content: (
        <div className="mn-remove-member-flow__reassign" data-per-item={perItem || undefined}>
          {perItem ? null : (
            <Field>
              <FieldLabel>{m.reassignAll}</FieldLabel>
              <AssigneePicker
                people={people}
                currentUserId={currentUserId}
                value={allAssignee}
                onValueChange={setAllAssignee}
                searchPlaceholder={m.search}
                messages={pickerMessages}
              />
            </Field>
          )}
          {openWork.length > 1 ? (
            <Checkbox
              label={m.perItem}
              checked={perItem}
              onCheckedChange={(next) => {
                const on = next === true;
                if (on) {
                  setAssignments(Object.fromEntries(openWork.map((w) => [w.id, allAssignee])));
                }
                setPerItem(on);
              }}
            />
          ) : null}
          {perItem ? (
            <ul className="mn-remove-member-flow__work">
              {openWork.map((w) => (
                <li key={w.id} className="mn-remove-member-flow__work-item">
                  <Field>
                    <FieldLabel>{m.workLabel(format.format(w.count), w.label)}</FieldLabel>
                    <AssigneePicker
                      people={people}
                      currentUserId={currentUserId}
                      value={assignments[w.id] ?? null}
                      onValueChange={(next) =>
                        setAssignments((prev) => ({ ...prev, [w.id]: next }))
                      }
                      searchPlaceholder={m.search}
                      messages={pickerMessages}
                    />
                  </Field>
                </li>
              ))}
            </ul>
          ) : (
            <ul className="mn-remove-member-flow__summary">
              {openWork.map((w) => (
                <li key={w.id}>{m.workLabel(format.format(w.count), w.label)}</li>
              ))}
            </ul>
          )}
        </div>
      ),
    };

    const steps: WizardStep[] = [
      ...(openWork.length > 0 ? [reassignStep] : []),
      {
        id: 'confirm',
        title: m.confirmTitle,
        description: m.confirmDescription,
        tone: 'danger',
        continueLabel: m.confirm,
        content: (
          <div className="mn-remove-member-flow__confirm">
            <MemberRow
              as="div"
              className="mn-remove-member-flow__member"
              name={member.name}
              email={member.email}
              avatarUrl={member.avatarUrl ?? null}
            />
            <ConsequenceList items={items} {...(locale ? { locale } : {})} />
          </div>
        ),
        onContinue: remove,
      },
    ];

    return (
      <>
        <WizardDialog
          ref={ref}
          {...rest}
          open={open}
          onOpenChange={setOpen}
          title={m.title(member.name)}
          steps={steps}
          messages={m}
          className={cx('mn-remove-member-flow', className)}
        />
        {stepUpElement}
        {onUndo ? <UndoBridge register={registerUndo} /> : null}
      </>
    );
  },
);
