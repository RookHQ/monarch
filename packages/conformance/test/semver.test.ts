import { describe, expect, it } from 'vitest';
import { compare, format, minVersion, parseVersion, satisfies } from '../src/semver.js';

describe('semver', () => {
  it.each([
    ['15.2.0', '^15.2.0', true],
    ['16.0.0', '^15.2.0', false],
    ['0.2.5', '^0.2.3', true],
    ['0.3.0', '^0.2.3', false],
    ['0.0.4', '^0.0.3', false],
    ['1.2.9', '~1.2.3', true],
    ['1.3.0', '~1.2.3', false],
    ['18.3.1', '>=18.3 <20', true],
    ['19.2.0', '>=18.3 <20', true],
    ['18.2.0', '>=18.3 <20', false],
    ['20.0.0', '>=18.3 <20', false],
    ['3.4.19', '>=3.4 <4', true],
    ['4.0.0', '>=3.4 <4', false],
    ['1.5.0', '1.x', true],
    ['2.0.0', '1.x || 3.x', false],
    ['3.1.0', '1.x || 3.x', true],
    ['1.2.3', '*', true],
    ['1.2.3', '1.0.0 - 1.2', true],
    ['1.3.0', '1.0.0 - 1.2', false],
    ['1.2.3', '>1.2', false],
    ['1.3.0', '>1.2', true],
    ['1.2.0-rc.1', '>=1.1.0', false],
    ['1.2.0-rc.2', '>=1.2.0-rc.1', true],
    ['1.2.0', '>= 1.1', true],
  ])('%s satisfies %s → %s', (v, r, expected) => {
    expect(satisfies(v, r)).toBe(expected);
  });

  it('orders prereleases before releases', () => {
    const p = (s: string) => parseVersion(s)!;
    expect(compare(p('1.0.0-alpha'), p('1.0.0'))).toBe(-1);
    expect(compare(p('1.0.0-alpha.2'), p('1.0.0-alpha.10'))).toBe(-1);
    expect(compare(p('1.0.0-beta'), p('1.0.0-alpha'))).toBe(1);
  });

  it.each([
    ['^15.2.0', '15.2.0'],
    ['^4', '4.0.0'],
    ['>=18.3 <20', '18.3.0'],
    ['~5.7.0', '5.7.0'],
    ['1.x || ^0.5.1', '0.5.1'],
    ['16.4.2', '16.4.2'],
  ])('minVersion(%s) = %s', (r, v) => {
    expect(format(minVersion(r)!)).toBe(v);
  });

  it('returns null for specs it cannot understand', () => {
    expect(minVersion('workspace:*')).toBeNull();
    expect(minVersion('github:RookHQ/monarch')).toBeNull();
    expect(satisfies('1.0.0', 'latest')).toBe(false);
  });
});
