'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Input } from '../../forms/text-field/index.js';
import {
  ReorderableList,
  type ReorderableListMessages,
} from '../../interaction/reorderable-list/index.js';
import type { AiPlanPart, AiPlanStepState } from '../ai-conversation/index.js';
import { AiChainOfSteps, type AiStepState } from '../ai-chain-of-steps/index.js';

/** One step of a plan, as in `AiPlanPart['steps']`. */
export type AiPlanStep = AiPlanPart['steps'][number];

/** Where the person is with the plan. */
export type AiPlanDecision = 'pending' | 'approved' | 'rejected';

/** Every visible and assistive-technology string. English defaults. */
export interface AiPlanMessages {
  /** Title above the steps. */
  title: string;
  /** Human-in-the-loop explanation while the plan waits for a decision. */
  description: string;
  /** Extra hint while the plan can be edited. */
  editHint: string;
  /** Shown (and announced) once approved. */
  approvedNotice: string;
  /** Shown (and announced) once rejected. */
  rejectedNotice: string;
  /** Accessible name of a step’s text field. */
  stepLabel: (position: number) => string;
  /** Placeholder of an empty step. */
  stepPlaceholder: string;
  /** Name used for an empty step in reorder announcements. */
  untitledStep: (position: number) => string;
  /** Accessible name of a step’s remove button. */
  remove: (label: string) => string;
  /** Announced when a step is removed. */
  removed: (label: string) => string;
  addStep: string;
  approve: string;
  reject: string;
  /** Shown when Approve is unavailable because every step is empty. */
  emptyPlan: string;
  /** Reorder handle and move button labels; see ReorderableList. */
  reorder?: Partial<ReorderableListMessages> | undefined;
}

export const AI_PLAN_MESSAGES: Omit<AiPlanMessages, 'reorder'> = {
  title: 'Proposed plan',
  description: 'The assistant will take these steps after you approve. Nothing runs until then.',
  editHint: 'You can edit, reorder, add or remove steps first.',
  approvedNotice: 'Plan approved. The assistant is working through it.',
  rejectedNotice: 'Plan rejected. The assistant will not take these steps.',
  stepLabel: (p) => `Step ${p}`,
  stepPlaceholder: 'Describe this step',
  untitledStep: (p) => `step ${p}`,
  remove: (label) => `Remove ${label}`,
  removed: (label) => `Removed ${label}`,
  addStep: 'Add step',
  approve: 'Approve plan',
  reject: 'Reject',
  emptyPlan: 'Add at least one step to approve the plan.',
};

export interface AiPlanProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onChange' | 'part' | 'title'
> {
  /** The plan part the agent proposed. Its steps seed the editable draft. */
  part: AiPlanPart;
  /**
   * Whether the person can reorder, edit, add and remove steps before
   * deciding. Once approved or rejected the plan is always read-only.
   * Default true.
   */
  editable?: boolean | undefined;
  /**
   * The decision, when you control it (for example from the conversation
   * state). Without it the component tracks its own after Approve or Reject.
   */
  decision?: AiPlanDecision | undefined;
  /** Called with the draft steps after every edit. */
  onChange?: ((steps: AiPlanStep[]) => void) | undefined;
  /** Called with the approved steps (empty steps dropped, labels trimmed). */
  onApprove?: ((steps: AiPlanStep[]) => void) | undefined;
  /** Called when the person rejects the plan. */
  onReject?: (() => void) | undefined;
  /** Replaces the "Proposed plan" title. */
  title?: ReactNode | undefined;
  /** Replaces any of the English strings. */
  messages?: Partial<AiPlanMessages> | undefined;
}

const TO_STEP_STATE: Record<AiPlanStepState, AiStepState> = {
  todo: 'pending',
  active: 'active',
  done: 'done',
  skipped: 'skipped',
  failed: 'failed',
};

// Not valid step ids: those are generated with useId or come from the model.
const ADD = '\u0000add';
const ROOT = '\u0000root';

function clean(steps: readonly AiPlanStep[]): AiPlanStep[] {
  return steps.map((s) => ({ ...s, label: s.label.trim() })).filter((s) => s.label.length > 0);
}

/**
 * The plan an agent proposes before it acts, for a person to review. They can
 * reorder (drag, keyboard or move buttons), edit, add and remove steps, then
 * Approve or Reject. Once approved it becomes a read-only, live list of the
 * steps as the agent works through them. Plugs into `AiMessageView` through
 * `renderPart`.
 */
export const AiPlan = forwardRef<HTMLDivElement, AiPlanProps>(function AiPlan(
  {
    part,
    editable = true,
    decision: decisionProp,
    onChange,
    onApprove,
    onReject,
    title,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_PLAN_MESSAGES, ...messagesProp };
  const id = useId();
  const titleId = `${id}-title`;
  const descriptionId = `${id}-description`;
  const [draft, setDraft] = useState<AiPlanStep[]>(() => part.steps.slice());
  const [internalDecision, setInternalDecision] = useState<AiPlanDecision>('pending');
  const decision = decisionProp ?? internalDecision;
  const [live, setLive] = useState('');
  const emitted = useRef<readonly AiPlanStep[]>(part.steps);
  const inputs = useRef(new Map<string, HTMLInputElement>());
  const addButton = useRef<HTMLButtonElement | null>(null);
  const root = useRef<HTMLDivElement | null>(null);
  /** What to focus after the next render: a step id, the Add button or the root. */
  const focusNext = useRef<string | null>(null);
  const counter = useRef(0);

  // A new plan from the model (not our own echo through onChange) replaces the draft.
  useEffect(() => {
    if (part.steps !== emitted.current) {
      emitted.current = part.steps;
      setDraft(part.steps.slice());
    }
  }, [part.steps]);

  useEffect(() => {
    const target = focusNext.current;
    if (!target) return;
    focusNext.current = null;
    if (target === ADD) addButton.current?.focus();
    else if (target === ROOT) root.current?.focus();
    else inputs.current.get(target)?.focus();
  });

  // Announce a decision change after the first render only.
  const lastDecision = useRef(decision);
  useEffect(() => {
    if (lastDecision.current === decision) return;
    lastDecision.current = decision;
    if (decision === 'approved') setLive(m.approvedNotice);
    else if (decision === 'rejected') setLive(m.rejectedNotice);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decision]);

  const update = (next: AiPlanStep[]) => {
    setDraft(next);
    emitted.current = next;
    onChange?.(next);
  };

  const labelOf = (step: AiPlanStep, index: number) =>
    step.label.trim() || m.untitledStep(index + 1);

  const addAfter = (index: number) => {
    counter.current += 1;
    const step: AiPlanStep = { id: `${id}-new-${counter.current}`, label: '', state: 'todo' };
    const next = draft.slice();
    next.splice(index + 1, 0, step);
    focusNext.current = step.id;
    update(next);
  };

  const remove = (index: number) => {
    const step = draft[index];
    if (!step) return;
    const next = draft.filter((_, i) => i !== index);
    const neighbour = next[Math.max(0, index - 1)];
    focusNext.current = neighbour ? neighbour.id : ADD;
    setLive(m.removed(labelOf(step, index)));
    update(next);
  };

  const onStepKeyDown = (e: KeyboardEvent<HTMLInputElement>, index: number) => {
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
      e.preventDefault();
      addAfter(index);
    } else if (e.key === 'Backspace' && e.currentTarget.value === '' && draft.length > 1) {
      e.preventDefault();
      remove(index);
    }
  };

  const approvable = clean(draft);
  const readOnly = decision !== 'pending' || !editable;
  const showDecisionButtons = decision === 'pending' && (onApprove || onReject || !decisionProp);

  return (
    <div
      ref={(node) => {
        root.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      // Focus lands here when the decision buttons disappear.
      tabIndex={decision === 'pending' ? undefined : -1}
      role="group"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
      {...rest}
      className={cx('mn-ai-plan', className)}
      data-decision={decision}
      data-editable={!readOnly || undefined}
    >
      <div className="mn-ai-plan__header">
        <p id={titleId} className="mn-ai-plan__title">
          {title ?? m.title}
        </p>
        <p id={descriptionId} className="mn-ai-plan__description">
          {decision === 'approved'
            ? m.approvedNotice
            : decision === 'rejected'
              ? m.rejectedNotice
              : readOnly
                ? m.description
                : `${m.description} ${m.editHint}`}
        </p>
      </div>

      {readOnly ? (
        <AiChainOfSteps
          className="mn-ai-plan__steps"
          steps={(decision === 'pending' ? draft : clean(draft)).map((s) => ({
            id: s.id,
            label: s.label,
            state: TO_STEP_STATE[s.state],
          }))}
          collapsible={false}
          announce={decision === 'approved'}
          messages={{ label: typeof title === 'string' ? title : m.title }}
        />
      ) : (
        <>
          <ReorderableList
            className="mn-ai-plan__list"
            items={draft}
            onReorder={(next) => update(next)}
            getKey={(s) => s.id}
            getLabel={(s) => labelOf(s, draft.indexOf(s))}
            moveButtons
            aria-label={typeof title === 'string' ? title : m.title}
            {...(m.reorder ? { messages: m.reorder } : {})}
            renderItem={(step, { index }) => (
              <span className="mn-ai-plan__step">
                <span className="mn-ai-plan__number" aria-hidden>
                  {index + 1}.
                </span>
                <Input
                  ref={(node) => {
                    if (node) inputs.current.set(step.id, node);
                    else inputs.current.delete(step.id);
                  }}
                  size="sm"
                  className="mn-ai-plan__input"
                  value={step.label}
                  placeholder={m.stepPlaceholder}
                  aria-label={m.stepLabel(index + 1)}
                  onChange={(e) => {
                    const value = e.currentTarget.value;
                    update(draft.map((s) => (s.id === step.id ? { ...s, label: value } : s)));
                  }}
                  onKeyDown={(e) => onStepKeyDown(e, index)}
                />
                <IconButton
                  size="sm"
                  className="mn-ai-plan__remove"
                  label={m.remove(labelOf(step, index))}
                  icon={<CloseGlyph />}
                  onClick={() => remove(index)}
                />
              </span>
            )}
          />
          <div className="mn-ai-plan__add">
            <Button
              ref={addButton}
              size="sm"
              variant="ghost"
              leadingIcon={<PlusGlyph />}
              onClick={() => addAfter(draft.length - 1)}
            >
              {m.addStep}
            </Button>
          </div>
        </>
      )}

      {showDecisionButtons ? (
        <div className="mn-ai-plan__footer">
          {approvable.length === 0 ? <p className="mn-ai-plan__empty">{m.emptyPlan}</p> : null}
          <div className="mn-ai-plan__actions">
            <Button
              size="sm"
              variant="secondary"
              onClick={() => {
                focusNext.current = ROOT;
                if (decisionProp === undefined) setInternalDecision('rejected');
                onReject?.();
              }}
            >
              {m.reject}
            </Button>
            <Button
              size="sm"
              variant="primary"
              disabled={approvable.length === 0}
              onClick={() => {
                focusNext.current = ROOT;
                if (decisionProp === undefined) setInternalDecision('approved');
                onApprove?.(approvable);
              }}
            >
              {m.approve}
            </Button>
          </div>
        </div>
      ) : null}

      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {live}
      </span>
    </div>
  );
});
