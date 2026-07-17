import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'providers' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'providers',
    loadComponent: () =>
      import('./features/providers/provider-list/provider-list.component').then((m) => m.ProviderListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'providers/:id',
    loadComponent: () =>
      import('./features/providers/provider-detail/provider-detail.component').then((m) => m.ProviderDetailComponent),
    canActivate: [authGuard],
  },
  {
    path: 'bookings',
    loadComponent: () =>
      import('./features/bookings/my-bookings/my-bookings.component').then((m) => m.MyBookingsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile.component').then((m) => m.ProfileComponent),
    canActivate: [authGuard],
  },
  {
    path: 'provider',
    loadComponent: () =>
      import('./features/provider-console/provider-console.component').then((m) => m.ProviderConsoleComponent),
    canActivate: [authGuard],
  },
  {
    path: 'provider/services',
    loadComponent: () =>
      import('./features/provider-console/service-offerings/service-offerings.component').then(
        (m) => m.ServiceOfferingsComponent,
      ),
    canActivate: [roleGuard('ROLE_PROVIDER')],
  },
  {
    path: 'admin/providers',
    loadComponent: () =>
      import('./features/admin/admin-providers.component').then((m) => m.AdminProvidersComponent),
    canActivate: [roleGuard('ROLE_ADMIN')],
  },
  { path: '**', redirectTo: 'providers' },
];
