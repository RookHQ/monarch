import { forwardRef, type ComponentType, type HTMLAttributes, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { EyeGlyph, LockGlyph, PencilGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';

/** How far an AI agent may go in one app. */
export type AiAccessLevel = 'none' | 'read' | 'write';

/** One app and what the AI may do in it. */
export interface AiPermissionScopeItem {
  /** The app's display name, e.g. "Calendar". Also the row's key, so keep it unique. */
  app: string;
  /** Which part of the app the access covers, e.g. "Shared projects only". */
  resources?: string | undefined;
  access: AiAccessLevel;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiPermissionScopeMessages {
  /** Accessible name of the list. */
  label: string;
  /** Full description of each level, shown in read-only mode. */
  access: Record<AiAccessLevel, string>;
  /** Short option labels for the editable control. */
  options: Record<AiAccessLevel, string>;
  /** Accessible name of each row's control, e.g. "Access to Calendar". */
  controlLabel: (app: string) => string;
  /** Text of the `manageHref` link. */
  manage: string;
  /** Shown when there are no scopes. */
  empty: string;
}

export const AI_PERMISSION_SCOPE_MESSAGES: AiPermissionScopeMessages = {
  label: 'What the AI can access',
  access: { none: 'No access', read: 'Can read', write: 'Can read and edit' },
  options: { none: 'None', read: 'Read', write: 'Write' },
  controlLabel: (app) => `Access to ${app}`,
  manage: 'Manage permissions',
  empty: 'The AI has no access to any app.',
};

const LEVELS: readonly AiAccessLevel[] = ['none', 'read', 'write'];

const ICONS: Record<AiAccessLevel, ComponentType<SVGProps<SVGSVGElement>>> = {
  none: LockGlyph,
  read: EyeGlyph,
  write: PencilGlyph,
};

export interface AiPermissionScopeProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onChange'
> {
  /** One row per app, in the order shown. */
  scopes: readonly AiPermissionScopeItem[];
  /**
   * Show a None / Read / Write control on each row instead of read-only text.
   * The component is controlled: pass the updated `scopes` back from `onChange`.
   * Default false.
   */
  editable?: boolean | undefined;
  /** Called with the full updated list and the row that changed. */
  onChange?:
    ((scopes: AiPermissionScopeItem[], changed: AiPermissionScopeItem) => void) | undefined;
  /** Link to the full permission settings. Unsafe URLs (`javascript:` and similar) are dropped. */
  manageHref?: string | undefined;
  /** Replaces any of the English strings; `access` and `options` may be partial. */
  messages?:
    | (Partial<Omit<AiPermissionScopeMessages, 'access' | 'options'>> & {
        access?: Partial<Record<AiAccessLevel, string>> | undefined;
        options?: Partial<Record<AiAccessLevel, string>> | undefined;
      })
    | undefined;
}

/**
 * What an AI agent may see and change, app by app: each row names the app and
 * the resources it covers and shows No access, Can read or Can read and edit,
 * with a None / Read / Write control on each row when editable.
 */
export const AiPermissionScope = forwardRef<HTMLDivElement, AiPermissionScopeProps>(
  function AiPermissionScope(
    { scopes, editable = false, onChange, manageHref, messages, className, ...rest },
    ref,
  ) {
    const { access, options, ...overrides } = messages ?? {};
    const m: AiPermissionScopeMessages = {
      ...AI_PERMISSION_SCOPE_MESSAGES,
      ...overrides,
      access: { ...AI_PERMISSION_SCOPE_MESSAGES.access, ...access },
      options: { ...AI_PERMISSION_SCOPE_MESSAGES.options, ...options },
    };
    const href = manageHref && isSafeUrl(manageHref) ? manageHref : undefined;

    const change = (index: number, level: AiAccessLevel) => {
      const current = scopes[index];
      if (!current || current.access === level) return;
      const changed = { ...current, access: level };
      onChange?.(
        scopes.map((s, i) => (i === index ? changed : s)),
        changed,
      );
    };

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-permission-scope', className)}
        data-editable={editable || undefined}
      >
        {scopes.length === 0 ? (
          <p className="mn-ai-permission-scope__empty">{m.empty}</p>
        ) : (
          <ul className="mn-ai-permission-scope__list" aria-label={m.label}>
            {scopes.map((scope, index) => {
              const Icon = ICONS[scope.access];
              return (
                <li
                  key={scope.app}
                  className="mn-ai-permission-scope__row"
                  data-access={scope.access}
                >
                  <span className="mn-ai-permission-scope__app">
                    <span className="mn-ai-permission-scope__name">{scope.app}</span>
                    {scope.resources ? (
                      <span className="mn-ai-permission-scope__resources">{scope.resources}</span>
                    ) : null}
                  </span>
                  {editable ? (
                    <SegmentedControl<AiAccessLevel>
                      className="mn-ai-permission-scope__control"
                      size="sm"
                      aria-label={m.controlLabel(scope.app)}
                      value={scope.access}
                      onValueChange={(level) => change(index, level)}
                      options={LEVELS.map((level) => ({ value: level, label: m.options[level] }))}
                    />
                  ) : (
                    <span className="mn-ai-permission-scope__access">
                      <Icon className="mn-ai-permission-scope__icon" />
                      <span>
                        <span className="mn-visually-hidden">: </span>
                        {m.access[scope.access]}
                      </span>
                    </span>
                  )}
                </li>
              );
            })}
          </ul>
        )}
        {href ? (
          <a className="mn-ai-permission-scope__manage" href={href}>
            {m.manage}
          </a>
        ) : null}
      </div>
    );
  },
);

/** True when `access` is at least `needed` (`write` includes `read`). */
export function aiAccessAtLeast(access: AiAccessLevel, needed: AiAccessLevel): boolean {
  return LEVELS.indexOf(access) >= LEVELS.indexOf(needed);
}
