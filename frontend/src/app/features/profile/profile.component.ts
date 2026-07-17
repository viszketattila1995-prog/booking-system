import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { extractErrorMessage } from '../../core/utils/api-error.util';

@Component({
  selector: 'app-profile',
  imports: [],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly initials = computed(() => {
    const name = this.currentUser()?.fullName ?? '';
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  });

  readonly loggingOutAll = signal(false);
  readonly logoutAllError = signal<string | null>(null);

  formatRole(role: string): string {
    const withoutPrefix = role.replace(/^ROLE_/, '').toLowerCase();
    return withoutPrefix.charAt(0).toUpperCase() + withoutPrefix.slice(1);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  logoutAllDevices(): void {
    this.loggingOutAll.set(true);
    this.logoutAllError.set(null);

    this.authService.logoutAll().subscribe({
      next: () => {
        this.loggingOutAll.set(false);
        this.router.navigate(['/login']);
      },
      error: (error: unknown) => {
        this.loggingOutAll.set(false);
        this.logoutAllError.set(extractErrorMessage(error, 'Could not log out of all devices.'));
      },
    });
  }
}
