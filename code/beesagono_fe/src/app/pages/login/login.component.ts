import {
  Component,
  ElementRef,
  HostListener,
  AfterViewInit,
  ViewChild,
  effect,
  inject,
  signal,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import { GoogleAuthService } from '../../services/auth/google-auth.service';
import { ToastService } from '../../services/toast/toast.service';

declare const google: any;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements AfterViewInit, OnDestroy {
  private readonly authService = inject(AuthService);
  public readonly googleAuth = inject(GoogleAuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  // ViewChild references for modal focus trapping
  @ViewChild('usernameInputRef') usernameInputRef?: ElementRef<HTMLInputElement>;
  @ViewChild('cancelBtnRef') cancelBtnRef?: ElementRef<HTMLButtonElement>;
  @ViewChild('completeBtnRef') completeBtnRef?: ElementRef<HTMLButtonElement>;

  usernameOrEmail = signal('');
  password = signal('');
  isLoading = signal(false);

  private readonly GOOGLE_CLIENT_ID = '362890069300-870q1d3b9nj16i0j19gco6npvcma1hpr.apps.googleusercontent.com';

  // Tracking timer for canceling retries and counting attempts
  private googleRetryTimer: ReturnType<typeof setTimeout> | null = null;
  private googleRetryAttempts = 0;
  private readonly MAX_GOOGLE_RETRY_ATTEMPTS = 15; // Max ~3 seconds

  constructor() {
    // Reactive scrolling and focus management with effect cleanup callback
    effect((onCleanup) => {
      const isPending = !!this.googleAuth.pendingGoogleUser();
      const previousOverflow = document.body.style.overflow;

      if (isPending) {
        document.body.style.overflow = 'hidden';
        this.focusUsernameInput();
      } else {
        document.body.style.overflow = '';
      }

      // Cleanup registered directly in the effect in case of destruction or re-evaluation
      onCleanup(() => {
        document.body.style.overflow = previousOverflow;
      });
    });
  }

  // Use AfterViewInit instead of OnInit to ensure the DOM is completely ready
  ngAfterViewInit(): void {
    this.renderGoogleButton();
  }

  ngOnDestroy(): void {
    // Cancel any pending retries for SDK loading
    if (this.googleRetryTimer !== null) {
      clearTimeout(this.googleRetryTimer);
      this.googleRetryTimer = null;
    }

    // Restore body style
    document.body.style.overflow = '';

    // Clear pending Google registration when navigating away
    if (this.googleAuth.pendingGoogleUser()) {
      this.googleAuth.cancelGoogleRegistration();
    }
  }

  private renderGoogleButton(): void {
    // Render immediately if the Google script is loaded
    if (typeof google !== 'undefined' && google.accounts) {
      this.googleAuth.initializeGoogleButton('googleBtn', this.GOOGLE_CLIENT_ID);
      this.googleRetryAttempts = 0;
    } else if (this.googleRetryAttempts < this.MAX_GOOGLE_RETRY_ATTEMPTS) {
      this.googleRetryAttempts++;
      this.googleRetryTimer = setTimeout(() => this.renderGoogleButton(), 200);
    } else {
      console.warn('Google SDK not loaded or blocked.');
    }
  }

  onSubmit(): void {
    if (!this.usernameOrEmail() || !this.password()) return;

    this.isLoading.set(true);

    this.authService.login({
      usernameOrEmail: this.usernameOrEmail(),
      password: this.password()
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.toast.show(`Bentornato, ${res.username || 'giocatore'}!`, {
          classname: 'bg-success text-white'
        });
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isLoading.set(false);
        let errorMsg = 'Credenziali non valide. Riprova.';
        if (err.status === 401) {
          errorMsg = 'Username/email o password errati.';
        }
        this.toast.show(errorMsg, {
          classname: 'bg-danger text-white'
        });
      }
    });
  }

  cancelGoogleModal(): void {
    document.body.style.overflow = '';
    this.googleAuth.cancelGoogleRegistration();
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    if (!this.googleAuth.pendingGoogleUser()) return;

    if (event.key === 'Tab') {
      const focusableElements: (HTMLInputElement | HTMLButtonElement)[] = [
        this.usernameInputRef?.nativeElement,
        this.cancelBtnRef?.nativeElement,
        this.completeBtnRef?.nativeElement
      ].filter((el): el is HTMLInputElement | HTMLButtonElement => !!el && !el.hasAttribute('disabled'));

      if (focusableElements.length === 0) return;

      const activeElement = document.activeElement as HTMLInputElement | HTMLButtonElement;
      const currentIndex = focusableElements.indexOf(activeElement);

      event.preventDefault();

      if (event.shiftKey) {
        const prevIndex = currentIndex <= 0 ? focusableElements.length - 1 : currentIndex - 1;
        focusableElements[prevIndex].focus();
      } else {
        const nextIndex = currentIndex === -1 || currentIndex >= focusableElements.length - 1 ? 0 : currentIndex + 1;
        focusableElements[nextIndex].focus();
      }
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelGoogleModal();
    }
  }

  private focusUsernameInput(): void {
    setTimeout(() => {
      this.usernameInputRef?.nativeElement.focus();
    }, 50);
  }
}