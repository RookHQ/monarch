import { spawn } from 'node:child_process';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { PROTOCOL_VERSIONS, createTools, handleMessage } from '../src/mcp.js';
import { here, tempDir } from './helpers.js';

const MANIFEST = join(here, 'fixtures/manifest/manifest.json');
const tools = createTools({ root: tempDir(), manifestPath: MANIFEST });
const call = (id: number, method: string, params?: unknown) =>
  handleMessage(tools, { jsonrpc: '2.0', id, method, ...(params ? { params } : {}) });

interface ToolResult {
  content: { type: string; text: string }[];
  structuredContent?: Record<string, unknown>;
  isError: boolean;
}
const callTool = async (name: string, args: unknown) =>
  (await call(9, 'tools/call', { name, arguments: args }))?.result as ToolResult;

describe('MCP protocol', () => {
  it('initialises with a supported protocol version', async () => {
    const r = await call(1, 'initialize', { protocolVersion: '2025-03-26', capabilities: {} });
    expect(r?.result).toMatchObject({
      protocolVersion: '2025-03-26',
      capabilities: { tools: {} },
      serverInfo: { name: 'monarch' },
    });
    const newer = await call(2, 'initialize', { protocolVersion: '2099-01-01' });
    expect((newer?.result as { protocolVersion: string }).protocolVersion).toBe(
      PROTOCOL_VERSIONS[0],
    );
  });

  it('answers ping, ignores notifications and rejects unknown methods', async () => {
    expect(await call(3, 'ping')).toEqual({ jsonrpc: '2.0', id: 3, result: {} });
    expect(
      await handleMessage(tools, { jsonrpc: '2.0', method: 'notifications/initialized' }),
    ).toBeNull();
    expect((await call(4, 'resources/list'))?.error?.code).toBe(-32601);
    expect((await handleMessage(tools, 'nonsense'))?.error?.code).toBe(-32600);
  });

  it('lists the five tools with input schemas', async () => {
    const r = (await call(5, 'tools/list'))?.result as {
      tools: { name: string; inputSchema: object }[];
    };
    expect(r.tools.map((t) => t.name)).toEqual([
      'search_components',
      'get_component',
      'list_tokens',
      'get_theme',
      'check_code',
    ]);
    for (const t of r.tools) expect(t.inputSchema).toMatchObject({ type: 'object' });
  });

  it('reports bad arguments as invalid params and tool failures as tool errors', async () => {
    expect(
      (await call(6, 'tools/call', { name: 'get_component', arguments: {} }))?.error?.code,
    ).toBe(-32602);
    expect((await call(7, 'tools/call', { name: 'nope' }))?.error?.code).toBe(-32602);
    const missing = await callTool('get_component', { name: 'Nope' });
    expect(missing.isError).toBe(true);
    expect(missing.content[0]?.text).toContain('not an export');
  });
});

describe('MCP tools', () => {
  it('search_components returns general components first', async () => {
    const r = await callTool('search_components', { query: 'sign in' });
    const results = r.structuredContent?.results as { name: string; scope: string }[];
    expect(results.map((x) => [x.name, x.scope])).toEqual([
      ['SignInForm', 'general'],
      ['SignInWithRookButton', 'rook'],
    ]);
  });

  it('get_component returns props with defaults and values, import, css and docs', async () => {
    const r = (await callTool('get_component', { name: 'Button' })).structuredContent as {
      import: string;
      css: string[];
      props: { name: string; default?: string; values?: string[] }[];
      docs: string;
      markdown: string;
    };
    expect(r.import).toBe("import { Button } from '@rookcom/monarch-react';");
    expect(r.props[0]).toMatchObject({
      name: 'variant',
      default: "'secondary'",
      values: ['primary', 'secondary'],
    });
    expect(r.markdown).toBe('https://monarchui.dev/components/button/index.md');
  });

  it('check_code finds problems in a snippet and returns fixes', async () => {
    const r = (
      await callTool('check_code', {
        code: "import '@rookcom/monarch-react/dist/styles.css';\nexport const x = () => <div onClick={() => 1}>x</div>;\n",
        filename: 'x.tsx',
      })
    ).structuredContent as { findings: { rule: string; help: string; fix?: unknown }[] };
    const rules = r.findings.map((f) => f.rule);
    expect(rules).toContain('a11y/click-without-key');
    expect(r.findings.find((f) => f.rule === 'imports/package-path')?.fix).toBeDefined();
    expect(r.findings[0]?.help).toBeTruthy();
    expect((await call(8, 'tools/call', { name: 'check_code', arguments: {} }))?.error?.code).toBe(
      -32602,
    );
  });

  it('list_tokens and get_theme use the tokens package', async () => {
    const tokens = (await callTool('list_tokens', { prefix: 'space', limit: 3 }))
      .structuredContent as {
      total: number;
      tokens: { path: string }[];
    };
    expect(tokens.total).toBeGreaterThan(0);
    expect(tokens.tokens.every((t) => t.path.startsWith('space'))).toBe(true);
    const theme = (await callTool('get_theme', { brand: '#3355aa', ai: '#7c3aed' }))
      .structuredContent as {
      css: string;
    };
    expect(theme.css).toContain('--mn-color-accent-solid');
    expect(theme.css).toContain('--mn-color-ai');
  });
});

describe('monarch mcp over stdio', () => {
  it('answers newline-delimited JSON-RPC', async () => {
    const cli = join(here, '../src/cli.ts');
    const child = spawn(process.execPath, ['--import', 'tsx', cli, 'mcp', '--manifest', MANIFEST], {
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    let out = '';
    child.stdout.on('data', (d: Buffer) => (out += d.toString()));
    child.stdin.write(
      JSON.stringify({
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: { protocolVersion: '2025-06-18' },
      }) + '\n',
    );
    child.stdin.write(
      JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) + '\n',
    );
    child.stdin.write(JSON.stringify({ jsonrpc: '2.0', id: 2, method: 'tools/list' }) + '\n');
    child.stdin.end();
    await new Promise((resolve) => child.on('close', resolve));
    const lines = out
      .trim()
      .split('\n')
      .map((l) => JSON.parse(l) as { id: number });
    expect(lines.map((l) => l.id)).toEqual([1, 2]);
  }, 30_000);
});
