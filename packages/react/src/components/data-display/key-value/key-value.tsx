import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CopyButton } from '../../actions/copy-button/index.js';

export interface KeyValueProps extends HTMLAttributes<HTMLDListElement> {
  /**
   * What sits between pairs: `dot` (default) a small middle dot, `divider` a
   * thin vertical line, `none` only space. Decorative; drawn in CSS.
   */
  separator?: 'dot' | 'divider' | 'none' | undefined;
  /** `sm` (default) for metadata under a title, `md` next to body text. */
  size?: 'sm' | 'md' | undefined;
  /**
   * `wrap` (default) lets pairs flow onto more lines; `stack` puts each pair on
   * its own line, the layout used inside narrow cards.
   */
  layout?: 'wrap' | 'stack' | undefined;
}

/**
 * Compact metadata as inline label and value pairs, such as
 * "Region eu-west · Plan Team · ID 4f2a…". A `<dl>`, so each label stays tied
 * to its value for assistive technology. For a grid of facts on a detail
 * page, use DescriptionList.
 */
export const KeyValue = forwardRef<HTMLDListElement, KeyValueProps>(function KeyValue(
  { separator = 'dot', size = 'sm', layout = 'wrap', className, ...rest },
  ref,
) {
  return (
    <dl
      ref={ref}
      {...rest}
      className={cx('mn-key-value', className)}
      data-separator={separator}
      data-size={size}
      data-layout={layout}
    />
  );
});

/** The words KeyValueItem shows and speaks. Override them to translate. */
export interface KeyValueItemMessages {
  /** Read for an empty value, which is shown as a dash. Default "Not set". */
  empty: string;
  /** Accessible name of the copy button. Default "Copy {label}" (or "Copy value" when the label is not text). */
  copy: (label: string | null) => string;
}

const DEFAULT_MESSAGES: KeyValueItemMessages = {
  empty: 'Not set',
  copy: (label) => (label ? `Copy ${label}` : 'Copy value'),
};

export interface KeyValueItemProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The key, shown muted before the value ("Region"). */
  label: ReactNode;
  /** The value. `null`, `undefined`, `false` and `''` show a dash read as `messages.empty`. */
  children?: ReactNode;
  /**
   * Adds an icon CopyButton after the value. `true` copies the value when it
   * is a string or number; pass a string to copy something else, such as the
   * full id behind a shortened one. Not shown for an empty value.
   */
  copyable?: boolean | string | undefined;
  /** Show the value in the monospace font, for ids, hashes and keys. */
  mono?: boolean | undefined;
  /** Called with the copied text after a successful copy. */
  onCopyValue?: ((text: string) => void) | undefined;
  /** Words to show and speak; merged over the English defaults. */
  messages?: Partial<KeyValueItemMessages> | undefined;
}

function isEmpty(value: ReactNode) {
  return value === null || value === undefined || value === false || value === '';
}

/** One label and value pair inside KeyValue. */
export const KeyValueItem = forwardRef<HTMLDivElement, KeyValueItemProps>(function KeyValueItem(
  {
    label,
    children,
    copyable,
    mono = false,
    onCopyValue,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const empty = isEmpty(children);
  const copyText =
    typeof copyable === 'string'
      ? copyable
      : copyable && (typeof children === 'string' || typeof children === 'number')
        ? String(children)
        : null;
  const textLabel = typeof label === 'string' || typeof label === 'number' ? String(label) : null;
  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-key-value__item', className)}
      data-empty={empty || undefined}
      data-mono={mono || undefined}
    >
      <dt className="mn-key-value__key">{label}</dt>
      <dd className="mn-key-value__value">
        {empty ? (
          <>
            <span aria-hidden="true">—</span>
            <span className="mn-visually-hidden">{messages.empty}</span>
          </>
        ) : (
          <span className="mn-key-value__text">{children}</span>
        )}
        {copyText !== null && !empty ? (
          <CopyButton
            value={copyText}
            iconOnly
            size="xs"
            variant="ghost"
            label={messages.copy(textLabel)}
            className="mn-key-value__copy"
            {...(onCopyValue !== undefined && { onCopy: onCopyValue })}
          />
        ) : null}
      </dd>
    </div>
  );
});
