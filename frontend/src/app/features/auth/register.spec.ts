import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router, provideRouter } from '@angular/router';
import { RegisterComponent } from './register';
import { AuthService } from '../../core/auth/auth.service';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
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
    expect(router.navigateByUrl).toHaveBeenCalledWith('/perfil');
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
