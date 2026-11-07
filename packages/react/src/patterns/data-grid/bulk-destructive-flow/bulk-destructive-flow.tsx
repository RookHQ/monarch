'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type ElementRef,
  type ReactNode,
} from 'react';
import { Button } from '../../../components/actions/button/index.js';
import { Progress } from '../../../components/data-display/progress-bar/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
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
import { cx } from '../../../internal/cx.js';

/** A CLDR plural category, as returned by `Intl.PluralRules#select`. */
export type BulkDestructivePluralCategory = 'zero' | 'one' | 'two' | 'few' | 'many' | 'other';

/**
 * The name of the items, by plural category (`{ one: 'lead', other: 'leads' }`),
 * picked with `Intl.PluralRules` in `locale`; or a function that returns the
 * right form for a count.
 */
export type BulkDestructiveNoun =
  | ({ other: string } & Partial<Record<BulkDestructivePluralCategory, string>>)
  | ((count: number) => string);

/** One item that could not be deleted. */
export interface BulkDestructiveFailure {
  /** The item's id, so a retry can target it. */
  id: string;
  /** What the person recognises the item by, e.g. its name. */
  label: ReactNode;
  /** Why it failed, in plain words. */
  error: string;
}

/** What `onDelete` and `onRetryFailed` resolve with. Resolve with nothing when every item was deleted. */
export interface BulkDestructiveResult {
  /** The items that could not be deleted. Empty or missing means all succeeded. */
  failed?: BulkDestructiveFailure[];
}

/**
 * Every visible string, for translation. `count`, `done`, `total` and
 * `succeeded` arrive already formatted with `Intl.NumberFormat`; `noun` is
 * already in the right plural form for the number next to it.
 */
export interface BulkDestructiveFlowMessages {
  /** Dialog title. Default "Delete 37 leads". */
  title: (count: string, noun: string) => string;
  /** Heading of the confirmation step. Default "Check before you delete". */
  confirmTitle: string;
  /** Line under that heading. Default "This cannot be undone." */
  irreversible: string;
  /** Heading of the sample list. Default "Selected". */
  sampleHeading: string;
  /** Last line of the sample list. Default "+34 more". */
  more: (count: string) => string;
  /** Label of the type-the-count field. Default: Type 37 to confirm, with the number highlighted. */
  typeLabel?: (count: string) => string;
  /** Why the delete button is disabled until the count is typed. Default "Type the number to continue." */
  typeBlocked: string;
  /** The delete button. Default "Delete 37 leads". */
  confirm: (count: string, noun: string) => string;
  /** Accessible name of the progress bar. Default "Deletion progress". */
  progressLabel: string;
  /** Progress text. Default "12 of 37 deleted". */
  progress: (done: string, total: string) => string;
  /** Result heading when every item was deleted. Default "37 leads deleted". */
  doneTitle: (count: string, noun: string) => string;
  /** Result heading when some failed. Default "35 of 37 leads deleted". */
  partialTitle: (succeeded: string, total: string, noun: string) => string;
  /** Warning on a partial result. Default "2 leads could not be deleted." */
  partialDescription: (failed: string, noun: string) => string;
  /** Heading of the failure list. Default "Not deleted". */
  failuresHeading: string;
  /** The retry button and the retry step's action. Default "Retry 2 leads". */
  retry: (count: string, noun: string) => string;
  /** Heading of the retry step. Default "Try the rest again". */
  retryTitle: string;
  /** Line under the retry heading. Default "Only the items below are deleted. This cannot be undone." */
  retryDescription: string;
  /** Why the step-up dialog appears. Default "Deleting many items at once needs you to confirm it’s you." */
  stepUpDescription: string;
}

const DEFAULT_MESSAGES: BulkDestructiveFlowMessages = {
  title: (count, noun) => `Delete ${count} ${noun}`,
  confirmTitle: 'Check before you delete',
  irreversible: 'This cannot be undone.',
  sampleHeading: 'Selected',
  more: (count) => `+${count} more`,
  typeBlocked: 'Type the number to continue.',
  confirm: (count, noun) => `Delete ${count} ${noun}`,
  progressLabel: 'Deletion progress',
  progress: (done, total) => `${done} of ${total} deleted`,
  doneTitle: (count, noun) => `${count} ${noun} deleted`,
  partialTitle: (succeeded, total, noun) => `${succeeded} of ${total} ${noun} deleted`,
  partialDescription: (failed, noun) => `${failed} ${noun} could not be deleted.`,
  failuresHeading: 'Not deleted',
  retry: (count, noun) => `Retry ${count} ${noun}`,
  retryTitle: 'Try the rest again',
  retryDescription: 'Only the items below are deleted. This cannot be undone.',
  stepUpDescription: 'Deleting many items at once needs you to confirm it’s you.',
};

export interface BulkDestructiveFlowProps extends Omit<
  WizardDialogProps,
  | 'title'
  | 'steps'
  | 'step'
  | 'defaultStep'
  | 'onStepChange'
  | 'onComplete'
  | 'showProgress'
  | 'messages'
> {
  /** How many items will be deleted. */
  count: number;
  /** The items' name in each plural form, or a function of the count. */
  noun: BulkDestructiveNoun;
  /** Labels of the first few items (three to five), shown with "+N more" for the rest. */
  sample?: ReactNode[];
  /**
   * When `count` is above this, the person must type the count to confirm.
   * Default 10. Pass `Infinity` to never ask.
   */
  typeCountAbove?: number;
  /** What else happens, e.g. related records that are deleted too. */
  consequences?: ConsequenceItem[];
  /** Overrides the dialog title. Default `messages.title`. */
  title?: ReactNode;
  /**
   * Runs the deletion. Call `report(done)` as items finish to move the
   * progress bar. Resolve with a `BulkDestructiveResult` (`{ failed }`) to
   * list items that could not be deleted, or with nothing when all succeeded; reject when nothing could be done, and the error is shown on the
   * confirmation step.
   */
  onDelete: (report: (done: number) => void) => Promise<unknown>;
  /**
   * Retries the failed items. When given, a partial result offers a retry
   * button that opens a retry step for those items. Resolves like `onDelete`.
   */
  onRetryFailed?: (
    failed: BulkDestructiveFailure[],
    report: (done: number) => void,
  ) => Promise<unknown>;
  /** Ask the person to re-authenticate before the deletion runs. */
  stepUp?: StepUpRequirement;
  /** Locale for numbers and plural rules. Default: the runtime's locale. */
  locale?: string;
  /** Visible strings, for translation. */
  messages?: Partial<BulkDestructiveFlowMessages>;
}

function nounFor(noun: BulkDestructiveNoun, count: number, rules: Intl.PluralRules): string {
  if (typeof noun === 'function') return noun(count);
  return noun[rules.select(count) as BulkDestructivePluralCategory] ?? noun.other;
}

/** The `failed` list of a `BulkDestructiveResult`; anything else means all succeeded. */
function failuresOf(result: unknown): BulkDestructiveFailure[] {
  if (typeof result !== 'object' || result === null) return [];
  const failed = (result as BulkDestructiveResult).failed;
  return Array.isArray(failed) ? failed : [];
}

type Phase = 'confirm' | 'retry';

/**
 * "Delete 37 leads": confirms a bulk deletion, shows a sample of what is
 * selected, asks for the count to be typed when there are many, runs the
 * deletion with a progress bar ("12 of 37 deleted") while the dialog is
 * locked, then reports the result, listing any items that failed with an
 * optional retry. Built on WizardDialog.
 */
export const BulkDestructiveFlow = forwardRef<
  ElementRef<typeof WizardDialog>,
  BulkDestructiveFlowProps
>(function BulkDestructiveFlow(
  {
    count,
    noun,
    sample = [],
    typeCountAbove = 10,
    consequences,
    title,
    onDelete,
    onRetryFailed,
    stepUp,
    locale,
    messages: messagesProp,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const rules = new Intl.PluralRules(locale);
  const numbers = new Intl.NumberFormat(locale);
  const n = (value: number) => numbers.format(value);
  const nounOf = (value: number) => nounFor(noun, value, rules);

  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const [phase, setPhase] = useState<Phase>('confirm');
  const [stepId, setStepId] = useState<string>('confirm');
  const [typed, setTyped] = useState('');
  const [running, setRunning] = useState<{ done: number; total: number } | null>(null);
  const [failed, setFailed] = useState<BulkDestructiveFailure[]>([]);
  const mounted = useRef(true);
  const { requireStepUp, element: stepUpElement } = useStepUpAuth();

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // Every opening starts on the confirmation step with nothing typed.
  useEffect(() => {
    if (open) {
      setPhase('confirm');
      setStepId('confirm');
      setTyped('');
      setRunning(null);
      setFailed([]);
    }
  }, [open]);

  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange],
  );

  const run = async (
    total: number,
    action: (report: (done: number) => void) => Promise<unknown>,
  ): Promise<boolean> => {
    if (stepUp) {
      const verified = await requireStepUp({
        description: m.stepUpDescription,
        ...stepUp,
      });
      if (!verified) return false;
    }
    setRunning({ done: 0, total });
    const report = (done: number) => {
      if (!mounted.current) return;
      setRunning({ done: Math.max(0, Math.min(done, total)), total });
    };
    try {
      const result = await action(report);
      if (mounted.current) setFailed(failuresOf(result));
    } catch (error) {
      if (mounted.current) setRunning(null);
      throw error;
    }
    return true;
  };

  const needsCount = count > typeCountAbove;
  const phrase = String(count);
  const countOk = !needsCount || phraseMatches(typed, phrase);
  const shown = sample.slice(0, Math.min(sample.length, count));
  const remaining = count - shown.length;

  const progress = running ? (
    <div className="mn-bulk-destructive-flow__progress">
      <Progress
        value={running.done}
        max={running.total}
        label={m.progressLabel}
        tone="danger"
        valueText={m.progress(n(running.done), n(running.total))}
      />
      <p className="mn-bulk-destructive-flow__progress-text">
        {m.progress(n(running.done), n(running.total))}
      </p>
    </div>
  ) : null;

  const failureList = (
    <section className="mn-bulk-destructive-flow__failures">
      <h4 className="mn-bulk-destructive-flow__heading">{m.failuresHeading}</h4>
      <ul className="mn-bulk-destructive-flow__list">
        {failed.map((item) => (
          <li key={item.id} className="mn-bulk-destructive-flow__failure">
            <span className="mn-bulk-destructive-flow__label">{item.label}</span>
            <span className="mn-bulk-destructive-flow__error">{item.error}</span>
          </li>
        ))}
      </ul>
    </section>
  );

  const confirmStep: WizardStep = {
    id: 'confirm',
    title: m.confirmTitle,
    description: m.irreversible,
    tone: 'danger',
    continueLabel: m.confirm(n(count), nounOf(count)),
    canContinue: countOk,
    blockedReason: m.typeBlocked,
    content: progress ?? (
      <>
        {shown.length > 0 ? (
          <section className="mn-bulk-destructive-flow__sample">
            <h4 className="mn-bulk-destructive-flow__heading">{m.sampleHeading}</h4>
            <ul className="mn-bulk-destructive-flow__list">
              {shown.map((label, index) => (
                <li key={index} className="mn-bulk-destructive-flow__label">
                  {label}
                </li>
              ))}
              {remaining > 0 ? (
                <li className="mn-bulk-destructive-flow__more">{m.more(n(remaining))}</li>
              ) : null}
            </ul>
          </section>
        ) : null}
        {consequences && consequences.length > 0 ? (
          <ConsequenceList items={consequences} {...(locale !== undefined ? { locale } : {})} />
        ) : null}
        {needsCount ? (
          <TypeToConfirmInput
            phrase={phrase}
            value={typed}
            onValueChange={setTyped}
            inputMode="numeric"
            {...(m.typeLabel ? { label: m.typeLabel(phrase) } : {})}
          />
        ) : null}
      </>
    ),
    onContinue: () => run(count, onDelete),
  };

  const retryStep: WizardStep = {
    id: 'retry',
    title: m.retryTitle,
    description: m.retryDescription,
    tone: 'danger',
    hideBack: true,
    continueLabel: m.retry(n(failed.length), nounOf(failed.length)),
    content: progress ?? failureList,
    onContinue: () => {
      const targets = failed;
      return run(targets.length, (report) =>
        onRetryFailed ? onRetryFailed(targets, report) : Promise.resolve({ failed: targets }),
      );
    },
  };

  const failedCount = failed.length;
  const succeeded = Math.max(0, count - failedCount);
  const resultStep: WizardStep = {
    id: 'result',
    kind: 'done',
    title:
      failedCount === 0
        ? m.doneTitle(n(count), nounOf(count))
        : m.partialTitle(n(succeeded), n(count), nounOf(count)),
    content:
      failedCount === 0 ? undefined : (
        <>
          <Alert tone="warning">{m.partialDescription(n(failedCount), nounOf(failedCount))}</Alert>
          {failureList}
          {onRetryFailed ? (
            <div className="mn-bulk-destructive-flow__retry">
              <Button
                variant="secondary"
                onClick={() => {
                  setRunning(null);
                  setPhase('retry');
                  setStepId('retry');
                }}
              >
                {m.retry(n(failedCount), nounOf(failedCount))}
              </Button>
            </div>
          ) : null}
        </>
      ),
  };

  return (
    <>
      <WizardDialog
        ref={ref}
        {...rest}
        open={open}
        onOpenChange={setOpen}
        title={title ?? m.title(n(count), nounOf(count))}
        steps={phase === 'retry' ? [retryStep, resultStep] : [confirmStep, resultStep]}
        step={stepId}
        onStepChange={(id) => {
          setRunning(null);
          setStepId(id);
        }}
        showProgress={false}
        data-phase={phase}
        data-running={running ? '' : undefined}
        className={cx('mn-bulk-destructive-flow', className)}
      />
      {stepUpElement}
    </>
  );
});
