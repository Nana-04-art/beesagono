import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../toast/toast.service';
import { GoogleCheckResponse } from '../../models/auth/auth-dto.model';

declare const google: any;

@Injectable({
  providedIn: 'root'
})
export class GoogleAuthService {
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  // Holds pending registration data when user is not found
  pendingGoogleUser = signal<GoogleCheckResponse | null>(null);
  pendingIdToken = signal<string>('');
  selectedUsername = signal<string>('');
  isLoading = signal<boolean>(false);

  initializeGoogleButton(elementId: string, clientId: string): void {
    if (typeof google !== 'undefined' && google.accounts) {
      google.accounts.id.initialize({
        client_id: clientId,
        callback: (response: any) => this.handleGoogleCredentialResponse(response.credential)
      });

      const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
      const element = document.getElementById(elementId);

      if (element) {
        google.accounts.id.renderButton(element, {
          theme: isDark ? 'filled_black' : 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          width: 320
        });
      }
    }
  }

  handleGoogleCredentialResponse(idToken: string): void {
    this.isLoading.set(true);
    this.pendingIdToken.set(idToken);

    this.authService.checkGoogleUser({ idToken }).subscribe({
      next: (response) => {
        this.isLoading.set(false);

        if (response.registered && response.loginResponse) {
          // Use the centralized method for the session
          this.authService.setSession(response.loginResponse);

          this.toast.show(`Bentornato, ${response.loginResponse.username}!`, {
            classname: 'bg-success text-white'
          });
          this.router.navigate(['/']);
        } else {
          this.pendingGoogleUser.set(response);
          this.selectedUsername.set(response.suggestedUsername || '');
        }
      },
      error: () => {
        this.isLoading.set(false);
        this.toast.show('Autenticazione con Google fallita. Riprova.', {
          classname: 'bg-danger text-white'
        });
      }
    });
  }

  completeGoogleRegistration(): void {
    const idToken = this.pendingIdToken();
    const username = this.selectedUsername();

    if (!idToken || !username.trim()) {
      this.toast.show('Inserisci un nome utente valido.', {
        classname: 'bg-warning text-dark'
      });
      return;
    }

    this.isLoading.set(true);

    this.authService.registerGoogleUser({ idToken, username }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.clearPendingState();

        // The session is already set by the tap() pipe inside AuthService.registerGoogleUser
        this.toast.show(`Benvenuto su Beesagono, ${response.username}!`, {
          classname: 'bg-success text-white'
        });
        this.router.navigate(['/']);
      },
      error: (error) => {
        this.isLoading.set(false);
        let message = 'Registrazione fallita. Lo username potrebbe essere già in uso.';
        if (error.status === 409) {
          message = 'Questo username è già stato preso.';
        }
        this.toast.show(message, {
          classname: 'bg-danger text-white'
        });
      }
    });
  }

  cancelGoogleRegistration(): void {
    this.clearPendingState();
  }

  private clearPendingState(): void {
    this.pendingGoogleUser.set(null);
    this.pendingIdToken.set('');
    this.selectedUsername.set('');
  }
}