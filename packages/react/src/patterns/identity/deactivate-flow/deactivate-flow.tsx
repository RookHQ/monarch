'use client';

import { forwardRef, type ElementRef, type ReactNode } from 'react';
import {
  ConsequenceList,
  type ConsequenceItem,
} from '../../../components/feedback/consequence-list/index.js';
import {
  ConfirmDialog,
  type ConfirmDetails,
  type ConfirmDialogProps,
  type ConfirmReasonField,
} from '../../../components/overlays/confirm-dialog/index.js';
import { cx } from '../../../internal/cx.js';
import { RetryGlyph } from '../../../internal/icons.js';

/** Every visible string, for translation. `subject` is the `subject` prop. */
export interface DeactivateFlowMessages {
  /** Title in deactivate mode. Default "Deactivate {subject}?". */
  deactivateTitle: (subject: ReactNode) => ReactNode;
  /** Title in reactivate mode. Default "Reactivate {subject}?". */
  reactivateTitle: (subject: ReactNode) => ReactNode;
  /** Description in deactivate mode. Default "It stops working until you turn it back on. Nothing is deleted." */
  deactivateDescription: string;
  /** Description in reactivate mode. Default "It starts working again straight away." */
  reactivateDescription: string;
  /**
   * The reversibility line in deactivate mode. Default "You can reactivate it
   * at any time from {from}." or, without `reactivateFrom`, "You can
   * reactivate it at any time."
   */
  reversible: (from?: ReactNode) => ReactNode;
  /** Consequence heading in deactivate mode. Default "While it is deactivated". */
  deactivateConsequences: string;
  /** Consequence heading in reactivate mode. Default "Once it is reactivated". */
  reactivateConsequences: string;
  /** Confirm button in deactivate mode. Default "Deactivate". */
  deactivate: string;
  /** Confirm button in reactivate mode. Default "Reactivate". */
  reactivate: string;
  /** Default "Cancel". */
  cancel: string;
  /** Label of the reason field. Default "Reason (optional)". */
  reasonLabel: string;
  /** Help under the reason field. Default "Kept with the change history." */
  reasonDescription: string;
}

const DEFAULT_MESSAGES: DeactivateFlowMessages = {
  deactivateTitle: (subject) => <>Deactivate {subject}?</>,
  reactivateTitle: (subject) => <>Reactivate {subject}?</>,
  deactivateDescription: 'It stops working until you turn it back on. Nothing is deleted.',
  reactivateDescription: 'It starts working again straight away.',
  reversible: (from) =>
    from ? (
      <>You can reactivate it at any time from {from}.</>
    ) : (
      'You can reactivate it at any time.'
    ),
  deactivateConsequences: 'While it is deactivated',
  reactivateConsequences: 'Once it is reactivated',
  deactivate: 'Deactivate',
  reactivate: 'Reactivate',
  cancel: 'Cancel',
  reasonLabel: 'Reason (optional)',
  reasonDescription: 'Kept with the change history.',
};

export interface DeactivateFlowProps extends Omit<
  ConfirmDialogProps,
  | 'title'
  | 'description'
  | 'consequences'
  | 'tone'
  | 'confirmLabel'
  | 'cancelLabel'
  | 'confirmPhrase'
  | 'confirmPhraseLabel'
  | 'confirmPhraseMismatch'
  | 'acknowledgement'
  | 'acknowledgementMissing'
  | 'reason'
  | 'onConfirm'
  | 'children'
> {
  /** `deactivate` (default) pauses the thing with a warning tone; `reactivate` turns it back on with the default tone. */
  mode?: 'deactivate' | 'reactivate';
  /** What is paused, e.g. "the Northwind partnership" or a person's name. Used in the default title. */
  subject: ReactNode;
  /** Overrides the title. */
  title?: ReactNode;
  /** Overrides the description under the title. */
  description?: ReactNode;
  /**
   * Where the person turns it back on, e.g. "Settings › Integrations". Shown
   * in the deactivate-mode line "You can reactivate it at any time from …".
   */
  reactivateFrom?: ReactNode;
  /** What changes while it is deactivated (or, in reactivate mode, once it is back). */
  consequences?: ConsequenceItem[];
  /** Ask for a reason, passed to the callback. `true` uses the default optional field. */
  reason?: boolean | Partial<ConfirmReasonField>;
  /** Overrides the confirm button label. Default "Deactivate" or "Reactivate". */
  confirmLabel?: ReactNode;
  /**
   * Runs the deactivation in deactivate mode. Return a promise to keep the
   * dialog pending; a rejection shows its message and the person can retry.
   */
  onDeactivate?: (details: ConfirmDetails) => unknown;
  /** Runs the reactivation in reactivate mode. Same contract as `onDeactivate`. */
  onReactivate?: (details: ConfirmDetails) => unknown;
  /** Locale for consequence counts. */
  locale?: string;
  /** Visible strings, for translation. */
  messages?: Partial<DeactivateFlowMessages>;
}

/**
 * Confirms a reversible pause: deactivating a partnership, a tracker or a
 * person, or turning one back on. A warning-toned ConfirmDialog (not danger,
 * because nothing is lost) that says where to reactivate, lists what changes
 * meanwhile and can ask for a reason. `mode="reactivate"` swaps the copy and
 * uses the default tone.
 */
export const DeactivateFlow = forwardRef<ElementRef<typeof ConfirmDialog>, DeactivateFlowProps>(
  function DeactivateFlow(
    {
      mode = 'deactivate',
      subject,
      title,
      description,
      reactivateFrom,
      consequences,
      reason,
      confirmLabel,
      onDeactivate,
      onReactivate,
      locale,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_MESSAGES, ...messagesProp };
    const deactivating = mode === 'deactivate';
    const reasonField: ConfirmReasonField | undefined = reason
      ? {
          label: m.reasonLabel,
          description: m.reasonDescription,
          ...(reason === true ? {} : reason),
        }
      : undefined;
    const hasConsequences = consequences !== undefined && consequences.length > 0;

    const body =
      deactivating || hasConsequences ? (
        <>
          {deactivating ? (
            <p className="mn-deactivate-flow__reversible">
              <RetryGlyph className="mn-deactivate-flow__reversible-icon" aria-hidden />
              <span>{m.reversible(reactivateFrom)}</span>
            </p>
          ) : null}
          {hasConsequences ? (
            <ConsequenceList
              heading={deactivating ? m.deactivateConsequences : m.reactivateConsequences}
              headingLevel="h3"
              items={consequences}
              {...(locale !== undefined ? { locale } : {})}
            />
          ) : null}
        </>
      ) : undefined;

    return (
      <ConfirmDialog
        ref={ref}
        {...rest}
        tone={deactivating ? 'warning' : 'default'}
        title={title ?? (deactivating ? m.deactivateTitle(subject) : m.reactivateTitle(subject))}
        description={
          description ?? (deactivating ? m.deactivateDescription : m.reactivateDescription)
        }
        confirmLabel={confirmLabel ?? (deactivating ? m.deactivate : m.reactivate)}
        cancelLabel={m.cancel}
        {...(reasonField ? { reason: reasonField } : {})}
        onConfirm={(details) => (deactivating ? onDeactivate : onReactivate)?.(details)}
        data-mode={mode}
        className={cx('mn-deactivate-flow', className)}
      >
        {body}
      </ConfirmDialog>
    );
  },
);
