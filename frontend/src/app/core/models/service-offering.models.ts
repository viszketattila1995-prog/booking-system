export interface ServiceOfferingResponse {
  id: string;
  providerId: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
}

export interface CreateServiceOfferingRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
}

export interface UpdateServiceOfferingRequest {
  name: string;
  description?: string;
  durationMinutes: number;
  price: number;
  active: boolean;
}
