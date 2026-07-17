import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BookingService } from '../../../core/services/booking.service';
import { ProviderService } from '../../../core/services/provider.service';
import { ServiceOfferingResponse } from '../../../core/models/service-offering.models';
import { TimeSlotResponse } from '../../../core/models/time-slot.models';
import { extractErrorMessage } from '../../../core/utils/api-error.util';

@Component({
  selector: 'app-provider-detail',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './provider-detail.component.html',
  styleUrl: './provider-detail.component.scss',
})
export class ProviderDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly providerService = inject(ProviderService);
  private readonly bookingService = inject(BookingService);
  private readonly router = inject(Router);

  readonly providerId = this.route.snapshot.paramMap.get('id')!;

  readonly offerings = signal<ServiceOfferingResponse[]>([]);
  readonly loadingOfferings = signal(true);
  readonly offeringsError = signal<string | null>(null);

  readonly selectedOfferingId = signal<string | null>(null);
  readonly timeSlots = signal<TimeSlotResponse[]>([]);
  readonly loadingSlots = signal(false);
  readonly slotsError = signal<string | null>(null);

  readonly bookingInProgressSlotId = signal<string | null>(null);
  readonly bookingError = signal<string | null>(null);
  readonly bookingSuccessMessage = signal<string | null>(null);

  constructor() {
    this.providerService.listServiceOfferings(this.providerId).subscribe({
      next: (offerings) => {
        this.offerings.set(offerings);
        this.loadingOfferings.set(false);
      },
      error: (error: unknown) => {
        this.offeringsError.set(extractErrorMessage(error, 'Could not load services.'));
        this.loadingOfferings.set(false);
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
    this.loadTimeSlots(offeringId);
  }

  book(slot: TimeSlotResponse): void {
    this.bookingInProgressSlotId.set(slot.id);
    this.bookingError.set(null);
    this.bookingSuccessMessage.set(null);

    this.bookingService.book(slot.id).subscribe({
      next: () => {
        this.bookingInProgressSlotId.set(null);
        this.bookingSuccessMessage.set('Booked! Redirecting to your bookings…');
        setTimeout(() => this.router.navigateByUrl('/bookings'), 900);
      },
      error: (error: unknown) => {
        this.bookingInProgressSlotId.set(null);
        this.bookingError.set(extractErrorMessage(error, 'Could not book this time slot.'));
        // Valaki más közben lefoglalhatta - frissítjük a listát, hogy a UI ne
        // mutasson tovább egy már nem elérhető AVAILABLE slotot.
        this.loadTimeSlots(slot.serviceOfferingId);
      },
    });
  }

  private loadTimeSlots(offeringId: string): void {
    this.timeSlots.set([]);
    this.slotsError.set(null);
    this.loadingSlots.set(true);

    this.providerService.listTimeSlots(offeringId).subscribe({
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
