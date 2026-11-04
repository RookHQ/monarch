import type { CheckResult } from './check.js';
import { RULES, type RuleId } from './rules.js';
import type { Severity } from './types.js';

const ORDER: Severity[] = ['error', 'warn', 'info'];

export function toJson(result: CheckResult): string {
  return JSON.stringify(result, null, 2) + '\n';
}

export function toText(result: CheckResult, opts: { maxPerRule?: number } = {}): string {
  const max = opts.maxPerRule ?? 5;
  const out: string[] = [];
  const c = result.counts;
  out.push(
    `Monarch conformance ${result.tool.version}${result.product ? ` · ${result.product}` : ''}`,
    `${result.root}${result.git.commit ? ` @ ${result.git.commit.slice(0, 12)}${result.git.dirty ? ' (dirty)' : ''}` : ''}`,
    `${result.filesScanned} files, ${result.linesScanned.toLocaleString('en-US')} lines scanned`,
    '',
  );
  const rules = Object.keys(c.byRule).sort((a, b) => {
    const sa = result.findings.find((f) => f.rule === a)?.severity ?? 'info';
    const sb = result.findings.find((f) => f.rule === b)?.severity ?? 'info';
    return ORDER.indexOf(sa) - ORDER.indexOf(sb) || (c.byRule[b] ?? 0) - (c.byRule[a] ?? 0);
  });
  for (const rule of rules) {
    const list = result.findings.filter((f) => f.rule === rule);
    const fresh = list.filter((f) => !f.baselined).length;
    const sev = list[0]?.severity ?? 'info';
    const meta = RULES[rule as RuleId];
    out.push(
      `${sev.toUpperCase().padEnd(5)} ${rule}  ${list.length}${result.baseline ? ` (${fresh} new)` : ''}  ${result.densityPer1kLines[rule] ?? 0}/1k lines`,
    );
    if (meta) out.push(`      ${meta.summary}. ${meta.help}`);
    const show = (result.baseline ? list.filter((f) => !f.baselined) : list).slice(0, max);
    for (const f of show) out.push(`      ${f.file}:${f.line}:${f.column}  ${f.message}`);
    const rest = (result.baseline ? fresh : list.length) - show.length;
    if (rest > 0) out.push(`      … ${rest} more`);
    out.push('');
  }
  if (result.exceptions.length) {
    out.push('Exceptions');
    for (const e of result.exceptions) {
      out.push(
        `  ${e.status.padEnd(7)} ${e.rule} on ${e.paths.join(', ')} · owner ${e.owner} · expires ${e.expires} · ${e.suppressed} suppressed`,
      );
    }
    out.push('');
  }
  out.push(
    `Totals: ${c.bySeverity.error} errors, ${c.bySeverity.warn} warnings, ${c.bySeverity.info} info` +
      (result.baseline ? ` · new vs baseline: ${c.new.error} errors, ${c.new.warn} warnings` : ''),
  );
  if (result.suppressed.inline || result.suppressed.exceptions) {
    out.push(
      `Suppressed: ${result.suppressed.inline} inline, ${result.suppressed.exceptions} by registry exceptions`,
    );
  }
  if (result.baseline?.improved) {
    out.push(
      `${result.baseline.improved} file/rule groups are below baseline; run --write-baseline to lock in the improvement.`,
    );
  }
  out.push(result.exitCode === 0 ? 'Result: pass' : 'Result: fail (new error-severity findings)');
  return out.join('\n') + '\n';
}

const LEVEL: Record<Severity, 'error' | 'warning' | 'note'> = {
  error: 'error',
  warn: 'warning',
  info: 'note',
};

/** SARIF 2.1.0 for GitHub code scanning (upload-sarif). */
export function toSarif(result: CheckResult): string {
  const usedRules = [...new Set(result.findings.map((f) => f.rule))].sort();
  const sarif = {
    $schema: 'https://json.schemastore.org/sarif-2.1.0.json',
    version: '2.1.0',
    runs: [
      {
        tool: {
          driver: {
            name: 'monarch',
            fullName: result.tool.name,
            version: result.tool.version,
            semanticVersion: result.tool.version,
            informationUri:
              'https://github.com/RookHQ/monarch/blob/main/docs/guides/conformance.md',
            rules: usedRules.map((id) => {
              const meta = RULES[id as RuleId];
              return {
                id,
                name: id.replace(/[/-](\w)/g, (_m, ch: string) => ch.toUpperCase()),
                shortDescription: { text: meta?.summary ?? id },
                fullDescription: { text: meta?.help ?? id },
                help: { text: meta?.help ?? id },
                defaultConfiguration: {
                  level: LEVEL[(meta?.severity as Severity | undefined) ?? 'warn'],
                },
              };
            }),
          },
        },
        ...(result.git.commit
          ? {
              versionControlProvenance: [
                { repositoryUri: result.root, revisionId: result.git.commit },
              ],
            }
          : {}),
        results: result.findings.map((f) => ({
          ruleId: f.rule,
          ruleIndex: usedRules.indexOf(f.rule),
          level: LEVEL[f.severity],
          message: { text: f.message },
          baselineState: result.baseline ? (f.baselined ? 'unchanged' : 'new') : undefined,
          locations: [
            {
              physicalLocation: {
                artifactLocation: { uri: f.file },
                region: { startLine: f.line, startColumn: f.column, snippet: { text: f.snippet } },
              },
            },
          ],
          partialFingerprints: { 'monarch/v1': fingerprint(`${f.rule}|${f.file}|${f.snippet}`) },
        })),
      },
    ],
  };
  return JSON.stringify(sarif, null, 2) + '\n';
}

/** FNV-1a: stable across line moves, so code scanning doesn't reopen alerts. */
function fingerprint(s: string): string {
  let h = 0x811c9dc5;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 0x01000193) >>> 0;
  }
  return h.toString(16).padStart(8, '0');
}
