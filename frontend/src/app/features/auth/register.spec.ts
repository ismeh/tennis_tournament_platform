import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { RegisterComponent } from './register';
import { AuthService } from '../../core/auth/auth.service';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['register']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    routerSpy.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('calls register and navigates to home on success', () => {
    authServiceSpy.register.and.returnValue(of({ accessToken: 'registered-token' }));

    component.form.setValue({
      name: 'New User',
      email: 'new@example.com',
      password: 'secret123'
    });

    component.submit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      name: 'New User',
      email: 'new@example.com',
      password: 'secret123'
    });
    expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/');
    expect(component.errorMessage()).toBeNull();
  });

  it('shows an error message on register failure', () => {
    authServiceSpy.register.and.returnValue(throwError(() => new Error('Conflict')));

    component.form.setValue({
      name: 'New User',
      email: 'new@example.com',
      password: 'secret123'
    });

    component.submit();

    expect(component.errorMessage()).toContain('No se pudo registrar');
  });
});
