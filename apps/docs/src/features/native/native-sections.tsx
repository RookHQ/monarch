import type { NativeComponentDocs } from '../../../plugins/native.js';
import { use, useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { loadDocs } from '../../lib/docs.js';
import { href } from '../../lib/router.js';
import { PageSection } from '../../layout/page-frame.js';
import { PLATFORM_NAMES, type Platform } from './labels.js';
import { NativeDocs, hasNativeDocs } from './native-docs.js';

export type PlatformTab = 'web' | Platform;

/** The tabs in reading order: the web first, then iOS and Android. */
export const TABS: readonly PlatformTab[] = ['web', 'ios', 'android'];
const TAB_NAMES: Readonly<Record<PlatformTab, string>> = { web: 'Web', ...PLATFORM_NAMES };

/** The id of a tab's panel. The web panel is the component page's own content. */
export const panelId = (tab: PlatformTab) => `panel-${tab}`;
const tabId = (tab: PlatformTab) => `tab-${tab}`;

/**
 * The tab whose panel holds the element a URL fragment names: `#ios` and
 * `#android` (the platform sections) and every heading inside them, `#web`
 * for the web panel. Null when the fragment is empty, or names nothing that
 * is in a platform panel, so the current tab stays.
 */
export function tabOfFragment(fragment: string, doc: Document = document): PlatformTab | null {
  const id = decodeURIComponent(fragment.replace(/^#/, ''));
  if (!id) return null;
  if (id === 'web') return 'web';
  const panel = doc.getElementById(id)?.closest('[role="tabpanel"]');
  const tab = panel?.id.replace(/^panel-/, '') as PlatformTab | undefined;
  return tab && TABS.includes(tab) ? tab : null;
}

/** The next tab for an arrow, Home or End key, or null for any other key. Arrows wrap. */
export function nextTab(current: PlatformTab, key: string, rtl = false): PlatformTab | null {
  const i = TABS.indexOf(current);
  const n = TABS.length;
  const step =
    key === 'ArrowRight' ? (rtl ? -1 : 1) : key === 'ArrowLeft' ? (rtl ? 1 : -1) : undefined;
  if (step !== undefined) return TABS[(i + step + n) % n] ?? null;
  if (key === 'Home') return TABS[0] ?? null;
  if (key === 'End') return TABS[n - 1] ?? null;
  return null;
}

/**
 * The Web | iOS | Android tabs of a component page (WAI-ARIA tabs with
 * automatic activation), and the iOS and Android panels, from the native
 * inventories (or the plan, where a platform has no entry yet). The web
 * panel is the page's own content, which follows this element; the tabs show
 * and hide it by its id, so the web content costs the page nothing extra.
 *
 * The prerendered HTML has the Web tab selected and all three panels in it,
 * so readers, crawlers and the Markdown copy get every platform. The
 * component page loads this module and the native data after it has
 * hydrated (pages/components/native-slot.tsx); `live` turns on the
 * behaviour: keyboard, the URL fragment, and showing the selected panel.
 */
export function NativeSections({
  title,
  docs,
  live = false,
}: {
  title: string;
  docs: NativeComponentDocs;
  live?: boolean;
}) {
  // Live, the sections replace their prerendered copy in one commit, so a
  // fragment such as `#ios` can be resolved against that copy at once.
  const [selected, setSelected] = useState<PlatformTab>(() =>
    live ? (tabOfFragment(window.location.hash) ?? 'web') : 'web',
  );
  const tabs = useRef<Partial<Record<PlatformTab, HTMLButtonElement | null>>>({});

  // A fragment selects the tab that holds it: on load, and when an in-page
  // link or the "On this page" list changes it. The target is scrolled to
  // once its panel is shown.
  useEffect(() => {
    if (!live) return;
    let frame = 0;
    const follow = () => {
      const tab = tabOfFragment(window.location.hash);
      if (!tab) return;
      setSelected(tab);
      const id = decodeURIComponent(window.location.hash.slice(1));
      frame = requestAnimationFrame(() =>
        document.getElementById(id)?.scrollIntoView({ block: 'start' }),
      );
    };
    follow();
    window.addEventListener('hashchange', follow);
    return () => {
      window.removeEventListener('hashchange', follow);
      cancelAnimationFrame(frame);
    };
  }, [live]);

  // The web panel is outside this component.
  useEffect(() => {
    if (!live) return;
    const web = document.getElementById(panelId('web'));
    web?.toggleAttribute('hidden', selected !== 'web');
    return () => web?.removeAttribute('hidden');
  }, [live, selected]);

  const select = (tab: PlatformTab, focus: boolean) => {
    setSelected(tab);
    if (focus) tabs.current[tab]?.focus();
    // The address names the tab, so it can be shared, without scrolling.
    const url = new URL(window.location.href);
    url.hash = tab === 'web' ? '' : tab;
    window.history.replaceState(window.history.state, '', url);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    const rtl = getComputedStyle(event.currentTarget).direction === 'rtl';
    const next = nextTab(selected, event.key, rtl);
    if (!next) return;
    event.preventDefault();
    select(next, true);
  };

  return (
    <>
      <div
        className="docs-platform-tabs"
        role="tablist"
        aria-label="Platform"
        data-live={live || undefined}
      >
        {TABS.map((tab) => (
          <button
            key={tab}
            ref={(el) => {
              tabs.current[tab] = el;
            }}
            type="button"
            role="tab"
            id={tabId(tab)}
            className="docs-platform-tabs__tab"
            aria-selected={selected === tab}
            aria-controls={panelId(tab)}
            tabIndex={selected === tab ? 0 : -1}
            onClick={live ? () => select(tab, false) : undefined}
            onKeyDown={live ? onKeyDown : undefined}
          >
            {TAB_NAMES[tab]}
          </button>
        ))}
      </div>
      {TABS.filter((tab): tab is Platform => tab !== 'web').map((platform) => {
        const platformDocs = docs[platform];
        return (
          <div
            key={platform}
            role="tabpanel"
            id={panelId(platform)}
            aria-labelledby={tabId(platform)}
            className="docs-platform-panel"
            hidden={selected !== platform}
          >
            <PageSection id={platform} title={PLATFORM_NAMES[platform]}>
              {hasNativeDocs(platformDocs) ? (
                <NativeDocs platform={platform} id={platform} title={title} docs={platformDocs} />
              ) : (
                <p className="docs-text">
                  Monarch for {PLATFORM_NAMES[platform]} has no documentation for {title}.
                </p>
              )}
              <p className="docs-footnote">
                Apps built with Monarch for {PLATFORM_NAMES[platform]} also use{' '}
                {platform === 'ios' ? 'Apple' : 'Google'}&apos;s SDK under its own terms; see the{' '}
                <a href={href(`/${platform}/guide`, 'platform-terms')}>platform terms</a>.
              </p>
            </PageSection>
          </div>
        );
      })}
    </>
  );
}

/** The sections with their behaviour, once the page has hydrated. */
export function LiveNativeSections(props: { title: string; docs: NativeComponentDocs }) {
  return <NativeSections {...props} live />;
}

/**
 * A component's Android and iOS documentation: the inventory entries of
 * `platforms/*`, or the plan where there is none yet. Each docs file imports
 * its own (the `loadNative` export added by plugins/native.ts).
 */
export const loadNative = (id: string): Promise<NativeComponentDocs> =>
  loadDocs(id)
    .then((d) => d.loadNative())
    .then((m) => m.default);

const pending = new Map<string, Promise<NativeComponentDocs>>();

/** The same sections for the prerender step, which waits for the component's native data. */
export function PrerenderedNativeSections({ id, title }: { id: string; title: string }) {
  let docs = pending.get(id);
  if (!docs) pending.set(id, (docs = loadNative(id)));
  return <NativeSections title={title} docs={use(docs)} />;
}
