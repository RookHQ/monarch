'use client';

import {
  forwardRef,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, DashGlyph, LockGlyph } from '../../../internal/icons.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { useDirection } from '../../../internal/direction.js';

/** A role: one column of the matrix. */
export interface PermissionMatrixRole {
  id: string;
  /** Column heading, e.g. "Admin". */
  label: string;
  /** A short line under the heading. */
  description?: ReactNode;
}

/** A permission: one row of the matrix. */
export interface PermissionMatrixPermission {
  id: string;
  /** Row heading, e.g. "Invite members". */
  label: string;
  /** What the permission allows, in plain words. */
  description?: ReactNode;
  /** Rows with the same group are listed together under a group heading, in first-seen order. */
  group?: string | undefined;
}

/** Granted permission ids per role id. */
export type PermissionMatrixValue = Record<string, readonly string[]>;

/** One cell that changed, passed to `onValueChange`. */
export interface PermissionMatrixChange {
  roleId: string;
  permissionId: string;
  granted: boolean;
}

/** Visible strings, for translation. */
export interface PermissionMatrixMessages {
  /** Heading of the first column. Default "Permission". */
  permissionHeader: string;
  /** Accessible name of a cell. Default `${permission} for ${role}`. */
  cellLabel: (permission: string, role: string) => string;
  /** Read-only text for a granted cell. Default "Allowed". */
  allowed: string;
  /** Read-only text for a cell that is not granted. Default "Not allowed". */
  notAllowed: string;
  /** Reason for a locked cell when `isLocked` returns `true`. Default "This can’t be changed". */
  locked: string;
}

export const defaultPermissionMatrixMessages: PermissionMatrixMessages = {
  permissionHeader: 'Permission',
  cellLabel: (permission, role) => `${permission} for ${role}`,
  allowed: 'Allowed',
  notAllowed: 'Not allowed',
  locked: 'This can’t be changed',
};

export interface PermissionMatrixProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** The roles, as columns. */
  roles: PermissionMatrixRole[];
  /** The permissions, as rows. */
  permissions: PermissionMatrixPermission[];
  /** Granted permissions per role (controlled). */
  value?: PermissionMatrixValue | undefined;
  /** Initial grants when uncontrolled. */
  defaultValue?: PermissionMatrixValue | undefined;
  /** Called with the whole new value and the cell that changed. */
  onValueChange?:
    ((value: Record<string, string[]>, change: PermissionMatrixChange) => void) | undefined;
  /**
   * Cells that cannot be changed, such as the owner role having everything.
   * Return a reason (shown and announced), `true` for the default reason, or
   * nothing for an editable cell.
   */
  isLocked?:
    ((roleId: string, permissionId: string) => ReactNode | boolean | undefined) | undefined;
  /** Show the grants without checkboxes. */
  readOnly?: boolean | undefined;
  /** Disables every checkbox, e.g. while saving. */
  disabled?: boolean | undefined;
  /** Table caption; also the accessible name. Use `aria-label` instead to hide it. */
  caption?: ReactNode;
  /** Visible strings, for translation. */
  messages?: Partial<PermissionMatrixMessages> | undefined;
}

interface Group {
  label: string | undefined;
  rows: PermissionMatrixPermission[];
}

function groupPermissions(permissions: PermissionMatrixPermission[]): Group[] {
  const groups: Group[] = [];
  const byLabel = new Map<string | undefined, Group>();
  for (const permission of permissions) {
    let group = byLabel.get(permission.group);
    if (!group) {
      group = { label: permission.group, rows: [] };
      byLabel.set(permission.group, group);
      groups.push(group);
    }
    group.rows.push(permission);
  }
  return groups;
}

/**
 * A roles × permissions grid: one column per role, one row per permission,
 * optionally grouped. Editable cells are checkboxes with arrow-key navigation
 * (WAI-ARIA grid); locked cells show why they cannot change; read-only mode
 * shows allowed and not-allowed marks.
 */
export const PermissionMatrix = forwardRef<HTMLDivElement, PermissionMatrixProps>(
  function PermissionMatrix(
    {
      roles,
      permissions,
      value: valueProp,
      defaultValue,
      onValueChange,
      isLocked,
      readOnly = false,
      disabled = false,
      caption,
      messages,
      className,
      'aria-label': ariaLabel,
      ...rest
    },
    ref,
  ) {
    const readDir = useDirection();
    const m = { ...defaultPermissionMatrixMessages, ...messages };
    const [valueState, setValueState] = useState<PermissionMatrixValue>(defaultValue ?? {});
    const value = valueProp ?? valueState;
    const [active, setActive] = useState<[number, number]>([0, 0]);
    const tableRef = useRef<HTMLTableElement | null>(null);
    const id = useId();
    const groups = groupPermissions(permissions);
    const orderedRows = groups.flatMap((group) => group.rows);
    const activeRow = Math.min(active[0], Math.max(0, orderedRows.length - 1));
    const activeCol = Math.min(active[1], Math.max(0, roles.length - 1));

    const granted = (roleId: string, permissionId: string) =>
      value[roleId]?.includes(permissionId) ?? false;

    const lockReason = (roleId: string, permissionId: string): ReactNode => {
      const result = isLocked?.(roleId, permissionId);
      if (result === true) return m.locked;
      if (result === false || result === undefined || result === null) return null;
      return result as ReactNode;
    };

    const toggle = (roleId: string, permissionId: string, next: boolean) => {
      const current = value[roleId] ?? [];
      const nextList = next
        ? [...current.filter((p) => p !== permissionId), permissionId]
        : current.filter((p) => p !== permissionId);
      const nextValue: Record<string, string[]> = {};
      for (const [key, list] of Object.entries(value)) nextValue[key] = [...list];
      nextValue[roleId] = nextList;
      if (valueProp === undefined) setValueState(nextValue);
      onValueChange?.(nextValue, { roleId, permissionId, granted: next });
    };

    const focusCell = (row: number, col: number) => {
      const r = Math.max(0, Math.min(orderedRows.length - 1, row));
      const c = Math.max(0, Math.min(roles.length - 1, col));
      setActive([r, c]);
      tableRef.current?.querySelector<HTMLElement>(`[data-cell="${r}-${c}"]`)?.focus();
    };

    const onKeyDown = (event: ReactKeyboardEvent<HTMLTableElement>) => {
      const target = event.target as HTMLElement;
      const cell = target.closest<HTMLElement>('[data-cell]')?.dataset.cell;
      if (!cell) return;
      const [row, col] = cell.split('-').map(Number) as [number, number];
      const lastRow = orderedRows.length - 1;
      const lastCol = roles.length - 1;
      const ctrl = event.ctrlKey || event.metaKey;
      const rtl = readDir(event.currentTarget) === 'rtl';
      let next: [number, number] | null = null;
      switch (event.key) {
        case 'ArrowRight':
          next = [row, col + (rtl ? -1 : 1)];
          break;
        case 'ArrowLeft':
          next = [row, col + (rtl ? 1 : -1)];
          break;
        case 'ArrowDown':
          next = [row + 1, col];
          break;
        case 'ArrowUp':
          next = [row - 1, col];
          break;
        case 'Home':
          next = ctrl ? [0, 0] : [row, 0];
          break;
        case 'End':
          next = ctrl ? [lastRow, lastCol] : [row, lastCol];
          break;
        case 'PageDown':
          next = [Math.min(lastRow, row + 5), col];
          break;
        case 'PageUp':
          next = [Math.max(0, row - 5), col];
          break;
        default:
          return;
      }
      event.preventDefault();
      focusCell(next[0], next[1]);
    };

    const name = ariaLabel ? { 'aria-label': ariaLabel } : {};
    const captionNode = caption ? (
      <caption className="mn-permission-matrix__caption">{caption}</caption>
    ) : null;
    const headerRow = (
      <thead>
        <tr>
          <th scope="col" className="mn-permission-matrix__corner">
            {m.permissionHeader}
          </th>
          {roles.map((role) => (
            <th key={role.id} scope="col" className="mn-permission-matrix__role">
              <span className="mn-permission-matrix__role-label">{role.label}</span>
              {role.description ? (
                <span className="mn-permission-matrix__role-description">{role.description}</span>
              ) : null}
            </th>
          ))}
        </tr>
      </thead>
    );

    let rowIndex = -1;
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-permission-matrix', className)}
        data-readonly={readOnly || undefined}
        data-disabled={disabled || undefined}
      >
        <div
          className="mn-permission-matrix__scroll"
          {...(readOnly ? { tabIndex: 0, role: 'region', 'aria-labelledby': `${id}-table` } : {})}
        >
          <table
            ref={tableRef}
            id={`${id}-table`}
            className="mn-permission-matrix__table"
            {...(readOnly ? {} : { role: 'grid', onKeyDown })}
            {...name}
          >
            {captionNode}
            {headerRow}
            {groups.map((group, groupIndex) => (
              <tbody key={group.label ?? `__${groupIndex}`}>
                {group.label ? (
                  <tr className="mn-permission-matrix__group-row">
                    <th
                      scope="rowgroup"
                      colSpan={roles.length + 1}
                      className="mn-permission-matrix__group"
                    >
                      {group.label}
                    </th>
                  </tr>
                ) : null}
                {group.rows.map((permission) => {
                  rowIndex += 1;
                  const r = rowIndex;
                  const descId = `${id}-p-${permission.id}`;
                  return (
                    <tr key={permission.id} className="mn-permission-matrix__row">
                      <th scope="row" className="mn-permission-matrix__permission">
                        <span className="mn-permission-matrix__permission-label">
                          {permission.label}
                        </span>
                        {permission.description ? (
                          <span
                            id={descId}
                            className="mn-permission-matrix__permission-description"
                          >
                            {permission.description}
                          </span>
                        ) : null}
                      </th>
                      {roles.map((role, c) => {
                        const on = granted(role.id, permission.id);
                        const reason = lockReason(role.id, permission.id);
                        const reasonId = `${id}-lock-${r}-${c}`;
                        if (readOnly) {
                          return (
                            <td
                              key={role.id}
                              className="mn-permission-matrix__cell"
                              data-granted={on || undefined}
                            >
                              <span className="mn-permission-matrix__mark" aria-hidden="true">
                                {on ? <CheckGlyph /> : <DashGlyph />}
                              </span>
                              <span className="mn-permission-matrix__sr">
                                {on ? m.allowed : m.notAllowed}
                              </span>
                            </td>
                          );
                        }
                        const describedBy = cx(
                          permission.description ? descId : undefined,
                          reason ? reasonId : undefined,
                        );
                        return (
                          <td
                            key={role.id}
                            className="mn-permission-matrix__cell"
                            data-granted={on || undefined}
                            data-locked={reason ? '' : undefined}
                          >
                            <Checkbox
                              data-cell={`${r}-${c}`}
                              className="mn-permission-matrix__checkbox"
                              checked={on}
                              tabIndex={activeRow === r && activeCol === c ? 0 : -1}
                              aria-label={m.cellLabel(permission.label, role.label)}
                              aria-describedby={describedBy || undefined}
                              disabled={disabled}
                              {...(reason ? { 'aria-disabled': true } : {})}
                              onFocus={() => setActive([r, c])}
                              onCheckedChange={(checked) => {
                                if (reason || disabled) return;
                                toggle(role.id, permission.id, checked === true);
                              }}
                            />
                            {reason ? (
                              <span
                                className="mn-permission-matrix__lock"
                                title={typeof reason === 'string' ? reason : undefined}
                              >
                                <LockGlyph aria-hidden="true" />
                                <span id={reasonId} className="mn-permission-matrix__sr">
                                  {reason}
                                </span>
                              </span>
                            ) : null}
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            ))}
          </table>
        </div>
      </div>
    );
  },
);
