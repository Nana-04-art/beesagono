import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { switchMap } from 'rxjs';
import { AuthService } from '../../services/auth/auth.service';
import { ToastService } from '../../services/toast/toast.service';
import { RegisterRequest } from '../../models/auth/auth-dto.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, NgClass, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  isLoading = signal<boolean>(false);
  registerForm!: FormGroup;

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.registerForm = new FormGroup(
      {
        username: new FormControl('', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]),
        email: new FormControl('', [Validators.required, Validators.email]),
        password: new FormControl('', [Validators.required, Validators.minLength(8)]),
        confirmPassword: new FormControl('', [Validators.required])
      },
      { validators: this.passwordMatchValidator }
    );
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    return password && confirmPassword && password.value !== confirmPassword.value
      ? { passwordMismatch: true }
      : null;
  }

  onRegister(): void {
    this.isLoading.set(true);
    const formValues = this.registerForm.getRawValue();

    const requestPayload: RegisterRequest = {
      username: formValues.username,
      email: formValues.email,
      password: formValues.password
    };

    this.authService.register(requestPayload).pipe(
      switchMap(() => {
        this.toast.show('Account creato! Accesso in corso...', {
          classname: 'bg-success text-white'
        });
        return this.authService.login({
          usernameOrEmail: formValues.username,
          password: formValues.password
        });
      })
    ).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.authService.setSession(response);

        this.toast.show(`Benvenuto/a su Beesagono, ${response.username}!`, {
          classname: 'bg-success text-white'
        });
        this.router.navigate(['/']);
      },
      error: (error) => {
        this.isLoading.set(false);
        let message = 'Registrazione non riuscita. L\'email o il nome utente potrebbero già esistere.';

        if (error.status === 409) {
          message = 'Nome utente o email già in uso.';
        }

        this.toast.show(message, {
          classname: 'bg-danger text-white'
        });
      }
    });
  }
}