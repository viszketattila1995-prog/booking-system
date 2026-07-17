import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { BookingService } from '../../../core/services/booking.service';
import { BookingResponse } from '../../../core/models/booking.models';
import { extractErrorMessage } from '../../../core/utils/api-error.util';

@Component({
  selector: 'app-my-bookings',
  imports: [DatePipe],
  templateUrl: './my-bookings.component.html',
  styleUrl: './my-bookings.component.scss',
})
export class MyBookingsComponent {
  private readonly bookingService = inject(BookingService);

  readonly bookings = signal<BookingResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly cancellingId = signal<string | null>(null);

  constructor() {
    this.load();
  }

  cancel(booking: BookingResponse): void {
    this.cancellingId.set(booking.id);
    this.errorMessage.set(null);

    this.bookingService.cancel(booking.id).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.load();
      },
      error: (error: unknown) => {
        this.cancellingId.set(null);
        this.errorMessage.set(extractErrorMessage(error, 'Could not cancel this booking.'));
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.bookingService.listMine().subscribe({
      next: (bookings) => {
        this.bookings.set(bookings);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(extractErrorMessage(error, 'Could not load your bookings.'));
        this.loading.set(false);
      },
    });
  }
}
