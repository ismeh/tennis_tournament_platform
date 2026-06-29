import { alpha3ToAlpha2 } from './country-flag.util';

describe('alpha3ToAlpha2', () => {
  it('converts valid alpha-3 code to alpha-2', () => {
    expect(alpha3ToAlpha2('ESP')).toBe('ES');
    expect(alpha3ToAlpha2('USA')).toBe('US');
    expect(alpha3ToAlpha2('FRA')).toBe('FR');
    expect(alpha3ToAlpha2('GBR')).toBe('GB');
  });

  it('converts lowercase input to alpha-2', () => {
    expect(alpha3ToAlpha2('esp')).toBe('ES');
    expect(alpha3ToAlpha2('usa')).toBe('US');
  });

  it('returns null for null input', () => {
    expect(alpha3ToAlpha2(null)).toBeNull();
  });

  it('returns null for undefined input', () => {
    expect(alpha3ToAlpha2(undefined)).toBeNull();
  });

  it('returns null for empty string', () => {
    expect(alpha3ToAlpha2('')).toBeNull();
  });

  it('returns null for unknown code', () => {
    expect(alpha3ToAlpha2('XXX')).toBeNull();
    expect(alpha3ToAlpha2('ZZZ')).toBeNull();
  });
});
