import { useState } from 'react';
import {
  PermissionMatrix,
  type PermissionMatrixPermission,
  type PermissionMatrixRole,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './permission-matrix.fixture.json';

const ROLES = fixture.data.roles as PermissionMatrixRole[];

const PERMISSIONS = fixture.data.permissions as unknown as PermissionMatrixPermission[];

const INITIAL: Record<string, string[]> = fixture.data.grants;

function Editable() {
  const [value, setValue] = useState(INITIAL);
  return (
    <PermissionMatrix
      caption={fixture.data.caption}
      roles={ROLES}
      permissions={PERMISSIONS}
      value={value}
      onValueChange={setValue}
      isLocked={(roleId) =>
        roleId === fixture.data.lockedRole ? fixture.data.lockReason : undefined
      }
    />
  );
}

export default {
  id: 'permission-matrix',
  title: 'PermissionMatrix',
  summary: 'A roles × permissions grid, editable with checkboxes and arrow keys.',
  sections: {
    purpose: {
      summary:
        'Admins need to see and change what every role can do at a glance. PermissionMatrix lays roles out as columns and permissions as rows (optionally grouped), with a checkbox in each cell, full grid keyboard navigation, locked cells that explain why they cannot change, and a read-only mode for people who may look but not edit. Needed by: Rook People (`settings/permissions`, `components/admin/PermissionsView.tsx`) and Rook ID workspace roles.',
      useWhen: [
        'Editing the permissions of several roles side by side.',
        'Showing a comparison of roles (read-only) next to a RolePicker.',
      ],
      avoidWhen: [
        'Assigning a role to a person: use RolePicker.',
        'One role’s permissions only: a CheckboxGroup or SwitchSettingsList reads better.',
        'Dozens of roles: a per-role edit page scales better than a wide grid.',
      ],
    },
    designPrinciples: {
      items: [
        'Permission names are written as actions (“Invite members”), so each cell reads as a sentence.',
        'One tab stop for the whole grid; arrow keys move between cells, so it is fast with a keyboard.',
        'Locked cells stay focusable and say why they are locked.',
        'Controlled data in, the changed cell out: the product decides how and when to save.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`value` is `Record<roleId, permissionId[]>`; `onValueChange(next, { roleId, permissionId, granted })`.',
        '`isLocked(roleId, permissionId)` returns a reason, `true` for the default reason, or nothing.',
        'Migrating from the app copy (Rook People `PermissionsView` role dialog): the per-role list of checkboxes becomes one matrix; map the catalogue to `permissions` (use its section as `group`) and role grants to `value`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'readOnly',
          values: ['false', 'true'],
          description:
            'Editable grid of checkboxes (default), or a plain table with allowed / not-allowed marks.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Granted',
          attribute: 'data-granted',
          description: 'On cells whose permission is granted.',
        },
        {
          name: 'Locked',
          attribute: 'data-locked',
          description:
            'The cell cannot be changed; its checkbox is `aria-disabled` with the reason.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description: 'No checkboxes; marks with text alternatives.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Every checkbox is disabled, e.g. while saving.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a checkbox grants or removes that permission for that role.',
        'Locked cells ignore clicks and keep their value.',
        'On narrow screens the grid scrolls sideways while the permission column stays in place.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the grid at the last focused cell, or out of it.' },
        { keys: 'ArrowLeft / ArrowRight', action: 'Previous or next role (mirrored in RTL).' },
        { keys: 'ArrowUp / ArrowDown', action: 'Previous or next permission.' },
        { keys: 'Home / End', action: 'First or last role in the row.' },
        { keys: 'Ctrl+Home / Ctrl+End', action: 'First or last cell of the grid.' },
        { keys: 'PageUp / PageDown', action: 'Five permissions up or down.' },
        { keys: 'Space', action: 'Toggles the focused cell.' },
      ],
    },
    accessibility: {
      role: 'grid (editable) or table (read-only)',
      name: 'The `caption`, or `aria-label`.',
      items: [
        'Follows the WAI-ARIA grid pattern with a roving tab stop.',
        'Roles are column headers, permissions row headers and groups row-group headers.',
        'Each checkbox is named "{permission} for {role}" and described by the permission’s description and any lock reason.',
        'Read-only marks have “Allowed” / “Not allowed” text for screen readers.',
      ],
    },
    responsive: {
      items: [
        'The grid scrolls horizontally inside its own frame; the page never scrolls sideways at 390px.',
        'The permission column is sticky on the inline-start side.',
        'Cells are at least 44px on touch screens.',
      ],
    },
    theme: {
      items: [
        'Headers on the subtle background; group rows on the muted background in small caps.',
        'Granted read-only marks use the success strong colour.',
        'Forced-colors mode draws every cell border in CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'Editable with locked owner column', stack: true, render: () => <Editable /> },
        {
          title: 'Read-only comparison',
          stack: true,
          render: () => (
            <PermissionMatrix
              caption="Compare roles"
              readOnly
              roles={ROLES}
              permissions={PERMISSIONS}
              value={INITIAL}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Order roles from least to most access, left to right.',
        'Lock cells that the system enforces, and say why.',
        'Save changes with an explicit button or with undo, and say when they take effect.',
      ],
      dont: [
        'Do not use colour alone for granted cells in read-only mode; the marks and text carry it.',
        'Do not hide permissions a role cannot have; lock them instead.',
      ],
    },
    related: {
      items: [
        { id: 'role-picker', reason: 'Assign one of these roles to a person.' },
        { id: 'checkbox', reason: 'The control in each cell.' },
        { id: 'data-table', reason: 'For lists of records rather than a fixed matrix.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['checkbox'] },
    testing: {
      unit: [
        'Renders a grid with row, column and group headers; locked reasons describe cells; axe passes.',
        'One tab stop; arrow, Home, End and Ctrl+Home move focus; Tab leaves and returns.',
        'Space toggles and reports the change; locked cells do not change.',
        'Controlled value.',
        'Read-only marks with text alternatives; axe passes.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
