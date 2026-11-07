'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CloseGlyph, DashGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Field, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Textarea } from '../../forms/text-field/index.js';
import type { AiToolCallPart } from '../ai-conversation/index.js';
import { formatToolValue, toolLabel } from '../ai-tool-call/index.js';

/** How much harm approving can do. `destructive` cannot be undone. */
export type AiToolRisk = 'low' | 'medium' | 'high' | 'destructive';

/** Every user-visible string in AiToolApproval. */
export interface AiToolApprovalMessages {
  /** Small label above the title. Default "Approval needed". */
  eyebrow: string;
  /** The risk badge text per risk. */
  risks: Record<AiToolRisk, string>;
  /**
   * The consequence line shown for `high` and `destructive` risk when no
   * `consequence` prop is given. Empty strings show nothing.
   */
  consequences: Record<AiToolRisk, string>;
  /** Heading of the arguments list. Default "Details". */
  arguments: string;
  /** Shown when the call has no arguments. Default "No arguments." */
  noArguments: string;
  approve: string;
  deny: string;
  /** Opens the JSON editor. Default "Edit arguments". */
  editArguments: string;
  /** Closes the JSON editor and drops the edits. Default "Discard edits". */
  discardEdits: string;
  /** Label of the JSON editor. Default "Arguments (JSON)". */
  argumentsJson: string;
  /** Shown when the edited arguments are not valid JSON. */
  invalidJson: string;
  /** Opens the reason field. Default "Add a reason". */
  addReason: string;
  /** Label of the reason field. Default "Reason (optional)". */
  reason: string;
  /** Label of the always-allow checkbox. Default "Always allow this tool". */
  alwaysAllow: string;
  /** Shown once the call is approved. Default "Approved". */
  approved: string;
  /** Shown once the call is denied. Default "Denied". */
  denied: string;
  /** Shown when the reply stopped before anyone decided. Default "Cancelled". */
  cancelled: string;
  /** Announced assertively when the card appears. */
  announce: (title: string) => string;
}

export const AI_TOOL_APPROVAL_MESSAGES: AiToolApprovalMessages = {
  eyebrow: 'Approval needed',
  risks: {
    low: 'Low risk',
    medium: 'Medium risk',
    high: 'High risk',
    destructive: 'Can’t be undone',
  },
  consequences: {
    low: '',
    medium: '',
    high: '',
    destructive: 'This can’t be undone once it runs.',
  },
  arguments: 'Details',
  noArguments: 'No arguments.',
  approve: 'Approve',
  deny: 'Deny',
  editArguments: 'Edit arguments',
  discardEdits: 'Discard edits',
  argumentsJson: 'Arguments (JSON)',
  invalidJson: 'Fix the JSON before approving.',
  addReason: 'Add a reason',
  reason: 'Reason (optional)',
  alwaysAllow: 'Always allow this tool',
  approved: 'Approved',
  denied: 'Denied',
  cancelled: 'Cancelled',
  announce: (title) => `Approval needed: ${title}`,
};

const RISK_TONE = {
  low: 'neutral',
  medium: 'warning',
  high: 'warning',
  destructive: 'danger',
} as const;

export interface AiToolApprovalProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'part'
> {
  /** The tool call waiting for a decision (`state: 'pending-approval'`). */
  part: AiToolCallPart;
  /**
   * Called when the person approves. `input` is set only when they edited the
   * arguments, so `approveTool(part.id, input)` passes it straight on.
   */
  onApprove: (input?: unknown) => void;
  /** Called when the person denies, with the reason they typed, if any. */
  onDeny: (reason?: string) => void;
  /**
   * Shows "Always allow this tool". When ticked, it is called with the tool
   * name just before `onApprove`. Never offered for `destructive` risk.
   */
  onAlwaysAllow?: ((toolName: string) => void) | undefined;
  /**
   * How much harm approving can do. `destructive` uses the danger tone, shows
   * the consequence line and puts focus on Deny, never on Approve. Default `medium`.
   */
  risk?: AiToolRisk;
  /**
   * The title in plain words, e.g. "Send email to 3 people" or
   * "Delete 12 records". Default: the tool's label.
   */
  describe?: ((part: AiToolCallPart) => ReactNode | undefined) | undefined;
  /** What happens if this is approved. Default: `messages.consequences[risk]`. */
  consequence?: ReactNode;
  /** Display names per tool name; unlisted tools get a humanized name. */
  toolLabels?: Readonly<Record<string, string>> | undefined;
  /** Labels for argument keys, e.g. `{ to: 'Recipients' }`. Unlisted keys are humanized. */
  argumentLabels?: Readonly<Record<string, string>> | undefined;
  /** Offers "Edit arguments". Default true. */
  editable?: boolean;
  /**
   * Moves focus into the card when it appears: to Deny for `high` and
   * `destructive`, to Approve otherwise. Default false (focus is never taken
   * unless you ask).
   */
  focusOnMount?: boolean;
  /** Replaces any of the English strings. `risks` and `consequences` merge per risk. */
  messages?: Partial<Omit<AiToolApprovalMessages, 'risks' | 'consequences'>> & {
    risks?: Partial<Record<AiToolRisk, string>>;
    consequences?: Partial<Record<AiToolRisk, string>>;
  };
}

function humanizeKey(key: string): string {
  const words = key
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_\-.]+/g, ' ')
    .trim()
    .toLowerCase();
  return words ? words.charAt(0).toUpperCase() + words.slice(1) : key;
}

function readable(value: unknown): string {
  if (value === null || value === undefined) return '—';
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean' || typeof value === 'bigint')
    return String(value);
  if (
    Array.isArray(value) &&
    value.every((v) => v === null || ['string', 'number', 'boolean'].includes(typeof v))
  )
    return value.map((v) => String(v)).join(', ');
  return formatToolValue(value).text;
}

function textOf(node: ReactNode): string {
  if (node === null || node === undefined || typeof node === 'boolean') return '';
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node)) return node.map(textOf).join('');
  return '';
}

function setRefs<T>(value: T, ...refs: (Ref<T> | undefined)[]) {
  for (const r of refs) {
    if (typeof r === 'function') r(value);
    else if (r) (r as { current: T }).current = value;
  }
}

/**
 * The approval gate for a tool call the assistant wants to run ("Send email
 * to 3 people", "Delete 12 records"): what it will do, its arguments as
 * readable key/value pairs, the risk, and Approve / Deny. People can edit the
 * arguments as JSON before approving, give a reason when denying and, when
 * offered, always allow the tool. Destructive calls use the danger tone, state
 * the consequence and never put focus on Approve. Announced assertively when
 * it appears.
 */
export const AiToolApproval = forwardRef<HTMLDivElement, AiToolApprovalProps>(
  function AiToolApproval(
    {
      part,
      onApprove,
      onDeny,
      onAlwaysAllow,
      risk = 'medium',
      describe,
      consequence,
      toolLabels,
      argumentLabels,
      editable = true,
      focusOnMount = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages: AiToolApprovalMessages = {
      ...AI_TOOL_APPROVAL_MESSAGES,
      ...messagesProp,
      risks: { ...AI_TOOL_APPROVAL_MESSAGES.risks, ...messagesProp?.risks },
      consequences: { ...AI_TOOL_APPROVAL_MESSAGES.consequences, ...messagesProp?.consequences },
    };
    const id = useId();
    const titleId = `${id}-title`;
    const consequenceId = `${id}-consequence`;
    const rootRef = useRef<HTMLDivElement | null>(null);
    const denyRef = useRef<HTMLButtonElement>(null);
    const approveRef = useRef<HTMLButtonElement>(null);
    const jsonRef = useRef<HTMLTextAreaElement>(null);

    const label = toolLabel(part.name, toolLabels);
    const title = describe?.(part) ?? label;
    const titleText = textOf(title) || label;
    const cautious = risk === 'high' || risk === 'destructive';
    const consequenceText = consequence ?? (messages.consequences[risk] || null);
    const pending = part.state === 'pending-approval';
    const [decision, setDecision] = useState<'approved' | 'denied' | null>(null);
    const resolved = !pending || decision !== null;
    const outcome: 'approved' | 'denied' | 'cancelled' =
      decision ??
      (part.state === 'denied' ? 'denied' : part.state === 'cancelled' ? 'cancelled' : 'approved');

    const original = formatToolValue(part.input ?? {}).text;
    const [editing, setEditing] = useState(false);
    const [json, setJson] = useState(original);
    const [jsonError, setJsonError] = useState(false);
    const [reasonOpen, setReasonOpen] = useState(false);
    const [reason, setReason] = useState('');
    const [always, setAlways] = useState(false);
    const offerAlways = Boolean(onAlwaysAllow) && risk !== 'destructive';

    // A new call resets the local decision.
    useEffect(() => {
      setDecision(null);
      setEditing(false);
      setJson(formatToolValue(part.input ?? {}).text);
      setJsonError(false);
      setReasonOpen(false);
      setReason('');
      setAlways(false);
      // eslint-disable-next-line react-hooks/exhaustive-deps -- reset per call id only.
    }, [part.id]);

    // Announce assertively once the card is on screen (an alert present at
    // mount is not reliably read).
    const [announcement, setAnnouncement] = useState('');
    useEffect(() => {
      if (pending) setAnnouncement(messages.announce(titleText));
      // eslint-disable-next-line react-hooks/exhaustive-deps -- once per call.
    }, [part.id]);

    useEffect(() => {
      if (!focusOnMount || !pending) return;
      (cautious ? denyRef.current : approveRef.current)?.focus();
      // eslint-disable-next-line react-hooks/exhaustive-deps -- only when the card appears.
    }, [part.id]);

    // When the buttons go away, keep focus in the card instead of dropping it on the body.
    const refocus = useRef(false);
    const rememberFocus = () => {
      refocus.current =
        typeof document !== 'undefined' &&
        Boolean(rootRef.current?.contains(document.activeElement));
    };
    useEffect(() => {
      if (resolved && refocus.current) {
        refocus.current = false;
        rootRef.current?.focus();
      }
    }, [resolved]);

    const jsonOpened = useRef(false);
    useEffect(() => {
      if (editing && jsonOpened.current) jsonRef.current?.focus();
      jsonOpened.current = false;
    }, [editing]);
    const reasonRef = useRef<HTMLTextAreaElement>(null);
    useEffect(() => {
      if (reasonOpen) reasonRef.current?.focus();
    }, [reasonOpen]);

    const approve = useCallback(() => {
      if (resolved) return;
      let input: unknown;
      if (editing && json.trim() !== original.trim()) {
        try {
          input = JSON.parse(json);
        } catch {
          setJsonError(true);
          jsonRef.current?.focus();
          return;
        }
      }
      if (always && offerAlways) onAlwaysAllow?.(part.name);
      rememberFocus();
      setDecision('approved');
      if (input !== undefined) onApprove(input);
      else onApprove();
    }, [
      resolved,
      editing,
      json,
      original,
      always,
      offerAlways,
      onAlwaysAllow,
      onApprove,
      part.name,
    ]);

    const deny = useCallback(() => {
      if (resolved) return;
      const why = reason.trim();
      rememberFocus();
      setDecision('denied');
      if (why) onDeny(why);
      else onDeny();
    }, [resolved, reason, onDeny]);

    const args =
      part.input && typeof part.input === 'object' && !Array.isArray(part.input)
        ? Object.entries(part.input as Record<string, unknown>)
        : null;

    return (
      <div
        ref={(node) => setRefs(node, ref, rootRef)}
        role="group"
        aria-labelledby={titleId}
        aria-describedby={consequenceText ? consequenceId : undefined}
        tabIndex={resolved ? -1 : undefined}
        {...rest}
        className={cx('mn-ai-tool-approval', className)}
        data-risk={risk}
        data-state={resolved ? outcome : 'pending'}
      >
        <div className="mn-ai-tool-approval__header">
          <span className="mn-ai-tool-approval__icon" aria-hidden>
            <WarningGlyph />
          </span>
          <div className="mn-ai-tool-approval__heading">
            <p className="mn-ai-tool-approval__eyebrow">{messages.eyebrow}</p>
            <p id={titleId} className="mn-ai-tool-approval__title">
              {title}
            </p>
          </div>
          <Badge className="mn-ai-tool-approval__risk" tone={RISK_TONE[risk]} size="sm">
            {messages.risks[risk]}
          </Badge>
        </div>

        {consequenceText ? (
          <p id={consequenceId} className="mn-ai-tool-approval__consequence">
            <WarningGlyph className="mn-ai-tool-approval__consequence-icon" />
            <span>{consequenceText}</span>
          </p>
        ) : null}

        <div className="mn-ai-tool-approval__args">
          <p className="mn-ai-tool-approval__args-label">{messages.arguments}</p>
          {args && args.length ? (
            <dl className="mn-ai-tool-approval__list">
              {args.map(([key, value]) => (
                <div key={key} className="mn-ai-tool-approval__row">
                  <dt className="mn-ai-tool-approval__key">
                    {argumentLabels?.[key] ?? humanizeKey(key)}
                  </dt>
                  <dd className="mn-ai-tool-approval__value">{readable(value)}</dd>
                </div>
              ))}
            </dl>
          ) : args || part.input === undefined || part.input === null ? (
            <p className="mn-ai-tool-approval__empty">{messages.noArguments}</p>
          ) : (
            <pre className="mn-ai-tool-approval__raw">{readable(part.input)}</pre>
          )}
        </div>

        {resolved ? (
          <p className="mn-ai-tool-approval__outcome" data-outcome={outcome}>
            <span className="mn-ai-tool-approval__outcome-icon" aria-hidden>
              {outcome === 'approved' ? (
                <CheckGlyph />
              ) : outcome === 'denied' ? (
                <CloseGlyph />
              ) : (
                <DashGlyph />
              )}
            </span>
            {messages[outcome]}
          </p>
        ) : (
          <>
            {editing ? (
              <Field className="mn-ai-tool-approval__editor" invalid={jsonError}>
                <FieldLabel>{messages.argumentsJson}</FieldLabel>
                <Textarea
                  ref={jsonRef}
                  className="mn-ai-tool-approval__json"
                  value={json}
                  rows={Math.min(12, Math.max(3, json.split('\n').length))}
                  spellCheck={false}
                  onChange={(e) => {
                    setJson(e.target.value);
                    if (jsonError) setJsonError(false);
                  }}
                />
                {jsonError ? <FieldError>{messages.invalidJson}</FieldError> : null}
              </Field>
            ) : null}

            {reasonOpen ? (
              <Field className="mn-ai-tool-approval__reason">
                <FieldLabel>{messages.reason}</FieldLabel>
                <Textarea
                  ref={reasonRef}
                  rows={2}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              </Field>
            ) : null}

            {offerAlways ? (
              <Checkbox
                className="mn-ai-tool-approval__always"
                label={messages.alwaysAllow}
                checked={always}
                onCheckedChange={(v) => setAlways(v === true)}
              />
            ) : null}

            <div className="mn-ai-tool-approval__actions">
              <div className="mn-ai-tool-approval__secondary">
                {editable ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    aria-expanded={editing}
                    onClick={() => {
                      if (editing) {
                        setJson(original);
                        setJsonError(false);
                      } else jsonOpened.current = true;
                      setEditing(!editing);
                    }}
                  >
                    {editing ? messages.discardEdits : messages.editArguments}
                  </Button>
                ) : null}
                {!reasonOpen ? (
                  <Button size="sm" variant="ghost" onClick={() => setReasonOpen(true)}>
                    {messages.addReason}
                  </Button>
                ) : null}
              </div>
              <div className="mn-ai-tool-approval__primary">
                <Button ref={denyRef} size="sm" variant="secondary" onClick={deny}>
                  {messages.deny}
                </Button>
                <Button
                  ref={approveRef}
                  size="sm"
                  variant={risk === 'destructive' ? 'danger' : 'primary'}
                  onClick={approve}
                >
                  {messages.approve}
                </Button>
              </div>
            </div>
          </>
        )}
        <span role="alert" className="mn-visually-hidden">
          {pending && !resolved ? announcement : ''}
        </span>
      </div>
    );
  },
);
