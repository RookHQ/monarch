'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { RadioCardGroup } from '../../forms/radio-card-group/index.js';
import { Select, SelectContent, SelectItem, SelectTrigger } from '../../forms/select/index.js';

/** One role a person can be given. */
export interface RoleOption {
  /** Stable id, e.g. `admin`; what `value` holds. */
  value: string;
  /** Name of the role, e.g. "Admin". */
  label: string;
  /** What someone with this role can do, in plain words. */
  description?: ReactNode;
  /** A short list of things the role can do, shown under the description in the cards layout. */
  capabilities?: ReactNode[] | undefined;
  /** The role cannot be chosen here. */
  disabled?: boolean | undefined;
  /** Why it cannot be chosen, e.g. "Only owners can make someone an owner". */
  disabledReason?: ReactNode;
}

/** Visible strings, for translation. */
export interface RolePickerMessages {
  /** Group label. Default "Role". */
  label: string;
  /** Compact trigger text when nothing is selected. Default "Choose a role". */
  placeholder: string;
}

export const defaultRolePickerMessages: RolePickerMessages = {
  label: 'Role',
  placeholder: 'Choose a role',
};

export interface RolePickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue' | 'children'
> {
  /** The roles, in order from least to most access. */
  roles: RoleOption[];
  /** Selected role (controlled). */
  value?: string | undefined;
  /** Initial role when uncontrolled. */
  defaultValue?: string | undefined;
  /** Called with the chosen role's value. */
  onValueChange?: ((value: string) => void) | undefined;
  /**
   * `cards`: every role as a radio card with its description and
   * capabilities, for invite and settings forms. `compact`: a select whose
   * options show the descriptions, for table rows and tight dialogs.
   * Default `cards`.
   */
  layout?: 'cards' | 'compact' | undefined;
  /** Form field name; the chosen value is posted with it. */
  name?: string | undefined;
  /** Disables every role. */
  disabled?: boolean | undefined;
  /** A role must be chosen. */
  required?: boolean | undefined;
  /** Marks the choice invalid. */
  invalid?: boolean | undefined;
  /** Hide the visible label; it still names the group. Default `false`. */
  hideLabel?: boolean | undefined;
  /** Help text under the label. */
  description?: ReactNode;
  /** Size of the compact trigger. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<RolePickerMessages> | undefined;
}

/**
 * Choose a role for a person, with a plain description of what each role can
 * do. Shows every role as a radio card, or as a compact select for table rows
 * and dialogs. Roles that cannot be assigned stay visible with the reason.
 */
export const RolePicker = forwardRef<HTMLDivElement, RolePickerProps>(function RolePicker(
  {
    roles,
    value: valueProp,
    defaultValue,
    onValueChange,
    layout = 'cards',
    name,
    disabled = false,
    required = false,
    invalid = false,
    hideLabel = false,
    description,
    size = 'md',
    messages,
    className,
    'aria-label': ariaLabel,
    ...rest
  },
  ref,
) {
  const m = { ...defaultRolePickerMessages, ...messages };
  const [valueState, setValueState] = useState(defaultValue);
  const value = valueProp ?? valueState;
  const id = useId();
  const labelId = `${id}-label`;
  const descriptionId = `${id}-description`;
  const select = (next: string) => {
    if (valueProp === undefined) setValueState(next);
    onValueChange?.(next);
  };
  const label = ariaLabel ? undefined : (
    <span
      id={labelId}
      className={cx('mn-role-picker__label', hideLabel && 'mn-role-picker__label--hidden')}
    >
      {m.label}
      {required ? (
        <span className="mn-role-picker__required" aria-hidden="true">
          *
        </span>
      ) : null}
    </span>
  );
  const help = description ? (
    <p id={descriptionId} className="mn-role-picker__description">
      {description}
    </p>
  ) : null;

  if (layout === 'compact') {
    const selected = roles.find((role) => role.value === value);
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-role-picker', className)}
        data-layout="compact"
        data-invalid={invalid || undefined}
      >
        {label}
        {help}
        <Select
          {...(value !== undefined ? { value } : {})}
          onValueChange={select}
          disabled={disabled}
          {...(name ? { name } : {})}
          required={required}
        >
          <SelectTrigger
            size={size}
            invalid={invalid}
            className="mn-role-picker__trigger"
            aria-labelledby={ariaLabel ? undefined : labelId}
            aria-label={ariaLabel}
            aria-describedby={description ? descriptionId : undefined}
          >
            <span className="mn-role-picker__value" data-placeholder={selected ? undefined : ''}>
              {selected?.label ?? m.placeholder}
            </span>
          </SelectTrigger>
          <SelectContent className="mn-role-picker__menu">
            {roles.map((role) => (
              <SelectItem
                key={role.value}
                value={role.value}
                textValue={role.label}
                disabled={role.disabled}
                className="mn-role-picker__option"
              >
                <span className="mn-role-picker__option-label">{role.label}</span>
                {role.description || (role.disabled && role.disabledReason) ? (
                  <span className="mn-role-picker__option-description">
                    {role.disabled && role.disabledReason ? role.disabledReason : role.description}
                  </span>
                ) : null}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    );
  }

  return (
    <RadioCardGroup
      ref={ref}
      {...rest}
      options={roles.map((role) => ({
        value: role.value,
        title: role.label,
        disabled: role.disabled === true,
        description: (
          <span className="mn-role-picker__card-text">
            {role.description ? (
              <span className="mn-role-picker__card-description">{role.description}</span>
            ) : null}
            {role.capabilities && role.capabilities.length > 0 ? (
              <span className="mn-role-picker__capabilities">
                {role.capabilities.map((capability, index) => (
                  <span key={index} className="mn-role-picker__capability">
                    {capability}
                  </span>
                ))}
              </span>
            ) : null}
            {role.disabled && role.disabledReason ? (
              <span className="mn-role-picker__reason">{role.disabledReason}</span>
            ) : null}
          </span>
        ),
      }))}
      {...(value !== undefined ? { value } : {})}
      onValueChange={select}
      name={name ?? `${id}-role`}
      disabled={disabled}
      required={required}
      invalid={invalid}
      label={ariaLabel || hideLabel ? undefined : m.label}
      aria-label={ariaLabel ?? (hideLabel ? m.label : undefined)}
      description={description}
      className={cx('mn-role-picker', className)}
      data-layout="cards"
    />
  );
});
