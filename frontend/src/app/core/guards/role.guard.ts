import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// A backend a szerepkör-alapú korlátozást @PreAuthorize("hasRole(...)")-lel
// kényszeríti ki a szerveroldalon - ez a guard csak UX: ne is jusson el a
// felhasználó egy olyan nézetig, ami úgyis 403-at adna vissza.
export function roleGuard(role: string): CanActivateFn {
  return (_route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }

    if (!authService.hasRole(role)) {
      return router.createUrlTree(['/providers']);
    }

    return true;
  };
}
