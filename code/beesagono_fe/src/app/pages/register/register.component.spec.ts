import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../services/auth/auth.service';
import { ToastService } from '../../services/toast/toast.service';
import { LoginResponse, RegisterResponse } from '../../models/auth/auth-dto.model';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let router: Router;

  let mockAuthService: {
    register: ReturnType<typeof vi.fn>;
    login: ReturnType<typeof vi.fn>;
    setSession: ReturnType<typeof vi.fn>;
  };

  let mockToastService: {
    show: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    mockAuthService = {
      register: vi.fn(),
      login: vi.fn(),
      setSession: vi.fn(),
    };

    mockToastService = {
      show: vi.fn(),
      clear: vi.fn(),
    };

    vi.spyOn(Storage.prototype, 'setItem');
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ToastService, useValue: mockToastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);

    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization and Validation', () => {
    it('should initialize an empty form with invalid status on init', () => {
      expect(component.registerForm).toBeDefined();
      expect(component.registerForm.valid).toBe(false);
    });

    it('should validate username minlength and maxlength rules', () => {
      const usernameControl = component.registerForm.get('username')!;

      usernameControl.setValue('abc');
      expect(usernameControl.hasError('minlength')).toBe(true);

      usernameControl.setValue('a'.repeat(51));
      expect(usernameControl.hasError('maxlength')).toBe(true);

      usernameControl.setValue('validUser');
      expect(usernameControl.valid).toBe(true);
    });

    it('should validate email format', () => {
      const emailControl = component.registerForm.get('email')!;

      emailControl.setValue('invalid-email');
      expect(emailControl.hasError('email')).toBe(true);

      emailControl.setValue('test@example.com');
      expect(emailControl.valid).toBe(true);
    });

    it('should validate password minlength rule', () => {
      const passwordControl = component.registerForm.get('password')!;

      passwordControl.setValue('1234567');
      expect(passwordControl.hasError('minlength')).toBe(true);

      passwordControl.setValue('Password123!');
      expect(passwordControl.valid).toBe(true);
    });

    it('should trigger passwordMismatch validator when passwords do not match', () => {
      component.registerForm.patchValue({
        password: 'Password123!',
        confirmPassword: 'DifferentPassword123!',
      });

      expect(component.registerForm.hasError('passwordMismatch')).toBe(true);
      expect(component.registerForm.valid).toBe(false);
    });

    it('should be valid when all fields are correct and passwords match', () => {
      component.registerForm.patchValue({
        username: 'testuser',
        email: 'test@example.com',
        password: 'Password123!',
        confirmPassword: 'Password123!',
      });

      expect(component.registerForm.hasError('passwordMismatch')).toBe(false);
      expect(component.registerForm.valid).toBe(true);
    });
  });

  describe('onRegister', () => {
    const validFormValues = {
      username: 'testuser',
      email: 'test@example.com',
      password: 'Password123!',
      confirmPassword: 'Password123!',
    };

    const mockRegisterResponse: RegisterResponse = {
      id: 'user-123',
      username: 'testuser',
      email: 'test@example.com',
      role: 'USER',
      message: 'User registered successfully',
    };

    const mockLoginResponse: LoginResponse = {
      accessToken: 'access-token-123',
      refreshToken: 'refresh-token-123',
      tokenType: 'Bearer',
      id: 'user-123',
      username: 'testuser',
      email: 'test@example.com',
      role: 'USER',
    };

    it('should execute registration, automatic login, delegate session storage to AuthService and navigate on success', () => {
      component.registerForm.setValue(validFormValues);

      mockAuthService.register.mockReturnValue(of(mockRegisterResponse));
      mockAuthService.login.mockReturnValue(of(mockLoginResponse));

      component.onRegister();

      expect(component.isLoading()).toBe(false);
      expect(mockAuthService.register).toHaveBeenCalledWith({
        username: 'testuser',
        email: 'test@example.com',
        password: 'Password123!',
      });

      expect(mockToastService.show).toHaveBeenCalledWith(
        'Account creato! Accesso in corso...',
        { classname: 'bg-success text-white' }
      );

      expect(mockAuthService.login).toHaveBeenCalledWith({
        usernameOrEmail: 'testuser',
        password: 'Password123!',
      });

      // Verify that the component called setSession with the returned LoginResponse// Verifichiamo che il componente abbia chiamato setSession con il LoginResponse restituito
      expect(mockAuthService.setSession).toHaveBeenCalledWith(mockLoginResponse);

      expect(mockToastService.show).toHaveBeenCalledWith(
        'Benvenuto/a su Beesagono, testuser!',
        { classname: 'bg-success text-white' }
      );

      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should handle 409 conflict error when username or email is already taken', () => {
      component.registerForm.setValue(validFormValues);

      mockAuthService.register.mockReturnValue(
        throwError(() => ({ status: 409 }))
      );

      component.onRegister();

      expect(component.isLoading()).toBe(false);
      expect(mockToastService.show).toHaveBeenCalledWith(
        'Nome utente o email già in uso.',
        { classname: 'bg-danger text-white' }
      );
    });

    it('should handle generic error when registration fails', () => {
      component.registerForm.setValue(validFormValues);

      mockAuthService.register.mockReturnValue(
        throwError(() => ({ status: 500 }))
      );

      component.onRegister();

      expect(component.isLoading()).toBe(false);
      expect(mockToastService.show).toHaveBeenCalledWith(
        "Registrazione non riuscita. L'email o il nome utente potrebbero già esistere.",
        { classname: 'bg-danger text-white' }
      );
    });
  });
});