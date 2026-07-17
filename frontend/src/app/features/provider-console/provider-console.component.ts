import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProviderConsoleService } from '../../core/services/provider-console.service';
import { ProviderResponse } from '../../core/models/provider.models';
import { extractErrorMessage } from '../../core/utils/api-error.util';

@Component({
  selector: 'app-provider-console',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './provider-console.component.html',
  styleUrl: './provider-console.component.scss',
})
export class ProviderConsoleComponent {
  private readonly fb = inject(FormBuilder);
  private readonly providerConsoleService = inject(ProviderConsoleService);

  readonly loading = signal(true);
  readonly provider = signal<ProviderResponse | null>(null);
  readonly loadError = signal<string | null>(null);

  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    organizationName: ['', [Validators.required, Validators.maxLength(255)]],
    organizationDescription: ['', [Validators.maxLength(2000)]],
  });

  constructor() {
    this.providerConsoleService.getMine().subscribe({
      next: (provider) => {
        this.provider.set(provider);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loadError.set(extractErrorMessage(error, 'Could not load your provider status.'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);

    this.providerConsoleService.apply(this.form.getRawValue()).subscribe({
      next: (provider) => {
        this.submitting.set(false);
        this.provider.set(provider);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.submitError.set(extractErrorMessage(error, 'Could not submit the application.'));
      },
    });
  }
}
