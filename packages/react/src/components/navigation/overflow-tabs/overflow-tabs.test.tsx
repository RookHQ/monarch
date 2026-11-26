import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { triggerResize } from '../../../../test/setup.js';
import { TabsContent } from '../tabs/index.js';
import { OverflowTabs, type OverflowTabsItem } from './index.js';

const TABS: OverflowTabsItem[] = [
  { value: 'overview', label: 'Overview' },
  { value: 'activity', label: 'Activity', count: 4, countTone: 'accent' },
  { value: 'files', label: 'Files' },
  { value: 'members', label: 'Members' },
  { value: 'billing', label: 'Billing', disabled: true },
  { value: 'settings', label: 'Settings' },
];

function Panels() {
  return (
    <>
      {TABS.map((t) => (
        <TabsContent key={t.value} value={t.value}>
          {t.label} panel
        </TabsContent>
      ))}
    </>
  );
}

/** Gives the bar a width and every measured tab a width, as a browser would. */
function mockLayout(barWidth: number, tabWidth = 100, moreWidth = 60) {
  vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockImplementation(function (
    this: HTMLElement,
  ) {
    return this.classList.contains('mn-overflow-tabs__bar') ? barWidth : 0;
  });
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (
    this: HTMLElement,
  ) {
    const inMeasure = this.parentElement?.classList.contains('mn-overflow-tabs__measure');
    const width = !inMeasure
      ? 0
      : this.classList.contains('mn-overflow-tabs__more')
        ? moreWidth
        : tabWidth;
    return {
      width,
      height: 36,
      top: 0,
      left: 0,
      right: width,
      bottom: 36,
      x: 0,
      y: 0,
      toJSON() {},
    };
  });
}

const rowTabs = () => screen.getAllByRole('tab').map((t) => t.textContent);

describe('OverflowTabs', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders every tab when there is room, with Tabs semantics', async () => {
    render(
      <OverflowTabs aria-label="Project sections" tabs={TABS}>
        <Panels />
      </OverflowTabs>,
    );
    expect(screen.getByRole('tablist', { name: 'Project sections' })).toBeInTheDocument();
    expect(rowTabs()).toEqual(['Overview', 'Activity4', 'Files', 'Members', 'Billing', 'Settings']);
    expect(screen.getByRole('tab', { name: 'Overview' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Overview panel');
    expect(screen.queryByRole('button', { name: 'More' })).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('re-measures when the bar is resized (ResizeObserver)', () => {
    mockLayout(1000);
    const { container } = render(
      <OverflowTabs aria-label="Sections" tabs={TABS}>
        <Panels />
      </OverflowTabs>,
    );
    expect(rowTabs()).toHaveLength(6);
    vi.restoreAllMocks();
    mockLayout(360);
    const bar = container.querySelector('.mn-overflow-tabs__bar') as HTMLElement;
    act(() => triggerResize(bar, { width: 360 }));
    expect(rowTabs()).toEqual(['Overview', 'Activity4', 'Files']);
    expect(screen.getByRole('button', { name: 'More' })).toBeInTheDocument();
  });

  describe('overflow', () => {
    beforeEach(() => mockLayout(360));

    it('moves tabs that do not fit into More; arrows rove the visible tabs', async () => {
      render(
        <OverflowTabs aria-label="Sections" tabs={TABS}>
          <Panels />
        </OverflowTabs>,
      );
      // 360 - 60 (More) leaves room for three 100px tabs.
      expect(rowTabs()).toEqual(['Overview', 'Activity4', 'Files']);
      const user = userEvent.setup();
      await user.tab();
      expect(screen.getByRole('tab', { name: 'Overview' })).toHaveFocus();
      await user.keyboard('{ArrowRight}');
      expect(screen.getByRole('tab', { name: 'Activity 4' })).toHaveFocus();
      await user.keyboard('{End}');
      expect(screen.getByRole('tab', { name: 'Files' })).toHaveFocus();
      await user.tab();
      expect(screen.getByRole('button', { name: 'More' })).toHaveFocus();
      await expectNoA11yViolations();
    });

    it('selects a tab from the menu, brings it into the row and focuses it', async () => {
      const onValueChange = vi.fn();
      render(
        <OverflowTabs aria-label="Sections" tabs={TABS} onValueChange={onValueChange}>
          <Panels />
        </OverflowTabs>,
      );
      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: 'More' }));
      const menu = await screen.findByRole('menu');
      const items = within(menu).getAllByRole('menuitem');
      expect(items.map((i) => i.textContent)).toEqual(['Members', 'Billing', 'Settings']);
      expect(items[1]).toHaveAttribute('aria-disabled', 'true');
      await user.click(within(menu).getByRole('menuitem', { name: 'Settings' }));
      expect(onValueChange).toHaveBeenCalledWith('settings');
      const settings = screen.getByRole('tab', { name: 'Settings' });
      expect(settings).toHaveAttribute('aria-selected', 'true');
      expect(rowTabs()).toEqual(['Overview', 'Activity4', 'Settings']);
      expect(screen.getByRole('tabpanel')).toHaveTextContent('Settings panel');
      await waitFor(() => expect(settings).toHaveFocus());
    });

    it('keeps a controlled selected tab visible and translates More', () => {
      function Controlled() {
        const [value, setValue] = useState('members');
        return (
          <OverflowTabs
            aria-label="Sections"
            tabs={TABS}
            value={value}
            onValueChange={setValue}
            messages={{ more: 'Mehr' }}
            variant="pill"
          >
            <Panels />
          </OverflowTabs>
        );
      }
      render(<Controlled />);
      expect(rowTabs()).toEqual(['Overview', 'Activity4', 'Members']);
      expect(screen.getByRole('button', { name: 'Mehr' })).toBeInTheDocument();
      expect(screen.getByRole('tablist')).toHaveAttribute('data-variant', 'pill');
    });
  });

  it('forwards its ref to the Tabs root and passes classes through', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <OverflowTabs
        ref={ref}
        aria-label="Sections"
        tabs={TABS}
        defaultValue="files"
        className="extra"
        listClassName="list-extra"
      >
        <Panels />
      </OverflowTabs>,
    );
    expect(ref.current).toHaveClass('mn-overflow-tabs', 'extra');
    expect(screen.getByRole('tablist')).toHaveClass('list-extra');
    expect(screen.getByRole('tab', { name: 'Files' })).toHaveAttribute('aria-selected', 'true');
  });

  it('renders on the server with every tab', () => {
    const html = renderToString(
      <OverflowTabs aria-label="Sections" tabs={TABS}>
        <Panels />
      </OverflowTabs>,
    );
    expect(html).toContain('mn-overflow-tabs');
    expect(html).toContain('role="tablist"');
    expect(html).toContain('Settings');
  });
});
