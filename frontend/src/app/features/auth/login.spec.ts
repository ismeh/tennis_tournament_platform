import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { LoginComponent } from './login';
import { AuthService } from '../../core/auth/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;
  let returnUrl: string | null;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    returnUrl = null;

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => key === 'returnUrl' ? returnUrl : null
              }
            }
          }
        },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  });

  it('calls login and navigates to tournaments on success when there is no return url', () => {
    authServiceSpy.login.and.returnValue(of({ accessToken: 'jwt-token', role: 'PLAYER' }));

    component.form.setValue({ email: 'test@example.com', password: 'secret123' });
    component.submit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({ email: 'test@example.com', password: 'secret123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/torneos');
    expect(component.errorMessage()).toBeNull();
  });

  it('navigates to the previous route after login when return url is present', () => {
    authServiceSpy.login.and.returnValue(of({ accessToken: 'jwt-token', role: 'PLAYER' }));
    returnUrl = '/torneos/tournament-id?tab=inscriptions';

    component.form.setValue({ email: 'test@example.com', password: 'secret123' });
    component.submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/torneos/tournament-id?tab=inscriptions');
  });

  it('shows an error message on login failure', () => {
    authServiceSpy.login.and.returnValue(throwError(() => new Error('Unauthorized')));

    component.form.setValue({ email: 'test@example.com', password: 'secret123' });
    component.submit();

    expect(component.errorMessage()).toContain('No se pudo iniciar sesión');
  });
});
