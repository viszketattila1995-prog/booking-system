export interface ServiceOfferingResponse {
  id: string;
  providerId: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
}
