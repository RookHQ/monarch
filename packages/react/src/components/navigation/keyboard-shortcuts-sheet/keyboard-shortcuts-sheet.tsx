'use client';

import {
  Fragment,
  forwardRef,
  useEffect,
  useId,
  useState,
  type ElementRef,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Kbd } from '../../typography/kbd/index.js';
import { formatHotkey, useHotkey, type Hotkey } from '../../search/command-palette/index.js';
import { SearchInput } from '../../search/search-input/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
  type SheetContentProps,
} from '../../overlays/sheet/index.js';

/** One shortcut in the list. */
export interface KeyboardShortcut {
  /**
   * The keys, as hotkey strings. `mod+k` is ⌘K on Apple platforms and
   * Ctrl+K elsewhere. Separate a sequence with spaces (`g i` is "G then I").
   * Give an array for alternatives (`['j', 'arrowdown']` is "J or ↓").
   */
  keys: string | readonly string[];
  /** What it does, e.g. "Create issue". */
  label: string;
  /** Heading of the group it belongs to, e.g. "Navigation". Shortcuts without one come first. */
  group?: string;
}

/** Visible and spoken strings, for translation. */
export interface KeyboardShortcutsSheetMessages {
  /** Sheet title. Default "Keyboard shortcuts". */
  title: string;
  /** Label of the filter field. Default "Filter shortcuts". */
  filter: string;
  /** Shown when the filter matches nothing. Default "No shortcuts match". */
  empty: string;
  /** Announced as the filter changes. Default "3 shortcuts". */
  count: (count: number) => string;
  /** Between the steps of a sequence. Default "then". */
  then: string;
  /** Between alternatives. Default "or". */
  or: string;
  /** Spoken names of keys whose symbol a screen reader may not say, keyed by hotkey part. */
  keyNames: Record<string, string>;
}

const KEY_NAMES: Record<string, string> = {
  mod: 'Command',
  meta: 'Command',
  cmd: 'Command',
  command: 'Command',
  ctrl: 'Control',
  control: 'Control',
  alt: 'Option',
  option: 'Option',
  shift: 'Shift',
  enter: 'Enter',
  escape: 'Escape',
  esc: 'Escape',
  arrowup: 'Up arrow',
  arrowdown: 'Down arrow',
  arrowleft: 'Left arrow',
  arrowright: 'Right arrow',
  backspace: 'Backspace',
  space: 'Space',
  tab: 'Tab',
  '/': 'Slash',
  '?': 'Question mark',
  '.': 'Period',
  ',': 'Comma',
};

export const KEYBOARD_SHORTCUTS_SHEET_MESSAGES: KeyboardShortcutsSheetMessages = {
  title: 'Keyboard shortcuts',
  filter: 'Filter shortcuts',
  empty: 'No shortcuts match',
  count: (n) => (n === 1 ? '1 shortcut' : `${n} shortcuts`),
  then: 'then',
  or: 'or',
  keyNames: KEY_NAMES,
};

function isApplePlatform(): boolean {
  if (typeof navigator === 'undefined') return false;
  const nav = navigator as Navigator & { userAgentData?: { platform?: string } };
  return /mac|iphone|ipad|ipod/i.test(nav.userAgentData?.platform ?? nav.platform ?? '');
}

function spokenPart(part: string, apple: boolean, names: Record<string, string>): string {
  const lower = part.toLowerCase();
  if (!apple && (lower === 'mod' || lower === 'ctrl' || lower === 'control')) return 'Control';
  if (!apple && (lower === 'alt' || lower === 'option')) return 'Alt';
  if (!apple && (lower === 'meta' || lower === 'cmd' || lower === 'command')) return 'Windows';
  return names[lower] ?? (part.length === 1 ? part.toUpperCase() : part);
}

/** The keys of one shortcut for reading aloud, e.g. "Command K" or "G then I". */
export function describeShortcutKeys(
  keys: KeyboardShortcut['keys'],
  options: { apple?: boolean; messages?: Partial<KeyboardShortcutsSheetMessages> } = {},
): string {
  const apple = options.apple ?? isApplePlatform();
  const m = { ...KEYBOARD_SHORTCUTS_SHEET_MESSAGES, ...options.messages };
  const names = { ...KEY_NAMES, ...options.messages?.keyNames };
  const alternatives = typeof keys === 'string' ? [keys] : keys;
  return alternatives
    .map((alt) =>
      alt
        .trim()
        .split(/\s+/)
        .map((step) => {
          const parts = step.endsWith('++')
            ? [...step.slice(0, -2).split('+'), '+']
            : step.split('+');
          return parts
            .filter(Boolean)
            .map((p) => spokenPart(p, apple, names))
            .join(' ');
        })
        .join(` ${m.then} `),
    )
    .join(` ${m.or} `);
}

function ShortcutKeys({
  keys,
  apple,
  messages,
}: {
  keys: KeyboardShortcut['keys'];
  apple: boolean;
  messages: KeyboardShortcutsSheetMessages;
}) {
  const alternatives = typeof keys === 'string' ? [keys] : keys;
  return (
    <>
      <span className="mn-keyboard-shortcuts-sheet__keys" aria-hidden>
        {alternatives.map((alt, a) => (
          <Fragment key={a}>
            {a > 0 ? (
              <span className="mn-keyboard-shortcuts-sheet__joiner">{messages.or}</span>
            ) : null}
            {alt
              .trim()
              .split(/\s+/)
              .map((step, s) => (
                <Fragment key={s}>
                  {s > 0 ? (
                    <span className="mn-keyboard-shortcuts-sheet__joiner">{messages.then}</span>
                  ) : null}
                  <span className="mn-keyboard-shortcuts-sheet__combo">
                    {formatHotkey(step, { apple }).map((k, i) => (
                      <Kbd key={i}>{k}</Kbd>
                    ))}
                  </span>
                </Fragment>
              ))}
          </Fragment>
        ))}
      </span>
      <span className="mn-visually-hidden">{describeShortcutKeys(keys, { apple, messages })}</span>
    </>
  );
}

export interface KeyboardShortcutsSheetProps extends Omit<
  SheetContentProps,
  'title' | 'children' | 'footer'
> {
  /** The shortcuts, optionally grouped with `group`. */
  shortcuts: readonly KeyboardShortcut[];
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** An element that opens the sheet (rendered with `asChild`), e.g. a help-menu item. */
  trigger?: ReactElement;
  /**
   * Hotkey that opens the sheet from anywhere, ignored while typing in a
   * field. Default `?`. `false` turns it off.
   */
  hotkey?: Hotkey | false;
  /** Text under the title, e.g. where the shortcuts work. */
  description?: ReactNode;
  /** Show a filter field above the list. Default `true` when there are more than 8 shortcuts. */
  filterable?: boolean;
  /**
   * Show Apple symbols (⌘ ⌥ ⇧) or names (Ctrl, Alt, Shift). By default it
   * follows the visitor's platform, detected after mounting so server and
   * first client render agree.
   */
  apple?: boolean;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<KeyboardShortcutsSheetMessages>;
}

/**
 * The list of an app's keyboard shortcuts in a side sheet, grouped, with
 * an optional filter. Opens with "?" (configurable), shows ⌘ or Ctrl to
 * match the platform, and reads each key out by name. The product still
 * binds its own shortcuts; this component only lists them.
 */
export const KeyboardShortcutsSheet = forwardRef<
  ElementRef<typeof SheetContent>,
  KeyboardShortcutsSheetProps
>(function KeyboardShortcutsSheet(
  {
    shortcuts,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    trigger,
    hotkey = '?',
    description,
    filterable,
    apple: appleProp,
    messages: messagesProp,
    side = 'right',
    size = 'sm',
    className,
    ...rest
  },
  ref,
) {
  const messages = {
    ...KEYBOARD_SHORTCUTS_SHEET_MESSAGES,
    ...messagesProp,
    keyNames: { ...KEY_NAMES, ...messagesProp?.keyNames },
  };
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const [detectedApple, setDetectedApple] = useState(false);
  const [query, setQuery] = useState('');
  const headingBase = useId();

  useEffect(() => {
    setDetectedApple(isApplePlatform());
  }, []);
  useEffect(() => {
    if (!open) setQuery('');
  }, [open]);

  const apple = appleProp ?? detectedApple;

  const setOpen = (next: boolean) => {
    if (openProp === undefined) setOpenState(next);
    onOpenChange?.(next);
  };

  useHotkey(hotkey, () => setOpen(true), { enabled: !open, inFields: false });

  const showFilter = filterable ?? shortcuts.length > 8;
  const q = query.trim().toLowerCase();
  const visible = q
    ? shortcuts.filter(
        (s) =>
          s.label.toLowerCase().includes(q) ||
          (s.group ?? '').toLowerCase().includes(q) ||
          describeShortcutKeys(s.keys, { apple, messages }).toLowerCase().includes(q),
      )
    : shortcuts;

  const groups: { title: string | undefined; items: KeyboardShortcut[] }[] = [];
  for (const s of visible) {
    let group = groups.find((g) => g.title === s.group);
    if (!group) {
      group = { title: s.group, items: [] };
      if (s.group === undefined) groups.unshift(group);
      else groups.push(group);
    }
    group.items.push(s);
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      {trigger ? <SheetTrigger asChild>{trigger}</SheetTrigger> : null}
      <SheetContent
        ref={ref}
        side={side}
        size={size}
        {...rest}
        className={cx('mn-keyboard-shortcuts-sheet', className)}
        {...(description ? {} : { 'aria-describedby': undefined })}
      >
        <SheetHeader>
          <SheetTitle>{messages.title}</SheetTitle>
          {description ? <SheetDescription>{description}</SheetDescription> : null}
        </SheetHeader>
        <SheetBody className="mn-keyboard-shortcuts-sheet__body">
          {showFilter ? (
            <div className="mn-keyboard-shortcuts-sheet__filter">
              <SearchInput
                aria-label={messages.filter}
                placeholder={messages.filter}
                value={query}
                onValueChange={setQuery}
                size="sm"
              />
              <span className="mn-visually-hidden" role="status">
                {q ? (visible.length ? messages.count(visible.length) : messages.empty) : ''}
              </span>
            </div>
          ) : null}
          {visible.length === 0 ? (
            <p className="mn-keyboard-shortcuts-sheet__empty">{messages.empty}</p>
          ) : (
            groups.map((group, g) => {
              const headingId = `${headingBase}-${g}`;
              const list = (
                <dl className="mn-keyboard-shortcuts-sheet__list">
                  {group.items.map((s, i) => (
                    <div key={`${s.label}-${i}`} className="mn-keyboard-shortcuts-sheet__row">
                      <dt className="mn-keyboard-shortcuts-sheet__label">{s.label}</dt>
                      <dd className="mn-keyboard-shortcuts-sheet__value">
                        <ShortcutKeys keys={s.keys} apple={apple} messages={messages} />
                      </dd>
                    </div>
                  ))}
                </dl>
              );
              return group.title ? (
                <section
                  key={group.title}
                  className="mn-keyboard-shortcuts-sheet__group"
                  aria-labelledby={headingId}
                >
                  <h3 id={headingId} className="mn-keyboard-shortcuts-sheet__group-title">
                    {group.title}
                  </h3>
                  {list}
                </section>
              ) : (
                <div key="__ungrouped" className="mn-keyboard-shortcuts-sheet__group">
                  {list}
                </div>
              );
            })
          )}
        </SheetBody>
      </SheetContent>
    </Sheet>
  );
});
