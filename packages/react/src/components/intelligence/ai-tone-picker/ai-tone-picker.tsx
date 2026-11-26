'use client';

import { forwardRef, useId, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { Field, FieldLabel, NativeSelect } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';

/** One writing tone. */
export interface AiTone {
  /** Stable id, the value. */
  id: string;
  label: string;
  /** How text in this tone reads. Shown on the card and read with the option. */
  description: string;
}

/** The value of the "Custom" option when `allowCustom` is set. */
export const AI_TONE_CUSTOM = 'custom';

/** The default tones: Professional, Technical, Casual, Executive. */
export const AI_DEFAULT_TONES: readonly AiTone[] = [
  { id: 'professional', label: 'Professional', description: 'Clear, polite and to the point.' },
  { id: 'technical', label: 'Technical', description: 'Precise terms, detail and structure.' },
  { id: 'casual', label: 'Casual', description: 'Friendly and conversational.' },
  { id: 'executive', label: 'Executive', description: 'Short, decision-first, no detail.' },
];

/** Every string AiTonePicker shows. */
export interface AiTonePickerMessages {
  /** Name of the choice (legend or select label). */
  label: string;
  /** Label of the custom option. */
  custom: string;
  /** Description of the custom option. */
  customDescription: string;
  /** Label of the custom tone field. */
  customField: string;
  customPlaceholder: string;
}

export const AI_TONE_PICKER_MESSAGES: AiTonePickerMessages = {
  label: 'Tone',
  custom: 'Custom',
  customDescription: 'Describe the tone in your own words.',
  customField: 'Describe the tone',
  customPlaceholder: 'e.g. Warm but brief, for a first-time customer',
};

export interface AiTonePickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** The tones to offer. Default `AI_DEFAULT_TONES`. */
  tones?: readonly AiTone[] | undefined;
  /** The selected tone id (`AI_TONE_CUSTOM` for the custom option). */
  value: string;
  /** Called with the new tone id. */
  onChange: (id: string) => void;
  /** Adds a "Custom" option with a text field for the person's own description. */
  allowCustom?: boolean | undefined;
  /** The custom tone text (controlled). */
  customText?: string | undefined;
  /** Initial custom tone text when uncontrolled. */
  defaultCustomText?: string | undefined;
  /** Called as the custom tone text changes. */
  onCustomTextChange?: ((text: string) => void) | undefined;
  /** `cards` (default): radio cards with descriptions. `compact`: a select, for toolbars. */
  variant?: 'cards' | 'compact' | undefined;
  /** Disables the choice. */
  disabled?: boolean | undefined;
  /** Override any string. */
  messages?: Partial<AiTonePickerMessages> | undefined;
}

/**
 * Chooses the tone the assistant writes in: Professional, Technical, Casual
 * and Executive by default, each with a one-line description, plus an
 * optional custom tone in the person's own words. Radio cards in a fieldset,
 * or a compact select for toolbars.
 */
export const AiTonePicker = forwardRef<HTMLDivElement, AiTonePickerProps>(function AiTonePicker(
  {
    tones = AI_DEFAULT_TONES,
    value,
    onChange,
    allowCustom = false,
    customText: customTextProp,
    defaultCustomText = '',
    onCustomTextChange,
    variant = 'cards',
    disabled = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_TONE_PICKER_MESSAGES, ...messagesProp };
  const uid = useId();
  const name = `${uid}-tone`;
  const [innerCustom, setInnerCustom] = useState(defaultCustomText);
  const customText = customTextProp ?? innerCustom;
  const setCustomText = (text: string) => {
    if (customTextProp === undefined) setInnerCustom(text);
    onCustomTextChange?.(text);
  };

  const options: AiTone[] = allowCustom
    ? [...tones, { id: AI_TONE_CUSTOM, label: m.custom, description: m.customDescription }]
    : [...tones];
  const customOn = allowCustom && value === AI_TONE_CUSTOM;

  const customField = customOn ? (
    <Field className="mn-ai-tone-picker__custom" disabled={disabled}>
      <FieldLabel>{m.customField}</FieldLabel>
      <Input
        size={variant === 'compact' ? 'sm' : 'md'}
        value={customText}
        placeholder={m.customPlaceholder}
        onChange={(e) => setCustomText(e.target.value)}
      />
    </Field>
  ) : null;

  if (variant === 'compact') {
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-tone-picker', className)}
        data-variant="compact"
        data-tone={value}
      >
        <NativeSelect
          size="sm"
          label={m.label}
          value={value}
          disabled={disabled}
          options={options.map((t) => ({ value: t.id, label: t.label }))}
          onChange={(e) => onChange(e.target.value)}
        />
        {customField}
      </div>
    );
  }

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-tone-picker', className)}
      data-variant="cards"
      data-tone={value}
    >
      <fieldset className="mn-ai-tone-picker__fieldset" disabled={disabled}>
        <legend className="mn-ai-tone-picker__legend">{m.label}</legend>
        <div className="mn-ai-tone-picker__options">
          {options.map((tone, index) => {
            const inputId = `${uid}-${index}`;
            const descId = `${inputId}-desc`;
            const checked = tone.id === value;
            return (
              <label
                key={tone.id}
                htmlFor={inputId}
                className="mn-ai-tone-picker__option"
                data-state={checked ? 'on' : 'off'}
                data-custom={tone.id === AI_TONE_CUSTOM || undefined}
              >
                <input
                  id={inputId}
                  type="radio"
                  name={name}
                  value={tone.id}
                  checked={checked}
                  aria-describedby={descId}
                  className="mn-ai-tone-picker__radio"
                  onChange={() => onChange(tone.id)}
                />
                <span className="mn-ai-tone-picker__text">
                  <span className="mn-ai-tone-picker__label">{tone.label}</span>
                  <span id={descId} className="mn-ai-tone-picker__description">
                    {tone.description}
                  </span>
                </span>
              </label>
            );
          })}
        </div>
      </fieldset>
      {customField}
    </div>
  );
});
