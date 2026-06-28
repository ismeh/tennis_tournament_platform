import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';
import { RegisterComponent } from './register';
import { AuthService } from '../../core/auth/auth.service';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

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
    fixture.detectChanges();
  });

  it('calls register and shows confirmation message on success', () => {
    authServiceSpy.register.and.returnValue(of({
      emailVerificationRequired: true,
      message: 'Cuenta creada. Revisa tu correo para confirmar el email.'
    }));

    component.form.setValue({
      email: 'new@example.com',
      password: 'secret123',
      role: 'PLAYER',
      privacyPolicyAccepted: true
    });

    component.submit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      email: 'new@example.com',
      password: 'secret123',
      role: 'PLAYER',
      privacyPolicyAccepted: true
    });
    expect(component.successMessage()).toBe('Cuenta creada. Revisa tu correo para confirmar el email.');
    expect(component.errorMessage()).toBeNull();
  });

  it('shows an error message on register failure', () => {
    authServiceSpy.register.and.returnValue(throwError(() => new Error('Conflict')));

    component.form.setValue({
      email: 'new@example.com',
      password: 'secret123',
      role: 'PLAYER',
      privacyPolicyAccepted: true
    });

    component.submit();

    expect(component.errorMessage()).toContain('No se pudo registrar');
  });
});
