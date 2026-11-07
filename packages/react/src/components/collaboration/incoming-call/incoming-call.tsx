'use client';

import { forwardRef, useEffect, useId, useRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar } from '../../identity/avatar/index.js';
import { CallControlButton, CallIcon } from '../call-controls/index.js';

/** Visible and announced strings. Override them to translate. */
export interface IncomingCallMessages {
  label: (name: string, kind: 'audio' | 'video') => string;
  incoming: (kind: 'audio' | 'video') => string;
  decline: string;
  answer: string;
  answerVideo: string;
  answerAudio: string;
}

export const DEFAULT_INCOMING_CALL_MESSAGES: IncomingCallMessages = {
  label: (name, kind) => `Incoming ${kind === 'video' ? 'video call' : 'call'} from ${name}`,
  incoming: (kind) => `Incoming ${kind === 'video' ? 'video call' : 'call'}`,
  decline: 'Decline',
  answer: 'Answer',
  answerVideo: 'Answer with video',
  answerAudio: 'Answer with audio only',
};

export interface IncomingCallProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Who is calling. */
  caller: { name: string; avatarUrl?: string | null | undefined };
  /** `video` adds an "Answer with audio only" choice. Default `audio`. */
  kind?: 'audio' | 'video' | undefined;
  /** Answers. `video` is false for audio calls and for "Answer with audio only". */
  onAccept: (options: { video: boolean }) => void;
  onDecline: () => void;
  /** While joining: the answer buttons are disabled. */
  joining?: boolean | undefined;
  /**
   * Stops ringing after this many milliseconds and calls `onTimeout` (for
   * example to log a missed call). Leave unset when the server ends the ring.
   */
  timeoutMs?: number | undefined;
  onTimeout?: (() => void) | undefined;
  /**
   * `floating` (default) pins it to the top of the viewport; `inline` lets
   * the product place it.
   */
  placement?: 'floating' | 'inline' | undefined;
  /** Moves focus to the answer button when it appears. Default false: a call should not steal typing. */
  focusAnswer?: boolean | undefined;
  /** Extra line under the caller, for example the conversation name. */
  detail?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<IncomingCallMessages> | undefined;
}

/**
 * The ringing banner for an incoming call: who is calling, Decline, and
 * Answer (with video, or audio only for a video call). It never answers by
 * itself and plays no sound; the product rings and joins.
 */
export const IncomingCall = forwardRef<HTMLDivElement, IncomingCallProps>(function IncomingCall(
  {
    caller,
    kind = 'audio',
    onAccept,
    onDecline,
    joining = false,
    timeoutMs,
    onTimeout,
    placement = 'floating',
    focusAnswer = false,
    detail,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_INCOMING_CALL_MESSAGES, ...messagesProp };
  const descriptionId = useId();
  const answerRef = useRef<HTMLButtonElement>(null);
  const timeoutRef = useRef(onTimeout);
  timeoutRef.current = onTimeout;
  const video = kind === 'video';

  useEffect(() => {
    if (!timeoutMs) return;
    const timer = window.setTimeout(() => timeoutRef.current?.(), timeoutMs);
    return () => window.clearTimeout(timer);
  }, [timeoutMs]);

  useEffect(() => {
    if (focusAnswer) answerRef.current?.focus();
  }, [focusAnswer]);

  return (
    <div
      ref={ref}
      role="alertdialog"
      aria-modal="false"
      aria-label={m.label(caller.name, kind)}
      aria-describedby={descriptionId}
      {...rest}
      data-placement={placement}
      data-kind={kind}
      className={cx('mn-incoming-call', className)}
    >
      <span className="mn-incoming-call__avatar">
        <span className="mn-incoming-call__ring" aria-hidden="true" />
        <Avatar name={caller.name} src={caller.avatarUrl ?? null} size="md" decorative />
      </span>
      <div className="mn-incoming-call__text">
        <p className="mn-incoming-call__name">{caller.name}</p>
        <p id={descriptionId} className="mn-incoming-call__kind">
          <CallIcon name={video ? 'video' : 'phone'} width={12} height={12} />
          {m.incoming(kind)}
        </p>
        {detail ? <p className="mn-incoming-call__detail">{detail}</p> : null}
      </div>
      <div className="mn-incoming-call__actions">
        <CallControlButton
          label={m.decline}
          tone="danger"
          icon={<CallIcon name="phone-off" width={18} height={18} />}
          onClick={onDecline}
        />
        {video && (
          <CallControlButton
            label={m.answerAudio}
            disabled={joining}
            icon={<CallIcon name="phone" width={18} height={18} />}
            onClick={() => onAccept({ video: false })}
          />
        )}
        <CallControlButton
          ref={answerRef}
          label={video ? m.answerVideo : m.answer}
          tone="active"
          disabled={joining}
          aria-busy={joining || undefined}
          icon={<CallIcon name={video ? 'video' : 'phone'} width={18} height={18} />}
          onClick={() => onAccept({ video })}
        />
      </div>
    </div>
  );
});
