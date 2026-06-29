import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { AppConfigService } from './app-config.service';
import { AppSettings } from '../../shared/constants';

describe('AppConfigService', () => {
  let service: AppConfigService;

  function setup(platformId: string) {
    TestBed.configureTestingModule({
      providers: [
        AppConfigService,
        { provide: PLATFORM_ID, useValue: platformId }
      ]
    });

    service = TestBed.inject(AppConfigService);
  }

  describe('in browser', () => {
    beforeEach(() => {
      setup('browser');
    });

    it('loads config from /config.json and sets API_URL', async () => {
      const originalFetch = window.fetch;
      window.fetch = jasmine.createSpy('fetch').and.returnValue(
        Promise.resolve(new Response(JSON.stringify({ apiUrl: 'https://custom.api/api' })))
      );

      await service.load();

      expect(AppSettings.API_URL).toBe('https://custom.api/api');

      window.fetch = originalFetch;
    });

    it('keeps defaults when config.json fails to load', async () => {
      const originalFetch = window.fetch;
      const urlBefore = AppSettings.API_URL;
      window.fetch = jasmine.createSpy('fetch').and.returnValue(
        Promise.reject(new Error('Network error'))
      );

      await service.load();

      expect(AppSettings.API_URL).toBe(urlBefore);

      window.fetch = originalFetch;
    });

    it('keeps defaults when config.json returns non-ok response', async () => {
      const originalFetch = window.fetch;
      const urlBefore = AppSettings.API_URL;
      window.fetch = jasmine.createSpy('fetch').and.returnValue(
        Promise.resolve(new Response('', { status: 404, statusText: 'Not Found' }))
      );

      await service.load();

      expect(AppSettings.API_URL).toBe(urlBefore);

      window.fetch = originalFetch;
    });

    it('returns same promise on multiple calls (idempotent)', async () => {
      const originalFetch = window.fetch;
      window.fetch = jasmine.createSpy('fetch').and.returnValue(
        Promise.resolve(new Response(JSON.stringify({ apiUrl: 'https://api.test/api' })))
      );

      const p1 = service.load();
      const p2 = service.load();

      expect(p1).toBe(p2);

      await p1;
      window.fetch = originalFetch;
    });
  });
});
