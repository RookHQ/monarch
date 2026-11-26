'use client';

import { forwardRef, useEffect, useId, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { EyeGlyph, EyeOffGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';

/** A recovery code, optionally marked as already used. */
export interface RecoveryCode {
  code: string;
  /** The code has been used and no longer works. */
  used?: boolean | undefined;
}

/** Visible strings, for translation. */
export interface RecoveryCodesMessages {
  /** Accessible name of the code list. Default "Recovery codes". */
  listLabel: string;
  /** Count line. Default `${left} of ${total} codes left`. */
  remaining: (left: number, total: number) => string;
  /** Hidden text after a used code. Default "(used)". */
  used: string;
  /** Default "Copy codes". */
  copy: string;
  /** Default "Copied". */
  copied: string;
  /** Default "Download". */
  download: string;
  /** Default "Show codes". */
  reveal: string;
  /** Default "Hide codes". */
  conceal: string;
  /** Shown instead of the codes while hidden. */
  concealedHint: string;
  /** Default "Generate new codes". */
  regenerate: string;
  /** Regenerate confirmation title. */
  regenerateTitle: string;
  /** Regenerate confirmation description. */
  regenerateDescription: string;
  /** Regenerate confirmation button. Default "Generate new codes". */
  regenerateConfirm: string;
  /** Default "Cancel". */
  cancel: string;
  /** Announced after new codes arrive. */
  regenerated: string;
  /** The acknowledgement checkbox. Default "I have saved my recovery codes". */
  acknowledge: string;
  /** Shown when continuing without ticking the box. */
  acknowledgeMissing: string;
  /** The continue button. Default "Continue". */
  continue: string;
}

export const defaultRecoveryCodesMessages: RecoveryCodesMessages = {
  listLabel: 'Recovery codes',
  remaining: (left, total) => `${left} of ${total} codes left`,
  used: '(used)',
  copy: 'Copy codes',
  copied: 'Copied',
  download: 'Download',
  reveal: 'Show codes',
  conceal: 'Hide codes',
  concealedHint: 'Your codes are hidden. Show them only where nobody can see your screen.',
  regenerate: 'Generate new codes',
  regenerateTitle: 'Generate new recovery codes?',
  regenerateDescription:
    'Your current codes will stop working straight away. Save the new ones somewhere safe.',
  regenerateConfirm: 'Generate new codes',
  cancel: 'Cancel',
  regenerated: 'New recovery codes generated. The old ones no longer work.',
  acknowledge: 'I have saved my recovery codes',
  acknowledgeMissing: 'Confirm that you have saved the codes.',
  continue: 'Continue',
};

export interface RecoveryCodesProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The codes, as strings or `{ code, used }`. */
  codes: ReadonlyArray<string | RecoveryCode>;
  /** File name of the download. Default "recovery-codes.txt". */
  fileName?: string | undefined;
  /**
   * Builds the downloaded and copied text from the unused codes. Default: one
   * code per line. Use it to add a heading such as the account name.
   */
  formatText?: ((codes: string[]) => string) | undefined;
  /** Codes are shown (controlled). */
  revealed?: boolean | undefined;
  /** Initial visibility when uncontrolled. Default `true`. */
  defaultRevealed?: boolean | undefined;
  /** Called when the person shows or hides the codes. */
  onRevealedChange?: ((revealed: boolean) => void) | undefined;
  /** Show the Show/Hide toggle. Default: `true` when the codes start hidden. */
  showRevealToggle?: boolean | undefined;
  /**
   * Replaces the codes, after a confirmation that the old ones stop working.
   * Return a promise to keep the confirmation pending; pass the new codes in
   * through `codes`. Without it there is no regenerate button.
   */
  onRegenerate?: (() => unknown) | undefined;
  /**
   * Called when the person ticks “I have saved my recovery codes” and presses
   * Continue. Without it neither is shown.
   */
  onAcknowledge?: (() => void) | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<RecoveryCodesMessages> | undefined;
}

function startDownload(text: string, fileName: string) {
  if (typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') return;
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Revoking in the same task can cancel the download in some browsers
  // (Safari, older Firefox); give it time to start first.
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/**
 * One-time recovery codes in a monospace grid, with Copy, Download, an
 * optional Show/Hide toggle, Generate new codes behind a confirmation, and an
 * “I have saved my recovery codes” acknowledgement for setup flows. Used
 * codes are struck through.
 */
export const RecoveryCodes = forwardRef<HTMLDivElement, RecoveryCodesProps>(function RecoveryCodes(
  {
    codes: codesProp,
    fileName = 'recovery-codes.txt',
    formatText = (list) => list.join('\n'),
    revealed: revealedProp,
    defaultRevealed = true,
    onRevealedChange,
    showRevealToggle,
    onRegenerate,
    onAcknowledge,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultRecoveryCodesMessages, ...messages };
  const { confirm, element } = useConfirm();
  const [revealedState, setRevealedState] = useState(defaultRevealed);
  const revealed = revealedProp ?? revealedState;
  const [acknowledged, setAcknowledged] = useState(false);
  const [attempted, setAttempted] = useState(false);
  const [announcement, setAnnouncement] = useState('');
  const mounted = useRef(true);
  const id = useId();
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const codes: RecoveryCode[] = codesProp.map((c) => (typeof c === 'string' ? { code: c } : c));
  const unused = codes.filter((c) => !c.used).map((c) => c.code);
  const anyUsed = unused.length !== codes.length;
  const text = formatText(unused);
  const toggle = showRevealToggle ?? !defaultRevealed;

  // A new set of codes needs a new acknowledgement.
  const signature = codes.map((c) => c.code).join('|');
  const lastSignature = useRef(signature);
  useEffect(() => {
    if (lastSignature.current === signature) return;
    lastSignature.current = signature;
    setAcknowledged(false);
    setAttempted(false);
  }, [signature]);

  const setRevealed = (next: boolean) => {
    if (revealedProp === undefined) setRevealedState(next);
    onRevealedChange?.(next);
  };

  const regenerate = async () => {
    if (!onRegenerate) return;
    setAnnouncement('');
    const result = await confirm({
      title: m.regenerateTitle,
      description: m.regenerateDescription,
      tone: 'danger',
      confirmLabel: m.regenerateConfirm,
      cancelLabel: m.cancel,
      onConfirm: () => onRegenerate(),
    });
    if (result && mounted.current) {
      setAnnouncement(m.regenerated);
      setRevealed(true);
    }
  };

  const errorId = `${id}-ack-error`;
  const missing = attempted && !acknowledged;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-recovery-codes', className)}
      data-revealed={revealed || undefined}
    >
      {anyUsed ? (
        <p className="mn-recovery-codes__count">{m.remaining(unused.length, codes.length)}</p>
      ) : null}
      {revealed ? (
        <ol className="mn-recovery-codes__list" aria-label={m.listLabel}>
          {codes.map((c) => (
            <li key={c.code} className="mn-recovery-codes__item" data-used={c.used || undefined}>
              <code className="mn-recovery-codes__code">{c.used ? <s>{c.code}</s> : c.code}</code>
              {c.used ? <span className="mn-recovery-codes__sr"> {m.used}</span> : null}
            </li>
          ))}
        </ol>
      ) : (
        <p className="mn-recovery-codes__concealed">{m.concealedHint}</p>
      )}
      <div className="mn-recovery-codes__actions">
        {toggle ? (
          <Button
            size="sm"
            variant="ghost"
            aria-pressed={revealed}
            leadingIcon={revealed ? <EyeOffGlyph /> : <EyeGlyph />}
            onClick={() => setRevealed(!revealed)}
          >
            {revealed ? m.conceal : m.reveal}
          </Button>
        ) : null}
        <CopyButton
          size="sm"
          value={text}
          label={m.copy}
          copiedLabel={m.copied}
          disabled={unused.length === 0}
        />
        <Button
          size="sm"
          disabled={unused.length === 0}
          onClick={() => startDownload(text, fileName)}
        >
          {m.download}
        </Button>
        {onRegenerate ? (
          <Button
            size="sm"
            variant="danger-subtle"
            className="mn-recovery-codes__regenerate"
            onClick={() => void regenerate()}
          >
            {m.regenerate}
          </Button>
        ) : null}
      </div>
      {onAcknowledge ? (
        <div className="mn-recovery-codes__acknowledge">
          <Checkbox
            label={m.acknowledge}
            checked={acknowledged}
            invalid={missing}
            {...(missing ? { 'aria-describedby': errorId } : {})}
            onCheckedChange={(next) => {
              setAcknowledged(next === true);
              setAttempted(false);
            }}
          />
          {missing ? (
            <p id={errorId} className="mn-recovery-codes__error" role="alert">
              {m.acknowledgeMissing}
            </p>
          ) : null}
          <Button
            variant="primary"
            className="mn-recovery-codes__continue"
            onClick={() => {
              if (!acknowledged) {
                setAttempted(true);
                return;
              }
              onAcknowledge();
            }}
          >
            {m.continue}
          </Button>
        </div>
      ) : null}
      <p className="mn-recovery-codes__status" role="status" aria-live="polite">
        {announcement}
      </p>
      {element}
    </div>
  );
});

/**
 * Formats codes for copying or download: an optional heading line (such as
 * the product and account), a blank line, then one code per line.
 */
export function formatRecoveryCodes(codes: readonly string[], heading?: string): string {
  return [...(heading ? [heading, ''] : []), ...codes].join('\n');
}
