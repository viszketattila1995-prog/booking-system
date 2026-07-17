export type TimeSlotStatus = 'AVAILABLE' | 'BOOKED' | 'CANCELLED';

export interface TimeSlotResponse {
  id: string;
  serviceOfferingId: string;
  startTime: string;
  endTime: string;
  status: TimeSlotStatus;
}

export interface CreateTimeSlotRequest {
  startTime: string;
  endTime: string;
}
