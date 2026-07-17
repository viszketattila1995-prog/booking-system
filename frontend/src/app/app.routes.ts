import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

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
  { path: '**', redirectTo: 'providers' },
];
