import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

const STORAGE_KEY = 'bs_auth';

interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  email: string;
  fullName: string;
  roles: string[];
}

// A refresh tokent (és az access tokent) localStorage-ban tartjuk - nincs
// httpOnly cookie a backend oldalán (JSON body-ban adja vissza mindkettőt,
// lásd AuthResponse), ez a pragmatikus megoldás egy cookie-alapú flow nélkül.
// XSS ellen ez önmagában nem védelem, de a backend jelenlegi szerződésével
// (nincs cookie-support) ez a reális kliens-oldali tárolási mód.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  private readonly state = signal<StoredAuth | null>(readStoredAuth());

  readonly isAuthenticated = computed(() => this.state() !== null);
  readonly currentUser = computed(() => {
    const state = this.state();
    return state ? { email: state.email, fullName: state.fullName, roles: state.roles } : null;
  });

  accessToken(): string | null {
    return this.state()?.accessToken ?? null;
  }

  refreshToken(): string | null {
    return this.state()?.refreshToken ?? null;
  }

  hasRole(role: string): boolean {
    return this.state()?.roles.includes(role) ?? false;
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiBaseUrl}/auth/login`, request)
      .pipe(tap((response) => this.applyAuth(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiBaseUrl}/auth/register`, request)
      .pipe(tap((response) => this.applyAuth(response)));
  }

  // Az interceptor hívja 401-nél - sikeres rotáció esetén frissíti a state-et,
  // sikertelennél a hívó (interceptor) felelőssége a clearAuth() + /login redirect.
  refresh(): Observable<AuthResponse> {
    const refreshToken = this.refreshToken();
    return this.http
      .post<AuthResponse>(`${this.apiBaseUrl}/auth/refresh`, { refreshToken })
      .pipe(tap((response) => this.applyAuth(response)));
  }

  logout(): void {
    const refreshToken = this.refreshToken();
    this.clearAuth();
    if (refreshToken) {
      // Best-effort: a kliens oldali state már törölve van attól függetlenül,
      // hogy a szerver oldali revoke sikerül-e (pl. offline kilépés esetén is).
      this.http.post(`${this.apiBaseUrl}/auth/logout`, { refreshToken }).subscribe({ error: () => {} });
    }
  }

  logoutAll(): Observable<void> {
    return this.http.post<void>(`${this.apiBaseUrl}/auth/logout-all`, {}).pipe(tap(() => this.clearAuth()));
  }

  clearAuth(): void {
    this.state.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private applyAuth(response: AuthResponse): void {
    const stored: StoredAuth = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      email: response.email,
      fullName: response.fullName,
      roles: response.roles,
    };
    this.state.set(stored);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  }
}

function readStoredAuth(): StoredAuth | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    return null;
  }
}
