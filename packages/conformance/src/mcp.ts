/**
 * `monarch mcp`: a Model Context Protocol server over stdio, so coding agents
 * (Claude Code, Cursor, VS Code) can look Monarch up instead of guessing.
 *
 * JSON-RPC 2.0, one message per line (the MCP stdio transport), implemented
 * here directly so the package keeps no runtime dependencies. Methods:
 * initialize, ping, tools/list, tools/call; notifications are accepted and
 * never answered.
 */
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, join } from 'node:path';
import { createInterface } from 'node:readline';
import { planAdd } from './add.js';
import { check } from './check.js';
import { loadManifest, scopeOf, summaryOf, type Manifest } from './manifest.js';
import { loadDeprecations } from './registry.js';
import { ruleInfo } from './report.js';
import { search } from './search.js';
import { flattenTokens, loadTokensModule, readTokensJson } from './tokens-bridge.js';
import { VERSION } from './version.js';

export const PROTOCOL_VERSIONS = ['2025-06-18', '2025-03-26', '2024-11-05'] as const;

type Json = null | boolean | number | string | Json[] | { [key: string]: Json };
type Params = Record<string, unknown>;

export interface JsonRpcResponse {
  readonly jsonrpc: '2.0';
  readonly id: string | number | null;
  readonly result?: unknown;
  readonly error?: { readonly code: number; readonly message: string };
}

interface Tool {
  readonly name: string;
  readonly description: string;
  readonly inputSchema: Record<string, unknown>;
  run(args: Params): Promise<unknown> | unknown;
}

class InvalidParams extends Error {}

const str = (args: Params, key: string, required = false): string | undefined => {
  const v = args[key];
  if (v === undefined || v === null) {
    if (required) throw new InvalidParams(`"${key}" is required`);
    return undefined;
  }
  if (typeof v !== 'string') throw new InvalidParams(`"${key}" must be a string`);
  return v;
};

const num = (args: Params, key: string): number | undefined => {
  const v = args[key];
  if (v === undefined) return undefined;
  if (typeof v !== 'number' || !Number.isFinite(v))
    throw new InvalidParams(`"${key}" must be a number`);
  return v;
};

export interface McpContext {
  /** The app the server answers for: its manifest, tokens and files. */
  readonly root: string;
  readonly manifestPath?: string;
}

export function createTools(ctx: McpContext): Tool[] {
  let cached: Manifest | undefined;
  const manifest = () => (cached ??= loadManifest(ctx.root, ctx.manifestPath).manifest);
  return [
    {
      name: 'search_components',
      description:
        'Search Monarch React exports by name or purpose ("date picker", "confirm dialog"). General components rank above Rook-specific ones. Use before writing any UI primitive.',
      inputSchema: {
        type: 'object',
        properties: {
          query: { type: 'string', description: 'Words to search for.' },
          limit: { type: 'number', description: 'Maximum results (default 10).' },
          componentsOnly: { type: 'boolean', description: 'Leave out hooks and utilities.' },
        },
        required: ['query'],
      },
      run: (args) =>
        search(manifest(), str(args, 'query', true) as string, {
          limit: num(args, 'limit') ?? 10,
          componentsOnly: args.componentsOnly === true,
        }).map(({ export: e }) => ({
          name: e.name,
          entry: e.entry,
          kind: e.kind,
          scope: scopeOf(e),
          summary: summaryOf(e.description),
          ...(e.docs ? { docs: e.docs } : {}),
        })),
    },
    {
      name: 'get_component',
      description:
        'Everything needed to use one Monarch export: props with types, defaults and allowed values, the import line, the stylesheets, a minimal example, related exports, accessibility notes and the docs page.',
      inputSchema: {
        type: 'object',
        properties: { name: { type: 'string', description: 'Export name, such as Select.' } },
        required: ['name'],
      },
      run: (args) => {
        const plan = planAdd(manifest(), str(args, 'name', true) as string);
        const e = plan.export;
        return {
          name: e.name,
          kind: e.kind,
          entry: e.entry,
          scope: scopeOf(e),
          description: e.description,
          import: plan.import,
          css: plan.css,
          ...(e.componentCss ? { componentCss: e.componentCss } : {}),
          ...(e.className ? { className: e.className } : {}),
          usesClient: e.usesClient ?? e.clientOnly,
          example: plan.example,
          props: e.props,
          related: plan.siblings,
          accessibility:
            'Follow the accessibility section of the docs page. Icon-only controls need a label, inputs sit in a Field with a FieldLabel, every Dialog has a DialogTitle.',
          ...(plan.docs ? { docs: plan.docs, markdown: `${plan.docs}index.md` } : {}),
          notes: plan.notes,
        };
      },
    },
    {
      name: 'list_tokens',
      description:
        'Monarch design tokens (tokens.json of the installed @rookcom/monarch-tokens): font, space, radius, duration and per-theme colours. Filter by a path prefix such as "themes.light.color".',
      inputSchema: {
        type: 'object',
        properties: {
          prefix: { type: 'string', description: 'Token path prefix.' },
          limit: { type: 'number', description: 'Maximum rows (default 200).' },
        },
      },
      run: (args) => {
        const rows = flattenTokens(readTokensJson(ctx.root), str(args, 'prefix') ?? '');
        const limit = num(args, 'limit') ?? 200;
        return { total: rows.length, tokens: rows.slice(0, limit) };
      },
    },
    {
      name: 'get_theme',
      description:
        "Derive an accessible theme from one brand colour with deriveTheme (light, dark and both more-contrast modes), optionally with an AI colour (the `ai` option), and the CSS to load. Uses the app's @rookcom/monarch-tokens.",
      inputSchema: {
        type: 'object',
        properties: {
          brand: { type: 'string', description: 'Brand colour, #rrggbb or #rgb.' },
          ai: { type: 'string', description: 'Optional AI colour, #rrggbb.' },
          selector: { type: 'string', description: 'CSS selector for the theme (default :root).' },
        },
        required: ['brand'],
      },
      run: async (args) => {
        const tokens = await loadTokensModule(ctx.root);
        const ai = str(args, 'ai');
        const theme = tokens.deriveTheme(str(args, 'brand', true) as string, ai ? { ai } : {});
        const selector = str(args, 'selector');
        return {
          theme,
          css: tokens.brandThemeCss(theme, selector ? { selector } : {}),
          usage:
            'Write the css to a file at build time and import it after @rookcom/monarch-tokens/css.',
        };
      },
    },
    {
      name: 'check_code',
      description:
        'Run Monarch conformance on a code snippet or on a file or folder of the app. Returns findings with help, docs and a machine-applicable fix where one is safe.',
      inputSchema: {
        type: 'object',
        properties: {
          code: { type: 'string', description: 'Source to check.' },
          filename: {
            type: 'string',
            description:
              'File name for the snippet, which sets the language (default snippet.tsx).',
          },
          path: { type: 'string', description: 'Or: a file or folder, relative to the app root.' },
        },
      },
      run: (args) => {
        const code = str(args, 'code');
        const path = str(args, 'path');
        if (code === undefined && path === undefined)
          throw new InvalidParams('pass "code" or "path"');
        const deprecations = loadDeprecations();
        if (code !== undefined) {
          const dir = mkdtempSync(join(tmpdir(), 'monarch-mcp-'));
          try {
            const name = basename(str(args, 'filename') ?? 'snippet.tsx');
            writeFileSync(join(dir, name), code);
            return findingsOf(check({ root: dir, include: [name], deprecations }));
          } finally {
            rmSync(dir, { recursive: true, force: true });
          }
        }
        return findingsOf(check({ root: ctx.root, include: [path as string], deprecations }));
      },
    },
  ];
}

function findingsOf(result: ReturnType<typeof check>) {
  return {
    filesScanned: result.filesScanned,
    counts: result.counts.bySeverity,
    findings: result.findings.map((f) => ({
      rule: f.rule,
      severity: f.severity,
      file: f.file,
      line: f.line,
      column: f.column,
      message: f.message,
      ...ruleInfo(f.rule),
      ...(f.fix ? { fix: f.fix } : {}),
    })),
  };
}

const ok = (id: JsonRpcResponse['id'], result: unknown): JsonRpcResponse => ({
  jsonrpc: '2.0',
  id,
  result,
});
const fail = (id: JsonRpcResponse['id'], code: number, message: string): JsonRpcResponse => ({
  jsonrpc: '2.0',
  id,
  error: { code, message },
});

/** Handles one parsed message. Returns null for notifications. */
export async function handleMessage(
  tools: readonly Tool[],
  message: unknown,
): Promise<JsonRpcResponse | null> {
  if (typeof message !== 'object' || message === null || Array.isArray(message))
    return fail(null, -32600, 'Invalid request');
  const msg = message as { jsonrpc?: unknown; id?: unknown; method?: unknown; params?: unknown };
  const isNotification = !('id' in msg);
  const id = typeof msg.id === 'string' || typeof msg.id === 'number' ? msg.id : null;
  if (msg.jsonrpc !== '2.0' || typeof msg.method !== 'string')
    return isNotification ? null : fail(id, -32600, 'Invalid request');
  const params = (
    typeof msg.params === 'object' && msg.params !== null ? msg.params : {}
  ) as Params;
  if (isNotification) return null;
  switch (msg.method) {
    case 'initialize': {
      const asked = typeof params.protocolVersion === 'string' ? params.protocolVersion : '';
      const protocolVersion = (PROTOCOL_VERSIONS as readonly string[]).includes(asked)
        ? asked
        : PROTOCOL_VERSIONS[0];
      return ok(id, {
        protocolVersion,
        capabilities: { tools: { listChanged: false } },
        serverInfo: { name: 'monarch', title: 'Monarch design system', version: VERSION },
        instructions:
          'Use search_components before writing UI, get_component for props, imports and CSS, and check_code before finishing.',
      });
    }
    case 'ping':
      return ok(id, {});
    case 'tools/list':
      return ok(id, {
        tools: tools.map(({ name, description, inputSchema }) => ({
          name,
          description,
          inputSchema,
        })),
      });
    case 'tools/call': {
      const tool = tools.find((t) => t.name === params.name);
      if (!tool) return fail(id, -32602, `Unknown tool ${String(params.name)}`);
      const args = (
        typeof params.arguments === 'object' && params.arguments !== null ? params.arguments : {}
      ) as Params;
      try {
        const result = (await tool.run(args)) as Json;
        return ok(id, {
          content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          structuredContent: Array.isArray(result) ? { results: result } : result,
          isError: false,
        });
      } catch (e) {
        if (e instanceof InvalidParams) return fail(id, -32602, e.message);
        // Tool errors go back to the model as content, so it can recover (MCP spec).
        return ok(id, {
          content: [{ type: 'text', text: (e as Error).message }],
          isError: true,
        });
      }
    }
    default:
      return fail(id, -32601, `Method not found: ${msg.method}`);
  }
}

/** Serves MCP on stdin/stdout until stdin closes. Logs go to stderr only. */
export function runStdio(ctx: McpContext): Promise<void> {
  const tools = createTools(ctx);
  const rl = createInterface({ input: process.stdin, crlfDelay: Infinity });
  const write = (r: JsonRpcResponse | null) => {
    if (r) process.stdout.write(JSON.stringify(r) + '\n');
  };
  let pending = Promise.resolve();
  rl.on('line', (line) => {
    if (!line.trim()) return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(line);
    } catch {
      write(fail(null, -32700, 'Parse error'));
      return;
    }
    // Answer in order; a batch (array) is answered message by message.
    const messages = Array.isArray(parsed) ? parsed : [parsed];
    pending = pending.then(async () => {
      for (const m of messages) write(await handleMessage(tools, m));
    });
  });
  process.stderr.write(`monarch mcp ${VERSION}: serving ${ctx.root} on stdio\n`);
  return new Promise((resolve) => rl.on('close', () => void pending.then(resolve)));
}
