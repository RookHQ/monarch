'use client';

import { useEffect, useRef } from 'react';

/**
 * Hotkeys are strings such as `mod+k`, `shift+?` or `/`. `mod` is ⌘ on Apple
 * platforms and Ctrl elsewhere. Several alternatives can be given as an array.
 */
export type Hotkey = string | readonly string[];

interface ParsedHotkey {
  key: string;
  mod: boolean;
  ctrl: boolean;
  meta: boolean;
  alt: boolean;
  shift: boolean;
}

export function isApplePlatform(): boolean {
  if (typeof navigator === 'undefined') return false;
  const nav = navigator as Navigator & { userAgentData?: { platform?: string } };
  const platform = nav.userAgentData?.platform ?? nav.platform ?? '';
  return /mac|iphone|ipad|ipod/i.test(platform);
}

function parse(hotkey: string): ParsedHotkey {
  const parts = hotkey.toLowerCase().split('+');
  // A trailing "+" means the plus key itself ("mod++").
  const key = hotkey.endsWith('++') ? '+' : (parts.pop() ?? '');
  const has = (m: string) => parts.includes(m);
  return {
    key,
    mod: has('mod'),
    ctrl: has('ctrl') || has('control'),
    meta: has('meta') || has('cmd') || has('command'),
    alt: has('alt') || has('option'),
    shift: has('shift'),
  };
}

const list = (hotkey: Hotkey): readonly string[] =>
  typeof hotkey === 'string' ? [hotkey] : hotkey;

/** Whether a keyboard event matches the hotkey (any of the alternatives). */
export function matchesHotkey(event: KeyboardEvent, hotkey: Hotkey): boolean {
  const apple = isApplePlatform();
  return list(hotkey).some((h) => {
    const p = parse(h);
    const wantMeta = p.meta || (p.mod && apple);
    const wantCtrl = p.ctrl || (p.mod && !apple);
    if (event.metaKey !== wantMeta || event.ctrlKey !== wantCtrl || event.altKey !== p.alt)
      return false;
    const key = event.key.toLowerCase();
    // Printable symbols such as "?" already include Shift; only check it for letters and named keys.
    const symbol = key.length === 1 && !/[a-z0-9]/.test(key);
    if (!symbol && event.shiftKey !== p.shift) return false;
    return key === p.key || (p.key === 'space' && key === ' ');
  });
}

/** True for a hotkey with ⌘, Ctrl or Alt, which cannot be typed as text. */
function isModified(hotkey: string) {
  const p = parse(hotkey);
  return p.mod || p.ctrl || p.meta || p.alt;
}

/** Whether focus is in a place where keys type text. */
export function isTypingTarget(target: EventTarget | null): boolean {
  if (typeof Element === 'undefined' || !(target instanceof Element)) return false;
  if (target instanceof HTMLElement && target.isContentEditable) return true;
  if (target.closest('[contenteditable=""], [contenteditable="true"]')) return true;
  const tag = target.tagName;
  if (tag === 'TEXTAREA' || tag === 'SELECT') return true;
  if (tag === 'INPUT') {
    const type = (target as HTMLInputElement).type;
    return !['button', 'checkbox', 'radio', 'range', 'reset', 'submit', 'color', 'file'].includes(
      type,
    );
  }
  const role = target.getAttribute('role');
  return role === 'textbox' || role === 'combobox' || role === 'searchbox';
}

const SYMBOLS_APPLE: Record<string, string> = {
  mod: '⌘',
  meta: '⌘',
  cmd: '⌘',
  command: '⌘',
  ctrl: '⌃',
  control: '⌃',
  alt: '⌥',
  option: '⌥',
  shift: '⇧',
};
const NAMES_OTHER: Record<string, string> = {
  mod: 'Ctrl',
  ctrl: 'Ctrl',
  control: 'Ctrl',
  meta: 'Win',
  cmd: 'Win',
  command: 'Win',
  alt: 'Alt',
  option: 'Alt',
  shift: 'Shift',
};
const KEY_NAMES: Record<string, string> = {
  enter: '↵',
  escape: 'Esc',
  esc: 'Esc',
  arrowup: '↑',
  arrowdown: '↓',
  arrowleft: '←',
  arrowright: '→',
  backspace: '⌫',
  space: 'Space',
  tab: 'Tab',
};

/**
 * The keys of a hotkey for display, one string per key: `mod+k` is
 * `['⌘', 'K']` on Apple platforms and `['Ctrl', 'K']` elsewhere.
 */
export function formatHotkey(hotkey: string, options: { apple?: boolean } = {}): string[] {
  const apple = options.apple ?? isApplePlatform();
  const parts = hotkey.split('+');
  const key = hotkey.endsWith('++') ? '+' : (parts.pop() ?? '');
  if (hotkey.endsWith('++')) parts.pop();
  const mods = parts.map((m) => {
    const lower = m.toLowerCase();
    return (apple ? SYMBOLS_APPLE[lower] : NAMES_OTHER[lower]) ?? m;
  });
  const named = KEY_NAMES[key.toLowerCase()];
  return [
    ...mods,
    named ?? (key.length === 1 || /^f\d{1,2}$/i.test(key) ? key.toUpperCase() : key),
  ];
}

export interface UseHotkeyOptions {
  /** Listen only while true. Default `true`. */
  enabled?: boolean;
  /**
   * Whether a hotkey with ⌘, Ctrl or Alt also fires while focus is in a text
   * field. Default `true`, as such keys cannot be typed. Single-key hotkeys
   * such as `/` or `?` never fire while typing.
   */
  inFields?: boolean;
}

/**
 * Calls `handler` when the hotkey is pressed anywhere in the document. Keys
 * pressed while composing text (IME) or already handled are ignored.
 */
export function useHotkey(
  hotkey: Hotkey | false | null | undefined,
  handler: (event: KeyboardEvent) => void,
  { enabled = true, inFields = true }: UseHotkeyOptions = {},
) {
  const handlerRef = useRef(handler);
  handlerRef.current = handler;
  const keys = hotkey ? list(hotkey).join('\n') : '';

  useEffect(() => {
    if (!enabled || !keys || typeof document === 'undefined') return;
    const alternatives = keys.split('\n');
    const onKey = (event: KeyboardEvent) => {
      if (event.defaultPrevented || event.isComposing || event.repeat) return;
      const match = alternatives.find((h) => matchesHotkey(event, h));
      if (!match) return;
      if (isTypingTarget(event.target) && (!isModified(match) || !inFields)) return;
      event.preventDefault();
      handlerRef.current(event);
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [keys, enabled, inFields]);
}
