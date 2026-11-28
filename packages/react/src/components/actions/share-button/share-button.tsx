'use client';

import { forwardRef, useEffect, useId, useRef, useState, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button, type ButtonProps } from '../button/index.js';
import { CopyButton } from '../copy-button/index.js';
import { Popover, PopoverAnchor, PopoverContent } from '../../overlays/popover/index.js';

/** What is shared: the same fields as the Web Share API. */
export interface ShareData {
  /** The link to share. */
  url: string;
  /** Title of the shared page or item. */
  title?: string;
  /** Short text that goes with the link. */
  text?: string;
}

/** A place to share to from the fallback panel, such as email or a chat app. */
export interface ShareTarget {
  /** Stable id, passed to `onShare`. */
  id: string;
  /** Visible text, and the accessible name. */
  label: string;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /**
   * Builds the link that shares the data, e.g. a `mailto:` URL. Links that
   * fail `isSafeUrl` (http, https and mailto only) are not rendered.
   */
  href: (data: ShareData) => string;
}

/** How the person shared, passed to `onShare`. */
export type ShareEvent =
  { method: 'native' } | { method: 'copy' } | { method: 'target'; target: string };

/** Every visible or announced string of ShareButton, for translation. */
export interface ShareButtonMessages {
  /** Button label. Default "Share". */
  label: string;
  /** Accessible name of the fallback panel. Default "Share". */
  panelLabel: string;
  /** Label of the link field. Default "Link". */
  linkLabel: string;
  /** Copy button. Default "Copy link". */
  copy: string;
  /** Copy button after copying. Default "Copied". */
  copied: string;
  /** Accessible name of the list of targets. Default "Share to". */
  targetsLabel: string;
  /** Copy button after a failed copy. Default "Couldn't copy". */
  copyFailed?: string;
  /** Announced after copying. Default "Copied to clipboard". */
  copiedAnnouncement?: string;
  /** Announced after a failed copy. Default "Couldn't copy. Select the text and copy it manually." */
  copyFailedAnnouncement?: string;
}

const DEFAULT_MESSAGES: ShareButtonMessages = {
  label: 'Share',
  panelLabel: 'Share',
  linkLabel: 'Link',
  copy: 'Copy link',
  copied: 'Copied',
  targetsLabel: 'Share to',
};

/** A share target that opens the person's email app with the title as subject and the link in the body. */
export function emailShareTarget(label = 'Email', icon?: ReactNode): ShareTarget {
  return {
    id: 'email',
    label,
    ...(icon ? { icon } : {}),
    href: ({ url, title, text }) => {
      const body = [text, url].filter(Boolean).join('\n\n');
      const query = [
        title ? `subject=${encodeURIComponent(title)}` : '',
        `body=${encodeURIComponent(body)}`,
      ]
        .filter(Boolean)
        .join('&');
      return `mailto:?${query}`;
    },
  };
}

type NativeShare = (data: { url?: string; title?: string; text?: string }) => Promise<void>;

/** The browser's share function when it can share this data; never at render time. */
function nativeShare(data: ShareData): NativeShare | null {
  if (typeof navigator === 'undefined') return null;
  const nav = navigator as Navigator & {
    share?: NativeShare;
    canShare?: (data: { url?: string; title?: string; text?: string }) => boolean;
  };
  if (typeof nav.share !== 'function') return null;
  if (typeof nav.canShare === 'function') {
    try {
      if (!nav.canShare(data)) return null;
    } catch {
      return null;
    }
  }
  return nav.share.bind(nav);
}

function isAbort(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    (error as { name?: unknown }).name === 'AbortError'
  );
}

export interface ShareButtonProps
  extends Omit<ButtonProps, 'children' | 'asChild' | 'onError' | 'title'>, ShareData {
  /**
   * `auto` (default) uses the system share sheet when the browser can share
   * the data, and the panel otherwise or when the share sheet fails; `panel`
   * always shows the panel.
   */
  mode?: 'auto' | 'panel';
  /** Extra places to share to in the panel, after "Copy link". */
  targets?: readonly ShareTarget[];
  /** Hide the visible label and show only `icon`. `messages.label` stays the accessible name. */
  iconOnly?: boolean;
  /** Icon before the label, or the only content with `iconOnly`. Decorative. */
  icon?: ReactNode;
  /** Replaces any of the default strings. */
  messages?: Partial<ShareButtonMessages>;
  /** Called after the person shared: through the share sheet, by copying, or with a target. */
  onShare?: ((event: ShareEvent) => void) | undefined;
  /** Called when the person closes the system share sheet without sharing. */
  onShareCancel?: (() => void) | undefined;
  /** Called when the share sheet fails for another reason; the panel opens instead. */
  onShareError?: ((error: Error) => void) | undefined;
  /** Tooltip of the button (the shared `title` is a separate prop). */
  tooltip?: string;
  /** Whether the panel is open (controlled), for example to show it from the start. */
  open?: boolean | undefined;
  /** Called when the panel opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
}

/**
 * Shares a link. Uses the system share sheet (Web Share API) when the
 * browser offers one; otherwise opens a small panel with the link, a Copy
 * link button and optional targets such as email. Nothing is shared until
 * the person presses the button, and cancelling the share sheet is not an
 * error.
 */
export const ShareButton = forwardRef<HTMLButtonElement, ShareButtonProps>(function ShareButton(
  {
    url,
    title,
    text,
    mode = 'auto',
    targets,
    iconOnly = false,
    icon,
    messages: messagesProp,
    onShare,
    onShareCancel,
    onShareError,
    tooltip,
    open: openProp,
    onOpenChange,
    onClick,
    leadingIcon,
    variant = iconOnly ? 'ghost' : 'secondary',
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const [open, setOpen] = useControllable(openProp, false, onOpenChange);
  const [busy, setBusy] = useState(false);
  // Whether a click will open the panel, known only in the browser.
  const [panelFirst, setPanelFirst] = useState(mode === 'panel');
  const buttonRef = useRef<HTMLButtonElement | null>(null);
  const fieldId = useId();
  const data: ShareData = {
    url,
    ...(title !== undefined ? { title } : {}),
    ...(text !== undefined ? { text } : {}),
  };

  useEffect(() => {
    setPanelFirst(mode === 'panel' || !nativeShare(data));
    // `data` is rebuilt every render; its fields are the real dependencies.
  }, [mode, url, title, text]); // eslint-disable-line react-hooks/exhaustive-deps

  const share = async () => {
    const native = mode === 'panel' ? null : nativeShare(data);
    if (!native) {
      setOpen(true);
      return;
    }
    setBusy(true);
    try {
      await native(data);
      onShare?.({ method: 'native' });
    } catch (raw) {
      if (isAbort(raw)) {
        onShareCancel?.();
      } else {
        onShareError?.(raw instanceof Error ? raw : new Error(String(raw)));
        setOpen(true);
      }
    } finally {
      setBusy(false);
    }
  };

  const safeTargets = (targets ?? [])
    .map((target) => ({ target, href: target.href(data) }))
    .filter(({ href }) => isSafeUrl(href));

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverAnchor asChild>
        <Button
          ref={(node) => {
            buttonRef.current = node;
            if (typeof ref === 'function') ref(node);
            else if (ref) ref.current = node;
          }}
          {...rest}
          variant={variant}
          loading={rest.loading || busy}
          leadingIcon={iconOnly ? undefined : (leadingIcon ?? icon)}
          aria-label={iconOnly ? messages.label : rest['aria-label']}
          title={tooltip ?? (iconOnly ? messages.label : undefined)}
          aria-haspopup={panelFirst ? 'dialog' : undefined}
          aria-expanded={panelFirst || open ? open : undefined}
          data-icon-only={iconOnly || undefined}
          data-state={open ? 'open' : 'closed'}
          className={cx('mn-share-button', iconOnly && 'mn-icon-button', className)}
          onClick={(event) => {
            onClick?.(event);
            if (event.defaultPrevented) return;
            if (open) {
              setOpen(false);
              return;
            }
            void share();
          }}
        >
          {iconOnly ? (
            <span className="mn-button__icon" aria-hidden>
              {icon}
            </span>
          ) : (
            messages.label
          )}
        </Button>
      </PopoverAnchor>
      <PopoverContent
        aria-label={messages.panelLabel}
        className="mn-share-button__panel"
        onCloseAutoFocus={(event) => {
          event.preventDefault();
          buttonRef.current?.focus();
        }}
        onInteractOutside={(event) => {
          // A click on the button itself toggles the panel; do not close and reopen it.
          if (buttonRef.current?.contains(event.target as Node)) event.preventDefault();
        }}
      >
        <label className="mn-share-button__label" htmlFor={fieldId}>
          {messages.linkLabel}
        </label>
        <div className="mn-share-button__row">
          <input
            id={fieldId}
            className="mn-input mn-share-button__url"
            type="url"
            readOnly
            value={url}
            onFocus={(event) => event.currentTarget.select()}
          />
          <CopyButton
            value={url}
            label={messages.copy}
            copiedLabel={messages.copied}
            {...(messages.copyFailed ? { errorLabel: messages.copyFailed } : {})}
            {...(messages.copiedAnnouncement ? { copiedMessage: messages.copiedAnnouncement } : {})}
            {...(messages.copyFailedAnnouncement
              ? { errorMessage: messages.copyFailedAnnouncement }
              : {})}
            onCopy={() => onShare?.({ method: 'copy' })}
          />
        </div>
        {safeTargets.length > 0 ? (
          <ul className="mn-share-button__targets" aria-label={messages.targetsLabel}>
            {safeTargets.map(({ target, href }) => (
              <li key={target.id}>
                <a
                  className="mn-share-button__target"
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={() => onShare?.({ method: 'target', target: target.id })}
                >
                  {target.icon ? (
                    <span className="mn-share-button__target-icon" aria-hidden>
                      {target.icon}
                    </span>
                  ) : null}
                  {target.label}
                </a>
              </li>
            ))}
          </ul>
        ) : null}
      </PopoverContent>
    </Popover>
  );
});
