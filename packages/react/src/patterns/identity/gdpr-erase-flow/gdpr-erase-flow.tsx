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
import { cx } from '../../../internal/cx.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import {
  ConsequenceList,
  type ConsequenceItem,
} from '../../../components/feedback/consequence-list/index.js';
import { Checkbox } from '../../../components/forms/checkbox/index.js';
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

/** One kind of personal data that is erased. */
export type GdprErasedItem = Omit<ConsequenceItem, 'kind'>;

/** One kind of data that is kept, with why. */
export interface GdprRetainedItem extends Omit<ConsequenceItem, 'kind'> {
  /**
   * Why it is kept and for how long, in the words your policy uses, e.g.
   * "Kept for the legally required retention period". Shown under the label.
   */
  reason?: ReactNode;
}

/** What `onErase` may resolve with. */
export interface GdprEraseResult {
  /** The audit-log entry recording the erasure, shown on the done screen. */
  auditId?: string;
}

/** Every visible string of the GdprEraseFlow, for translation. */
export interface GdprEraseFlowMessages {
  /** Dialog title and accessible name. Default "Erase personal data". */
  title: string;
  /** Review step heading. */
  reviewTitle: string;
  /** Review step description. */
  reviewDescription: (subjectName: string) => ReactNode;
  /** Heading over the erased list. Default "Will be erased". */
  erasedHeading: string;
  /** Heading over the retained list. Default "Will be kept". */
  retainedHeading: string;
  /** Shown beside the disabled Continue while the lists load or failed to load. */
  reviewBlocked: string;
  /** Title of the irreversibility notice. */
  irreversibleTitle: string;
  /** Text of the irreversibility notice. */
  irreversible: string;
  /** Confirm step heading. */
  confirmTitle: string;
  /** Confirm step description. */
  confirmDescription: (subjectName: string) => ReactNode;
  /** Label of the acknowledgement checkbox. */
  acknowledgement: string;
  /** Label of the confirmation field. `phrase` is already styled. */
  typeToConfirm: (phrase: ReactNode) => ReactNode;
  /** Shown beside the disabled button until both are done. */
  confirmBlocked: string;
  /** Destructive button. Default "Erase data". */
  eraseAction: string;
  /** Description inside the step-up dialog. */
  stepUpDescription: string;
  /** Done heading. Default "Personal data erased". */
  doneTitle: string;
  doneDescription: (subjectName: string) => ReactNode;
  /** Line shown with the audit id. Default "Logged in the audit log". */
  auditLogged: string;
  /** Label before the audit id. Default "Reference". */
  auditIdLabel: string;
}

export const GDPR_ERASE_FLOW_MESSAGES: GdprEraseFlowMessages = {
  title: 'Erase personal data',
  reviewTitle: 'What happens',
  reviewDescription: (name) => `This erases the personal data held about ${name}.`,
  erasedHeading: 'Will be erased',
  retainedHeading: 'Will be kept',
  reviewBlocked: 'The summary must load before you continue.',
  irreversibleTitle: 'This cannot be undone',
  irreversible: 'Erased data cannot be recovered, by you or by support.',
  confirmTitle: 'Confirm the erasure',
  confirmDescription: (name) => `Erase the personal data of ${name} now.`,
  acknowledgement: 'I understand this cannot be undone',
  typeToConfirm: (phrase) => <>Type {phrase} to confirm</>,
  confirmBlocked: 'Tick the box and type the confirmation text to continue.',
  eraseAction: 'Erase data',
  stepUpDescription: 'Erasing personal data needs you to confirm your identity.',
  doneTitle: 'Personal data erased',
  doneDescription: (name) => `The personal data of ${name} has been erased.`,
  auditLogged: 'Logged in the audit log',
  auditIdLabel: 'Reference',
};

export interface GdprEraseFlowProps extends Omit<
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
  /** Name of the person whose data is erased (the data subject). Shown in the copy. */
  subjectName: string;
  /** What the person types to confirm, e.g. the subject's email. Default `subjectName`. */
  confirmPhrase?: string;
  /** The personal data that is erased. */
  erased: GdprErasedItem[];
  /** Data that is kept, each with an optional `reason` (e.g. a retention period). */
  retained?: GdprRetainedItem[];
  /** The lists are still loading: skeleton rows, and Continue waits. */
  loading?: boolean;
  /** The lists could not be loaded, e.g. an `InlineErrorRetry`. Continue stays disabled. */
  error?: ReactNode;
  /**
   * Re-authentication before the erasure runs (`methods`, `onVerify`, …), the shape
   * every account flow shares. When given,
   * the destructive button opens the step-up dialog first, and the erasure
   * only runs once it succeeds.
   */
  stepUp?: StepUpRequirement;
  /**
   * Runs the erasure. Resolve with a `GdprEraseResult` (`{ auditId }`) to show the audit-log
   * reference on the done screen. A rejection's message is shown on the
   * confirm step.
   */
  onErase: () => unknown;
  /** Called when the done screen is closed. */
  onDone?: () => void;
  /** Locale for counts. Default: the runtime's locale. */
  locale?: string;
  /** Visible strings, for translation. */
  messages?: Partial<GdprEraseFlowMessages>;
  /** Strings of the underlying WizardDialog (Back, Cancel, Continue, Done, progress). */
  wizardMessages?: WizardDialogProps['messages'];
}

/**
 * Legal-grade erasure of one person's personal data (a data subject
 * request). Two steps: what is erased and what is kept (with retention
 * reasons) plus an irreversibility notice, then an "I understand" checkbox
 * and typing a confirmation phrase, with optional re-authentication. The done
 * screen shows the audit-log reference when the backend returns one.
 */
export const GdprEraseFlow = forwardRef<ElementRef<typeof WizardDialog>, GdprEraseFlowProps>(
  function GdprEraseFlow(
    {
      subjectName,
      confirmPhrase,
      erased,
      retained = [],
      loading = false,
      error,
      stepUp,
      onErase,
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
    const m = { ...GDPR_ERASE_FLOW_MESSAGES, ...messagesProp };
    const phrase = confirmPhrase ?? subjectName;
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [typed, setTyped] = useState('');
    const [acknowledged, setAcknowledged] = useState(false);
    const [auditId, setAuditId] = useState<string | undefined>(undefined);
    const { requireStepUp, element: stepUpElement } = useStepUpAuth();
    const erasedDone = useRef(false);

    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
        if (!next && erasedDone.current) {
          erasedDone.current = false;
          onDone?.();
        }
      },
      [openProp, onOpenChange, onDone],
    );

    // Every opening starts clean.
    useEffect(() => {
      if (open) {
        erasedDone.current = false;
        setTyped('');
        setAcknowledged(false);
        setAuditId(undefined);
      }
    }, [open]);

    const hasError = error !== undefined && error !== null && error !== false;
    const localeProp = locale !== undefined ? { locale } : {};
    const retainedItems: ConsequenceItem[] = retained.map(({ reason, detail, ...item }) => ({
      ...item,
      kind: 'kept',
      detail:
        reason && detail ? (
          <>
            {detail} · {reason}
          </>
        ) : (
          (reason ?? detail)
        ),
    }));

    const steps: WizardStep[] = [
      {
        id: 'review',
        title: m.reviewTitle,
        description: m.reviewDescription(subjectName),
        content: (
          <>
            <ConsequenceList
              heading={m.erasedHeading}
              headingLevel="h4"
              items={erased.map((item) => ({ ...item, kind: 'removed' }))}
              loading={loading}
              error={error}
              className="mn-gdpr-erase-flow__erased"
              {...localeProp}
            />
            {!loading && !hasError && retainedItems.length > 0 ? (
              <ConsequenceList
                heading={m.retainedHeading}
                headingLevel="h4"
                items={retainedItems}
                className="mn-gdpr-erase-flow__retained"
                {...localeProp}
              />
            ) : null}
            <Alert tone="danger" title={m.irreversibleTitle}>
              {m.irreversible}
            </Alert>
          </>
        ),
        canContinue: !loading && !hasError,
        blockedReason: m.reviewBlocked,
      },
      {
        id: 'confirm',
        title: m.confirmTitle,
        description: m.confirmDescription(subjectName),
        content: (
          <div className="mn-gdpr-erase-flow__fields">
            <Checkbox
              label={m.acknowledgement}
              checked={acknowledged}
              onCheckedChange={(next) => setAcknowledged(next === true)}
            />
            <TypeToConfirmInput
              phrase={phrase}
              value={typed}
              onValueChange={setTyped}
              label={m.typeToConfirm(
                <span className="mn-type-to-confirm-input__phrase">{phrase}</span>,
              )}
            />
          </div>
        ),
        canContinue: acknowledged && phraseMatches(typed, phrase),
        blockedReason: m.confirmBlocked,
        tone: 'danger',
        continueLabel: m.eraseAction,
        onContinue: async () => {
          if (stepUp) {
            const verified = await requireStepUp({ description: m.stepUpDescription, ...stepUp });
            if (!verified) return false;
          }
          const result = (await onErase()) as GdprEraseResult | undefined | null;
          erasedDone.current = true;
          const id = typeof result === 'object' && result ? result.auditId : undefined;
          setAuditId(typeof id === 'string' && id !== '' ? id : undefined);
          return true;
        },
      },
      {
        id: 'done',
        kind: 'done',
        title: m.doneTitle,
        content: (
          <div className="mn-gdpr-erase-flow__done">
            <p className="mn-gdpr-erase-flow__done-text">{m.doneDescription(subjectName)}</p>
            {auditId ? (
              <p className="mn-gdpr-erase-flow__audit">
                <span>{m.auditLogged}</span>
                <span className="mn-gdpr-erase-flow__audit-id">
                  {m.auditIdLabel}: <code>{auditId}</code>
                </span>
              </p>
            ) : null}
          </div>
        ),
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
          {...(wizardMessages ? { messages: wizardMessages } : {})}
          className={cx('mn-gdpr-erase-flow', className)}
        />
        {stepUpElement}
      </>
    );
  },
);
