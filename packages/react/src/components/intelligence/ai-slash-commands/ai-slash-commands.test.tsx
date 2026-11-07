import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiPromptInput } from '../ai-prompt-input/index.js';
import {
  AiSlashCommands,
  combineAiPromptMenus,
  filterAiSlashCommands,
  useAiSlashCommands,
  type AiPromptMenuExtension,
  type AiSlashCommand,
} from './index.js';

const COMMANDS: AiSlashCommand[] = [
  { id: 'sum', name: 'summarize', description: 'Summarize the open document' },
  { id: 'tr', name: '/translate', description: 'Translate the selection', keywords: ['language'] },
  { id: 'out', name: 'outline', description: 'Draft an outline' },
  { id: 'img', name: 'image', description: 'Needs a selection', disabled: true },
];

function Harness({
  onSelect = () => {},
  onSubmit = () => {},
  extra,
}: {
  onSelect?: (c: AiSlashCommand) => unknown;
  onSubmit?: (text: string) => void;
  extra?: AiPromptMenuExtension;
}) {
  const slash = useAiSlashCommands(COMMANDS, onSelect);
  const menu = extra ? combineAiPromptMenus(slash, extra) : slash;
  return <AiPromptInput label="Message the assistant" onSubmit={onSubmit} {...menu} />;
}

function OtherMenu() {
  // A second menu on '#', built with the same hook, to test combining.
  return useAiSlashCommands([{ id: 'tag', name: 'urgent' }], () => '#urgent ', { char: '#' });
}

describe('filterAiSlashCommands', () => {
  it('ranks prefix, then substring, then description and keyword matches', () => {
    expect(filterAiSlashCommands(COMMANDS, '').map((c) => c.id)).toEqual([
      'sum',
      'tr',
      'out',
      'img',
    ]);
    expect(filterAiSlashCommands(COMMANDS, 'TR').map((c) => c.id)).toEqual(['tr']);
    expect(filterAiSlashCommands(COMMANDS, 'line').map((c) => c.id)).toEqual(['out']);
    expect(filterAiSlashCommands(COMMANDS, 'o').map((c) => c.id)).toEqual([
      'out',
      'sum',
      'tr',
      'img',
    ]);
    expect(filterAiSlashCommands(COMMANDS, 'language').map((c) => c.id)).toEqual(['tr']);
    expect(filterAiSlashCommands(COMMANDS, 'zzz')).toEqual([]);
  });
});

describe('AiSlashCommands', () => {
  it('renders a named listbox with names, descriptions and the active option', async () => {
    const ref = createRef<HTMLDivElement>();
    const onSelect = vi.fn();
    render(
      <AiSlashCommands
        ref={ref}
        data-testid="root"
        commands={COMMANDS}
        activeId="tr"
        onSelect={onSelect}
      />,
    );
    expect(ref.current).toBe(screen.getByTestId('root'));
    expect(ref.current).toHaveClass('mn-ai-slash-commands');
    const list = screen.getByRole('listbox', { name: 'Commands' });
    const options = within(list).getAllByRole('option');
    expect(options).toHaveLength(4);
    expect(options[1]).toHaveAccessibleName('/translate');
    expect(options[1]).toHaveAccessibleDescription('Translate the selection');
    expect(options[1]).toHaveAttribute('aria-selected', 'true');
    expect(options[3]).toHaveAttribute('aria-disabled', 'true');
    await userEvent.click(options[3] as HTMLElement);
    expect(onSelect).not.toHaveBeenCalled();
    await userEvent.click(options[0] as HTMLElement);
    expect(onSelect).toHaveBeenCalledWith(COMMANDS[0]);
    await expectNoA11yViolations();
  });

  it('shows the empty message', () => {
    render(<AiSlashCommands commands={[]} query="zz" onSelect={() => {}} />);
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(screen.getByText('No commands match “/zz”')).toBeInTheDocument();
  });
});

describe('useAiSlashCommands', () => {
  it('opens on /, filters, moves with arrows and inserts the returned text', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn((c: AiSlashCommand) => `/${c.name.replace('/', '')} `);
    render(<Harness onSelect={onSelect} />);
    const box = screen.getByRole('textbox', { name: 'Message the assistant' });
    expect(box).not.toHaveAttribute('aria-controls');
    await user.click(box);
    await user.keyboard('/');
    const list = screen.getByRole('listbox', { name: 'Commands' });
    expect(box).toHaveAttribute('aria-autocomplete', 'list');
    expect(box).toHaveAttribute('aria-controls', list.id);
    const first = within(list).getAllByRole('option')[0] as HTMLElement;
    expect(box).toHaveAttribute('aria-activedescendant', first.id);
    await expectNoA11yViolations();

    await user.keyboard('t');
    // Prefix match first, then name matches, then description matches.
    expect(
      within(list)
        .getAllByRole('option')
        .map((o) => o.textContent)
        .slice(0, 2),
    ).toEqual(['/translateTranslate the selection', '/outlineDraft an outline']);
    await user.keyboard('{ArrowDown}');
    const outline = screen.getByRole('option', { name: /outline/ });
    expect(outline).toHaveAttribute('aria-selected', 'true');
    expect(box).toHaveAttribute('aria-activedescendant', outline.id);
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('option', { name: /summarize/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await user.keyboard('{ArrowUp}{Enter}');
    expect(onSelect).toHaveBeenCalledWith(COMMANDS[2]);
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(box).toHaveValue('/outline ');
    expect(box).toHaveFocus();
  });

  it('skips disabled commands and removes the query when onSelect returns nothing', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    const onSubmit = vi.fn();
    render(<Harness onSelect={onSelect} onSubmit={onSubmit} />);
    const box = screen.getByRole('textbox');
    await user.click(box);
    await user.keyboard('hi /ima');
    // Only the disabled "image" matches: nothing is active, Enter is left to the input.
    expect(box).not.toHaveAttribute('aria-activedescendant');
    await user.keyboard('{Backspace}{Backspace}{Backspace}su{Tab}');
    expect(onSelect).toHaveBeenCalledWith(COMMANDS[0]);
    expect(box).toHaveValue('hi ');
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('Escape closes the menu without sending; Enter sends when no command matches', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Harness onSubmit={onSubmit} />);
    const box = screen.getByRole('textbox');
    await user.click(box);
    await user.keyboard('/');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(box).not.toHaveAttribute('aria-controls');
    await user.keyboard('zz');
    await user.keyboard('{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('/zz', { files: [] });
  });

  it('picks with the pointer and keeps focus in the textarea', async () => {
    const user = userEvent.setup();
    render(<Harness onSelect={() => '/summarize '} />);
    const box = screen.getByRole('textbox');
    await user.click(box);
    await user.keyboard('/');
    await user.click(screen.getByRole('option', { name: /summarize/ }));
    expect(box).toHaveValue('/summarize ');
    expect(box).toHaveFocus();
  });
});

describe('combineAiPromptMenus', () => {
  it('routes each trigger to its own menu and keys', async () => {
    const user = userEvent.setup();
    function Combined() {
      const other = OtherMenu();
      return <Harness extra={other} />;
    }
    render(<Combined />);
    const box = screen.getByRole('textbox');
    await user.click(box);
    await user.keyboard('#');
    expect(screen.getByRole('option', { name: '#urgent' })).toBeInTheDocument();
    expect(box).toHaveAttribute('aria-controls');
    await user.keyboard('{Enter}');
    expect(box).toHaveValue('#urgent ');
    await user.keyboard('/');
    expect(screen.getAllByRole('option')).toHaveLength(4);
  });
});

it('renders on the server', () => {
  const html = renderToString(<AiSlashCommands commands={COMMANDS} onSelect={() => {}} />);
  expect(html).toContain('mn-ai-slash-commands');
  expect(html).toContain('/summarize');
  expect(renderToString(<Harness />)).toContain('aria-autocomplete="list"');
});
