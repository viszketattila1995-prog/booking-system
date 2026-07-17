import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProviderConsoleService } from '../../../core/services/provider-console.service';
import { ServiceOfferingResponse } from '../../../core/models/service-offering.models';
import { TimeSlotResponse } from '../../../core/models/time-slot.models';
import { extractErrorMessage } from '../../../core/utils/api-error.util';

@Component({
  selector: 'app-service-offerings',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './service-offerings.component.html',
  styleUrl: './service-offerings.component.scss',
})
export class ServiceOfferingsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly providerConsoleService = inject(ProviderConsoleService);

  readonly offerings = signal<ServiceOfferingResponse[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly showCreateForm = signal(false);
  readonly creating = signal(false);
  readonly createError = signal<string | null>(null);

  readonly editingId = signal<string | null>(null);
  readonly savingEditId = signal<string | null>(null);
  readonly editError = signal<string | null>(null);

  readonly deletingId = signal<string | null>(null);

  readonly selectedOfferingId = signal<string | null>(null);
  readonly timeSlots = signal<TimeSlotResponse[]>([]);
  readonly loadingSlots = signal(false);
  readonly slotsError = signal<string | null>(null);
  readonly creatingSlot = signal(false);
  readonly cancellingSlotId = signal<string | null>(null);

  readonly createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(2000)]],
    durationMinutes: [30, [Validators.required, Validators.min(1)]],
    price: [0, [Validators.required, Validators.min(0)]],
  });

  readonly editForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(2000)]],
    durationMinutes: [30, [Validators.required, Validators.min(1)]],
    price: [0, [Validators.required, Validators.min(0)]],
    active: [true],
  });

  readonly slotForm = this.fb.nonNullable.group({
    startTime: ['', [Validators.required]],
    endTime: ['', [Validators.required]],
  });

  constructor() {
    this.loadOfferings();
  }

  toggleCreateForm(): void {
    this.showCreateForm.set(!this.showCreateForm());
    this.createError.set(null);
    this.createForm.reset({ name: '', description: '', durationMinutes: 30, price: 0 });
  }

  submitCreate(): void {
    if (this.createForm.invalid || this.creating()) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.creating.set(true);
    this.createError.set(null);

    this.providerConsoleService.createServiceOffering(this.createForm.getRawValue()).subscribe({
      next: (offering) => {
        this.creating.set(false);
        this.showCreateForm.set(false);
        this.offerings.set([...this.offerings(), offering]);
      },
      error: (error: unknown) => {
        this.creating.set(false);
        this.createError.set(extractErrorMessage(error, 'Could not create the service.'));
      },
    });
  }

  startEdit(offering: ServiceOfferingResponse): void {
    this.editingId.set(offering.id);
    this.editError.set(null);
    this.editForm.reset({
      name: offering.name,
      description: offering.description ?? '',
      durationMinutes: offering.durationMinutes,
      price: offering.price,
      active: offering.active,
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  submitEdit(offeringId: string): void {
    if (this.editForm.invalid || this.savingEditId()) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.savingEditId.set(offeringId);
    this.editError.set(null);

    this.providerConsoleService.updateServiceOffering(offeringId, this.editForm.getRawValue()).subscribe({
      next: (updated) => {
        this.savingEditId.set(null);
        this.editingId.set(null);
        this.offerings.set(this.offerings().map((o) => (o.id === offeringId ? updated : o)));
      },
      error: (error: unknown) => {
        this.savingEditId.set(null);
        this.editError.set(extractErrorMessage(error, 'Could not save changes.'));
      },
    });
  }

  deleteOffering(offering: ServiceOfferingResponse): void {
    this.deletingId.set(offering.id);
    this.providerConsoleService.deleteServiceOffering(offering.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.offerings.set(this.offerings().filter((o) => o.id !== offering.id));
        if (this.selectedOfferingId() === offering.id) {
          this.selectedOfferingId.set(null);
        }
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.loadError.set(extractErrorMessage(error, 'Could not delete the service.'));
      },
    });
  }

  selectOffering(offeringId: string): void {
    if (this.selectedOfferingId() === offeringId) {
      this.selectedOfferingId.set(null);
      this.timeSlots.set([]);
      return;
    }

    this.selectedOfferingId.set(offeringId);
    this.slotForm.reset({ startTime: '', endTime: '' });
    this.loadTimeSlots(offeringId);
  }

  submitCreateSlot(offeringId: string): void {
    if (this.slotForm.invalid || this.creatingSlot()) {
      this.slotForm.markAllAsTouched();
      return;
    }

    const { startTime, endTime } = this.slotForm.getRawValue();

    this.creatingSlot.set(true);
    this.slotsError.set(null);

    this.providerConsoleService
      .createTimeSlot(offeringId, {
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
      })
      .subscribe({
        next: () => {
          this.creatingSlot.set(false);
          this.slotForm.reset({ startTime: '', endTime: '' });
          this.loadTimeSlots(offeringId);
        },
        error: (error: unknown) => {
          this.creatingSlot.set(false);
          this.slotsError.set(extractErrorMessage(error, 'Could not create the time slot.'));
        },
      });
  }

  cancelSlot(slot: TimeSlotResponse): void {
    this.cancellingSlotId.set(slot.id);
    this.providerConsoleService.cancelTimeSlot(slot.id).subscribe({
      next: () => {
        this.cancellingSlotId.set(null);
        this.loadTimeSlots(slot.serviceOfferingId);
      },
      error: (error: unknown) => {
        this.cancellingSlotId.set(null);
        this.slotsError.set(extractErrorMessage(error, 'Could not cancel the time slot.'));
      },
    });
  }

  private loadOfferings(): void {
    this.providerConsoleService.listMyServiceOfferings().subscribe({
      next: (offerings) => {
        this.offerings.set(offerings);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loadError.set(extractErrorMessage(error, 'Could not load your services.'));
        this.loading.set(false);
      },
    });
  }

  private loadTimeSlots(offeringId: string): void {
    this.timeSlots.set([]);
    this.slotsError.set(null);
    this.loadingSlots.set(true);

    this.providerConsoleService.listTimeSlots(offeringId).subscribe({
      next: (slots) => {
        this.timeSlots.set(slots);
        this.loadingSlots.set(false);
      },
      error: (error: unknown) => {
        this.slotsError.set(extractErrorMessage(error, 'Could not load time slots.'));
        this.loadingSlots.set(false);
      },
    });
  }
}
