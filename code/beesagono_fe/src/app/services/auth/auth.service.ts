import { HttpClient } from '@angular/common/http';
import { Injectable, signal, computed, inject, Injector } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { SyncService } from '../sync/sync.service';
import { GameFacadeService } from '../game/game-facade.service';
import { environment } from '../../environments/environment';
import {
  GoogleCheckResponse,
  GoogleLoginRequest,
  GoogleRegisterRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse
} from '../../models/auth/auth-dto.model';

export interface User {
  username: string;
  email?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly syncService = inject(SyncService);
  private readonly injector = inject(Injector);

  // Getter to resolve GameFacadeService lazily at runtime, preventing circular dependency issues
  private get gameFacade(): GameFacadeService {
    return this.injector.get(GameFacadeService);
  }

  readonly currentUser = signal<User | null>(this.getUserFromStorage());
  readonly isLoggedIn = computed(() => !!this.currentUser() && !!localStorage.getItem('token'));

  private getUserFromStorage(): User | null {
    const username = localStorage.getItem('username');
    return username ? { username } : null;
  }

  public setSession(loginResponse: LoginResponse): void {
    localStorage.setItem('token', loginResponse.accessToken);
    if (loginResponse.refreshToken) {
      localStorage.setItem('refreshToken', loginResponse.refreshToken);
    }
    if (loginResponse.username) {
      localStorage.setItem('username', loginResponse.username);
      this.currentUser.set({ username: loginResponse.username });
    }

    this.syncService.syncLocalProgressWithServer().subscribe({
      next: () => {
        this.gameFacade.loadDailyGame();
      },
      error: (err) => {
        console.error('[AuthService] Synchronization failed:', err);
        this.gameFacade.loadDailyGame();
      }
    });
  }

  // -- REGISTER METHOD --
  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/register`, request);
  }

  // -- LOGIN METHOD --
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => this.setSession(response))
    );
  }

  // -- LOGOUT METHOD --
  logout(): void {
    this.http.post(`${this.baseUrl}/logout`, {}).subscribe({
      next: () => this.clearSession(),
      error: () => this.clearSession()
    });
  }

  // -- CHECK GOOGLE USER METHOD --
  checkGoogleUser(request: GoogleLoginRequest): Observable<GoogleCheckResponse> {
    return this.http.post<GoogleCheckResponse>(`${this.baseUrl}/google/check`, request);
  }

  // -- REGISTER GOOGLE USER METHOD --  
  registerGoogleUser(request: GoogleRegisterRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/google/register`, request).pipe(
      tap((response) => this.setSession(response))
    );
  }

  // Helper Method to Clear Session
  public clearSession(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
    this.currentUser.set(null);

    this.gameFacade.loadDailyGame();
    this.router.navigate(['/login']);
  }
}