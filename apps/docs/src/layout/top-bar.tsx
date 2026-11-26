import { NEW_TAB } from '../lib/external.js';
import { Badge, Button, IconButton } from '@rookcom/monarch-react';
import { reactVersion } from '../lib/docs.js';
import { href } from '../lib/router.js';
import { Wordmark } from '../features/brand/crown-mark.js';
import { SearchPalette } from '../features/search/search-palette.js';
import { ThemeSwitcher } from '../features/appearance/theme-switcher.js';
import { AppearanceSettings } from '../features/appearance/appearance-settings.js';
import { MenuIcon, RepoIcon } from '../ui/icons.js';
import { NAV, type NavGroup } from '../app/navigation.js';

/** The drawer (a modal Sheet) loads the first time it is opened, or when its button is hovered. */
export const loadDrawer = () => import('./nav-drawer.js');

export const REPO_URL = 'https://github.com/RookHQ/monarch';

/** Section links in the bar, each to the first page of its group. */
const SECTIONS = NAV.map((g) => ({ group: g, path: g.items[0]?.path ?? '/' }));

/**
 * The sticky top bar: menu (narrow screens), wordmark, section links (wide
 * screens), search, theme, appearance settings and the repository link.
 */
export function TopBar({
  currentGroup,
  onOpenMenu,
  menuOpen,
}: {
  currentGroup: NavGroup | undefined;
  onOpenMenu: () => void;
  menuOpen: boolean;
}) {
  return (
    <header className="docs-topbar">
      <div className="docs-topbar__inner">
        <IconButton
          className="docs-topbar__menu"
          label="Open navigation"
          icon={<MenuIcon />}
          variant="ghost"
          size="sm"
          aria-expanded={menuOpen}
          aria-haspopup="dialog"
          onClick={onOpenMenu}
          onPointerEnter={() => void loadDrawer()}
          onFocus={() => void loadDrawer()}
        />
        <a className="docs-topbar__brand" href={href('/')} aria-label="Monarch home">
          <Wordmark />
        </a>
        <Badge size="sm" variant="outline" className="docs-topbar__version">
          v{reactVersion}
        </Badge>
        <nav className="docs-topbar__sections" aria-label="Sections">
          {SECTIONS.map(({ group, path }) => (
            <a
              key={group.id}
              href={href(path)}
              className="docs-topbar__section"
              aria-current={currentGroup?.id === group.id ? 'true' : undefined}
            >
              {group.heading}
            </a>
          ))}
        </nav>
        <div className="docs-topbar__actions">
          <SearchPalette />
          <ThemeSwitcher compact className="docs-topbar__theme" />
          <AppearanceSettings />
          <Button asChild variant="ghost" size="sm" leadingIcon={<RepoIcon />}>
            <a {...NEW_TAB} href={REPO_URL} className="docs-topbar__repo">
              <span className="docs-topbar__repo-label">GitHub</span>
            </a>
          </Button>
        </div>
      </div>
    </header>
  );
}
