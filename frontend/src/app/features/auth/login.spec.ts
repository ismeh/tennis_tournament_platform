import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router, provideRouter } from '@angular/router';
import { LoginComponent } from './login';
import { AuthService } from '../../core/auth/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  });

  it('calls login and navigates to home on success', () => {
    authServiceSpy.login.and.returnValue(of({ accessToken: 'jwt-token' }));

    component.form.setValue({ email: 'test@example.com', password: 'secret123' });
    component.submit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({ email: 'test@example.com', password: 'secret123' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    expect(component.errorMessage()).toBeNull();
  });

  it('shows an error message on login failure', () => {
    authServiceSpy.login.and.returnValue(throwError(() => new Error('Unauthorized')));

    component.form.setValue({ email: 'test@example.com', password: 'secret123' });
    component.submit();

    expect(component.errorMessage()).toContain('No se pudo iniciar sesion');
  });
});
