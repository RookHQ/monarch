'use client';

import { useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { LockGlyph } from '../../../internal/icons.js';
import { Button } from '../../../components/actions/button/index.js';
import { EmptyState } from '../../../components/feedback/empty-state/index.js';
import { LoadingState } from '../../../components/feedback/loading-state/index.js';
import { Field, FieldDescription, FieldLabel } from '../../../components/forms/form-field/index.js';

export interface SettingsSectionProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Section title (h2). */
  title: ReactNode;
  description?: ReactNode;
  /** Footer with Save/Cancel actions. */
  actions?: ReactNode;
  /** `danger` for destructive zones (delete workspace, transfer ownership). */
  tone?: 'default' | 'danger';
  /** Heading level if the section is nested under another h2. */
  headingLevel?: 'h2' | 'h3';
  /**
   * The draft differs from the saved settings. Shows `dirtyMessage` in the
   * footer (a polite status) and enables Save and Discard. Exposed as `data-dirty`.
   */
  dirty?: boolean;
  /**
   * Renders the standard footer: a status line, Discard and Save. The product
   * owns the draft; Save calls this, Discard calls `onCancel`. Save and
   * Discard are disabled while the section is not `dirty`.
   */
  onSave?: () => void;
  /** Discards the draft. Shown when `onSave` is given. */
  onCancel?: () => void;
  /** Save is in flight: Save shows a spinner and Discard is disabled. */
  saving?: boolean;
  /** Default "Save changes". */
  saveLabel?: string;
  /** Default "Discard". */
  cancelLabel?: string;
  /** Status text while `dirty`. Default "You have unsaved changes." */
  dirtyMessage?: ReactNode;
  /** Keep the footer stuck to the bottom of the scroll area while `dirty`. */
  stickyFooter?: boolean;
  /**
   * `loading` shows a skeleton instead of the rows (aria-busy); `no-access`
   * replaces the rows and footer with a locked notice. Default `ready`.
   * Exposed as `data-state`.
   */
  state?: 'ready' | 'loading' | 'no-access';
  /** Announced while loading. Default "Loading settings". */
  loadingLabel?: string;
  /** Title of the `no-access` notice. Default "You don’t have access to these settings". */
  noAccessTitle?: ReactNode;
  /** Explanation under the `no-access` title, such as who can change them. */
  noAccessDescription?: ReactNode;
  children?: ReactNode;
}

/** Titled group of settings in a card, as in Workspace App Settings. */
export function SettingsSection({
  title,
  description,
  actions,
  tone = 'default',
  headingLevel: H = 'h2',
  dirty = false,
  onSave,
  onCancel,
  saving = false,
  saveLabel = 'Save changes',
  cancelLabel = 'Discard',
  dirtyMessage = 'You have unsaved changes.',
  stickyFooter = false,
  state = 'ready',
  loadingLabel = 'Loading settings',
  noAccessTitle = 'You don’t have access to these settings',
  noAccessDescription,
  className,
  children,
  ...rest
}: SettingsSectionProps) {
  const id = useId();
  const ready = state === 'ready';
  const showFooter = ready && (Boolean(actions) || Boolean(onSave));
  return (
    <section
      {...rest}
      aria-labelledby={`${id}-title`}
      aria-busy={state === 'loading' || saving || undefined}
      data-tone={tone}
      data-dirty={dirty || undefined}
      data-state={state === 'ready' ? undefined : state}
      data-sticky-footer={stickyFooter || undefined}
      className={cx('mn-settings-section', className)}
    >
      <div className="mn-settings-section__header">
        <H id={`${id}-title`} className="mn-settings-section__title">
          {title}
        </H>
        {description ? <p className="mn-settings-section__description">{description}</p> : null}
      </div>
      <div className="mn-settings-section__card">
        <div className="mn-settings-section__body">
          {state === 'loading' ? (
            <LoadingState
              label={loadingLabel}
              variant="rows"
              rows={3}
              className="mn-settings-section__loading"
            />
          ) : state === 'no-access' ? (
            <EmptyState
              size="compact"
              layout="line"
              headingLevel="p"
              icon={<LockGlyph />}
              title={noAccessTitle}
              description={noAccessDescription}
              className="mn-settings-section__no-access"
            />
          ) : (
            children
          )}
        </div>
        {showFooter ? (
          <div className="mn-settings-section__footer">
            {onSave ? (
              <>
                <p className="mn-settings-section__status" role="status" aria-live="polite">
                  {dirty ? dirtyMessage : null}
                </p>
                {actions}
                <Button disabled={!dirty || saving} onClick={onCancel}>
                  {cancelLabel}
                </Button>
                <Button variant="primary" disabled={!dirty} loading={saving} onClick={onSave}>
                  {saveLabel}
                </Button>
              </>
            ) : (
              actions
            )}
          </div>
        ) : null}
      </div>
    </section>
  );
}

export interface SettingsRowProps {
  /** Label of the setting; becomes the control's label. */
  label: ReactNode;
  description?: ReactNode;
  /** The control: Input, Select, Switch, Combobox. It receives the Field ids. */
  children: ReactNode;
  /** Pass `id` to match an existing control id. */
  id?: string;
  required?: boolean;
  disabled?: boolean;
  invalid?: boolean;
  /** Error message rendered under the control. */
  error?: ReactNode;
  className?: string;
}

/** One setting: label and description on the left, control on the right. */
export function SettingsRow({
  label,
  description,
  children,
  id,
  required,
  disabled,
  invalid,
  error,
  className,
}: SettingsRowProps) {
  return (
    <Field
      {...(id ? { id } : {})}
      required={required ?? false}
      disabled={disabled ?? false}
      invalid={invalid ?? Boolean(error)}
      className={cx('mn-settings-row', className)}
    >
      <div className="mn-settings-row__text">
        <FieldLabel>{label}</FieldLabel>
        {description ? <FieldDescription>{description}</FieldDescription> : null}
      </div>
      <div className="mn-settings-row__control">
        {children}
        {error ? (
          <p className="mn-field__error" aria-live="polite">
            {error}
          </p>
        ) : null}
      </div>
    </Field>
  );
}

export interface SettingsActionRowProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** What the action does ("Delete workspace"). */
  title: ReactNode;
  /** The consequence, in one or two sentences. */
  description?: ReactNode;
  /** The button that starts the action, usually opening a ConfirmDialog. */
  action: ReactNode;
}

/**
 * One action in a settings card: title and consequence on the left, a button
 * on the right. Used for danger-zone items (transfer, archive, delete) where
 * the row is not a form control.
 */
export function SettingsActionRow({
  title,
  description,
  action,
  className,
  ...rest
}: SettingsActionRowProps) {
  return (
    <div {...rest} className={cx('mn-settings-row', 'mn-settings-action-row', className)}>
      <div className="mn-settings-row__text">
        <p className="mn-settings-action-row__title">{title}</p>
        {description ? <p className="mn-settings-action-row__description">{description}</p> : null}
      </div>
      <div className="mn-settings-action-row__action">{action}</div>
    </div>
  );
}
