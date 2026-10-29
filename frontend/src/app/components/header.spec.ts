import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, BehaviorSubject } from 'rxjs';
import { HeaderComponent } from './header';
import { AuthService } from '../core/auth/auth.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const isLoggedInSubject = new BehaviorSubject<boolean>(false);
  const displayNameSubject = new BehaviorSubject<string | null>(null);
  const roleSubject = new BehaviorSubject<string | null>(null);
  const nationalitySubject = new BehaviorSubject<string | null>(null);

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'getCurrentUserEmail',
      'logout',
      'login',
    ], {
      isLoggedIn$: isLoggedInSubject.asObservable(),
      displayName$: displayNameSubject.asObservable(),
      role$: roleSubject.asObservable(),
      nationality$: nationalitySubject.asObservable(),
    });

    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        provideRouter([{ path: '**', component: HeaderComponent }]),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('getUserInitial', () => {
    it('should return first letter capitalized for valid name', () => {
      expect(component.getUserInitial('Juan')).toBe('J');
    });

    it('should return email prefix when displayName is null', () => {
      authServiceSpy.getCurrentUserEmail.and.returnValue('player@example.com');
      expect(component.getUserInitial(null)).toBe('P');
    });

    it('should return ? when no name available', () => {
      authServiceSpy.getCurrentUserEmail.and.returnValue(null);
      expect(component.getUserInitial(null)).toBe('?');
    });

    it('should return ? for empty string', () => {
      authServiceSpy.getCurrentUserEmail.and.returnValue(null);
      expect(component.getUserInitial('')).toBe('?');
    });
  });

  describe('resolveDisplayName', () => {
    it('should return displayName if provided', () => {
      expect(component.resolveDisplayName('Juan')).toBe('Juan');
    });

    it('should return email prefix if displayName is null', () => {
      authServiceSpy.getCurrentUserEmail.and.returnValue('player@example.com');
      expect(component.resolveDisplayName(null)).toBe('player');
    });

    it('should return Player if no name or email', () => {
      authServiceSpy.getCurrentUserEmail.and.returnValue(null);
      expect(component.resolveDisplayName(null)).toBe('Player');
    });
  });

  describe('loginQueryParams', () => {
    it('should return returnUrl', () => {
      const params = component.loginQueryParams();
      expect(params.returnUrl).toBeDefined();
    });

    it('should return /torneos if on login page', () => {
      const router = TestBed.inject(Router) as any;
      Object.defineProperty(router, 'url', { value: '/login', writable: true, configurable: true });
      const params = component.loginQueryParams();
      expect(params.returnUrl).toBe('/torneos');
    });

    it('should return /torneos if on register page', () => {
      const router = TestBed.inject(Router) as any;
      Object.defineProperty(router, 'url', { value: '/register', writable: true, configurable: true });
      const params = component.loginQueryParams();
      expect(params.returnUrl).toBe('/torneos');
    });

    it('should return /torneos if on confirmar-email page', () => {
      const router = TestBed.inject(Router) as any;
      Object.defineProperty(router, 'url', { value: '/confirmar-email', writable: true, configurable: true });
      const params = component.loginQueryParams();
      expect(params.returnUrl).toBe('/torneos');
    });

    it('should return /torneos if url is empty', () => {
      const router = TestBed.inject(Router) as any;
      Object.defineProperty(router, 'url', { value: '', writable: true, configurable: true });
      const params = component.loginQueryParams();
      expect(params.returnUrl).toBe('/torneos');
    });

    it('should return current url when not on auth pages', () => {
      const params = component.loginQueryParams();
      expect(params.returnUrl).toBeDefined();
    });
  });

  describe('toggleProfileMenu', () => {
    it('should toggle profile menu', () => {
      expect(component.isProfileMenuOpen()).toBeFalse();
      component.toggleProfileMenu();
      expect(component.isProfileMenuOpen()).toBeTrue();
      component.toggleProfileMenu();
      expect(component.isProfileMenuOpen()).toBeFalse();
    });
  });

  describe('closeProfileMenu', () => {
    it('should close profile menu', () => {
      component.toggleProfileMenu();
      component.closeProfileMenu();
      expect(component.isProfileMenuOpen()).toBeFalse();
    });
  });

  describe('toggleMobileMenu', () => {
    it('should toggle mobile menu and close profile', () => {
      expect(component.isMobileMenuOpen()).toBeFalse();
      component.toggleMobileMenu();
      expect(component.isMobileMenuOpen()).toBeTrue();
      expect(component.isProfileMenuOpen()).toBeFalse();
      component.toggleMobileMenu();
      expect(component.isMobileMenuOpen()).toBeFalse();
    });
  });

  describe('closeMobileMenu', () => {
    it('should close mobile menu and profile', () => {
      component.toggleMobileMenu();
      component.closeMobileMenu();
      expect(component.isMobileMenuOpen()).toBeFalse();
      expect(component.isMobileProfileOpen()).toBeFalse();
    });
  });

  describe('toggleMobileProfile', () => {
    it('should toggle mobile profile', () => {
      expect(component.isMobileProfileOpen()).toBeFalse();
      component.toggleMobileProfile();
      expect(component.isMobileProfileOpen()).toBeTrue();
      component.toggleMobileProfile();
      expect(component.isMobileProfileOpen()).toBeFalse();
    });
  });

  describe('onLogout', () => {
    it('should close menus and call auth service logout', () => {
      authServiceSpy.logout.and.returnValue(of(undefined));
      component.onLogout();
      expect(authServiceSpy.logout).toHaveBeenCalled();
      expect(component.isProfileMenuOpen()).toBeFalse();
      expect(component.isMobileMenuOpen()).toBeFalse();
    });
  });

  describe('countryCode signal', () => {
    it('should update countryCode when nationality changes', () => {
      nationalitySubject.next('ESP');
      expect(component.countryCode()).toBeTruthy();
    });

    it('should set null for unknown nationality', () => {
      nationalitySubject.next('XXX');
      expect(component.countryCode()).toBeNull();
    });
  });
});
