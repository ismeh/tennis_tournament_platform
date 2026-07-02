import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmEmailComponent } from './confirm-email';
import { AuthService } from '../../core/auth/auth.service';

describe('ConfirmEmailComponent', () => {
  let component: ConfirmEmailComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  function createActivatedRoute(queryParams: Record<string, string>) {
    return {
      snapshot: {
        queryParamMap: {
          get: (key: string) => queryParams[key] ?? null,
        },
      },
    };
  }

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['confirmEmail']);

    await TestBed.configureTestingModule({
      imports: [ConfirmEmailComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ActivatedRoute, useValue: createActivatedRoute({ token: 'valid-token' }) },
      ],
    }).compileComponents();
  });

  it('should create', () => {
    authServiceSpy.confirmEmail.and.returnValue(of({ message: 'Email confirmado', emailVerificationRequired: false }));
    const fixture = TestBed.createComponent(ConfirmEmailComponent);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  describe('when token is valid', () => {
    it('should call confirmEmail and show success message', () => {
      authServiceSpy.confirmEmail.and.returnValue(of({ message: 'Email confirmado correctamente', emailVerificationRequired: false }));
      const fixture = TestBed.createComponent(ConfirmEmailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(authServiceSpy.confirmEmail).toHaveBeenCalledWith('valid-token');
      expect(component.isLoading()).toBeFalse();
      expect(component.isSuccess()).toBeTrue();
      expect(component.message()).toBe('Email confirmado correctamente');
    });
  });

  describe('when token is missing', () => {
    it('should show invalid link message', () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        imports: [ConfirmEmailComponent],
        providers: [
          { provide: AuthService, useValue: authServiceSpy },
          { provide: ActivatedRoute, useValue: createActivatedRoute({}) },
        ],
      });

      const fixture = TestBed.createComponent(ConfirmEmailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.isLoading()).toBeFalse();
      expect(component.isSuccess()).toBeFalse();
      expect(component.message()).toBe('El enlace de confirmación no es válido.');
      expect(authServiceSpy.confirmEmail).not.toHaveBeenCalled();
    });
  });

  describe('when confirmEmail fails', () => {
    it('should show error message', () => {
      authServiceSpy.confirmEmail.and.returnValue(throwError(() => ({ status: 400 })));
      const fixture = TestBed.createComponent(ConfirmEmailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.isLoading()).toBeFalse();
      expect(component.isSuccess()).toBeFalse();
      expect(component.message()).toBeTruthy();
    });
  });

  describe('when confirmEmail returns generic error', () => {
    it('should show default error message', () => {
      authServiceSpy.confirmEmail.and.returnValue(throwError(() => new Error('Network error')));
      const fixture = TestBed.createComponent(ConfirmEmailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.isLoading()).toBeFalse();
      expect(component.isSuccess()).toBeFalse();
    });
  });
});
