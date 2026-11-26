import { forwardRef, type HTMLAttributes, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar } from '../../identity/avatar/index.js';

export interface ReadReceiptReader {
  /** Stable id, used as the React key and the avatar colour seed. Default: the name. */
  id?: string | undefined;
  /** Display name, read out in the full “Seen by …” sentence. */
  name: string;
  /** Avatar image for `display="avatars"`. Initials are shown without it. */
  avatarSrc?: string | null | undefined;
}

export type ReadReceiptDisplay = 'text' | 'count' | 'avatars';

export interface ReadReceiptMessages {
  /** Visible text for `display="text"` and `"avatars"`. Default "Seen". */
  seen: string;
  /** Visible text for `display="count"`. Default "Seen by {count}". */
  seenByCount: (count: number) => string;
  /** The full sentence for screen readers and the tooltip. Default "Seen by {names}". */
  seenBy: (names: string) => string;
  /** Joins reader names. Default "Ana", "Ana and Ben", "Ana, Ben and Chloé". */
  list: (names: readonly string[]) => string;
}

const DEFAULT_MESSAGES: ReadReceiptMessages = {
  seen: 'Seen',
  seenByCount: (count) => `Seen by ${count}`,
  seenBy: (names) => `Seen by ${names}`,
  list: (names) =>
    names.length <= 1
      ? (names[0] ?? '')
      : `${names.slice(0, -1).join(', ')} and ${names[names.length - 1]}`,
};

export interface ReadReceiptProps extends Omit<HTMLAttributes<HTMLParagraphElement>, 'children'> {
  /**
   * People who have read the message. Nothing renders while the list is
   * empty. Leave the sender out.
   */
  readers: readonly ReadReceiptReader[];
  /**
   * `text` (default) shows “Seen”, the usual choice in a one-to-one
   * conversation; `count` shows “Seen by 3” for groups; `avatars` shows the
   * readers' faces. Every display reads the full list of names to screen
   * readers.
   */
  display?: ReadReceiptDisplay;
  /** With `display="avatars"`, show at most this many faces, then “+N”. Default 3. */
  maxAvatars?: number;
  /** Which side of the conversation the receipt sits on. Default `end`, under the sender's own message. */
  align?: 'start' | 'end';
  /**
   * Makes the receipt a button, for example to open the full list of readers
   * with times. Without it the receipt is plain text.
   */
  onShowReaders?: (() => void) | undefined;
  /** Translated text. Unset keys keep their English defaults. */
  messages?: Partial<ReadReceiptMessages>;
}

function DoubleCheckGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable="false"
      {...props}
    >
      <path d="M18 6 7 17l-5-5" />
      <path d="m22 10-7.5 7.5L13 16" />
    </svg>
  );
}

/**
 * “Seen” under the newest message the other people have read. The product
 * decides which message carries it and who has read it; the receipt shows
 * “Seen”, “Seen by 3” or the readers' avatars, and always gives screen
 * readers and the tooltip the full list of names.
 */
export const ReadReceipt = forwardRef<HTMLParagraphElement, ReadReceiptProps>(function ReadReceipt(
  {
    readers,
    display = 'text',
    maxAvatars = 3,
    align = 'end',
    onShowReaders,
    messages,
    className,
    title,
    ...rest
  },
  ref,
) {
  if (readers.length === 0) return null;
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const full = m.seenBy(m.list(readers.map((r) => r.name)));
  const limit = Math.max(1, Math.floor(maxAvatars));
  const shown = readers.slice(0, readers.length - limit === 1 ? readers.length : limit);
  const hidden = readers.length - shown.length;

  const visual = (
    <span className="mn-read-receipt__visual" aria-hidden>
      {display === 'avatars' ? (
        <span className="mn-read-receipt__avatars">
          {shown.map((r) => (
            <Avatar
              key={r.id ?? r.name}
              name={r.name}
              src={r.avatarSrc ?? null}
              seed={r.id ?? r.name}
              size={16}
              decorative
            />
          ))}
          {hidden > 0 ? <span className="mn-read-receipt__more">{`+${hidden}`}</span> : null}
        </span>
      ) : (
        <DoubleCheckGlyph className="mn-read-receipt__icon" />
      )}
      <span className="mn-read-receipt__label">
        {display === 'count' ? m.seenByCount(readers.length) : m.seen}
      </span>
    </span>
  );
  const spoken = <span className="mn-visually-hidden">{full}</span>;

  return (
    <p
      ref={ref}
      {...rest}
      className={cx('mn-read-receipt', className)}
      data-display={display}
      data-align={align}
      title={title ?? full}
    >
      {onShowReaders ? (
        <button type="button" className="mn-read-receipt__button" onClick={onShowReaders}>
          {visual}
          {spoken}
        </button>
      ) : (
        <>
          {visual}
          {spoken}
        </>
      )}
    </p>
  );
});
