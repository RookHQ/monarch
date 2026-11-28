import { useState } from 'react';
import { DropdownMenuItem } from '@rookcom/monarch-react';
import {
  AccountMenu,
  type AccountMenuTheme,
  type AccountMenuWorkspace,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './account-menu.fixture.json';

const PERSON = { id: 'person-1', ...fixture.data.user };
const WORKSPACES: AccountMenuWorkspace[] = fixture.data.workspaces;

function Interactive() {
  const [workspace, setWorkspace] = useState(fixture.data.workspace);
  const [theme, setTheme] = useState(fixture.data.theme as AccountMenuTheme);
  return (
    <AccountMenu
      defaultOpen
      user={PERSON}
      profileHref="#profile"
      settingsHref="#settings"
      workspaces={WORKSPACES}
      currentWorkspaceId={workspace}
      onWorkspaceChange={setWorkspace}
      theme={theme}
      onThemeChange={setTheme}
      items={<DropdownMenuItem>{fixture.data.extraItem}</DropdownMenuItem>}
      onSignOut={() => undefined}
    />
  );
}

export default {
  id: 'account-menu',
  title: 'AccountMenu',
  summary: 'The signed-in person’s avatar menu: profile, settings, workspace, theme and sign out.',
  sections: {
    purpose: {
      summary:
        'Every signed-in screen needs one predictable place for the person’s own things: who is signed in, their profile and settings, which workspace they are in, the colour scheme and Sign out. AccountMenu is that menu, opened from the avatar, with slots for the product’s own workspace switcher and items. Needed by: apps that do not use the Rook shell’s IdentityBar (Rook ID, marketing sites with a signed-in state) and the shell itself, which has its own internal copy of these items.',
      useWhen: [
        'The top bar or sidebar footer of a signed-in app.',
        'A page outside the app shell that still needs the account controls.',
      ],
      avoidWhen: [
        'Inside the Rook app shell: IdentityBar and RookRail already include the account menu.',
        'Choosing a workspace as the main task: use WorkspaceSwitcher.',
      ],
    },
    designPrinciples: {
      items: [
        'Who is signed in comes first, so people know which account they are about to change.',
        'Sign out is always last and separated, so it is not chosen by accident.',
        'Links are real links (Profile, Settings) and work with the product’s router through `renderLink`.',
        'Callbacks out, no storage: the product saves the theme and switches the workspace.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`user` is `{ name, email?, avatarSrc?, id?, status? }`; `workspaces` is `{ id, name, avatarSrc? }[]`.',
        '`renderLink({ href, className, children })` renders the Profile and Settings links, e.g. with Next.js `Link`.',
        'Migrating from shadcn/ui: a hand-built `<DropdownMenu><DropdownMenuTrigger><Avatar/></DropdownMenuTrigger><DropdownMenuContent>…` user menu becomes `<AccountMenu user profileHref settingsHref onSignOut />`; any other `DropdownMenuItem`s move into `items`, and a theme toggle item becomes `theme` + `onThemeChange`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showName',
          values: ['false', 'true'],
          description: 'Avatar only (default), or avatar, name and a chevron for wider bars.',
        },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Menu alignment against the trigger. Default `end` for a top-right avatar.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'On the trigger while the menu is open.',
        },
        {
          name: 'With name',
          attribute: 'data-show-name',
          description: 'The trigger shows the name and a chevron.',
        },
        {
          name: 'Checked',
          attribute: 'aria-checked (menuitemradio)',
          description: 'The current workspace and theme.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the avatar opens the menu; choosing an item closes it and returns focus to the avatar.',
        'Profile and Settings follow their links, or call `onProfile` / `onSettings`.',
        'Choosing a workspace or theme calls `onWorkspaceChange(id)` or `onThemeChange(theme)`.',
        'Sections without data (no workspaces, no `onThemeChange`, no `onSignOut`) are left out.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ArrowDown', action: 'Opens the menu on the first item.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Moves between items.' },
        { keys: 'Home / End', action: 'First or last item.' },
        { keys: 'A letter', action: 'Jumps to the next item starting with it.' },
        { keys: 'Enter / Space', action: 'Chooses the item.' },
        { keys: 'Escape', action: 'Closes the menu and returns focus to the avatar.' },
      ],
    },
    accessibility: {
      role: 'button (aria-haspopup="menu") opening a menu',
      name: '"Account menu for {name}" (`messages.triggerLabel`).',
      items: [
        'Follows the WAI-ARIA menu button pattern (Radix DropdownMenu).',
        'Workspace and theme choices are `menuitemradio` inside named groups.',
        'Avatars are decorative because the name is always next to them or in the trigger label.',
      ],
    },
    responsive: {
      items: [
        'The menu is at most the viewport width minus 2rem; long names and emails truncate.',
        'The trigger and every item are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses the shared menu styles: raised surface, subtle border, muted highlight.',
        'The trigger is transparent with a muted background on hover and while open.',
        'Forced-colors mode outlines the trigger and keeps link items in menu colours.',
      ],
    },
    examples: {
      items: [
        { title: 'Workspaces, theme and sign out', stage: true, render: () => <Interactive /> },
        {
          title: 'With the name in the trigger',
          render: () => (
            <AccountMenu
              user={PERSON}
              showName
              profileHref="#profile"
              onSignOut={() => undefined}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show the email so people with several accounts know which one is signed in.',
        'Pass the router link through `renderLink` so navigation stays client-side.',
      ],
      dont: [
        'Do not put destructive account actions (delete account) in this menu: link to settings instead.',
        'Do not add a second Sign out elsewhere in the same bar.',
      ],
    },
    related: {
      items: [
        { id: 'dropdown-menu', reason: 'The menu it is built on.' },
        { id: 'workspace-switcher', reason: 'A standalone workspace picker.' },
        { id: 'avatar', reason: 'The trigger image with initials fallback.' },
        { id: 'theme-toggle', reason: 'A standalone colour scheme control.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'dropdown-menu'] },
    testing: {
      unit: [
        'Opens from the keyboard with the person, links and Sign out; focus returns; axe passes.',
        'Workspace and theme radio items call their callbacks.',
        'Workspace slot, extra items and a custom link component.',
        'Translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
