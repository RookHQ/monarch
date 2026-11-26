'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';
import { Avatar } from '../avatar/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';

/** One app that can be granted. */
export interface AppAccessOption {
  /** Stable id (for example the app's slug); what `value` holds. */
  id: string;
  /** The app's name. */
  name: string;
  /** One line about what the app is for. */
  description?: ReactNode;
  /** Logo image URL. Unsafe URLs, or a logo that fails to load, show the initial instead. */
  logoSrc?: string | null | undefined;
  /** A short tag after the name, e.g. a plan or "Beta". */
  badge?: ReactNode;
  /** The app cannot be changed here (for example it is included for everyone). */
  disabled?: boolean | undefined;
}

/** Visible strings, for translation. */
export interface AppAccessPickerMessages {
  /** Group label (legend). Default "App access". */
  label: string;
  /** Toolbar button when not everything is selected. Default "Select all". */
  selectAll: string;
  /** Toolbar button when everything is selected. Default "Clear all". */
  clearAll: string;
  /** Selection count, announced politely. Default "N of M selected". */
  count: (selected: number, total: number) => string;
  /** Announced while loading. Default "Loading apps". */
  loading: string;
  /** Shown when there are no apps. Default "No apps are available yet.". */
  empty: string;
}

export const defaultAppAccessPickerMessages: AppAccessPickerMessages = {
  label: 'App access',
  selectAll: 'Select all',
  clearAll: 'Clear all',
  count: (s, t) => `${s} of ${t} selected`,
  loading: 'Loading apps',
  empty: 'No apps are available yet.',
};

export interface AppAccessPickerProps extends Omit<
  HTMLAttributes<HTMLFieldSetElement>,
  'onChange' | 'defaultValue'
> {
  /** The apps that can be granted. */
  apps: AppAccessOption[];
  /** Ids of the selected apps (controlled). */
  value: string[];
  /** Called with the new list of selected ids. */
  onValueChange: (value: string[]) => void;
  /** Disables every checkbox, e.g. while saving. */
  disabled?: boolean | undefined;
  /** Shows placeholder rows instead of the apps. */
  loading?: boolean | undefined;
  /** Hide the legend visually; it still names the group. Default `false`. */
  hideLabel?: boolean | undefined;
  /** Shows the "Select all / Clear all" button and the count. Default `true`. */
  showBulkActions?: boolean | undefined;
  /** Help text under the list, e.g. when the grants take effect. */
  description?: ReactNode;
  /** Visible strings, for translation. */
  messages?: Partial<AppAccessPickerMessages> | undefined;
}

function logoOf(src: string | null | undefined) {
  return src && isSafeImageSrc(src) ? src : null;
}

/**
 * A controlled checklist of apps to grant, for inviting someone to a
 * workspace or editing a member's access: logo, name, description and an
 * optional tag per app, with "Select all / Clear all" and a selection count.
 */
export const AppAccessPicker = forwardRef<HTMLFieldSetElement, AppAccessPickerProps>(
  function AppAccessPicker(
    {
      apps,
      value,
      onValueChange,
      disabled = false,
      loading = false,
      hideLabel = false,
      showBulkActions = true,
      description,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultAppAccessPickerMessages, ...messages };
    const auto = useId();
    const descriptionId = description ? `${auto}-description` : undefined;
    const selected = new Set(value);
    const changeable = apps.filter((a) => !a.disabled);
    const allSelected = changeable.length > 0 && changeable.every((a) => selected.has(a.id));
    const count = apps.filter((a) => selected.has(a.id)).length;

    const toggle = (id: string, on: boolean) => {
      const next = on ? [...value.filter((v) => v !== id), id] : value.filter((v) => v !== id);
      onValueChange(next);
    };

    const toggleAll = () => {
      const fixed = value.filter((v) => !changeable.some((a) => a.id === v));
      onValueChange(allSelected ? fixed : [...fixed, ...changeable.map((a) => a.id)]);
    };

    return (
      <fieldset
        ref={ref}
        {...rest}
        className={cx('mn-app-access-picker', className)}
        disabled={disabled}
        aria-busy={loading || undefined}
        aria-describedby={cx(rest['aria-describedby'], descriptionId) || undefined}
        data-loading={loading || undefined}
        data-disabled={disabled || undefined}
      >
        <legend className="mn-app-access-picker__legend" data-hidden={hideLabel || undefined}>
          {m.label}
        </legend>
        {showBulkActions && !loading && apps.length > 0 ? (
          <div className="mn-app-access-picker__toolbar">
            <span className="mn-app-access-picker__count" aria-live="polite">
              {m.count(count, apps.length)}
            </span>
            <Button
              size="xs"
              variant="ghost"
              className="mn-app-access-picker__bulk"
              disabled={disabled || changeable.length === 0}
              onClick={toggleAll}
            >
              {allSelected ? m.clearAll : m.selectAll}
            </Button>
          </div>
        ) : null}
        {loading ? (
          <div className="mn-app-access-picker__loading">
            <span className="mn-app-access-picker__sr" role="status">
              {m.loading}
            </span>
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="mn-app-access-picker__skeleton" />
            ))}
          </div>
        ) : apps.length === 0 ? (
          <p className="mn-app-access-picker__empty">{m.empty}</p>
        ) : (
          <ul className="mn-app-access-picker__list">
            {apps.map((app) => {
              const id = `${auto}-${app.id}`;
              const checked = selected.has(app.id);
              const descId = app.description ? `${id}-description` : undefined;
              return (
                <li
                  key={app.id}
                  className="mn-app-access-picker__item"
                  data-selected={checked || undefined}
                  data-disabled={app.disabled || undefined}
                >
                  <Checkbox
                    id={id}
                    className="mn-app-access-picker__checkbox"
                    checked={checked}
                    disabled={disabled || app.disabled}
                    aria-describedby={descId}
                    onCheckedChange={(next) => toggle(app.id, next === true)}
                  />
                  <label htmlFor={id} className="mn-app-access-picker__label">
                    <Avatar
                      className="mn-app-access-picker__logo"
                      name={app.name}
                      src={logoOf(app.logoSrc)}
                      size={28}
                      shape="square"
                      seed={app.id}
                      palette="tokens"
                      decorative
                    />
                    <span className="mn-app-access-picker__text">
                      <span className="mn-app-access-picker__name-row">
                        <span className="mn-app-access-picker__name">{app.name}</span>
                        {app.badge ? (
                          <span className="mn-app-access-picker__badge">{app.badge}</span>
                        ) : null}
                      </span>
                      {app.description ? (
                        <span id={descId} className="mn-app-access-picker__description">
                          {app.description}
                        </span>
                      ) : null}
                    </span>
                  </label>
                </li>
              );
            })}
          </ul>
        )}
        {description ? (
          <p id={descriptionId} className="mn-app-access-picker__help">
            {description}
          </p>
        ) : null}
      </fieldset>
    );
  },
);

/** Visible strings of AppAccessSummary, for translation. */
export interface AppAccessSummaryMessages {
  /** Shown when nothing is granted. Default "No app access". */
  none: string;
  /** Spoken after the shown names. Default ", and N more: …". */
  more: (count: number, names: string) => string;
}

export interface AppAccessSummaryProps extends HTMLAttributes<HTMLSpanElement> {
  /** The apps, to look up names. Ids without a match show the id. */
  apps: Pick<AppAccessOption, 'id' | 'name'>[];
  /** Ids of the granted apps. */
  value: string[];
  /** How many names to show before "+N". Default 2. */
  max?: number | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<AppAccessSummaryMessages> | undefined;
}

/**
 * A compact, read-only line of the apps someone can open, for a members
 * table: the first names as tags, then "+N"; "No app access" when empty.
 */
export const AppAccessSummary = forwardRef<HTMLSpanElement, AppAccessSummaryProps>(
  function AppAccessSummary({ apps, value, max = 2, messages, className, ...rest }, ref) {
    const m: AppAccessSummaryMessages = {
      none: 'No app access',
      more: (n, names) => `, and ${n} more: ${names}`,
      ...messages,
    };
    if (value.length === 0) {
      return (
        <span ref={ref} {...rest} className={cx('mn-app-access-summary', className)} data-empty>
          {m.none}
        </span>
      );
    }
    const names = value
      .map((id) => apps.find((a) => a.id === id)?.name ?? id)
      .sort((a, b) => a.localeCompare(b));
    const shown = names.slice(0, Math.max(1, max));
    const hidden = names.slice(shown.length);
    return (
      <span ref={ref} {...rest} className={cx('mn-app-access-summary', className)}>
        {shown.map((name, index) => (
          <span key={name} className="mn-app-access-summary__tag">
            {index > 0 ? <span className="mn-app-access-picker__sr">, </span> : null}
            {name}
          </span>
        ))}
        {hidden.length > 0 ? (
          <>
            <span className="mn-app-access-summary__more" aria-hidden="true">
              +{hidden.length}
            </span>
            <span className="mn-app-access-picker__sr">
              {m.more(hidden.length, hidden.join(', '))}
            </span>
          </>
        ) : null}
      </span>
    );
  },
);
