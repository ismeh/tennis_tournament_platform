import { AppSettings } from './constants';

describe('AppSettings', () => {
  const originalUrl = AppSettings.API_URL;

  afterEach(() => {
    AppSettings.API_URL = originalUrl;
  });

  it('has correct static values', () => {
    expect(AppSettings.PROJECT_NAME).toBe('PuntoMatch');
    expect(AppSettings.TOKEN_KEY).toBe('access_token');
    expect(AppSettings.REFRESH_TOKEN_KEY).toBe('refresh_token');
    expect(AppSettings.USER_NAME_KEY).toBe('user_display_name');
    expect(AppSettings.USER_ROLE_KEY).toBe('user_role');
    expect(AppSettings.USER_NATIONALITY_KEY).toBe('user_nationality');
  });

  it('configureApiUrl sets API_URL when given a valid string', () => {
    AppSettings.configureApiUrl('https://example.com/api');
    expect(AppSettings.API_URL).toBe('https://example.com/api');
  });

  it('configureApiUrl trims whitespace before setting', () => {
    AppSettings.configureApiUrl('  https://trimmed.com/api  ');
    expect(AppSettings.API_URL).toBe('https://trimmed.com/api');
  });

  it('configureApiUrl does not change API_URL when given empty string', () => {
    const urlBefore = AppSettings.API_URL;
    AppSettings.configureApiUrl('');
    expect(AppSettings.API_URL).toBe(urlBefore);
  });

  it('configureApiUrl does not change API_URL when given null', () => {
    const urlBefore = AppSettings.API_URL;
    AppSettings.configureApiUrl(null);
    expect(AppSettings.API_URL).toBe(urlBefore);
  });

  it('configureApiUrl does not change API_URL when given undefined', () => {
    const urlBefore = AppSettings.API_URL;
    AppSettings.configureApiUrl(undefined);
    expect(AppSettings.API_URL).toBe(urlBefore);
  });

  it('configureApiUrl does not change API_URL when given whitespace only', () => {
    const urlBefore = AppSettings.API_URL;
    AppSettings.configureApiUrl('   ');
    expect(AppSettings.API_URL).toBe(urlBefore);
  });
});
