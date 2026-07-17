import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProviderService } from '../../../core/services/provider.service';
import { ProviderResponse } from '../../../core/models/provider.models';
import { extractErrorMessage } from '../../../core/utils/api-error.util';

@Component({
  selector: 'app-provider-list',
  imports: [RouterLink],
  templateUrl: './provider-list.component.html',
  styleUrl: './provider-list.component.scss',
})
export class ProviderListComponent {
  private readonly providerService = inject(ProviderService);

  readonly providers = signal<ProviderResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.providerService.listApproved().subscribe({
      next: (providers) => {
        this.providers.set(providers);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(extractErrorMessage(error, 'Could not load providers.'));
        this.loading.set(false);
      },
    });
  }
}
