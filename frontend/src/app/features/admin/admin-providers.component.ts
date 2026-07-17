import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { AdminService } from '../../core/services/admin.service';
import { ProviderResponse } from '../../core/models/provider.models';
import { extractErrorMessage } from '../../core/utils/api-error.util';

@Component({
  selector: 'app-admin-providers',
  imports: [DatePipe],
  templateUrl: './admin-providers.component.html',
  styleUrl: './admin-providers.component.scss',
})
export class AdminProvidersComponent {
  private readonly adminService = inject(AdminService);

  readonly providers = signal<ProviderResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly decidingId = signal<string | null>(null);

  constructor() {
    this.load();
  }

  approve(provider: ProviderResponse): void {
    this.decide(provider, this.adminService.approve(provider.id));
  }

  reject(provider: ProviderResponse): void {
    this.decide(provider, this.adminService.reject(provider.id));
  }

  private decide(provider: ProviderResponse, request: ReturnType<AdminService['approve']>): void {
    this.decidingId.set(provider.id);
    this.errorMessage.set(null);

    request.subscribe({
      next: () => {
        this.decidingId.set(null);
        this.providers.set(this.providers().filter((p) => p.id !== provider.id));
      },
      error: (error: unknown) => {
        this.decidingId.set(null);
        this.errorMessage.set(extractErrorMessage(error, 'Could not process this application.'));
      },
    });
  }

  private load(): void {
    this.adminService.listPendingProviders().subscribe({
      next: (providers) => {
        this.providers.set(providers);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(extractErrorMessage(error, 'Could not load pending applications.'));
        this.loading.set(false);
      },
    });
  }
}
