'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph } from '../../../internal/icons.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';
import { useDirection } from '../../../internal/direction.js';

/** True for `#rgb` or `#rrggbb` (the `#` is required). */
export function isHexColor(value: unknown): value is string {
  return typeof value === 'string' && /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i.test(value.trim());
}

/**
 * Reads `#rgb`, `#rrggbb`, `rgb`, `rrggbb` (with or without `#`, any case,
 * surrounding spaces) and returns lowercase `#rrggbb`, or `null` when it is
 * not a hex colour.
 */
export function normalizeHex(input: string): string | null {
  const v = input.trim().replace(/^#/, '');
  if (/^[0-9a-f]{3}$/i.test(v)) return `#${v.replace(/./g, (c) => c + c).toLowerCase()}`;
  if (/^[0-9a-f]{6}$/i.test(v)) return `#${v.toLowerCase()}`;
  return null;
}

/** A preset colour. */
export interface ColorSwatch {
  /** Hex colour, `#rgb` or `#rrggbb`. Invalid values are skipped. */
  value: string;
  /** Name read by screen readers and shown as a tooltip, e.g. "Blue". */
  label: string;
}

/** Ten neutral presets that read well on light and dark surfaces. */
export const DEFAULT_COLOR_SWATCHES: readonly ColorSwatch[] = [
  { value: '#64748b', label: 'Slate' },
  { value: '#dc2626', label: 'Red' },
  { value: '#ea580c', label: 'Orange' },
  { value: '#ca8a04', label: 'Yellow' },
  { value: '#16a34a', label: 'Green' },
  { value: '#0d9488', label: 'Teal' },
  { value: '#2563eb', label: 'Blue' },
  { value: '#4f46e5', label: 'Indigo' },
  { value: '#9333ea', label: 'Purple' },
  { value: '#db2777', label: 'Pink' },
];

/** Interface text, for translation. */
export interface ColorPickerMessages {
  /** Name of the swatch group. Default "Preset colours". */
  swatches: string;
  /** Accessible name of a swatch. Default "Blue, #2563eb". */
  swatchLabel: (label: string, hex: string) => string;
  /** Name of the hex text box. Default "Hex colour". */
  hex: string;
  /** Name of the native colour input. Default "Custom colour". */
  custom: string;
  /** Shown when the hex text is not a colour. */
  invalid: string;
}

export const COLOR_PICKER_MESSAGES: ColorPickerMessages = {
  swatches: 'Preset colours',
  swatchLabel: (label, hex) => `${label}, ${hex}`,
  hex: 'Hex colour',
  custom: 'Custom colour',
  invalid: 'Enter a hex colour like #2563eb.',
};

export interface ColorPickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** Controlled colour as lowercase `#rrggbb`, or `null` for none. */
  value?: string | null | undefined;
  /** Uncontrolled initial colour. */
  defaultValue?: string | null | undefined;
  /** Called with lowercase `#rrggbb` when a swatch, valid hex text or the native picker changes it. */
  onValueChange?: ((value: string) => void) | undefined;
  /** Preset colours. Default `DEFAULT_COLOR_SWATCHES`; `[]` shows only the custom row. */
  swatches?: readonly ColorSwatch[] | undefined;
  /** Show the hex text box and the native colour input. Default `true`. */
  allowCustom?: boolean | undefined;
  disabled?: boolean | undefined;
  /** Marks the value invalid (also inherited from a surrounding Field). */
  invalid?: boolean | undefined;
  /** Form field name; renders a hidden input carrying the value. */
  name?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ColorPickerMessages> | undefined;
}

/**
 * Picks a colour from named swatches, or any colour by hex text or the
 * platform colour picker. Swatches are a radio group (arrow keys move and
 * select) with names like "Blue, #2563eb" and a check mark on the selected
 * one, so the choice never depends on seeing the colour. Hex text is
 * validated and normalised to lowercase `#rrggbb`.
 */
export const ColorPicker = forwardRef<HTMLDivElement, ColorPickerProps>(function ColorPicker(
  {
    value: valueProp,
    defaultValue = null,
    onValueChange,
    swatches = DEFAULT_COLOR_SWATCHES,
    allowCustom = true,
    disabled: disabledProp,
    invalid,
    name,
    messages: messagesProp,
    className,
    id,
    'aria-label': ariaLabel,
    'aria-labelledby': ariaLabelledBy,
    'aria-describedby': ariaDescribedBy,
    ...rest
  },
  ref,
) {
  const readDir = useDirection();
  const messages = { ...COLOR_PICKER_MESSAGES, ...messagesProp };
  const autoId = useId();
  const messageId = `mn-color-picker-${autoId}-message`;
  const field = useFieldContext();
  const control = useFieldControl(
    { id, disabled: disabledProp, 'aria-describedby': ariaDescribedBy },
    { invalid },
  );
  const disabled = !!control.disabled;
  const labelledBy = ariaLabelledBy ?? (ariaLabel || !field ? undefined : field.labelId);

  const [internal, setInternal] = useState<string | null>(
    defaultValue === null ? null : normalizeHex(defaultValue),
  );
  const raw = valueProp !== undefined ? valueProp : internal;
  const value = raw === null ? null : normalizeHex(raw);
  const [draft, setDraft] = useState(value ?? '');
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    setDraft(value ?? '');
    setError(null);
  }, [value]);

  const presets = swatches
    .map((s) => ({ ...s, hex: normalizeHex(s.value) }))
    .filter((s): s is ColorSwatch & { hex: string } => s.hex !== null);
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);
  const selectedIndex = presets.findIndex((s) => s.hex === value);
  const tabIndexAt = selectedIndex === -1 ? 0 : selectedIndex;

  const set = (next: string) => {
    if (disabled) return;
    setError(null);
    setDraft(next);
    if (next === value) return;
    if (valueProp === undefined) setInternal(next);
    onValueChange?.(next);
  };

  const moveTo = (index: number) => {
    const count = presets.length;
    if (!count) return;
    const i = ((index % count) + count) % count;
    buttons.current[i]?.focus();
    const s = presets[i];
    if (s) set(s.hex);
  };

  const handleSwatchKey = (e: KeyboardEvent<HTMLDivElement>) => {
    if (disabled) return;
    const current = buttons.current.findIndex((b) => b === e.target);
    if (current === -1) return;
    const rtl = readDir(e.currentTarget) === 'rtl';
    switch (e.key) {
      case rtl ? 'ArrowLeft' : 'ArrowRight':
      case 'ArrowDown':
        e.preventDefault();
        moveTo(current + 1);
        break;
      case rtl ? 'ArrowRight' : 'ArrowLeft':
      case 'ArrowUp':
        e.preventDefault();
        moveTo(current - 1);
        break;
      case 'Home':
        e.preventDefault();
        moveTo(0);
        break;
      case 'End':
        e.preventDefault();
        moveTo(presets.length - 1);
        break;
      default:
    }
  };

  const commitDraft = () => {
    if (draft.trim() === '') {
      setDraft(value ?? '');
      setError(null);
      return;
    }
    const hex = normalizeHex(draft);
    if (hex === null) setError(messages.invalid);
    else set(hex);
  };

  const groupInvalid = !!control['aria-invalid'];
  const describedBy = control['aria-describedby'];

  return (
    <div
      ref={ref}
      role="group"
      {...rest}
      id={control.id}
      aria-label={ariaLabel}
      aria-labelledby={labelledBy}
      aria-describedby={describedBy}
      className={cx('mn-color-picker', className)}
      data-disabled={disabled || undefined}
      data-invalid={groupInvalid || error ? '' : undefined}
    >
      {presets.length > 0 ? (
        // Roving focus: the radios inside take focus, not the group.
        // eslint-disable-next-line jsx-a11y/interactive-supports-focus
        <div
          role="radiogroup"
          aria-label={messages.swatches}
          aria-disabled={disabled || undefined}
          className="mn-color-picker__swatches"
          onKeyDown={handleSwatchKey}
        >
          {presets.map((s, index) => {
            const checked = s.hex === value;
            return (
              <button
                key={s.hex}
                ref={(el) => {
                  buttons.current[index] = el;
                }}
                type="button"
                role="radio"
                aria-checked={checked}
                aria-label={messages.swatchLabel(s.label, s.hex)}
                title={s.label}
                tabIndex={index === tabIndexAt ? 0 : -1}
                disabled={disabled}
                className="mn-color-picker__swatch"
                data-state={checked ? 'checked' : 'unchecked'}
                style={{ '--_swatch': s.hex } as CSSProperties}
                onClick={() => set(s.hex)}
              >
                {checked ? <CheckGlyph className="mn-color-picker__check" /> : null}
              </button>
            );
          })}
        </div>
      ) : null}
      {allowCustom ? (
        <div className="mn-color-picker__custom">
          <input
            type="color"
            className="mn-color-picker__native"
            aria-label={messages.custom}
            value={value ?? '#000000'}
            disabled={disabled}
            onChange={(e) => {
              const hex = normalizeHex(e.target.value);
              if (hex) set(hex);
            }}
          />
          <input
            type="text"
            className="mn-color-picker__hex"
            aria-label={messages.hex}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? messageId : undefined}
            autoComplete="off"
            spellCheck={false}
            maxLength={7}
            placeholder="#000000"
            value={draft}
            disabled={disabled}
            onChange={(e) => {
              setDraft(e.target.value);
              if (error) setError(null);
            }}
            onBlur={commitDraft}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                commitDraft();
              } else if (e.key === 'Escape' && (error || draft !== (value ?? ''))) {
                e.preventDefault();
                setDraft(value ?? '');
                setError(null);
              }
            }}
          />
        </div>
      ) : null}
      {error ? (
        <p id={messageId} className="mn-color-picker__message" role="alert">
          {error}
        </p>
      ) : null}
      {name ? (
        <input type="hidden" name={name} value={value ?? ''} disabled={disabled || undefined} />
      ) : null}
    </div>
  );
});
