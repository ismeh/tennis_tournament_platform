import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PLATFORM_ID } from '@angular/core';
import { of, BehaviorSubject } from 'rxjs';
import { HomeComponent } from './home';
import { AuthService } from '../core/auth/auth.service';
import { UserRole } from '../core/auth/auth.model';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let roleSubject: BehaviorSubject<UserRole | null>;

  beforeEach(async () => {
    roleSubject = new BehaviorSubject<UserRole | null>(null);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUserEmail', 'logout'], {
      role$: roleSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', component: HomeComponent }]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have carousel images', () => {
    expect(component.carouselImages.length).toBe(4);
  });

  it('should start at slide 0', () => {
    expect(component.currentSlide).toBe(0);
  });

  it('should start with progress 0', () => {
    expect(component.progress).toBe(0);
  });

  describe('nextSlide', () => {
    it('should advance to next slide', () => {
      component.nextSlide();
      expect(component.currentSlide).toBe(1);
    });

    it('should wrap around to first slide', () => {
      component.currentSlide = component.carouselImages.length - 1;
      component.nextSlide();
      expect(component.currentSlide).toBe(0);
    });

    it('should reset progress', () => {
      component.progress = 50;
      component.nextSlide();
      expect(component.progress).toBe(0);
    });
  });

  describe('prevSlide', () => {
    it('should go to previous slide', () => {
      component.currentSlide = 2;
      component.prevSlide();
      expect(component.currentSlide).toBe(1);
    });

    it('should wrap around to last slide', () => {
      component.currentSlide = 0;
      component.prevSlide();
      expect(component.currentSlide).toBe(component.carouselImages.length - 1);
    });

    it('should reset progress', () => {
      component.progress = 75;
      component.prevSlide();
      expect(component.progress).toBe(0);
    });
  });

  describe('goToSlide', () => {
    it('should go to specified slide', () => {
      component.goToSlide(2);
      expect(component.currentSlide).toBe(2);
    });

    it('should reset progress', () => {
      component.progress = 60;
      component.goToSlide(1);
      expect(component.progress).toBe(0);
    });
  });

  describe('onCarouselHover', () => {
    it('should set isPaused to true on hover', () => {
      component.onCarouselHover(true);
      expect(component.isPaused).toBeTrue();
    });

    it('should set isPaused to false on leave', () => {
      component.isPaused = true;
      component.onCarouselHover(false);
      expect(component.isPaused).toBeFalse();
    });
  });

  describe('role$ observable', () => {
    it('should expose role from auth service', () => {
      let role: string | null = null;
      component.role$.subscribe(r => role = r);
      expect(role).toBeNull();
    });

    it('should emit ORGANIZER role', () => {
      let role: any = null;
      component.role$.subscribe(r => role = r);
      roleSubject.next('ORGANIZER');
      expect(role).toBe('ORGANIZER');
    });
  });

  describe('settings', () => {
    it('should expose AppSettings with PROJECT_NAME', () => {
      expect(component.settings.PROJECT_NAME).toBeDefined();
    });
  });

  describe('ngOnDestroy', () => {
    it('should not throw on destroy', () => {
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });
});
