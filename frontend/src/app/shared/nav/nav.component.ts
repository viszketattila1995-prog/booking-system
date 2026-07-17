import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-nav',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './nav.component.html',
  styleUrl: './nav.component.scss',
})
export class NavComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly currentUser = this.authService.currentUser;
  readonly isProvider = computed(() => this.authService.hasRole('ROLE_PROVIDER'));
  readonly isAdmin = computed(() => this.authService.hasRole('ROLE_ADMIN'));

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
