// Writes the repository-root Package.swift from platforms/ios/Package.swift, so
// an app can add Monarch for iOS as a Swift Package Manager URL dependency
// (https://github.com/RookHQ/monarch, version 0.14.N). SwiftPM only reads a
// manifest at the root of a repository.
//
//   node scripts/native/emit-root-package.mjs           rewrite Package.swift
//   node scripts/native/emit-root-package.mjs --check   fail if Package.swift is stale
//
// The root manifest has the same products, targets, settings and platforms as
// platforms/ios/Package.swift, with every target path prefixed by platforms/ios/.
// Test targets are left out: consumers never build them, and development and
// testing happen in platforms/ios (Scripts/verify.sh). So is every block between
// `// emit-root-package: begin test-only` and `// emit-root-package: end test-only`.
//
// A target kind this script does not know (binary, plugin, macro, system library,
// executable) fails the run, so a change to the iOS manifest is never copied
// half right. Plain Node, no dependencies.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const root = new URL('../../', import.meta.url);
const sourceFile = new URL('platforms/ios/Package.swift', root);
const targetFile = new URL('Package.swift', root);
const PREFIX = 'platforms/ios';

/** Index of the bracket that closes the one at `open`, skipping strings and comments. */
function closing(text, open, opener, closer) {
  let depth = 0;
  for (let i = open; i < text.length; i++) {
    const c = text[i];
    if (c === '"') {
      i++;
      while (i < text.length && text[i] !== '"') i += text[i] === '\\' ? 2 : 1;
    } else if (c === '/' && text[i + 1] === '/') {
      while (i < text.length && text[i] !== '\n') i++;
    } else if (c === opener) {
      depth++;
    } else if (c === closer) {
      depth--;
      if (depth === 0) return i;
    }
  }
  throw new Error(`unbalanced ${opener}${closer} in platforms/ios/Package.swift`);
}

const closingParen = (text, open) => closing(text, open, '(', ')');
const closingBracket = (text, open) => closing(text, open, '[', ']');

/** Removes the call that starts at `start` (`.testTarget(`), with its trailing comma and line. */
function removeCall(text, start) {
  const open = text.indexOf('(', start);
  let end = closingParen(text, open) + 1;
  if (text[end] === ',') end++;
  const lineStart = text.lastIndexOf('\n', start) + 1;
  const lineEnd = text.indexOf('\n', end);
  const before = text.slice(lineStart, start);
  const after = text.slice(end, lineEnd === -1 ? text.length : lineEnd);
  if (before.trim() === '' && after.trim() === '') {
    return text.slice(0, lineStart) + text.slice(lineEnd === -1 ? text.length : lineEnd + 1);
  }
  return text.slice(0, start) + text.slice(end);
}

export function emitRootPackage(source) {
  const lines = source.split('\n');
  if (!lines[0].startsWith('// swift-tools-version:')) {
    throw new Error('platforms/ios/Package.swift must start with // swift-tools-version:');
  }

  const unknown = source.match(/\.(binaryTarget|plugin|macro|systemLibrary|executableTarget)\s*\(/);
  if (unknown) {
    throw new Error(
      `platforms/ios/Package.swift uses .${unknown[1]}, which emit-root-package.mjs does not translate. Extend the script.`,
    );
  }

  let body = lines.slice(1).join('\n');

  // Drop the blocks marked test-only (for example the conditional snapshot test target).
  body = body.replace(
    /^[ \t]*\/\/ emit-root-package: begin test-only[^\n]*\n[\s\S]*?^[ \t]*\/\/ emit-root-package: end test-only[^\n]*\n?/gm,
    '',
  );
  if (body.includes('emit-root-package: begin') || body.includes('emit-root-package: end')) {
    throw new Error('platforms/ios/Package.swift: unmatched emit-root-package test-only markers');
  }

  // Drop test targets.
  for (
    let at = body.search(/\.testTarget\s*\(/);
    at !== -1;
    at = body.search(/\.testTarget\s*\(/)
  ) {
    body = removeCall(body, at);
  }

  // Prefix or add the path of every remaining target.
  let out = '';
  let rest = body;
  for (let at = rest.search(/\.target\s*\(/); at !== -1; at = rest.search(/\.target\s*\(/)) {
    const open = rest.indexOf('(', at);
    const close = closingParen(rest, open);
    let call = rest.slice(at, close + 1);
    const name = call.match(/name:\s*"([^"]+)"/)?.[1];
    if (!name) throw new Error(`a .target without a literal name: ${call.slice(0, 80)}`);
    if (/\bpath:\s*"/.test(call)) {
      call = call.replace(/\bpath:\s*"([^"]*)"/, (_, p) => `path: "${PREFIX}/${p}"`);
    } else {
      // Swift requires the labels in declaration order: name, dependencies, path, ...
      const space = call.match(/name:\s*"[^"]+",(\s*)/)[1];
      let after = call.indexOf(',', call.search(/name:\s*"/)) + 1;
      const deps = call.search(/\bdependencies:\s*\[/);
      if (deps !== -1) {
        after = call.indexOf(',', closingBracket(call, call.indexOf('[', deps))) + 1;
      }
      if (after === 0) throw new Error(`.target "${name}": no argument follows name/dependencies`);
      call = `${call.slice(0, after)}${space}path: "${PREFIX}/Sources/${name}",${call.slice(after)}`;
    }
    out += rest.slice(0, at) + call;
    rest = rest.slice(close + 1);
  }
  out += rest;

  return [
    lines[0],
    '//',
    '// GENERATED by scripts/native/emit-root-package.mjs from platforms/ios/Package.swift.',
    '// Do not edit: change platforms/ios/Package.swift, then run',
    '// `node scripts/native/emit-root-package.mjs`. `--check` (npm run verify and the',
    '// Native workflow) fails when this file is stale.',
    '//',
    '// This root manifest lets an app add Monarch for iOS by URL. It has no test',
    '// targets; develop and test in platforms/ios.',
    out.replace(/^\n*/, '\n').replace(/\n+$/, '\n'),
  ].join('\n');
}

const isMain = import.meta.url === pathToFileURL(resolve(process.argv[1] ?? '')).href;
if (isMain) {
  const check = process.argv.includes('--check');
  const expected = emitRootPackage(readFileSync(sourceFile, 'utf8'));
  const current = existsSync(targetFile) ? readFileSync(targetFile, 'utf8') : null;
  if (check) {
    if (current !== expected) {
      console.error(
        'emit-root-package: Package.swift is stale. Run `node scripts/native/emit-root-package.mjs`.',
      );
      process.exit(1);
    }
    console.log('emit-root-package: Package.swift matches platforms/ios/Package.swift');
  } else if (current === expected) {
    console.log('emit-root-package: Package.swift is up to date');
  } else {
    writeFileSync(targetFile, expected);
    console.log('emit-root-package: Package.swift written');
  }
}
