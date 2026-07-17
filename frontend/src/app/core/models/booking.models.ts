export type BookingStatus = 'CONFIRMED' | 'CANCELLED_BY_GUEST' | 'CANCELLED_BY_PROVIDER';

export interface BookingResponse {
  id: string;
  timeSlotId: string;
  guestId: string;
  status: BookingStatus;
  bookedAt: string;
  cancelledAt: string | null;
}
