import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { GlobalErrorHandler } from './global-error-handler';
import { ToastService } from './toast.service';

describe('GlobalErrorHandler', () => {
  let handler: GlobalErrorHandler;
  let toastService: jasmine.SpyObj<ToastService>;

  function setup(platformId: string) {
    toastService = jasmine.createSpyObj('ToastService', ['showError']);

    TestBed.configureTestingModule({
      providers: [
        GlobalErrorHandler,
        { provide: ToastService, useValue: toastService },
        { provide: PLATFORM_ID, useValue: platformId }
      ]
    });

    handler = TestBed.inject(GlobalErrorHandler);
  }

  describe('in browser', () => {
    beforeEach(() => {
      setup('browser');
    });

    it('shows toast with Error message', () => {
      handler.handleError(new Error('Something broke'));
      expect(toastService.showError).toHaveBeenCalledWith('Something broke');
    });

    it('shows toast with string error', () => {
      handler.handleError('Simple error string');
      expect(toastService.showError).toHaveBeenCalledWith('Simple error string');
    });

    it('shows toast with object that has message property', () => {
      handler.handleError({ message: 'Object error', code: 42 });
      expect(toastService.showError).toHaveBeenCalledWith('Object error');
    });

    it('shows toast with unknown object via String()', () => {
      handler.handleError(42);
      expect(toastService.showError).toHaveBeenCalledWith('42');
    });

    it('shows toast with null/undefined using fallback', () => {
      handler.handleError(null);
      expect(toastService.showError).toHaveBeenCalledWith('Ocurrió un error inesperado en la aplicación.');

      toastService.showError.calls.reset();

      handler.handleError(undefined);
      expect(toastService.showError).toHaveBeenCalledWith('Ocurrió un error inesperado en la aplicación.');
    });

    it('shows toast with falsy object with message property', () => {
      handler.handleError({ message: '' });
      expect(toastService.showError).toHaveBeenCalledWith('');
    });
  });

  describe('in server (SSR)', () => {
    beforeEach(() => {
      setup('server');
    });

    it('does not show toast during SSR', () => {
      handler.handleError(new Error('SSR error'));
      expect(toastService.showError).not.toHaveBeenCalled();
    });
  });
});
