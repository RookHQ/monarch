'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type LiHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, RetryGlyph } from '../../../internal/icons.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { Switch } from '../switch/index.js';

export interface SwitchSettingsLabels {
  /** Accessible name of the saving spinner. Default "Saving". */
  saving?: string;
  /** Shown and announced after a successful save. Default "Saved". */
  saved?: string;
  /** Shown and announced when a save fails, before the error's message. Default "Couldn’t save." */
  error?: string;
  /** The retry button. Its accessible name is this plus the row label. Default "Retry". */
  retry?: string;
}

const DEFAULT_LABELS: Required<SwitchSettingsLabels> = {
  saving: 'Saving',
  saved: 'Saved',
  error: 'Couldn’t save.',
  retry: 'Retry',
};

interface ListContextValue {
  labels: Required<SwitchSettingsLabels>;
  savedDuration: number;
  getErrorMessage: (error: unknown) => ReactNode;
}

function defaultMessage(error: unknown): ReactNode {
  return error instanceof Error && error.message ? error.message : null;
}

const ListContext = createContext<ListContextValue>({
  labels: DEFAULT_LABELS,
  savedDuration: 2000,
  getErrorMessage: defaultMessage,
});

export interface SwitchSettingsListProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Group heading, for example "Email notifications". Names the group. */
  title?: ReactNode;
  /** A sentence under the title. */
  description?: ReactNode;
  /** Heading element for `title`. Default `h3`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
  /** Status wording for every row (rows can override). */
  labels?: SwitchSettingsLabels;
  /** How long "Saved" stays visible, in ms. Default 2000. */
  savedDuration?: number;
  /** Turns a failed save into a message shown after `labels.error`. Default: the Error's message. */
  getErrorMessage?: (error: unknown) => ReactNode;
  /** SwitchSettingsItem rows. */
  children: ReactNode;
}

/**
 * A group of on/off settings that each save on their own, such as
 * notification preferences. Rows toggle optimistically, show their own
 * saving and error state with retry, and roll back if the save fails.
 */
export const SwitchSettingsList = forwardRef<HTMLDivElement, SwitchSettingsListProps>(
  function SwitchSettingsList(
    {
      title,
      description,
      headingLevel: H = 'h3',
      labels,
      savedDuration = 2000,
      getErrorMessage = defaultMessage,
      className,
      children,
      ...rest
    },
    ref,
  ) {
    const auto = useId();
    const titleId = `mn-switch-settings-${auto}-title`;
    const descriptionId = `mn-switch-settings-${auto}-description`;
    const value = useMemo<ListContextValue>(
      () => ({ labels: { ...DEFAULT_LABELS, ...labels }, savedDuration, getErrorMessage }),
      [labels, savedDuration, getErrorMessage],
    );
    const named = Boolean(title) || Boolean(rest['aria-label']) || Boolean(rest['aria-labelledby']);
    return (
      <ListContext.Provider value={value}>
        <div
          ref={ref}
          role={named ? 'group' : undefined}
          aria-labelledby={title ? titleId : undefined}
          aria-describedby={description ? descriptionId : undefined}
          {...rest}
          className={cx('mn-switch-settings-list', className)}
        >
          {title || description ? (
            <div className="mn-switch-settings-list__header">
              {title ? (
                <H id={titleId} className="mn-switch-settings-list__title">
                  {title}
                </H>
              ) : null}
              {description ? (
                <p id={descriptionId} className="mn-switch-settings-list__description">
                  {description}
                </p>
              ) : null}
            </div>
          ) : null}
          <ul className="mn-switch-settings-list__rows">{children}</ul>
        </div>
      </ListContext.Provider>
    );
  },
);

export type SwitchSettingsStatus = 'idle' | 'saving' | 'saved' | 'error';

export interface SwitchSettingsItemProps extends Omit<
  LiHTMLAttributes<HTMLLIElement>,
  'onChange' | 'defaultChecked'
> {
  /** The setting's name. Labels the switch. */
  label: ReactNode;
  /** What the setting does. Describes the switch. */
  description?: ReactNode;
  /** The saved state (controlled). */
  checked?: boolean;
  /** The initial saved state (uncontrolled). */
  defaultChecked?: boolean;
  /**
   * Saves the new state. The switch moves at once; return a promise to show
   * "Saving" and then "Saved", or reject it to roll the switch back and show
   * the error with a retry button.
   */
  onCheckedChange?: (checked: boolean) => void | Promise<void>;
  disabled?: boolean;
  /** Override the list's status wording for this row. */
  labels?: SwitchSettingsLabels;
  /** Called whenever the row's save status changes. */
  onStatusChange?: (status: SwitchSettingsStatus) => void;
}

/** One setting row: label and description on the left, the switch and its save status on the right. */
export const SwitchSettingsItem = forwardRef<HTMLLIElement, SwitchSettingsItemProps>(
  function SwitchSettingsItem(
    {
      label,
      description,
      checked: checkedProp,
      defaultChecked = false,
      onCheckedChange,
      disabled = false,
      labels: rowLabels,
      onStatusChange,
      className,
      ...rest
    },
    ref,
  ) {
    const list = useContext(ListContext);
    const labels = { ...list.labels, ...rowLabels };
    const auto = useId();
    const base = `mn-switch-settings-${auto}`;
    const switchId = `${base}-switch`;
    const labelId = `${base}-label`;
    const descriptionId = `${base}-description`;
    const statusId = `${base}-status`;
    const retryId = `${base}-retry`;

    const [inner, setInner] = useState(defaultChecked);
    const saved = checkedProp ?? inner;
    // The optimistic value while saving (and, controlled, until the parent's
    // `checked` catches up with a successful save).
    const [pending, setPending] = useState<boolean | null>(null);
    const [status, setStatusState] = useState<SwitchSettingsStatus>('idle');
    const [message, setMessage] = useState<ReactNode>(null);
    const [retryValue, setRetryValue] = useState<boolean | null>(null);
    const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
        clearTimeout(timer.current);
      };
    }, []);

    const statusRef = useRef(status);
    const setStatus = useCallback(
      (next: SwitchSettingsStatus) => {
        statusRef.current = next;
        setStatusState(next);
        onStatusChange?.(next);
      },
      [onStatusChange],
    );

    // A new `checked` from the parent is the truth, unless a save is in flight.
    useEffect(() => {
      if (statusRef.current !== 'saving') setPending(null);
    }, [checkedProp]);

    const shown = pending ?? saved;

    const save = async (next: boolean) => {
      if (statusRef.current === 'saving' || disabled) return;
      clearTimeout(timer.current);
      setRetryValue(null);
      setPending(next);
      setMessage(null);
      setStatus('saving');
      try {
        const result = onCheckedChange?.(next);
        const async = Boolean(result && typeof (result as Promise<void>).then === 'function');
        await result;
        if (!mounted.current) return;
        if (checkedProp === undefined) {
          setInner(next);
          setPending(null);
        } else if (checkedProp === next) {
          setPending(null);
        }
        if (async) {
          setStatus('saved');
          timer.current = setTimeout(() => {
            if (mounted.current) setStatus('idle');
          }, list.savedDuration);
        } else {
          setStatus('idle');
        }
      } catch (error) {
        if (!mounted.current) return;
        setPending(null);
        setRetryValue(next);
        setMessage(list.getErrorMessage(error));
        setStatus('error');
      }
    };

    const announcement =
      status === 'saved'
        ? labels.saved
        : status === 'error'
          ? [labels.error, typeof message === 'string' ? message : null].filter(Boolean).join(' ')
          : '';

    return (
      <li
        ref={ref}
        {...rest}
        className={cx('mn-switch-settings-list__row', className)}
        data-status={status}
        data-disabled={disabled || undefined}
      >
        <div className="mn-switch-settings-list__text">
          <label id={labelId} htmlFor={switchId} className="mn-switch-settings-list__label">
            {label}
          </label>
          {description ? (
            <p id={descriptionId} className="mn-switch-settings-list__item-description">
              {description}
            </p>
          ) : null}
          {status === 'error' ? (
            <p className="mn-switch-settings-list__error" aria-hidden>
              {labels.error}
              {message ? <> {message}</> : null}
            </p>
          ) : null}
        </div>
        <div className="mn-switch-settings-list__control">
          {status === 'saving' ? (
            <Spinner size="sm" label={labels.saving} className="mn-switch-settings-list__spinner" />
          ) : status === 'saved' ? (
            <span className="mn-switch-settings-list__saved" aria-hidden>
              <CheckGlyph />
              {labels.saved}
            </span>
          ) : status === 'error' && retryValue !== null ? (
            <button
              type="button"
              id={retryId}
              aria-labelledby={`${retryId} ${labelId}`}
              className="mn-switch-settings-list__retry"
              onClick={() => void save(retryValue)}
            >
              <RetryGlyph aria-hidden />
              {labels.retry}
            </button>
          ) : null}
          <Switch
            id={switchId}
            checked={shown}
            disabled={disabled}
            aria-describedby={cx(
              description ? descriptionId : undefined,
              status === 'error' ? statusId : undefined,
            )}
            aria-busy={status === 'saving' || undefined}
            onCheckedChange={(next) => void save(next)}
          />
        </div>
        <span id={statusId} className="mn-visually-hidden" role="status">
          {announcement}
        </span>
      </li>
    );
  },
);
