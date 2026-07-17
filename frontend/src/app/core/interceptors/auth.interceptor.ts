import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, shareReplay, switchMap, throwError } from 'rxjs';
import { AuthResponse } from '../models/auth.models';
import { AuthService } from '../services/auth.service';

// Modul-szintű (nem a függvényen belüli), hogy ha egyszerre több kérés is
// 401-et kap, mindegyik UGYANAZT a folyamatban lévő refresh hívást ossza meg,
// ne indítson mindegyik saját rotációt - két egyidejű rotációs kísérlet a
// backend refresh token reuse-detektálását (lásd RefreshTokenService)
// triggerelné feleslegesen, és minden aktív session-t kilőne.
let refreshInFlight: Observable<AuthResponse> | null = null;

const PUBLIC_AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.accessToken();
  const authorizedReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authorizedReq).pipe(
    catchError((error: unknown) => {
      const isPublicAuthEndpoint = PUBLIC_AUTH_PATHS.some((path) => req.url.includes(path));
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isPublicAuthEndpoint || !authService.refreshToken()) {
        return throwError(() => error);
      }

      if (!refreshInFlight) {
        refreshInFlight = authService.refresh().pipe(shareReplay(1));
      }

      return refreshInFlight.pipe(
        switchMap((response) => {
          refreshInFlight = null;
          const retriedReq = req.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } });
          return next(retriedReq);
        }),
        catchError((refreshError: unknown) => {
          refreshInFlight = null;
          authService.clearAuth();
          router.navigate(['/login']);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
